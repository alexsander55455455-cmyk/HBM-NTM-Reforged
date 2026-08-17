package com.hbm.compat.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.PngSizeInfo;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.FMLClientHandler;
import org.apache.commons.compress.utils.IOUtils;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.function.Function;

@Pseudo
@Mixin(targets = "com.leafia.contents.gear.ntmfbottle.TextureAtlasSpriteMask", remap = false)
public abstract class MixinCursedAddonBottleTexture extends TextureAtlasSprite {

    @Shadow(remap = false)
    @Final
    public ResourceLocation mask;

    @Shadow(remap = false)
    @Final
    public ResourceLocation texture;

    @Shadow(remap = false)
    private int mipmap;

    protected MixinCursedAddonBottleTexture(String spriteName) {
        super(spriteName);
    }

    /**
     * Cursed Addon's original loader requires the potion mask and fluid texture
     * to have identical dimensions. Reforged fluid art is often a vertical
     * animation strip, so use its first square frame for the static bottle icon.
     *
     * @author HBM NTM Reforged
     * @reason Preserve the external addon's bottle renderer with animated fluids.
     */
    @Overwrite(remap = false)
    public boolean load(IResourceManager manager, ResourceLocation location,
                        Function<ResourceLocation, TextureAtlasSprite> textureGetter) {
        IResource maskResource = null;
        IResource fluidResource = null;
        try {
            maskResource = manager.getResource(mask);
            PngSizeInfo maskSize = PngSizeInfo.makeFromResource(maskResource);
            loadSprite(maskSize, false);

            fluidResource = manager.getResource(texture);
            PngSizeInfo fluidSize = PngSizeInfo.makeFromResource(fluidResource);
            if (maskSize.pngWidth != fluidSize.pngWidth
                    || fluidSize.pngHeight < maskSize.pngHeight
                    || fluidSize.pngHeight % maskSize.pngHeight != 0) {
                throw new IOException("Bottle fluid texture must contain square frames matching the potion mask");
            }

            IOUtils.closeQuietly(maskResource);
            IOUtils.closeQuietly(fluidResource);
            maskResource = manager.getResource(mask);
            fluidResource = manager.getResource(texture);

            mipmap = Minecraft.getMinecraft().getTextureMapBlocks().getMipmapLevels() + 1;
            hbm$loadFirstMaskedFrame(maskResource, fluidResource, mipmap);
            return false;
        } catch (RuntimeException | IOException e) {
            FMLClientHandler.instance().trackBrokenTexture(texture, e.getMessage());
            return true;
        } finally {
            IOUtils.closeQuietly(maskResource);
            IOUtils.closeQuietly(fluidResource);
        }
    }

    @Unique
    private void hbm$loadFirstMaskedFrame(IResource maskResource, IResource fluidResource,
                                          int mipmapLevels) throws IOException {
        BufferedImage maskImage = ImageIO.read(maskResource.getInputStream());
        BufferedImage fluidImage = ImageIO.read(fluidResource.getInputStream());
        int width = maskImage.getWidth();
        int height = maskImage.getHeight();
        int[][] frameData = new int[mipmapLevels][];
        frameData[0] = new int[width * height];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int maskPixel = maskImage.getRGB(x, y);
                int fluidPixel = fluidImage.getRGB(x, y);
                int red = ((maskPixel >>> 16 & 0xFF) * (fluidPixel >>> 16 & 0xFF)) / 255;
                int green = ((maskPixel >>> 8 & 0xFF) * (fluidPixel >>> 8 & 0xFF)) / 255;
                int blue = ((maskPixel & 0xFF) * (fluidPixel & 0xFF)) / 255;
                int alpha = ((maskPixel >>> 24 & 0xFF) * (fluidPixel >>> 24 & 0xFF)) / 255;
                frameData[0][y * width + x] = alpha << 24 | red << 16 | green << 8 | blue;
            }
        }

        framesTextureData.clear();
        framesTextureData.add(frameData);
        animationMetadata = null;
    }
}
