package com.hbm.inventory;

import com.google.gson.JsonParser;
import com.google.gson.stream.JsonWriter;
import com.hbm.blocks.BlockEnums.EnumStoneType;
import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.RockMillRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipes.ChanceOutputMulti;
import com.hbm.render.misc.EnumSymbol;
import com.hbm.tileentity.TilePort.PortDef;
import com.hbm.tileentity.machine.TileEntityMachineRockMill;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.OreDictionary;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RockMillParityTest {
    @BeforeAll static void bootstrap() throws Exception {
        AutoCrystallizerOreDictionaryTest.bootstrap();
        com.hbm.main.MainRegistry.proxy = new com.hbm.main.ServerProxy();
        if (Fluids.NONE == null) Fluids.NONE = fluid("NONE");
        if (Fluids.WATER == null) Fluids.WATER = fluid("WATER");
        if (Fluids.COLLOID == null) Fluids.COLLOID = fluid("COLLOID");
        if (Fluids.LAVA == null) Fluids.LAVA = fluid("LAVA");
        // A source-only overlay still uses the old registry class. The real JAR must provide this alias itself.
        if (OreDictionary.getOres(OreDictManager.BAUXITE.ore()).isEmpty()) {
            OreDictionary.registerOre(OreDictManager.BAUXITE.ore(), new ItemStack(ModBlocks.stone_resource, 1, EnumStoneType.BAUXITE.ordinal()));
        }
        RockMillRecipes.INSTANCE.deleteRecipes();
        RockMillRecipes.INSTANCE.registerDefaults();
        GameRegistry.registerTileEntity(Mill.class, new ResourceLocation("hbm", "test_rockmill"));
    }

    private static FluidType fluid(String name) { return new FluidType(name, 0x888888, 0, 0, 0, EnumSymbol.NONE); }
    private static GenericRecipe recipe(String name) { return RockMillRecipes.INSTANCE.recipeNameMap.get("rock." + name); }
    private static Mill mill(String name) {
        Mill mill = new Mill();
        mill.rockMillModule.setRecipe("rock." + name, false);
        mill.rockMillModule.setupTanks(recipe(name));
        return mill;
    }
    private static void run(Mill mill, int ticks) {
        for (int i = 0; i < ticks; i++) mill.rockMillModule.update(1, 1, true, ItemStack.EMPTY);
    }

    @Test void allElevenRecipesHaveUsableInputsAndRespectMachineLimits() {
        assertEquals(11, RockMillRecipes.INSTANCE.recipeNameMap.size());
        for (GenericRecipe recipe : RockMillRecipes.INSTANCE.recipeOrderedList) {
            assertEquals(25, recipe.power);
            assertTrue(recipe.duration == 100 || recipe.duration == 200);
            if (recipe.inputItem != null) for (RecipesCommon.AStack input : recipe.inputItem) assertFalse(input.getStackList().isEmpty());
            assertTrue(recipe.inputItem == null || recipe.inputItem.length <= 3);
            assertTrue(recipe.outputItem == null || recipe.outputItem.length <= 3);
        }
        assertEquals(9, RockMillRecipes.INSTANCE.autoSwitchGroups.get("autoswitch.crushing").size());
    }

    @Test void bauxiteMatchesTheExistingMetadataWithoutChangingTheGemAlias() {
        ItemStack bauxite = new ItemStack(ModBlocks.stone_resource, 1, EnumStoneType.BAUXITE.ordinal());
        assertTrue(recipe("bauxite").inputItem[0].matchesRecipe(bauxite, false));
        assertFalse(OreDictionary.getOres(OreDictManager.BAUXITE.gem()).isEmpty());
    }

    @Test void clayRecipeConservesSandColloidPowerAndOutput() {
        Mill mill = mill("clay");
        mill.inventory.setStackInSlot(2, new ItemStack(Blocks.SAND, 2));
        mill.inputTanks[0].setFill(2500);
        mill.power = 5000;
        run(mill, 200);
        assertTrue(mill.inventory.getStackInSlot(2).isEmpty());
        assertEquals(Items.CLAY_BALL, mill.inventory.getStackInSlot(5).getItem());
        assertEquals(4, mill.inventory.getStackInSlot(5).getCount());
        assertEquals(0, mill.inputTanks[0].getFill());
        assertEquals(0, mill.power);
    }

    @Test void colloidRecipeDoesNotRequireAnItemInput() {
        Mill mill = mill("colloid");mill.inputTanks[0].setFill(1000);mill.power = 2500;
        run(mill, 100);
        assertEquals(new ItemStack(Blocks.SAND).getItem(), mill.inventory.getStackInSlot(5).getItem());
        assertEquals(1, mill.inventory.getStackInSlot(5).getCount());
        assertEquals(0, mill.inputTanks[0].getFill());
    }

    @Test void insufficientPowerDoesNotConsumeIngredientsOrFluids() {
        Mill mill = mill("clay");mill.inventory.setStackInSlot(2, new ItemStack(Blocks.SAND, 2));mill.inputTanks[0].setFill(2500);mill.power = 24;
        run(mill, 5);
        assertEquals(2, mill.inventory.getStackInSlot(2).getCount());assertEquals(2500, mill.inputTanks[0].getFill());assertEquals(24, mill.power);assertFalse(mill.rockMillModule.didProcess);
    }

    @Test void insufficientFluidDoesNotConsumePowerOrItems() {
        Mill mill = mill("clay");mill.inventory.setStackInSlot(2, new ItemStack(Blocks.SAND, 2));mill.inputTanks[0].setFill(2499);mill.power = 5000;
        run(mill, 5);
        assertEquals(2, mill.inventory.getStackInSlot(2).getCount());assertEquals(2499, mill.inputTanks[0].getFill());assertEquals(5000, mill.power);
    }

    @Test void blockedItemOutputDoesNotConsumeInputs() {
        Mill mill = mill("clay");mill.inventory.setStackInSlot(2, new ItemStack(Blocks.SAND, 2));mill.inventory.setStackInSlot(5, new ItemStack(Items.DIAMOND, 64));mill.inputTanks[0].setFill(2500);mill.power = 5000;
        run(mill, 5);
        assertEquals(2, mill.inventory.getStackInSlot(2).getCount());assertEquals(2500, mill.inputTanks[0].getFill());assertEquals(5000, mill.power);
    }

    @Test void blockedFluidOutputStopsCrushingBeforeEnergyIsUsed() {
        Mill mill = mill("cobble");mill.inventory.setStackInSlot(2, new ItemStack(Blocks.COBBLESTONE));mill.inputTanks[0].setFill(250);mill.outputTanks[0].setFill(3900);mill.power = 2500;
        run(mill, 5);assertEquals(2500, mill.power);assertEquals(250, mill.inputTanks[0].getFill());assertEquals(3900, mill.outputTanks[0].getFill());
    }

    @Test void autoSwitchUsesTheNextCrushingRecipeWithoutProcessingTheOldOne() {
        Mill mill = mill("cobble");mill.inventory.setStackInSlot(2, new ItemStack(Blocks.GRAVEL));mill.inputTanks[0].setFill(250);mill.power = 2500;
        run(mill, 1);assertEquals("rock.gravel", mill.rockMillModule.recipe);assertEquals(2500, mill.power);
        run(mill, 100);assertTrue(mill.inventory.getStackInSlot(2).isEmpty());assertEquals(250, mill.outputTanks[0].getFill());
    }

    @Test void overlappingCobblestoneAliasDoesNotOscillateAwayFromSelectedBasaltRecipe() {
        ItemStack basalt = new ItemStack(ModBlocks.basalt);
        OreDictionary.registerOre(OreDictManager.KEY_COBBLESTONE, basalt);
        Mill mill = mill("basalt");mill.inventory.setStackInSlot(2, basalt);mill.inputTanks[0].setFill(250);mill.power = 5000;
        run(mill, 200);
        assertEquals("rock.basalt", mill.rockMillModule.recipe);
        assertEquals(0, mill.power);assertTrue(mill.inventory.getStackInSlot(2).isEmpty());assertEquals(250, mill.outputTanks[0].getFill());
    }

    @Test void shrinkingSameFluidRecipeBuffersRetainsExistingContents() {
        Mill mill = mill("clay");mill.inputTanks[0].setFill(4800);
        mill.rockMillModule.setupTanks(recipe("colloid"));
        assertEquals(4800, mill.inputTanks[0].getFill());assertEquals(4800, mill.inputTanks[0].getMaxFill());
    }

    @Test void crushingWeightsAndResultsMatchThePinnedOriginal() {
        ChanceOutputMulti output = (ChanceOutputMulti) recipe("cobble").outputItem[0];
        assertEquals(95, output.pool.get(0).itemWeight);assertEquals(5, output.pool.get(1).itemWeight);
        GenericRecipes.RNG.setSeed(5846);int gravel = 0;
        for (int i = 0; i < 1000; i++) if (output.collapse().getItem() == new ItemStack(Blocks.GRAVEL).getItem()) gravel++;
        assertTrue(gravel > 920 && gravel < 980);
    }

    @Test void checkedGuiInventoryRejectsWrongInputsAndProtectsOutputSlots() {
        Mill mill = mill("clay");
        assertEquals(Items.DIAMOND, mill.getCheckedInventory().insertItem(0, new ItemStack(Items.DIAMOND), false).getItem());
        assertEquals(Items.DIAMOND, mill.getCheckedInventory().insertItem(1, new ItemStack(Items.DIAMOND), false).getItem());
        assertEquals(Items.DIAMOND, mill.getCheckedInventory().insertItem(2, new ItemStack(Items.DIAMOND), false).getItem());
        assertTrue(mill.getCheckedInventory().insertItem(2, new ItemStack(Blocks.SAND), false).isEmpty());
        assertFalse(mill.getCheckedInventory().insertItem(5, new ItemStack(Items.CLAY_BALL), false).isEmpty());
    }

    @Test void eightPortsHaveUniquePositionsAndPointOutOfTheFootprint() {
        Mill mill = new Mill();mill.setPos(new BlockPos(10, 20, 30));Set<BlockPos> positions = new HashSet<>();
        for (PortDef port : mill.getPorts()) {
            assertTrue(positions.add(port.portPositions[0]));
            BlockPos relative = port.portPositions[0].subtract(mill.getPos());
            assertEquals(3, Math.abs(relative.getX()) + Math.abs(relative.getZ()));
            BlockPos external = port.portConnections[0].getPos().subtract(mill.getPos());
            assertEquals(4, Math.abs(external.getX()) + Math.abs(external.getZ()));
        }
        assertEquals(8, positions.size());
        assertArrayEquals(new int[]{2,3,4,5,6,7}, mill.getAccessibleSlotsFromSide(EnumFacing.NORTH));
    }

    @Test void nbtRestoresInputsOutputPowerRecipeAndProgress() {
        Mill mill = mill("clay");mill.power = 3210;mill.maxPower = 6000;mill.inputTanks[0].setFill(2222);mill.inventory.setStackInSlot(2, new ItemStack(Blocks.SAND, 2));mill.rockMillModule.progress = 0.375;
        Mill restored = new Mill();restored.readFromNBT(mill.writeToNBT(new NBTTagCompound()));
        assertEquals(3210, restored.power);assertEquals(6000, restored.maxPower);assertEquals(2222, restored.inputTanks[0].getFill());assertSame(Fluids.COLLOID, restored.inputTanks[0].getTankType());assertEquals("rock.clay", restored.rockMillModule.recipe);assertEquals(0.375, restored.rockMillModule.progress);assertEquals(2, restored.inventory.getStackInSlot(2).getCount());
    }

    @Test void packetRoundtripKeepsEnergyProcessingAndTankState() {
        Mill mill = mill("cobble");mill.power = 2345;mill.inputTanks[0].setFill(777);mill.didProcess = true;mill.rockMillModule.progress = 0.25;
        ByteBuf buffer = Unpooled.buffer();try {
            mill.serialize(buffer);Mill restored = new Mill();restored.deserialize(buffer);
            assertEquals(2345, restored.power);assertEquals(777, restored.inputTanks[0].getFill());assertTrue(restored.didProcess);assertEquals(0.25, restored.rockMillModule.progress);assertEquals("rock.cobble", restored.rockMillModule.recipe);assertEquals(0, buffer.readableBytes());
        } finally { buffer.release(); }
    }

    @Test void recipeConfigurationRoundtripsAllElevenRecipes() throws Exception {
        RockMillRecipes copy = new RockMillRecipes();
        for (GenericRecipe recipe : RockMillRecipes.INSTANCE.recipeOrderedList) {
            StringWriter text = new StringWriter();try (JsonWriter writer = new JsonWriter(text)) { writer.beginObject();RockMillRecipes.INSTANCE.writeRecipe(recipe, writer);writer.endObject(); }
            copy.readRecipe(new JsonParser().parse(text.toString()));
        }
        assertEquals(11, copy.recipeNameMap.size());assertEquals(2500, copy.recipeNameMap.get("rock.clay").inputFluid[0].fill);
        assertEquals(95, ((ChanceOutputMulti)copy.recipeNameMap.get("rock.cobble").outputItem[0]).pool.get(0).itemWeight);
    }

    public static class Mill extends TileEntityMachineRockMill {
        @Override public void markDirty() { }
        @Override public void markChanged() { }
    }
}
