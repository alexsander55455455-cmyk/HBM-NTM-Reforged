package com.hbm.inventory;

import com.hbm.blocks.ModBlocks;
import com.hbm.config.ServerConfig;
import com.hbm.module.IParse;
import com.hbm.module.IParse.ParseContext;
import com.hbm.module.ParseMSES1Ext1;
import com.hbm.tileentity.network.RTTYSystem;
import com.hbm.tileentity.network.TileEntityRadioAUTOCAL;
import com.hbm.util.Tuple.Pair;
import io.netty.buffer.Unpooled;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.profiler.Profiler;
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
import java.net.URL;
import static com.hbm.module.IParse.EnumStatementReturn.*;
import static org.junit.jupiter.api.Assertions.*;

class AutocalTest {
    final IParse parser = new ParseMSES1Ext1();
    ParseContext ctx;
    TestWorld world;

    @BeforeAll static void bootstrap() throws Exception {
        if(Launch.classLoader == null) Launch.classLoader = new LaunchClassLoader(new URL[0]);
        AutoCrystallizerOreDictionaryTest.bootstrap();
        net.minecraftforge.fml.common.registry.GameRegistry.registerTileEntity(Autocal.class, new net.minecraft.util.ResourceLocation("hbm", "test_autocal"));
    }
    @BeforeEach void reset() {
        ServerConfig.AUTOCAL_MAX_CLOCK.set(20);
        ServerConfig.MAX_BUFFER_LENGTH.set(256);
        ServerConfig.MAX_STACK_SIZE.set(256);
        RTTYSystem.broadcast.clear(); RTTYSystem.newMessages.clear();
        world = new TestWorld(); ctx = new ParseContext(world);
    }
    void eval(String line) { assertEquals(OK, parser.eval(ctx, line), line); }
    Autocal tile(String script) {
        Autocal tile = new Autocal(); tile.setWorld(world); tile.setPos(new BlockPos(0,64,0));
        tile.setScript(script); tile.isOn = true; return tile;
    }
    void signal(String channel, String value, long timestamp) {
        RTTYSystem.RTTYChannel signal = new RTTYSystem.RTTYChannel(); signal.signal=value; signal.timeStamp=timestamp;
        RTTYSystem.broadcast.put(new Pair<World,String>(world,channel),signal);
    }
    @Test void namedVariablesAndBufferSubstitutionCalculate() {
        eval("buffer 4"); eval("save a"); eval("eval $a$ + $buffer$ * 2");
        assertEquals("12.0",ctx.readBuffer());
        eval("evalr $missing$ + 3.6"); assertEquals("4",ctx.readBuffer());
    }
    @Test void arithmeticPrecedenceFunctionsAndPowersUseOriginalCalculator() {
        eval("eval (2 + 3) * 4"); assertEquals("20.0",ctx.readBuffer());
        eval("eval sqrt(81)"); assertEquals("9.0",ctx.readBuffer());
        eval("eval 2^3"); assertEquals("8.0",ctx.readBuffer());
    }
    @Test void roundedInstructionsAndAliases() {
        for(String[] test : new String[][]{{"floor","-2.3","-3"},{"rounddown","2.9","2"},{"ceil","2.1","3"},{"roundup","-2.9","-2"},{"round","2.6","3"},{"nearest","2.4","2"}}) {
            eval("buffer "+test[1]); eval(test[0]); assertEquals(test[2],ctx.readBuffer());
        }
    }
    @Test void comparisonsSubstituteVariables() {
        ctx.variables.setString("threshold","5");
        for(String command:new String[]{"gtb","ltb","geb","leb","eq"}) {
            eval("buffer 5"); eval(command+" $threshold$");
            assertEquals(command.equals("gtb")||command.equals("ltb")?"false":"true",ctx.readBuffer());
        }
    }
    @Test void firstLastAndSplitSubstituteTheirParameters() {
        ctx.variables.setString("n","2");
        eval("buffer abcd"); eval("first $n$"); assertEquals("ab",ctx.readBuffer());
        eval("buffer abcd"); eval("last $n$"); assertEquals("cd",ctx.readBuffer());
        eval("buffer first;second;third"); eval("split $n$"); assertEquals("second",ctx.readBuffer());
    }
    @Test void literalSplitterAndLength() {
        eval("splitter ."); eval("buffer a.b.c"); eval("splitcount"); assertEquals("3",ctx.readBuffer());
        eval("buffer a.b.c"); eval("split 2"); assertEquals("b",ctx.readBuffer());
        eval("length"); assertEquals("1",ctx.readBuffer());
    }
    @Test void substringClampsToLengthAndRejectsMalformedValues() {
        eval("buffer abc"); eval("first 999"); assertEquals("abc",ctx.readBuffer());
        eval("last 0"); assertEquals("",ctx.readBuffer());
        assertEquals(PARAMETER_PARSE_ERROR,parser.eval(ctx,"first nope"));
        assertEquals(PARAMETER_OOB,parser.eval(ctx,"split 0"));
        assertEquals(PARAMETER_OOB,parser.eval(ctx,"split 3"));
    }
    @Test void stackIsLastInFirstOutAndPeekDoesNotRemove() {
        eval("push first"); eval("buffer second"); eval("push");
        eval("peek"); assertEquals("second",ctx.readBuffer());
        eval("pop"); assertEquals("second",ctx.readBuffer());
        eval("pop"); assertEquals("first",ctx.readBuffer());
        assertEquals(UNDEFINED,parser.eval(ctx,"pop"));
    }
    @Test void stackCapacityIsPerUnitAndBufferLimitUpdates() {
        ServerConfig.MAX_STACK_SIZE.set(1); ctx=new ParseContext(world);
        eval("push one"); assertEquals(STACK_EXCEEDED,parser.eval(ctx,"push two"));
        ServerConfig.MAX_STACK_SIZE.set(2);
        assertEquals(STACK_EXCEEDED,parser.eval(ctx,"push two"));
        ParseContext next=new ParseContext(world); assertTrue(next.push("one")); assertTrue(next.push("two"));
        ServerConfig.MAX_BUFFER_LENGTH.set(3);
        assertFalse(ctx.writeBuffer("abcdef")); assertEquals("abc",ctx.readBuffer());
        assertEquals("one",ctx.pop()); assertTrue(ctx.push("abcdef")); assertEquals("abc",ctx.pop());
    }
    @Test void bufferStackVariablesAndInstructionStateSurviveNBT() {
        eval("buffer saved"); eval("save named"); eval("push old"); eval("push recent");
        ctx.current=3; ctx.clockSpeed=7; ctx.splitString="|";
        NBTTagCompound nbt=new NBTTagCompound(); ctx.writeToNBT(nbt);
        ParseContext loaded=new ParseContext(world); loaded.readFromNBT(nbt,new String[]{"dest again"},parser);
        assertEquals("saved",loaded.readBuffer()); assertEquals("saved",loaded.variables.getString("named"));
        assertEquals(3,loaded.current); assertEquals(7,loaded.clockSpeed); assertEquals("|",loaded.splitString);
        assertEquals("recent",loaded.pop()); assertEquals("old",loaded.pop()); assertEquals(Integer.valueOf(0),loaded.jmp.get("again"));
    }
    @Test void changedLimitsAndMissingNBTDoNotCorruptStack() {
        ctx.push("one"); ctx.push("two"); NBTTagCompound nbt=new NBTTagCompound(); ctx.writeToNBT(nbt);
        ServerConfig.MAX_STACK_SIZE.set(1); ParseContext loaded=new ParseContext(world);
        loaded.readFromNBT(nbt,new String[0],parser); assertEquals("one",loaded.pop()); assertNull(loaded.pop());
        loaded.readFromNBT(new NBTTagCompound(),new String[0],parser);
        assertEquals(1,loaded.clockSpeed); assertEquals(";",loaded.splitString);
    }
    @Test void stopResetsVariablesBufferClockAndProgramCounter() {
        ctx.variables.setString("v","a"); ctx.writeBuffer("value"); ctx.current=30; ctx.clockSpeed=20;
        ctx.turnOff(); assertTrue(ctx.variables.isEmpty()); assertEquals("",ctx.readBuffer()); assertEquals(0,ctx.current); assertEquals(1,ctx.clockSpeed);
    }
    @Test void commentsDestinationsAndClocksDoNotConsumeCycles() {
        assertEquals(SKIP,parser.eval(ctx,"# comment")); assertEquals(SKIP,parser.eval(ctx,""));
        assertEquals(SKIP,parser.eval(ctx,"dest here")); assertEquals(SKIP,parser.eval(ctx,"clockspeed 20"));
        assertEquals(20,ctx.clockSpeed); assertEquals(PARAMETER_OOB,parser.eval(ctx,"clockspeed 21"));
        assertEquals(PARAMETER_PARSE_ERROR,parser.eval(ctx,"clockspeed fast"));
    }
    @Test void jumpAndConditionalJumps() {
        parser.generateJumpPoints(ctx,"dest target",12); ctx.variables.setString("dest","target");
        eval("jmp $dest$"); assertEquals(12,ctx.current);
        ctx.current=0; eval("buffer false"); eval("jmpif target"); assertEquals(0,ctx.current);
        eval("jmpnot target"); assertEquals(12,ctx.current);
        assertEquals(NO_DESTINATION,parser.eval(ctx,"jmp missing"));
    }
    @Test void listenWorksWithEmptyBufferAndPollIgnoresStaleSignals() {
        signal("input","42",100); eval("listen input"); assertEquals("42",ctx.readBuffer());
        eval("buffer unchanged"); signal("input","old",98); eval("poll input"); assertEquals("unchanged",ctx.readBuffer());
        signal("input","fresh",99); eval("poll input"); assertEquals("fresh",ctx.readBuffer());
    }
    @Test void sendAndWorldtimeUseRealRadioQueue() {
        ctx.variables.setString("out","result"); eval("buffer 123"); eval("send $out$");
        assertEquals("123",RTTYSystem.newMessages.get(new Pair<World,String>(world,"result")));
        eval("worldtime"); assertEquals("100",ctx.readBuffer());
    }
    @Test void twoRunningUnitsExchangeCounterThroughActualTickQueue() {
        Autocal sender=tile("clockspeed 20\ndest loop\nevalr ($count$ + 1) % 16\nsave count\nsend autocal_test\nendtick\njmp loop");
        Autocal receiver=tile("clockspeed 20\ndest loop\npoll autocal_test\nendtick\njmp loop");
        for(int tick=1;tick<=50;tick++) {
            RTTYSystem.updateBroadcastQueue(); sender.update(); receiver.update();
            assertTrue(sender.isOn); assertTrue(receiver.isOn);
            assertEquals(Integer.toString(tick%16),sender.ctx.readBuffer());
            assertEquals(tick==1?"":Integer.toString((tick-1)%16),receiver.ctx.readBuffer());
        }
    }
    @Test void allDetailedErrorCategoriesAreExposed() {
        assertEquals(UNRECOGNIZED_COMMAND,parser.eval(ctx,"unknown"));
        assertEquals(PARAMETER_MISSING,parser.eval(ctx,"buffer "));
        assertEquals(BUFFER_EMPTY,parser.eval(ctx,"save a"));
        assertEquals(ARITHMETIC_EXCEPTION,parser.eval(ctx,"eval ("));
        assertEquals(END_TICK,parser.eval(ctx,"endtick")); assertEquals(SHUTDOWN,parser.eval(ctx,"shutdown"));
    }
    @Test void uploadTrimsScriptAndRebuildsJumpMapAndStopsRunningUnit() {
        Autocal tile=tile("dest old\nbuffer x"); tile.ctx.writeBuffer("old");
        tile.setScript("  dest new  \n buffer new ");
        assertFalse(tile.isOn); assertFalse(tile.ctx.jmp.containsKey("old")); assertEquals(Integer.valueOf(0),tile.ctx.jmp.get("new"));
        assertEquals("buffer new",tile.script[1]); assertEquals("",tile.ctx.readBuffer());
    }
    @Test void errorStopsAtOnceWithoutRunningFollowingLine() {
        Autocal tile=tile("clockspeed 20\nunknown\nbuffer must_not_run"); tile.update();
        assertFalse(tile.isOn); assertEquals("",tile.ctx.readBuffer());
        assertTrue(tile.history[5].contains("Unrecognized"));
    }
    @Test void ignoredErrorSetsLastErrorAndContinues() {
        Autocal tile=tile("clockspeed 20\nunknown\nload last_error\nendtick\nnop"); tile.ignoreError=true; tile.update();
        assertTrue(tile.isOn); assertEquals("unrecognized_command",tile.ctx.readBuffer()); assertEquals(4,tile.ctx.current);
    }
    @Test void endtickAndShutdownStopExecutionWithinTick() {
        Autocal tile=tile("clockspeed 20\nbuffer before\nendtick\nbuffer after\nshutdown\nbuffer never");
        tile.update(); assertEquals("before",tile.ctx.readBuffer()); assertTrue(tile.isOn);
        tile.update(); assertFalse(tile.isOn); assertEquals("",tile.ctx.readBuffer());
    }
    @Test void skippingInstructionsStillHasOneHundredStepBrake() {
        StringBuilder script=new StringBuilder(); for(int i=0;i<150;i++)script.append("# skip\n"); script.append("buffer done");
        Autocal tile=tile(script.toString()); tile.update(); assertEquals(100,tile.ctx.current); assertTrue(tile.isOn);
        tile.update(); assertEquals("done",tile.ctx.readBuffer());
    }
    @Test void exceptionStopsWithoutContinuingNextInstruction() {
        Autocal tile=tile("buffer never");
        tile.msesv1ext=new ParseMSES1Ext1(){ @Override public EnumStatementReturn eval(ParseContext ctx,String line){ throw new IllegalArgumentException(); } };
        tile.ctx.clockSpeed=20; tile.update(); assertFalse(tile.isOn); assertEquals(0,tile.ctx.current);
    }
    @Test void autoRebootStartsAtBeginningOnNextTick() {
        Autocal tile=tile("buffer started\nendtick\nshutdown"); tile.autoReboot=true;
        tile.update(); assertEquals("started",tile.ctx.readBuffer()); tile.update(); tile.update(); assertFalse(tile.isOn);
        tile.update(); assertTrue(tile.isOn); assertEquals("started",tile.ctx.readBuffer());
    }
    @Test void tilePersistenceRestoresProgramFlagsAndContinues() {
        Autocal tile=tile("buffer before\nendtick\nbuffer after\nendtick\nnop"); tile.ignoreError=true; tile.autoReboot=true;
        tile.ctx.clockSpeed=20; tile.update(); NBTTagCompound nbt=tile.writeToNBT(new NBTTagCompound());
        Autocal loaded=tile("unused"); loaded.readFromNBT(nbt);
        assertTrue(loaded.ignoreError); assertTrue(loaded.autoReboot); assertTrue(loaded.isOn); assertEquals(2,loaded.ctx.current);
        loaded.update(); assertEquals("after",loaded.ctx.readBuffer()); assertEquals(4,loaded.ctx.current);
    }
    @Test void freshTileCanBeSavedBeforeFirstTick() {
        NBTTagCompound nbt=new Autocal().writeToNBT(new NBTTagCompound());
        assertEquals(1,nbt.getInteger("clockSpeed")); assertEquals(0,nbt.getTagList("script",8).tagCount());
    }
    @Test void clientSyncKeepsFlagsAndAllHistoryLines() {
        Autocal tile=tile("nop"); tile.ignoreError=true; tile.autoReboot=true;
        for(int i=0;i<tile.history.length;i++)tile.history[i]="line"+i;
        ByteBuf buf=Unpooled.buffer(); try {
            tile.serialize(buf); Autocal client=new Autocal(); client.deserialize(buf);
            assertTrue(client.isOn); assertTrue(client.ignoreError); assertTrue(client.autoReboot); assertArrayEquals(tile.history,client.history);
            assertEquals(0,buf.readableBytes());
        } finally { buf.release(); }
    }
    @Test void controlsToggleAndUploadBeforeAnyTick() {
        Autocal tile=tile("nop"); tile.isOn=false;
        NBTTagCompound nbt=new NBTTagCompound(); nbt.setBoolean("ignore",true); nbt.setBoolean("auto",true); nbt.setString("payload","buffer new");
        tile.receiveControl(nbt); assertTrue(tile.ignoreError); assertTrue(tile.autoReboot); assertEquals("buffer new",tile.script[0]);
        nbt=new NBTTagCompound(); nbt.setBoolean("on",true); tile.receiveControl(nbt); assertTrue(tile.isOn);
        tile.receiveControl(nbt); assertFalse(tile.isOn);
    }
    @Test void optionalOpenComputersCallbacksUseSameProgramAndLimits() {
        Autocal tile=tile("nop"); tile.isOn=false;
        li.cil.oc.api.machine.Arguments args=(li.cil.oc.api.machine.Arguments)Proxy.newProxyInstance(
                getClass().getClassLoader(),new Class<?>[]{li.cil.oc.api.machine.Arguments.class},
                (p,m,a)->m.getName().equals("checkString")?"buffer oc\nendtick":m.getName().equals("checkBoolean")?true:0);
        tile.setScript(null,args); assertEquals("buffer oc",tile.script[0]);
        assertEquals("buffer oc\nendtick\n",tile.getScript(null,args)[0]);
        tile.setIgnoreError(null,args); tile.setAutoReboot(null,args); tile.setState(null,args);
        assertEquals(true,tile.getState(null,args)[0]); assertEquals(true,tile.getIgnoreError(null,args)[0]);
        assertEquals(true,tile.getAutoReboot(null,args)[0]); assertEquals("radio_autocal",tile.getComponentName());
        ServerConfig.MAX_BUFFER_LENGTH.set(3); assertEquals(false,tile.setBuffer(null,args)[0]);
        assertEquals("buf",tile.getBuffer(null,args)[0]);
    }
    @Test void forgeServerTransformerRemovesClientGuiDependency() throws Exception {
        java.lang.reflect.Field launchSide=net.minecraftforge.fml.relauncher.FMLLaunchHandler.class.getDeclaredField("side");
        launchSide.setAccessible(true); Object previousLaunchSide=launchSide.get(null);
        launchSide.set(null,net.minecraftforge.fml.relauncher.Side.SERVER);
        java.lang.reflect.Field side=net.minecraftforge.fml.common.asm.transformers.SideTransformer.class.getDeclaredField("SIDE");
        side.setAccessible(true); Object previous=side.get(null); side.set(null,"SERVER");
        try {
            String name="com.hbm.tileentity.network.TileEntityRadioAUTOCAL";
            java.io.InputStream input=getClass().getClassLoader().getResourceAsStream(name.replace('.','/')+".class");
            java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream(); byte[] block=new byte[4096]; int count;
            try {while((count=input.read(block))!=-1)bytes.write(block,0,count);} finally {input.close();}
            final byte[] transformed=new net.minecraftforge.fml.common.asm.transformers.SideTransformer().transform(name,name,bytes.toByteArray());
            ClassLoader serverLoader=new ClassLoader(getClass().getClassLoader()) {
                @Override protected Class<?> loadClass(String requested,boolean resolve) throws ClassNotFoundException {
                    if(requested.startsWith("net.minecraft.client.")||requested.startsWith("com.hbm.inventory.gui."))throw new ClassNotFoundException(requested);
                    if(requested.equals(name)){Class<?> found=findLoadedClass(requested);return found!=null?found:defineClass(requested,transformed,0,transformed.length);}
                    return super.loadClass(requested,resolve);
                }
            };
            Class<?> type=serverLoader.loadClass(name); assertNotNull(type.newInstance());
            for(java.lang.reflect.Method method:type.getDeclaredMethods())assertNotEquals("provideGUI",method.getName());
        } finally {side.set(null,previous);launchSide.set(null,previousLaunchSide);}
    }
    @Test void blockCreatesCoreInAllFourOrientationsAndTwoBlockFootprint() {
        assertArrayEquals(new int[]{1,0,0,0,0,0},((com.hbm.blocks.BlockDummyable)ModBlocks.radio_autocal).getDimensions());
        for(int meta=0;meta<16;meta++) {
            if(meta>=12)assertTrue(((net.minecraft.block.BlockContainer)ModBlocks.radio_autocal).createNewTileEntity(null,meta) instanceof TileEntityRadioAUTOCAL);
            else assertNull(((net.minecraft.block.BlockContainer)ModBlocks.radio_autocal).createNewTileEntity(null,meta));
        }
    }
    public static class Autocal extends TileEntityRadioAUTOCAL {
        @Override public void markChanged() {}
        @Override public void networkPackNT(int range) {}
        @Override public void markDirty() {}
    }
    private static class TestWorld extends World {
        TestWorld(){super(new SaveHandlerMP(),new WorldInfo(new NBTTagCompound()),new WorldProviderSurface(),new Profiler(),false);}
        @Override public long getTotalWorldTime(){return 100;}
        @Override protected IChunkProvider createChunkProvider(){return (IChunkProvider)Proxy.newProxyInstance(IChunkProvider.class.getClassLoader(),new Class<?>[]{IChunkProvider.class},(p,m,a)->null);}
        @Override protected boolean isChunkLoaded(int x,int z,boolean allowEmpty){return true;}
        @Override public net.minecraft.entity.Entity getEntityByID(int id){return null;}
        @Override public IBlockState getBlockState(BlockPos pos){return Blocks.AIR.getDefaultState();}
    }
}
