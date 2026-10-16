package com.hbm.inventory;

import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.network.RadioTorchBase;
import com.hbm.blocks.network.RadioTorchController;
import com.hbm.tileentity.network.RTTYSystem;
import com.hbm.tileentity.network.TileEntityRadioTorchController;
import com.hbm.tileentity.network.TileEntityRadioTorchReader;
import com.hbm.util.BufferUtil;
import com.hbm.util.Tuple.Pair;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.profiler.Profiler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.storage.SaveHandlerMP;
import net.minecraft.world.storage.WorldInfo;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class RoRBurstParityTest {
    static final BlockPos OWNER = new BlockPos(0,64,0), TORCH = OWNER.up();
    TestWorld world; Target target; Controller controller; Reader reader;
    @BeforeAll static void bootstrap() throws Exception {
        AutocalTest.bootstrap();
        net.minecraftforge.fml.common.registry.GameRegistry.registerTileEntity(Controller.class,new net.minecraft.util.ResourceLocation("hbm","test_ror_controller"));
        net.minecraftforge.fml.common.registry.GameRegistry.registerTileEntity(Reader.class,new net.minecraft.util.ResourceLocation("hbm","test_ror_reader"));
    }
    @BeforeEach void setup() {
        RTTYSystem.broadcast.clear(); RTTYSystem.newMessages.clear(); world=new TestWorld(false);
        target=new Target(); world.tiles.put(OWNER,target);target.setWorld(world);target.setPos(OWNER);
        controller=new Controller();controller.setWorld(world);controller.setPos(TORCH);controller.channel="commands";controller.polling=false;
        reader=new Reader();reader.setWorld(world);reader.setPos(TORCH);reader.channels[0]="values";reader.names[0]="Metric";
    }
    void signal(String channel,String command,long time) {
        RTTYSystem.RTTYChannel value=new RTTYSystem.RTTYChannel();value.signal=command;value.timeStamp=time;
        RTTYSystem.broadcast.put(new Pair<World,String>(world,channel),value);
    }
    void signal(String command) { signal("commands",command,world.time); }
    void tick() { controller.update();world.time++; }
    void commandChannel(String channel) {NBTTagCompound nbt=new NBTTagCompound();nbt.setString("channel",channel);controller.receiveControl(nbt);}
    Object queued(String channel) {return RTTYSystem.newMessages.get(new Pair<World,String>(world,channel));}
    @Test void freshCommandDispatchesOnceAndPreservesDeclaredCase() {
        signal("dOmIxEd!one:two");tick();assertEquals(1,target.calls);assertEquals("FUN:doMixed",target.lastName);assertArrayEquals(new String[]{"one","two"},target.params);
    }
    @Test void sameContinuousSignalExecutesOnlyOnceInStateChangeMode() {
        for(int i=0;i<5;i++){signal("doMixed!same");tick();}assertEquals(1,target.calls);
    }
    @Test void changedCommandExecutesWithinContinuousBurst() {
        signal("doMixed!first");tick();signal("doMixed!second");tick();assertEquals(2,target.calls);
    }
    @Test void identicalCommandExecutesAgainAfterIdleGap() {
        signal("doMixed!same");tick();tick();tick();signal("doMixed!same");tick();assertEquals(2,target.calls);
    }
    @Test void pollingExecutesEveryFreshTick() {
        controller.polling=true;for(int i=0;i<5;i++){signal("doMixed!same");tick();}assertEquals(5,target.calls);
    }
    @Test void staleChangedCommandNeverExecutes() {
        signal("commands","doMixed!old",world.time-2);tick();assertEquals(0,target.calls);assertNull(controller.prev);
    }
    @Test void pollingAlsoIgnoresStaleCommand() {
        controller.polling=true;signal("commands","doMixed!old",world.time-2);tick();assertEquals(0,target.calls);
    }
    @Test void uninitializedTimestampDoesNotExecuteAtWorldStart() {
        world.time=0;signal("commands","doMixed!old",-1);tick();assertEquals(0,target.calls);
    }
    @Test void previousTickMessageIsStillFresh() {
        signal("commands","doMixed!fresh",world.time-1);tick();assertEquals(1,target.calls);
    }
    @Test void channelChangeResetsSameCommandSuppression() {
        signal("doMixed!same");tick();commandChannel("other");signal("other","doMixed!same",world.time);tick();assertEquals(2,target.calls);
    }
    @Test void savingUnchangedChannelDoesNotReplayCommand() {
        signal("doMixed!same");tick();commandChannel("commands");signal("doMixed!same");tick();assertEquals(1,target.calls);
    }
    Controller restored(NBTTagCompound saved) {
        Controller loaded=new Controller();loaded.setWorld(world);loaded.readFromNBT(saved);loaded.setPos(TORCH);return loaded;
    }
    @Test void reloadPreservesBurstAcrossOneEmptyQueueTick() {
        signal("doMixed!same");tick();NBTTagCompound saved=controller.writeToNBT(new NBTTagCompound());
        controller=restored(saved);RTTYSystem.broadcast.clear();tick();signal("doMixed!same");tick();assertEquals(1,target.calls);
    }
    @Test void reloadAllowsNewBurstAfterTwoEmptyTicks() {
        signal("doMixed!same");tick();controller=restored(controller.writeToNBT(new NBTTagCompound()));
        RTTYSystem.broadcast.clear();tick();tick();signal("doMixed!same");tick();assertEquals(2,target.calls);
    }
    @Test void oldSaveWithoutBurstFlagPreservesPreviousCommand() {
        NBTTagCompound old=new NBTTagCompound();old.setString("channel","commands");old.setString("prev","doMixed!same");old.setBoolean("polling",false);
        controller=restored(old);tick();signal("doMixed!same");tick();assertEquals(0,target.calls);
    }
    @Test void explicitlyIdleSavedStateAllowsSameCommand() {
        NBTTagCompound old=new NBTTagCompound();old.setString("channel","commands");old.setString("prev","doMixed!same");old.setBoolean("receiving",false);
        controller=restored(old);signal("doMixed!same");tick();assertEquals(1,target.calls);
    }
    @Test void burstChangesAreMarkedDirtyAndStoredWithExistingKeys() {
        signal("doMixed!same");tick();NBTTagCompound saved=controller.writeToNBT(new NBTTagCompound());
        assertTrue(saved.getBoolean("receiving"));assertEquals("commands",saved.getString("channel"));assertFalse(saved.getBoolean("polling"));assertEquals("doMixed!same",saved.getString("prev"));assertTrue(controller.dirty>0);
        tick();tick();assertFalse(controller.writeToNBT(new NBTTagCompound()).getBoolean("receiving"));
    }
    @Test void malformedCommandDoesNotPreventNextValidCommand() {
        signal("bad!a!b");tick();signal("doMixed!good");tick();assertEquals(1,target.calls);
    }
    @Test void clientControllerNeverDispatches() {signal("doMixed!same");controller.setWorld(new TestWorld(true));controller.update();assertEquals(0,target.calls);}
    @Test void firstReaderTickPublishesEvenWhenSavedValueMatches() {
        reader.prev[0]="7";reader.update();assertEquals("7",queued("values"));
    }
    @Test void unchangedReaderValueIsSuppressedAfterFirstPublish() {
        reader.update();RTTYSystem.newMessages.clear();reader.update();assertNull(queued("values"));
    }
    @Test void pollingReaderPublishesEveryTick() {
        reader.polling=true;reader.update();RTTYSystem.newMessages.clear();reader.update();assertEquals("7",queued("values"));
    }
    @Test void readerChannelChangePublishesUnchangedValue() {
        reader.update();RTTYSystem.newMessages.clear();NBTTagCompound change=new NBTTagCompound();change.setString("channels0","new");reader.receiveControl(change);reader.update();assertEquals("7",queued("new"));
    }
    @Test void readerValueNameChangePublishesEqualValue() {
        reader.update();RTTYSystem.newMessages.clear();NBTTagCompound change=new NBTTagCompound();change.setString("names0","Second");reader.receiveControl(change);reader.update();assertEquals("7",queued("values"));
    }
    @Test void unchangedReaderSettingsDoNotResend() {
        reader.update();RTTYSystem.newMessages.clear();NBTTagCompound change=new NBTTagCompound();change.setString("channels0","values");change.setString("names0","Metric");reader.receiveControl(change);reader.update();assertTrue(RTTYSystem.newMessages.isEmpty());
    }
    @Test void readerChangingOneRowDoesNotRepublishOthers() {
        reader.channels[1]="other";reader.names[1]="Metric";reader.update();RTTYSystem.newMessages.clear();
        NBTTagCompound change=new NBTTagCompound();change.setString("channels0","new");reader.receiveControl(change);reader.update();assertEquals("7",queued("new"));assertNull(queued("other"));
    }
    @Test void loadedReaderRepublishesSavedValuesIntoEmptyQueue() {
        reader.update();NBTTagCompound saved=reader.writeToNBT(new NBTTagCompound());reader=new Reader();reader.setWorld(world);reader.setPos(TORCH);reader.readFromNBT(saved);RTTYSystem.newMessages.clear();reader.update();assertEquals("7",queued("values"));
    }
    @Test void nullValueDoesNotConsumePendingUpdate() {
        target.result=null;reader.prev[0]="7";reader.update();assertNull(queued("values"));target.result="7";reader.update();assertEquals("7",queued("values"));
    }
    @Test void emptyValueCanBePublishedInitially() {target.result="";reader.update();assertEquals("",queued("values"));}
    @Test void readerKeepsMixedCaseValueResolution() {reader.names[0]="mEtRiC";reader.update();assertEquals("VAL:Metric",target.lastValue);assertEquals("7",queued("values"));}
    @Test void controllerAndReaderPacketLayoutsStayUnchanged() {
        ByteBuf buf=Unpooled.buffer();try {
            controller.serialize(buf);assertFalse(buf.readBoolean());assertEquals("commands",BufferUtil.readString(buf));assertFalse(buf.isReadable());
            reader.serialize(buf);assertFalse(buf.readBoolean());for(String channel:reader.channels)assertEquals(channel,BufferUtil.readString(buf));for(String name:reader.names)assertEquals(name,BufferUtil.readString(buf));assertFalse(buf.isReadable());
        }finally{buf.release();}
    }
    @Test void controllerAttachesToInteractiveOnlyTargetOnAllSixSides() {
        world.placement=true;world.tiles.put(OWNER,new InteractiveOnly());RadioTorchController block=(RadioTorchController)ModBlocks.radio_torch_controller;
        for(EnumFacing side:EnumFacing.values())assertTrue(block.canPlaceBlockOnSide(world,OWNER.offset(side),side),side.toString());
    }
    @Test void controllerRejectsValueOnlyTargetAndOrdinaryStone() {
        world.placement=true;RadioTorchController block=(RadioTorchController)ModBlocks.radio_torch_controller;
        world.tiles.put(OWNER,new ValueOnly());assertFalse(block.canPlaceBlockOnSide(world,TORCH,EnumFacing.UP));world.tiles.clear();assertFalse(block.canPlaceBlockOnSide(world,TORCH,EnumFacing.UP));
    }
    public static class Controller extends TileEntityRadioTorchController {
        int dirty;@Override public void markDirty(){dirty++;}@Override public void networkPackNT(int range){}
    }
    public static class Reader extends TileEntityRadioTorchReader {
        @Override public void markDirty(){}@Override public void networkPackNT(int range){}
    }
    static class InteractiveOnly extends TileEntity implements IRORInteractive {
        int calls;String lastName;String[] params;
        @Override public String[] getFunctionInfo(){return new String[]{"FUN:doMixed!params"};}
        @Override public String runRORFunction(String name,String[] params){calls++;lastName=name;this.params=params;return null;}
    }
    static class ValueOnly extends TileEntity implements IRORValueProvider {
        @Override public String[] getFunctionInfo(){return new String[]{"VAL:Metric"};}
        @Override public String provideRORValue(String name){return "7";}
    }
    static class Target extends InteractiveOnly implements IRORValueProvider {
        String result="7",lastValue;
        @Override public String[] getFunctionInfo(){return new String[]{"FUN:doMixed!params","VAL:Metric","VAL:Second"};}
        @Override public String provideRORValue(String name){lastValue=name;return result;}
    }
    static class TestWorld extends World {
        long time=100;boolean placement;Map<BlockPos,TileEntity> tiles=new HashMap<>();
        TestWorld(boolean client){super(new SaveHandlerMP(),new WorldInfo(new NBTTagCompound()),new WorldProviderSurface(),new Profiler(),client);chunkProvider=createChunkProvider();}
        @Override public long getTotalWorldTime(){return time;}
        @Override protected IChunkProvider createChunkProvider(){return (IChunkProvider)Proxy.newProxyInstance(IChunkProvider.class.getClassLoader(),new Class<?>[]{IChunkProvider.class},(p,m,a)->m.getReturnType()==boolean.class?true:null);}
        @Override protected boolean isChunkLoaded(int x,int z,boolean allowEmpty){return true;}
        @Override public TileEntity getTileEntity(BlockPos pos){return tiles.get(pos);}
        @Override public IBlockState getBlockState(BlockPos pos){
            if(placement)return pos.equals(OWNER)?Blocks.STONE.getDefaultState():Blocks.AIR.getDefaultState();
            return pos.equals(TORCH)?ModBlocks.radio_torch_controller.getDefaultState().withProperty(RadioTorchBase.FACING,EnumFacing.UP):Blocks.STONE.getDefaultState();
        }
    }
}
