package com.hbm.items.weapon;

import com.hbm.blocks.fluid.ModFluids;
import com.hbm.items.ModItems;
import com.hbm.items.ItemEnumMulti;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.factory.GunFactory;
import com.hbm.items.weapon.sedna.impl.ItemGunChemthrower;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.main.MainRegistry;
import com.hbm.main.MaterialRegistry;
import com.hbm.testutil.ForgeTestBootstrap;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Bootstrap;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnJre;
import org.junit.jupiter.api.condition.JRE;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

// Forge enum registration requires Java 8; run against the downgraded dev JAR.
@EnabledOnJre(JRE.JAVA_8)
class ItemAmmoContainerTest {

    @BeforeAll
    static void bootstrap() {
        ForgeTestBootstrap.ensureServerSide();
        MainRegistry.logger = LogManager.getLogger("AmmoContainerTest");
        if (!Bootstrap.isRegistered()) Bootstrap.register();
        HBMSoundHandler.init();
        MaterialRegistry.init();
        ModFluids.init();
        if (ModItems.ammo_standard == null) {
            ModItems.ammo_standard = new ItemEnumMulti<>("ammo_standard", GunFactory.EnumAmmo.VALUES, true, true);
        }
    }

    @Test
    void chemthrowerWithoutAmmoDoesNotCrashOrConsumeTheContainer() throws Exception {
        Fixture fixture = new Fixture(0);
        ItemGunChemthrower chemthrower = chemthrower();
        assertNull(chemthrower.defaultAmmo);
        fixture.player.inventory.mainInventory.set(1, new ItemStack(chemthrower));

        fixture.use();

        assertEquals(1, fixture.stack.getCount());
        assertEquals(0, fixture.ammoCount());
        assertEquals(0, fixture.world.sounds);
        assertEquals(0, fixture.sync.updates);
    }

    @Test
    void mixedInventorySkipsUnsupportedGunsAndSuppliesTheRifle() throws Exception {
        Fixture fixture = new Fixture(0);
        fixture.player.inventory.mainInventory.set(1, new ItemStack(chemthrower()));
        ItemGunBaseNT rifle = gun("test_rifle").setDefaultAmmo(GunFactory.EnumAmmo.BMG50_FMJ, 17);
        fixture.player.inventory.mainInventory.set(2, new ItemStack(rifle));
        ItemGunBaseNT empty = gun("test_empty");
        empty.defaultAmmo = ItemStack.EMPTY;
        fixture.player.inventory.mainInventory.set(3, new ItemStack(empty));

        fixture.use();

        assertEquals(17, fixture.ammoCount());
        assertEquals(17, rifle.defaultAmmo.getCount());
        fixture.assertConsumedOnce();
    }

    @Test
    void constrainedContainerHalvesAmmoAndSkipsExpensiveWeapons() throws Exception {
        Fixture fixture = new Fixture(1);
        fixture.player.inventory.mainInventory.set(1, new ItemStack(chemthrower()));
        fixture.player.inventory.mainInventory.set(2, new ItemStack(
                gun("test_rifle").setDefaultAmmo(GunFactory.EnumAmmo.BMG50_FMJ, 17)));
        fixture.player.inventory.mainInventory.set(3, new ItemStack(
                gun("test_expensive").setDefaultAmmoExpensive(GunFactory.EnumAmmo.BMG50_FMJ, 64)));

        fixture.use();

        assertEquals(9, fixture.ammoCount());
        fixture.assertConsumedOnce();
    }

    @Test
    void containerSuppliesAtMostThreeGunsAndConsumesOneUse() throws Exception {
        Fixture fixture = new Fixture(0);
        for (int i = 1; i <= 5; i++) {
            fixture.player.inventory.mainInventory.set(i, new ItemStack(
                    gun("test_rifle_" + i).setDefaultAmmo(GunFactory.EnumAmmo.BMG50_FMJ, 17)));
        }

        fixture.use();

        assertEquals(51, fixture.ammoCount());
        fixture.assertConsumedOnce();
    }

    private static ItemGunChemthrower chemthrower() {
        return new ItemGunChemthrower(ItemGunBaseNT.WeaponQuality.DEBUG, "test_chemthrower", new GunConfig());
    }

    private static ItemGunBaseNT gun(String name) {
        return new ItemGunBaseNT(ItemGunBaseNT.WeaponQuality.DEBUG, name, new GunConfig());
    }

    private static <T> T allocate(Class<T> type) throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return type.cast(((Unsafe) field.get(null)).allocateInstance(type));
    }

    private static class Fixture {
        final EntityPlayerMP player = allocate(EntityPlayerMP.class);
        final RecordingWorld world = allocate(RecordingWorld.class);
        final RecordingContainer sync = new RecordingContainer();
        final ItemStack stack;

        Fixture(int metadata) throws Exception {
            player.inventory = new InventoryPlayer(player);
            player.inventoryContainer = sync;
            stack = new ItemStack(ModItems.ammo_container, 1, metadata);
            player.inventory.mainInventory.set(0, stack);
        }

        void use() {
            assertEquals(EnumActionResult.SUCCESS,
                    ModItems.ammo_container.onItemRightClick(world, player, EnumHand.MAIN_HAND).getType());
        }

        int ammoCount() {
            return player.inventory.mainInventory.stream()
                    .filter(item -> item.getItem() == ModItems.ammo_standard
                            && item.getItemDamage() == GunFactory.EnumAmmo.BMG50_FMJ.ordinal())
                    .mapToInt(ItemStack::getCount).sum();
        }

        void assertConsumedOnce() {
            assertEquals(0, stack.getCount());
            assertEquals(1, world.sounds);
            assertEquals(1, sync.updates);
        }
    }

    private static class RecordingContainer extends Container {
        int updates;

        @Override public boolean canInteractWith(EntityPlayer player) { return true; }
        @Override public void detectAndSendChanges() { updates++; }
    }

    private static class RecordingWorld extends World {
        int sounds;

        private RecordingWorld() { super(null, null, null, null, false); }

        @Override protected IChunkProvider createChunkProvider() { return null; }
        @Override protected boolean isChunkLoaded(int x, int z, boolean allowEmpty) { return false; }
        @Override public void playSound(EntityPlayer player, double x, double y, double z,
                                       SoundEvent sound, SoundCategory category, float volume, float pitch) {
            sounds++;
        }
    }
}
