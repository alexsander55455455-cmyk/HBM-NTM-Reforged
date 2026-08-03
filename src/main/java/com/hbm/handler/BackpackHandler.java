package com.hbm.handler;

import com.hbm.capability.BackpackCapability;
import com.hbm.config.BackpackConfig;
import com.hbm.hazard.HazardRegistry;
import com.hbm.hazard.HazardSystem;
import com.hbm.inventory.IBackpackInventory;
import com.hbm.inventory.BackpackAmmoProvider;
import com.hbm.inventory.BackpackUpgradeManager;
import com.hbm.inventory.BackpackVirtualStorage;
import com.hbm.items.tool.ItemBackpack;
import com.hbm.items.tool.ItemBlackBoxBackpack;
import com.hbm.items.tool.ItemPocketHoleBackpack;
import com.hbm.items.tool.ItemSmugglerBackpack;
import com.hbm.util.ContaminationUtil;
import com.hbm.lib.Library;
import com.hbm.main.MainRegistry;
import com.hbm.packet.PacketDispatcher;
import com.hbm.packet.toclient.BackpackSlotSyncPacket;
import com.hbm.saveddata.BlackHoleBackpackSavedData;
import com.hbm.saveddata.PocketHoleBackpackSavedData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.text.TextComponentTranslation;

import java.util.List;

public final class BackpackHandler {
    public static final int EQUIPPED_GUI_ID = 74;
    public static final int HELD_GUI_ID = 75;
    public static final int GUI_FEATURE_AUTO_PICKUP = 1;

    private BackpackHandler() {
    }

    public static void equip(EntityPlayer player, ItemStack stack) {
        if (!(stack.getItem() instanceof ItemBackpack)) return;
        prepareBackpackStorage(player, stack);

        ItemStack previous = BackpackCapability.getData(player).getEquippedBackpack().copy();
        BackpackCapability.getData(player).setEquippedBackpack(stack.copy());
        if (!previous.isEmpty()) {
            Library.addToInventoryOrDrop(player, previous);
        }
        if (!player.world.isRemote) {
            player.world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.ITEM_ARMOR_EQUIP_LEATHER, SoundCategory.PLAYERS, 0.8F, 0.9F);
            syncEquipmentState(player);
        }
    }

    /**
     * Updates the one server-authoritative equipment slot. It is deliberately
     * narrower than an ordinary inventory setter: only backpacks may persist
     * here, which also rejects malformed Creative/network writes.
     */
    public static boolean setEquippedBackpack(EntityPlayer player, ItemStack stack) {
        boolean accepted = setEquippedBackpackFromContainer(player, stack);
        syncEquipmentState(player);
        return accepted;
    }

    /**
     * Container transactions already synchronize after slotClick returns.
     * Keeping this setter quiet avoids publishing a half-finished cursor/slot
     * state while the transaction is still in progress.
     */
    public static boolean setEquippedBackpackFromContainer(EntityPlayer player, ItemStack stack) {
        if (player == null) return false;
        if (!stack.isEmpty() && !(stack.getItem() instanceof ItemBackpack)) {
            return false;
        }

        ItemStack stored = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
        if (!stored.isEmpty()) {
            stored.setCount(1);
            BackpackAmmoProvider.stripClientAmmoSummary(stored);
            prepareBackpackStorage(player, stored);
        }
        BackpackCapability.getData(player).setEquippedBackpack(stored);
        return true;
    }

    /** Sends both ordinary inventory and capability-backed slot state after a server mutation. */
    public static void syncEquipmentState(EntityPlayer player) {
        if (player == null || player.world.isRemote) return;

        player.inventory.markDirty();
        if (player.inventoryContainer != null) {
            player.inventoryContainer.detectAndSendChanges();
        }
        if (player.openContainer != null && player.openContainer != player.inventoryContainer) {
            player.openContainer.detectAndSendChanges();
        }
        if (player instanceof EntityPlayerMP) {
            syncToClient((EntityPlayerMP) player);
        }
    }

    public static void openEquippedBackpack(EntityPlayer player) {
        ItemStack backpack = BackpackCapability.getData(player).getEquippedBackpack();
        prepareBackpackStorage(player, backpack);
        if (!canOpenBlackBox(player, backpack)) return;
        if (player instanceof EntityPlayerMP) syncToClient((EntityPlayerMP) player);
        player.openGui(MainRegistry.instance, EQUIPPED_GUI_ID, player.world,
                getCapacity(backpack), 0, getGuiFeatures(backpack));
    }

    public static void openHeldBackpack(EntityPlayer player, EnumHand hand) {
        ItemStack backpack = player.getHeldItem(hand);
        prepareBackpackStorage(player, backpack);
        if (!canOpenBlackBox(player, backpack)) return;
        player.openGui(MainRegistry.instance, HELD_GUI_ID, player.world,
                hand.ordinal(), getCapacity(backpack), getGuiFeatures(backpack));
    }

    private static boolean canOpenBlackBox(EntityPlayer player, ItemStack backpack) {
        if (!(backpack.getItem() instanceof ItemBlackBoxBackpack blackBox) || blackBox.canAccess(backpack, player)) {
            return true;
        }
        if (!player.world.isRemote) {
            player.sendStatusMessage(new TextComponentTranslation("message.backpack.black_box.access_denied"), true);
        }
        return false;
    }

    private static int getCapacity(ItemStack backpack) {
        if (backpack.getItem() instanceof ItemPocketHoleBackpack pocketHole) {
            return pocketHole.getCachedVirtualSlotCount(backpack);
        }
        return backpack.getItem() instanceof ItemBackpack item ? item.getInitialViewCapacity(backpack) : 0;
    }

    private static int getGuiFeatures(ItemStack backpack) {
        return BackpackUpgradeManager.supportsAutoPickup(backpack) ? GUI_FEATURE_AUTO_PICKUP : 0;
    }

    public static void syncToClient(EntityPlayerMP player) {
        ItemStack backpack = BackpackCapability.getData(player).getEquippedBackpack();
        PacketDispatcher.sendTo(new BackpackSlotSyncPacket(
                BackpackAmmoProvider.createClientSyncedBackpack(player, backpack)), player);
    }

    public static void updateEquippedBackpack(EntityPlayer player) {
        boolean stateChanged = updateBackpack(player, BackpackCapability.getData(player).getEquippedBackpack(), true);

        for (ItemStack backpack : player.inventory.mainInventory) {
            stateChanged |= updateBackpack(player, backpack, false);
        }
        for (ItemStack backpack : player.inventory.armorInventory) {
            stateChanged |= updateBackpack(player, backpack, false);
        }
        for (ItemStack backpack : player.inventory.offHandInventory) {
            stateChanged |= updateBackpack(player, backpack, false);
        }
        if (stateChanged) {
            syncEquipmentState(player);
        }
    }

    private static boolean updateBackpack(EntityPlayer player, ItemStack backpack, boolean equipped) {
        if (!(backpack.getItem() instanceof ItemBackpack item)) return false;

        boolean stateChanged = prepareBackpackStorage(player, backpack);
        stateChanged |= item.onBackpackTick(player, backpack, equipped);

        if (item.isLead()) {
            // Four lead ores use the same HBM toxic-hazard path, including its
            // gas-mask and hazmat protection checks.
            HazardRegistry.TOXIC.onUpdate(player, HazardRegistry.ore * 4D, backpack);
        }
        if (item.isAsbestos() && player.ticksExisted % 5 != 0) {
            // Raw asbestos applies level 1 every tick. Skipping one tick in
            // five preserves the native fine-particle protection path while
            // making the sealed backpack slightly weaker than loose asbestos.
            HazardRegistry.ASBESTOS.onUpdate(player, 1D, backpack);
        }

        // Automation belongs to the backpack in the dedicated equipped slot.
        // Inventory backpacks still tick their passive material behavior, but
        // must not keep collecting after the equipped backpack is switched OFF.
        if (!player.world.isRemote && equipped && BackpackUpgradeManager.canAutoPickup(backpack)) {
            stateChanged |= collectNearbyItems(player, backpack, item);
        }

        double leakage = Math.max(0D, 1D - item.getRadiationShielding());
        if (leakage > 0D && backpack.hasTagCompound() && item.getFilledSlotCount(backpack) > 0) {
            IBackpackInventory inventory = item.createInventory(backpack, player.world);
            if (inventory.usesVirtualLongCounts()) {
                inventory.forEachStoredStack((content, count) -> applyCargoHazards(player, leakage, content));
            } else {
                for (int slot = 0; slot < inventory.getSlots(); slot++) {
                    ItemStack content = inventory.getStackInSlot(slot);
                    if (!content.isEmpty()) {
                        applyCargoHazards(player, leakage, content);
                    }
                }
            }
        }
        return stateChanged;
    }

    private static void applyCargoHazards(EntityPlayer player, double leakage, ItemStack content) {
        double radiation = HazardSystem.getHazardLevelFromStack(content, HazardRegistry.RADIATION) * leakage;
        if (radiation > 0D) {
            HazardRegistry.RADIATION.onUpdate(player, radiation, content);
        }

        double neutronRadiation = ContaminationUtil.getNeutronRads(content) * leakage;
        if (neutronRadiation > 0D) {
            ContaminationUtil.contaminate(player,
                    ContaminationUtil.HazardType.NEUTRON,
                    ContaminationUtil.ContaminationType.CREATIVE,
                    neutronRadiation * 0.05D);
        }
    }

    /**
     * Radiation seen by a backpack-mounted detector. The live player buffer
     * covers the environment and ordinary exposure, while the direct inventory
     * scan also sees radioactive cargo hidden behind complete shielding.
     */
    public static double getDetectorRadiation(EntityPlayer player) {
        double storedSources = getDetectorRadiation(
                BackpackCapability.getData(player).getEquippedBackpack());
        for (ItemStack stack : player.inventory.mainInventory) {
            storedSources += getDetectorRadiation(stack);
        }
        for (ItemStack stack : player.inventory.armorInventory) {
            storedSources += getDetectorRadiation(stack);
        }
        for (ItemStack stack : player.inventory.offHandInventory) {
            storedSources += getDetectorRadiation(stack);
        }
        return Math.max(Math.max(0D, ContaminationUtil.getActualPlayerRads(player)), storedSources);
    }

    private static double getDetectorRadiation(ItemStack stack) {
        if (stack.isEmpty()) return 0D;
        if (stack.getItem() instanceof ItemBackpack backpack) {
            return ItemBackpack.getStackRadiation(stack) + backpack.getDetectorRadiation(stack);
        }
        return ItemBackpack.getStackRadiation(stack);
    }

    private static boolean prepareBackpackStorage(EntityPlayer player, ItemStack backpack) {
        if (!player.world.isRemote && backpack.getItem() instanceof ItemBackpack item) {
            boolean changed = false;
            for (ItemStack removed : BackpackUpgradeManager.takeUpgradesBeyondPhysicalSlots(backpack)) {
                Library.addToInventoryOrDrop(player, removed);
                changed = true;
            }
            for (ItemStack incompatible : BackpackUpgradeManager.takeIncompatibleUpgrades(backpack)) {
                Library.addToInventoryOrDrop(player, incompatible);
                changed = true;
            }
            int configuredCapacity = item.getStorageSlots(backpack);

            boolean sparse = BackpackConfig.usesSparseStorage(backpack);
            boolean hadSparseStorage = BackpackVirtualStorage.getStorageId(backpack) != null;
            boolean migratePocketStorage = sparse && !hadSparseStorage
                    && item instanceof ItemPocketHoleBackpack pocket
                    && pocket.getStorageId(backpack) != null;

            item.prepareServerStorage(backpack, player.world);
            if (sparse) {
                BlackHoleBackpackSavedData.BackpackStorage storage =
                        BackpackVirtualStorage.getStorage(player.world, backpack);
                if (!hadSparseStorage) {
                    if (migratePocketStorage) {
                        ItemPocketHoleBackpack pocket = (ItemPocketHoleBackpack) item;
                        PocketHoleBackpackSavedData.PocketHoleStorage oldStorage =
                                pocket.getServerStorage(player.world, backpack);
                        for (int slot = 0; slot < PocketHoleBackpackSavedData.PocketHoleStorage.MAX_SLOTS; slot++) {
                            storage.importStoredCount(slot, oldStorage.getSlotPrototype(slot),
                                    oldStorage.getSlotCount(slot));
                        }
                        oldStorage.clearForBackendMigration();
                        changed = true;
                    } else {
                        List<ItemStack> legacyContents = BackpackUpgradeManager.takeOverflowItems(backpack, 0);
                        for (ItemStack legacy : legacyContents) {
                            ItemStack remaining = storage.insertStackAnywhere(legacy, false,
                                    BackpackUpgradeManager.isAutoSortEnabled(backpack), configuredCapacity,
                                    BackpackConfig.allowsOverstack(backpack));
                            if (!remaining.isEmpty()) Library.addToInventoryOrDrop(player, remaining);
                            changed = true;
                        }
                    }
                }

                NBTTagCompound tag = backpack.getTagCompound();
                boolean allowOverstack = BackpackConfig.allowsOverstack(backpack);
                if (!tag.hasKey(BackpackVirtualStorage.OVERSTACK_POLICY_TAG)
                        || tag.getBoolean(BackpackVirtualStorage.OVERSTACK_POLICY_TAG) != allowOverstack) {
                    if (storage.applySlotLimitPolicy(allowOverstack)) {
                        tag.setBoolean(BackpackVirtualStorage.OVERSTACK_POLICY_TAG, allowOverstack);
                        changed = true;
                    }
                }
                BlackHoleBackpackSavedData.BackpackStorage.ShrinkTransaction transaction =
                        storage.prepareShrink(configuredCapacity);
                if (transaction.isPrepared() && !transaction.getDrops().isEmpty() && transaction.commit()) {
                    for (ItemStack overflow : transaction.getDrops()) {
                        Library.addToInventoryOrDrop(player, overflow);
                    }
                    changed = true;
                }
                BackpackVirtualStorage.updateSummary(backpack, storage);
            } else if (item instanceof ItemPocketHoleBackpack pocket) {
                PocketHoleBackpackSavedData.PocketHoleStorage storage =
                        pocket.getServerStorage(player.world, backpack);
                PocketHoleBackpackSavedData.PocketHoleStorage.ShrinkTransaction transaction =
                        storage.prepareShrink(configuredCapacity);
                if (transaction.isPrepared() && !transaction.getDrops().isEmpty() && transaction.commit()) {
                    for (ItemStack overflow : transaction.getDrops()) {
                        Library.addToInventoryOrDrop(player, overflow);
                    }
                    changed = true;
                }
                pocket.setCachedSummary(backpack, storage);
            } else {
                for (ItemStack overflow : BackpackUpgradeManager.takeOverflowItems(backpack, configuredCapacity)) {
                    Library.addToInventoryOrDrop(player, overflow);
                    changed = true;
                }
            }
            if (item instanceof ItemBlackBoxBackpack blackBox) {
                changed |= blackBox.bindOwner(backpack, player);
                changed |= blackBox.unlockIfReturnedToOwner(backpack, player);
                return changed;
            }
            if (item instanceof ItemSmugglerBackpack smuggler) {
                changed |= smuggler.bindOwner(backpack, player);
            }
            return changed;
        }
        return false;
    }

    static boolean collectNearbyItems(EntityPlayer player, ItemStack backpack, ItemBackpack item) {
        java.util.List<EntityItem> nearby = player.world.getEntitiesWithinAABB(
                EntityItem.class, player.getEntityBoundingBox().grow(BackpackUpgradeManager.getPickupRange(backpack)));
        if (nearby.isEmpty()) return false;

        // Build the (potentially expensive) inventory view only once a real
        // pickup candidate exists, not every tick just for wearing the pack.
        IBackpackInventory inventory = null;
        for (EntityItem entity : nearby) {
            // Respect vanilla's delay as well as permanent no-pickup entities.
            // This is what keeps Q-dropped items and a dropped magnet module
            // from being pulled straight back into the same backpack.
            if (entity.isDead || entity.cannotPickup()) continue;
            ItemStack dropped = entity.getItem();
            if (dropped.isEmpty() || !BackpackUpgradeManager.canAutoPickup(backpack)) continue;

            if (inventory == null) inventory = item.createInventory(backpack, player.world);
            ItemStack preview = inventory.insertItemAnywhere(dropped.copy(), true);
            if (preview.getCount() == dropped.getCount()) continue;
            if (!BackpackUpgradeManager.consumeAutoPickupEnergy(backpack)) return false;

            ItemStack remaining = inventory.insertItemAnywhere(dropped.copy(), false);
            if (remaining.getCount() == dropped.getCount()) return true;

            if (remaining.isEmpty()) {
                entity.setDead();
            } else {
                entity.setItem(remaining);
            }
            return true;
        }
        return false;
    }
}
