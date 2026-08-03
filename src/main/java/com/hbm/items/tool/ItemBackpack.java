package com.hbm.items.tool;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.capability.BackpackCapability;
import com.hbm.config.BackpackConfig;
import com.hbm.handler.BackpackHandler;
import com.hbm.hazard.HazardRegistry;
import com.hbm.hazard.HazardSystem;
import com.hbm.hazard.type.HazardTypeRadiation;
import com.hbm.inventory.IBackpackInventory;
import com.hbm.inventory.BackpackVirtualStorage;
import com.hbm.inventory.BlackHoleBackpackInventory;
import com.hbm.inventory.BackpackUpgradeManager;
import com.hbm.items.ItemBakedBase;
import com.hbm.items.ModItems;
import com.hbm.lib.Library;
import com.hbm.lib.ModDamageSource;
import com.hbm.main.MainRegistry;
import com.hbm.tileentity.IPersistentNBT;
import com.hbm.util.ContaminationUtil;
import com.hbm.util.CrateUtil;
import com.hbm.util.I18nUtil;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.UUID;

public class ItemBackpack extends ItemBakedBase implements IBatteryItem {

    public static final String INVENTORY_TAG = "BackpackInventory";
    public static final String INSTANCE_ID_TAG = "BackpackInstanceId";
    public static final String WORKBENCH_PANEL_TAG = "BackpackWorkbenchPanelOpen";
    public static final float TUNGSTEN_THORNS_DAMAGE = 2.0F;
    private static final int MAX_CRATE_NESTING = 64;
    private static final String AUTOMATION_CHARGE_TAG = "charge";

    private final int slots;
    private final double radiationShielding;
    private final boolean lead;
    private final MaterialProtection materialProtection;

    public ItemBackpack(String name, int slots, double radiationShielding, boolean lead) {
        super(name);
        this.slots = slots;
        this.radiationShielding = radiationShielding;
        this.lead = lead;
        this.materialProtection = MaterialProtection.forBackpack(name);
        setMaxStackSize(1);
        setCreativeTab(MainRegistry.consumableTab);
    }

    public int getSlots() {
        return BackpackConfig.getBaseStorageSlots(this, slots);
    }

    public int getStorageSlots(ItemStack backpack) {
        return BackpackConfig.getStorageSlots(backpack, slots);
    }

    @Override
    public void chargeBattery(ItemStack stack, long amount) {
        if (amount <= 0L) return;
        long maximum = getMaxCharge(stack);
        if (maximum <= 0L) return;
        long current = getCharge(stack);
        setCharge(stack, current > maximum - Math.min(amount, maximum) ? maximum : current + amount);
    }

    @Override
    public void setCharge(ItemStack stack, long amount) {
        long maximum = getMaxCharge(stack);
        if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
        stack.getTagCompound().setLong(AUTOMATION_CHARGE_TAG,
                Math.max(0L, Math.min(maximum, amount)));
    }

    @Override
    public void dischargeBattery(ItemStack stack, long amount) {
        if (amount > 0L) setCharge(stack, Math.max(0L, getCharge(stack) - amount));
    }

    @Override
    public long getCharge(ItemStack stack) {
        if (!stack.hasTagCompound()) return 0L;
        return Math.max(0L, Math.min(getMaxCharge(stack),
                stack.getTagCompound().getLong(AUTOMATION_CHARGE_TAG)));
    }

    @Override
    public long getMaxCharge(ItemStack stack) {
        return BackpackConfig.autoMagnetRequiresEnergy(stack)
                ? BackpackConfig.getAutoMagnetEnergyCapacity(stack) : 0L;
    }

    @Override
    public long getChargeRate(ItemStack stack) {
        return getMaxCharge(stack);
    }

    @Override
    public long getDischargeRate(ItemStack stack) {
        return getMaxCharge(stack);
    }

    @Override
    public boolean showDurabilityBar(ItemStack stack) {
        return getMaxCharge(stack) > 0L && getCharge(stack) < getMaxCharge(stack);
    }

    @Override
    public double getDurabilityForDisplay(ItemStack stack) {
        long maximum = getMaxCharge(stack);
        return maximum <= 0L ? 0D : 1D - (double) getCharge(stack) / (double) maximum;
    }

    public static boolean isWorkbenchPanelOpen(ItemStack backpack) {
        return backpack.hasTagCompound() && backpack.getTagCompound().getBoolean(WORKBENCH_PANEL_TAG);
    }

    public static void setWorkbenchPanelOpen(ItemStack backpack, boolean open) {
        if (!backpack.hasTagCompound()) backpack.setTagCompound(new NBTTagCompound());
        backpack.getTagCompound().setBoolean(WORKBENCH_PANEL_TAG, open);
    }

    /**
     * Capacity used when the backpack screen is first opened. Most backpacks
     * expose their complete storage, while compartmented or virtual-storage
     * implementations may present a smaller initial view.
     */
    public int getInitialViewCapacity() {
        return slots;
    }

    public int getInitialViewCapacity(ItemStack backpack) {
        return getStorageSlots(backpack);
    }

    public double getRadiationShielding() {
        return radiationShielding;
    }

    public boolean isLead() {
        return lead;
    }

    public boolean isAsbestos() {
        return materialProtection == MaterialProtection.ASBESTOS;
    }

    /**
     * Covers the item entity only. Stored items are already insulated while
     * inside the backpack, so this keeps the whole dropped backpack from being
     * destroyed by the material hazards it is made to resist.
     */
    public boolean protectsDroppedItemDamage(DamageSource source, float amount) {
        return materialProtection.protects(source, amount);
    }

    /**
     * How long a dropped backpack survives while continuously submerged in lava.
     * Zero keeps vanilla lava behavior, a positive value is measured in ticks,
     * and a negative value means permanent lava immunity.
     */
    public int getDroppedLavaSurvivalTicks() {
        return materialProtection.lavaSurvivalTicks;
    }

    public IBackpackInventory createInventory(ItemStack backpack, @Nullable World world) {
        if (BackpackConfig.usesSparseStorage(backpack)) {
            return BlackHoleBackpackInventory.create(this, backpack, world);
        }
        return new BackpackInventory(backpack);
    }

    /**
     * Called before a backpack is opened or updated on the logical server.
     * World-backed implementations use this to assign their storage identity.
     */
    public void prepareServerStorage(ItemStack backpack, World world) {
        if (world.isRemote || backpack.isEmpty()) return;
        if (!backpack.hasTagCompound()) {
            backpack.setTagCompound(new NBTTagCompound());
        }
        NBTTagCompound tag = backpack.getTagCompound();
        if (!tag.hasUniqueId(INSTANCE_ID_TAG)) {
            tag.setUniqueId(INSTANCE_ID_TAG, UUID.randomUUID());
        }
        if (BackpackConfig.usesSparseStorage(backpack)) {
            BackpackVirtualStorage.getOrCreateStorageId(backpack);
        }
    }

    @Nullable
    public static UUID getInstanceId(ItemStack backpack) {
        return backpack.hasTagCompound() && backpack.getTagCompound().hasUniqueId(INSTANCE_ID_TAG)
                ? backpack.getTagCompound().getUniqueId(INSTANCE_ID_TAG)
                : null;
    }

    /**
     * Per-player backpack hook. Returning true requests one authoritative
     * inventory/equipment synchronization after all backpacks were updated.
     */
    public boolean onBackpackTick(EntityPlayer player, ItemStack backpack, boolean equipped) {
        return false;
    }

    /** Recipes that consume a backpack must never accept one with contents. */
    public boolean isEmptyForUpgrade(ItemStack stack) {
        return getFilledSlotCount(stack) == 0;
    }

    /** Server-aware variant for backpacks whose authoritative contents live in world data. */
    public boolean isEmptyForUpgrade(ItemStack stack, @Nullable World world) {
        if (world != null && !world.isRemote && BackpackConfig.usesSparseStorage(stack)) {
            UUID storageId = BackpackVirtualStorage.getStorageId(stack);
            return storageId == null || BackpackVirtualStorage.getStorage(world, stack).getFilledSlotCount() == 0;
        }
        return isEmptyForUpgrade(stack);
    }

    public int getFilledSlotCount(ItemStack stack) {
        if (BackpackConfig.usesSparseStorage(stack)) {
            return BackpackVirtualStorage.getCachedFilledSlots(stack);
        }
        if (stack.isEmpty() || !stack.hasTagCompound()) return 0;

        NBTTagList items = stack.getTagCompound().getCompoundTag(INVENTORY_TAG).getTagList("Items", Constants.NBT.TAG_COMPOUND);
        int capacity = getStorageSlots(stack);
        BitSet filled = new BitSet(capacity);
        for (int index = 0; index < items.tagCount(); index++) {
            NBTTagCompound item = items.getCompoundTagAt(index);
            int slot = item.getInteger("Slot");
            if (slot >= 0 && slot < capacity && item.hasKey("id", Constants.NBT.TAG_STRING)) {
                filled.set(slot);
            }
        }
        return filled.cardinality();
    }

    protected int getTooltipCapacity(ItemStack stack) {
        return getStorageSlots(stack);
    }

    protected int getTooltipFilledSlotCount(ItemStack stack) {
        return getFilledSlotCount(stack);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (player.isSneaking()) {
            if (!world.isRemote) {
                BackpackHandler.equip(player, stack);
            }
            return new ActionResult<>(EnumActionResult.SUCCESS, ItemStack.EMPTY);
        }

        if (!world.isRemote) {
            BackpackHandler.openHeldBackpack(player, hand);
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        int tooltipCapacity = getTooltipCapacity(stack);
        int filled = getTooltipFilledSlotCount(stack);
        if (BackpackConfig.hasInfiniteSlots(stack)) {
            tooltip.add(TextFormatting.GRAY + I18n.format("container.hbm_backpack.capacity", "\u221E"));
            tooltip.add(I18n.format("container.hbm_backpack.filled",
                    TextFormatting.GREEN + String.valueOf(filled), TextFormatting.DARK_GREEN + "\u221E")
                    + TextFormatting.RESET);
        } else {
            tooltip.add(TextFormatting.GRAY + I18n.format("container.hbm_backpack.capacity", tooltipCapacity));
            float percent = tooltipCapacity <= 0 ? 0F : Library.roundFloat(filled * 100F / tooltipCapacity, 1);
            TextFormatting filledColor = percent >= 75F ? TextFormatting.RED : percent < 25F ? TextFormatting.GREEN : TextFormatting.YELLOW;
            TextFormatting capacityColor = percent >= 75F ? TextFormatting.DARK_RED : percent < 25F ? TextFormatting.DARK_GREEN : TextFormatting.GOLD;
            tooltip.add(I18n.format("container.hbm_backpack.filled", filledColor + String.valueOf(filled), capacityColor + String.valueOf(tooltipCapacity))
                    + " " + filledColor + "(" + percent + "%)" + TextFormatting.RESET);
        }
        tooltip.add(TextFormatting.GREEN + I18nUtil.resolveKey("desc.backpack.shielding", Math.round(radiationShielding * 100D)));
        tooltip.add(TextFormatting.YELLOW + I18nUtil.resolveKey("desc.backpack.leakage_radiation", getLeakedRadiationText(stack)));
        if (materialProtection.tooltipKey != null) {
            tooltip.add(TextFormatting.AQUA + I18nUtil.resolveKey(materialProtection.tooltipKey));
        }
        tooltip.add(TextFormatting.DARK_GRAY + I18nUtil.resolveKey("desc.backpack.equip_hint"));
        int upgradeSlots = BackpackUpgradeManager.getUpgradeSlotCount(stack);
        if (upgradeSlots > 0) {
            tooltip.add(TextFormatting.AQUA + I18nUtil.resolveKey("desc.backpack.upgrade_slots", upgradeSlots));
        }
        addBuiltInMagnetEnergyInformation(stack, tooltip);
        if (lead) {
            tooltip.add(TextFormatting.GOLD + I18nUtil.resolveKey("desc.backpack.lead_warning"));
        }
        if (isAsbestos()) {
            tooltip.add(TextFormatting.RED + I18nUtil.resolveKey("desc.backpack.asbestos_warning"));
        }
        if (stack.getItem() == ModItems.backpack_tungsten) {
            tooltip.add(TextFormatting.DARK_GRAY + I18nUtil.resolveKey("desc.backpack.tungsten.thorns",
                    (int) (TUNGSTEN_THORNS_DAMAGE / 2.0F)));
        }
    }

    protected void addBuiltInMagnetEnergyInformation(ItemStack stack, List<String> tooltip) {
        if (!BackpackConfig.autoMagnetRequiresEnergy(stack)) return;
        tooltip.add(TextFormatting.AQUA + I18nUtil.resolveKey("desc.backpack.dineutronium.energy",
                Library.getShortNumber(getCharge(stack)), Library.getShortNumber(getMaxCharge(stack))));
        tooltip.add(TextFormatting.DARK_GRAY + I18nUtil.resolveKey(
                "desc.backpack.dineutronium.autopickup_cost",
                Library.getShortNumber(BackpackConfig.getAutoMagnetEnergyPerOperation(stack))));
    }

    public static void handleTungstenThornsAttack(LivingAttackEvent event) {
        DamageSource source = event.getSource();
        if (event.isCanceled() || !(event.getEntityLiving() instanceof EntityPlayer player)
                || player.world.isRemote
                || source instanceof EntityDamageSource entitySource && entitySource.getIsThornsDamage()) {
            return;
        }

        Entity attacker = source.getTrueSource();
        if (!(attacker instanceof EntityLivingBase)
                || source.getImmediateSource() != attacker
                || attacker == player) {
            return;
        }

        ItemStack stack = BackpackCapability.getData(player).getEquippedBackpack();
        if (stack.getItem() == ModItems.backpack_tungsten
                && attacker.attackEntityFrom(DamageSource.causeThornsDamage(player), TUNGSTEN_THORNS_DAMAGE)) {
            attacker.playSound(SoundEvents.ENCHANT_THORNS_HIT, 0.5F, 1.0F);
        }
    }

    protected double getContainedRadiation(ItemStack backpack) {
        if (BackpackConfig.usesSparseStorage(backpack)) {
            return BackpackVirtualStorage.getCachedRadiation(backpack);
        }
        if (backpack.isEmpty() || !backpack.hasTagCompound()) return 0D;

        NBTTagList items = backpack.getTagCompound().getCompoundTag(INVENTORY_TAG).getTagList("Items", Constants.NBT.TAG_COMPOUND);
        double radiation = 0D;
        for (int index = 0; index < items.tagCount(); index++) {
            ItemStack content = new ItemStack(items.getCompoundTagAt(index));
            radiation += getStackRadiation(content);
        }
        return radiation;
    }

    /** Total unshielded radiation available to a detector outside the storage logic. */
    public double getDetectorRadiation(ItemStack backpack) {
        return getContainedRadiation(backpack);
    }

    protected String getContainedRadiationText(ItemStack backpack) {
        double radiation = getContainedRadiation(backpack);
        return Library.roundFloat(HazardTypeRadiation.getNewValue(radiation), 3)
                + HazardTypeRadiation.getSuffix(radiation) + " " + I18nUtil.resolveKey("desc.rads");
    }

    protected String getLeakedRadiationText(ItemStack backpack) {
        double radiation = getContainedRadiation(backpack) * Math.max(0D, 1D - radiationShielding);
        return Library.roundFloat(HazardTypeRadiation.getNewValue(radiation), 3)
                + HazardTypeRadiation.getSuffix(radiation) + " " + I18nUtil.resolveKey("desc.rads");
    }

    public static double getStackRadiation(ItemStack stack) {
        if (stack.isEmpty()) return 0D;
        return HazardSystem.getHazardLevelFromStack(stack, HazardRegistry.RADIATION) * stack.getCount()
                + ContaminationUtil.getNeutronRads(stack);
    }

    /**
     * A backpack may not contain another backpack, directly or hidden in an
     * HBM storage crate. Empty crates and crates with ordinary contents remain valid.
     */
    public static boolean isForbiddenBackpackContent(ItemStack stack) {
        return containsForbiddenBackpackContent(stack, 0);
    }

    private static boolean containsForbiddenBackpackContent(ItemStack stack, int depth) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() instanceof ItemBackpack) return true;
        if (!CrateUtil.isCrateItem(stack) || !stack.hasTagCompound()) return false;
        if (depth >= MAX_CRATE_NESTING) return true;

        NBTTagCompound root = stack.getTagCompound();
        NBTTagCompound contents = root.hasKey(IPersistentNBT.NBT_PERSISTENT_KEY, Constants.NBT.TAG_COMPOUND)
                ? root.getCompoundTag(IPersistentNBT.NBT_PERSISTENT_KEY)
                : root;
        for (String key : contents.getKeySet()) {
            if (!key.startsWith("slot")) continue;
            NBTBase serializedStack = contents.getTag(key);
            if (!(serializedStack instanceof NBTTagCompound)) continue;

            if (containsForbiddenBackpackContent(new ItemStack((NBTTagCompound) serializedStack), depth + 1)) {
                return true;
            }
        }
        return false;
    }

    public static class BackpackInventory extends ItemStackHandler implements IBackpackInventory {
        private final ItemStack backpack;
        private int autoSortBatchDepth;
        private boolean autoSortPending;

        public BackpackInventory(ItemStack backpack) {
            super(getSlots(backpack));
            this.backpack = backpack;
            if (!backpack.hasTagCompound()) {
                backpack.setTagCompound(new NBTTagCompound());
            }
            NBTTagCompound inventoryTag = backpack.getTagCompound().getCompoundTag(INVENTORY_TAG).copy();
            inventoryTag.setInteger("Size", getSlots(backpack));
            deserializeNBT(inventoryTag);
            if (BackpackUpgradeManager.isAutoSortEnabled(backpack)) sortAutoSortRanges();
        }

        private static int getSlots(ItemStack backpack) {
            return backpack.getItem() instanceof ItemBackpack item ? item.getStorageSlots(backpack) : 0;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return !stack.isEmpty() && !isForbiddenBackpackContent(stack);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!isItemValid(slot, stack)) return stack;
            ItemStack remaining = super.insertItem(slot, stack, simulate);
            if (!simulate && remaining.getCount() != stack.getCount()) requestAutoSort();
            return remaining;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            ItemStack extracted = super.extractItem(slot, amount, simulate);
            if (!simulate && !extracted.isEmpty()) requestAutoSort();
            return extracted;
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            if (stack.isEmpty() || isItemValid(slot, stack)) {
                ItemStack before = getStackInSlot(slot).copy();
                super.setStackInSlot(slot, stack);
                if (!ItemStack.areItemStacksEqual(before, getStackInSlot(slot))) requestAutoSort();
            }
        }

        @Override
        protected void onContentsChanged(int slot) {
            backpack.getTagCompound().setTag(INVENTORY_TAG, serializeNBT());
        }

        @Override
        public int getCapacity() {
            return getSlots();
        }

        @Override
        public int getFilledSlotCount() {
            int filled = 0;
            for (int slot = 0; slot < getSlots(); slot++) {
                if (!getStackInSlot(slot).isEmpty()) {
                    filled++;
                }
            }
            return filled;
        }

        @Override
        public ItemStack insertItemAnywhere(ItemStack stack, boolean simulate) {
            if (isForbiddenBackpackContent(stack)) return stack;

            if (!simulate) beginAutoSortBatch();
            try {
                ItemStack remaining = stack;
                for (int pass = 0; pass < 2 && !remaining.isEmpty(); pass++) {
                    for (int slot = 0; slot < getSlots() && !remaining.isEmpty(); slot++) {
                        boolean empty = getStackInSlot(slot).isEmpty();
                        if ((pass == 0 && empty) || (pass == 1 && !empty)) continue;
                        remaining = insertItem(slot, remaining, simulate);
                    }
                }
                return remaining;
            } finally {
                if (!simulate) endAutoSortBatch();
            }
        }

        @Override
        public boolean supportsAutoPickup() {
            return BackpackUpgradeManager.supportsAutoPickup(backpack);
        }

        @Override
        public boolean isAutoPickupEnabled() {
            return BackpackUpgradeManager.isAutoPickupEnabled(backpack);
        }

        @Override
        public void setAutoPickupEnabled(boolean enabled) {
            BackpackUpgradeManager.setAutoPickupEnabled(backpack, enabled);
        }

        @Override
        public boolean supportsAutoSorting() {
            return BackpackUpgradeManager.supportsAutoSorting(backpack);
        }

        @Override
        public boolean isAutoSortEnabled() {
            return BackpackUpgradeManager.isAutoSortEnabled(backpack);
        }

        @Override
        public void setAutoSortEnabled(boolean enabled) {
            boolean wasEnabled = isAutoSortEnabled();
            BackpackUpgradeManager.setAutoSortEnabled(backpack, enabled);
            if (enabled && !wasEnabled) sortAutoSortRanges();
        }

        @Override
        public boolean supportsManualSorting() {
            return getSlots() > 1;
        }

        @Override
        public void sortContents() {
            sortContents(0, getSlots());
        }

        @Override
        public void sortContents(int fromInclusive, int toExclusive) {
            int start = Math.max(0, fromInclusive);
            int end = Math.min(getSlots(), Math.max(start, toExclusive));
            if (end - start < 2) return;

            List<ItemStack> compacted = new ArrayList<>();
            for (int slot = start; slot < end; slot++) {
                ItemStack stack = getStackInSlot(slot);
                if (!stack.isEmpty()) compacted.add(stack.copy());
            }

            compacted.sort(ItemBackpack::compareForBackpackSort);

            List<ItemStack> sorted = new ArrayList<>();
            for (ItemStack stack : compacted) {
                for (int targetIndex = 0; targetIndex < sorted.size(); targetIndex++) {
                    ItemStack target = sorted.get(targetIndex);
                    if (!ItemHandlerHelper.canItemStacksStack(target, stack)) continue;
                    int limit = Math.min(target.getMaxStackSize(), getSlotLimit(start + targetIndex));
                    int moved = Math.min(stack.getCount(), limit - target.getCount());
                    if (moved > 0) {
                        target.grow(moved);
                        stack.shrink(moved);
                    }
                    if (stack.isEmpty()) break;
                }
                if (!stack.isEmpty()) sorted.add(stack);
            }

            boolean changed = false;
            for (int slot = start; slot < end; slot++) {
                ItemStack desired = slot - start < sorted.size() ? sorted.get(slot - start) : ItemStack.EMPTY;
                if (!ItemStack.areItemStacksEqual(stacks.get(slot), desired)) {
                    changed = true;
                    break;
                }
            }
            if (!changed) return;

            // Replace the complete range first and serialize once. Calling the
            // public setter per slot would publish transient duplicated layouts.
            for (int slot = start; slot < end; slot++) {
                ItemStack desired = slot - start < sorted.size() ? sorted.get(slot - start) : ItemStack.EMPTY;
                stacks.set(slot, desired.isEmpty() ? ItemStack.EMPTY : desired.copy());
            }
            backpack.getTagCompound().setTag(INVENTORY_TAG, serializeNBT());
        }

        private void beginAutoSortBatch() {
            autoSortBatchDepth++;
        }

        private void endAutoSortBatch() {
            if (autoSortBatchDepth <= 0) return;
            autoSortBatchDepth--;
            if (autoSortBatchDepth == 0 && autoSortPending) {
                autoSortPending = false;
                sortAutoSortRanges();
            }
        }

        private void requestAutoSort() {
            if (!isAutoSortEnabled()) return;
            if (autoSortBatchDepth > 0) {
                autoSortPending = true;
                return;
            }
            sortAutoSortRanges();
        }

        private void sortAutoSortRanges() {
            if (backpack.getItem() instanceof ItemSmugglerBackpack) {
                int hiddenStart = Math.min(getSlots(),
                        ((ItemSmugglerBackpack) backpack.getItem()).getVisibleSlotCount(backpack));
                sortContents(0, hiddenStart);
                sortContents(hiddenStart, getSlots());
                return;
            }
            sortContents();
        }
    }

    /**
     * Deterministic order shared by physical and virtual backpack storage.
     * Registry names remain stable across client/server numeric-ID remaps.
     */
    public static int compareForBackpackSort(ItemStack left, ItemStack right) {
        int registry = String.valueOf(left.getItem().getRegistryName())
                .compareTo(String.valueOf(right.getItem().getRegistryName()));
        if (registry != 0) return registry;

        int metadata = Integer.compare(left.getMetadata(), right.getMetadata());
        if (metadata != 0) return metadata;

        ItemStack normalizedLeft = left.copy();
        ItemStack normalizedRight = right.copy();
        normalizedLeft.setCount(1);
        normalizedRight.setCount(1);
        return normalizedLeft.serializeNBT().toString()
                .compareTo(normalizedRight.serializeNBT().toString());
    }

    private enum MaterialProtection {
        NONE(false, false, 0F, 0, null),
        STEEL(true, false, 12F, 0, "desc.backpack.dropped.fire_small_explosion"),
        TITANIUM(true, false, 20F, 45 * 20, "desc.backpack.dropped.fire_small_explosion"),
        LEAD(true, false, 20F, 0, "desc.backpack.dropped.fire_small_explosion"),
        REINFORCED_STEEL(true, false, 40F, 60 * 20, "desc.backpack.dropped.fire_explosion_resistant"),
        ASBESTOS(true, true, 0F, 120 * 20, "desc.backpack.dropped.fire_acid"),
        DURALUMIN(false, false, 10F, 0, "desc.backpack.dropped.explosion_resistant"),
        // Beryllium stays solid to 1287 C, so it outlasts duralumin in a blast.
        BERYLLIUM(true, false, 50F, 30 * 20, "desc.backpack.dropped.fire_explosion_resistant"),
        HIGH_STRENGTH_STEEL(true, false, 60F, 180 * 20, "desc.backpack.dropped.fire_explosion_resistant"),
        TUNGSTEN(true, false, 120F, -1, "desc.backpack.dropped.fire_heavy_explosion"),
        // Uses the actual post-distance entity damage, so moderate blasts survive while stronger ones do not.
        DESH(true, false, 200F, -1, "desc.backpack.dropped.fire_heavy_explosion"),
        // Conventional and heavy blasts survive; only four-digit, super-heavy damage destroys it.
        SCHRABIDIUM(true, true, 1_000F, -1, "desc.backpack.dropped.fire_acid_heavy_explosion"),
        DINEUTRONIUM(true, true, Float.MAX_VALUE, -1, "desc.backpack.dropped.indestructible");

        private final boolean fire;
        private final boolean acid;
        private final float maxExplosionDamage;
        private final int lavaSurvivalTicks;
        private final String tooltipKey;

        MaterialProtection(boolean fire, boolean acid, float maxExplosionDamage, int lavaSurvivalTicks, String tooltipKey) {
            this.fire = fire;
            this.acid = acid;
            this.maxExplosionDamage = maxExplosionDamage;
            this.lavaSurvivalTicks = lavaSurvivalTicks;
            this.tooltipKey = tooltipKey;
        }

        private boolean protects(DamageSource source, float amount) {
            if (this == DINEUTRONIUM) return true;
            if (source == null) return false;
            if (fire && source.isFireDamage() && !"lava".equals(source.getDamageType())) return true;
            if (acid && (source == ModDamageSource.acid || ModDamageSource.s_acid.equals(source.getDamageType()))) return true;
            return maxExplosionDamage > 0F && source.isExplosion() && amount <= maxExplosionDamage;
        }

        private static MaterialProtection forBackpack(String name) {
            switch (name) {
                case "backpack_steel":
                    return STEEL;
                case "backpack_titanium":
                    return TITANIUM;
                case "backpack_lead":
                    return LEAD;
                case "backpack_reinforced_steel":
                    return REINFORCED_STEEL;
                case "backpack_asbestos":
                    return ASBESTOS;
                case "backpack_duralumin":
                    return DURALUMIN;
                case "backpack_beryllium":
                    return BERYLLIUM;
                case "backpack_high_strength_steel":
                    return TUNGSTEN;
                case "backpack_tungsten":
                    return HIGH_STRENGTH_STEEL;
                case "backpack_desh":
                    return DESH;
                case "backpack_schrabidium":
                    return SCHRABIDIUM;
                case "backpack_dineutronium":
                    return DINEUTRONIUM;
                default:
                    return NONE;
            }
        }
    }
}
