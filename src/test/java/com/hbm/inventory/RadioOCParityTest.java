package com.hbm.inventory;

import com.hbm.tileentity.machine.rbmk.TileEntityRBMKTerminal;
import com.hbm.tileentity.network.TileEntityRadioTorchBase;
import com.hbm.tileentity.network.TileEntityRadioTorchController;
import com.hbm.tileentity.network.TileEntityRadioTorchReader;
import com.hbm.tileentity.network.RTTYSystem;
import com.hbm.util.Tuple.Pair;
import com.hbm.util.BufferUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Context;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.lang.reflect.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class RadioOCParityTest {
    Base base; Controller controller; Reader reader; Terminal terminal;
    RoRBurstParityTest.TestWorld world; RoRBurstParityTest.Target target;
    @BeforeAll static void bootstrap() throws Exception {
        AutocalTest.bootstrap();
        Class<?>[] types={Base.class,Controller.class,Reader.class,Terminal.class};
        for(Class<?> type:types) net.minecraftforge.fml.common.registry.GameRegistry.registerTileEntity(
            type.asSubclass(net.minecraft.tileentity.TileEntity.class),new net.minecraft.util.ResourceLocation("hbm","test_oc_"+type.getSimpleName().toLowerCase(Locale.ROOT)));
    }
    @BeforeEach void setup() {
        world=new RoRBurstParityTest.TestWorld(false);target=new RoRBurstParityTest.Target();world.tiles.put(RoRBurstParityTest.OWNER,target);
        base=new Base();controller=new Controller();reader=new Reader();terminal=new Terminal();
        for(com.hbm.tileentity.TileEntityLoadedBase tile:new com.hbm.tileentity.TileEntityLoadedBase[]{base,controller,reader,terminal}){
            tile.setWorld(world);tile.setPos(RoRBurstParityTest.TORCH);
        }
        RTTYSystem.broadcast.clear();RTTYSystem.newMessages.clear();
    }
    Object[] invoke(Object tile,String name,Object... values) throws Exception {
        Arguments args=(Arguments)Proxy.newProxyInstance(Arguments.class.getClassLoader(),new Class<?>[]{Arguments.class},(p,m,a)->{
            if(m.getName().startsWith("check")){
                Object value=values[(Integer)a[0]];
                if(m.getName().equals("checkInteger"))return ((Number)value).intValue();
                return value;
            }
            return null;
        });
        try{return (Object[])tile.getClass().getMethod(name,Context.class,Arguments.class).invoke(tile,null,args);}
        catch(InvocationTargetException e){if(e.getCause() instanceof Exception)throw (Exception)e.getCause();throw e;}
    }
    Object queued(String name){return RTTYSystem.newMessages.get(new Pair<World,String>(world,name));}
    void enable() throws Exception {assertArrayEquals(new Object[]{true},invoke(terminal,"enableOCMode",true));}
    void signal(String channel,String text){RTTYSystem.RTTYChannel c=new RTTYSystem.RTTYChannel();c.signal=text;c.timeStamp=world.time;RTTYSystem.broadcast.put(new Pair<World,String>(world,channel),c);}
    @Test void componentNamesMatchOriginal() throws Exception {
        assertEquals("radio_torch",base.getClass().getMethod("getComponentName").invoke(base));
        assertEquals("radio_controller",controller.getClass().getMethod("getComponentName").invoke(controller));
        assertEquals("radio_reader",reader.getClass().getMethod("getComponentName").invoke(reader));
        assertEquals("rbmk_terminal",terminal.getClass().getMethod("getComponentName").invoke(terminal));
    }
    @Test void baseChannelRoundTripsAndPersists() throws Exception {invoke(base,"setChannel","Mixed");assertArrayEquals(new Object[]{"Mixed"},invoke(base,"getChannel"));assertEquals("Mixed",base.writeToNBT(new NBTTagCompound()).getString("channel"));assertTrue(base.dirty>0);}
    @Test void basePollingRoundTripsAndMarksDirty() throws Exception {invoke(base,"setPolling",true);assertArrayEquals(new Object[]{true},invoke(base,"getPolling"));assertTrue(base.dirty>0);}
    @Test void baseCustomMapRoundTripsAndMarksDirty() throws Exception {invoke(base,"setCustomMap",true);assertArrayEquals(new Object[]{true},invoke(base,"getCustomMap"));assertTrue(base.dirty>0);}
    @Test void baseMappingUsesOneToSixteenAndIgnoresOtherEntries() throws Exception {
        base.mapping[1]="keep";Map<Object,Object> values=new HashMap<>();values.put(1,"zero");values.put(16,"fifteen");values.put(0,"bad");values.put(17,"bad");values.put(2,42);
        invoke(base,"setCustomMapValues",values);assertEquals("zero",base.mapping[0]);assertEquals("fifteen",base.mapping[15]);assertEquals("keep",base.mapping[1]);assertTrue(base.dirty>0);
        assertArrayEquals(base.mapping,(Object[])invoke(base,"getCustomMapValues")[0]);
    }
    @Test void controllerChannelChangeAllowsSameCommandAgain() throws Exception {
        invoke(controller,"setPolling",false);invoke(controller,"setChannel","first");signal("first","doMixed!same");controller.update();
        invoke(controller,"setChannel","second");signal("second","doMixed!same");controller.update();assertEquals(2,target.calls);assertTrue(controller.dirty>0);
    }
    @Test void controllerUnchangedChannelDoesNotReplay() throws Exception {
        invoke(controller,"setPolling",false);invoke(controller,"setChannel","same");signal("same","doMixed!a");controller.update();
        invoke(controller,"setChannel","same");controller.update();assertEquals(1,target.calls);assertArrayEquals(new Object[]{"same"},invoke(controller,"getChannel"));
    }
    @Test void controllerPollingGetterAndPersistence() throws Exception {invoke(controller,"setPolling",false);assertArrayEquals(new Object[]{false},invoke(controller,"getPolling"));assertFalse(controller.writeToNBT(new NBTTagCompound()).getBoolean("polling"));assertTrue(controller.dirty>0);}
    @Test void controllerSendUsesRadioQueue() throws Exception {invoke(controller,"setChannel","send");invoke(controller,"send","write!Lua");assertEquals("write!Lua",queued("send"));}
    @Test void controllerSendIgnoresEmptyChannelOrCommand() throws Exception {invoke(controller,"send","no");assertTrue(RTTYSystem.newMessages.isEmpty());invoke(controller,"setChannel","send");invoke(controller,"send","");assertTrue(RTTYSystem.newMessages.isEmpty());}
    @Test void readerUsesZeroBasedIndicesAndReturnsLastValue() throws Exception {for(int i=0;i<8;i++){invoke(reader,"setChannel",i,"c"+i);invoke(reader,"setName",i,"Metric");reader.prev[i]="v"+i;assertArrayEquals(new Object[]{"c"+i},invoke(reader,"getChannel",i));assertArrayEquals(new Object[]{"Metric"},invoke(reader,"getName",i));assertArrayEquals(new Object[]{"v"+i},invoke(reader,"read",i));}assertTrue(reader.dirty>0);}
    @Test void readerInvalidIndicesDoNotChangeRows() throws Exception {for(int index:new int[]{-1,8,100}){invoke(reader,"setChannel",index,"bad");invoke(reader,"setName",index,"bad");assertEquals(0,invoke(reader,"getChannel",index).length);assertEquals(0,invoke(reader,"getName",index).length);assertEquals(0,invoke(reader,"read",index).length);}for(String value:reader.channels)assertEquals("",value);}
    @Test void readerChannelChangeRepublishesSameValue() throws Exception {invoke(reader,"setChannel",0,"first");invoke(reader,"setName",0,"Metric");reader.update();RTTYSystem.newMessages.clear();invoke(reader,"setChannel",0,"second");reader.update();assertEquals("7",queued("second"));}
    @Test void readerNameChangeRepublishesSameValue() throws Exception {invoke(reader,"setChannel",0,"first");invoke(reader,"setName",0,"Metric");reader.update();RTTYSystem.newMessages.clear();invoke(reader,"setName",0,"Second");reader.update();assertEquals("7",queued("first"));}
    @Test void readerUnchangedSettingsDoNotRepublish() throws Exception {invoke(reader,"setChannel",0,"first");invoke(reader,"setName",0,"Metric");reader.update();RTTYSystem.newMessages.clear();invoke(reader,"setChannel",0,"first");invoke(reader,"setName",0,"Metric");reader.update();assertTrue(RTTYSystem.newMessages.isEmpty());}
    @Test void readerPollingIsSaved() throws Exception {invoke(reader,"setPolling",true);assertArrayEquals(new Object[]{true},invoke(reader,"getPolling"));assertTrue(reader.writeToNBT(new NBTTagCompound()).getBoolean("polling"));assertTrue(reader.dirty>0);}
    @Test void terminalDefaultsToOrdinaryModeForOldSaves() throws Exception {terminal.readFromNBT(new NBTTagCompound());assertArrayEquals(new Object[]{false},invoke(terminal,"isOCMode"));terminal.eval("chan old");assertEquals("old",terminal.channel);}
    @Test void disabledTerminalRejectsOCOutput() throws Exception {assertArrayEquals(new Object[]{false,"OC mode not enabled"},invoke(terminal,"write","no"));assertArrayEquals(new Object[]{false,"OC mode not enabled"},invoke(terminal,"writeln","no"));assertArrayEquals(new Object[]{false,"OC mode not enabled"},invoke(terminal,"clearScreen"));assertEquals("",terminal.history[0]);}
    @Test void disabledTerminalReportsReadErrors() throws Exception {assertArrayEquals(new Object[]{"","OC mode not enabled"},invoke(terminal,"readInput"));assertArrayEquals(new Object[]{null,"OC mode not enabled"},invoke(terminal,"getAllHistory"));}
    @Test void enablingModeClearsHistoryAndShowsReady() throws Exception {terminal.push("old");enable();assertEquals("Terminal ready.",terminal.history[0]);assertEquals("OC MODE ENABLED",terminal.history[1]);for(int i=2;i<17;i++)assertEquals("",terminal.history[i]);assertTrue(terminal.dirty>0);}
    @Test void terminalWriteAndWritelnEachPushOneLine() throws Exception {enable();invoke(terminal,"write","one");invoke(terminal,"writeln","two");assertEquals("two",terminal.history[0]);assertEquals("one",terminal.history[1]);}
    @Test void terminalInputIsLatestHistoryLineWithoutConsumption() throws Exception {enable();terminal.eval("hello");assertArrayEquals(new Object[]{"hello"},invoke(terminal,"readInput"));assertArrayEquals(new Object[]{"hello"},invoke(terminal,"readInput"));invoke(terminal,"write","output");assertArrayEquals(new Object[]{"output"},invoke(terminal,"readInput"));}
    @Test void terminalInputDoesNotExecuteOrdinaryCommandsInOCMode() throws Exception {terminal.channel="keep";terminal.repeatCmd="keep";enable();for(String input:new String[]{"chan other","stop","clear","selfdestruct"}){terminal.eval(input);assertEquals(input,terminal.history[0]);assertEquals("keep",terminal.channel);assertEquals("keep",terminal.repeatCmd);}}
    @Test void terminalHistoryIsSeventeenNewestFirstWithDefensiveCopy() throws Exception {enable();for(int i=0;i<25;i++)invoke(terminal,"write","line"+i);Object[] history=(Object[])invoke(terminal,"getAllHistory")[0];assertEquals(17,history.length);assertEquals("line24",history[0]);assertEquals("line8",history[16]);history[0]="mutated";assertEquals("line24",terminal.history[0]);}
    @Test void terminalNullLinesBecomeEmptyInOCReads() throws Exception {enable();terminal.history[0]=null;assertArrayEquals(new Object[]{""},invoke(terminal,"readInput"));assertEquals("",((Object[])invoke(terminal,"getAllHistory")[0])[0]);}
    @Test void terminalClearScreenKeepsModeAndConfiguration() throws Exception {terminal.channel="keep";terminal.repeatCmd="repeat";enable();invoke(terminal,"clearScreen");for(String line:terminal.history)assertEquals("",line);assertArrayEquals(new Object[]{true},invoke(terminal,"isOCMode"));assertEquals("keep",terminal.channel);assertEquals("repeat",terminal.repeatCmd);assertTrue(terminal.dirty>0);}
    @Test void terminalOCModeSuppressesRepeatAndUsesFastSync() throws Exception {terminal.channel="send";terminal.repeatCmd="no";enable();terminal.update();assertTrue(RTTYSystem.newMessages.isEmpty());assertEquals(10,terminal.range);}
    @Test void disablingOCModeRestoresRepeatAndOrdinaryCommands() throws Exception {terminal.channel="send";terminal.repeatCmd="yes";enable();invoke(terminal,"enableOCMode",false);terminal.update();assertEquals("yes",queued("send"));assertEquals(50,terminal.range);terminal.eval("chan restored");assertEquals("restored",terminal.channel);}
    @Test void terminalModeAndHistorySurviveNbtRoundTrip() throws Exception {enable();invoke(terminal,"write","saved");terminal.channel="channel";terminal.repeatCmd="repeat";NBTTagCompound tag=terminal.writeToNBT(new NBTTagCompound());assertTrue(tag.getBoolean("ocMode"));Terminal loaded=new Terminal();loaded.readFromNBT(tag);assertArrayEquals(new Object[]{true},invoke(loaded,"isOCMode"));assertEquals("saved",loaded.history[0]);assertEquals("channel",loaded.channel);assertEquals("repeat",loaded.repeatCmd);}
    @Test void terminalWireLayoutStillUsesTwoFlagsRepeatAndSeventeenLines() throws Exception {enable();terminal.repeatCmd="repeat";ByteBuf b=Unpooled.buffer();try{terminal.serialize(b);assertFalse(b.readBoolean());assertFalse(b.readBoolean());assertTrue(b.readBoolean());for(String line:terminal.history)assertEquals(line,BufferUtil.readString(b));assertFalse(b.isReadable());}finally{b.release();}}
    @Test void terminalRoRWritesAndSubmitRemainAvailableInOCMode() throws Exception {enable();terminal.runRORFunction("FUN:write",new String[]{"radio","text"});assertEquals("radio text",terminal.history[0]);terminal.runRORFunction("FUN:set17",new String[]{"last"});assertEquals("last",terminal.history[16]);terminal.runRORFunction("FUN:submit",new String[]{"chan","ignored"});assertEquals("chan ignored",terminal.history[0]);assertEquals("",terminal.channel);terminal.runRORFunction("FUN:clear",new String[]{});for(String line:terminal.history)assertEquals("",line);}
    public static class Base extends TileEntityRadioTorchBase {int dirty;@Override public void markDirty(){dirty++;}@Override public void markChanged(){dirty++;}}
    public static class Controller extends RoRBurstParityTest.Controller {}
    public static class Reader extends RoRBurstParityTest.Reader {int dirty;@Override public void markDirty(){dirty++;}}
    public static class Terminal extends TileEntityRBMKTerminal {int dirty,range;@Override public void markDirty(){dirty++;}@Override public void markChanged(){dirty++;}@Override public void networkPackNT(int range){this.range=range;}}
}
