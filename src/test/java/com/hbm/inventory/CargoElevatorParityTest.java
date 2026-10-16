package com.hbm.inventory;

import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.api.redstoneoverradio.RORFunctionException;
import com.hbm.tileentity.machine.TileEntityCargoElevator;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.math.AxisAlignedBB;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class CargoElevatorParityTest {
    static final BlockPos POSITION = new BlockPos(0,64,0);
    Elevator lift;
    @BeforeAll static void bootstrap() throws Exception {
        AutocalTest.bootstrap();
        net.minecraftforge.fml.common.registry.GameRegistry.registerTileEntity(Elevator.class,new net.minecraft.util.ResourceLocation("hbm","test_cargo_elevator"));
    }
    @BeforeEach void setup() { lift=loaded(6,0,0,false); }
    NBTTagCompound state(int height,double extension) {
        NBTTagCompound tag=new NBTTagCompound();tag.setInteger("height",height);tag.setDouble("extension",extension);tag.setBoolean("renderPlatform",true);return tag;
    }
    Elevator loaded(int height,double extension,int target,boolean client) {
        NBTTagCompound tag=state(height,extension);tag.setInteger("targetExtension",target);return restored(tag,client);
    }
    Elevator restored(NBTTagCompound tag,boolean client) {
        Elevator lift=new Elevator();lift.readFromNBT(tag);lift.setWorld(new TestWorld(client));lift.setPos(POSITION);return lift;
    }
    int target(Elevator lift) { return lift.writeToNBT(new NBTTagCompound()).getInteger("targetExtension"); }
    void command(String input) {
        IRORInteractive interactive=(IRORInteractive)lift;
        interactive.runRORFunction(IRORInteractive.resolveFunctionName(interactive,input),IRORInteractive.getParams(input));
    }
    void ticks(int count) {for(int i=0;i<count;i++)lift.update();}
    @Test void implementsTheOriginalInteractiveContract() { assertTrue(lift instanceof IRORInteractive); }
    @Test void declaresOnlyTheWorkingRadioCommand() {
        assertArrayEquals(new String[]{"FUN:setextension!amount"},((IRORInteractive)lift).getFunctionInfo());
        assertFalse(lift instanceof IRORValueProvider);
    }
    @Test void commandAcceptsEveryHeightWithinShaft() {
        for(int i=0;i<=6;i++){command("setextension!"+i);assertEquals(i,target(lift));}
    }
    @Test void mixedCaseDispatchKeepsLocalResolver() {command("SeTeXtEnSiOn!3");assertEquals(3,target(lift));}
    @Test void commandRetainsRoundedNumericInputs() {command("setextension!3.6");assertEquals(4,target(lift));}
    @Test void missingParameterDoesNotChangeTarget() {lift=loaded(6,2,3,false);command("setextension");assertEquals(3,target(lift));}
    @Test void unknownCommandDoesNotChangeTarget() {lift=loaded(6,2,3,false);command("other!5");assertEquals(3,target(lift));}
    @Test void malformedParameterKeepsTarget() {
        lift=loaded(6,2,3,false);assertThrows(RORFunctionException.class,()->command("setextension!bad"));assertEquals(3,target(lift));
    }
    @Test void outOfRangeTargetsAreRejected() {
        lift=loaded(6,2,3,false);
        for(String input:Arrays.asList("-1","7","100000000000"))assertThrows(RORFunctionException.class,()->command("setextension!"+input));
        assertEquals(3,target(lift));
    }
    @Test void commandMakesTargetSaveable() {command("setextension!3");assertTrue(lift.dirty>0);assertTrue(lift.changed>0);}
    @Test void movesUpAtExistingSpeed() {lift=loaded(6,0,3,false);ticks(1);assertEquals(0.1,lift.extension,1E-9);}
    @Test void movesDownAtExistingSpeed() {lift=loaded(6,6,3,false);ticks(1);assertEquals(5.9,lift.extension,1E-9);}
    @Test void stopsExactlyAtIntermediateHeightFromBelow() {lift=loaded(6,0,3,false);ticks(80);assertEquals(3,lift.extension,0);}
    @Test void stopsExactlyAtIntermediateHeightFromAbove() {lift=loaded(6,6,3,false);ticks(80);assertEquals(3,lift.extension,0);}
    @Test void upwardLastStepDoesNotOvershootTarget() {lift=loaded(6,2.95,3,false);ticks(1);assertEquals(3,lift.extension,0);}
    @Test void downwardLastStepDoesNotOvershootTarget() {lift=loaded(6,3.05,3,false);ticks(1);assertEquals(3,lift.extension,0);}
    @Test void remainsAtTargetAcrossFurtherTicks() {lift=loaded(6,3,3,false);ticks(20);assertEquals(3,lift.extension,0);}
    @Test void newCommandReversesWhileMoving() {
        command("setextension!6");ticks(20);double before=lift.extension;command("setextension!0");ticks(1);assertEquals(before-0.1,lift.extension,1E-9);ticks(30);assertEquals(0,lift.extension,0);
    }
    @Test void clickAtBottomTargetsTop() {lift.toggleElevator();assertEquals(6,target(lift));ticks(70);assertEquals(6,lift.extension,0);}
    @Test void clickDuringAscentReversesDown() {lift=loaded(6,2,6,false);lift.toggleElevator();assertEquals(0,target(lift));ticks(30);assertEquals(0,lift.extension,0);}
    @Test void clickDuringDescentReversesUp() {lift=loaded(6,4,0,false);lift.toggleElevator();assertEquals(6,target(lift));ticks(30);assertEquals(6,lift.extension,0);}
    @Test void clickAtIntermediateTargetHeadsDown() {lift=loaded(6,3,3,false);lift.toggleElevator();assertEquals(0,target(lift));}
    @Test void clickKeepsDirtyAndChangedNotifications() {lift.toggleElevator();assertTrue(lift.dirty>0);assertTrue(lift.changed>0);}
    @Test void oldAscendingSaveMigratesToShaftHeight() {
        NBTTagCompound old=state(6,2.25);old.setBoolean("isExtending",true);lift=restored(old,false);assertEquals(6,target(lift));ticks(50);assertEquals(6,lift.extension,0);
    }
    @Test void oldDescendingSaveMigratesToBottom() {
        NBTTagCompound old=state(6,2.25);old.setBoolean("isExtending",false);lift=restored(old,false);assertEquals(0,target(lift));ticks(30);assertEquals(0,lift.extension,0);
    }
    @Test void explicitNewTargetWinsOverOldDirection() {
        NBTTagCompound saved=state(6,2.25);saved.setBoolean("isExtending",true);saved.setInteger("targetExtension",3);lift=restored(saved,false);assertEquals(3,target(lift));ticks(20);assertEquals(3,lift.extension,0);
    }
    @Test void explicitNewZeroWinsOverOldAscent() {
        NBTTagCompound saved=state(6,2.25);saved.setBoolean("isExtending",true);saved.setInteger("targetExtension",0);lift=restored(saved,false);ticks(30);assertEquals(0,lift.extension,0);
    }
    @Test void defaultEmptySaveStaysAtBottom() {lift=restored(new NBTTagCompound(),false);ticks(3);assertEquals(0,lift.extension,0);assertEquals(0,target(lift));}
    @Test void nativeNbtRoundTripPreservesProgressAndDestination() {
        lift=loaded(6,2.25,3,false);NBTTagCompound saved=lift.writeToNBT(new NBTTagCompound());lift=restored(saved,false);
        assertEquals(6,lift.height);assertEquals(2.25,lift.extension,0);assertEquals(3,target(lift));assertTrue(lift.renderPlatform);ticks(20);assertEquals(3,lift.extension,0);
    }
    @Test void packetLayoutKeepsExistingClientFieldsOnly() {
        lift=loaded(6,2.25,3,false);ByteBuf buf=Unpooled.buffer();try {
            lift.serialize(buf);assertFalse(buf.readBoolean());assertFalse(buf.readBoolean());assertTrue(buf.readBoolean());assertEquals(6,buf.readShort());assertEquals(2.25,buf.readDouble(),0);assertFalse(buf.isReadable());
        }finally{buf.release();}
    }
    @Test void clientKeepsExistingThreeTickInterpolation() {
        lift=loaded(6,0,0,true);ByteBuf buf=Unpooled.buffer();try {
            buf.writeBoolean(false).writeBoolean(false).writeBoolean(true).writeShort(6).writeDouble(3);
            lift.deserialize(buf);ticks(1);assertEquals(1,lift.extension,0);ticks(1);assertEquals(2,lift.extension,0);ticks(1);assertEquals(3,lift.extension,0);
        }finally{buf.release();}
    }
    @Test void serverUpdatesKeepPlatformAndPreviousPosition() {lift=loaded(6,2,3,false);ticks(1);assertEquals(2,lift.prevExtension,0);assertTrue(lift.renderPlatform);}
    @Test void renderBoundsKeepCurrentShaftDimensions() {
        lift=loaded(6,2,3,false);AxisAlignedBB box=lift.getRenderBoundingBox();assertEquals(-1,box.minX,0);assertEquals(2,box.maxX,0);assertEquals(64,box.minY,0);assertEquals(71,box.maxY,0);
    }
    public static class Elevator extends TileEntityCargoElevator {
        int dirty,changed;
        @Override public void markDirty(){dirty++;}
        @Override public void markChanged(){changed++;}
        @Override public void networkPackNT(int range){}
    }
    static class TestWorld extends World {
        TestWorld(boolean client){super(new SaveHandlerMP(),new WorldInfo(new NBTTagCompound()),new WorldProviderSurface(),new Profiler(),client);chunkProvider=createChunkProvider();}
        @Override protected IChunkProvider createChunkProvider(){return (IChunkProvider)Proxy.newProxyInstance(IChunkProvider.class.getClassLoader(),new Class<?>[]{IChunkProvider.class},(p,m,a)->m.getReturnType()==boolean.class?true:null);}
        @Override protected boolean isChunkLoaded(int x,int z,boolean allowEmpty){return true;}
        @Override public IBlockState getBlockState(BlockPos pos){return Blocks.AIR.getDefaultState();}
        @Override public <T extends Entity> List<T> getEntitiesWithinAABB(Class<? extends T> type,AxisAlignedBB box){return Collections.emptyList();}
    }
}
