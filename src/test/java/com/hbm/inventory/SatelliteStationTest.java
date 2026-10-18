package com.hbm.inventory;

import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.items.ISatChip;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemDrive.EnumDriveType;
import com.hbm.saveddata.satellites.*;
import com.hbm.tileentity.machine.TileEntityMachineSatLink;
import com.hbm.tileentity.network.RTTYSystem;
import com.hbm.util.Tuple.Pair;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.storage.*;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.registry.GameRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class SatelliteStationTest {
    private static final int FREQ=58600, REMOTE=5847;
    private static final String VALUE=IRORValueProvider.PREFIX_VALUE, FUNCTION=IRORInteractive.PREFIX_FUNCTION;
    @BeforeAll static void bootstrap() throws Exception {
        DataConsumerRecipeTest.bootstrap();SatelliteTypeRegistry.registerSpaceExtensions();
        if(!DimensionManager.isDimensionRegistered(REMOTE))DimensionManager.registerDimension(REMOTE,net.minecraft.world.DimensionType.OVERWORLD);
        GameRegistry.registerTileEntity(Link.class,new ResourceLocation("hbm","test_satlink"));
    }
    private static SatelliteOperationContext context(TestWorld world){return SatelliteOperationContext.create(world,new SatelliteHandle(OrbitKey.dimension(REMOTE),FREQ));}
    private static Link link(TestWorld world){Link link=new Link();link.setWorld(world);link.setPos(new BlockPos(0,64,0));link.setFrequency(FREQ);return link;}
    private static ItemStack chip(OrbitKey orbit){ItemStack chip=new ItemStack(ModItems.sat_chip);ISatChip.setFreqS(chip,FREQ);ISatChip.setOrbitKeyS(chip,orbit);return chip;}
    private static String key(ITextComponent line){return ((TextComponentTranslation)line).getKey();}
    private static boolean has(ITextComponent[] lines,String key){for(ITextComponent line:lines)if(line instanceof TextComponentTranslation&&key.equals(key(line)))return true;return false;}
    private static Object[] params(ITextComponent[] lines,String key){for(ITextComponent line:lines)if(line instanceof TextComponentTranslation&&key.equals(key(line)))return ((TextComponentTranslation)line).getFormatArgs();throw new AssertionError(key);}
    private static Arguments args(final Object...values){return (Arguments)Proxy.newProxyInstance(Arguments.class.getClassLoader(),new Class<?>[]{Arguments.class},(p,m,a)->{if(m.getName().equals("checkInteger"))return ((Number)values[(Integer)a[0]]).intValue();if(m.getName().equals("checkString"))return values[(Integer)a[0]];return null;});}
    @Test void telemetryTypesPreservePinnedNamesAndOwnDysonAndScienceIds(){
        String[] types={"NOT_A_SPY_SATELLITE_:)","DEPTH_SCANNER","LEO_RADAR","ORBITAL_FUN_PLATFORM_:)","XEN_RELAY","DIMENSIONAL_RELAY","ASTEROID_MINER","LUNAR_MINER","PAYLOAD_UNKNOWN","ORBITAL_TATOO_REMOVER","UWB_EMISSION_DETECTOR","NB_RAY_SCANNER","DYSON_RELAY","SCIENCE_PROBE"};
        for(int i=0;i<types.length;i++){Satellite s=SatelliteTypeRegistry.createByLegacyId(i);assertEquals(i,s.getID());assertEquals(types[i],s.getType());}
    }
    @Test void everyRegisteredSatelliteUsesItsCanonicalLocalizedItemName(){
        TestWorld world=new TestWorld(false);for(SatelliteTypeRegistry.Descriptor d:SatelliteTypeRegistry.descriptors()){
            // Space item initialization needs mixins; those descriptors are checked in the native runtime.
            if(d.create().getID() >= 12) continue;
            Satellite s=d.create();ITextComponent[] info=s.getInfo(context(world));assertTrue(info.length>0);assertEquals(d.getCanonicalStack().getTranslationKey()+".name",key(info[0]));for(ITextComponent line:info)assertNotNull(line);
        }
    }
    @Test void laboratoryInfoIsReadOnlyAndContainsRealCountersQueueAndCargo(){
        TestWorld world=new TestWorld(false);world.time=100;SatelliteScience s=new SatelliteScience();s.sensorCount=3;s.sensorProgress=123;s.assemblerCount=2;s.assemblerProgress=.375;s.assemblerTasks.add("space.crystalcircuit");s.requestableSlots=new ItemStack[]{new ItemStack(ModItems.circuit,2,20)};s.produceData(EnumDriveType.DISK_EMPTY,EnumDriveType.DISK_FLIGHTDATA);
        ITextComponent[] info=s.getInfo(context(world));assertEquals(9,info.length);assertEquals("3",params(info,"satellite.sensors")[0].toString());assertEquals("2",params(info,"satellite.assemblers")[0].toString());assertEquals("38%",params(info,"satellite.progress")[0]);assertEquals("1",params(info,"satellite.queue")[0].toString());assertTrue(has(info,"satellite.data"));assertTrue(has(info,"satellite.cargo"));
        assertEquals(123,s.sensorProgress);assertEquals(.375,s.assemblerProgress);assertEquals(0,s.lastScience);assertEquals(2,s.requestableSlots[0].getCount());assertEquals(EnumDriveType.DISK_FLIGHTDATA,s.driveOutput);
    }
    @Test void emptyLaboratoryInfoDoesNotInventModulesWorkOrFlightData(){
        TestWorld world=new TestWorld(false);world.time=20000;SatelliteScience s=new SatelliteScience();ITextComponent[] info=s.getInfo(context(world));assertEquals(2,info.length);assertTrue(has(info,"satellite.ready"));assertNull(s.driveOutput);assertEquals(0,s.lastScience);assertEquals(0,s.sensorProgress);
    }
    @Test void laserTelemetryRefusesToInventAvailabilityForUnloadedSurface(){
        TestWorld world=new TestWorld(false);for(Satellite s:new Satellite[]{new SatelliteLaser(),new SatellitePrecisionLaser()}){ITextComponent[] info=s.getInfo(context(world));assertTrue(has(info,"satellite.surface_unloaded"));assertFalse(has(info,"satellite.ready"));}
        assertNull(DimensionManager.getWorld(REMOTE,false));
    }
    @Test void minerTelemetryUsesOwnDockCooldownInsteadOfFakeMiningProgress(){
        SatelliteMiner s=new SatelliteMiner();s.lastOp=System.currentTimeMillis();ITextComponent[] info=s.getInfo(context(new TestWorld(false)));assertTrue(has(info,"satellite.cooldown"));assertFalse(has(info,"satellite.minerprogress"));s.lastOp=0;assertTrue(has(s.getInfo(context(new TestWorld(false))),"satellite.ready"));
    }
    @Test void horizonsTelemetryReflectsPersistedSingleUseFlag(){
        SatelliteHorizons s=new SatelliteHorizons();assertTrue(has(s.getInfo(context(new TestWorld(false))),"satellite.ready"));NBTTagCompound tag=new NBTTagCompound();tag.setBoolean("used",true);s.readFromNBT(tag);assertTrue(has(s.getInfo(context(new TestWorld(false))),"satellite.spent"));
    }
    @Test void connectedStationPublishesInfoAndObstructionClearsIt(){
        TestWorld world=new TestWorld(false);Link link=link(world);link.getSatelliteData().putSatellite(FREQ,new SatelliteScience());link.update();assertTrue(link.connected);assertEquals(2,link.info.length);world.height=100;link.update();assertFalse(link.connected);assertEquals(0,link.info.length);assertEquals(2,link.packs);
    }
    @Test void removingTheSatelliteClearsPreviousStationTelemetry(){
        Link link=link(new TestWorld(false));link.getSatelliteData().putSatellite(FREQ,new SatelliteMapper());link.update();assertEquals(1,link.info.length);link.getSatelliteData().removeSatellite(FREQ);link.update();assertFalse(link.connected);assertEquals(0,link.info.length);
    }
    @Test void stationReadsDoNotProduceScienceDataOrAdvanceAssembly(){
        TestWorld world=new TestWorld(false);world.time=20000;Link link=link(world);SatelliteScience s=new SatelliteScience();s.sensorCount=1;s.assemblerCount=1;s.assemblerTasks.add("space.crystalcircuit");link.getSatelliteData().putSatellite(FREQ,s);link.update();assertEquals(0,s.sensorProgress);assertEquals(0,s.assemblerProgress);assertNull(s.driveOutput);assertEquals(0,s.lastScience);
    }
    @Test void synchronizedTextRetainsTranslationKeysAndArguments(){
        Link source=new Link();source.setFrequency(FREQ);source.connected=true;source.info=new ITextComponent[]{new TextComponentTranslation("satellite.sensors",3),new TextComponentTranslation("satellite.progress","38%")};ByteBuf buf=Unpooled.buffer();try{source.serialize(buf);Link copy=new Link();copy.deserialize(buf);assertTrue(copy.connected);assertEquals(FREQ,copy.getFrequency());assertEquals(2,copy.info.length);assertEquals("satellite.sensors",key(copy.info[0]));assertEquals("3",((TextComponentTranslation)copy.info[0]).getFormatArgs()[0].toString());assertEquals("38%",((TextComponentTranslation)copy.info[1]).getFormatArgs()[0].toString());assertEquals(0,buf.readableBytes());}finally{buf.release();}
    }
    @Test void disconnectedPacketReplacesOldInfoWithEmptySnapshot(){
        Link source=new Link(),copy=new Link();copy.info=new ITextComponent[]{new TextComponentTranslation("satellite.data")};ByteBuf buf=Unpooled.buffer();try{source.serialize(buf);copy.deserialize(buf);assertFalse(copy.connected);assertEquals(0,copy.info.length);assertEquals(0,buf.readableBytes());}finally{buf.release();}
    }
    @Test void malformedPacketCannotAllocateUnboundedInfoArrays(){
        for(int invalid:new int[]{-1,17,Integer.MAX_VALUE}){ByteBuf buf=Unpooled.buffer();try{new Link().serialize(buf);buf.setInt(7,invalid);assertThrows(IllegalArgumentException.class,()->new Link().deserialize(buf));}finally{buf.release();}}
    }
    @Test void frequencyChangeClampsAndDropsStaleConnectedInfo(){
        Link link=new Link();link.connected=true;link.info=new ITextComponent[]{new TextComponentTranslation("satellite.data")};link.setFrequency(100001);assertEquals(100000,link.getFrequency());assertFalse(link.connected);assertEquals(0,link.info.length);link.setFrequency(-3);assertEquals(0,link.getFrequency());
    }
    @Test void chipBindingAndLegacyNbtPreserveExplicitOrbitAndFrequency(){
        TestWorld world=new TestWorld(false);Link source=link(world);source.setLink(chip(OrbitKey.dimension(REMOTE)));NBTTagCompound tag=source.writeToNBT(new NBTTagCompound());assertEquals(FREQ,tag.getInteger("freq"));assertEquals("dim:"+REMOTE,tag.getString("orbitKey"));Link restored=link(world);restored.readFromNBT(tag);assertEquals(OrbitKey.dimension(REMOTE),restored.getSatelliteData().getOrbitKey());assertEquals(FREQ,restored.getFrequency());NBTTagCompound legacy=new NBTTagCompound();legacy.setInteger("freq",42);restored.readFromNBT(legacy);assertEquals(42,restored.getFrequency());assertEquals(SatelliteSavedData.getData(world).getOrbitKey(),restored.getSatelliteData().getOrbitKey());
    }
    @Test void rorExposesOriginalTypeAndTxrxWithoutLosingOldFunctions(){
        Link link=new Link();String all=String.join(";",link.getFunctionInfo());for(String name:new String[]{"connected","freq","rx","type","setfreq","tx","txrx"})assertTrue(all.contains(name));assertEquals(7,link.getFunctionInfo().length);
    }
    @Test void sameFrequencyRemoteCommandsAndReplyStayInTheLinkedNamespace(){
        TestWorld world=new TestWorld(false);Link link=link(world);SatelliteMapper local=new SatelliteMapper(),remote=new SatelliteMapper();local.setTarget(1,2);remote.setTarget(7,8);SatelliteSavedData localData=link.getSatelliteData(),remoteData=SatelliteSavedData.getDataForOrbit(world,OrbitKey.dimension(REMOTE));localData.putSatellite(FREQ,local);remoteData.putSatellite(FREQ,remote);link.setLink(chip(OrbitKey.dimension(REMOTE)));String channel="s01_remote";RTTYSystem.newMessages.clear();link.runRORFunction(FUNCTION+"txrx",new String[]{channel,"gettarget"});assertEquals("7;8",RTTYSystem.newMessages.get(new Pair<World,String>(world,channel)));assertEquals("",local.getTransmission());assertEquals("7;8",remote.getTransmission());assertNull(DimensionManager.getWorld(REMOTE,false));
    }
    @Test void remoteTargetCoordinatesCanChangeWithoutCallingWorldEffects(){
        TestWorld world=new TestWorld(false);Link link=link(world);ProbeSatellite s=new ProbeSatellite();SatelliteSavedData data=SatelliteSavedData.getDataForOrbit(world,OrbitKey.dimension(REMOTE));data.putSatellite(FREQ,s);link.setLink(chip(OrbitKey.dimension(REMOTE)));long revision=data.getRevision();assertTrue(link.sendCommand("  settarget   12   -34  "));assertEquals(12,s.getTargetX());assertEquals(-34,s.getTargetZ());assertEquals(0,s.effects);assertTrue(data.getRevision()>revision);assertTrue(link.sendCommand("gettarget"));assertEquals("12;-34",s.getTransmission());
    }
    @Test void unknownRemoteActionsCannotRunOnStationWorldOrBroadcastStaleReply(){
        TestWorld world=new TestWorld(false);Link link=link(world);ProbeSatellite s=new ProbeSatellite();s.onCommand(world,"gettarget");int before=s.effects;SatelliteSavedData.getDataForOrbit(world,OrbitKey.dimension(REMOTE)).putSatellite(FREQ,s);link.setLink(chip(OrbitKey.dimension(REMOTE)));RTTYSystem.newMessages.clear();assertFalse(link.sendCommand("fire"));link.runRORFunction(FUNCTION+"txrx",new String[]{"must_stay_empty","fire"});assertEquals(before,s.effects);assertFalse(RTTYSystem.newMessages.containsKey(new Pair<World,String>(world,"must_stay_empty")));
    }
    @Test void missingSatelliteEmptyCommandsAndIncompleteTxrxProduceNoReply(){
        Link link=link(new TestWorld(false));RTTYSystem.newMessages.clear();assertFalse(link.sendCommand(null));assertFalse(link.sendCommand(" "));assertFalse(link.sendCommand("gettarget"));link.runRORFunction(FUNCTION+"txrx",new String[]{"incomplete"});assertTrue(RTTYSystem.newMessages.isEmpty());assertEquals("",link.provideRORValue(VALUE+"type"));assertEquals("",link.provideRORValue(VALUE+"rx"));
    }
    @Test void rorSetfreqKeepsExistingLocalResetSemantics(){
        TestWorld world=new TestWorld(false);Link link=link(world);link.setLink(chip(OrbitKey.dimension(REMOTE)));assertThrows(com.hbm.api.redstoneoverradio.RORFunctionException.class,()->link.runRORFunction(FUNCTION+"setfreq",new String[]{"100001"}));link.runRORFunction(FUNCTION+"setfreq",new String[]{"100000"});assertEquals(100000,link.getFrequency());assertEquals(SatelliteSavedData.getData(world).getOrbitKey(),link.getSatelliteData().getOrbitKey());
    }
    @Test void clientCommandPathsCannotModifySatelliteTargets(){
        TestWorld client=new TestWorld(true);ProbeSatellite s=new ProbeSatellite();assertFalse(s.sendCommand(context(client),"settarget","4","5"));assertEquals(0,s.getTargetX());assertFalse(link(client).sendCommand("settarget 4 5"));
    }
    @Test void ocCallbacksAndProxyDispatchExposeTheSixOriginalOperations() throws Exception{
        Link link=new Link();assertEquals("ntm_satlink",link.getComponentName());assertArrayEquals(new String[]{"isConnected","setFreq","getFreq","getType","send","read"},link.methods());link.invoke("setFreq",null,args(100001));assertEquals(100000,link.invoke("getFreq",null,args())[0]);assertEquals(false,link.invoke("isConnected",null,args())[0]);assertEquals("",link.invoke("getType",null,args())[0]);assertEquals("",link.invoke("read",null,args())[0]);assertThrows(NoSuchMethodException.class,()->link.invoke("missing",null,args()));
    }
    @Test void ocFrequencyChangesRetainChipBodyClampAndMarkStateDirty() throws Exception{
        TestWorld world=new TestWorld(false);Link link=link(world);link.setLink(chip(OrbitKey.dimension(REMOTE)));int dirty=link.dirty;link.setFreq(null,args(73));assertEquals(73,link.getFrequency());assertEquals(OrbitKey.dimension(REMOTE),link.getSatelliteData().getOrbitKey());assertTrue(link.dirty>dirty);
    }
    @Test void ocSendUsesScopedCommandsAndReportsUnavailableActions() throws Exception{
        TestWorld world=new TestWorld(false);Link link=link(world);SatelliteMapper s=new SatelliteMapper();SatelliteSavedData.getDataForOrbit(world,OrbitKey.dimension(REMOTE)).putSatellite(FREQ,s);link.setLink(chip(OrbitKey.dimension(REMOTE)));link.invoke("send",null,args("settarget 9 10"));link.invoke("send",null,args("gettarget"));assertEquals("9;10",link.invoke("read",null,args())[0]);assertEquals("NOT_A_SPY_SATELLITE_:)",link.invoke("getType",null,args())[0]);assertThrows(IllegalStateException.class,()->link.send(null,args("fire")));assertFalse(TileEntityMachineSatLink.class.getMethod("send",li.cil.oc.api.machine.Context.class,Arguments.class).getAnnotation(Callback.class).direct());
    }
    public static class Link extends TileEntityMachineSatLink {int packs,dirty;@Override public void networkPackNT(int range){packs++;}@Override public void markDirty(){dirty++;}}
    private static class ProbeSatellite extends Satellite {int effects;@Override protected void onCommandImpl(World world,String...commands){effects++;}@Override public float[] getColor(){return new float[]{1,1,1};}}
    private static class TestWorld extends World {
        long time;int height;private final MapStorage storage=new MapStorage(null);
        TestWorld(boolean client){super(new SaveHandlerMP(),new WorldInfo(new NBTTagCompound()),new WorldProviderSurface(),new Profiler(),client);}
        @Override public long getTotalWorldTime(){return time;}@Override public int getHeight(int x,int z){return height;}
        @Override public MapStorage getPerWorldStorage(){return storage;}@Override protected IChunkProvider createChunkProvider(){return null;}@Override protected boolean isChunkLoaded(int x,int z,boolean allowEmpty){return true;}
    }
}
