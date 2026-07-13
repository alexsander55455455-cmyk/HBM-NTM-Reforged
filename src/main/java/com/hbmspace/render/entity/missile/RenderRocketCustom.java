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
import net.minecraft.client.renderer.OpenGlHelper;
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
     * Grounded rockets use the normal entity pass (vanilla light setup, matches pad TESR).
     * In-flight rockets use the IConstantRenderer pass so F5/seat angles do not cull them.
     */
    @Override
    public boolean shouldRender(EntityRideableRocket entity, ICamera camera, double camX, double camY, double camZ) {
        if(entity == null || entity.isDead) {
            return false;
        }
        entity.ignoreFrustumCheck = true;
        return !useConstantPassOnly(entity);
    }

    @Override
    public void doRender(EntityRideableRocket entity, double x, double y, double z, float f, float interp) {
        if(entity == null || entity.isDead) {
            return;
        }

        boolean constantPass = ClientProxy.renderingConstant;
        boolean flightPass = useConstantPassOnly(entity);
        if(constantPass != flightPass) {
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
            if(constantPass) {
                bindEntityLightmap(entity);
            }

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

    private static boolean useConstantPassOnly(EntityRideableRocket entity) {
        return switch(entity.getState()) {
            case LAUNCHING, LANDING, TRANSFER, UNDOCKING, DOCKING, TIPPING -> true;
            default -> false;
        };
    }

    /** IConstantRenderer pass skips RenderManager light setup; apply world lightmap manually. */
    private static void bindEntityLightmap(EntityRideableRocket entity) {
        int packed = entity.getBrightnessForRender();
        OpenGlHelper.setLightmapTextureCoords(
                OpenGlHelper.lightmapTexUnit,
                (float) (packed & 0xFFFF),
                (float) (packed >>> 16)
        );
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
