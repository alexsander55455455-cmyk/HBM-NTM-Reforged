package com.hbm.inventory;

import com.hbm.blocks.ModBlocks;
import com.hbm.crafting.MineralRecipes;
import com.hbm.hazard.HazardSystem;
import com.hbm.hazard.HazardRegistry;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.items.ModItems;
import com.hbm.render.misc.EnumSymbol;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.OreDictionary;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnJre;
import org.junit.jupiter.api.condition.JRE;

import static org.junit.jupiter.api.Assertions.*;

// Run in an isolated Java 8 process against the downgraded dev JAR.
@EnabledOnJre(JRE.JAVA_8)
class Gold198BlockTest {
    @BeforeAll
    static void bootstrap() throws Exception {
        AutoCrystallizerOreDictionaryTest.bootstrap();
        // The unrelated balefire recipes only require an antimatter cell ID.
        Fluids.AMAT = new FluidType("AMAT", 0x010101, 5, 0, 5, EnumSymbol.ANTIMATTER);
        MineralRecipes.register();
    }

    @Test
    void blockHasBothIsotopeAliases() {
        ItemStack block = new ItemStack(ModBlocks.block_au198);
        for (String alias : new String[]{"blockGold198", "blockAu198"}) {
            assertTrue(OreDictionary.getOres(alias).stream()
                    .anyMatch(stack -> OreDictionary.itemMatches(stack, block, false)), alias);
        }
    }

    @Test
    void blockHasConfiguredRadiationMultiplier() {
        double ingot = HazardSystem.getRawRadsFromStack(new ItemStack(ModItems.ingot_au198));
        assertTrue(ingot > 0);
        assertEquals(ingot * HazardRegistry.block / HazardRegistry.ingot,
                HazardSystem.getRawRadsFromBlock(ModBlocks.block_au198), 0.00001);
    }

    @Test
    void blockAndNineIngotsConvertBothWays() {
        InventoryCrafting grid = new InventoryCrafting(new Container() {
            @Override public boolean canInteractWith(EntityPlayer player) { return true; }
        }, 3, 3);
        grid.setInventorySlotContents(4, new ItemStack(ModBlocks.block_au198));
        assertCrafts(grid, new ItemStack(ModItems.ingot_au198, 9));
        for (int slot = 0; slot < 9; slot++) {
            grid.setInventorySlotContents(slot, new ItemStack(ModItems.ingot_au198));
        }
        assertCrafts(grid, new ItemStack(ModBlocks.block_au198));
        grid.setInventorySlotContents(8, ItemStack.EMPTY);
        assertFalse(ForgeRegistries.RECIPES.getValuesCollection().stream()
                .anyMatch(recipe -> recipe.matches(grid, null)
                        && ItemStack.areItemStacksEqual(recipe.getCraftingResult(grid), new ItemStack(ModBlocks.block_au198))));
    }

    private static void assertCrafts(InventoryCrafting grid, ItemStack expected) {
        for (IRecipe recipe : ForgeRegistries.RECIPES.getValuesCollection()) {
            if (recipe.matches(grid, null) && ItemStack.areItemStacksEqual(recipe.getCraftingResult(grid), expected)) return;
        }
        fail("Missing recipe for " + expected.getItem().getRegistryName() + " x" + expected.getCount());
    }
}
