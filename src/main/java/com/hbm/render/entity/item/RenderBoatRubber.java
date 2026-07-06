package com.hbm.render.entity.item;

import com.hbm.Tags;
import com.hbm.entity.item.EntityBoatRubber;
import com.hbm.interfaces.AutoRegister;
import net.minecraft.client.model.IMultipassModel;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBoat;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.client.registry.IRenderFactory;

@AutoRegister(entity = EntityBoatRubber.class, factory = "FACTORY")
public class RenderBoatRubber extends Render<EntityBoatRubber> {

    private static final ResourceLocation BOAT_TEXTURES = new ResourceLocation(Tags.MODID, "textures/entity/boat_rubber.png");
    protected ModelBase modelBoat = new ModelBoat();

    public static final IRenderFactory<EntityBoatRubber> FACTORY = RenderBoatRubber::new;

    public RenderBoatRubber(RenderManager renderManager) {
        super(renderManager);
        this.shadowSize = 0.5F;
    }

    @Override
    public void doRender(EntityBoatRubber entity, double x, double y, double z, float yaw, float partialTicks) {
        GlStateManager.pushMatrix();
        setupTranslation(x, y, z);
        setupRotation(entity, yaw, partialTicks);
        bindEntityTexture(entity);
        modelBoat.render(entity, partialTicks, 0.0F, -0.1F, 0.0F, 0.0F, 0.0625F);
        GlStateManager.popMatrix();
        super.doRender(entity, x, y, z, yaw, partialTicks);
    }

    @Override
    public boolean isMultipass() {
        return true;
    }

    @Override
    public void renderMultipass(EntityBoatRubber entity, double x, double y, double z, float yaw, float partialTicks) {
        GlStateManager.pushMatrix();
        setupTranslation(x, y, z);
        setupRotation(entity, yaw, partialTicks);
        bindEntityTexture(entity);
        ((IMultipassModel) modelBoat).renderMultipass(entity, partialTicks, 0.0F, -0.1F, 0.0F, 0.0F, 0.0625F);
        GlStateManager.popMatrix();
    }

    private void setupRotation(EntityBoatRubber entity, float yaw, float partialTicks) {
        GlStateManager.rotate(180.0F - yaw, 0.0F, 1.0F, 0.0F);
        float timeSinceHit = (float) entity.getTimeSinceHit() - partialTicks;
        float damageTaken = entity.getDamageTaken() - partialTicks;

        if (damageTaken < 0.0F) {
            damageTaken = 0.0F;
        }

        if (timeSinceHit > 0.0F) {
            GlStateManager.rotate(MathHelper.sin(timeSinceHit) * timeSinceHit * damageTaken / 10.0F * (float) entity.getForwardDirection(), 1.0F, 0.0F, 0.0F);
        }

        GlStateManager.scale(-1.0F, -1.0F, 1.0F);
    }

    private void setupTranslation(double x, double y, double z) {
        GlStateManager.translate((float) x, (float) y + 0.375F, (float) z);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityBoatRubber entity) {
        return BOAT_TEXTURES;
    }
}