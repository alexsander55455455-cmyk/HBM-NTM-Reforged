package com.hbm.inventory;

import com.hbm.config.GeneralConfig;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.*;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemBlueprints;
import com.hbm.items.machine.ItemDrive.EnumDriveType;
import net.minecraft.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.math.BigInteger;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

// Run in an isolated Java 8 process against the downgraded dev JAR.
class DataConsumerRecipeTest {
    @BeforeAll static void bootstrap() throws Exception {
        AutoCrystallizerOreDictionaryTest.bootstrap();
        com.hbm.main.MainRegistry.proxy = new com.hbm.main.ServerProxy();
        com.hbm.main.MainRegistry.configHbmDir = java.nio.file.Files.createTempDirectory("hbm-data-consumer-test").toFile();
        com.hbm.main.MainRegistry.configHbmDir.deleteOnExit();
        com.hbm.potion.HbmPotion.preinit();
        com.hbm.potion.HbmPotion.registerPotions(net.minecraftforge.fml.common.registry.ForgeRegistries.POTIONS);
        Fluids.init();
        for(java.io.File file : com.hbm.main.MainRegistry.configHbmDir.listFiles()) file.deleteOnExit();
        OreDictManager.registerGroups();
        OreDictManager groups = new OreDictManager();
        for(String name : net.minecraftforge.oredict.OreDictionary.getOreNames())
            for(ItemStack stack : new ArrayList<>(net.minecraftforge.oredict.OreDictionary.getOres(name)))
                groups.onRegisterOre(new net.minecraftforge.oredict.OreDictionary.OreRegisterEvent(name, stack));
    }
    static void defaults(boolean expensive, boolean mode528) {
        GeneralConfig.enableExpensiveMode = expensive; GeneralConfig.enable528 = mode528;
        GenericRecipes.clearPools();
        AssemblyMachineRecipes.INSTANCE.deleteRecipes(); AssemblyMachineRecipes.INSTANCE.registerDefaults();
        PlasmaForgeRecipes.INSTANCE.deleteRecipes(); PlasmaForgeRecipes.INSTANCE.registerDefaults();
        new AnnihilatorRecipes().deleteRecipes(); new AnnihilatorRecipes().registerDefaults();
    }
    private static GenericRecipe recipe(String name) {
        return name.startsWith("ass.") ? AssemblyMachineRecipes.INSTANCE.recipeNameMap.get(name) : PlasmaForgeRecipes.INSTANCE.recipeNameMap.get(name);
    }
    private static boolean drive(AStack input, EnumDriveType type) {
        List<ItemStack> stacks = input.getStackList();
        return stacks.size() == 1 && stacks.get(0).getItem() == ModItems.drive && stacks.get(0).getItemDamage() == type.ordinal();
    }
    private static void variant(String name, EnumDriveType type, int count, boolean replacesCoin) {
        GenericRecipe old = recipe(name), added = recipe(name + "_data");
        assertNotNull(old); assertNotNull(added);
        assertEquals(old.duration, added.duration); assertEquals(old.power, added.power);
        assertArrayEquals(old.getPools(), added.getPools());
        assertTrue(ItemStack.areItemStacksEqual(old.outputItem[0].getSingle(), added.outputItem[0].getSingle()));
        assertEquals(old.inputItem.length + (replacesCoin ? 0 : 1), added.inputItem.length);
        for(int i = 0; i < old.inputItem.length - (replacesCoin ? 1 : 0); i++) {
            assertEquals(old.inputItem[i].stacksize, added.inputItem[i].stacksize);
            List<ItemStack> a = old.inputItem[i].getStackList(), b = added.inputItem[i].getStackList();
            assertEquals(a.size(), b.size());
            for(int j = 0; j < a.size(); j++) assertTrue(ItemStack.areItemStacksEqual(a.get(j), b.get(j)));
        }
        AStack last = added.inputItem[added.inputItem.length - 1];
        assertTrue(drive(last, type)); assertEquals(count, last.stacksize);
        if(replacesCoin) {
            assertSame(ModItems.coin_ufo, old.inputItem[old.inputItem.length - 1].getStackList().get(0).getItem());
            assertEquals(1, old.inputItem[old.inputItem.length - 1].stacksize);
            assertEquals(((PlasmaForgeRecipe)old).ignitionTemp, ((PlasmaForgeRecipe)added).ignitionTemp);
        }
    }
    @Test void additiveVariantsPreserveExistingCostsOutputsTimingAndPoolsInAllModes() {
        for(boolean expensive : new boolean[]{false, true}) for(boolean mode528 : new boolean[]{false, true}) {
            defaults(expensive, mode528);
            variant("ass.fusioncore", EnumDriveType.FLASH_PARTICLESIM, expensive ? 16 : 4, false);
            variant("ass.soyuz", EnumDriveType.FLASH_FLIGHTSIM, 4, false);
            variant("ass.lander", EnumDriveType.FLASH_FLIGHTSIM, 8, false);
            variant("plsm.gerald", EnumDriveType.DISK_ORBITDATA_PROCESSED, expensive ? 64 : 16, true);
        }
    }
    @Test void onlyCompatibleDriveMetadataMatchesEachNewRecipe() {
        defaults(false, false);
        for(String name : new String[]{"ass.fusioncore_data", "ass.soyuz_data", "ass.lander_data", "plsm.gerald_data"}) {
            GenericRecipe r = recipe(name); AStack input = r.inputItem[r.inputItem.length - 1];
            int expected = input.getStackList().get(0).getItemDamage();
            for(EnumDriveType type : EnumDriveType.values())
                assertEquals(type.ordinal() == expected, input.matchesRecipe(new ItemStack(ModItems.drive, input.stacksize, type.ordinal()), true), name + ": " + type);
        }
    }
    @Test void legacyRecipesStillRequireNoDataDrives() {
        for(boolean expensive : new boolean[]{false, true}) {
            defaults(expensive, false);
            for(String name : new String[]{"ass.fusioncore", "ass.soyuz", "ass.lander", "plsm.gerald"})
                for(AStack input : recipe(name).inputItem) for(ItemStack choice : input.getStackList()) assertNotSame(ModItems.drive, choice.getItem());
        }
    }
    @Test void newRecipesRespectMachineInputSlotLimitsInBothCostModes() {
        for(boolean expensive : new boolean[]{false, true}) {
            defaults(expensive, false);
            for(String name : new String[]{"ass.fusioncore_data", "ass.soyuz_data", "ass.lander_data", "plsm.gerald_data"})
                assertTrue(recipe(name).inputItem.length <= 12, name);
        }
    }
    @Test void blueprintPoolsContainOldAndDataVariantsWithoutDuplicates() {
        defaults(false, true);
        for(String[] group : new String[][]{{"528.tcalloy", "ass.fusioncore", "ass.fusioncore_data"}, {"discover.soyuz", "ass.soyuz", "ass.soyuz_data", "ass.lander", "ass.lander_data"}, {"discover.gerald", "plsm.gerald", "plsm.gerald_data"}}) {
            List<String> pool = GenericRecipes.blueprintPools.get(group[0]); assertNotNull(pool);
            for(int i = 1; i < group.length; i++) assertEquals(1, Collections.frequency(pool, group[i]));
        }
    }
    @Test void normalModeDoesNotAddAnnihilatorMilestones() {
        defaults(false, false); assertTrue(AnnihilatorRecipes.recipes.isEmpty());
    }
    @Test void annihilatorPaysOnceAtSixtyFourAndUnlocksTheActualSoyuzPool() {
        defaults(false, true); ItemStack drive = new ItemStack(ModItems.drive, 1, EnumDriveType.FLASH_FLIGHTSIM.ordinal());
        assertNull(AnnihilatorRecipes.getHighestPayoutFromStack(drive, BigInteger.ZERO, BigInteger.valueOf(63)));
        ItemStack blueprint = AnnihilatorRecipes.getHighestPayoutFromStack(drive, BigInteger.valueOf(63), BigInteger.valueOf(64));
        assertNotNull(blueprint); assertSame(ModItems.blueprints, blueprint.getItem());
        String pool = ItemBlueprints.grabPool(blueprint); assertEquals("discover.soyuz", pool);
        for(String name : new String[]{"ass.soyuz", "ass.soyuz_data", "ass.lander", "ass.lander_data"}) assertTrue(Arrays.asList(recipe(name).getPools()).contains(pool));
        assertNull(AnnihilatorRecipes.getHighestPayoutFromStack(drive, BigInteger.valueOf(64), BigInteger.valueOf(65)));
    }
    @Test void annihilatorRejectsEveryOtherDriveMetadata() {
        defaults(true, true);
        for(EnumDriveType type : EnumDriveType.values()) if(type != EnumDriveType.FLASH_FLIGHTSIM)
            assertNull(AnnihilatorRecipes.getHighestPayoutFromStack(new ItemStack(ModItems.drive, 1, type.ordinal()), BigInteger.ZERO, BigInteger.valueOf(64)));
    }
}
