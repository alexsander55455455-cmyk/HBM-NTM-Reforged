package com.hbm.render.entity.projectile;

import com.hbm.entity.projectile.EntityBrandonBarrel;
import com.hbm.interfaces.AutoRegister;
import com.hbm.main.ResourceManager;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.registry.IRenderFactory;

@AutoRegister(factory = "FACTORY")
public class RenderBrandonBarrel extends Render<EntityBrandonBarrel> {

  public static final IRenderFactory<EntityBrandonBarrel> FACTORY = RenderBrandonBarrel::new;

  public RenderBrandonBarrel(RenderManager manager) { super(manager); }

  @Override
  public void doRender(EntityBrandonBarrel barrel, double x, double y, double z, float yaw, float partialTicks) {
    GlStateManager.pushMatrix();
    GlStateManager.translate(x, y, z);
    GlStateManager.rotate(barrel.rotationYaw - 90, 0, 1, 0);
    GlStateManager.rotate((barrel.ticksExisted + partialTicks) * 12, 0, 0, 1);
    GlStateManager.translate(-0.875, -2, 0);
    bindEntityTexture(barrel);
    ResourceManager.turret_brandon.renderPart("Barrel");
    GlStateManager.popMatrix();
  }

  @Override
  protected ResourceLocation getEntityTexture(EntityBrandonBarrel barrel) {
    return ResourceManager.brandon_explosive;
  }
}
