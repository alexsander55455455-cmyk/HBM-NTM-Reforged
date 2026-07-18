package com.hbmspace.render.entity.missile;

import com.hbmspace.dim.CelestialBody;
import com.hbmspace.entity.missile.EntityRideableRocket;
import com.hbmspace.handler.RocketStruct;
import com.hbmspace.main.ResourceManagerSpace;
import com.hbmspace.render.misc.RocketPronter;
import com.hbmspace.interfaces.AutoRegister;
import com.hbm.main.ClientProxy;
import com.hbm.util.BobMathUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
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

    /** Grounded rockets use vanilla rendering; active flight uses the uncullable constant pass. */
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
        if(constantPass) {
            mc.entityRenderer.enableLightmap();
            RenderHelper.enableStandardItemLighting();
        }

        double[] renderPos = resolveRenderTranslation(entity, x, y, z, interp);

        GlStateManager.pushMatrix();
        try {
            if(constantPass) {
                bindEntityLightmap(entity);
            }

            GlStateManager.translate(renderPos[0], renderPos[1], renderPos[2]);
            GlStateManager.rotate(yaw - 90.0F, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(pitch, 0.0F, 0.0F, 1.0F);
            GlStateManager.rotate(yaw - 90.0F, 0.0F, -1.0F, 0.0F);

            GlStateManager.enableTexture2D();
            GlStateManager.enableRescaleNormal();
            GlStateManager.disableCull();

            RocketPronter.prontRocket(rocket, entity, Minecraft.getMinecraft().getTextureManager(), !CelestialBody.inOrbit(entity.world), entity.decoupleTimer, entity.shroudTimer, interp);

            if(constantPass) {
                // Stage-separation clip plane / smooth shading can stomp lightmap coords.
                bindEntityLightmap(entity);
                mc.entityRenderer.enableLightmap();
            }
        } finally {
            // Hard reset in case shroud clip plane leaked and culled by camera angle.
            GL11.glDisable(GL11.GL_CLIP_PLANE0);
            GlStateManager.enableCull();
            GlStateManager.shadeModel(GL11.GL_FLAT);
            GlStateManager.popMatrix();

            if(constantPass) {
                RenderHelper.disableStandardItemLighting();
                mc.entityRenderer.disableLightmap();
            }
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

    /**
     * F5/third-person: anchor the mesh to the interpolated rider pose so rocket and camera
     * do not drift apart from separate entity-vs-player interpolation.
     */
    private static double[] resolveRenderTranslation(EntityRideableRocket entity, double x, double y, double z, float interp) {
        Minecraft mc = Minecraft.getMinecraft();
        Entity rider = mc.player;
        if(rider == null || rider.getRidingEntity() != entity) {
            return new double[] { x, y, z };
        }

        float yaw = entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * interp;
        float pitch = entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * interp;
        double transferOffset = entity.getState() == EntityRideableRocket.RocketState.TRANSFER ? 1.62D : 0.0D;
        double length = entity.getMountedYOffset() + rider.getYOffset() - transferOffset;
        Vec3d seatOffset = BobMathUtil.getDirectionFromAxisAngle(pitch - 90.0F, 180.0F - yaw, length);

        double riderX = rider.lastTickPosX + (rider.posX - rider.lastTickPosX) * interp;
        double riderY = rider.lastTickPosY + (rider.posY - rider.lastTickPosY) * interp;
        double riderZ = rider.lastTickPosZ + (rider.posZ - rider.lastTickPosZ) * interp;

        Entity view = mc.getRenderViewEntity();
        if(view == null) {
            return new double[] { x, y, z };
        }

        double viewX = view.lastTickPosX + (view.posX - view.lastTickPosX) * interp;
        double viewY = view.lastTickPosY + (view.posY - view.lastTickPosY) * interp;
        double viewZ = view.lastTickPosZ + (view.posZ - view.lastTickPosZ) * interp;

        return new double[] {
                (riderX - seatOffset.x) - viewX,
                (riderY - seatOffset.y) - viewY,
                (riderZ - seatOffset.z) - viewZ
        };
    }

    /** IConstantRenderer pass skips RenderManager light setup; bind world lightmap manually. */
    private static void bindEntityLightmap(EntityRideableRocket entity) {
        Minecraft mc = Minecraft.getMinecraft();
        mc.entityRenderer.enableLightmap();
        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GlStateManager.bindTexture(mc.entityRenderer.lightmapTexture.getGlTextureId());
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);

        int packed = entity.getBrightnessForRender();
        OpenGlHelper.setLightmapTextureCoords(
                OpenGlHelper.lightmapTexUnit,
                (float) (packed & 0xFFFF),
                (float) (packed >>> 16)
        );
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
