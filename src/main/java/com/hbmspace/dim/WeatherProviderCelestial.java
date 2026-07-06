package com.hbmspace.dim;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.client.IRenderHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import java.util.Random;

@SideOnly(Side.CLIENT)
public class WeatherProviderCelestial extends IRenderHandler {

	private static final ResourceLocation RAIN_TEXTURE = new ResourceLocation("textures/environment/rain.png");
	private static final ResourceLocation SNOW_TEXTURE = new ResourceLocation("textures/environment/snow.png");

	private final Random random = new Random();
	private float[] rainXCoords;
	private float[] rainYCoords;

	@Override
	public void render(float partialTicks, WorldClient world, Minecraft mc) {
		float intensity = world.getRainStrength(partialTicks);

		if(intensity <= 0.0F) {
			return;
		}

		Entity camera = mc.getRenderViewEntity();
		if(camera == null) {
			return;
		}

		if(world.provider instanceof WorldProviderCelestial && !((WorldProviderCelestial)world.provider).hasWeatherCycle()) {
			return;
		}

		mc.entityRenderer.enableLightmap();
		initRainCoords();

		int timer = mc.player != null ? mc.player.ticksExisted : (int)(world.getTotalWorldTime() & Integer.MAX_VALUE);
		int playerX = MathHelper.floor(camera.posX);
		int playerY = MathHelper.floor(camera.posY);
		int playerZ = MathHelper.floor(camera.posZ);
		double interpX = camera.lastTickPosX + (camera.posX - camera.lastTickPosX) * partialTicks;
		double interpY = camera.lastTickPosY + (camera.posY - camera.lastTickPosY) * partialTicks;
		double interpZ = camera.lastTickPosZ + (camera.posZ - camera.lastTickPosZ) * partialTicks;
		int playerHeight = MathHelper.floor(interpY);
		int renderLayerCount = mc.gameSettings.fancyGraphics ? 10 : 5;
		Vec3d rainColor = getRainColor(world);
		Vec3d snowColor = getSnowColor(world);

		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.getBuffer();
		BlockPos.MutableBlockPos blockpos = new BlockPos.MutableBlockPos();

		GlStateManager.disableCull();
		GlStateManager.glNormal3f(0.0F, 1.0F, 0.0F);
		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
		GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		enableTextureAlphaTint();

		int layer = -1;

		for(int layerZ = playerZ - renderLayerCount; layerZ <= playerZ + renderLayerCount; ++layerZ) {
			for(int layerX = playerX - renderLayerCount; layerX <= playerX + renderLayerCount; ++layerX) {
				int rainCoord = (layerZ - playerZ + 16) * 32 + layerX - playerX + 16;
				float rainCoordX = this.rainXCoords[rainCoord] * 0.5F;
				float rainCoordY = this.rainYCoords[rainCoord] * 0.5F;
				blockpos.setPos(layerX, 0, layerZ);
				Biome biome = world.getBiome(blockpos);

				if(!biome.canRain() && !biome.getEnableSnow()) {
					continue;
				}

				int precipitationHeight = world.getPrecipitationHeight(blockpos).getY();
				int minHeight = playerY - renderLayerCount;
				int maxHeight = playerY + renderLayerCount;

				if(minHeight < precipitationHeight) minHeight = precipitationHeight;
				if(maxHeight < precipitationHeight) maxHeight = precipitationHeight;

				int layerY = precipitationHeight;
				if(precipitationHeight < playerHeight) layerY = playerHeight;

				if(minHeight == maxHeight) {
					continue;
				}

				this.random.setSeed(layerX * layerX * 3121 + layerX * 45238971 ^ layerZ * layerZ * 418711 + layerZ * 13761);
				blockpos.setPos(layerX, minHeight, layerZ);
				float temperature = biome.getTemperature(blockpos);
				boolean renderRain = world.getBiomeProvider().getTemperatureAtHeight(temperature, precipitationHeight) >= 0.15F;

				if(renderRain) {
					if(layer != 0) {
						if(layer >= 0) {
							tessellator.draw();
						}

						layer = 0;
						mc.getTextureManager().bindTexture(RAIN_TEXTURE);
						buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.PARTICLE_POSITION_TEX_COLOR_LMAP);
					}

					int rainSeed = layerX * layerX * 3121 + layerX * 45238971 + layerZ * layerZ * 418711 + layerZ * 13761;
					float rainOffset = ((timer + (rainSeed & 31)) + partialTicks) / 32.0F * (3.0F + this.random.nextFloat());
					double distX = layerX + 0.5D - camera.posX;
					double distZ = layerZ + 0.5D - camera.posZ;
					float intensityMod = MathHelper.sqrt(distX * distX + distZ * distZ) / renderLayerCount;

					blockpos.setPos(layerX, layerY, layerZ);
					int light = world.getCombinedLight(blockpos, 0);
					int lightU = light >> 16 & 65535;
					int lightV = light & 65535;
					float alpha = ((1.0F - intensityMod * intensityMod) * 0.5F + 0.5F) * intensity;

					buffer.setTranslation(-interpX, -interpY, -interpZ);
					buffer.pos(layerX - rainCoordX + 0.5D, minHeight, layerZ - rainCoordY + 0.5D).tex(0.0D, minHeight / 4.0D + rainOffset).color((float)rainColor.x, (float)rainColor.y, (float)rainColor.z, alpha).lightmap(lightU, lightV).endVertex();
					buffer.pos(layerX + rainCoordX + 0.5D, minHeight, layerZ + rainCoordY + 0.5D).tex(1.0D, minHeight / 4.0D + rainOffset).color((float)rainColor.x, (float)rainColor.y, (float)rainColor.z, alpha).lightmap(lightU, lightV).endVertex();
					buffer.pos(layerX + rainCoordX + 0.5D, maxHeight, layerZ + rainCoordY + 0.5D).tex(1.0D, maxHeight / 4.0D + rainOffset).color((float)rainColor.x, (float)rainColor.y, (float)rainColor.z, alpha).lightmap(lightU, lightV).endVertex();
					buffer.pos(layerX - rainCoordX + 0.5D, maxHeight, layerZ - rainCoordY + 0.5D).tex(0.0D, maxHeight / 4.0D + rainOffset).color((float)rainColor.x, (float)rainColor.y, (float)rainColor.z, alpha).lightmap(lightU, lightV).endVertex();
					buffer.setTranslation(0.0D, 0.0D, 0.0D);
				} else {
					if(layer != 1) {
						if(layer >= 0) {
							tessellator.draw();
						}

						layer = 1;
						mc.getTextureManager().bindTexture(SNOW_TEXTURE);
						buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.PARTICLE_POSITION_TEX_COLOR_LMAP);
					}

					float swayLoop = ((timer & 511) + partialTicks) / 512.0F;
					float fallVariation = this.random.nextFloat() + timer * 0.01F * (float)this.random.nextGaussian();
					float swayVariation = this.random.nextFloat() + timer * (float)this.random.nextGaussian() * 0.001F;
					double distX = layerX + 0.5D - camera.posX;
					double distZ = layerZ + 0.5D - camera.posZ;
					float intensityMod = MathHelper.sqrt(distX * distX + distZ * distZ) / renderLayerCount;

					blockpos.setPos(layerX, layerY, layerZ);
					int light = (world.getCombinedLight(blockpos, 0) * 3 + 15728880) / 4;
					int lightU = light >> 16 & 65535;
					int lightV = light & 65535;
					float alpha = ((1.0F - intensityMod * intensityMod) * 0.3F + 0.5F) * intensity;

					buffer.setTranslation(-interpX, -interpY, -interpZ);
					buffer.pos(layerX - rainCoordX + 0.5D, minHeight, layerZ - rainCoordY + 0.5D).tex(0.0F + fallVariation, minHeight / 4.0F + swayLoop + swayVariation).color((float)snowColor.x, (float)snowColor.y, (float)snowColor.z, alpha).lightmap(lightU, lightV).endVertex();
					buffer.pos(layerX + rainCoordX + 0.5D, minHeight, layerZ + rainCoordY + 0.5D).tex(1.0F + fallVariation, minHeight / 4.0F + swayLoop + swayVariation).color((float)snowColor.x, (float)snowColor.y, (float)snowColor.z, alpha).lightmap(lightU, lightV).endVertex();
					buffer.pos(layerX + rainCoordX + 0.5D, maxHeight, layerZ + rainCoordY + 0.5D).tex(1.0F + fallVariation, maxHeight / 4.0F + swayLoop + swayVariation).color((float)snowColor.x, (float)snowColor.y, (float)snowColor.z, alpha).lightmap(lightU, lightV).endVertex();
					buffer.pos(layerX - rainCoordX + 0.5D, maxHeight, layerZ - rainCoordY + 0.5D).tex(0.0F + fallVariation, maxHeight / 4.0F + swayLoop + swayVariation).color((float)snowColor.x, (float)snowColor.y, (float)snowColor.z, alpha).lightmap(lightU, lightV).endVertex();
					buffer.setTranslation(0.0D, 0.0D, 0.0D);
				}
			}
		}

		if(layer >= 0) {
			tessellator.draw();
		}

		GlStateManager.enableCull();
		GlStateManager.disableBlend();
		GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		disableTextureAlphaTint();
		mc.entityRenderer.disableLightmap();
	}

	private void enableTextureAlphaTint() {
		GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL13.GL_COMBINE);
		GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_COMBINE_RGB, GL11.GL_REPLACE);
		GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_SOURCE0_RGB, GL13.GL_PRIMARY_COLOR);
		GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_OPERAND0_RGB, GL11.GL_SRC_COLOR);
		GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_COMBINE_ALPHA, GL11.GL_MODULATE);
		GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_SOURCE0_ALPHA, GL11.GL_TEXTURE);
		GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_OPERAND0_ALPHA, GL11.GL_SRC_ALPHA);
		GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_SOURCE1_ALPHA, GL13.GL_PRIMARY_COLOR);
		GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL13.GL_OPERAND1_ALPHA, GL11.GL_SRC_ALPHA);
	}

	private void disableTextureAlphaTint() {
		GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_MODULATE);
	}

	private void initRainCoords() {
		if(this.rainXCoords != null) {
			return;
		}

		this.rainXCoords = new float[1024];
		this.rainYCoords = new float[1024];

		for(int i = 0; i < 32; ++i) {
			for(int j = 0; j < 32; ++j) {
				float coordX = j - 16;
				float coordY = i - 16;
				float coordLength = MathHelper.sqrt(coordX * coordX + coordY * coordY);
				this.rainXCoords[i << 5 | j] = -coordY / coordLength;
				this.rainYCoords[i << 5 | j] = coordX / coordLength;
			}
		}
	}

	private Vec3d getRainColor(WorldClient world) {
		if(world.provider instanceof WorldProviderCelestial) {
			return ((WorldProviderCelestial)world.provider).getWeatherColor();
		}

		return new Vec3d(1.0D, 1.0D, 1.0D);
	}

	private Vec3d getSnowColor(WorldClient world) {
		if(world.provider instanceof WorldProviderCelestial) {
			return ((WorldProviderCelestial)world.provider).getSnowColor();
		}

		return new Vec3d(1.0D, 1.0D, 1.0D);
	}
}