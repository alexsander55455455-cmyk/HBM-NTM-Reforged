package com.hbm.inventory.recipes;

import com.hbm.blocks.fluid.ModFluids;
import com.hbm.items.ModItems;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.main.MainRegistry;
import com.hbm.main.MaterialRegistry;
import com.hbm.testutil.ForgeTestBootstrap;
import com.hbm.tileentity.machine.TileEntityMachineAmmoPress;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnJre;
import org.junit.jupiter.api.condition.JRE;

import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Forge enum registration requires Java 8; run against the downgraded dev JAR.
@EnabledOnJre(JRE.JAVA_8)
class EEAmmoPressRecipesTest {

    @BeforeAll
    static void bootstrap() {
        ForgeTestBootstrap.ensureServerSide();
        MainRegistry.logger = LogManager.getLogger("AmmoPressRecipesTest");
        if (!Bootstrap.isRegistered()) Bootstrap.register();
        HBMSoundHandler.init();
        MaterialRegistry.init();
        ModFluids.init();
    }

    @BeforeEach
    void registerRecipes() {
        AmmoPressRecipes.recipes.clear();
        EEAmmoPressRecipes.register(AmmoPressRecipes.recipes);
    }

    @Test
    void allLegacyRecipesProvideNineInputSlots() {
        assertFalse(AmmoPressRecipes.recipes.isEmpty());
        String invalid = AmmoPressRecipes.recipes.stream()
                .filter(recipe -> recipe.input.length != 9)
                .map(recipe -> recipe.output.getItem().getRegistryName() + ": " + recipe.input.length)
                .collect(Collectors.joining(", "));
        assertTrue(invalid.isEmpty(), invalid);
    }

    @Test
    void steelAssemblyProducesOneLegacyBullet() {
        assertAssemblyRecipe(ModItems.assembly_steel, ModItems.gun_revolver_ammo);
    }

    @Test
    void nightmareAssemblyProducesOneLegacyBullet() {
        assertAssemblyRecipe(ModItems.assembly_nightmare, ModItems.gun_revolver_nightmare_ammo);
    }

    @Test
    void legacyRecipeAllowsCheckingTheEmptyNinthSlot() {
        TileEntityMachineAmmoPress press = new TileEntityMachineAmmoPress();
        press.selectedRecipe = recipeIndex(ModItems.gun_revolver_ammo);

        assertFalse(press.isItemValidForSlot(8, new ItemStack(ModItems.assembly_steel)));
    }

    private static int recipeIndex(Item output) {
        for (int i = 0; i < AmmoPressRecipes.recipes.size(); i++) {
            if (AmmoPressRecipes.recipes.get(i).output.getItem() == output) return i;
        }
        throw new AssertionError("Missing ammo press recipe for " + output.getRegistryName());
    }

    private static void assertAssemblyRecipe(Item assembly, Item output) {
        TileEntityMachineAmmoPress press = new TileEntityMachineAmmoPress();
        press.selectedRecipe = recipeIndex(output);
        press.inventory.setStackInSlot(3, new ItemStack(assembly));

        assertTrue(press.hasIngredients(AmmoPressRecipes.recipes.get(press.selectedRecipe)));
        press.performRecipe();

        assertTrue(press.inventory.getStackInSlot(3).isEmpty());
        assertSame(output, press.inventory.getStackInSlot(9).getItem());
        assertEquals(1, press.inventory.getStackInSlot(9).getCount());
        assertFalse(press.hasIngredients(AmmoPressRecipes.recipes.get(press.selectedRecipe)));
    }
}
