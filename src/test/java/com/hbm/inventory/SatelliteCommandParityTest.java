package com.hbm.inventory;

import com.hbm.saveddata.satellites.*;
import com.hbm.tileentity.network.RTTYSystem;
import com.hbm.util.Tuple.Pair;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.profiler.Profiler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.world.*;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.storage.*;
import net.minecraftforge.common.DimensionManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;
import java.lang.reflect.Field;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class SatelliteCommandParityTest {
    private static Unsafe unsafe;
    @BeforeAll static void bootstrap() throws Exception {
        DataConsumerRecipeTest.bootstrap();
        Field f=Unsafe.class.getDeclaredField("theUnsafe");f.setAccessible(true);unsafe=(Unsafe)f.get(null);
    }
    private static TestWorld world(int dim){TestWorld w=new TestWorld(false);w.provider.setDimension(dim);return w;}
    private static SatelliteRayScan scan(TestWorld w,int x,int z){SatelliteRayScan s=new SatelliteRayScan();s.setTarget(x,z);s.onCommand(w,"survey");return s;}
    private static String read(Satellite s,World w,String...cmd){s.onCommand(w,cmd);return s.getTransmission();}
    private static void event(TestWorld w,int x,int y,int z,String info,int life){SatelliteRayScan.reportEvent(w,x,y,z,info,life);}
    // A registry entry is enough for a relay lookup. Real initialized worlds are covered by the native smoke probe.
    private static void withTarget(int dim,TargetCheck check) throws Exception {
        Field f=DimensionManager.class.getDeclaredField("worlds");f.setAccessible(true);
        @SuppressWarnings("unchecked") Map<Integer,WorldServer> worlds=(Map<Integer,WorldServer>)f.get(null);
        WorldServer target=(WorldServer)unsafe.allocateInstance(WorldServer.class);WorldServer previous=worlds.put(dim,target);
        RTTYSystem.newMessages.clear();
        try{check.run(target);}finally{if(previous==null)worlds.remove(dim);else worlds.put(dim,previous);RTTYSystem.newMessages.clear();}
    }
    private interface TargetCheck{void run(WorldServer target) throws Exception;}
    @Test void relayDeliversAllSignalWordsOnlyToTheRequestedWorld() throws Exception {
        withTarget(5849,target->{TestWorld source=world(0);new SatelliteRelay().onCommand(source,"relay","5849","s05","hello","other","world");assertEquals("hello other world",RTTYSystem.newMessages.get(new Pair<World,String>(target,"s05")));assertFalse(RTTYSystem.newMessages.containsKey(new Pair<World,String>(source,"s05")));});
    }
    @Test void relaySupportsNegativeDimensionIdsAndChannelNames() throws Exception {
        withTarget(-5849,target->{new SatelliteRelay().onCommand(world(0),"relay","-5849","Band:_42","-12");assertEquals("-12",RTTYSystem.newMessages.get(new Pair<World,String>(target,"Band:_42")));});
    }
    @Test void relayPreservesSingleTokenAndEmptySignalArguments() throws Exception {
        withTarget(5849,target->{SatelliteRelay s=new SatelliteRelay();s.onCommand(world(0),"relay","5849","single","word");s.onCommand(world(0),"relay","5849","empty","");assertEquals("word",RTTYSystem.newMessages.get(new Pair<World,String>(target,"single")));assertEquals("",RTTYSystem.newMessages.get(new Pair<World,String>(target,"empty")));});
    }
    @Test void relayKeepsExistingRttyNumericAddition() throws Exception {
        withTarget(5849,target->{SatelliteRelay s=new SatelliteRelay();s.onCommand(world(0),"relay","5849","sum","3");s.onCommand(world(0),"relay","5849","sum","4");assertEquals("7",RTTYSystem.newMessages.get(new Pair<World,String>(target,"sum")));});
    }
    @Test void invalidDimensionDoesNotFallBackToLoadedOverworld() throws Exception {
        withTarget(0,target->{SatelliteRelay s=new SatelliteRelay();for(String dim:new String[]{"bad","2147483648","-2147483649","1.5",""})s.onCommand(world(0),"relay",dim,"s05","bad");assertTrue(RTTYSystem.newMessages.isEmpty());});
    }
    @Test void incompleteAndUnknownRelayCommandsCannotBroadcast() throws Exception {
        withTarget(5849,target->{SatelliteRelay s=new SatelliteRelay();TestWorld w=world(0);for(String[] cmd:new String[][]{{},{"relay"},{"relay","5849"},{"relay","5849","band"},{"unknown","5849","band","bad"}})s.onCommand(w,cmd);assertTrue(RTTYSystem.newMessages.isEmpty());});
    }
    @Test void unloadedRelayTargetRemainsUnloaded(){RTTYSystem.newMessages.clear();assertNull(DimensionManager.getWorld(5850,false));new SatelliteRelay().onCommand(world(0),"relay","5850","band","bad");assertNull(DimensionManager.getWorld(5850,false));assertTrue(RTTYSystem.newMessages.isEmpty());}
    @Test void relayCannotBroadcastFromClientWorld() throws Exception {
        withTarget(5849,target->{new SatelliteRelay().onCommand(new TestWorld(true),"relay","5849","band","bad");assertTrue(RTTYSystem.newMessages.isEmpty());});
    }
    @Test void surveyIncludesCircularBoundaryAndExcludesOutsideCorners(){
        SatelliteRayScan.clearAll();TestWorld w=world(0);event(w,250,64,0,"edge",40);event(w,150,64,200,"diagonal",40);event(w,251,64,0,"outside",40);event(w,250,64,250,"corner",40);SatelliteRayScan s=scan(w,0,0);assertEquals("2",read(s,w,"count"));assertEquals("edge",read(s,w,"getinfo","1"));assertEquals("150;200",read(s,w,"getposition","2"));
    }
    @Test void surveyWorksAtBothMinecraftWorldBordersWithoutIntOverflow(){
        for(int center:new int[]{-29999700,29999700}){SatelliteRayScan.clearAll();TestWorld w=world(0);event(w,center+250,20,center,"edge",20);event(w,-center,20,-center,"far",20);assertEquals("1",read(scan(w,center,center),w,"count"));}
    }
    @Test void surveyCannotReadOtherDimensionAtSameCoordinates(){
        SatelliteRayScan.clearAll();TestWorld a=world(0),b=world(-1);event(a,1,64,2,"local",20);event(b,1,64,2,"foreign",20);assertEquals("local",read(scan(a,0,0),a,"getinfo","1"));assertEquals("foreign",read(scan(b,0,0),b,"getinfo","1"));
    }
    @Test void surveyCannotReadAnotherServerAtSameCoordinates() throws Exception {
        SatelliteRayScan.clearAll();TestWorld a=world(0),b=world(0);a.server=(MinecraftServer)unsafe.allocateInstance(DedicatedServer.class);b.server=(MinecraftServer)unsafe.allocateInstance(DedicatedServer.class);event(a,1,64,2,"first",20);event(b,1,64,2,"second",20);assertEquals("first",read(scan(a,0,0),a,"getinfo","1"));assertEquals("second",read(scan(b,0,0),b,"getinfo","1"));
    }
    @Test void expirationUsesWorldClockAndDoesNotExpireOtherDimensions(){
        SatelliteRayScan.clearAll();TestWorld a=world(0),b=world(1);a.time=100;b.time=100;event(a,0,64,0,"a",2);event(b,0,64,0,"b",2);a.time=102;SatelliteRayScan.updateSystem(a);assertEquals("1",read(scan(a,0,0),a,"count"));a.time=103;SatelliteRayScan.updateSystem(a);assertEquals("0",read(scan(a,0,0),a,"count"));assertEquals("1",read(scan(b,0,0),b,"count"));
    }
    @Test void eventRefreshKeepsOnePositionAndDifferentHeightsRemainDistinct(){
        SatelliteRayScan.clearAll();TestWorld w=world(0);event(w,1,64,2,"old",1);event(w,1,64,2,"fresh",20);event(w,1,65,2,"higher",20);w.time=2;SatelliteRayScan.updateSystem(w);SatelliteRayScan s=scan(w,0,0);assertEquals("2",read(s,w,"count"));assertEquals("fresh",read(s,w,"getinfo","1"));assertEquals("higher",read(s,w,"getinfo","2"));
    }
    @Test void nonPositiveLifetimeLastsAtLeastOneTick(){SatelliteRayScan.clearAll();TestWorld w=world(0);event(w,0,0,0,"a",-20);w.time=1;SatelliteRayScan.updateSystem(w);assertEquals("1",read(scan(w,0,0),w,"count"));w.time=2;SatelliteRayScan.updateSystem(w);assertEquals("0",read(scan(w,0,0),w,"count"));}
    @Test void queryIndicesRejectMalformedZeroNegativeAndOverflow(){
        SatelliteRayScan.clearAll();TestWorld w=world(0);event(w,0,0,0,"first",20);SatelliteRayScan s=scan(w,0,0);for(String index:new String[]{"0","-1","2","bad","2147483648","-2147483648"}){assertEquals("",read(s,w,"getinfo",index));assertEquals("",read(s,w,"getposition",index));}assertEquals("first",read(s,w,"getinfo","1"));
    }
    @Test void surveySnapshotChangesOnlyWhenSurveyRunsAgain(){SatelliteRayScan.clearAll();TestWorld w=world(0);SatelliteRayScan s=scan(w,0,0);event(w,0,64,0,"later",20);assertEquals("0",read(s,w,"count"));s.onCommand(w,"survey");assertEquals("1",read(s,w,"count"));SatelliteRayScan.clearWorld(w);assertEquals("1",read(s,w,"count"));s.onCommand(w,"survey");assertEquals("0",read(s,w,"count"));}
    @Test void boundedHistoryEvictsOldestInsteadOfGrowingForever(){SatelliteRayScan.clearAll();TestWorld w=world(0);for(int y=0;y<4097;y++)event(w,0,y,0,"e"+y,20);SatelliteRayScan s=scan(w,0,0);assertEquals("4096",read(s,w,"count"));assertEquals("e1",read(s,w,"getinfo","1"));assertEquals("e4096",read(s,w,"getinfo","4096"));}
    @Test void worldCleanupPreservesOtherDimensions(){SatelliteRayScan.clearAll();TestWorld a=world(0),b=world(1);event(a,0,0,0,"a",20);event(b,0,0,0,"b",20);SatelliteRayScan.clearWorld(a);assertEquals("0",read(scan(a,0,0),a,"count"));assertEquals("b",read(scan(b,0,0),b,"getinfo","1"));SatelliteRayScan.clearAll();assertEquals("0",read(scan(b,0,0),b,"count"));}
    @Test void nullAndClientEventReportsCannotPublish(){SatelliteRayScan.clearAll();SatelliteRayScan.reportEvent(null,0,0,0,"bad",20);SatelliteRayScan.reportEvent(new TestWorld(true),0,0,0,"bad",20);TestWorld w=world(0);assertEquals("0",read(scan(w,0,0),w,"count"));}
    @Test void nullEventInfoIsAnEmptyTransmission(){SatelliteRayScan.clearAll();TestWorld w=world(0);event(w,0,0,0,null,20);SatelliteRayScan s=scan(w,0,0);assertEquals("1",read(s,w,"count"));assertEquals("",read(s,w,"getinfo","1"));assertEquals("0;0",read(s,w,"getposition","1"));}
    private static class TestWorld extends World {
        long time;MinecraftServer server;
        TestWorld(boolean client){super(new SaveHandlerMP(),new WorldInfo(new NBTTagCompound()),new WorldProviderSurface(),new Profiler(),client);}
        @Override public long getTotalWorldTime(){return time;}
        @Override public MinecraftServer getMinecraftServer(){return server;}
        @Override protected IChunkProvider createChunkProvider(){return null;}
        @Override protected boolean isChunkLoaded(int x,int z,boolean allowEmpty){return true;}
    }
}
