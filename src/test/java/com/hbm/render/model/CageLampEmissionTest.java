package com.hbm.render.model;

import com.hbm.blocks.BlockEnums.LightType;
import com.hbm.blocks.machine.Spotlight;
import com.hbm.render.loader.HFRWavefrontObject;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.EnumFacing;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.io.InputStream;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class CageLampEmissionTest {
    @BeforeAll static void bootstrap() throws Exception {
        java.lang.reflect.Method bootstrap = Class.forName("com.hbm.inventory.AutoCrystallizerOreDictionaryTest")
                .getDeclaredMethod("bootstrap");
        bootstrap.setAccessible(true);
        bootstrap.invoke(null);
    }

    @Test void onlyTheLitBulbIsFullBrightForEveryMountingDirection() throws Exception {
        HFRWavefrontObject mesh = mesh();
        TextureAtlasSprite sprite = new TestSprite();
        Spotlight on = new Spotlight("emission_test_on", Material.IRON, 0, LightType.INCANDESCENT, true);
        Spotlight off = new Spotlight("emission_test_off", Material.IRON, 0, LightType.INCANDESCENT, false);
        BlockSpotlightBakedModel litModel = BlockSpotlightBakedModel.forBlock(mesh, sprite, LightType.INCANDESCENT);
        BlockSpotlightBakedModel darkModel = BlockSpotlightBakedModel.forBlock(mesh, sprite, LightType.INCANDESCENT);
        for (EnumFacing facing : EnumFacing.VALUES) {
            List<BakedQuad> lit = litModel.getQuads(on.getDefaultState().withProperty(Spotlight.FACING, facing), null, 0);
            List<BakedQuad> dark = darkModel.getQuads(off.getDefaultState().withProperty(Spotlight.FACING, facing), null, 0);
            assertEquals(58, lit.size(), "The complete authored mesh must remain present");
            assertEquals(lit.size(), dark.size());
            int glowingFaces = 0;
            for (int i = 0; i < lit.size(); i++) {
                BakedQuad light = lit.get(i), unlit = dark.get(i);
                int stride = light.getFormat().getIntegerSize();
                int lightOffset = light.getFormat().getUvOffsetById(1) / 4;
                int colorOffset = light.getFormat().getColorOffset() / 4;
                boolean glowing = light.getVertexData()[lightOffset] == 0x00F000F0;
                if (glowing) glowingFaces++;
                for (int vertex = 0; vertex < 4; vertex++) {
                    int base = vertex * stride;
                    assertEquals(0, unlit.getVertexData()[base + lightOffset]);
                    for (int coordinate = 0; coordinate < 3; coordinate++) {
                        assertEquals(unlit.getVertexData()[base + coordinate], light.getVertexData()[base + coordinate],
                                "Lighting must not move the model");
                    }
                    if (glowing) {
                        assertEquals(0x00F000F0, light.getVertexData()[base + lightOffset]);
                        assertEquals(-1, light.getVertexData()[base + colorOffset]);
                    } else {
                        assertArrayEquals(unlit.getVertexData(), light.getVertexData(), "The metal frame keeps normal shading");
                    }
                }
            }
            assertEquals(10, glowingFaces, "Only the ten triangles of the bulb emit light");
        }
    }

    @Test void inventoryModelRetainsTheFrameAndBulb() throws Exception {
        BlockSpotlightBakedModel item = BlockSpotlightBakedModel.forItem(mesh(), new TestSprite(), LightType.INCANDESCENT);
        assertEquals(58, item.getQuads(null, null, 0).size());
        assertTrue(item.getQuads(null, EnumFacing.UP, 0).isEmpty());
    }

    private static HFRWavefrontObject mesh() throws Exception {
        try (InputStream stream = CageLampEmissionTest.class.getResourceAsStream("/assets/hbm/models/lights/cage_lamp.obj")) {
            assertNotNull(stream);
            return new HFRWavefrontObject("cage_lamp.obj", stream);
        }
    }
    private static class TestSprite extends TextureAtlasSprite {
        TestSprite() { super("cage_lamp_test"); }
        @Override public float getInterpolatedU(double u) { return (float) u / 16; }
        @Override public float getInterpolatedV(double v) { return (float) v / 16; }
    }
}
