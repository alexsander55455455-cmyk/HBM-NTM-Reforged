package com.hbm.blocks.generic;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.PlantEnums.EnumFlowerPlantType;
import com.hbm.render.block.BlockBakeFrame;
import com.hbmspace.enums.EnumAddonFlowerPlantTypes;
import net.minecraft.block.state.IBlockState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class WildPlantRenderingTest {
    @BeforeAll static void bootstrap() throws Exception {
        java.lang.reflect.Method method = Class.forName("com.hbm.inventory.AutoCrystallizerOreDictionaryTest")
                .getDeclaredMethod("bootstrap");
        method.setAccessible(true);
        method.invoke(null);
        EnumAddonFlowerPlantTypes.init();
    }

    @Test void flowersAndTallPlantsKeepTheirUntintedModels() throws Exception {
        Field framesField = BlockMeta.class.getDeclaredField("blockFrames");
        framesField.setAccessible(true);
        for (BlockPlantEnumMeta<?> plant : new BlockPlantEnumMeta<?>[]{
                (BlockNTMFlower) ModBlocks.plant_flower, (BlockTallPlant) ModBlocks.plant_tall}) {
            assertNull(plant.getBlockColorHandler(), "The added biome color handler must be removed");
            for (BlockBakeFrame frame : (BlockBakeFrame[]) framesField.get(plant)) {
                assertEquals("minecraft:block/cross", frame.getBaseModelLocation().toString());
            }
        }
    }

    @Test void addedPlantsKeepTheirMetadataAndTextureInsteadOfFallingBackToFoxglove() throws Exception {
        BlockNTMFlower flower = (BlockNTMFlower) ModBlocks.plant_flower;
        Field countField = BlockMeta.class.getDeclaredField("META_COUNT");
        countField.setAccessible(true);
        assertEquals(8, countField.getShort(flower), "The primitive variant count must match the eight plant types");
        Field framesField = BlockMeta.class.getDeclaredField("blockFrames");
        framesField.setAccessible(true);
        BlockBakeFrame[] frames = (BlockBakeFrame[]) framesField.get(flower);
        java.util.Map<IBlockState, net.minecraft.client.renderer.block.model.ModelResourceLocation> variants =
                flower.getStateMapper(flower.getRegistryName()).putStateModelLocations(flower);
        for (EnumFlowerPlantType type : flower.blockEnum) {
            IBlockState state = flower.getDefaultState().withProperty(BlockMeta.META, type.ordinal());
            assertEquals("meta=" + type.ordinal(), variants.get(state).getVariant());
            assertEquals("plant_flower_" + type.name().toLowerCase(java.util.Locale.ROOT),
                    frames[type.ordinal()].getTexturePath(0));
        }
        assertEquals("plant_flower_strawberry", frames[6].getTexturePath(0));
        assertEquals("plant_flower_mint", frames[7].getTexturePath(0));
    }
}
