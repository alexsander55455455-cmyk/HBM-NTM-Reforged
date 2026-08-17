package com.hbm.compat.mixin;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.data.AnimationFrame;
import net.minecraft.client.resources.data.AnimationMetadataSection;
import net.minecraft.client.resources.data.IMetadataSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Supplies metadata shipped in Reforged for non-square textures owned by Leafia's resource pack. */
@Mixin(TextureAtlasSprite.class)
public abstract class MixinTextureAtlasSpriteCursedAddonMetadata {

    @Shadow
    public abstract String getIconName();

    @ModifyVariable(method = "loadSprite", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private boolean hbm$acceptLeafiaAnimationDimensions(boolean hasAnimationMetadata) {
        return hasAnimationMetadata || hbm$requiresAnimationMetadata(getIconName());
    }

    @Redirect(
            method = "loadSpriteFrames",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/resources/IResource;getMetadata(Ljava/lang/String;)Lnet/minecraft/client/resources/data/IMetadataSection;"
            )
    )
    private IMetadataSection hbm$provideLeafiaAnimationMetadata(IResource resource, String sectionName) throws IOException {
        IMetadataSection metadata = resource.getMetadata(sectionName);
        if (metadata != null || !"animation".equals(sectionName)) return metadata;

        String path = resource.getResourceLocation().toString();
        if (path.equals("leafia:textures/blocks/forgefluid/corium_bale_still_e.png")) {
            return hbm$pingPongFrames(2);
        }
        if (path.equals("leafia:textures/blocks/forgefluid/osmiridium_still_e.png")) {
            return hbm$pingPongFrames(5);
        }
        if (path.equals("leafia:textures/blocks/forgefluid/osmiridium_flow_e.png")) {
            return new AnimationMetadataSection(Collections.emptyList(), -1, -1, 3, false);
        }
        if (path.matches("leafia:textures/models/leafia/elevator/s6/indicator/[0-9]_e\\.png")) {
            return new AnimationMetadataSection(Collections.singletonList(new AnimationFrame(0, -1)), 29, 57, 1, false);
        }
        return null;
    }

    private static boolean hbm$requiresAnimationMetadata(String iconName) {
        if (iconName == null) return false;
        if (iconName.equals("leafia:blocks/forgefluid/corium_bale_still_e")
                || iconName.equals("leafia:blocks/forgefluid/osmiridium_still_e")
                || iconName.equals("leafia:blocks/forgefluid/osmiridium_flow_e")) {
            return true;
        }
        return iconName.matches("leafia:models/leafia/elevator/s6/indicator/[0-9]_e");
    }

    private static AnimationMetadataSection hbm$pingPongFrames(int frameTime) {
        List<AnimationFrame> frames = new ArrayList<>(38);
        for (int frame = 0; frame <= 19; frame++) frames.add(new AnimationFrame(frame, -1));
        for (int frame = 18; frame >= 1; frame--) frames.add(new AnimationFrame(frame, -1));
        return new AnimationMetadataSection(frames, -1, -1, frameTime, false);
    }
}
