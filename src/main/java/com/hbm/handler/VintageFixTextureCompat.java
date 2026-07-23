package com.hbm.handler;

import com.google.common.collect.ImmutableSet;
import com.hbm.main.MainRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.PngSizeInfo;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.data.AnimationMetadataSection;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Prevents VintageFix's broad resource scan from treating direct UV textures as atlas sprites.
 */
@SideOnly(Side.CLIENT)
public final class VintageFixTextureCompat {

    public static final VintageFixTextureCompat INSTANCE = new VintageFixTextureCompat();
    // OptiFine promotes these direct-UV companions to strong atlas entries before our event handler runs.
    private static final Set<String> DIRECT_UV_EMISSIVE_TEXTURES = ImmutableSet.of(
            "hbm:models/bombs/fstbmb_e",
            "hbm:models/deco/modelbroadcaster_e",
            "hbm:models/deco/modelradioreceiver_e",
            "hbm:models/machines/breeder_e",
            "hbm:models/machines/crane_console_e",
            "hbm:models/machines/mining_laser_laser_e",
            "hbm:models/machines/rbmk_control_e",
            "hbm:models/projectiles/baleflare_e",
            "hbm:models/turrets/railgun_main_e",
            "hbm:models/weapons/bflauncher_e",
            "hbm:models/weapons/modelempray_e",
            "hbm:models/weapons/modelzomg_e");

    private Field weakRegisteredSpritesField;
    private Field mapRegisteredSpritesField;
    private boolean reflectionReady;
    private boolean disabled;

    private VintageFixTextureCompat() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onTextureStitchPre(TextureStitchEvent.Pre event) {
        if (disabled || !prepareReflection()) {
            return;
        }

        try {
            Object weakValue = weakRegisteredSpritesField.get(event.getMap());
            Object registeredValue = mapRegisteredSpritesField.get(event.getMap());
            if (!(weakValue instanceof Set<?>) || !(registeredValue instanceof Map<?, ?>)) {
                disable("VintageFix texture fields have unexpected runtime types", null);
                return;
            }

            @SuppressWarnings("unchecked")
            Set<Object> weakSprites = (Set<Object>) weakValue;
            @SuppressWarnings("unchecked")
            Map<Object, Object> registeredSprites = (Map<Object, Object>) registeredValue;
            List<String> weakKeys = validateAndCopyKeys(weakSprites, registeredSprites);
            if (weakKeys == null) {
                return;
            }

            IResourceManager resourceManager = Minecraft.getMinecraft().getResourceManager();
            List<String> invalidKeys = new ArrayList<>();
            int unavailable = 0;
            int nonSquare = 0;

            for (String key : weakKeys) {
                TextureAtlasSprite sprite = (TextureAtlasSprite) registeredSprites.get(key);
                WeakTextureStatus status = inspectWeakTexture(resourceManager, new ResourceLocation(key), sprite);
                if (status == WeakTextureStatus.UNAVAILABLE) {
                    invalidKeys.add(key);
                    unavailable++;
                } else if (status == WeakTextureStatus.NON_SQUARE_WITHOUT_ANIMATION) {
                    invalidKeys.add(key);
                    nonSquare++;
                }
            }

            for (String key : invalidKeys) {
                if (weakSprites.remove(key)) {
                    registeredSprites.remove(key);
                }
            }

            int directUvEmissive = removeDirectUvEmissiveTextures(resourceManager, registeredSprites);
            int skipped = invalidKeys.size() + directUvEmissive;
            if (skipped > 0) {
                MainRegistry.logger.info(
                        "VintageFix compatibility skipped {} impossible atlas candidates ({} unavailable weak paths, {} non-square weak textures, {} direct-UV emissive textures)",
                        skipped, unavailable, nonSquare, directUvEmissive);
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            disable("Could not inspect VintageFix weak atlas registrations", e);
        }
    }

    private boolean prepareReflection() {
        if (reflectionReady) {
            return true;
        }

        try {
            weakRegisteredSpritesField = findWeakRegisteredSpritesField();
            mapRegisteredSpritesField = ObfuscationReflectionHelper.findField(TextureMap.class, "field_110574_e");
            weakRegisteredSpritesField.setAccessible(true);
            mapRegisteredSpritesField.setAccessible(true);
            reflectionReady = true;
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            disable("VintageFix weak atlas fields were not found", e);
            return false;
        }
    }

    private Field findWeakRegisteredSpritesField() throws NoSuchFieldException {
        try {
            return TextureMap.class.getDeclaredField("weakRegisteredSprites");
        } catch (NoSuchFieldException ignored) {
            for (Field field : TextureMap.class.getDeclaredFields()) {
                String name = field.getName().toLowerCase(Locale.ROOT);
                if (Set.class.isAssignableFrom(field.getType()) && name.contains("weak") && name.contains("sprite")) {
                    return field;
                }
            }
            throw new NoSuchFieldException("weakRegisteredSprites");
        }
    }

    private List<String> validateAndCopyKeys(Set<Object> weakSprites, Map<Object, Object> registeredSprites) {
        List<String> keys = new ArrayList<>(weakSprites.size());
        for (Object rawKey : weakSprites) {
            if (!(rawKey instanceof String)) {
                disable("VintageFix weak atlas set contains a non-string key", null);
                return null;
            }

            Object sprite = registeredSprites.get(rawKey);
            if (!(sprite instanceof TextureAtlasSprite)) {
                disable("VintageFix weak atlas entry has an unexpected sprite type", null);
                return null;
            }
            keys.add((String) rawKey);
        }
        return keys;
    }

    private int removeDirectUvEmissiveTextures(
            IResourceManager resourceManager, Map<Object, Object> registeredSprites) {
        int removed = 0;
        for (String key : DIRECT_UV_EMISSIVE_TEXTURES) {
            Object rawSprite = registeredSprites.get(key);
            if (!(rawSprite instanceof TextureAtlasSprite)) {
                continue;
            }

            WeakTextureStatus status = inspectWeakTexture(
                    resourceManager, new ResourceLocation(key), (TextureAtlasSprite) rawSprite);
            if (status == WeakTextureStatus.NON_SQUARE_WITHOUT_ANIMATION) {
                registeredSprites.remove(key);
                removed++;
            }
        }
        return removed;
    }

    private WeakTextureStatus inspectWeakTexture(
            IResourceManager resourceManager, ResourceLocation spriteLocation, TextureAtlasSprite sprite) {
        ResourceLocation fileLocation = new ResourceLocation(
                spriteLocation.getNamespace(), "textures/" + spriteLocation.getPath() + ".png");

        try {
            if (sprite.hasCustomLoader(resourceManager, fileLocation)) {
                return WeakTextureStatus.KEEP;
            }
        } catch (RuntimeException ignored) {
            return WeakTextureStatus.KEEP;
        }

        try (IResource resource = resourceManager.getResource(fileLocation)) {
            AnimationMetadataSection animation = resource.getMetadata("animation");
            if (animation != null) {
                return WeakTextureStatus.KEEP;
            }

            PngSizeInfo size = new PngSizeInfo(resource.getInputStream());
            return size.pngWidth == size.pngHeight
                    ? WeakTextureStatus.KEEP
                    : WeakTextureStatus.NON_SQUARE_WITHOUT_ANIMATION;
        } catch (FileNotFoundException e) {
            return WeakTextureStatus.UNAVAILABLE;
        } catch (IOException | RuntimeException e) {
            // Keep malformed PNGs and metadata so TextureMap reports the real resource error.
            return WeakTextureStatus.KEEP;
        }
    }

    private void disable(String message, Throwable cause) {
        if (disabled) {
            return;
        }
        disabled = true;
        if (cause == null) {
            MainRegistry.logger.warn("{}; leaving VintageFix texture handling unchanged", message);
        } else {
            MainRegistry.logger.warn("{}; leaving VintageFix texture handling unchanged", message, cause);
        }
    }

    private enum WeakTextureStatus {
        KEEP,
        UNAVAILABLE,
        NON_SQUARE_WITHOUT_ANIMATION
    }
}
