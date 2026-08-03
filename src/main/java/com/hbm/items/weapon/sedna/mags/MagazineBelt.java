package com.hbm.items.weapon.sedna.mags;

import com.hbm.items.ModItems;
import com.hbm.inventory.BackpackAmmoProvider;
import com.hbm.items.tool.ItemAmmoBag;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.particle.SpentCasing;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MagazineBelt implements IMagazine<BulletConfig> {

    public List<BulletConfig> acceptedBullets = new ArrayList<>();

    public MagazineBelt addConfigs(BulletConfig... cfgs) {
        Collections.addAll(acceptedBullets, cfgs);
        return this;
    }

    @Override
    public BulletConfig getType(ItemStack stack, IInventory inventory) {
        BulletConfig config = getFirstConfig(stack, inventory);
        if(this.getMagType(stack) != config.id) {
            this.setMagType(stack, config.id);
        }
        return config;
    }

    @Override
    public void useUpAmmo(ItemStack stack, IInventory inventory, int amount) {

        BulletConfig first = this.getFirstConfig(stack, inventory);

        for(int i = 0; i < inventory.getSizeInventory(); i++) {
            ItemStack slot = inventory.getStackInSlot(i);

            if (!slot.isEmpty()) {
                if (first.ammo.matchesRecipe(slot, true)) {
                    int toRemove = Math.min(slot.getCount(), amount);
                    amount -= toRemove;
                    inventory.decrStackSize(i, toRemove);
                    IMagazine.handleAmmoBag(inventory, first, toRemove);
                    if (amount <= 0) return;
                }


                boolean infBag = slot.getItem() == ModItems.ammo_bag_infinite;
                if (slot.getItem() == ModItems.ammo_bag || infBag) {
                    ItemAmmoBag.InventoryAmmoBag bag = new ItemAmmoBag.InventoryAmmoBag(slot);
                    for (int j = 0; j < bag.getSlots(); j++) {
                        ItemStack bagslot = bag.getStackInSlot(j);

                        if (!bagslot.isEmpty() && first.ammo.matchesRecipe(bagslot, true)) {
                            int planned = Math.min(bagslot.getCount(), amount);
                            int actuallyRemoved = planned;

                            if (!infBag) {
                                ItemStack extracted = bag.extractItem(j, planned, false);
                                actuallyRemoved = extracted.isEmpty() ? 0 : extracted.getCount();
                            }

                            if (actuallyRemoved > 0) {
                                amount -= actuallyRemoved;
                                IMagazine.handleAmmoBag(inventory, first, actuallyRemoved);
                            }

                            if (amount <= 0) return;
                        }
                    }
                }
            }
        }
        if (amount > 0 && inventory instanceof InventoryPlayer playerInventory) {
            BackpackAmmoProvider.extractMatching(playerInventory.player,
                    ammo -> first.ammo.matchesRecipe(ammo, true), amount);
        }
    }

    @Override public void setType(ItemStack stack, BulletConfig type) { }
    @Override public int getCapacity(ItemStack stack) { return 0; }
    @Override public void setAmount(ItemStack stack, int amount) { }
    @Override public boolean canReload(ItemStack stack, IInventory inventory) { return false; }
    @Override public void initNewType(ItemStack stack, IInventory inventory) { }
    @Override public void reloadAction(ItemStack stack, IInventory inventory) { }
    @Override public void setAmountBeforeReload(ItemStack stack, int amount) { }
    @Override public int getAmountBeforeReload(ItemStack stack) { return 0; }
    @Override public void setAmountAfterReload(ItemStack stack, int amount) { }
    @Override public int getAmountAfterReload(ItemStack stack) { return 0; }

    @Override
    public int getAmount(ItemStack stack, IInventory inventory) {
        if(inventory == null) return 1; // for EntityAIFireGun
        BulletConfig first = this.getFirstConfig(stack, inventory);
        int count = 0;
        for(int i = 0; i < inventory.getSizeInventory(); i++) {
            ItemStack slot = inventory.getStackInSlot(i);

            if(!slot.isEmpty()) {
                if(first.ammo.matchesRecipe(slot, true)) count += slot.getCount();

                boolean infBag = slot.getItem() == ModItems.ammo_bag_infinite;
                if(slot.getItem() == ModItems.ammo_bag || infBag) {
                    ItemAmmoBag.InventoryAmmoBag bag = new ItemAmmoBag.InventoryAmmoBag(slot);
                    for(int j = 0; j < bag.getSlots(); j++) {
                        ItemStack bagslot = bag.getStackInSlot(j);

                        if(!bagslot.isEmpty()) {
                            if(first.ammo.matchesRecipe(bagslot, true)) {
                                if(infBag) return 9_999;
                                count += bagslot.getCount();
                            }
                        }
                    }
                }
            }
        }
        if (inventory instanceof InventoryPlayer playerInventory) {
            count += BackpackAmmoProvider.countMatching(playerInventory.player,
                    ammo -> first.ammo.matchesRecipe(ammo, true), Integer.MAX_VALUE - count);
        }
        return count;
    }

    @Override
    public ItemStack getIconForHUD(ItemStack stack, EntityPlayer player) {
        BulletConfig first = this.getFirstConfig(stack, player.inventory);
        return first.ammo.toStack();
    }

    @Override
    public String reportAmmoStateForHUD(ItemStack stack, EntityPlayer player) {
        return "x" + getAmount(stack, player.inventory);
    }

    @Override
    public SpentCasing getCasing(ItemStack stack, IInventory invnetory) {
        return getFirstConfig(stack, invnetory).casing;
    }

    public BulletConfig getFirstConfig(ItemStack stack, IInventory inventory) {

        if(inventory == null) return acceptedBullets.get(0);

        for(int i = 0; i < inventory.getSizeInventory(); i++) {
            ItemStack slot = inventory.getStackInSlot(i);

            if(!slot.isEmpty()) {
                for(BulletConfig config : this.acceptedBullets) {
                    if(config.ammo.matchesRecipe(slot, true)) return config;
                }

                if(slot.getItem() == ModItems.ammo_bag || slot.getItem() == ModItems.ammo_bag_infinite) {
                    ItemAmmoBag.InventoryAmmoBag bag = new ItemAmmoBag.InventoryAmmoBag(slot);
                    for(int j = 0; j < bag.getSlots(); j++) {
                        ItemStack bagslot = bag.getStackInSlot(j);

                        if(!bagslot.isEmpty()) {
                            for(BulletConfig config : this.acceptedBullets) {
                                if(config.ammo.matchesRecipe(bagslot, true)) return config;
                            }
                        }
                    }
                }
            }
        }

        if (inventory instanceof InventoryPlayer playerInventory) {
            for (BulletConfig config : acceptedBullets) {
                if (!BackpackAmmoProvider.findFirst(playerInventory.player,
                        ammo -> config.ammo.matchesRecipe(ammo, true)).isEmpty()) return config;
            }
        }
        BulletConfig cached = BulletConfig.configs.get(this.getMagType(stack));
        return acceptedBullets.contains(cached) ? cached : acceptedBullets.get(0);
    }

    public static final String KEY_MAG_TYPE = "magtype";
    public static int getMagType(ItemStack stack) { return ItemGunBaseNT.getValueInt(stack, KEY_MAG_TYPE); }
    public static void setMagType(ItemStack stack, int value) { ItemGunBaseNT.setValueInt(stack, KEY_MAG_TYPE, value); }
}
