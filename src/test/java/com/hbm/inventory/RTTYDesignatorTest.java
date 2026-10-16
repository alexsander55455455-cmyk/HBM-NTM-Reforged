package com.hbm.inventory;

import com.hbm.items.tool.ItemRTTYDesignator;
import com.hbm.packet.toserver.NBTItemControlPacket;
import com.hbm.tileentity.network.RTTYSystem;
import com.hbm.util.Tuple.Pair;
import com.mojang.authlib.GameProfile;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.storage.SaveHandlerMP;
import net.minecraft.world.storage.WorldInfo;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class RTTYDesignatorTest {
    TestWorld world;
    Player player;
    ItemRTTYDesignator item;
    ItemStack stack;

    @BeforeAll static void bootstrap() throws Exception { AutocalTest.bootstrap(); }
    @BeforeEach void setup() {
        RTTYSystem.broadcast.clear(); RTTYSystem.newMessages.clear();
        world = new TestWorld(false); player = new Player(world);
        item = new ItemRTTYDesignator("test_rtty_designator"); stack = new ItemStack(item);
        player.setHeldItem(EnumHand.MAIN_HAND, stack);
    }
    void channel(String name) {
        NBTTagCompound data = new NBTTagCompound(); data.setString("chan", name); item.receiveControl(stack, data);
    }
    void aim(BlockPos target, EnumFacing face, double distance) {
        world.blocks.put(target, Blocks.STONE.getDefaultState());
        Vec3d eye = new Vec3d(target).add(0.5, 0.5, 0.5).add(new Vec3d(face.getDirectionVec()).scale(distance));
        player.setPosition(eye.x, eye.y - (player.getEyeHeight() - player.getDefaultEyeHeight() + player.eyeHeight), eye.z);
        player.prevPosX=player.posX; player.prevPosY=player.posY; player.prevPosZ=player.posZ;
        player.look = new Vec3d(face.getOpposite().getDirectionVec());
    }
    void click() { item.onItemRightClick(world, player, EnumHand.MAIN_HAND); }
    Object queued(String name) { return RTTYSystem.newMessages.get(new Pair<>(world, name)); }
    void face(EnumFacing face) {
        channel("target"); BlockPos block = new BlockPos(-8, 64, 12); aim(block, face, 10); click();
        BlockPos expected=block.offset(face);
        assertEquals(expected.getX()+":"+expected.getY()+":"+expected.getZ(), queued("target"));
    }
    @Test void selectsNorthFace() { face(EnumFacing.NORTH); }
    @Test void selectsSouthFace() { face(EnumFacing.SOUTH); }
    @Test void selectsEastFace() { face(EnumFacing.EAST); }
    @Test void selectsWestFace() { face(EnumFacing.WEST); }
    @Test void selectsTopFace() { face(EnumFacing.UP); }
    @Test void selectsBottomFace() { face(EnumFacing.DOWN); }
    @Test void acceptsDistantTargetInsideThreeHundredBlocks() {
        channel("target"); aim(new BlockPos(0,64,0),EnumFacing.EAST,299); click(); assertEquals("1:64:0", queued("target"));
    }
    @Test void ignoresTargetBeyondThreeHundredBlocks() {
        channel("target"); aim(new BlockPos(0,64,0),EnumFacing.EAST,302); click(); assertTrue(RTTYSystem.newMessages.isEmpty());
    }
    @Test void ignoresMissAndSkyWithoutCrashing() { channel("target"); player.look=new Vec3d(0,1,0); click(); assertTrue(RTTYSystem.newMessages.isEmpty()); }
    @Test void ignoresUnsetChannel() { aim(new BlockPos(0,64,0),EnumFacing.UP,10); click(); assertTrue(RTTYSystem.newMessages.isEmpty()); }
    @Test void ignoresEmptyChannel() { channel(""); aim(new BlockPos(0,64,0),EnumFacing.UP,10); click(); assertTrue(RTTYSystem.newMessages.isEmpty()); }
    @Test void serverSneakingDoesNotBroadcast() { channel("target"); aim(new BlockPos(0,64,0),EnumFacing.UP,10); player.setSneaking(true); click(); assertTrue(RTTYSystem.newMessages.isEmpty()); }
    @Test void clientDoesNotBroadcast() {
        world=new TestWorld(true); player=new Player(world); player.setHeldItem(EnumHand.MAIN_HAND,stack);
        channel("target"); aim(new BlockPos(0,64,0),EnumFacing.UP,10); click(); assertTrue(RTTYSystem.newMessages.isEmpty());
    }
    @Test void offhandUsesItsOwnChannelAndLeavesMainHandUntouched() {
        channel("main"); ItemStack off = new ItemStack(item); NBTTagCompound data=new NBTTagCompound();data.setString("chan","off");item.receiveControl(off,data);
        player.setHeldItem(EnumHand.OFF_HAND,off); aim(new BlockPos(0,64,0),EnumFacing.UP,10);
        assertSame(off,item.onItemRightClick(world,player,EnumHand.OFF_HAND).getResult());
        assertEquals("0:65:0",queued("off")); assertNull(queued("main")); assertEquals("main",stack.getTagCompound().getString("chan"));
    }
    @Test void sendsThroughExistingOneTickRTTYQueue() {
        channel("target"); aim(new BlockPos(0,64,0),EnumFacing.UP,10); click(); assertNull(RTTYSystem.listen(world,"target"));
        RTTYSystem.updateBroadcastQueue(); assertEquals("0:65:0",RTTYSystem.listen(world,"target").signal);
    }
    @Test void channelUpdatePreservesItemNameAndOtherNBT() {
        stack.setStackDisplayName("Custom"); stack.getTagCompound().setInteger("marker",42); channel("old"); channel("new");
        assertEquals("new",stack.getTagCompound().getString("chan")); assertEquals("Custom",stack.getDisplayName()); assertEquals(42,stack.getTagCompound().getInteger("marker"));
    }
    @Test void unrelatedOrWronglyTypedControlDoesNotClearChannel() {
        channel("keep"); item.receiveControl(stack,new NBTTagCompound()); NBTTagCompound bad=new NBTTagCompound();bad.setInteger("chan",1);item.receiveControl(stack,bad);
        assertEquals("keep",stack.getTagCompound().getString("chan"));
    }
    @Test void savedStackRetainsChannel() {
        channel("saved"); ItemStack loaded=new ItemStack(stack.writeToNBT(new NBTTagCompound())); assertEquals("saved",loaded.getTagCompound().getString("chan"));
    }
    @Test void channelCanBeCleared() { channel("old"); channel(""); assertEquals("",stack.getTagCompound().getString("chan")); }
    @Test void controlPacketRetainsLegacyEncoding() throws Exception {
        NBTTagCompound data=new NBTTagCompound();data.setString("chan","legacy");ByteBuf bytes=Unpooled.buffer();
        try {new NBTItemControlPacket(data).toBytes(bytes);assertEquals(data,new PacketBuffer(bytes).readCompoundTag());assertFalse(bytes.isReadable());} finally {bytes.release();}
    }
    @Test void controlPacketCarriesBothExplicitHands() throws Exception {
        for(EnumHand hand:EnumHand.values()) { NBTTagCompound data=new NBTTagCompound();data.setString("chan","test");ByteBuf bytes=Unpooled.buffer();
            try {new NBTItemControlPacket(data,hand).toBytes(bytes);assertEquals(data,new PacketBuffer(bytes).readCompoundTag());assertEquals(hand.ordinal(),bytes.readUnsignedByte());assertFalse(bytes.isReadable());} finally {bytes.release();}
        }
    }
    @Test void nonStackableAndNoServerContainer() { assertEquals(1,item.getItemStackLimit()); assertNull(item.provideContainer(0,player,world,0,-1,0)); }

    static class Player extends EntityPlayer {
        Vec3d look=new Vec3d(0,0,1);
        Player(World world) {super(world,new GameProfile(UUID.fromString("11111111-1111-1111-1111-111111111111"),"DesignatorTest"));eyeHeight=0;}
        @Override public Vec3d getLook(float partialTicks) {return look;}
        @Override public boolean isSpectator() {return false;}
        @Override public boolean isCreative() {return false;}
    }
    static class TestWorld extends World {
        final Map<BlockPos,IBlockState> blocks=new HashMap<>();
        TestWorld(boolean client) {super(new SaveHandlerMP(),new WorldInfo(new NBTTagCompound()),new WorldProviderSurface(),new Profiler(),client);chunkProvider=createChunkProvider();}
        @Override protected IChunkProvider createChunkProvider() {
            return (IChunkProvider) java.lang.reflect.Proxy.newProxyInstance(IChunkProvider.class.getClassLoader(), new Class<?>[]{IChunkProvider.class}, (proxy,method,args) -> {
                if (method.getName().equals("getLoadedChunk") || method.getName().equals("provideChunk")) {
                    net.minecraft.world.chunk.Chunk chunk = new net.minecraft.world.chunk.Chunk(this,(Integer)args[0],(Integer)args[1]);
                    for (Map.Entry<BlockPos,IBlockState> entry : blocks.entrySet()) {
                        BlockPos pos=entry.getKey();
                        if ((pos.getX() >> 4)!=(Integer)args[0] || (pos.getZ() >> 4)!=(Integer)args[1]) continue;
                        net.minecraft.world.chunk.storage.ExtendedBlockStorage storage=new net.minecraft.world.chunk.storage.ExtendedBlockStorage(pos.getY() & ~15,true);
                        storage.set(pos.getX() & 15,pos.getY() & 15,pos.getZ() & 15,entry.getValue());
                        chunk.getBlockStorageArray()[pos.getY() >> 4]=storage;
                    }
                    return chunk;
                }
                return null;
            });
        }
        @Override public BlockPos getSpawnPoint() {return new BlockPos(0,64,0);}
        @Override protected boolean isChunkLoaded(int x,int z,boolean allowEmpty) {return true;}
        @Override public IBlockState getBlockState(BlockPos pos) {return blocks==null?Blocks.AIR.getDefaultState():blocks.getOrDefault(pos,Blocks.AIR.getDefaultState());}
    }
}
