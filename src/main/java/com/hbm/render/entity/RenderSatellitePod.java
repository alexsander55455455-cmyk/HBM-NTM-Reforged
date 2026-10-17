package com.hbm.render.entity;

import com.hbm.entity.missile.EntitySatellitePod;
import com.hbm.interfaces.AutoRegister;
import com.hbm.main.ResourceManager;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import org.lwjgl.opengl.GL11;

@AutoRegister(factory = "FACTORY")
public class RenderSatellitePod extends Render<EntitySatellitePod> {
    public static final IRenderFactory<EntitySatellitePod> FACTORY = RenderSatellitePod::new;

    public RenderSatellitePod(RenderManager manager) { super(manager); }

    @Override
    public void doRender(EntitySatellitePod entity, double x, double y, double z, float yaw, float partialTicks) {
        boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        int shade = GL11.glGetInteger(GL11.GL_SHADE_MODEL);
        int texture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GlStateManager.pushMatrix();
        try {
            GlStateManager.translate(x, y, z);
            GlStateManager.enableCull();
            GlStateManager.shadeModel(GL11.GL_SMOOTH);
            bindEntityTexture(entity);
            ResourceManager.dropship.renderPart("Pod");
            float legs = entity.prevLegs + (entity.legs - entity.prevLegs) * partialTicks;
            for(int i = 0; i < 4; i++) {
                GlStateManager.pushMatrix();
                GlStateManager.rotate(45 + 90 * i, 0, 1, 0);
                GlStateManager.translate(0.5, 1.75, 0);
                GlStateManager.rotate(150 * (1 - legs), 0, 0, 1);
                GlStateManager.translate(-0.5, -1.75, 0);
                ResourceManager.dropship.renderPart("Leg");
                GlStateManager.popMatrix();
            }
        } finally {
            GlStateManager.bindTexture(texture);
            GlStateManager.shadeModel(shade);
            if(cull) GlStateManager.enableCull(); else GlStateManager.disableCull();
            GlStateManager.popMatrix();
        }
    }

    @Override
    protected ResourceLocation getEntityTexture(EntitySatellitePod entity) { return ResourceManager.dropship_tex; }
}
