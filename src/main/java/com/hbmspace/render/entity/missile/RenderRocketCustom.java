package com.hbmspace.render.entity.missile;

import com.hbmspace.dim.CelestialBody;
import com.hbmspace.entity.missile.EntityRideableRocket;
import com.hbmspace.handler.RocketStruct;
import com.hbmspace.main.ResourceManagerSpace;
import com.hbmspace.render.misc.RocketPronter;
import com.hbmspace.interfaces.AutoRegister;
import com.hbm.main.ClientProxy;
import com.hbm.util.RenderUtil;
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
     * Pad, ascent, and descent use the normal entity pass (vanilla lighting, matches pad TESR).
     * True space flight uses the IConstantRenderer pass so F5/seat angles do not cull the mesh.
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

        Minecraft mc = Minecraft.getMinecraft();
        boolean hadLighting = RenderUtil.isLightingEnabled();
        if(constantPass) {
            mc.entityRenderer.enableLightmap();
        }
        if(!hadLighting) {
            GlStateManager.enableLighting();
        }

        GlStateManager.pushMatrix();
        try {
            bindEntityLightmap(entity);

            GlStateManager.translate(x, y, z);
            GlStateManager.rotate(yaw - 90.0F, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(pitch, 0.0F, 0.0F, 1.0F);
            GlStateManager.rotate(yaw - 90.0F, 0.0F, -1.0F, 0.0F);

            GlStateManager.enableTexture2D();
            GlStateManager.enableRescaleNormal();
            GlStateManager.disableCull();

            RocketPronter.prontRocket(rocket, entity, Minecraft.getMinecraft().getTextureManager(), !CelestialBody.inOrbit(entity.world), entity.decoupleTimer, entity.shroudTimer, interp);

            // Stage-separation clip plane / smooth shading can stomp lightmap coords.
            bindEntityLightmap(entity);
            if(constantPass) {
                mc.entityRenderer.enableLightmap();
            }
        } finally {
            // Hard reset in case shroud clip plane leaked and culled by camera angle.
            GL11.glDisable(GL11.GL_CLIP_PLANE0);
            GlStateManager.enableCull();
            GlStateManager.shadeModel(GL11.GL_FLAT);
            GlStateManager.popMatrix();

            if(constantPass) {
                mc.entityRenderer.disableLightmap();
            }
            if(!hadLighting) {
                GlStateManager.disableLighting();
            }
        }
    }

    @Override
    protected ResourceLocation getEntityTexture(@NotNull EntityRideableRocket entity) {
        return ResourceManagerSpace.universal;
    }

    private static boolean useConstantPassOnly(EntityRideableRocket entity) {
        return switch(entity.getState()) {
            case TRANSFER, UNDOCKING, DOCKING, TIPPING -> true;
            case LAUNCHING, LANDING -> isHighAltitudeFlight(entity);
            default -> false;
        };
    }

    /** Pitched ascent/descent high above the surface still needs the constant pass for F5 culling. */
    private static boolean isHighAltitudeFlight(EntityRideableRocket entity) {
        int surface = entity.world.getHeight((int) entity.posX, (int) entity.posZ);
        return entity.posY >= surface + 48.0D;
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
