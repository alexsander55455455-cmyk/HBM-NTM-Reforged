package com.hbm.items.weapon.sedna.mags;

import com.hbm.items.ModItems;
import com.hbm.items.tool.ItemAmmoBag;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.particle.SpentCasing;
import com.hbm.util.BobMathUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public abstract class MagazineSingleTypeBase implements IMagazine<BulletConfig> {

    public static final String KEY_MAG_COUNT = "magcount";
    public static final String KEY_MAG_TYPE = "magtype";
    public static final String KEY_MAG_PREV = "magprev";
    public static final String KEY_MAG_AFTER = "magafter";

    public List<BulletConfig> acceptedBullets = new ArrayList<>();

    /** A number so the gun tell multiple mags apart */
    public int index;
    /** How much ammo this mag can hold */
    public int capacity;

    public MagazineSingleTypeBase(int index, int capacity) {
        this.index = index;
        this.capacity = capacity;
    }

    public MagazineSingleTypeBase addConfigs(BulletConfig... cfgs) {
        if (cfgs == null) {
            return this;
        }
        for (BulletConfig cfg : cfgs) {
            if (cfg != null) {
                acceptedBullets.add(cfg);
            }
        }
        return this;
    }

    private static boolean isValidConfig(BulletConfig config) {
        return config != null && config.ammo != null;
    }

    private BulletConfig firstAcceptedConfig() {
        for (BulletConfig cfg : acceptedBullets) {
            if (isValidConfig(cfg)) {
                return cfg;
            }
        }
        return null;
    }

    private BulletConfig resolveConfig(ItemStack stack) {
        BulletConfig config = this.getType(stack, null);
        if (!isValidConfig(config)) {
            config = firstAcceptedConfig();
            if (isValidConfig(config)) {
                this.setType(stack, config);
            }
        }
        return config;
    }

    private static boolean matchesConfigAmmo(BulletConfig config, ItemStack slot) {
        return isValidConfig(config) && config.ammo.matchesRecipe(slot, true);
    }

    @Override
    public BulletConfig getType(ItemStack stack, IInventory inventory) {
        int type = getMagType(stack, index);
        if (type >= 0 && type < BulletConfig.configs.size()) {
            BulletConfig cfg = BulletConfig.configs.get(type);
            if (isValidConfig(cfg) && acceptedBullets.contains(cfg)) {
                return cfg;
            }
            return firstAcceptedConfig();
        }
        return firstAcceptedConfig();
    }

    @Override
    public void setType(ItemStack stack, BulletConfig type) {
        if (!isValidConfig(type)) {
            return;
        }
        int i = BulletConfig.configs.indexOf(type);
        if (i >= 0) {
            setMagType(stack, index, i);
        }
    }

    @Override
    public ItemStack getIconForHUD(ItemStack stack, EntityPlayer player) {
        BulletConfig config = this.getType(stack, player.inventory);
        if (isValidConfig(config)) {
            return config.ammo.toStack();
        }
        return ItemStack.EMPTY;
    }

    @Override
    public String reportAmmoStateForHUD(ItemStack stack, EntityPlayer player) {
        return getAmount(stack, player.inventory) + " / " + getCapacity(stack);
    }

    @Override
    public SpentCasing getCasing(ItemStack stack, IInventory inventory) {
        BulletConfig config = this.getType(stack, inventory);
        return config != null ? config.casing : null;
    }

    @Override
    public void useUpAmmo(ItemStack stack, IInventory inventory, int amount) {
        BulletConfig config = this.getType(stack, inventory);
        if (!isValidConfig(config)) {
            return;
        }
        this.setAmount(stack, this.getAmount(stack, inventory) - amount);
        IMagazine.handleAmmoBag(inventory, config, amount);
    }

    /** Returns true if the player has the same ammo if partially loaded, or any valid ammo if not */
    @Override
    public boolean canReload(ItemStack stack, IInventory inventory) {
        if (this.getAmount(stack, inventory) >= this.getCapacity(stack)) {
            return false;
        }
        if (inventory == null) {
            return true;
        }
        return getFirstConfig(stack, inventory) != null;
    }

    public void standardReload(ItemStack stack, IInventory inventory, int loadLimit) {

        if (inventory == null) {
            BulletConfig config = resolveConfig(stack);
            if (!isValidConfig(config)) {
                return;
            }
            this.setAmount(stack, this.capacity);
            return;
        }

        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            ItemStack slot = inventory.getStackInSlot(i);

            if (loadLimit <= 0) {
                return;
            }

            if (!slot.isEmpty()) {

                //mag is empty, assume next best type
                if (this.getAmount(stack, null) == 0) {

                    for (BulletConfig config : this.acceptedBullets) {
                        if (!matchesConfigAmmo(config, slot)) {
                            continue;
                        }
                        this.setType(stack, config);
                        int wantsToLoad = (int) Math.ceil((double) this.getCapacity(stack) / (double) config.ammoReloadCount);
                        int toLoad = BobMathUtil.min(wantsToLoad, slot.getCount(), loadLimit);
                        this.setAmount(stack, Math.min(toLoad * config.ammoReloadCount, this.capacity));
                        inventory.decrStackSize(i, toLoad);
                        loadLimit -= toLoad;
                        break;
                    }
                    //mag has a type set, only load that
                } else {
                    BulletConfig config = resolveConfig(stack);
                    if (!isValidConfig(config) || !matchesConfigAmmo(config, slot)) {
                        continue;
                    }

                    int alreadyLoaded = this.getAmount(stack, null);
                    int wantsToLoad = (int) Math.ceil((double) (this.getCapacity(stack) - alreadyLoaded) / (double) config.ammoReloadCount);
                    int toLoad = BobMathUtil.min(wantsToLoad, slot.getCount(), loadLimit);
                    this.setAmount(stack, Math.min((toLoad * config.ammoReloadCount) + alreadyLoaded, this.capacity));
                    inventory.decrStackSize(i, toLoad);
                    loadLimit -= toLoad;
                }

                boolean infBag = slot.getItem() == ModItems.ammo_bag_infinite;
                if (slot.getItem() == ModItems.ammo_bag || infBag) {
                    ItemAmmoBag.InventoryAmmoBag bag = new ItemAmmoBag.InventoryAmmoBag(slot);

                    for (int j = 0; j < bag.getSlots(); j++) {
                        ItemStack bagslot = bag.getStackInSlot(j);

                        if (!bagslot.isEmpty()) {

                            //mag is empty, assume next best type
                            if (this.getAmount(stack, null) == 0) {

                                for (BulletConfig config : this.acceptedBullets) {
                                    if (!matchesConfigAmmo(config, bagslot)) {
                                        continue;
                                    }
                                    this.setType(stack, config);
                                    int wantsToLoad = (int) Math.ceil((double) this.getCapacity(stack) / (double) config.ammoReloadCount);
                                    int toLoad = BobMathUtil.min(wantsToLoad, infBag ? 9_999 : bagslot.getCount(), loadLimit);
                                    this.setAmount(stack, Math.min(toLoad * config.ammoReloadCount, this.capacity));
                                    if (!infBag) {
                                        bag.setStackInSlot(j, new ItemStack(bagslot.getItem(), bagslot.getCount() - toLoad, bagslot.getMetadata()));
                                    }
                                    loadLimit -= toLoad;
                                    break;
                                }
                                //mag has a type set, only load that
                            } else {
                                BulletConfig config = resolveConfig(stack);
                                if (!isValidConfig(config) || !matchesConfigAmmo(config, bagslot)) {
                                    continue;
                                }

                                int alreadyLoaded = getMagCount(stack, index);
                                int wantsToLoad = (int) Math.ceil((double) (this.getCapacity(stack) - alreadyLoaded) / (double) config.ammoReloadCount);
                                int toLoad = BobMathUtil.min(wantsToLoad, infBag ? 9_999 : bagslot.getCount(), loadLimit);
                                this.setAmount(stack, Math.min((toLoad * config.ammoReloadCount) + alreadyLoaded, this.capacity));
                                if (!infBag) {
                                    bag.setStackInSlot(j, new ItemStack(bagslot.getItem(), bagslot.getCount() - toLoad, bagslot.getMetadata()));
                                }
                                loadLimit -= toLoad;
                            }
                        }
                    }
                }
            }
        }
    }

    /** Returns the config of the first potential loadable round, either what's already chambered or the first valid one if empty */
    public BulletConfig getFirstConfig(ItemStack stack, IInventory inventory) {
        if (inventory == null) {
            return null;
        }

        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            ItemStack slot = inventory.getStackInSlot(i);

            if (!slot.isEmpty()) {
                if (this.getAmount(stack, null) == 0) {
                    for (BulletConfig config : this.acceptedBullets) {
                        if (matchesConfigAmmo(config, slot)) {
                            return config;
                        }
                    }
                } else {
                    BulletConfig config = resolveConfig(stack);
                    if (matchesConfigAmmo(config, slot)) {
                        return config;
                    }
                }

                if (slot.getItem() == ModItems.ammo_bag || slot.getItem() == ModItems.ammo_bag_infinite) {
                    ItemAmmoBag.InventoryAmmoBag bag = new ItemAmmoBag.InventoryAmmoBag(slot);

                    for (int j = 0; j < bag.getSlots(); j++) {
                        ItemStack bagslot = bag.getStackInSlot(j);

                        if (!bagslot.isEmpty()) {
                            if (this.getAmount(stack, null) == 0) {
                                for (BulletConfig config : this.acceptedBullets) {
                                    if (matchesConfigAmmo(config, bagslot)) {
                                        return config;
                                    }
                                }
                            } else {
                                BulletConfig config = resolveConfig(stack);
                                if (matchesConfigAmmo(config, bagslot)) {
                                    return config;
                                }
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    @Override public void initNewType(ItemStack stack, IInventory inventory) {
        if (inventory == null) {
            return;
        }
        BulletConfig nextConfig = getFirstConfig(stack, inventory);
        if (nextConfig != null) {
            int i = BulletConfig.configs.indexOf(nextConfig);
            this.setMagType(stack, index, i);
        }
    }

    @Override public int getCapacity(ItemStack stack) { return capacity; }
    @Override public int getAmount(ItemStack stack, IInventory inventory) { return getMagCount(stack, index); }
    @Override public void setAmount(ItemStack stack, int amount) { setMagCount(stack, index, amount); }

    @Override public void setAmountBeforeReload(ItemStack stack, int amount) { ItemGunBaseNT.setValueInt(stack, KEY_MAG_PREV + index, amount); }
    @Override public int getAmountBeforeReload(ItemStack stack) { return ItemGunBaseNT.getValueInt(stack, KEY_MAG_PREV + index); }
    @Override public void setAmountAfterReload(ItemStack stack, int amount) { ItemGunBaseNT.setValueInt(stack, KEY_MAG_AFTER + index, amount); }
    @Override public int getAmountAfterReload(ItemStack stack) { return ItemGunBaseNT.getValueInt(stack, KEY_MAG_AFTER + index); }

    // MAG TYPE //
    public static int getMagType(ItemStack stack, int index) { return ItemGunBaseNT.getValueInt(stack, KEY_MAG_TYPE + index); } //TODO: replace with named tags to avoid ID shifting
    public static void setMagType(ItemStack stack, int index, int value) { ItemGunBaseNT.setValueInt(stack, KEY_MAG_TYPE + index, value); }

    // MAG COUNT //
    public static int getMagCount(ItemStack stack, int index) { return ItemGunBaseNT.getValueInt(stack, KEY_MAG_COUNT + index); }
    public static void setMagCount(ItemStack stack, int index, int value) { ItemGunBaseNT.setValueInt(stack, KEY_MAG_COUNT + index, value); }
}