package com.hbm.inventory;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.fluid.ModFluids;
import com.hbm.inventory.material.Mats;
import com.hbm.items.ItemEnums;
import com.hbm.items.ModItems;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.main.CraftingManager;
import com.hbm.main.MainRegistry;
import com.hbm.main.MaterialRegistry;
import com.hbm.testutil.ForgeTestBootstrap;
import net.minecraft.block.Block;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.DummyModContainer;
import net.minecraftforge.fml.common.LoadController;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModMetadata;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.OreDictionary;
import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnJre;
import org.junit.jupiter.api.condition.JRE;

import java.lang.reflect.Field;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;

// Run in an isolated Java 8 process against the downgraded dev JAR.
@EnabledOnJre(JRE.JAVA_8)
class AutoCrystallizerOreDictionaryTest {

    @BeforeAll
    static void bootstrap() throws Exception {
        ForgeTestBootstrap.ensureServerSide();
        if (!Bootstrap.isRegistered()) Bootstrap.register();
        ModMetadata metadata = new ModMetadata();
        metadata.modId = "hbm";
        metadata.name = "HBM test";
        metadata.version = "test";
        DummyModContainer mod = new DummyModContainer(metadata);
        Field controller = Loader.class.getDeclaredField("modController");
        controller.setAccessible(true);
        controller.set(Loader.instance(), new LoadController(Loader.instance()));
        Field namedMods = Loader.class.getDeclaredField("namedMods");
        namedMods.setAccessible(true);
        namedMods.set(Loader.instance(), Collections.singletonMap("hbm", mod));
        Loader.instance().setActiveModContainer(mod);
        MainRegistry.logger = LogManager.getLogger("AutoCrystallizerOreDictionaryTest");
        HBMSoundHandler.init();
        MaterialRegistry.init();
        ModFluids.init();
        ModBlocks.registerBlocks();
        for (Block block : ModBlocks.ALL_BLOCKS) {
            ForgeRegistries.ITEMS.register(new ItemBlock(block).setRegistryName(block.getRegistryName()));
        }
        for (Item item : ModItems.ALL_ITEMS) ForgeRegistries.ITEMS.register(item);
        CraftingManager.hack = new RegistryEvent.Register<>(new ResourceLocation("minecraft:recipes"), ForgeRegistries.RECIPES);
        OreDictManager.registerOres();
    }

    @Test
    void regularRareEarthHasMatchingCrystal() {
        assertMapping(ModBlocks.ore_rare, "RareEarth", ModItems.crystal_rare);
    }

    @Test
    void gneissRareEarthHasMatchingCrystal() {
        assertMapping(ModBlocks.ore_gneiss_rare, "RareEarth", ModItems.crystal_rare);
    }

    @Test
    void trixiteHasMatchingCrystal() {
        assertMapping(ModBlocks.ore_tikite, "Trixite", ModItems.crystal_trixite);
    }

    @Test
    void netherPhosphorusHasMatchingCrystal() {
        assertMapping(ModBlocks.ore_nether_fire, "RedPhosphorus", ModItems.crystal_phosphorus);
    }

    @Test
    void copperStillHasMatchingCrystal() {
        assertMapping(ModBlocks.ore_copper, "Copper", ModItems.crystal_copper);
    }

    @Test
    void netherUraniumRetainsItsBaseOreMapping() {
        assertMapping(ModBlocks.ore_nether_uranium, "Uranium", ModItems.crystal_uranium);
    }

    @Test
    void cinnabarRetainsItsExistingCrystalAlias() {
        assertMapping(ModBlocks.ore_cinnabar, "Cinnabar", ModItems.cinnabar);
    }

    @Test
    void deepCinnabarRetainsItsExistingCrystalAlias() {
        assertMapping(ModBlocks.ore_depth_cinnabar, "Cinnabar", ModItems.cinnabar);
    }

    @Test
    void rareEarthFragmentsStillCraftTheExistingOreChunk() {
        ItemStack fragment = Mats.MAT_RAREEARTH.make(ModItems.bedrock_ore_fragment);
        for (IRecipe recipe : ForgeRegistries.RECIPES) {
            if (recipe.getIngredients().size() != 9) continue;
            if (!recipe.getIngredients().stream().allMatch(input -> input.apply(fragment))) continue;
            assertSame(ModItems.chunk_ore, recipe.getRecipeOutput().getItem());
            assertEquals(ItemEnums.EnumChunkType.RARE.ordinal(), recipe.getRecipeOutput().getMetadata());
            assertEquals(1, recipe.getRecipeOutput().getCount());
            return;
        }
        fail("Missing rare-earth fragment conversion recipe");
    }

    private static void assertMapping(Block ore, String suffix, Item crystal) {
        assertTrue(contains(OreDictionary.getOres("ore" + suffix, false), new ItemStack(ore)),
                "Missing ore mapping: " + ore.getRegistryName());
        assertTrue(contains(OreDictionary.getOres("crystal" + suffix, false), new ItemStack(crystal)),
                "Missing crystal mapping: crystal" + suffix);
    }

    private static boolean contains(Iterable<ItemStack> entries, ItemStack expected) {
        for (ItemStack entry : entries) {
            if (OreDictionary.itemMatches(entry, expected, false)) return true;
        }
        return false;
    }
}
