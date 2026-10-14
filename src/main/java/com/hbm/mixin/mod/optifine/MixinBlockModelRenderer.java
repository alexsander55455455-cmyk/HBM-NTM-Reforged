package com.hbm.mixin.mod.optifine;

import com.hbm.render.model.IHbmBakedQuad;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.BlockModelRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.block.model.BakedQuad;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BlockModelRenderer.class)
public abstract class MixinBlockModelRenderer {
    // OptiFine overwrites the baked lightmap after appending the quad to the world buffer.
    @Dynamic
    @WrapOperation(method = "renderQuadsFlat", remap = false,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/BufferBuilder;putBrightness4(IIII)V", remap = true),
            require = 1)
    private void hbm$preserveEmissiveLightmap(BufferBuilder buffer, int a, int b, int c, int d,
                                             Operation<Void> original, @Local BakedQuad quad) {
        boolean emissive = false;
        if (quad instanceof IHbmBakedQuad && quad.getFormat().hasUvOffset(1)) {
            int[] data = quad.getVertexData();
            int stride = data.length / 4;
            int offset = quad.getFormat().getUvOffsetById(1) / 4;
            emissive = true;
            for (int vertex = 0; vertex < 4; vertex++) {
                if (data[vertex * stride + offset] != 0x00F000F0) emissive = false;
            }
        }
        original.call(buffer, emissive ? 0x00F000F0 : a, emissive ? 0x00F000F0 : b,
                emissive ? 0x00F000F0 : c, emissive ? 0x00F000F0 : d);
    }
}
