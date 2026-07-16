package com.hbmspace.dim.thatmo;

import com.hbm.blocks.ModBlocks;
import com.hbmspace.config.SpaceConfig;
import com.hbmspace.dim.CelestialBody;
import com.hbmspace.dim.trait.CelestialBodyTrait;
import com.hbmspace.dim.WorldProviderCelestial;
import com.hbmspace.lib.HBMSpaceSoundHandler;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.DimensionType;
import net.minecraft.world.biome.BiomeProviderSingle;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.NotNull;

public class WorldProviderThatmo extends WorldProviderCelestial {

    private int chargeTime;
    private float flashDistance;

    @Override
    public void init() {
        this.biomeProvider = new BiomeProviderSingle(BiomeGenThatmo.biome);
    }

    @Override
    public @NotNull IChunkGenerator createChunkGenerator() {
        return new ChunkProviderThatmo(this.world, this.getSeed(), false);
    }

    @Override
    public Block getStone() {
        return ModBlocks.sellafield_slaked;
    }

    @Override
    public void updateWeather() {
        super.updateWeather();

        // Battlefield flash/meteors are client visuals only; isolate Minecraft for dedicated server.
        if(world.isRemote) {
            updateWeatherClient();
        }
    }

    @SideOnly(Side.CLIENT)
    private void updateWeatherClient() {
        if(CelestialBody.getTrait(world, CelestialBodyTrait.CBT_BATTLEFIELD.class) == null) {
            return;
        }

        if(chargeTime <= 1000) {
            chargeTime++;
            flashDistance = 0;
        } else {
            EntityPlayer clientPlayer = net.minecraft.client.Minecraft.getMinecraft().player;
            if(flashDistance <= 1 && clientPlayer != null) {
                clientPlayer.playSound(HBMSpaceSoundHandler.fireFlash, 10F, 1F);
            }

            flashDistance += 0.3F;
            flashDistance = Math.min(100.0F, flashDistance + 0.3F * (100.0F - flashDistance) * 0.15F);

            if(flashDistance >= 100) {
                chargeTime = 0;
            }
        }

        for(int i = 0; i < meteors.size(); i++) {
            meteors.get(i).update();
        }

        EntityPlayer player = net.minecraft.client.Minecraft.getMinecraft().player;
        if(player != null && world.rand.nextInt(4) == 0) {
            Meteor meteor = new Meteor(
                    (player.posX + world.rand.nextInt(16000)) - 8000,
                    2017,
                    (player.posZ + world.rand.nextInt(16000)) - 8000
            );
            meteors.add(meteor);
        }

        meteors.removeIf(meteor -> meteor.isDead);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public @NotNull Vec3d getSkyColor(@NotNull Entity cameraEntity, float partialTicks) {
        Vec3d base = super.getSkyColor(cameraEntity, partialTicks);
        float alpha = getFlashAlpha();

        return new Vec3d(base.x + alpha, base.y + alpha, base.z + alpha);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public @NotNull Vec3d getFogColor(float celestialAngle, float partialTicks) {
        Vec3d base = super.getFogColor(celestialAngle, partialTicks);
        float alpha = getFlashAlpha();

        return new Vec3d(base.x + alpha, base.y + alpha, base.z + alpha);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public float getSunBrightness(float partialTicks) {
        return super.getSunBrightness(partialTicks) * 0.1F + getFlashAlpha();
    }

    @SideOnly(Side.CLIENT)
    private float getFlashAlpha() {
        return flashDistance <= 0 ? 0.0F : 1.0F - Math.min(1.0F, flashDistance / 100);
    }

    public float getFlashDistance() {
        return this.flashDistance;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public net.minecraftforge.client.IRenderHandler getSkyRenderer() {
        return new SkyProviderThatmo();
    }

    @Override
    public @NotNull DimensionType getDimensionType() {
        return DimensionType.getById(SpaceConfig.thatmoDimension);
    }
}
