package com.hbm.render.model;

import com.hbm.mixin.mod.optifine.MixinBlockModelRenderer;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import static org.junit.jupiter.api.Assertions.*;

class OptifineEmissiveLightmapTest {
    private static final int FULL_BRIGHT = 0x00F000F0;
    private static final int[] WORLD_LIGHT = {0x00200030, 0x00300040, 0x00400050, 0x00500060};

    @Test void optifineOverwriteWouldDimTheBulbWithoutTheHook() {
        BakedQuad quad = quad(true, true);
        BufferBuilder buffer = buffer(quad);
        buffer.putBrightness4(WORLD_LIGHT[0], WORLD_LIGHT[1], WORLD_LIGHT[2], WORLD_LIGHT[3]);
        assertBrightness(buffer, WORLD_LIGHT);
    }

    @Test void emissiveHbmBulbRetainsFullBrightnessInTheWorldBuffer() throws Exception {
        BakedQuad quad = quad(true, true);
        BufferBuilder buffer = buffer(quad);
        invoke(buffer, quad);
        assertBrightness(buffer, new int[]{FULL_BRIGHT, FULL_BRIGHT, FULL_BRIGHT, FULL_BRIGHT});
    }

    @Test void unlitBulbAndMetalFrameRetainNormalWorldLighting() throws Exception {
        BakedQuad quad = quad(true, false);
        BufferBuilder buffer = buffer(quad);
        invoke(buffer, quad);
        assertBrightness(buffer, WORLD_LIGHT);
    }

    @Test void hookDoesNotAlterOtherModsQuads() throws Exception {
        BakedQuad quad = quad(false, true);
        BufferBuilder buffer = buffer(quad);
        invoke(buffer, quad);
        assertBrightness(buffer, WORLD_LIGHT);
    }

    private static BakedQuad quad(boolean hbm, boolean lit) {
        int stride = DefaultVertexFormats.BLOCK.getIntegerSize();
        int[] data = new int[stride * 4];
        int offset = DefaultVertexFormats.BLOCK.getUvOffsetById(1) / 4;
        for (int vertex = 0; vertex < 4; vertex++) data[vertex * stride + offset] = lit ? FULL_BRIGHT : 0;
        return hbm ? new HbmBakedQuad(data, -1, EnumFacing.NORTH, null, DefaultVertexFormats.BLOCK)
                : new BakedQuad(data, -1, EnumFacing.NORTH, null, false, DefaultVertexFormats.BLOCK);
    }

    private static BufferBuilder buffer(BakedQuad quad) {
        BufferBuilder buffer = new BufferBuilder(128);
        buffer.begin(7, DefaultVertexFormats.BLOCK);
        buffer.addVertexData(quad.getVertexData());
        return buffer;
    }

    private static void invoke(BufferBuilder buffer, BakedQuad quad) throws Exception {
        Method hook = MixinBlockModelRenderer.class.getDeclaredMethod("hbm$preserveEmissiveLightmap",
                BufferBuilder.class, int.class, int.class, int.class, int.class, Operation.class, BakedQuad.class);
        hook.setAccessible(true);
        Operation<Void> original = args -> {
            ((BufferBuilder) args[0]).putBrightness4((Integer) args[1], (Integer) args[2], (Integer) args[3], (Integer) args[4]);
            return null;
        };
        hook.invoke(new MixinBlockModelRenderer() { }, buffer, WORLD_LIGHT[0], WORLD_LIGHT[1], WORLD_LIGHT[2], WORLD_LIGHT[3], original, quad);
    }

    private static void assertBrightness(BufferBuilder buffer, int[] expected) {
        ByteBuffer data = buffer.getByteBuffer();
        int offset = DefaultVertexFormats.BLOCK.getUvOffsetById(1);
        int stride = DefaultVertexFormats.BLOCK.getSize();
        for (int vertex = 0; vertex < 4; vertex++) assertEquals(expected[vertex], data.getInt(vertex * stride + offset));
    }
}
