package com.hbm.render.entity.effect;

import com.hbm.entity.logic.EntityOrbitalLaser;
import com.hbm.interfaces.AutoRegister;
import com.hbm.main.ClientProxy;
import com.hbm.render.amlfrom1710.Vec3;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.opengl.GL11;

@AutoRegister(entity = EntityOrbitalLaser.class, factory = "FACTORY")
public class RenderOrbitalLaser extends Render<EntityOrbitalLaser> {

    public static final IRenderFactory<EntityOrbitalLaser> FACTORY = RenderOrbitalLaser::new;

    public RenderOrbitalLaser(RenderManager renderManager) {
        super(renderManager);
    }

    @Override
    public void doRender(EntityOrbitalLaser entity, double x, double y, double z, float entityYaw, float partialTicks) {
        if(!ClientProxy.renderingConstant) return;

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.disableLighting();
        GlStateManager.enableCull();
        GlStateManager.disableTexture2D();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        GlStateManager.depthMask(false);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        RenderHelper.disableStandardItemLighting();

        Vec3 vector = Vec3.createVectorHelper(0.5D, 0D, 0D);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

        for(int i = 0; i < 8; i++) {
            buffer.pos(vector.xCoord, 250D, vector.zCoord).color(1F, 0F, 0F, 1F).endVertex();
            buffer.pos(vector.xCoord, 0D, vector.zCoord).color(1F, 0F, 0F, 1F).endVertex();
            vector.rotateAroundY(45F);
            buffer.pos(vector.xCoord, 0D, vector.zCoord).color(1F, 0F, 0F, 1F).endVertex();
            buffer.pos(vector.xCoord, 250D, vector.zCoord).color(1F, 0F, 0F, 1F).endVertex();
        }

        for(int i = 0; i < 8; i++) {
            buffer.pos(vector.xCoord * 0.5D, 250D, vector.zCoord * 0.5D).color(1F, 1F, 1F, 1F).endVertex();
            buffer.pos(vector.xCoord * 0.5D, 0D, vector.zCoord * 0.5D).color(1F, 1F, 1F, 1F).endVertex();
            vector.rotateAroundY(45F);
            buffer.pos(vector.xCoord * 0.5D, 0D, vector.zCoord * 0.5D).color(1F, 1F, 1F, 1F).endVertex();
            buffer.pos(vector.xCoord * 0.5D, 250D, vector.zCoord * 0.5D).color(1F, 1F, 1F, 1F).endVertex();
        }

        tessellator.draw();
        GlStateManager.depthMask(true);
        GlStateManager.disableCull();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.color(1F, 1F, 1F, 1F);
        GlStateManager.popMatrix();
    }

    @Override
    public void doRenderShadowAndFire(Entity entity, double x, double y, double z, float yaw, float partialTicks) {
    }

    @Override
    protected ResourceLocation getEntityTexture(@NotNull EntityOrbitalLaser entity) {
        return null;
    }
}
