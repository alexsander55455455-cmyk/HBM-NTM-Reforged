package com.hbm.inventory;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.PlantEnums.EnumTallPlantType;
import com.hbm.blocks.machine.MachineThresher;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.render.misc.EnumSymbol;
import com.hbm.tileentity.TilePort.PortDef;
import com.hbm.tileentity.machine.TileEntityMachineThresher;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.storage.SaveHandlerMP;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.fml.common.registry.GameRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ThresherParityTest {
    private static final BlockPos CORE = new BlockPos(0, 5, 0), CROP = new BlockPos(0, 5, 6);
    private static MachineThresher block;

    @BeforeAll static void bootstrap() throws Exception {
        AutoCrystallizerOreDictionaryTest.bootstrap();
        com.hbm.main.MainRegistry.proxy = new com.hbm.main.ServerProxy();
        for(String name : new String[]{"NONE", "WATER", "WOODOIL", "ETHANOL", "FISHOIL", "HEAVYOIL", "COALCREOSOTE"}) {
            java.lang.reflect.Field field = Fluids.class.getField(name);
            if(field.get(null) == null) field.set(null, new FluidType(name, 0x888888, 0, 0, 0, EnumSymbol.NONE));
        }
        block = new MachineThresher("test_thresher");
        GameRegistry.registerTileEntity(Thresher.class, new ResourceLocation("hbm", "test_thresher"));
    }

    private static Thresher fixture(TestWorld world) {
        world.blocks.put(CORE, block.getStateFromMeta(3));
        Thresher machine = new Thresher();machine.setWorld(world);machine.setPos(CORE);return machine;
    }
    private static void plant(TestWorld world, Block plant, int meta) {
        world.blocks.put(CROP.down(), (plant == Blocks.NETHER_WART ? Blocks.SOUL_SAND : Blocks.FARMLAND).getDefaultState());
        world.blocks.put(CROP, plant.getStateFromMeta(meta));
    }
    private static int count(List<ItemStack> drops, Item item) { return drops.stream().filter(s -> s.getItem() == item).mapToInt(ItemStack::getCount).sum(); }

    @Test void allFiveOriginalFuelsWorkAndWaterDoesNot() {
        for(FluidType fuel : new FluidType[]{Fluids.WOODOIL,Fluids.ETHANOL,Fluids.FISHOIL,Fluids.HEAVYOIL,Fluids.COALCREOSOTE}) assertTrue(TileEntityMachineThresher.acceptsFuel(fuel));
        assertFalse(TileEntityMachineThresher.acceptsFuel(Fluids.WATER));assertFalse(TileEntityMachineThresher.acceptsFuel(null));
    }
    @Test void stateMetadataPreservesAllFourOriginalDirections() {
        for(int meta = 2;meta <= 5;meta++) assertEquals(meta, block.getMetaFromState(block.getStateFromMeta(meta)));
    }
    @Test void matureEligibilityIncludesMelonsPumpkinsAndNetherWartButRejectsStone() {
        for(Block plant : new Block[]{Blocks.WHEAT,Blocks.CARROTS,Blocks.POTATOES,Blocks.BEETROOTS,Blocks.NETHER_WART,Blocks.MELON_BLOCK,Blocks.PUMPKIN}) assertTrue(TileEntityMachineThresher.canCut(plant));
        assertFalse(TileEntityMachineThresher.canCut(Blocks.STONE));
    }
    @Test void maturityChecksPreserveStemsAndImmatureWart() {
        TestWorld world = new TestWorld();plant(world,Blocks.WHEAT,0);assertTrue(TileEntityMachineThresher.shouldIgnore(world,CROP.getX(),CROP.getY(),CROP.getZ(),Blocks.WHEAT,0));
        plant(world,Blocks.WHEAT,7);assertFalse(TileEntityMachineThresher.shouldIgnore(world,CROP.getX(),CROP.getY(),CROP.getZ(),Blocks.WHEAT,7));
        assertTrue(TileEntityMachineThresher.shouldIgnore(world,0,5,6,Blocks.MELON_STEM,7));assertTrue(TileEntityMachineThresher.shouldIgnore(world,0,5,6,Blocks.PUMPKIN_STEM,7));
        assertTrue(TileEntityMachineThresher.shouldIgnore(world,0,5,6,Blocks.NETHER_WART,2));assertFalse(TileEntityMachineThresher.shouldIgnore(world,0,5,6,Blocks.NETHER_WART,3));
    }
    @Test void wheatHarvestReplantsAndCollectsProduce() {
        TestWorld world = new TestWorld();Thresher machine = fixture(world);plant(world,Blocks.WHEAT,7);machine.crop(Blocks.WHEAT,7);
        assertEquals(Blocks.WHEAT,world.getBlockState(CROP).getBlock());assertEquals(0,Blocks.WHEAT.getMetaFromState(world.getBlockState(CROP)));assertEquals(1,count(machine.drops,Items.WHEAT));
    }
    @Test void carrotsAndPotatoesSpendExactlyOneDropOnReplanting() {
        for(Block crop : new Block[]{Blocks.CARROTS,Blocks.POTATOES}) {
            TestWorld world = new TestWorld();Thresher machine = fixture(world);plant(world,crop,7);world.rand.setSeed(5846);List<ItemStack> expected=crop.getDrops(world,CROP,world.getBlockState(CROP),0);world.rand.setSeed(5846);machine.crop(crop,7);
            Item seed=crop==Blocks.CARROTS?Items.CARROT:Items.POTATO;assertEquals(count(expected,seed)-1,count(machine.drops,seed));assertEquals(crop,world.getBlockState(CROP).getBlock());assertEquals(0,crop.getMetaFromState(world.getBlockState(CROP)));
        }
    }
    @Test void matureNetherWartReplantsOnSoulSand() {
        TestWorld world = new TestWorld();Thresher machine=fixture(world);plant(world,Blocks.NETHER_WART,3);machine.crop(Blocks.NETHER_WART,3);
        assertEquals(Blocks.NETHER_WART,world.getBlockState(CROP).getBlock());assertEquals(0,Blocks.NETHER_WART.getMetaFromState(world.getBlockState(CROP)));assertTrue(count(machine.drops,Items.NETHER_WART)>0);
    }
    @Test void melonAndPumpkinHarvestKeepsAdjacentStem() {
        for(Block crop:new Block[]{Blocks.MELON_BLOCK,Blocks.PUMPKIN}) {
            TestWorld world=new TestWorld();Thresher machine=fixture(world);plant(world,crop,0);world.blocks.put(CROP.east(),Blocks.MELON_STEM.getStateFromMeta(7));machine.crop(crop,0);
            assertEquals(Blocks.AIR,world.getBlockState(CROP).getBlock());assertEquals(Blocks.MELON_STEM,world.getBlockState(CROP.east()).getBlock());assertFalse(machine.drops.isEmpty());
        }
    }
    @Test void threeHighCaneLeavesItsBaseAndCollectsOnlyUpperBlocks() {
        TestWorld world=new TestWorld();Thresher machine=fixture(world);for(int i=0;i<3;i++)world.blocks.put(CROP.up(i),Blocks.REEDS.getDefaultState());machine.cane(Blocks.REEDS,CROP);
        assertEquals(Blocks.REEDS,world.getBlockState(CROP).getBlock());assertEquals(Blocks.AIR,world.getBlockState(CROP.up()).getBlock());assertEquals(Blocks.AIR,world.getBlockState(CROP.up(2)).getBlock());assertEquals(2,count(machine.drops,Items.REEDS));
    }
    @Test void shortCaneNeverDeletesUnrelatedBlocksAboveIt() {
        TestWorld world=new TestWorld();Thresher machine=fixture(world);world.blocks.put(CROP,Blocks.REEDS.getDefaultState());world.blocks.put(CROP.up(),Blocks.STONE.getDefaultState());world.blocks.put(CROP.up(2),Blocks.DIAMOND_BLOCK.getDefaultState());machine.cane(Blocks.REEDS,CROP);
        assertEquals(Blocks.STONE,world.getBlockState(CROP.up()).getBlock());assertEquals(Blocks.DIAMOND_BLOCK,world.getBlockState(CROP.up(2)).getBlock());assertTrue(machine.drops.isEmpty());
    }
    @Test void raisedCactusHitStillKeepsLowestBlock() {
        TestWorld world=new TestWorld();Thresher machine=fixture(world);for(int i=0;i<3;i++)world.blocks.put(CROP.up(i),Blocks.CACTUS.getDefaultState());machine.cane(Blocks.CACTUS,CROP.up());
        assertEquals(Blocks.CACTUS,world.getBlockState(CROP).getBlock());assertEquals(2,count(machine.drops,Item.getItemFromBlock(Blocks.CACTUS)));
    }
    @Test void immatureTallPlantUpperHalfIsLeftIntact() {
        for(EnumTallPlantType type:new EnumTallPlantType[]{EnumTallPlantType.MUSTARD_WILLOW_2_LOWER,EnumTallPlantType.MUSTARD_WILLOW_3_LOWER}) {
            TestWorld world=new TestWorld();Thresher machine=fixture(world);world.blocks.put(CROP,ModBlocks.plant_tall.getStateFromMeta(type.ordinal()));world.blocks.put(CROP.up(),ModBlocks.plant_tall.getStateFromMeta(type.ordinal()+1));machine.tall(type.ordinal());
            assertEquals(ModBlocks.plant_tall,world.getBlockState(CROP.up()).getBlock());assertTrue(machine.drops.isEmpty());
        }
    }
    @Test void matureTallPlantLosesOnlyUpperHalf() {
        for(EnumTallPlantType type:new EnumTallPlantType[]{EnumTallPlantType.HEMP_LOWER,EnumTallPlantType.MUSTARD_WILLOW_4_LOWER}) {
            TestWorld world=new TestWorld();Thresher machine=fixture(world);int meta=type.ordinal();world.blocks.put(CROP,ModBlocks.plant_tall.getStateFromMeta(meta));world.blocks.put(CROP.up(),ModBlocks.plant_tall.getStateFromMeta(meta+1));machine.tall(meta);
            assertEquals(ModBlocks.plant_tall,world.getBlockState(CROP).getBlock());assertEquals(Blocks.AIR,world.getBlockState(CROP.up()).getBlock());assertFalse(machine.drops.isEmpty());
        }
    }
    @Test void portsUseOnlyTwoLateralFacesForEveryOrientation() {
        for(int meta=2;meta<=5;meta++) {
            TestWorld world=new TestWorld();Thresher machine=fixture(world);world.blocks.put(CORE,block.getStateFromMeta(meta));PortDef[] ports=machine.getPorts();assertEquals(1,ports.length);assertEquals(CORE,ports[0].portPositions[0]);assertEquals(2,ports[0].portConnections.length);assertEquals(ports[0].portConnections[0].getDir().getOpposite(),ports[0].portConnections[1].getDir());assertNotEquals(meta,ports[0].portConnections[0].getDir().ordinal());
        }
    }
    @Test void directUpperHalfHitHarvestsMatureHempAndWillowWithoutMovingUpAgain() {
        for(EnumTallPlantType type:new EnumTallPlantType[]{EnumTallPlantType.HEMP_UPPER,EnumTallPlantType.MUSTARD_WILLOW_4_UPPER}) {
            TestWorld world=new TestWorld();Thresher machine=fixture(world);world.blocks.put(CROP.down(),ModBlocks.plant_tall.getStateFromMeta(type.ordinal()-1));world.blocks.put(CROP,ModBlocks.plant_tall.getStateFromMeta(type.ordinal()));machine.tall(type.ordinal());
            assertEquals(ModBlocks.plant_tall,world.getBlockState(CROP.down()).getBlock());assertEquals(Blocks.AIR,world.getBlockState(CROP).getBlock());assertFalse(machine.drops.isEmpty());
        }
    }
    @Test void nbtPreservesFuelSuspensionArmAndCycleState() {
        TestWorld world=new TestWorld();Thresher machine=fixture(world);machine.isOn=true;machine.isSuspended=true;machine.angle=41.25F;machine.tank.setTankType(Fluids.COALCREOSOTE);machine.tank.setFill(73);NBTTagCompound tag=machine.writeToNBT(new NBTTagCompound());Thresher restored=new Thresher();restored.readFromNBT(tag);
        assertTrue(restored.isOn);assertTrue(restored.isSuspended);assertEquals(41.25F,restored.angle);assertSame(Fluids.COALCREOSOTE,restored.tank.getTankType());assertEquals(73,restored.tank.getFill());assertSame(restored.tank,restored.getTankToPaste());
    }
    @Test void packetConsumesEntireBufferAndRestoresInterpolationTarget() {
        Thresher machine=new Thresher();machine.isOn=true;machine.isSuspended=true;machine.angle=42F;machine.tank.setFill(51);ByteBuf data=Unpooled.buffer();try {machine.serialize(data);Thresher restored=new Thresher();restored.deserialize(data);assertTrue(restored.isOn);assertTrue(restored.isSuspended);assertEquals(42F,restored.syncAngle);assertEquals(51,restored.tank.getFill());assertEquals(0,data.readableBytes());}finally{data.release();}
    }

    public static class Thresher extends TileEntityMachineThresher {
        final List<ItemStack> drops=new ArrayList<>();
        @Override protected void dropItem(ItemStack stack) { if(!stack.isEmpty())drops.add(stack.copy()); }
        @Override public void markDirty() { }
        @Override public void markChanged() { }
        @Override public void networkPackNT(int range) { }
        void crop(Block block,int meta) { cutCrop(block,meta,CROP.getX(),CROP.getY(),CROP.getZ()); }
        void cane(Block block,BlockPos at) { cutCane(block,at.getX(),at.getY(),at.getZ()); }
        void tall(int meta) { cutTallPlant(ModBlocks.plant_tall,meta,CROP.getX(),CROP.getY(),CROP.getZ()); }
    }
    private static class TestWorld extends World {
        final Map<BlockPos,IBlockState> blocks=new HashMap<>();
        TestWorld() { super(new SaveHandlerMP(),new WorldInfo(new NBTTagCompound()),new WorldProviderSurface(),new Profiler(),false); }
        @Override public IBlockState getBlockState(BlockPos pos) { return blocks==null?Blocks.AIR.getDefaultState():blocks.getOrDefault(pos,Blocks.AIR.getDefaultState()); }
        @Override public boolean setBlockState(BlockPos pos,IBlockState state,int flags) { blocks.put(pos,state);return true; }
        @Override public void playEvent(int type,BlockPos pos,int data) { }
        @Override protected IChunkProvider createChunkProvider() { return null; }
        @Override protected boolean isChunkLoaded(int x,int z,boolean allowEmpty) { return true; }
        @Override public Entity getEntityByID(int id) { return null; }
    }
}
