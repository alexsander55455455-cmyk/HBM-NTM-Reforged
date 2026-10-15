package com.hbm.entity.projectile;

import com.hbm.interfaces.AutoRegister;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

@AutoRegister(name = "entity_brandon_barrel", trackingRange = 128)
public class EntityBrandonBarrel extends EntityThrowableNT {

  public static final float GRAVITY = 0.04F;

  public EntityBrandonBarrel(World world) {
    super(world);
    setSize(0.65F, 0.65F);
  }

  @Override public float getGravityVelocity() { return GRAVITY; }
  @Override protected float getAirDrag() { return 1; }
  @Override protected float getWaterDrag() { return 1; }

  @Override
  public void onUpdate() {
    super.onUpdate();
    if (!world.isRemote && ticksInAir >= 200) setDead();
  }

  @Override
  protected void onImpact(RayTraceResult hit) {
    if (!world.isRemote && !isDead) {
      setDead();
      // Same strength, fire and terrain damage as ModBlocks.red_barrel.
      world.newExplosion(this, hit.hitVec.x, hit.hitVec.y, hit.hitVec.z, 2.5F, true, true);
    }
  }
}
