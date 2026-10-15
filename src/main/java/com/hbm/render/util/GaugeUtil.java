package com.hbm.render.util;

import com.hbm.Tags;
import com.hbm.util.MutableVec3d;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;

public class GaugeUtil {

	public enum Gauge {

		ROUND_SMALL(new ResourceLocation(Tags.MODID + ":textures/gui/gauges/small_round.png"), 18, 18, 13),
		ROUND_LARGE(new ResourceLocation(Tags.MODID + ":textures/gui/gauges/large_round.png"), 36, 36, 13),
		BOW_SMALL(new ResourceLocation(Tags.MODID + ":textures/gui/gauges/small_bow.png"), 18, 18, 13),
		BOW_LARGE(new ResourceLocation(Tags.MODID + ":textures/gui/gauges/large_bow.png"), 36, 36, 13),
		WIDE_SMALL(new ResourceLocation(Tags.MODID + ":textures/gui/gauges/small_wide.png"), 18, 12, 7),
		WIDE_LARGE(new ResourceLocation(Tags.MODID + ":textures/gui/gauges/large_wide.png"), 36, 24, 11),
		BAR_SMALL(new ResourceLocation(Tags.MODID + ":textures/gui/gauges/small_bar.png"), 36, 12, 16);

		ResourceLocation texture;
		int width;
		int height;
		int count;

		Gauge(ResourceLocation texture, int width, int height, int count) {
			this.texture = texture;
			this.width = width;
			this.height = height;
			this.count = count;
		}
	}

	/**
	 * 
	 * @param gauge The gauge enum to use
	 * @param x The x coord in the GUI (left)
	 * @param y The y coord in the GUI (top)
	 * @param z The z-level (from GUI.zLevel)
	 * @param progress Double from 0-1 how far the gauge has progressed
	 */
	public static void renderGauge(Gauge gauge, float x, float y, float z, float progress) {

		Minecraft.getMinecraft().renderEngine.bindTexture(gauge.texture);

		int frameNum = (int) Math.round((gauge.count - 1) * progress);
		float singleFrame = 1F / gauge.count;
		float frameOffset = singleFrame * frameNum;

		NTMBufferBuilder buf = NTMImmediate.INSTANCE.beginPositionTexQuads(1);
		buf.appendPositionTexQuadUnchecked(
				x, y + gauge.height, z, 0, frameOffset + singleFrame,
				x + gauge.width, y + gauge.height, z, 1, frameOffset + singleFrame,
				x + gauge.width, y, z, 1, frameOffset,
				x, y, z, 0, frameOffset
		);
		NTMImmediate.INSTANCE.draw();
	}

	public static void renderGauge(Gauge gauge, double x, double y, double z, double progress) {
		renderGauge(gauge, (float) x, (float) y, (float) z, (float) progress);
	}

	public static void drawSmoothLinearGauge(int x, int y, double z, double progress, double tipLength, double backLength, double backSide, double scale, float rotation, int color) {
		GlStateManager.disableTexture2D();
		double distance = MathHelper.clamp(progress, 0, 1) * Math.max(scale, 1);
		double angle = Math.toRadians(-rotation);
		double cos = Math.cos(angle), sin = Math.sin(angle);
		double[] xs = {0, -backSide, -backSide, backSide, backSide};
		double[] ys = {-tipLength, 0, backLength, backLength, 0};
		NTMBufferBuilder buffer = NTMImmediate.INSTANCE.beginPositionColor(GL11.GL_TRIANGLES, 18);
		for(int layer = 0; layer < 2; layer++) {
			double mult = layer == 0 ? 1.5 : 1;
			int packedColor = layer == 0 ? NTMBufferBuilder.packColor(0, 0, 0, 255)
					: NTMBufferBuilder.packColor(color >> 16 & 255, color >> 8 & 255, color & 255, 255);
			for(int triangle = 1; triangle < 4; triangle++) {
				for(int vertex = 0; vertex < 3; vertex++) {
					int index = vertex == 0 ? 0 : triangle + vertex - 1;
					double vx = xs[index] * cos + ys[index] * sin;
					double vy = ys[index] * cos - xs[index] * sin;
					buffer.appendPositionColor((float) (x + distance * cos + vx * mult),
							(float) (y + distance * sin + vy * (index == 2 || index == 3 ? 1 : mult)), (float) z, packedColor);
				}
			}
		}
		NTMImmediate.INSTANCE.draw();
		GlStateManager.enableTexture2D();
		GlStateManager.color(1, 1, 1, 1);
	}

	/** Reveals a 270 degree arc from bottom-left to bottom-right on a 256x256 atlas. */
	public static void drawSmoothTextureModalCircle(int x, int y, float z, int u, int v, int width, int height, double progress) {
		progress = MathHelper.clamp(progress, 0, 1);
		if(progress <= 0) return;
		double angle = Math.toRadians(135 + progress * 270);
		double dx = Math.cos(angle), dy = Math.sin(angle);
		double edge = Math.max(Math.abs(dx), Math.abs(dy));
		float targetX = (float) ((dx / edge + 1) * width / 2);
		float targetY = (float) ((dy / edge + 1) * height / 2);
		int segment = Math.min((int) (progress * 3), 2);
		NTMBufferBuilder buffer = NTMImmediate.INSTANCE.beginPositionTex(GL11.GL_TRIANGLES, (segment + 1) * 3);
		for(int i = 0; i <= segment; i++) {
			float startX = i == 2 ? width : 0, startY = i == 0 ? height : 0;
			float endX = i == segment ? targetX : (i == 0 ? 0 : width);
			float endY = i == segment ? targetY : 0;
			buffer.appendPositionTex(x + startX, y + startY, z, (u + startX) / 256F, (v + startY) / 256F);
			buffer.appendPositionTex(x + width / 2F, y + height / 2F, z, (u + width / 2F) / 256F, (v + height / 2F) / 256F);
			buffer.appendPositionTex(x + endX, y + endY, z, (u + endX) / 256F, (v + endY) / 256F);
		}
		NTMImmediate.INSTANCE.draw();
	}

	public static void drawSmoothGauge(int x, int y, double z, double progress, double tipLength, double backLength, double backSide, int color) {
		drawSmoothGauge(x, y, z, progress, tipLength, backLength, backSide, color, 0x000000);
	}

	public static void drawSmoothGauge(int x, int y, double z, double progress, double tipLength, double backLength, double backSide, int color, int colorOuter) {
		GlStateManager.disableTexture2D();

		progress = MathHelper.clamp(progress, 0, 1);
		float angle = (float) Math.toRadians(-progress * 270 - 45);

		MutableVec3d tip = new MutableVec3d(0, tipLength, 0);
		MutableVec3d left = new MutableVec3d(backSide, -backLength, 0);
		MutableVec3d right = new MutableVec3d(-backSide, -backLength, 0);

		tip.rotateRollSelf(angle);
		left.rotateRollSelf(angle);
		right.rotateRollSelf(angle);

		NTMBufferBuilder bufferbuilder = NTMImmediate.INSTANCE.beginPositionColor(GL11.GL_TRIANGLES, 6);

		float r_outer = (float)(colorOuter >> 16 & 255) / 255.0F;
		float g_outer = (float)(colorOuter >> 8 & 255) / 255.0F;
		float b_outer = (float)(colorOuter & 255) / 255.0F;

		float r_inner = (float)(color >> 16 & 255) / 255.0F;
		float g_inner = (float)(color >> 8 & 255) / 255.0F;
		float b_inner = (float)(color & 255) / 255.0F;

		float xBase = (float) x;
		float yBase = (float) y;
		float zBase = (float) z;
		float mult = 1.5F;
		int outerColor = NTMBufferBuilder.packColor(r_outer, g_outer, b_outer, 1.0F);
		int innerColor = NTMBufferBuilder.packColor(r_inner, g_inner, b_inner, 1.0F);
		float tx = (float) tip.x;
		float ty = (float) tip.y;
		float lx = (float) left.x;
		float ly = (float) left.y;
		float rx = (float) right.x;
		float ry = (float) right.y;
		bufferbuilder.appendPositionColor(xBase + tx * mult, yBase + ty * mult, zBase, outerColor);
		bufferbuilder.appendPositionColor(xBase + lx * mult, yBase + ly * mult, zBase, outerColor);
		bufferbuilder.appendPositionColor(xBase + rx * mult, yBase + ry * mult, zBase, outerColor);

		bufferbuilder.appendPositionColor(xBase + tx, yBase + ty, zBase, innerColor);
		bufferbuilder.appendPositionColor(xBase + lx, yBase + ly, zBase, innerColor);
		bufferbuilder.appendPositionColor(xBase + rx, yBase + ry, zBase, innerColor);

		NTMImmediate.INSTANCE.draw();

		GlStateManager.enableTexture2D();
		GlStateManager.color(1, 1, 1, 1);
	}
}
