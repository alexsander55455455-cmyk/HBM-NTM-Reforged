package com.hbm.inventory;

import com.hbm.blocks.ModBlocks;
import com.hbm.entity.projectile.EntityBrandonBarrel;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemTurretBiometry;
import com.hbm.tileentity.turret.TileEntityTurretBrandon;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.fml.common.registry.GameRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import static org.junit.jupiter.api.Assertions.*;

// Run in an isolated Java 8 process against the downgraded dev JAR.
class BrandonBarrelLauncherTest {
  private static Unsafe unsafe;
  private static LaunchClassLoader previousClassLoader;

  @BeforeAll static void bootstrap() throws Exception {
    previousClassLoader = Launch.classLoader;
    if (previousClassLoader == null) Launch.classLoader = new LaunchClassLoader(new URL[0]);
    AutoCrystallizerOreDictionaryTest.bootstrap();
    GameRegistry.registerTileEntity(TestTurret.class, new ResourceLocation("hbm", "brandon_test_turret"));
    Field field = Unsafe.class.getDeclaredField("theUnsafe");
    field.setAccessible(true);
    unsafe = (Unsafe) field.get(null);
  }

  @AfterAll static void cleanup() throws Exception {
    if (previousClassLoader == null && Launch.classLoader != null) {
      Launch.classLoader.close();
      Launch.classLoader = null;
    }
  }

  private TestWorld world(boolean blocked) throws Exception {
    TestWorld world = (TestWorld) unsafe.allocateInstance(TestWorld.class);
    world.spawned = new ArrayList<>();
    world.acceptSpawn = true;
    Field provider = World.class.getDeclaredField("provider");
    provider.setAccessible(true);
    provider.set(world, new WorldProviderSurface());
    TestChunk chunk = (TestChunk) unsafe.allocateInstance(TestChunk.class);
    chunk.sections = new ExtendedBlockStorage[16];
    if (blocked) {
      // A solid ceiling at Y=70 blocks the arc without obstructing a level sight line at Y=65.
      ExtendedBlockStorage section = new ExtendedBlockStorage(64, true);
      for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++)
        section.set(x, 6, z, Blocks.STONE.getDefaultState());
      chunk.sections[4] = section;
    }
    world.chunks = (IChunkProvider) Proxy.newProxyInstance(IChunkProvider.class.getClassLoader(),
        new Class<?>[]{IChunkProvider.class}, (proxy, method, args) ->
            method.getReturnType() == Chunk.class ? chunk : null);
    return world;
  }

  private TestTurret turret(TestWorld world) {
    TestTurret turret = new TestTurret();
    turret.setWorld(world);
    turret.setPos(new BlockPos(0, 64, 0));
    turret.power = 10000;
    turret.isOn = true;
    turret.aligned = true;
    turret.tPos = new Vec3d(0, 65.5, 36);
    turret.inventory.setStackInSlot(1, new ItemStack(ModBlocks.red_barrel, 3));
    for (int i = 0; i < 40; i++) turret.aim();
    return turret;
  }

  @Test void successfulShotConsumesExactlyOneBarrelAndOneEnergyCharge() throws Exception {
    TestWorld world = world(false);
    TestTurret turret = turret(world);
    turret.updateFiringTick();
    assertEquals(1, world.spawned.size());
    assertTrue(world.spawned.get(0) instanceof EntityBrandonBarrel);
    assertEquals(2, turret.inventory.getStackInSlot(1).getCount());
    assertEquals(9000, turret.power);
    assertEquals(80, turret.getReloadTicks());
    assertFalse(turret.hasLoadedBarrel());
    turret.updateFiringTick();
    assertEquals(1, world.spawned.size());
    assertEquals(9000, turret.power);
  }

  @Test void idleAndReloadingTurretDoesNotDrainPowerAndWaitsFullFourSeconds() throws Exception {
    TestWorld world = world(false);
    TestTurret turret = turret(world);
    turret.updateFiringTick();
    turret.isOn = false;
    for (int i = 0; i < 59; i++) turret.update();
    assertEquals(21, turret.getReloadTicks());
    assertFalse(turret.hasLoadedBarrel());
    turret.update();
    assertTrue(turret.hasLoadedBarrel());
    for (int i = 0; i < 20; i++) turret.update();
    assertEquals(0, turret.getReloadTicks());
    assertEquals(9000, turret.power);
    assertEquals(1, world.spawned.size());
  }

  @Test void blockedArcAndCancelledSpawnDoNotConsumeResources() throws Exception {
    TestWorld blocked = world(true);
    TestTurret turret = turret(blocked);
    turret.updateFiringTick();
    assertTrue(blocked.spawned.isEmpty());
    assertEquals(10000, turret.power);
    assertEquals(3, turret.inventory.getStackInSlot(1).getCount());
    TestWorld cancelled = world(false);
    cancelled.acceptSpawn = false;
    turret = turret(cancelled);
    turret.updateFiringTick();
    assertTrue(cancelled.spawned.isEmpty());
    assertEquals(10000, turret.power);
    assertEquals(3, turret.inventory.getStackInSlot(1).getCount());
    assertEquals(0, turret.getReloadTicks());
  }

  @Test void powerAmmoRangeSwitchAndClientGuardsPreventShots() throws Exception {
    for (int mode = 0; mode < 7; mode++) {
      TestWorld world = world(false);
      TestTurret turret = turret(world);
      if (mode == 0) turret.power = 999;
      if (mode == 1) turret.inventory.setStackInSlot(1, ItemStack.EMPTY);
      if (mode == 2) turret.inventory.setStackInSlot(1, new ItemStack(ModBlocks.pink_barrel, 3));
      if (mode == 3) turret.tPos = new Vec3d(0, 65.5, 7);
      if (mode == 4) turret.tPos = new Vec3d(0, 65.5, 49);
      if (mode == 5) turret.isOn = false;
      if (mode == 6) {
        Field remote = World.class.getDeclaredField("isRemote");
        remote.setAccessible(true);
        remote.setBoolean(world, true);
      }
      long before = turret.power;
      turret.updateFiringTick();
      assertTrue(world.spawned.isEmpty(), "mode " + mode);
      assertEquals(before, turret.power);
    }
  }

  @Test void onlyRedBarrelsEnterAmmoSlotsThroughItemHandlers() {
    TestTurret turret = new TestTurret();
    ItemStack red = new ItemStack(ModBlocks.red_barrel, 8);
    for (int slot = 1; slot <= 9; slot++) {
      assertTrue(turret.inventory.insertItem(slot, red.copy(), false).isEmpty());
      assertEquals(1, turret.inventory.insertItem(slot, new ItemStack(Blocks.DIRT), false).getCount());
      assertFalse(turret.isItemValidForSlot(slot, new ItemStack(ModBlocks.pink_barrel)));
    }
    assertFalse(turret.inventory.isItemValid(0, red));
    assertFalse(turret.inventory.isItemValid(10, red));
    assertTrue(turret.inventory.isItemValid(0, new ItemStack(ModItems.turret_chip)));
    assertTrue(turret.inventory.isItemValid(10, new ItemStack(ModItems.battery_creative)));
    assertEquals(ModBlocks.red_barrel, net.minecraft.block.Block.getBlockFromItem(
        turret.getAmmoTypesForDisplay().get(0).getItem()));
  }

  @Test void disabledTargetCategoryAndBiometryWhitelistPreventFiringAtRetainedTarget() throws Exception {
    TestWorld world = world(false);
    TestTurret turret = turret(world);
    TestMob mob = new TestMob(world);
    mob.setPosition(0, 65, 36);
    mob.prevPosX = mob.posX;
    mob.prevPosY = mob.posY;
    mob.prevPosZ = mob.posZ;
    turret.target = mob;
    turret.targetMobs = false;
    turret.updateFiringTick();
    assertTrue(world.spawned.isEmpty());
    turret.targetMobs = true;
    mob.setCustomNameTag("Protected");
    ItemStack chip = new ItemStack(ModItems.turret_chip);
    ItemTurretBiometry.addName(chip, "Protected");
    turret.inventory.setStackInSlot(0, chip);
    turret.updateFiringTick();
    assertTrue(world.spawned.isEmpty());
    assertEquals(10000, turret.power);
    turret.inventory.setStackInSlot(0, ItemStack.EMPTY);
    turret.updateFiringTick();
    assertEquals(1, world.spawned.size());
  }

  @Test void ownerIdentitySurvivesReloadAndOverridesHostileTargetCategory() throws Exception {
    TestWorld world = world(false);
    TestMob owner = new TestMob(world);
    TestTurret turret = turret(world);
    assertTrue(turret.entityAcceptableTarget(owner));
    turret.setOwner(owner.getUniqueID());
    assertFalse(turret.entityAcceptableTarget(owner));
    TestTurret restored = new TestTurret();
    restored.readFromNBT(turret.writeToNBT(new NBTTagCompound()));
    assertFalse(restored.entityAcceptableTarget(owner));
    assertTrue(restored.entityAcceptableTarget(new TestMob(world)));
  }

  @Test void movingTargetIsLedWithoutSteeringTheProjectileAfterLaunch() throws Exception {
    TestWorld world = world(false);
    TestTurret turret = turret(world);
    TestMob mob = new TestMob(world);
    mob.setPosition(0, 65, 36);
    mob.prevPosX = -0.1;
    mob.prevPosY = mob.posY;
    mob.prevPosZ = mob.posZ;
    turret.target = mob;
    turret.tPos = turret.getEntityPos(mob);
    for (int i = 0; i < 40; i++) turret.aim();
    turret.updateFiringTick();
    assertEquals(1, world.spawned.size());
    Entity shot = world.spawned.get(0);
    double time = (mob.posZ - shot.posZ) / shot.motionZ;
    TestBarrel barrel = new TestBarrel(world);
    barrel.setPosition(shot.posX, shot.posY, shot.posZ);
    barrel.motionX = shot.motionX;
    barrel.motionY = shot.motionY;
    barrel.motionZ = shot.motionZ;
    for (int i = 0; i < (int) time; i++) barrel.onUpdate();
    Vec3d hit = barrel.getPositionVector().add(new Vec3d(
        barrel.motionX, barrel.motionY, barrel.motionZ).scale(time - (int) time));
    assertEquals(0, hit.distanceTo(turret.tPos.add(time * 0.1, 0, 0)), 0.005);
  }

  @Test void cooldownInventoryAndLoadedStateSurviveSaveAndNetworkSync() throws Exception {
    TestTurret turret = turret(world(false));
    turret.updateFiringTick();
    TestTurret restored = new TestTurret();
    restored.readFromNBT(turret.writeToNBT(new NBTTagCompound()));
    assertEquals(80, restored.getReloadTicks());
    assertEquals(9000, restored.power);
    assertEquals(2, restored.inventory.getStackInSlot(1).getCount());
    TestWorld clientWorld = world(false);
    Field remote = World.class.getDeclaredField("isRemote");
    remote.setAccessible(true);
    remote.setBoolean(clientWorld, true);
    TestTurret client = new TestTurret();
    client.setWorld(clientWorld);
    ByteBuf buf = Unpooled.buffer();
    try {
      restored.serialize(buf);
      client.deserialize(buf);
      assertEquals(80, client.getReloadTicks());
      assertFalse(client.hasLoadedBarrel());
      for (int i = 0; i < 60; i++) client.update();
      assertTrue(client.hasLoadedBarrel());
      assertEquals(0, buf.readableBytes());
    } finally { buf.release(); }
  }

  @Test void calculatedVelocityReachesTargetsUsingActualProjectileTicks() throws Exception {
    TestWorld world = world(false);
    for (double distance : new double[]{8, 24.4, 48}) {
      for (double angle : new double[]{0, Math.PI / 2, Math.PI, -Math.PI / 2}) {
        for (double height : new double[]{-2, 0, 4}) {
          Vec3d start = new Vec3d(0, 65, 0);
          Vec3d end = start.add(Math.sin(angle) * distance, height, Math.cos(angle) * distance);
          Vec3d velocity = TileEntityTurretBrandon.getLaunchVelocity(start, end);
          assertNotNull(velocity);
          TestBarrel barrel = new TestBarrel(world);
          barrel.setPosition(start.x, start.y, start.z);
          barrel.motionX = velocity.x;
          barrel.motionY = velocity.y;
          barrel.motionZ = velocity.z;
          double time = distance / 1.2;
          for (int i = 0; i < (int) time; i++) barrel.onUpdate();
          Vec3d hit = barrel.getPositionVector().add(
              new Vec3d(barrel.motionX, barrel.motionY, barrel.motionZ).scale(time - (int) time));
          assertEquals(0, hit.distanceTo(end), 0.000001, "distance " + distance + ", height " + height);
        }
      }
    }
    assertNull(TileEntityTurretBrandon.getLaunchVelocity(Vec3d.ZERO, new Vec3d(1, 40, 0)));
  }

  @Test void impactExplodesOnceAtActualHitWithRedBarrelStrength() throws Exception {
    TestWorld world = world(false);
    TestBarrel barrel = new TestBarrel(world);
    Vec3d hit = new Vec3d(7.25, 66.5, 22.75);
    barrel.impact(new RayTraceResult(hit, net.minecraft.util.EnumFacing.UP, new BlockPos(hit)));
    barrel.impact(new RayTraceResult(hit, net.minecraft.util.EnumFacing.UP, new BlockPos(hit)));
    assertTrue(barrel.isDead);
    assertEquals(1, world.explosions);
    assertEquals(hit, world.explosionPosition);
    assertEquals(2.5F, world.strength);
    assertTrue(world.fire);
    assertTrue(world.terrain);
  }

  private static class TestTurret extends TileEntityTurretBrandon {
    void aim() { alignTurret(); }
    @Override public Vec3d getTurretPos() { return new Vec3d(0, 65.5, 0); }
    @Override public void markDirty() { }
    @Override public void updateConnections() { }
    @Override public void networkPackNT(int range) { }
  }

  private static class TestBarrel extends EntityBrandonBarrel {
    TestBarrel(World world) { super(world); }
    void impact(RayTraceResult hit) { onImpact(hit); }
    @Override public void onEntityUpdate() { }
    @Override public boolean isInWater() { return false; }
    @Override public boolean doesImpactEntities() { return false; }
  }

  private static class TestMob extends EntityLiving implements IMob {
    TestMob(World world) { super(world); }
    @Override protected void initEntityAI() { }
  }

  private static class TestChunk extends Chunk {
    ExtendedBlockStorage[] sections;
    private TestChunk() { super(null, 0, 0); }
    @Override public ExtendedBlockStorage[] getBlockStorageArray() { return sections; }
  }

  private static class TestWorld extends World {
    List<Entity> spawned;
    IChunkProvider chunks;
    boolean acceptSpawn, fire, terrain;
    int explosions;
    float strength;
    Vec3d explosionPosition;
    private TestWorld() { super(null, null, null, null, false); }
    @Override public IChunkProvider getChunkProvider() { return chunks; }
    @Override protected IChunkProvider createChunkProvider() { return chunks; }
    @Override protected boolean isChunkLoaded(int x, int z, boolean allowEmpty) { return true; }
    @Override public IBlockState getBlockState(BlockPos pos) { return Blocks.AIR.getDefaultState(); }
    @Override public boolean spawnEntity(Entity entity) {
      if (acceptSpawn) spawned.add(entity);
      return acceptSpawn;
    }
    @Override public void playSound(EntityPlayer player, BlockPos pos, SoundEvent sound,
        SoundCategory category, float volume, float pitch) { }
    @Override public Explosion newExplosion(Entity entity, double x, double y, double z,
        float strength, boolean fire, boolean terrain) {
      explosions++;
      this.strength = strength;
      this.fire = fire;
      this.terrain = terrain;
      explosionPosition = new Vec3d(x, y, z);
      return null;
    }
  }
}
