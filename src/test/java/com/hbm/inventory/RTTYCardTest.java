package com.hbm.inventory;

import com.hbm.items.machine.ItemRTTYCard;
import com.hbm.items.machine.ItemRTTYCard.Environment;
import com.hbm.items.machine.ItemRTTYCard.RTTYChanHandle;
import com.hbm.tileentity.network.RTTYSystem;
import com.hbm.util.Tuple.Pair;
import li.cil.oc.api.API;
import li.cil.oc.api.detail.Builder;
import li.cil.oc.api.detail.NetworkAPI;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Network;
import li.cil.oc.api.network.Node;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class RTTYCardTest {
    TestWorld world;
    NetworkAPI previousNetwork;
    Environment card;
    final Map<String, Node> nodes = new HashMap<>();
    final List<Object[]> events = new ArrayList<>();
    int addresses;
    Network network;

    interface Invoke { Object call(String name, Object[] args); }
    @SuppressWarnings("unchecked")
    static <T> T proxy(Class<T> type, Invoke invoke) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (p,m,a) -> {
            if(m.getName().equals("hashCode")) return System.identityHashCode(p);
            if(m.getName().equals("equals")) return p == a[0];
            if(m.getName().equals("toString")) return type.getSimpleName();
            return invoke.call(m.getName(), a);
        });
    }
    Arguments args(Object value) {
        return proxy(Arguments.class, (name,a) -> {
            if(name.equals("isString")) return value instanceof String;
            if(name.equals("checkString")) {
                if(!(value instanceof String)) throw new IllegalArgumentException("string expected");
                return value;
            }
            if(name.equals("checkAny")) return value;
            return null;
        });
    }
    Context context() { return proxy(Context.class, (name,a) -> name.equals("node") ? card.node() : null); }
    @BeforeAll static void bootstrap() throws Exception { AutocalTest.bootstrap(); }
    @BeforeEach void setup() {
        world = new TestWorld();
        RTTYSystem.broadcast.clear(); RTTYSystem.newMessages.clear();
        previousNetwork = API.network;
        network = proxy(Network.class, (name,a) -> name.equals("node") ? nodes.get(a[0]) : null);
        API.network = proxy(NetworkAPI.class, (name,a) -> {
            if(!name.equals("newNode")) return null;
            Object host = a[0]; String address = "card-" + ++addresses;
            Component node = proxy(Component.class, (n,b) -> {
                if(n.equals("host")) return host;
                if(n.equals("address")) return address;
                if(n.equals("network")) return network;
                if(n.equals("sendToReachable")) events.add((Object[])b[1]);
                return null;
            });
            nodes.put(address,node);
            Builder.ComponentBuilder component = proxy(Builder.ComponentBuilder.class, (n,b) -> n.equals("create") ? node : null);
            return proxy(Builder.NodeBuilder.class, (n,b) -> n.equals("withComponent") ? component : node);
        });
        EnvironmentHost host = proxy(EnvironmentHost.class, (name,a) -> name.equals("world") ? world : null);
        card = new Environment(host);
    }
    @AfterEach void teardown() { API.network = previousNetwork; }
    RTTYChanHandle handle(String channel) { return (RTTYChanHandle)card.open(context(),args(channel))[0]; }
    void signal(String channel, Object value, long timestamp) {
        RTTYSystem.RTTYChannel message = new RTTYSystem.RTTYChannel(); message.signal=value; message.timeStamp=timestamp;
        RTTYSystem.broadcast.put(new Pair<>(world,channel),message);
    }
    NBTTagCompound bindings(String... names) {
        NBTTagCompound nbt = new NBTTagCompound(); NBTTagList list = new NBTTagList();
        for(String name:names) list.appendTag(new NBTTagString(name));
        nbt.setTag("boundEvents",list); return nbt;
    }
    @Test void driverUsesCardSlotAndTierTwo() {
        Item item = new Item(); ItemRTTYCard.Driver driver = new ItemRTTYCard.Driver(item);
        assertEquals("card",driver.slot(new ItemStack(item))); assertEquals(1,driver.tier(new ItemStack(item)));
    }
    @Test void compatibilityHandlerVerifiesAndSkipsInitializationWithoutOC() throws Exception {
        final String name="com.hbm.handler.CompatHandler";
        byte[] bytes;
        try(java.io.InputStream input=getClass().getClassLoader().getResourceAsStream(name.replace('.','/')+".class")) {
            java.io.ByteArrayOutputStream buffer=new java.io.ByteArrayOutputStream();byte[] block=new byte[4096];int read;
            while((read=input.read(block))!=-1)buffer.write(block,0,read);bytes=buffer.toByteArray();
        }
        org.objectweb.asm.tree.ClassNode node=new org.objectweb.asm.tree.ClassNode();
        new org.objectweb.asm.ClassReader(bytes).accept(node,0);
        com.google.common.collect.ListMultimap<String,net.minecraftforge.fml.common.discovery.ASMDataTable.ASMData> optionals=com.google.common.collect.ArrayListMultimap.create();
        for(Object entry:node.methods) {
            org.objectweb.asm.tree.MethodNode method=(org.objectweb.asm.tree.MethodNode)entry;
            List<org.objectweb.asm.tree.AnnotationNode> annotations=new ArrayList<>();
            if(method.visibleAnnotations!=null)annotations.addAll(method.visibleAnnotations);
            if(method.invisibleAnnotations!=null)annotations.addAll(method.invisibleAnnotations);
            for(org.objectweb.asm.tree.AnnotationNode annotation:annotations)if(annotation.desc.equals("Lnet/minecraftforge/fml/common/Optional$Method;")) {
                Map<String,Object> info=new HashMap<>();
                for(int i=0;i<annotation.values.size();i+=2)info.put((String)annotation.values.get(i),annotation.values.get(i+1));
                optionals.put(name,new net.minecraftforge.fml.common.discovery.ASMDataTable.ASMData(null,"net.minecraftforge.fml.common.Optional$Method",name,method.name+method.desc,info));
            }
        }
        net.minecraftforge.fml.common.asm.transformers.ModAPITransformer transformer=new net.minecraftforge.fml.common.asm.transformers.ModAPITransformer();
        java.lang.reflect.Field field=transformer.getClass().getDeclaredField("optionals");field.setAccessible(true);field.set(transformer,optionals);
        java.lang.reflect.Field apis=net.minecraftforge.fml.common.ModAPIManager.class.getDeclaredField("apiContainers");apis.setAccessible(true);
        Object previousAPIs=apis.get(net.minecraftforge.fml.common.ModAPIManager.INSTANCE);
        final byte[] stripped;
        try {
            apis.set(net.minecraftforge.fml.common.ModAPIManager.INSTANCE,Collections.emptyMap());
            stripped=transformer.transform(name,name,bytes);
        } finally { apis.set(net.minecraftforge.fml.common.ModAPIManager.INSTANCE,previousAPIs); }
        ClassLoader withoutOC=new ClassLoader(getClass().getClassLoader()) {
            @Override protected Class<?> loadClass(String requested,boolean resolve)throws ClassNotFoundException {
                if(requested.startsWith("li.cil.oc."))throw new ClassNotFoundException(requested);
                if(requested.equals(name)) {
                    Class<?> found=findLoadedClass(requested);
                    if(found==null)found=defineClass(requested,stripped,0,stripped.length);
                    if(resolve)resolveClass(found);return found;
                }
                return super.loadClass(requested,resolve);
            }
        };
        assertFalse(net.minecraftforge.fml.common.Loader.isModLoaded("opencomputers"));
        Class<?> handler=Class.forName(name,true,withoutOC);
        handler.getMethod("init").invoke(null);
    }
    @Test void driverRecognizesOnlyItsCardAndKeepsEnvironmentData() {
        Item item=new Item(); ItemStack stack=new ItemStack(item); ItemRTTYCard.Driver driver=new ItemRTTYCard.Driver(item);
        assertTrue(driver.worksWith(stack)); assertFalse(driver.worksWith(new ItemStack(new Item())));
        driver.dataTag(stack).setString("probe","saved"); assertEquals("saved",driver.dataTag(stack).getString("probe"));
    }
    @Test void environmentUsesItsHostWorldAndUpdates() { assertSame(world,card.world()); assertTrue(card.canUpdate()); }
    @Test void openReturnsNamedHandle() { assertEquals("channel",handle("channel").channel(context(),null)[0]); }
    @Test void emptyChannelRejectedByAllBindings() {
        assertThrows(IllegalArgumentException.class,()->card.open(context(),args("")));
        assertThrows(IllegalArgumentException.class,()->card.bindEvent(context(),args("")));
        assertThrows(IllegalArgumentException.class,()->card.unbindEvent(context(),args("")));
    }
    @Test void channelTypeIsChecked() { assertThrows(IllegalArgumentException.class,()->card.open(context(),args(42))); }
    @Test void absentChannelIsNotANewSignal() { assertArrayEquals(new Object[]{null,false},handle("empty").read(context(),null)); }
    @Test void newSignalIsReportedOnlyOncePerHandle() {
        RTTYChanHandle handle=handle("a"); signal("a","one",10);
        assertArrayEquals(new Object[]{"one",true},handle.read(context(),null));
        assertArrayEquals(new Object[]{"one",false},handle.read(context(),null));
        signal("a","two",11); assertArrayEquals(new Object[]{"two",true},handle.read(context(),null));
    }
    @Test void handlesTrackReadsIndependently() {
        RTTYChanHandle a=handle("a"), b=handle("a"); signal("a",3,10);
        assertEquals(true,a.read(context(),null)[1]); assertEquals(true,b.read(context(),null)[1]);
    }
    @Test void writesAreQueuedForNextTick() {
        RTTYChanHandle handle=handle("a"); handle.write(context(),args("hello"));
        assertEquals("hello",RTTYSystem.newMessages.get(new Pair<>(world,"a")));
        assertArrayEquals(new Object[]{null,false},handle.read(context(),null));
        RTTYSystem.updateBroadcastQueue(); assertArrayEquals(new Object[]{"hello",true},handle.read(context(),null));
    }
    @Test void nativeValuesAndNumericAggregationRemainSupported() {
        RTTYChanHandle handle=handle("a"); handle.write(context(),args(2)); handle.write(context(),args(3));
        assertEquals("5",RTTYSystem.newMessages.get(new Pair<>(world,"a")));
        handle.write(context(),args(true)); assertEquals(true,RTTYSystem.newMessages.get(new Pair<>(world,"a")));
    }
    @Test void synchronousAliasesUseTheSameChannel() {
        RTTYChanHandle handle=handle("a"); handle.writeSync(context(),args("sync")); RTTYSystem.updateBroadcastQueue();
        assertArrayEquals(new Object[]{"sync",true},handle.readSync(context(),null));
    }
    @Test void disconnectedCardProducesUsefulError() {
        RTTYChanHandle handle=handle("a"); nodes.clear();
        assertEquals("no such component",assertThrows(IllegalArgumentException.class,()->handle.read(context(),null)).getMessage());
    }
    @Test void addressOfAnotherComponentProducesUsefulError() {
        RTTYChanHandle handle=handle("a"); nodes.put(card.node().address(),proxy(Node.class,(name,a)->null));
        assertEquals("no such component",assertThrows(IllegalArgumentException.class,()->handle.read(context(),null)).getMessage());
    }
    @Test void handleRetainsCardAddressAndChannelAcrossNBT() {
        RTTYChanHandle handle=handle("a"); NBTTagCompound nbt=new NBTTagCompound(); handle.save(nbt);
        RTTYChanHandle restored=new RTTYChanHandle(); restored.load(nbt); restored.write(context(),args("restored"));
        assertEquals("restored",RTTYSystem.newMessages.get(new Pair<>(world,"a")));
    }
    @Test void boundEventsFireOnlyForNewSignals() {
        card.bindEvent(context(),args("a")); signal("a","message",10); card.update(); card.update();
        assertEquals(1,events.size()); assertArrayEquals(new Object[]{"a","message"},events.get(0));
        signal("a","next",11); card.update(); assertEquals(2,events.size());
    }
    @Test void duplicateBindingAndUnbindingAreIdempotent() {
        card.bindEvent(context(),args("a")); card.bindEvent(context(),args("a"));
        signal("a","one",10); card.update(); assertEquals(1,events.size());
        card.unbindEvent(context(),args("a")); card.unbindEvent(context(),args("a"));
        signal("a","two",11); card.update(); assertEquals(1,events.size());
    }
    @Test void nonexistentBoundChannelDoesNotEmit() { card.bindEvent(context(),args("a")); card.update(); assertTrue(events.isEmpty()); }
    @Test void eventBindingsSurviveNBTWithoutDuplicates() {
        card.load(bindings("a","a","b")); card.load(bindings("a","a","b"));
        NBTTagCompound nbt=new NBTTagCompound(); card.save(nbt); assertEquals(2,nbt.getTagList("boundEvents",8).tagCount());
        signal("a","one",10); signal("b","two",10); card.update(); assertEquals(2,events.size());
    }
    @Test void loadingReplacesPreviousBindings() {
        card.bindEvent(context(),args("old")); card.load(bindings("new"));
        signal("old","old",10); signal("new","new",10); card.update();
        assertEquals(1,events.size()); assertEquals("new",events.get(0)[0]);
    }
    @Test void savingAnUnboundCardRemovesStaleSavedBindings() {
        NBTTagCompound nbt=bindings("old"); card.bindEvent(context(),args("a")); card.unbindEvent(context(),args("a"));
        card.save(nbt); assertFalse(nbt.hasKey("boundEvents"));
    }
    @Test void emptyNBTLoadClearsBindings() {
        card.bindEvent(context(),args("a")); card.load(new NBTTagCompound()); signal("a","one",10); card.update(); assertTrue(events.isEmpty());
    }
    @Test void radioValuesAreIsolatedByWorldAndChannelCase() {
        signal("a","local",10); assertNull(RTTYSystem.listen(new TestWorld(),"a")); assertNull(RTTYSystem.listen(world,"A"));
    }
    @Test void parallelDirectWritesDoNotLoseNumericSignals() throws Exception {
        ExecutorService workers=Executors.newFixedThreadPool(4);
        try {
            List<Future<?>> tasks=new ArrayList<>();
            for(int i=0;i<4;i++)tasks.add(workers.submit(()->{for(int j=0;j<250;j++)RTTYSystem.broadcast(world,"parallel","1");}));
            for(Future<?> task:tasks)task.get(10,TimeUnit.SECONDS);
            assertEquals("1000",RTTYSystem.newMessages.get(new Pair<>(world,"parallel")));
        } finally { workers.shutdownNow(); }
    }
    @Test void directWritesAndQueuePublicationDoNotRace() throws Exception {
        ExecutorService workers=Executors.newFixedThreadPool(2);
        try {
            Future<?> write=workers.submit(()->{for(int i=0;i<1000;i++)RTTYSystem.broadcast(world,"a","1");});
            Future<?> publish=workers.submit(()->{for(int i=0;i<1000;i++){RTTYSystem.updateBroadcastQueue();RTTYSystem.listen(world,"a");}});
            write.get(10,TimeUnit.SECONDS); publish.get(10,TimeUnit.SECONDS);
        } finally { workers.shutdownNow(); }
    }
    static class TestWorld extends net.minecraft.world.World {
        TestWorld() { super(new net.minecraft.world.storage.SaveHandlerMP(), new net.minecraft.world.storage.WorldInfo(new NBTTagCompound()), new net.minecraft.world.WorldProviderSurface(), new net.minecraft.profiler.Profiler(), false); }
        @Override protected net.minecraft.world.chunk.IChunkProvider createChunkProvider() { return null; }
        @Override protected boolean isChunkLoaded(int x,int z,boolean allowEmpty) { return true; }
        @Override public net.minecraft.entity.Entity getEntityByID(int id) { return null; }
        @Override public net.minecraft.block.state.IBlockState getBlockState(net.minecraft.util.math.BlockPos pos) { return net.minecraft.init.Blocks.AIR.getDefaultState(); }
    }
}
