package com.hbmspace.render.entity.missile;

import com.hbmspace.dim.CelestialBody;
import com.hbmspace.entity.missile.EntityRideableRocket;
import com.hbmspace.handler.RocketStruct;
import com.hbmspace.main.ResourceManagerSpace;
import com.hbmspace.render.misc.RocketPronter;
import com.hbmspace.interfaces.AutoRegister;
import com.hbm.main.ClientProxy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.opengl.GL11;

@AutoRegister(factory = "FACTORY")
public class RenderRocketCustom extends Render<EntityRideableRocket> {

    public static final IRenderFactory<EntityRideableRocket> FACTORY = RenderRocketCustom::new;

    protected RenderRocketCustom(RenderManager renderManager) {
        super(renderManager);
        this.shadowSize = 0.0F;
    }

    /**
     * Rideable rockets draw in the IConstantRenderer pass (see doRender). The normal
     * entity sweep culls them from F5 and steep seat angles because the mesh sits far
     * from the entity origin.
     */
    @Override
    public boolean shouldRender(EntityRideableRocket entity, ICamera camera, double camX, double camY, double camZ) {
        return false;
    }

    @Override
    public void doRender(EntityRideableRocket entity, double x, double y, double z, float f, float interp) {
        if(!ClientProxy.renderingConstant || entity == null || entity.isDead) {
            return;
        }

        entity.ignoreFrustumCheck = true;
        RocketStruct rocket = entity.getRocket();
        if(rocket == null) {
            return;
        }

        float yaw = entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * interp;
        float pitch = entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * interp;
        if(Float.isNaN(yaw)) yaw = entity.rotationYaw;
        if(Float.isNaN(pitch)) pitch = entity.rotationPitch;

        GlStateManager.pushMatrix();
        try {
            GlStateManager.translate(x, y, z);
            GlStateManager.rotate(yaw - 90.0F, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(pitch, 0.0F, 0.0F, 1.0F);
            GlStateManager.rotate(yaw - 90.0F, 0.0F, -1.0F, 0.0F);

            GlStateManager.enableTexture2D();
            GlStateManager.enableRescaleNormal();
            GlStateManager.disableCull();

            RocketPronter.prontRocket(rocket, entity, Minecraft.getMinecraft().getTextureManager(), !CelestialBody.inOrbit(entity.world), entity.decoupleTimer, entity.shroudTimer, interp);
        } finally {
            // Hard reset in case shroud clip plane leaked and culled by camera angle.
            GL11.glDisable(GL11.GL_CLIP_PLANE0);
            GlStateManager.enableCull();
            GlStateManager.shadeModel(GL11.GL_FLAT);
            GlStateManager.popMatrix();
        }
    }

    @Override
    protected ResourceLocation getEntityTexture(@NotNull EntityRideableRocket entity) {
        return ResourceManagerSpace.universal;
    }
}
