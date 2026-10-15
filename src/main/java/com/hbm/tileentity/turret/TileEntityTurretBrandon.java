package com.hbm.tileentity.turret;

import com.hbm.interfaces.AutoRegister;
import com.hbm.blocks.ModBlocks;
import com.hbm.entity.projectile.EntityBrandonBarrel;
import com.hbm.inventory.container.ContainerTurretBase;
import com.hbm.inventory.gui.GUITurretChekhov;
import com.hbm.items.machine.ItemTurretBiometry;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.lib.Library;
import com.hbm.tileentity.IGUIProvider;
import io.netty.buffer.ByteBuf;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.ItemStackHandler;

@AutoRegister
public class TileEntityTurretBrandon extends TileEntityTurretBaseNT implements IGUIProvider {

  public static final int RELOAD_TICKS = 80;
  public static final long ENERGY_PER_SHOT = 1000;
  private static final double HORIZONTAL_SPEED = 1.2;
  private int reloadTicks;
  private boolean clientHasBarrel;
  private UUID owner;

  public void setOwner(UUID owner) {
    this.owner = owner;
    markDirty();
  }

  @Override
  public boolean entityAcceptableTarget(Entity entity) {
    return (owner == null || !owner.equals(entity.getUniqueID())) && super.entityAcceptableTarget(entity);
  }

  @Override
  protected ItemStackHandler getNewInventory(int count, int limit) {
    return new ItemStackHandler(count) {
      @Override public boolean isItemValid(int slot, ItemStack stack) {
        return isItemValidForSlot(slot, stack);
      }
      @Override protected void onContentsChanged(int slot) {
        markDirty();
        if (slot == 0) cachedWhitelist = null;
      }
      @Override public int getSlotLimit(int slot) { return limit; }
    };
  }

  @Override
  public boolean isItemValidForSlot(int slot, ItemStack stack) {
    if (stack.isEmpty()) return false;
    if (slot == 0) return stack.getItem() instanceof ItemTurretBiometry;
    if (slot == 10) return Library.isBattery(stack);
    return slot >= 1 && slot <= 9 && isBarrel(stack);
  }

  private static boolean isBarrel(ItemStack stack) {
    return !stack.isEmpty() && stack.getItem() == Item.getItemFromBlock(ModBlocks.red_barrel)
        && stack.getMetadata() == 0;
  }

  private int findBarrel() {
    for (int slot = 1; slot <= 9; slot++) {
      if (isBarrel(inventory.getStackInSlot(slot))) return slot;
    }
    return -1;
  }

  @Override
  public void update() {
    if (reloadTicks > 0) {
      reloadTicks--;
      if (!world.isRemote) markDirty();
    }
    super.update();
  }

  public int getReloadTicks() { return reloadTicks; }

  public boolean hasLoadedBarrel() {
    return reloadTicks <= 20 && (world.isRemote ? clientHasBarrel : findBarrel() >= 0);
  }

  @Override public long getConsumption() { return 0; }
  @Override public boolean hasPower() { return getPower() >= ENERGY_PER_SHOT; }
  @Override public double getDecetorRange() { return 48; }
  @Override public double getDecetorGrace() { return 8; }
  @Override public double getTurretElevation() { return 60; }
  @Override public double getAcceptableInaccuracy() { return 0.5; }

  public Vec3d getMuzzle() {
    return getBarrelPosition(1.55);
  }

  private Vec3d getBarrelPosition(double forward) {
    // The existing barrel mesh is centred at (0.875, 2, 0), above the 1.5-high pivot.
    double horizontal = forward * Math.cos(rotationPitch) - 0.5 * Math.sin(rotationPitch);
    return getTurretPos().add(-Math.sin(rotationYaw) * horizontal,
        forward * Math.sin(rotationPitch) + 0.5 * Math.cos(rotationPitch),
        Math.cos(rotationYaw) * horizontal);
  }

  public static Vec3d getLaunchVelocity(Vec3d start, Vec3d end) {
    Vec3d delta = end.subtract(start);
    double time = Math.sqrt(delta.x * delta.x + delta.z * delta.z) / HORIZONTAL_SPEED;
    if (time < 1 || time > 60) return null;
    int ticks = (int) time;
    // EntityThrowableNT moves first, then applies gravity. Include its fractional last tick.
    double fall = EntityBrandonBarrel.GRAVITY * (ticks * (ticks - 1) * 0.5 + ticks * (time - ticks));
    Vec3d velocity = new Vec3d(delta.x / time, (delta.y + fall) / time, delta.z / time);
    double pitch = Math.toDegrees(Math.atan2(velocity.y, HORIZONTAL_SPEED));
    return pitch < -30 || pitch > 60 ? null : velocity;
  }

  private Vec3d getAimPoint(Vec3d muzzle) {
    Vec3d aim = tPos;
    if (target != null) {
      Vec3d movement = new Vec3d(target.posX - target.prevPosX,
          target.posY - target.prevPosY, target.posZ - target.prevPosZ);
      for (int i = 0; i < 3; i++) {
        Vec3d delta = aim.subtract(muzzle);
        double time = Math.sqrt(delta.x * delta.x + delta.z * delta.z) / HORIZONTAL_SPEED;
        aim = tPos.add(movement.scale(time));
      }
    }
    return aim;
  }

  @Override
  protected void alignTurret() {
    Vec3d velocity = getLaunchVelocity(getMuzzle(), getAimPoint(getMuzzle()));
    if (velocity != null) {
      turnTowardsAngle(Math.atan2(velocity.y, HORIZONTAL_SPEED), -Math.atan2(velocity.x, velocity.z));
    }
  }

  @Override
  public boolean entityInLOS(Entity entity) {
    return getTurretPos().distanceTo(getEntityPos(entity)) <= getDecetorRange()
        && super.entityInLOS(entity);
  }

  private boolean isPathClear(Vec3d start, Vec3d velocity, Vec3d end) {
    if (Library.rayTraceBlocks(world, getBarrelPosition(0.875), start, false, true, false) != null) return false;
    Vec3d delta = end.subtract(start);
    double time = Math.sqrt(delta.x * delta.x + delta.z * delta.z) / HORIZONTAL_SPEED;
    Vec3d from = start;
    for (double elapsed = 0; elapsed < time; elapsed++) {
      Vec3d to = from.add(velocity.scale(Math.min(1, time - elapsed)));
      if (Library.rayTraceBlocks(world, from, to, false, true, false) != null) return false;
      from = to;
      velocity = velocity.add(0, -EntityBrandonBarrel.GRAVITY, 0);
    }
    return true;
  }

  @Override
  public long getMaxPower() {
    return 10000;
  }

  @Override
  public void updateFiringTick() {
    if (world.isRemote || !isOn || !aligned || !hasPower() || reloadTicks > 0 || tPos == null) return;
    if (target != null && (!entityAcceptableTarget(target) || !entityInLOS(target))) return;
    double distance = getTurretPos().distanceTo(tPos);
    if (distance < getDecetorGrace() || distance > getDecetorRange()) return;
    int slot = findBarrel();
    if (slot < 0) return;
    Vec3d muzzle = getMuzzle();
    Vec3d aim = getAimPoint(muzzle);
    Vec3d velocity = getLaunchVelocity(muzzle, aim);
    if (velocity == null || !isPathClear(muzzle, velocity, aim)) return;
    EntityBrandonBarrel barrel = new EntityBrandonBarrel(world);
    barrel.setPosition(muzzle.x, muzzle.y, muzzle.z);
    barrel.shoot(velocity.x, velocity.y, velocity.z, (float) velocity.length(), 0);
    if (!world.spawnEntity(barrel)) return;
    inventory.extractItem(slot, 1, false);
    setPower(getPower() - ENERGY_PER_SHOT);
    reloadTicks = RELOAD_TICKS;
    markDirty();
    world.playSound(null, pos, HBMSoundHandler.jeremy_fire, SoundCategory.BLOCKS, 2F, 0.8F);
  }

  @Override
  public void serialize(ByteBuf buf) {
    super.serialize(buf);
    buf.writeInt(reloadTicks);
    buf.writeBoolean(findBarrel() >= 0);
  }

  @Override
  public void deserialize(ByteBuf buf) {
    super.deserialize(buf);
    reloadTicks = buf.readInt();
    clientHasBarrel = buf.readBoolean();
  }

  @Override
  public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
    nbt.setInteger("brandonReload", reloadTicks);
    if (owner != null) nbt.setUniqueId("brandonOwner", owner);
    return super.writeToNBT(nbt);
  }

  @Override
  public void readFromNBT(NBTTagCompound nbt) {
    super.readFromNBT(nbt);
    reloadTicks = Math.max(0, Math.min(RELOAD_TICKS, nbt.getInteger("brandonReload")));
    owner = nbt.hasUniqueId("brandonOwner") ? nbt.getUniqueId("brandonOwner") : null;
  }

  @Override
  @SideOnly(Side.CLIENT)
  public List<ItemStack> getAmmoTypesForDisplay() {
    return Collections.singletonList(new ItemStack(ModBlocks.red_barrel));
  }

  @Override
  protected List<Integer> getAmmoList() {
    return Collections.emptyList();
  }

  @Override
  public String getDefaultName() {
    return "container.turretBrandon";
  }

  @Override
  public Container provideContainer(int ID, EntityPlayer player, World world, int x, int y, int z) {
    return new ContainerTurretBase(player.inventory, this);
  }

  @Override
  @SideOnly(Side.CLIENT)
  public GuiScreen provideGUI(int ID, EntityPlayer player, World world, int x, int y, int z) {
    return new GUITurretChekhov(player.inventory, this);
  }
}
