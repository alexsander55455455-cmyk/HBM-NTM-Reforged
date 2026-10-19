package com.hbm.inventory;

import com.hbm.blocks.ModBlocks;
import com.hbm.config.GeneralConfig;
import com.hbm.items.ItemEnums.EnumCircuitType;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemBatteryPack.EnumBatteryPack;
import com.hbm.main.CraftingManager;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Each 528/expensive combination runs in a separate Java 8 process.
class LauncherCoreRecipeTest {
    @BeforeAll static void bootstrap() throws Exception {
        DataConsumerRecipeTest.bootstrap();
        GeneralConfig.enable528 = Boolean.getBoolean("launcher.mode528");
        GeneralConfig.enableExpensiveMode = Boolean.getBoolean("launcher.expensive");
        CraftingManager.addCrafting();
    }

    private static IRecipe recipe(boolean large) {
        IRecipe recipe = ForgeRegistries.RECIPES.getValue(new ResourceLocation("hbm", large ? "struct_launcher_core_large" : "struct_launcher_core"));
        assertNotNull(recipe, "Launcher core must remain craftable in 528 mode");
        return recipe;
    }

    private static InventoryCrafting grid(boolean large) {
        InventoryCrafting grid = new InventoryCrafting(new Container() {
            @Override public boolean canInteractWith(EntityPlayer player) { return true; }
        }, 3, 3);
        for(int slot = 0; slot < 9; slot++) {
            ItemStack input;
            if(slot == 7) input = new ItemStack(ModItems.battery_pack, 1, EnumBatteryPack.BATTERY_LEAD.ordinal());
            else if(slot == 6 || slot == 8) input = new ItemStack(ModBlocks.struct_launcher);
            else if(large ? slot == 1 || slot == 3 || slot == 5 : slot == 4) input = new ItemStack(Blocks.IRON_BARS);
            else if(large || slot == 1) input = new ItemStack(ModItems.circuit, 1, (large ? EnumCircuitType.ADVANCED : EnumCircuitType.BASIC).ordinal());
            else input = new ItemStack(ModBlocks.steel_scaffold);
            grid.setInventorySlotContents(slot, input);
        }
        return grid;
    }

    private static void result(boolean large) {
        IRecipe recipe = recipe(large); InventoryCrafting grid = grid(large);
        assertTrue(recipe.matches(grid, null));
        ItemStack result = recipe.getCraftingResult(grid);
        assertEquals(new ItemStack(large ? ModBlocks.struct_launcher_core_large : ModBlocks.struct_launcher_core).getItem(), result.getItem());
        assertEquals(1, result.getCount()); assertEquals(0, result.getMetadata());
        assertTrue(recipe.getRemainingItems(grid).stream().allMatch(ItemStack::isEmpty));
    }

    @Test void smallCoreCraftsWithItsExistingGridAndOutput() { result(false); }
    @Test void largeCoreCraftsWithItsExistingGridAndOutput() { result(true); }

    @Test void bothRecipesRejectEveryMissingOrUnrelatedInput() {
        for(boolean large : new boolean[]{false, true}) for(int slot = 0; slot < 9; slot++) {
            InventoryCrafting grid = grid(large); grid.setInventorySlotContents(slot, ItemStack.EMPTY);
            assertFalse(recipe(large).matches(grid, null), "Missing slot " + slot);
            grid.setInventorySlotContents(slot, new ItemStack(Items.DIAMOND));
            assertFalse(recipe(large).matches(grid, null), "Wrong slot " + slot);
        }
    }

    @Test void circuitTierAndBatteryMetadataAreRequired() {
        for(boolean large : new boolean[]{false, true}) {
            InventoryCrafting grid = grid(large);
            grid.setInventorySlotContents(7, new ItemStack(ModItems.battery_pack, 1, EnumBatteryPack.BATTERY_LITHIUM.ordinal()));
            assertFalse(recipe(large).matches(grid, null));
            grid = grid(large);
            grid.setInventorySlotContents(large ? 4 : 1, new ItemStack(ModItems.circuit, 1, (large ? EnumCircuitType.BASIC : EnumCircuitType.ADVANCED).ordinal()));
            assertFalse(recipe(large).matches(grid, null));
        }
    }

    @Test void soyuzAndRbmkStillRespectTheirExisting528Restriction() {
        for(String id : new String[]{"struct_soyuz_core", "rbmk_console", "rbmk_crane_console", "rbmk_rod", "rbmk_rod_mod", "rbmk_boiler", "rbmk_heater", "rbmk_cooler"})
            assertEquals(!GeneralConfig.enable528, ForgeRegistries.RECIPES.containsKey(new ResourceLocation("hbm", id)), id);
    }

    @Test void archiveEntireCraftingRegistrationForBeforeAfterComparison() throws Exception {
        String path = System.getProperty("launcher.catalog");
        if(path == null) return;
        List<String> entries = new ArrayList<>();
        for(IRecipe recipe : ForgeRegistries.RECIPES.getValuesCollection()) {
            StringBuilder row = new StringBuilder(recipe.getRegistryName() + "|" + recipe.getClass().getName() + "|" + recipe.getRecipeOutput().serializeNBT());
            recipe.getIngredients().forEach(ingredient -> {
                row.append('|');
                for(ItemStack stack : ingredient.getMatchingStacks()) row.append(stack.serializeNBT()).append(';');
            });
            entries.add(row.toString());
        }
        Files.write(Paths.get(path), entries, StandardCharsets.UTF_8);
    }
}
