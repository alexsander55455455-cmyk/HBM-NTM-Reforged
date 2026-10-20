package com.hbm.inventory;

import com.google.gson.GsonBuilder;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.*;
import com.hbm.items.weapon.sedna.factory.*;
import com.hbm.items.weapon.sedna.mags.*;
import com.hbm.items.weapon.sedna.mods.*;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

// Run in an isolated Java 8 process against the downgraded dev artifact.
class MK108UpgradeTest {
    @BeforeAll static void bootstrap() throws Exception {
        DataConsumerRecipeTest.bootstrap(); GunFactory.init();
        for(net.minecraft.item.Item item : ModItems.ALL_ITEMS)
            if(!net.minecraftforge.fml.common.registry.ForgeRegistries.ITEMS.containsKey(item.getRegistryName()))
                net.minecraftforge.fml.common.registry.ForgeRegistries.ITEMS.register(item);
        XWeaponModManager.init();
    }
    private static ItemStack gun() { return new ItemStack(ModItems.gun_mk108); }
    private static ItemStack speed() { return new ItemStack(ModItems.weapon_mod_special, 1, GunFactory.EnumModSpecial.SPEEDUP.ordinal()); }
    private static ItemStack damage() { return new ItemStack(ModItems.weapon_mod_generic, 1, GunFactory.EnumModGeneric.WSTEEL_DAMAGE.ordinal()); }
    private static ItemStack durability() { return new ItemStack(ModItems.weapon_mod_generic, 1, GunFactory.EnumModGeneric.WSTEEL_DURA.ordinal()); }
    private static GunConfig config(ItemStack stack) { return ((ItemGunBaseNT)ModItems.gun_mk108).getConfig(stack, 0); }
    private static Receiver receiver(ItemStack stack) { return config(stack).getReceivers(stack)[0]; }
    private static IMagazine magazine(ItemStack stack) { return receiver(stack).getMagazine(stack); }
    private static void loaded(ItemStack stack) { magazine(stack).setType(stack, XFactory40mm.g40_heat); magazine(stack).setAmount(stack, 23); }

    @Test void stockReceiverAndFiveAmmoTypesArePreserved() {
        ItemStack stack = gun(); Receiver r = receiver(stack);
        assertEquals(1, config(stack).getReceivers(stack).length); assertEquals(10, r.getDelayAfterFire(stack)); assertEquals(10, r.getDelayAfterDryFire(stack));
        assertTrue(r.getRefireOnHold(stack)); assertEquals(30, magazine(stack).getCapacity(stack)); assertEquals(5000F, config(stack).getDurability(stack));
        assertEquals(Arrays.asList(XFactory40mm.g40_he, XFactory40mm.g40_heat, XFactory40mm.g40_demo, XFactory40mm.g40_inc, XFactory40mm.g40_phosphorus), ((MagazineSingleTypeBase)magazine(stack)).acceptedBullets);
    }
    @Test void auxiliaryEngineIsRecognizedAndApplicable() {
        ItemStack stack = gun(); assertTrue(XWeaponModManager.isApplicable(stack, speed(), 0, false));
        assertTrue(((ItemGunBaseNT)ModItems.gun_mk108).recognizedMods.contains(new RecipesCommon.ComparableStack(speed())));
    }
    @Test void installedEngineHalvesBothDelaysAndKeepsDamageCapacityAndDurability() {
        ItemStack stack = gun(); float damage = receiver(stack).getBaseDamage(stack); XWeaponModManager.install(stack, 0, speed());
        assertEquals(5, receiver(stack).getDelayAfterFire(stack)); assertEquals(5, receiver(stack).getDelayAfterDryFire(stack));
        assertEquals(damage, receiver(stack).getBaseDamage(stack)); assertEquals(30, magazine(stack).getCapacity(stack)); assertEquals(5000F, config(stack).getDurability(stack));
    }
    @Test void weaponSteelDamageAndDurabilityUpgradesWork() {
        ItemStack stack = gun(); float base = receiver(stack).getBaseDamage(stack);
        assertTrue(XWeaponModManager.isApplicable(stack, damage(), 0, false)); assertTrue(XWeaponModManager.isApplicable(stack, durability(), 0, false));
        XWeaponModManager.install(stack, 0, damage(), durability());
        assertEquals(base * 1.15F, receiver(stack).getBaseDamage(stack)); assertEquals(10000F, config(stack).getDurability(stack)); assertEquals(10, receiver(stack).getDelayAfterFire(stack));
    }
    @Test void allThreeUpgradesUseTheExistingIds() {
        ItemStack stack = gun(); XWeaponModManager.install(stack, 0, speed(), damage(), durability());
        Set<Integer> ids = new HashSet<>(); for(int id : stack.getTagCompound().getIntArray(XWeaponModManager.KEY_MOD_LIST + 0)) ids.add(id);
        assertEquals(new HashSet<>(Arrays.asList(108, 109, 209)), ids); assertEquals(3, XWeaponModManager.getUpgradeItems(stack, 0).length);
    }
    @Test void removingEngineRestoresDelayAndPreservesLoadedAmmoAndOtherNbt() {
        ItemStack stack = gun(); loaded(stack); ItemGunBaseNT.setWear(stack, 0, 17F); stack.getTagCompound().setString("testOwner", "existing");
        XWeaponModManager.install(stack, 0, speed()); XWeaponModManager.uninstall(stack, 0);
        assertEquals(10, receiver(stack).getDelayAfterFire(stack)); assertEquals(23, magazine(stack).getAmount(stack, null)); assertSame(XFactory40mm.g40_heat, magazine(stack).getType(stack, null));
        assertEquals(17F, ItemGunBaseNT.getWear(stack, 0)); assertEquals("existing", stack.getTagCompound().getString("testOwner"));
    }
    @Test void installedUpgradesAndLoadedMagazineSurviveNbtRoundtrip() {
        ItemStack stack = gun(); loaded(stack); XWeaponModManager.install(stack, 0, speed(), damage(), durability()); ItemStack copy = new ItemStack(stack.serializeNBT());
        assertEquals(5, receiver(copy).getDelayAfterFire(copy)); assertEquals(10000F, config(copy).getDurability(copy)); assertEquals(23, magazine(copy).getAmount(copy, null)); assertSame(XFactory40mm.g40_heat, magazine(copy).getType(copy, null));
        assertEquals(3, XWeaponModManager.getUpgradeItems(copy, 0).length);
    }
    @Test void previouslySavedEngineIdStillWorksWithoutMigration() {
        ItemStack stack = gun(); NBTTagCompound tag = new NBTTagCompound(); tag.setIntArray(XWeaponModManager.KEY_MOD_LIST + 0, new int[]{209, 987654}); stack.setTagCompound(tag);
        assertEquals(5, receiver(stack).getDelayAfterFire(stack)); assertArrayEquals(new int[]{209, 987654}, tag.getIntArray(XWeaponModManager.KEY_MOD_LIST + 0));
    }
    @Test void existingShotgunAndMinigunEngineBindingsRemainValid() {
        for(net.minecraft.item.Item item : new net.minecraft.item.Item[]{ModItems.gun_autoshotgun, ModItems.gun_autoshotgun_shredder, ModItems.gun_minigun_sedna, ModItems.gun_minigun_dual}) {
            ItemStack stack = new ItemStack(item); assertTrue(XWeaponModManager.isApplicable(stack, speed(), 0, false));
        }
    }
    @Test void fullReloadStillLoadsThirtyLooseGrenadesWithAndWithoutEngine() {
        for(boolean upgraded : new boolean[]{false, true}) {
            ItemStack stack = gun(); if(upgraded) XWeaponModManager.install(stack, 0, speed()); InventoryBasic inv = new InventoryBasic("test", true, 1);
            ItemStack ammo = XFactory40mm.g40_he.ammo.toStack(); ammo.setCount(40); inv.setInventorySlotContents(0, ammo); magazine(stack).reloadAction(stack, inv);
            assertEquals(30, magazine(stack).getAmount(stack, inv)); assertEquals(10, inv.getStackInSlot(0).getCount()); assertSame(XFactory40mm.g40_he, magazine(stack).getType(stack, inv));
        }
    }
    @Test void speedMutexAndUnrelatedWeaponRejectionRemainEffective() {
        ItemStack stack = gun(); XWeaponModManager.install(stack, 0, speed()); assertFalse(XWeaponModManager.isApplicable(stack, speed(), 0, true));
        assertFalse(XWeaponModManager.isApplicable(new ItemStack(ModItems.gun_flaregun), speed(), 0, false));
    }
    @Test void archiveAllExistingModIdsAndGunBindings() throws Exception {
        String path = System.getProperty("mk108.catalog"); if(path == null) return;
        Map<String,Object> result = new TreeMap<>(); Map<Integer,String> ids = new TreeMap<>(); Map<String,Integer> bindings = new TreeMap<>();
        XWeaponModManager.idToMod.forEach((id,mod) -> ids.put(id, mod.getClass().getName() + "|" + XWeaponModManager.modToStack.get(mod).serializeNBT()));
        XWeaponModManager.stackToMod.forEach((upgrade,def) -> def.modByGun.forEach((weapon,mod) -> bindings.put((weapon == null ? "*" : weapon.item.getRegistryName() + ":" + weapon.meta) + "|" + upgrade.item.getRegistryName() + ":" + upgrade.meta, XWeaponModManager.idToMod.inverse().get(mod))));
        result.put("ids", ids); result.put("bindings", bindings); Files.write(Paths.get(path), new GsonBuilder().setPrettyPrinting().create().toJson(result).getBytes(StandardCharsets.UTF_8));
    }
}
