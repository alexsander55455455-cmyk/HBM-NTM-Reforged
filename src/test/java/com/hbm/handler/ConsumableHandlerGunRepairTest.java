package com.hbm.handler;

import com.hbm.blocks.fluid.ModFluids;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.ItemGunBase;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.main.MainRegistry;
import com.hbm.main.MaterialRegistry;
import com.hbm.testutil.ForgeTestBootstrap;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundEvent;
import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnJre;
import org.junit.jupiter.api.condition.JRE;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Forge enum registration requires Java 8; run against the downgraded dev JAR.
@EnabledOnJre(JRE.JAVA_8)
class ConsumableHandlerGunRepairTest {

    @BeforeAll
    static void bootstrap() throws Exception {
        ForgeTestBootstrap.ensureServerSide();
        MainRegistry.logger = LogManager.getLogger("GunRepairTest");
        if (!Bootstrap.isRegistered()) Bootstrap.register();
        HBMSoundHandler.init();
        MaterialRegistry.init();
        ModFluids.init();
    }

    @Test
    void oilRepairsSauerInTheHotbar() throws Exception {
        RecordingContext context = context(false);
        ItemStack gun = new ItemStack(ModItems.gun_sauer);
        ItemGunBase.setItemWear(gun, 900);
        context.user.inventory.mainInventory.set(1, gun);

        use(context, false);

        assertEquals(600, ItemGunBase.getItemWear(gun));
        assertUsedOnce(context);
    }

    @Test
    void repairKitRepairsSauerInTheOffhand() throws Exception {
        RecordingContext context = context(true);
        ItemStack gun = new ItemStack(ModItems.gun_sauer);
        ItemGunBase.setItemWear(gun, 2400);
        context.user.inventory.offHandInventory.set(0, gun);

        use(context, true);

        assertEquals(900, ItemGunBase.getItemWear(gun));
        assertUsedOnce(context);
    }

    @Test
    void repairStopsAtZeroAndPreservesOtherWeaponData() throws Exception {
        RecordingContext context = context(true);
        ItemStack gun = new ItemStack(ModItems.gun_ks23);
        ItemGunBase.setItemWear(gun, 10);
        ItemGunBase.setMag(gun, 3);
        gun.getTagCompound().setString("testMarker", "keep");
        context.user.inventory.mainInventory.set(1, gun);

        use(context, true);

        assertEquals(0, ItemGunBase.getItemWear(gun));
        assertEquals(3, ItemGunBase.getMag(gun));
        assertEquals("keep", gun.getTagCompound().getString("testMarker"));
        assertUsedOnce(context);
    }

    @Test
    void mixedWeaponsKeepModernRepairBehaviorAndConsumeOneUse() throws Exception {
        RecordingContext context = context(false);
        ItemStack legacy = new ItemStack(ModItems.gun_sauer);
        ItemGunBase.setItemWear(legacy, 900);
        context.user.inventory.mainInventory.set(1, legacy);
        ItemStack modern = new ItemStack(new TestModernGun());
        ItemGunBaseNT.setWear(modern, 0, 500F);
        ItemGunBaseNT.setWear(modern, 1, 1700F);
        context.user.inventory.mainInventory.set(8, modern);

        use(context, false);

        assertEquals(600, ItemGunBase.getItemWear(legacy));
        assertEquals(250F, ItemGunBaseNT.getWear(modern, 0));
        assertEquals(1200F, ItemGunBaseNT.getWear(modern, 1));
        assertUsedOnce(context);
    }

    @Test
    void healthyWeaponsAndWeaponsOutsideTheHotbarDoNotConsumeTheKit() throws Exception {
        RecordingContext context = context(true);
        context.user.inventory.mainInventory.set(1, new ItemStack(ModItems.gun_sauer));
        ItemStack outside = new ItemStack(ModItems.gun_sauer);
        ItemGunBase.setItemWear(outside, 900);
        context.user.inventory.mainInventory.set(10, outside);

        use(context, true);

        assertEquals(EnumActionResult.FAIL, context.actionResult);
        assertEquals(0, context.uses);
        assertEquals(0, context.sounds);
        assertEquals(0, context.user.getHeldItemMainhand().getItemDamage());
        assertEquals(900, ItemGunBase.getItemWear(outside));
    }

    private static RecordingContext context(boolean repairKit) throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        EntityPlayerMP player = (EntityPlayerMP) ((Unsafe) field.get(null)).allocateInstance(EntityPlayerMP.class);
        player.inventory = new InventoryPlayer(player);
        player.inventory.mainInventory.set(0, new ItemStack(repairKit ? ModItems.gun_kit_2 : ModItems.gun_kit_1));
        return new RecordingContext(player);
    }

    private static void use(RecordingContext context, boolean repairKit) throws Exception {
        Method method = ConsumableHandler.class.getDeclaredMethod(repairKit ? "handleGunKit2" : "handleGunKit1", ConsumableHandler.Context.class);
        method.setAccessible(true);
        method.invoke(null, context);
    }

    private static void assertUsedOnce(RecordingContext context) {
        assertEquals(EnumActionResult.SUCCESS, context.actionResult);
        assertEquals(1, context.uses);
        assertEquals(1, context.sounds);
        assertEquals(1, context.user.getHeldItemMainhand().getItemDamage());
    }

    private static class RecordingContext extends ConsumableHandler.Context {
        int uses;
        int sounds;

        RecordingContext(EntityPlayerMP player) {
            super(null, player, EnumHand.MAIN_HAND);
        }

        @Override
        public void playSound(SoundEvent sound) {
            sounds++;
        }

        @Override
        public void damageCurrentItem(int damage) {
            uses += damage;
            ItemStack stack = user.getHeldItem(hand);
            stack.setItemDamage(stack.getItemDamage() + damage);
        }
    }

    private static class TestModernGun extends ItemGunBaseNT {
        TestModernGun() {
            super(WeaponQuality.DEBUG, "test_gun_repair", new GunConfig().dura(1000), new GunConfig().dura(2000));
        }

        @Override
        public GunConfig getConfig(ItemStack stack, int index) {
            return super.getConfig(null, index);
        }
    }
}
