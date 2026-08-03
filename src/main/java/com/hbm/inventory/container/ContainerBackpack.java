package com.hbm.inventory.container;

import com.hbm.capability.BackpackCapability;
import com.hbm.config.BackpackConfig;
import com.hbm.handler.BackpackHandler;
import com.hbm.inventory.BackpackEquipmentSlot;
import com.hbm.inventory.BackpackUpgradeManager;
import com.hbm.inventory.BackpackVirtualStorage;
import com.hbm.inventory.BackpackUpgradeManager.UpgradeInventory;
import com.hbm.inventory.EquippedBackpackItemHandler;
import com.hbm.inventory.IBackpackInventory;
import com.hbm.items.tool.BackpackUpgradeType;
import com.hbm.items.tool.ItemBackpack;
import com.hbm.items.tool.ItemBlackBoxBackpack;
import com.hbm.items.tool.ItemPocketHoleBackpack;
import com.hbm.items.tool.ItemRealityErrorBackpack;
import com.hbm.items.tool.ItemSmugglerBackpack;
import com.hbm.packet.PacketDispatcher;
import com.hbm.packet.toclient.BlackHoleBackpackPagePacket;
import com.hbm.saveddata.BlackHoleBackpackSavedData;
import com.hbm.saveddata.PocketHoleBackpackSavedData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.InventoryCraftResult;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.inventory.SlotCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.SlotItemHandler;

import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;

public class ContainerBackpack extends Container {
    public static final int MAX_COLUMNS = 13;
    public static final int MAX_VISIBLE_ROWS = 4;
    public static final int VISIBLE_SLOTS = 65;
    public static final int SCROLL_UP = 0;
    public static final int SCROLL_DOWN = 1;
    public static final int TOGGLE_AUTO_PICKUP = 2;
    public static final int TOGGLE_AUTO_SORT = 3;
    public static final int TOGGLE_SMUGGLER_COMPARTMENT = 4;
    public static final int SORT_CONTENTS = 5;
    public static final int TOGGLE_WORKBENCH = 6;
    public static final int TOGGLE_UPGRADE_DRAWER = 7;
    public static final int UPGRADE_DRAWER_WIDTH = 26;
    public static final int UPGRADE_DRAWER_SLOT_X = -21;
    public static final int UPGRADE_DRAWER_SLOT_Y = 8;
    public static final int WORKBENCH_PANEL_WIDTH = 76;
    public static final int WORKBENCH_PANEL_RIGHT = -UPGRADE_DRAWER_WIDTH - 4;
    public static final int WORKBENCH_PANEL_LEFT = WORKBENCH_PANEL_RIGHT - WORKBENCH_PANEL_WIDTH;
    public static final int WORKBENCH_PANEL_TOP = 3;
    public static final int WORKBENCH_GRID_TOP = 23;
    public static final int WORKBENCH_RESULT_TOP = 84;
    public static final int WORKBENCH_SLOT_SIZE = 18;
    public static final int WORKBENCH_PANEL_BOTTOM = WORKBENCH_RESULT_TOP + WORKBENCH_SLOT_SIZE
            + (WORKBENCH_GRID_TOP - WORKBENCH_PANEL_TOP);
    public static final int WORKBENCH_ARROW_WIDTH = 18;
    public static final int WORKBENCH_ARROW_HEIGHT = 14;

    private static final int CONTROL_TOP = 4;
    private static final int CONTROL_HEIGHT = 20;
    private static final int CONTROL_GAP = 2;
    private static final int CONTROL_SECTION_GAP = 3;
    private static final int MINIMUM_SEARCH_Y = 45;

    private static final int PROPERTY_CAPACITY_LOW = 16;
    private static final int PROPERTY_CAPACITY_HIGH = 17;
    private static final int PROPERTY_FILLED_LOW = 18;
    private static final int PROPERTY_FILLED_HIGH = 19;
    private static final int PROPERTY_AUTO_PICKUP = 20;
    private static final int PROPERTY_AUTO_SORT = 21;
    private static final int PROPERTY_SMUGGLER_COMPARTMENT = 22;
    private static final int PROPERTY_SCROLL_ROW = 23;
    private static final int PROPERTY_VIRTUAL_CLICK_ACK = 24;
    private static final int PROPERTY_WORKBENCH_VIEW = 25;
    private static final int PROPERTY_UPGRADE_DRAWER = 26;
    private static final int PROPERTY_AVAILABLE_ACTIONS = 27;
    private static final int PROPERTY_TRUE_COUNT_BASE = 32;
    private static final int TRUE_COUNT_WORDS = 4;
    private static final int TRUE_COUNT_SLOTS = ItemPocketHoleBackpack.VISIBLE_SLOTS;
    private static final int BLACK_HOLE_FULL_PAGE_DELTA_THRESHOLD = 16;

    private static final int HIDDEN_SLOT_POSITION = -10000;

    private final EntityPlayer player;
    private final EnumHand heldHand;
    private final boolean equippedView;
    private final boolean virtualLongCounts;
    private final boolean serverAuthoritativeBlackHolePages;
    private final HeldBackpackItemHandler heldInventory;
    private final int announcedCapacity;
    private final int announcedFeatures;
    private final PagedBackpackItemHandler contents;
    private final UpgradeInventory upgrades;
    private final InventoryCrafting craftMatrix = new InventoryCrafting(this, 3, 3);
    private final InventoryCraftResult craftResult = new InventoryCraftResult();
    private int viewCapacityHighWater;
    private final int contentStart;
    private final int contentX;
    private int contentY;
    private final int columns;
    private int visibleRows;
    private int backpackPaneHeight;
    private final int playerPanelX;
    private int playerPanelY;
    private int playerY;
    private final int guiWidth;
    private int guiHeight;
    private final int playerStart;
    private int upgradeStart;
    private int craftStart;
    private final int upgradeSlotCount;
    private int syncedCapacity;
    private boolean receivedCapacitySync;
    private int syncedFilled = -1;
    private boolean syncedAutoPickup;
    private boolean syncedAutoSort;
    private int lastSyncedCapacity = -1;
    private int lastSyncedFilled = -1;
    private boolean lastSyncedAutoPickup;
    private boolean lastSyncedAutoSort;
    private boolean hiddenSmugglerCompartment;
    private boolean lastSyncedHiddenSmugglerCompartment;
    private int lastSyncedScrollRow = -1;
    private boolean sentAutomationState;
    private final long[] lastSyncedTrueCounts = new long[TRUE_COUNT_SLOTS];
    private boolean lastSyncedVirtualCounts;
    private final ItemStack[] lastSyncedBlackHolePageStacks = new ItemStack[VISIBLE_SLOTS];
    private final long[] lastSyncedBlackHolePageCounts = new long[VISIBLE_SLOTS];
    private boolean blackHolePageInitialized;
    private int lastSyncedBlackHolePageRow = -1;
    private int lastSyncedBlackHolePageCapacity = -1;
    private int lastSyncedBlackHolePageFilled = -1;
    private UUID lastSyncedBlackHoleStorageId;
    private int blackHolePageSequence;
    private int lastAppliedBlackHolePageSequence = -1;
    private boolean blackHolePageNeedsFullSync;
    private boolean blackHolePagePending;
    private UUID blackHolePendingStorageId;
    private boolean blackHoleScrollAcknowledgement;
    private int serverVirtualClickSequence;
    private int lastSyncedVirtualClickSequence = -1;
    private int virtualClickAckDelay;
    private int clientVirtualClickSequence = -1;
    private boolean virtualClickPending;
    private boolean workbenchView;
    private boolean lastSyncedWorkbenchView;
    private boolean upgradeDrawerOpen;
    private boolean lastSyncedUpgradeDrawerOpen;
    private int syncedAvailableActions;
    private int lastSyncedAvailableActions = -1;
    private boolean receivedAvailableActions;

    public ContainerBackpack(EntityPlayer player) {
        this(player, 0);
    }

    public ContainerBackpack(EntityPlayer player, int announcedCapacity) {
        this(player, announcedCapacity, 0);
    }

    public ContainerBackpack(EntityPlayer player, int announcedCapacity, int announcedFeatures) {
        this.player = player;
        this.heldHand = null;
        this.equippedView = true;
        this.heldInventory = null;
        ItemStack backpack = BackpackCapability.getData(player).getEquippedBackpack();
        this.virtualLongCounts = BackpackConfig.usesSparseStorage(backpack)
                || backpack.getItem() instanceof ItemPocketHoleBackpack
                || backpack.getItem() instanceof ItemRealityErrorBackpack;
        this.serverAuthoritativeBlackHolePages = BackpackConfig.usesSparseStorage(backpack);
        EquippedBackpackItemHandler inventory = new EquippedBackpackItemHandler(player);
        this.announcedCapacity = Math.max(0, announcedCapacity);
        this.announcedFeatures = announcedFeatures;
        this.syncedAvailableActions = (announcedFeatures & BackpackHandler.GUI_FEATURE_AUTO_PICKUP) != 0
                ? BackpackUpgradeManager.ACTION_AUTO_PICKUP : 0;
        this.syncedCapacity = this.announcedCapacity;
        this.contents = new PagedBackpackItemHandler(inventory, inventory::getCapacity);
        this.upgrades = BackpackUpgradeManager.createInventory(backpack);
        this.upgradeSlotCount = upgrades.getSlots();
        this.workbenchView = BackpackUpgradeManager.hasUpgrade(backpack, BackpackUpgradeType.WORKBENCH)
                && ItemBackpack.isWorkbenchPanelOpen(backpack);
        this.upgradeDrawerOpen = workbenchView;
        this.viewCapacityHighWater = Math.max(this.announcedCapacity, contents.getBackendCapacity());
        this.syncedAutoPickup = contents.isAutoPickupEnabled();
        this.syncedAutoSort = contents.isAutoSortEnabled();
        this.contentStart = 1;

        addSlotToContainer(new BackpackEquipmentSlot(player, 8, 18));
        boolean ownerManaged = isOwnerManagedBackpack(backpack);
        Layout layout = new Layout(getBaseStorageCapacity(backpack), getCapacity(), ownerManaged);
        this.columns = layout.columns;
        this.visibleRows = layout.visibleRows;
        this.guiWidth = layout.guiWidth;
        this.contentX = layout.contentX;
        this.contentY = calculateContentY();
        this.backpackPaneHeight = contentY + visibleRows * 18 + 8;
        this.playerPanelX = layout.playerPanelX;
        this.playerPanelY = backpackPaneHeight + (ownerManaged ? 2 : 4);
        this.playerY = playerPanelY + 16;
        this.guiHeight = playerPanelY + 94;
        this.playerStart = addContentAndPlayerSlots(player.inventory);
        layoutModeSlots();
    }

    public ContainerBackpack(EntityPlayer player, EnumHand hand) {
        this(player, hand, 0);
    }

    public ContainerBackpack(EntityPlayer player, EnumHand hand, int announcedCapacity) {
        this(player, hand, announcedCapacity, 0);
    }

    public ContainerBackpack(EntityPlayer player, EnumHand hand, int announcedCapacity, int announcedFeatures) {
        this.player = player;
        this.heldHand = hand;
        this.equippedView = false;
        this.heldInventory = new HeldBackpackItemHandler(player, hand);
        ItemStack backpack = player.getHeldItem(hand);
        this.virtualLongCounts = BackpackConfig.usesSparseStorage(backpack)
                || backpack.getItem() instanceof ItemPocketHoleBackpack
                || backpack.getItem() instanceof ItemRealityErrorBackpack;
        this.serverAuthoritativeBlackHolePages = BackpackConfig.usesSparseStorage(backpack);
        HeldBackpackItemHandler inventory = this.heldInventory;
        this.announcedCapacity = Math.max(0, announcedCapacity);
        this.announcedFeatures = announcedFeatures;
        this.syncedAvailableActions = (announcedFeatures & BackpackHandler.GUI_FEATURE_AUTO_PICKUP) != 0
                ? BackpackUpgradeManager.ACTION_AUTO_PICKUP : 0;
        this.syncedCapacity = this.announcedCapacity;
        this.contents = new PagedBackpackItemHandler(inventory, inventory::getCapacity);
        this.upgrades = BackpackUpgradeManager.createInventory(backpack);
        this.upgradeSlotCount = upgrades.getSlots();
        this.workbenchView = BackpackUpgradeManager.hasUpgrade(backpack, BackpackUpgradeType.WORKBENCH)
                && ItemBackpack.isWorkbenchPanelOpen(backpack);
        this.upgradeDrawerOpen = workbenchView;
        this.viewCapacityHighWater = Math.max(this.announcedCapacity, contents.getBackendCapacity());
        this.syncedAutoPickup = contents.isAutoPickupEnabled();
        this.syncedAutoSort = contents.isAutoSortEnabled();
        this.contentStart = 0;

        boolean ownerManaged = isOwnerManagedBackpack(backpack);
        Layout layout = new Layout(getBaseStorageCapacity(backpack), getCapacity(), ownerManaged);
        this.columns = layout.columns;
        this.visibleRows = layout.visibleRows;
        this.guiWidth = layout.guiWidth;
        this.contentX = layout.contentX;
        this.contentY = calculateContentY();
        this.backpackPaneHeight = contentY + visibleRows * 18 + 8;
        this.playerPanelX = layout.playerPanelX;
        this.playerPanelY = backpackPaneHeight + (ownerManaged ? 2 : 4);
        this.playerY = playerPanelY + 16;
        this.guiHeight = playerPanelY + 94;
        this.playerStart = addContentAndPlayerSlots(player.inventory);
        layoutModeSlots();
    }

    private int addContentAndPlayerSlots(InventoryPlayer inventory) {
        for (int slot = 0; slot < VISIBLE_SLOTS; slot++) {
            addSlotToContainer(new BackpackContentSlot(contents, slot, HIDDEN_SLOT_POSITION, HIDDEN_SLOT_POSITION,
                    this::usesVirtualLongCounts, this::isProcessingQuickCraft, () -> player.world.isRemote));
        }

        upgradeStart = inventorySlots.size();
        for (int slot = 0; slot < upgradeSlotCount; slot++) {
            addSlotToContainer(new SlotItemHandler(upgrades, slot, HIDDEN_SLOT_POSITION, HIDDEN_SLOT_POSITION));
        }

        craftStart = inventorySlots.size();
        addSlotToContainer(new SlotCrafting(player, craftMatrix, craftResult, 0,
                HIDDEN_SLOT_POSITION, HIDDEN_SLOT_POSITION));
        for (int slot = 0; slot < 9; slot++) {
            addSlotToContainer(new Slot(craftMatrix, slot, HIDDEN_SLOT_POSITION, HIDDEN_SLOT_POSITION));
        }

        int start = inventorySlots.size();
        int playerX = playerPanelX + 8;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlotToContainer(new Slot(inventory, column + row * 9 + 9, playerX + column * 18, playerY + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlotToContainer(new Slot(inventory, column, playerX + column * 18, playerY + 58));
        }
        return start;
    }

    private void layoutContentSlots() {
        int visibleSlots = getVisibleSlotCount();
        for (int displaySlot = 0; displaySlot < VISIBLE_SLOTS; displaySlot++) {
            Slot slot = inventorySlots.get(contentStart + displaySlot);
            if (displaySlot < visibleSlots) {
                int row = displaySlot / columns;
                int column = displaySlot % columns;
                slot.xPos = contentX + column * 18;
                slot.yPos = contentY + row * 18;
            } else {
                slot.xPos = HIDDEN_SLOT_POSITION;
                slot.yPos = HIDDEN_SLOT_POSITION;
            }
        }
    }

    private void layoutUpgradeSlots() {
        boolean visible = isUpgradeDrawerOpen();
        for (int slot = 0; slot < upgradeSlotCount; slot++) {
            Slot upgrade = inventorySlots.get(upgradeStart + slot);
            upgrade.xPos = visible ? UPGRADE_DRAWER_SLOT_X : HIDDEN_SLOT_POSITION;
            upgrade.yPos = visible ? UPGRADE_DRAWER_SLOT_Y + slot * 18 : HIDDEN_SLOT_POSITION;
        }
    }

    private void layoutCraftingSlots() {
        Slot result = inventorySlots.get(craftStart);
        result.xPos = workbenchView ? WORKBENCH_PANEL_LEFT + 29 : HIDDEN_SLOT_POSITION;
        result.yPos = workbenchView ? WORKBENCH_RESULT_TOP : HIDDEN_SLOT_POSITION;
        for (int slot = 0; slot < 9; slot++) {
            Slot input = inventorySlots.get(craftStart + 1 + slot);
            input.xPos = workbenchView ? WORKBENCH_PANEL_LEFT + 11 + slot % 3 * 18 : HIDDEN_SLOT_POSITION;
            input.yPos = workbenchView ? WORKBENCH_GRID_TOP + slot / 3 * 18 : HIDDEN_SLOT_POSITION;
        }
    }

    private void layoutModeSlots() {
        layoutContentSlots();
        layoutUpgradeSlots();
        layoutCraftingSlots();
    }

    private void layoutPlayerSlots() {
        int playerX = playerPanelX + 8;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                Slot slot = inventorySlots.get(playerStart + column + row * 9);
                slot.xPos = playerX + column * 18;
                slot.yPos = playerY + row * 18;
            }
        }
        for (int column = 0; column < 9; column++) {
            Slot slot = inventorySlots.get(playerStart + 27 + column);
            slot.xPos = playerX + column * 18;
            slot.yPos = playerY + 58;
        }
    }

    /** Rebuilds every position derived from the current server capacity. */
    public boolean refreshLayout() {
        int totalRows = Math.max(1, (getCapacity() + columns - 1) / columns);
        int nextVisibleRows = Math.min(MAX_VISIBLE_ROWS, totalRows);
        int nextContentY = calculateContentY();
        boolean dimensionsChanged = nextVisibleRows != visibleRows || nextContentY != contentY;
        visibleRows = nextVisibleRows;
        contentY = nextContentY;
        backpackPaneHeight = contentY + visibleRows * 18 + 8;
        playerPanelY = backpackPaneHeight + (isOwnerManagedBackpack() ? 2 : 4);
        playerY = playerPanelY + 16;
        guiHeight = playerPanelY + 94;
        contents.clampScrollRow();
        if (!inventorySlots.isEmpty()) {
            layoutModeSlots();
            layoutPlayerSlots();
        }
        return dimensionsChanged;
    }

    public void setOwnerSettingsView(boolean settingsView) {
        if (settingsView) {
            for (Slot slot : inventorySlots) {
                slot.xPos = HIDDEN_SLOT_POSITION;
                slot.yPos = HIDDEN_SLOT_POSITION;
            }
            return;
        }
        if (equippedView) {
            inventorySlots.get(0).xPos = 8;
            inventorySlots.get(0).yPos = 18;
        }
        layoutModeSlots();
        layoutPlayerSlots();
    }

    public int getCapacity() {
        if (supportsSmugglerCompartments()) {
            ItemStack stack = getBackpackStack();
            ItemSmugglerBackpack smuggler = (ItemSmugglerBackpack) stack.getItem();
            return canAccessSmugglerHiddenCompartment() && hiddenSmugglerCompartment
                    ? smuggler.getHiddenSlotCount(stack) : smuggler.getVisibleSlotCount(stack);
        }
        int backendCapacity = contents.getBackendCapacity();
        if (!player.world.isRemote) {
            viewCapacityHighWater = backendCapacity;
            return backendCapacity;
        }
        int current = receivedCapacitySync
                ? Math.max(syncedCapacity, backendCapacity)
                : Math.max(announcedCapacity, Math.max(syncedCapacity, backendCapacity));
        viewCapacityHighWater = current;
        return viewCapacityHighWater;
    }

    public boolean supportsSmugglerCompartments() {
        return getBackpackStack().getItem() instanceof ItemSmugglerBackpack;
    }

    public boolean canAccessSmugglerHiddenCompartment() {
        ItemStack stack = getBackpackStack();
        return stack.getItem() instanceof ItemSmugglerBackpack smuggler
                && smuggler.canAccessHidden(stack, player);
    }

    public boolean isHiddenSmugglerCompartment() {
        return canAccessSmugglerHiddenCompartment() && hiddenSmugglerCompartment;
    }

    public boolean isRealityErrorBackpack() {
        return getBackpackStack().getItem() instanceof ItemRealityErrorBackpack;
    }

    public boolean isPocketHoleBackpack() {
        return getBackpackStack().getItem() instanceof ItemPocketHoleBackpack;
    }

    /**
     * True stored count behind a displayed content slot, for backpacks whose
     * slots hold more than one vanilla stack. Returns -1 when the slot has no
     * oversized count and should render normally.
     */
    public long getTrueSlotCount(int displaySlot) {
        if (!usesVirtualLongCounts() || displaySlot < 0 || displaySlot >= VISIBLE_SLOTS) return -1L;
        long count = contents.getTrueSlotCount(displaySlot);
        return count > 0L ? count : -1L;
    }

    public boolean usesVirtualLongCounts() {
        return virtualLongCounts;
    }

    public boolean usesServerAuthoritativeBlackHolePages() {
        return serverAuthoritativeBlackHolePages
                && BackpackConfig.usesSparseStorage(getBackpackStack());
    }

    public boolean beginBlackHolePageRequest() {
        if (!player.world.isRemote || !usesServerAuthoritativeBlackHolePages()) {
            return false;
        }
        ItemStack stack = getBackpackStack();
        UUID storageId = BackpackVirtualStorage.getStorageId(stack);
        if (storageId == null) return false;
        if (blackHolePagePending && !storageId.equals(blackHolePendingStorageId)) {
            blackHolePagePending = false;
        }
        if (blackHolePagePending) return false;
        blackHolePagePending = true;
        blackHolePendingStorageId = storageId;
        return true;
    }

    public boolean isBlackHolePagePending() {
        if (!usesServerAuthoritativeBlackHolePages()) {
            blackHolePagePending = false;
            blackHolePendingStorageId = null;
        }
        return blackHolePagePending;
    }

    public boolean beginVirtualClickRequest() {
        if (!player.world.isRemote || !usesVirtualLongCounts() || virtualClickPending) {
            return false;
        }
        virtualClickPending = true;
        return true;
    }

    public boolean isVirtualClickPending() {
        if (!usesVirtualLongCounts()) {
            virtualClickPending = false;
        }
        return virtualClickPending;
    }

    private boolean processingQuickCraft = false;

    private boolean isProcessingQuickCraft() {
        return processingQuickCraft;
    }

    public boolean isEquippedView() {
        return equippedView;
    }

    public int getContentX() {
        return contentX;
    }

    public int getContentStart() {
        return contentStart;
    }

    public int getContentY() {
        return contentY;
    }

    public int getColumns() {
        return columns;
    }

    public int getVisibleRows() {
        return visibleRows;
    }

    public int getVisibleSlotCount() {
        int firstSlot = contents.getScrollRow() * columns;
        return Math.max(0, Math.min(columns * visibleRows, getCapacity() - firstSlot));
    }

    public int getBackpackPaneHeight() {
        return backpackPaneHeight;
    }

    public int getUpgradeSlotCount() {
        return upgradeSlotCount;
    }

    public int getUpgradeStart() {
        return upgradeStart;
    }

    public boolean isUpgradeDrawerOpen() {
        return upgradeSlotCount > 0 && upgradeDrawerOpen;
    }

    public int getUpgradeDrawerBottom() {
        return upgradeSlotCount * 18 + (supportsWorkbench() ? 26 : 11);
    }

    public int getWorkbenchButtonX() {
        return UPGRADE_DRAWER_SLOT_X - 1;
    }

    public int getWorkbenchButtonY() {
        return upgradeSlotCount * 18 + 9;
    }

    public int getLeftExtension() {
        if (isWorkbenchView()) return -WORKBENCH_PANEL_LEFT;
        if (isUpgradeDrawerOpen()) return UPGRADE_DRAWER_WIDTH + 20;
        return upgradeSlotCount > 0 ? 20 : 0;
    }

    public int getAutoPickupButtonY() {
        return CONTROL_TOP;
    }

    public int getAutoSortButtonY() {
        return autoSortButtonY(supportsAutoPickup());
    }

    public int getActionButtonY() {
        return actionButtonY(supportsAutoPickup(), supportsAutoSorting(),
                canAccessSmugglerHiddenCompartment());
    }

    public int getSearchRelativeY() {
        int controlsBottom = 0;
        if (supportsAutoPickup()) controlsBottom = getAutoPickupButtonY() + CONTROL_HEIGHT;
        if (supportsAutoSorting()) controlsBottom = Math.max(controlsBottom, getAutoSortButtonY() + CONTROL_HEIGHT);
        if (supportsManualSorting() || canAccessSmugglerHiddenCompartment()) {
            controlsBottom = Math.max(controlsBottom, getActionButtonY() + CONTROL_HEIGHT);
        }
        return Math.max(MINIMUM_SEARCH_Y, controlsBottom + CONTROL_SECTION_GAP);
    }

    private int calculateContentY() {
        return contentYForControls(supportsAutoPickup(), supportsAutoSorting(),
                supportsManualSorting(), canAccessSmugglerHiddenCompartment());
    }

    static int autoSortButtonY(boolean autoPickup) {
        return CONTROL_TOP + (autoPickup ? CONTROL_HEIGHT + CONTROL_GAP : 0);
    }

    static int actionButtonY(boolean autoPickup, boolean autoSort, boolean smuggler) {
        int rows = (autoPickup ? 1 : 0) + (autoSort ? 1 : 0);
        int y = CONTROL_TOP + rows * (CONTROL_HEIGHT + CONTROL_GAP);
        return smuggler ? Math.max(48, y) : y;
    }

    static int contentYForControls(boolean autoPickup, boolean autoSort,
                                   boolean manualSort, boolean smuggler) {
        int controlsBottom = 0;
        if (autoPickup) controlsBottom = CONTROL_TOP + CONTROL_HEIGHT;
        if (autoSort) controlsBottom = Math.max(controlsBottom,
                autoSortButtonY(autoPickup) + CONTROL_HEIGHT);
        if (manualSort || smuggler) controlsBottom = Math.max(controlsBottom,
                actionButtonY(autoPickup, autoSort, smuggler) + CONTROL_HEIGHT);
        return Math.max(MINIMUM_SEARCH_Y, controlsBottom + CONTROL_SECTION_GAP) + 16;
    }

    public int getCraftStart() {
        return craftStart;
    }

    public boolean supportsWorkbench() {
        return (getAvailableActions() & BackpackUpgradeManager.ACTION_WORKBENCH) != 0;
    }

    public boolean isWorkbenchView() {
        return workbenchView && supportsWorkbench();
    }

    public int getPlayerPanelX() {
        return playerPanelX;
    }

    public int getPlayerPanelY() {
        return playerPanelY;
    }

    public int getPlayerY() {
        return playerY;
    }

    public int getGuiWidth() {
        return guiWidth;
    }

    public int getGuiHeight() {
        return guiHeight;
    }

    public int getScrollRow() {
        return contents.getScrollRow();
    }

    public int getMaxScrollRows() {
        return contents.getMaxScrollRows();
    }

    public int getFilledSlotCount() {
        return syncedFilled >= 0 ? syncedFilled : contents.getFilledSlotCount();
    }

    public boolean isInfiniteStorage() {
        return contents.isInfiniteStorage();
    }

    public boolean supportsAutoPickup() {
        return (getAvailableActions() & BackpackUpgradeManager.ACTION_AUTO_PICKUP) != 0;
    }

    public boolean isAutoPickupEnabled() {
        return syncedAutoPickup;
    }

    public boolean supportsAutoSorting() {
        return (getAvailableActions() & BackpackUpgradeManager.ACTION_AUTO_SORT) != 0;
    }

    public boolean supportsManualSorting() {
        return contents.supportsManualSorting();
    }

    public boolean isAutoSortEnabled() {
        return syncedAutoSort;
    }

    private int getAvailableActions() {
        if (!player.world.isRemote) return BackpackUpgradeManager.getAvailableActions(getBackpackStack());
        return receivedAvailableActions
                ? syncedAvailableActions
                : syncedAvailableActions | BackpackUpgradeManager.getAvailableActions(getBackpackStack());
    }

    public String getBackpackName() {
        ItemStack stack = getBackpackStack();
        return stack.isEmpty() ? "Backpack Slot" : stack.getDisplayName();
    }

    public ItemStack getBackpackStack() {
        return equippedView ? BackpackCapability.getData(player).getEquippedBackpack() : player.getHeldItem(heldHand);
    }

    public boolean isBlackBoxBackpack() {
        return getBackpackStack().getItem() instanceof ItemBlackBoxBackpack;
    }

    public boolean isBlackBoxOwner() {
        ItemStack stack = getBackpackStack();
        return stack.getItem() instanceof ItemBlackBoxBackpack blackBox && blackBox.isOwner(stack, player);
    }

    public boolean isSmugglerOwner() {
        ItemStack stack = getBackpackStack();
        return stack.getItem() instanceof ItemSmugglerBackpack smuggler && smuggler.isOwner(stack, player);
    }

    public boolean isOwnerManagedBackpack() {
        return isBlackBoxBackpack() || supportsSmugglerCompartments();
    }

    public boolean isBackpackOwner() {
        return isBlackBoxOwner() || isSmugglerOwner();
    }

    private static boolean isOwnerManagedBackpack(ItemStack stack) {
        return stack.getItem() instanceof ItemBlackBoxBackpack
                || stack.getItem() instanceof ItemSmugglerBackpack;
    }

    private static int getBaseStorageCapacity(ItemStack stack) {
        return stack.getItem() instanceof ItemBackpack item ? item.getSlots() : 0;
    }

    static int columnsForCapacity(int capacity) {
        return BackpackUpgradeManager.getStorageColumnCount(capacity);
    }

    private boolean usesLegacyTrueCountProperties() {
        return usesVirtualLongCounts() && !usesServerAuthoritativeBlackHolePages();
    }

    @Override
    public void addListener(IContainerListener listener) {
        super.addListener(listener);
        sendBackpackState(listener, getCapacity(), contents.getFilledSlotCount(),
                contents.isAutoPickupEnabled(), contents.isAutoSortEnabled());
        int actions = BackpackUpgradeManager.getAvailableActions(getBackpackStack());
        listener.sendWindowProperty(this, PROPERTY_AVAILABLE_ACTIONS, actions);
        lastSyncedAvailableActions = actions;
        listener.sendWindowProperty(this, PROPERTY_WORKBENCH_VIEW, isWorkbenchView() ? 1 : 0);
        listener.sendWindowProperty(this, PROPERTY_UPGRADE_DRAWER, isUpgradeDrawerOpen() ? 1 : 0);
        if (!player.world.isRemote && usesVirtualLongCounts()) {
            listener.sendWindowProperty(this, PROPERTY_VIRTUAL_CLICK_ACK, serverVirtualClickSequence);
            lastSyncedVirtualClickSequence = serverVirtualClickSequence;
        }
        if (usesLegacyTrueCountProperties()) sendAllTrueSlotCounts(listener);
        if (!player.world.isRemote && usesServerAuthoritativeBlackHolePages()) {
            syncBlackHolePageIfNeeded(true);
            primeVanillaBlackHoleContentCache();
        }
    }

    @Override
    public void detectAndSendChanges() {
        refreshLayout();
        contents.clampScrollRow();
        if (workbenchView && !supportsWorkbench()) {
            workbenchView = false;
            ItemBackpack.setWorkbenchPanelOpen(getBackpackStack(), false);
            if (!player.world.isRemote) returnWorkbenchContents();
            layoutModeSlots();
        }
        if (hiddenSmugglerCompartment && !canAccessSmugglerHiddenCompartment()) {
            hiddenSmugglerCompartment = false;
            contents.resetScroll();
            layoutContentSlots();
            if (!player.world.isRemote) {
                sendBackpackStateBeforeSlotSync(false);
            }
        }
        /*
         * NetHandlerPlayServer handles an accepted CPacketClickWindow with
         * EntityPlayerMP.isChangingQuantityOnly set. During that call vanilla
         * suppresses both slot packets and the cursor packet because it expects
         * the client to have predicted the click. Virtual-count backpacks are
         * deliberately server-authoritative, so consuming their slot changes
         * in that suppressed pass leaves the client with an invisible cursor
         * and stale slots. Keep the vanilla cache unchanged for that one pass;
         * the normal container tick then publishes the real server state.
         */
        boolean deferVirtualSlotSync = !player.world.isRemote
                && usesVirtualLongCounts() && virtualClickAckDelay > 0;
        boolean serverBlackHolePage = !player.world.isRemote && usesServerAuthoritativeBlackHolePages();
        if (serverBlackHolePage) {
            primeVanillaBlackHoleContentCache();
        } else if (!player.world.isRemote) {
            blackHolePageInitialized = false;
            lastSyncedBlackHoleStorageId = null;
            int scrollRow = contents.getScrollRow();
            if (scrollRow != lastSyncedScrollRow) {
                for (IContainerListener listener : listeners) {
                    listener.sendWindowProperty(this, PROPERTY_SCROLL_ROW, scrollRow);
                }
                lastSyncedScrollRow = scrollRow;
            }
        }
        if (!deferVirtualSlotSync) {
            super.detectAndSendChanges();
        }
        if (serverBlackHolePage && !deferVirtualSlotSync) {
            boolean forceFull = blackHolePageNeedsFullSync;
            blackHolePageNeedsFullSync = false;
            syncBlackHolePageIfNeeded(forceFull);
        }
        if (player.world.isRemote) return;
        if (deferVirtualSlotSync) {
            syncVirtualClickAcknowledgement();
            return;
        }

        int capacity = getCapacity();
        int filled = contents.getFilledSlotCount();
        boolean autoPickup = contents.isAutoPickupEnabled();
        boolean autoSort = contents.isAutoSortEnabled();
        boolean virtualCounts = usesLegacyTrueCountProperties();
        boolean backpackStateChanged = capacity != lastSyncedCapacity || filled != lastSyncedFilled
                || !sentAutomationState
                || autoPickup != lastSyncedAutoPickup || autoSort != lastSyncedAutoSort
                || hiddenSmugglerCompartment != lastSyncedHiddenSmugglerCompartment;
        boolean virtualModeChanged = virtualCounts != lastSyncedVirtualCounts;
        boolean trueCountsChanged = virtualCounts && haveTrueSlotCountsChanged();
        if (backpackStateChanged || virtualModeChanged || trueCountsChanged) {
            for (IContainerListener listener : listeners) {
                if (backpackStateChanged || virtualModeChanged) {
                    sendBackpackState(listener, capacity, filled, autoPickup, autoSort);
                }
                if (virtualCounts) {
                    for (int slot = 0; slot < TRUE_COUNT_SLOTS; slot++) {
                        long count = Math.max(0L, contents.getTrueSlotCount(slot));
                        if (virtualModeChanged || count != lastSyncedTrueCounts[slot]) {
                            sendTrueSlotCount(listener, slot, count);
                        }
                    }
                }
            }
            lastSyncedCapacity = capacity;
            lastSyncedFilled = filled;
            lastSyncedAutoPickup = autoPickup;
            lastSyncedAutoSort = autoSort;
            lastSyncedHiddenSmugglerCompartment = hiddenSmugglerCompartment;
            lastSyncedVirtualCounts = virtualCounts;
            for (int slot = 0; slot < TRUE_COUNT_SLOTS; slot++) {
                lastSyncedTrueCounts[slot] = virtualCounts ? Math.max(0L, contents.getTrueSlotCount(slot)) : 0L;
            }
            sentAutomationState = true;
        }
        if (lastSyncedWorkbenchView != isWorkbenchView()) {
            for (IContainerListener listener : listeners) {
                listener.sendWindowProperty(this, PROPERTY_WORKBENCH_VIEW, isWorkbenchView() ? 1 : 0);
            }
            lastSyncedWorkbenchView = isWorkbenchView();
        }
        if (lastSyncedUpgradeDrawerOpen != isUpgradeDrawerOpen()) {
            for (IContainerListener listener : listeners) {
                listener.sendWindowProperty(this, PROPERTY_UPGRADE_DRAWER, isUpgradeDrawerOpen() ? 1 : 0);
            }
            lastSyncedUpgradeDrawerOpen = isUpgradeDrawerOpen();
        }
        int actions = BackpackUpgradeManager.getAvailableActions(getBackpackStack());
        if (lastSyncedAvailableActions != actions) {
            for (IContainerListener listener : listeners) {
                listener.sendWindowProperty(this, PROPERTY_AVAILABLE_ACTIONS, actions);
            }
            lastSyncedAvailableActions = actions;
        }
        syncVirtualClickAcknowledgement();
    }

    @Override
    public void onCraftMatrixChanged(IInventory inventory) {
        if (inventory == craftMatrix) {
            craftResult.setInventorySlotContents(0, CraftingManager.findMatchingResult(craftMatrix, player.world));
        }
    }

    private void syncVirtualClickAcknowledgement() {
        if (!usesVirtualLongCounts()) {
            virtualClickAckDelay = 0;
            lastSyncedVirtualClickSequence = serverVirtualClickSequence;
            return;
        }
        if (virtualClickAckDelay > 0) {
            virtualClickAckDelay--;
            return;
        }
        if (lastSyncedVirtualClickSequence == serverVirtualClickSequence) return;
        if (player instanceof EntityPlayerMP) {
            ((EntityPlayerMP) player).updateHeldItem();
        }
        for (IContainerListener listener : listeners) {
            listener.sendWindowProperty(this, PROPERTY_VIRTUAL_CLICK_ACK, serverVirtualClickSequence);
        }
        lastSyncedVirtualClickSequence = serverVirtualClickSequence;
    }

    private void scheduleVirtualClickAcknowledgement() {
        serverVirtualClickSequence = serverVirtualClickSequence >= Short.MAX_VALUE
                ? 0 : serverVirtualClickSequence + 1;
        virtualClickAckDelay = 1;
    }

    private boolean haveTrueSlotCountsChanged() {
        for (int slot = 0; slot < TRUE_COUNT_SLOTS; slot++) {
            if (Math.max(0L, contents.getTrueSlotCount(slot)) != lastSyncedTrueCounts[slot]) return true;
        }
        return false;
    }

    private void sendAllTrueSlotCounts(IContainerListener listener) {
        for (int slot = 0; slot < TRUE_COUNT_SLOTS; slot++) {
            sendTrueSlotCount(listener, slot, Math.max(0L, contents.getTrueSlotCount(slot)));
        }
    }

    private void sendTrueSlotCount(IContainerListener listener, int slot, long count) {
        for (int word = 0; word < TRUE_COUNT_WORDS; word++) {
            int property = PROPERTY_TRUE_COUNT_BASE + slot * TRUE_COUNT_WORDS + word;
            int value = (int) (count >>> (word * 16) & 0xFFFFL);
            listener.sendWindowProperty(this, property, value);
        }
    }

    private void syncBlackHolePageIfNeeded(boolean forceFull) {
        if (!(player instanceof EntityPlayerMP) || !usesServerAuthoritativeBlackHolePages()) return;

        ItemStack backpackStack = getBackpackStack();
        UUID storageId = BackpackVirtualStorage.getOrCreateStorageId(backpackStack);
        int scrollRow = contents.getScrollRow();
        int capacity = getCapacity();
        int filled = contents.getFilledSlotCount();
        List<Integer> changedSlots = new ArrayList<>();
        ItemStack[] currentStacks = new ItemStack[VISIBLE_SLOTS];
        long[] currentCounts = new long[VISIBLE_SLOTS];

        for (int displaySlot = 0; displaySlot < VISIBLE_SLOTS; displaySlot++) {
            ItemStack current = contents.isAvailable(displaySlot)
                    ? contents.getStackInSlot(displaySlot) : ItemStack.EMPTY;
            if (!current.isEmpty()) {
                current = current.copy();
                current.setCount(1);
            }
            long count = current.isEmpty() ? 0L : Math.max(0L, contents.getTrueSlotCount(displaySlot));
            currentStacks[displaySlot] = current;
            currentCounts[displaySlot] = count;
            ItemStack previous = lastSyncedBlackHolePageStacks[displaySlot];
            if (previous == null || !ItemStack.areItemStacksEqual(previous, current)
                    || lastSyncedBlackHolePageCounts[displaySlot] != count) {
                changedSlots.add(displaySlot);
            }
        }

        boolean storageChanged = !storageId.equals(lastSyncedBlackHoleStorageId);
        boolean pageChanged = storageChanged
                || scrollRow != lastSyncedBlackHolePageRow
                || capacity != lastSyncedBlackHolePageCapacity;
        boolean stateChanged = filled != lastSyncedBlackHolePageFilled;
        if (!forceFull && blackHolePageInitialized && !pageChanged
                && !stateChanged && changedSlots.isEmpty()) {
            return;
        }

        boolean full = forceFull || !blackHolePageInitialized || pageChanged
                || changedSlots.size() > BLACK_HOLE_FULL_PAGE_DELTA_THRESHOLD;
        List<Integer> packetSlots = new ArrayList<>();
        if (full) {
            for (int displaySlot = 0; displaySlot < VISIBLE_SLOTS; displaySlot++) {
                packetSlots.add(displaySlot);
            }
        } else {
            packetSlots.addAll(changedSlots);
        }

        int[] indices = new int[packetSlots.size()];
        ItemStack[] prototypes = new ItemStack[packetSlots.size()];
        long[] counts = new long[packetSlots.size()];
        for (int index = 0; index < packetSlots.size(); index++) {
            int displaySlot = packetSlots.get(index);
            indices[index] = displaySlot;
            prototypes[index] = currentStacks[displaySlot].isEmpty()
                    ? ItemStack.EMPTY : currentStacks[displaySlot].copy();
            counts[index] = currentCounts[displaySlot];
        }

        if (blackHolePageSequence == Integer.MAX_VALUE) blackHolePageSequence = 0;
        PacketDispatcher.sendTo(new BlackHoleBackpackPagePacket(
                windowId, storageId, ++blackHolePageSequence, scrollRow, capacity, filled,
                full, blackHoleScrollAcknowledgement, indices, prototypes, counts), (EntityPlayerMP) player);
        blackHoleScrollAcknowledgement = false;

        for (int displaySlot = 0; displaySlot < VISIBLE_SLOTS; displaySlot++) {
            lastSyncedBlackHolePageStacks[displaySlot] = currentStacks[displaySlot].isEmpty()
                    ? ItemStack.EMPTY : currentStacks[displaySlot].copy();
            lastSyncedBlackHolePageCounts[displaySlot] = currentCounts[displaySlot];
        }
        blackHolePageInitialized = true;
        lastSyncedBlackHolePageRow = scrollRow;
        lastSyncedBlackHolePageCapacity = capacity;
        lastSyncedBlackHolePageFilled = filled;
        lastSyncedBlackHoleStorageId = storageId;
    }

    private void primeVanillaBlackHoleContentCache() {
        for (int displaySlot = 0; displaySlot < VISIBLE_SLOTS; displaySlot++) {
            int containerSlot = contentStart + displaySlot;
            if (containerSlot < 0 || containerSlot >= inventoryItemStacks.size()) continue;
            ItemStack current = contents.getStackInSlot(displaySlot);
            inventoryItemStacks.set(containerSlot,
                    current.isEmpty() ? ItemStack.EMPTY : current.copy());
        }
    }

    public void applyBlackHolePage(UUID storageId, int sequence, int scrollRow, int capacity, int filled,
                                   boolean full, boolean scrollAcknowledgement,
                                   int[] indices, ItemStack[] prototypes, long[] counts) {
        if (!player.world.isRemote || windowId < 0 || !usesServerAuthoritativeBlackHolePages()) return;
        if (!isNewerBlackHolePageSequence(sequence, lastAppliedBlackHolePageSequence)
                || indices == null || prototypes == null || counts == null
                || indices.length != prototypes.length || indices.length != counts.length) {
            return;
        }
        lastAppliedBlackHolePageSequence = sequence;

        ItemStack backpackStack = getBackpackStack();
        UUID currentStorageId = BackpackVirtualStorage.getStorageId(backpackStack);
        if (currentStorageId == null || !currentStorageId.equals(storageId)) return;
        if (!full && contents.getScrollRow() != scrollRow) return;

        syncedCapacity = Math.max(0, Math.min(
                BlackHoleBackpackSavedData.BackpackStorage.MAX_LOGICAL_SLOTS, capacity));
        receivedCapacitySync = true;
        syncedFilled = Math.max(0, filled);
        if (full) {
            contents.resetClientBlackHoleStorage(syncedCapacity);
            contents.setScrollRowFromServer(scrollRow);
        }

        for (int index = 0; index < indices.length; index++) {
            int displaySlot = indices[index];
            if (displaySlot < 0 || displaySlot >= VISIBLE_SLOTS) continue;
            ItemStack prototype = prototypes[index];
            long count = Math.max(0L, counts[index]);
            if (prototype == null || prototype.isEmpty() || count <= 0L) {
                contents.applyClientBlackHolePageSlot(scrollRow, displaySlot, ItemStack.EMPTY, 0L);
            } else {
                ItemStack normalized = prototype.copy();
                normalized.setCount(1);
                contents.applyClientBlackHolePageSlot(scrollRow, displaySlot, normalized, count);
            }
        }

        for (int displaySlot = 0; displaySlot < VISIBLE_SLOTS; displaySlot++) {
            int containerSlot = contentStart + displaySlot;
            if (containerSlot < 0 || containerSlot >= inventoryItemStacks.size()) continue;
            ItemStack current = contents.getStackInSlot(displaySlot);
            inventoryItemStacks.set(containerSlot,
                    current.isEmpty() ? ItemStack.EMPTY : current.copy());
        }
        lastAppliedBlackHolePageSequence = sequence;
        if (full && (scrollAcknowledgement || blackHolePendingStorageId == null
                || !storageId.equals(blackHolePendingStorageId))) {
            blackHolePagePending = false;
            blackHolePendingStorageId = null;
        }
        layoutContentSlots();
    }

    private static boolean isNewerBlackHolePageSequence(int sequence, int previous) {
        if (sequence <= 0) return false;
        if (previous <= 0) return true;
        if (sequence == previous) return false;

        long modulus = Integer.MAX_VALUE;
        long forward = (sequence - (long) previous + modulus) % modulus;
        return forward > 0L && forward <= modulus / 2L;
    }

    @Override
    public void updateProgressBar(int id, int data) {
        if (id == PROPERTY_CAPACITY_LOW) {
            syncedCapacity = (syncedCapacity & 0xFFFF0000) | (data & 0xFFFF);
        } else if (id == PROPERTY_CAPACITY_HIGH) {
            syncedCapacity = (syncedCapacity & 0xFFFF) | ((data & 0xFFFF) << 16);
            receivedCapacitySync = true;
            refreshLayout();
        } else if (id == PROPERTY_FILLED_LOW) {
            syncedFilled = (Math.max(0, syncedFilled) & 0xFFFF0000) | (data & 0xFFFF);
        } else if (id == PROPERTY_FILLED_HIGH) {
            syncedFilled = (Math.max(0, syncedFilled) & 0xFFFF) | ((data & 0xFFFF) << 16);
        } else if (id == PROPERTY_AUTO_PICKUP) {
            syncedAutoPickup = data != 0;
            if (player.world.isRemote) contents.setAutoPickupEnabled(syncedAutoPickup);
        } else if (id == PROPERTY_AUTO_SORT) {
            syncedAutoSort = data != 0;
            if (player.world.isRemote) contents.setAutoSortEnabled(syncedAutoSort);
        } else if (id == PROPERTY_WORKBENCH_VIEW) {
            workbenchView = data != 0 && supportsWorkbench();
            layoutModeSlots();
        } else if (id == PROPERTY_UPGRADE_DRAWER) {
            upgradeDrawerOpen = data != 0 && upgradeSlotCount > 0;
            layoutUpgradeSlots();
        } else if (id == PROPERTY_AVAILABLE_ACTIONS) {
            syncedAvailableActions = data;
            receivedAvailableActions = true;
            if ((data & BackpackUpgradeManager.ACTION_WORKBENCH) == 0) workbenchView = false;
            refreshLayout();
            layoutModeSlots();
        } else if (id == PROPERTY_SMUGGLER_COMPARTMENT) {
            boolean nextHidden = data != 0 && canAccessSmugglerHiddenCompartment();
            if (hiddenSmugglerCompartment != nextHidden) {
                hiddenSmugglerCompartment = nextHidden;
                contents.resetScroll();
            }
        } else if (id == PROPERTY_SCROLL_ROW) {
            if (player.world.isRemote && !usesServerAuthoritativeBlackHolePages()) {
                contents.setScrollRowFromServer(data);
            }
        } else if (id == PROPERTY_VIRTUAL_CLICK_ACK) {
            if (player.world.isRemote && data != clientVirtualClickSequence) {
                clientVirtualClickSequence = data;
                virtualClickPending = false;
            }
        } else if (id >= PROPERTY_TRUE_COUNT_BASE
                && id < PROPERTY_TRUE_COUNT_BASE + TRUE_COUNT_SLOTS * TRUE_COUNT_WORDS) {
            int relative = id - PROPERTY_TRUE_COUNT_BASE;
            int slot = relative / TRUE_COUNT_WORDS;
            int word = relative % TRUE_COUNT_WORDS;
            int shift = word * 16;
            long current = Math.max(0L, contents.getTrueSlotCount(slot));
            long mask = 0xFFFFL << shift;
            long updated = current & ~mask | ((long) data & 0xFFFFL) << shift;
            contents.setClientTrueSlotCount(slot, updated < 0L ? Long.MAX_VALUE : updated);
        } else {
            super.updateProgressBar(id, data);
            return;
        }
        layoutContentSlots();
    }

    private void sendBackpackState(IContainerListener listener, int capacity, int filled, boolean autoPickup, boolean autoSort) {
        listener.sendWindowProperty(this, PROPERTY_CAPACITY_LOW, capacity & 0xFFFF);
        listener.sendWindowProperty(this, PROPERTY_CAPACITY_HIGH, capacity >>> 16 & 0xFFFF);
        listener.sendWindowProperty(this, PROPERTY_FILLED_LOW, filled & 0xFFFF);
        listener.sendWindowProperty(this, PROPERTY_FILLED_HIGH, filled >>> 16 & 0xFFFF);
        listener.sendWindowProperty(this, PROPERTY_AUTO_PICKUP, autoPickup ? 1 : 0);
        listener.sendWindowProperty(this, PROPERTY_AUTO_SORT, autoSort ? 1 : 0);
        listener.sendWindowProperty(this, PROPERTY_SMUGGLER_COMPARTMENT,
                hiddenSmugglerCompartment && canAccessSmugglerHiddenCompartment() ? 1 : 0);
        if (!usesServerAuthoritativeBlackHolePages()) {
            listener.sendWindowProperty(this, PROPERTY_SCROLL_ROW, contents.getScrollRow());
        }
    }

    private void sendBackpackStateBeforeSlotSync(boolean forceContentSlots) {
        int capacity = getCapacity();
        int filled = contents.getFilledSlotCount();
        boolean autoPickup = contents.isAutoPickupEnabled();
        boolean autoSort = contents.isAutoSortEnabled();
        for (IContainerListener listener : listeners) {
            sendBackpackState(listener, capacity, filled, autoPickup, autoSort);
            if (forceContentSlots) {
                int visibleSlots = getVisibleSlotCount();
                for (int displaySlot = 0; displaySlot < visibleSlots; displaySlot++) {
                    Slot slot = inventorySlots.get(contentStart + displaySlot);
                    listener.sendSlotContents(this, contentStart + displaySlot, slot.getStack().copy());
                }
            }
            if (usesLegacyTrueCountProperties()) sendAllTrueSlotCounts(listener);
        }
        lastSyncedCapacity = capacity;
        lastSyncedFilled = filled;
        lastSyncedAutoPickup = autoPickup;
        lastSyncedAutoSort = autoSort;
        lastSyncedHiddenSmugglerCompartment = hiddenSmugglerCompartment;
        lastSyncedScrollRow = contents.getScrollRow();
        lastSyncedVirtualCounts = usesLegacyTrueCountProperties();
        for (int slot = 0; slot < TRUE_COUNT_SLOTS; slot++) {
            lastSyncedTrueCounts[slot] = lastSyncedVirtualCounts
                    ? Math.max(0L, contents.getTrueSlotCount(slot)) : 0L;
        }
        sentAutomationState = true;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        Slot slot = inventorySlots.get(index);
        if (slot == null || !slot.getHasStack()) return ItemStack.EMPTY;

        ItemStack source = slot.getStack();
        ItemStack original = source.copy();
        int contentEnd = contentStart + VISIBLE_SLOTS;
        int upgradeEnd = upgradeStart + upgradeSlotCount;
        int craftEnd = craftStart + 10;

        if (index == craftStart) {
            ItemStack preview = simulatePlayerMerge(source);
            preview = contents.insertCraftingResult(preview, true);
            if (!preview.isEmpty()) return ItemStack.EMPTY;

            mergeItemStack(source, playerStart, inventorySlots.size(), true);
            if (!source.isEmpty()) {
                requestBlackHoleFullPageSync();
                ItemStack remaining = contents.insertCraftingResult(source.copy(), false);
                source.setCount(remaining.isEmpty() ? 0 : remaining.getCount());
            }
            if (!source.isEmpty()) return ItemStack.EMPTY;
            slot.onSlotChange(source, original);
            slot.onTake(player, source);
            return original;
        }

        if (equippedView && index == 0) {
            if (!mergeItemStack(source, playerStart, inventorySlots.size(), false)) return ItemStack.EMPTY;
        } else if (index >= contentStart && index < contentEnd) {
            if (isVirtualContentSlot(index)) {
                return transferVirtualContentToPlayer(slot, index - contentStart);
            }
            if (!mergeItemStack(source, playerStart, inventorySlots.size(), false)) return ItemStack.EMPTY;
        } else if (index >= upgradeStart && index < upgradeEnd) {
            int upgradeSlot = index - upgradeStart;
            ItemStack extracted = upgrades.extractItem(upgradeSlot, source.getCount(), false);
            if (extracted.isEmpty()) return ItemStack.EMPTY;
            ItemStack remaining = extracted.copy();
            if (!mergeItemStack(remaining, playerStart, inventorySlots.size(), false)) {
                upgrades.insertItem(upgradeSlot, extracted, false);
                return ItemStack.EMPTY;
            }
            if (!remaining.isEmpty()) upgrades.insertItem(upgradeSlot, remaining, false);
            slot.onSlotChanged();
            return original;
        } else if (index > craftStart && index < craftEnd) {
            if (!mergeItemStack(source, playerStart, inventorySlots.size(), false)) return ItemStack.EMPTY;
        } else if (equippedView && source.getItem() instanceof ItemBackpack) {
            if (!mergeItemStack(source, 0, 1, false)) return ItemStack.EMPTY;
        } else {
            if (isUpgradeDrawerOpen()
                    && source.getItem() instanceof com.hbm.items.tool.ItemBackpackUpgrade) {
                if (mergeItemStack(source, upgradeStart, upgradeEnd, false)) {
                    if (source.isEmpty()) slot.putStack(ItemStack.EMPTY);
                    else slot.onSlotChanged();
                    return original;
                }
            }
            if (ItemBackpack.isForbiddenBackpackContent(source)) return ItemStack.EMPTY;
            requestBlackHoleFullPageSync();
            ItemStack remaining = contents.insertItemAnywhere(source.copy(), false);
            if (remaining.getCount() == source.getCount()) return ItemStack.EMPTY;
            source.setCount(remaining.getCount());
        }

        if (source.isEmpty()) {
            slot.putStack(ItemStack.EMPTY);
        } else {
            slot.onSlotChanged();
        }
        return original;
    }

    private ItemStack simulatePlayerMerge(ItemStack source) {
        ItemStack remaining = source.copy();
        for (int pass = 0; pass < 2 && !remaining.isEmpty(); pass++) {
            for (int index = inventorySlots.size() - 1; index >= playerStart && !remaining.isEmpty(); index--) {
                Slot target = inventorySlots.get(index);
                ItemStack present = target.getStack();
                if (!target.isItemValid(remaining)) continue;
                if (pass == 0) {
                    if (present.isEmpty() || !ItemStack.areItemsEqual(present, remaining)
                            || !ItemStack.areItemStackTagsEqual(present, remaining)) continue;
                    int limit = Math.min(target.getSlotStackLimit(), remaining.getMaxStackSize());
                    int moved = Math.min(remaining.getCount(), Math.max(0, limit - present.getCount()));
                    remaining.shrink(moved);
                } else {
                    if (!present.isEmpty()) continue;
                    int limit = Math.min(target.getSlotStackLimit(), remaining.getMaxStackSize());
                    remaining.shrink(Math.min(remaining.getCount(), limit));
                }
            }
        }
        return remaining;
    }

    private ItemStack transferVirtualContentToPlayer(Slot slot, int displaySlot) {
        ItemStack snapshot = getVirtualTransactionSnapshot(displaySlot);
        if (snapshot.isEmpty()) return ItemStack.EMPTY;
        if (!player.inventory.getItemStack().isEmpty()) return snapshot;

        ItemStack extractable = contents.extractItem(displaySlot, snapshot.getCount(), true);
        if (extractable.isEmpty()) return ItemStack.EMPTY;

        int transferCount = extractable.getCount();
        ItemStack transfer = extractable.copy();
        if (!mergeItemStack(transfer, playerStart, inventorySlots.size(), false)) return ItemStack.EMPTY;

        int moved = transferCount - (transfer.isEmpty() ? 0 : transfer.getCount());
        if (moved <= 0) return ItemStack.EMPTY;
        contents.extractItem(displaySlot, moved, false);
        slot.onSlotChanged();
        syncVirtualSlotCache(slot.slotNumber, displaySlot);
        return snapshot;
    }

    /**
     * The transaction response carries one legal vanilla stack, while the slot
     * itself still renders a count-one proxy. If a client's long count is stale,
     * its response differs from the server and vanilla rejects/resynchronizes the
     * transaction instead of accepting different extraction amounts.
     */
    private ItemStack getVirtualTransactionSnapshot(int displaySlot) {
        long trueCount = contents.getTrueSlotCount(displaySlot);
        ItemStack prototype = contents.getStackInSlot(displaySlot);
        if (trueCount <= 0L || prototype.isEmpty()) return ItemStack.EMPTY;
        ItemStack snapshot = prototype.copy();
        snapshot.setCount((int) Math.min((long) Math.max(1, prototype.getMaxStackSize()), trueCount));
        return snapshot;
    }

    private boolean isContentSlot(int slotId) {
        return slotId >= contentStart && slotId < contentStart + VISIBLE_SLOTS;
    }

    public boolean isSlotActive(int slotId) {
        if (slotId < 0 || slotId >= inventorySlots.size()) return false;
        if (equippedView && slotId == 0) return true;
        if (isContentSlot(slotId)) {
            return slotId - contentStart < getVisibleSlotCount();
        }
        if (slotId >= upgradeStart && slotId < upgradeStart + upgradeSlotCount) {
            return isUpgradeDrawerOpen();
        }
        if (slotId >= craftStart && slotId < craftStart + 10) {
            return isWorkbenchView();
        }
        return slotId >= playerStart;
    }

    private boolean isVirtualContentSlot(int slotId) {
        if (!usesVirtualLongCounts() || !isContentSlot(slotId)) return false;
        int displaySlot = slotId - contentStart;
        return displaySlot >= 0 && displaySlot < columns * visibleRows;
    }

    private boolean isForbiddenBackpackPlacement(int slotId, int dragType, ClickType clickType, EntityPlayer player) {
        if (clickType == ClickType.QUICK_MOVE) {
            return slotId >= playerStart && slotId < inventorySlots.size()
                    && ItemBackpack.isForbiddenBackpackContent(inventorySlots.get(slotId).getStack());
        }
        if (!isContentSlot(slotId)) return false;

        ItemStack candidate = ItemStack.EMPTY;
        if (clickType == ClickType.SWAP && dragType >= 0 && dragType < player.inventory.getSizeInventory()) {
            candidate = player.inventory.getStackInSlot(dragType);
        } else if (clickType == ClickType.PICKUP || clickType == ClickType.QUICK_CRAFT) {
            candidate = player.inventory.getItemStack();
        }
        return ItemBackpack.isForbiddenBackpackContent(candidate);
    }

    @Override
    public ItemStack slotClick(int slotId, int dragType, ClickType clickType, EntityPlayer player) {
        if (slotId >= 0 && !isSlotActive(slotId)) return ItemStack.EMPTY;
        if (player.world.isRemote) {
            if (!usesVirtualLongCounts()) {
                return super.slotClick(slotId, dragType, clickType, player);
            }
            if ((clickType == ClickType.PICKUP || clickType == ClickType.QUICK_MOVE)
                    && slotId >= 0 && slotId < inventorySlots.size()) {
                if (isVirtualContentSlot(slotId)) {
                    return getVirtualTransactionSnapshot(slotId - contentStart);
                }
                ItemStack stack = inventorySlots.get(slotId).getStack();
                return stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
            }
            return ItemStack.EMPTY;
        }
        if (!canPrepareCapacityRemoval(slotId, dragType, clickType)) return ItemStack.EMPTY;
        boolean virtualCounts = usesVirtualLongCounts();
        NBTTagCompound upgradesBefore = upgrades.serializeNBT().copy();
        int capacityBefore = getCapacity();
        boolean workbenchBefore = supportsWorkbench();
        boolean acknowledgeVirtualClick = virtualCounts
                && (clickType != ClickType.QUICK_CRAFT || (dragType & 3) == 2);
        try {
            if (heldHand == EnumHand.MAIN_HAND) {
                int heldInventorySlot = player.inventory.currentItem;
                int heldContainerSlot = playerStart + 27 + heldInventorySlot;
                if (slotId == heldContainerSlot || clickType == ClickType.SWAP && dragType == player.inventory.currentItem) {
                    return ItemStack.EMPTY;
                }
            }
            boolean completedQuickCraft = clickType == ClickType.QUICK_CRAFT && (dragType & 3) == 2;
            if (usesServerAuthoritativeBlackHolePages()
                    && (clickType == ClickType.QUICK_CRAFT ? completedQuickCraft : isContentSlot(slotId))) {
                blackHoleScrollAcknowledgement = true;
                requestBlackHoleFullPageSync();
            }
            if (isForbiddenBackpackPlacement(slotId, dragType, clickType, player)) {
                return ItemStack.EMPTY;
            }
            if (virtualCounts && clickType == ClickType.QUICK_CRAFT) {
                processingQuickCraft = true;
                try {
                    return super.slotClick(slotId, dragType, clickType, player);
                } finally {
                    processingQuickCraft = false;
                }
            }
            if (isVirtualContentSlot(slotId)) {
                invalidateVirtualCountSync(slotId - contentStart);
                ItemStack result = ItemStack.EMPTY;
                if (clickType == ClickType.PICKUP) {
                    result = clickVirtualContentSlot(slotId, dragType, player);
                } else if (clickType == ClickType.THROW) {
                    result = throwFromVirtualContentSlot(slotId, dragType, player);
                } else if (clickType == ClickType.QUICK_MOVE) {
                    result = transferVirtualContentToPlayer(inventorySlots.get(slotId), slotId - contentStart);
                } else if (clickType == ClickType.SWAP) {
                    result = swapVirtualContentWithHotbar(slotId, dragType, player);
                }
                syncVirtualSlotCache(slotId, slotId - contentStart);
                return result;
            }
            return super.slotClick(slotId, dragType, clickType, player);
        } finally {
            if (!upgradesBefore.equals(upgrades.serializeNBT())) {
                handleUpgradeChange(capacityBefore, workbenchBefore);
            }
            if (acknowledgeVirtualClick) {
                scheduleVirtualClickAcknowledgement();
            }
        }
    }

    private void handleUpgradeChange(int oldCapacity, boolean hadWorkbench) {
        ItemStack backpack = getBackpackStack();
        int newCapacity = backpack.getItem() instanceof ItemBackpack item
                ? item.getStorageSlots(backpack) : getCapacity();
        if (newCapacity < oldCapacity) {
            if (BackpackConfig.usesSparseStorage(backpack)) {
                dropSparseOverflow(backpack, newCapacity);
            } else if (backpack.getItem() instanceof ItemPocketHoleBackpack pocketHole) {
                dropPocketHoleOverflow(pocketHole, backpack, newCapacity);
            } else {
                for (ItemStack overflow : BackpackUpgradeManager.takeOverflowItems(backpack, newCapacity)) {
                    EntityItem dropped = player.dropItem(overflow, false);
                    if (dropped != null) dropped.setPickupDelay(40);
                }
            }
        }
        if (hadWorkbench && !supportsWorkbench()) {
            workbenchView = false;
            ItemBackpack.setWorkbenchPanelOpen(backpack, false);
            if (!player.world.isRemote) returnWorkbenchContents();
        }
        refreshLayout();
        contents.clampScrollRow();
        sendBackpackStateBeforeSlotSync(true);
        BackpackHandler.syncEquipmentState(player);
    }

    private boolean canPrepareCapacityRemoval(int slotId, int dragType, ClickType clickType) {
        ItemStack backpack = getBackpackStack();
        if (!BackpackConfig.usesSparseStorage(backpack)
                && !(backpack.getItem() instanceof ItemPocketHoleBackpack)) return true;
        if (clickType == ClickType.QUICK_CRAFT || clickType == ClickType.CLONE) return true;

        List<Integer> removedSlots = new ArrayList<>();
        if (slotId >= upgradeStart && slotId < upgradeStart + upgradeSlotCount) {
            int upgradeSlot = slotId - upgradeStart;
            ItemStack installed = upgrades.getStackInSlot(upgradeSlot);
            if (installed.getItem() instanceof com.hbm.items.tool.ItemBackpackUpgrade upgrade
                    && upgrade.getUpgradeType() == BackpackUpgradeType.CAPACITY) {
                removedSlots.add(upgradeSlot);
            }
        }
        if (clickType == ClickType.PICKUP_ALL) {
            for (int slot = 0; slot < upgrades.getSlots(); slot++) {
                ItemStack installed = upgrades.getStackInSlot(slot);
                if (installed.getItem() instanceof com.hbm.items.tool.ItemBackpackUpgrade upgrade
                        && upgrade.getUpgradeType() == BackpackUpgradeType.CAPACITY
                        && !removedSlots.contains(slot)) {
                    removedSlots.add(slot);
                }
            }
        }
        if (removedSlots.isEmpty()) return true;

        ItemStack preview = backpack.copy();
        UpgradeInventory previewUpgrades = BackpackUpgradeManager.createInventory(preview);
        for (int slot : removedSlots) {
            ItemStack installed = previewUpgrades.getStackInSlot(slot);
            previewUpgrades.extractItem(slot, installed.getCount(), false);
        }
        if (clickType != ClickType.PICKUP_ALL && removedSlots.size() == 1) {
            ItemStack replacement = ItemStack.EMPTY;
            if (clickType == ClickType.PICKUP) {
                replacement = player.inventory.getItemStack();
            } else if (clickType == ClickType.SWAP
                    && dragType >= 0 && dragType < player.inventory.getSizeInventory()) {
                replacement = player.inventory.getStackInSlot(dragType);
            }
            if (!replacement.isEmpty()) {
                ItemStack single = replacement.copy();
                single.setCount(1);
                previewUpgrades.insertItem(removedSlots.get(0), single, false);
            }
        }
        ItemBackpack item = (ItemBackpack) backpack.getItem();
        int newCapacity = item.getStorageSlots(preview);
        if (newCapacity >= item.getStorageSlots(backpack)) return true;
        if (BackpackConfig.usesSparseStorage(backpack)) {
            return BackpackVirtualStorage.getStorage(player.world, backpack)
                    .prepareShrink(newCapacity).isPrepared();
        }
        ItemPocketHoleBackpack pocketHole = (ItemPocketHoleBackpack) item;
        return pocketHole.getServerStorage(player.world, backpack).prepareShrink(newCapacity).isPrepared();
    }

    private void dropSparseOverflow(ItemStack backpack, int newCapacity) {
        BlackHoleBackpackSavedData.BackpackStorage storage =
                BackpackVirtualStorage.getStorage(player.world, backpack);
        BlackHoleBackpackSavedData.BackpackStorage.ShrinkTransaction transaction =
                storage.prepareShrink(newCapacity);
        if (!transaction.isPrepared() || !transaction.commit()) return;

        List<EntityItem> spawned = new ArrayList<>();
        boolean failed = false;
        for (ItemStack overflow : transaction.getDrops()) {
            EntityItem dropped = player.dropItem(overflow, false);
            if (dropped == null) {
                failed = true;
                break;
            }
            dropped.setPickupDelay(40);
            spawned.add(dropped);
        }
        if (failed) {
            for (EntityItem dropped : spawned) dropped.setDead();
            transaction.rollback();
        }
        BackpackVirtualStorage.updateSummary(backpack, storage);
    }

    private void dropPocketHoleOverflow(ItemPocketHoleBackpack pocketHole, ItemStack backpack, int newCapacity) {
        PocketHoleBackpackSavedData.PocketHoleStorage storage =
                pocketHole.getServerStorage(player.world, backpack);
        PocketHoleBackpackSavedData.PocketHoleStorage.ShrinkTransaction transaction =
                storage.prepareShrink(newCapacity);
        if (!transaction.isPrepared() || !transaction.commit()) return;

        List<EntityItem> spawned = new ArrayList<>();
        boolean failed = false;
        for (ItemStack overflow : transaction.getDrops()) {
            EntityItem dropped = player.dropItem(overflow, false);
            if (dropped == null) {
                failed = true;
                break;
            }
            dropped.setPickupDelay(40);
            spawned.add(dropped);
        }
        if (failed) {
            for (EntityItem dropped : spawned) dropped.setDead();
            transaction.rollback();
        }
        pocketHole.setCachedSummary(backpack, storage);
    }

    private void returnWorkbenchContents() {
        for (int slot = 0; slot < craftMatrix.getSizeInventory(); slot++) {
            ItemStack stack = craftMatrix.removeStackFromSlot(slot);
            if (!stack.isEmpty()) player.dropItem(stack, false);
        }
        craftResult.setInventorySlotContents(0, ItemStack.EMPTY);
    }

    private void invalidateVirtualCountSync(int displaySlot) {
        if (!player.world.isRemote) {
            if (usesServerAuthoritativeBlackHolePages()) {
                blackHolePageNeedsFullSync = true;
            } else if (displaySlot >= 0 && displaySlot < lastSyncedTrueCounts.length) {
                lastSyncedTrueCounts[displaySlot] = -1L;
            }
        }
    }

    private void requestBlackHoleFullPageSync() {
        if (!player.world.isRemote && usesServerAuthoritativeBlackHolePages()) {
            blackHolePageNeedsFullSync = true;
        }
    }

    private void syncVirtualSlotCache(int slotId, int displaySlot) {
        if (usesServerAuthoritativeBlackHolePages()
                && displaySlot >= 0 && slotId >= 0 && slotId < inventoryItemStacks.size()) {
            ItemStack current = contents.getStackInSlot(displaySlot);
            inventoryItemStacks.set(slotId, current.isEmpty() ? ItemStack.EMPTY : current.copy());
        }
    }

    private ItemStack clickVirtualContentSlot(int slotId, int mouseButton, EntityPlayer player) {
        if (mouseButton != 0 && mouseButton != 1) return ItemStack.EMPTY;

        int displaySlot = slotId - contentStart;
        Slot slot = inventorySlots.get(slotId);
        ItemStack snapshot = getVirtualTransactionSnapshot(displaySlot);
        ItemStack cursor = player.inventory.getItemStack();

        if (!cursor.isEmpty()) {
            if (ItemBackpack.isForbiddenBackpackContent(cursor)) return snapshot;
            ItemStack offered = cursor.copy();
            if (mouseButton == 1) offered.setCount(1);
            ItemStack remaining = contents.insertItem(displaySlot, offered, false);
            int accepted = offered.getCount() - (remaining.isEmpty() ? 0 : remaining.getCount());
            if (accepted > 0) {
                cursor.shrink(accepted);
                player.inventory.setItemStack(cursor.isEmpty() ? ItemStack.EMPTY : cursor);
                slot.onSlotChanged();
                syncVirtualSlotCache(slotId, displaySlot);
            }
            return snapshot;
        }

        if (snapshot.isEmpty()) return snapshot;
        int amount = mouseButton == 0 ? snapshot.getCount() : (snapshot.getCount() + 1) / 2;
        ItemStack extracted = contents.extractItem(displaySlot, amount, false);
        if (!extracted.isEmpty()) {
            player.inventory.setItemStack(extracted);
            slot.onSlotChanged();
            syncVirtualSlotCache(slotId, displaySlot);
        }
        return snapshot;
    }

    private ItemStack swapVirtualContentWithHotbar(int slotId, int hotbarTarget, EntityPlayer player) {
        if (hotbarTarget < 0 || hotbarTarget >= 9) return ItemStack.EMPTY;
        int displaySlot = slotId - contentStart;
        ItemStack hotbarStack = player.inventory.getStackInSlot(hotbarTarget);
        ItemStack snapshot = getVirtualTransactionSnapshot(displaySlot);

        if (!hotbarStack.isEmpty()) {
            if (ItemBackpack.isForbiddenBackpackContent(hotbarStack)) return ItemStack.EMPTY;
            ItemStack offered = hotbarStack.copy();
            ItemStack remaining = contents.insertItem(displaySlot, offered, false);
            int accepted = offered.getCount() - (remaining.isEmpty() ? 0 : remaining.getCount());
            if (accepted > 0) {
                hotbarStack.shrink(accepted);
                player.inventory.setInventorySlotContents(hotbarTarget, hotbarStack.isEmpty() ? ItemStack.EMPTY : hotbarStack);
                inventorySlots.get(slotId).onSlotChanged();
                syncVirtualSlotCache(slotId, displaySlot);
            }
            return ItemStack.EMPTY;
        }

        if (snapshot.isEmpty()) return ItemStack.EMPTY;
        ItemStack extracted = contents.extractItem(displaySlot, snapshot.getCount(), false);
        if (!extracted.isEmpty()) {
            player.inventory.setInventorySlotContents(hotbarTarget, extracted);
            inventorySlots.get(slotId).onSlotChanged();
            syncVirtualSlotCache(slotId, displaySlot);
        }
        return ItemStack.EMPTY;
    }

    private ItemStack throwFromVirtualContentSlot(int slotId, int throwMode, EntityPlayer player) {
        int displaySlot = slotId - contentStart;
        ItemStack snapshot = getVirtualTransactionSnapshot(displaySlot);
        if (!player.inventory.getItemStack().isEmpty() || snapshot.isEmpty()) return ItemStack.EMPTY;

        int amount = throwMode == 0 ? 1 : snapshot.getCount();
        ItemStack extracted = contents.extractItem(displaySlot, amount, false);
        if (!extracted.isEmpty()) {
            player.dropItem(extracted, true);
            inventorySlots.get(slotId).onSlotChanged();
            syncVirtualSlotCache(slotId, displaySlot);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canDragIntoSlot(Slot slot) {
        return slot != null && isSlotActive(slot.slotNumber) && super.canDragIntoSlot(slot);
    }

    @Override
    public boolean canMergeSlot(ItemStack stack, Slot slot) {
        return slot != null && isSlotActive(slot.slotNumber) && super.canMergeSlot(stack, slot);
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        ItemStack stack = getBackpackStack();
        if (stack.getItem() instanceof ItemBlackBoxBackpack blackBox && !blackBox.canAccess(stack, player)) {
            return false;
        }
        return heldHand == null || heldInventory != null && heldInventory.isCurrentBackpack();
    }

    @Override
    public void onContainerClosed(EntityPlayer player) {
        super.onContainerClosed(player);
        if (!player.world.isRemote) {
            clearContainer(player, player.world, craftMatrix);
        }
        if (equippedView && !player.world.isRemote && player instanceof EntityPlayerMP) {
            BackpackHandler.syncToClient((EntityPlayerMP) player);
        }
    }

    @Override
    public boolean enchantItem(EntityPlayer player, int id) {
        if (player.world.isRemote) return false;
        if (id == SCROLL_UP) {
            contents.scrollRows(-1);
            if (!player.world.isRemote && usesServerAuthoritativeBlackHolePages()) {
                blackHoleScrollAcknowledgement = true;
            }
            requestBlackHoleFullPageSync();
        } else if (id == SCROLL_DOWN) {
            contents.scrollRows(1);
            if (!player.world.isRemote && usesServerAuthoritativeBlackHolePages()) {
                blackHoleScrollAcknowledgement = true;
            }
            requestBlackHoleFullPageSync();
        } else if (id == TOGGLE_AUTO_PICKUP && supportsAutoPickup()) {
            contents.setAutoPickupEnabled(!contents.isAutoPickupEnabled());
        } else if (id == TOGGLE_AUTO_SORT && supportsAutoSorting()) {
            contents.setAutoSortEnabled(!contents.isAutoSortEnabled());
            contents.clampScrollRow();
            if (!player.world.isRemote && usesServerAuthoritativeBlackHolePages()) {
                blackHoleScrollAcknowledgement = true;
                requestBlackHoleFullPageSync();
            }
        } else if (id == SORT_CONTENTS && contents.supportsManualSorting()) {
            if (!player.world.isRemote) {
                contents.sortContents();
                if (usesServerAuthoritativeBlackHolePages()) {
                    blackHoleScrollAcknowledgement = true;
                    requestBlackHoleFullPageSync();
                }
            }
            contents.clampScrollRow();
        } else if (id == TOGGLE_SMUGGLER_COMPARTMENT && canAccessSmugglerHiddenCompartment()) {
            hiddenSmugglerCompartment = !hiddenSmugglerCompartment;
            contents.resetScroll();
            layoutContentSlots();
            if (!player.world.isRemote) {
                // The client must change its backend slot offset before the
                // following detectAndSendChanges publishes compartment items.
                sendBackpackStateBeforeSlotSync(true);
            }
        } else if (id == TOGGLE_WORKBENCH && supportsWorkbench()) {
            workbenchView = !workbenchView;
            if (workbenchView) upgradeDrawerOpen = true;
            ItemBackpack.setWorkbenchPanelOpen(getBackpackStack(), workbenchView);
            layoutModeSlots();
        } else if (id == TOGGLE_UPGRADE_DRAWER && upgradeSlotCount > 0) {
            if (isWorkbenchView()) {
                workbenchView = false;
                ItemBackpack.setWorkbenchPanelOpen(getBackpackStack(), false);
            }
            upgradeDrawerOpen = !upgradeDrawerOpen;
            layoutModeSlots();
        } else {
            return false;
        }
        layoutModeSlots();
        detectAndSendChanges();
        return true;
    }

    private static final class BackpackContentSlot extends SlotItemHandler {
        private final IItemHandlerModifiable inventory;
        private final int handlerIndex;
        private final BooleanSupplier virtualCounts;
        private final BooleanSupplier quickCraft;
        private final BooleanSupplier clientSide;
        private ItemStack workingStack;

        private BackpackContentSlot(IItemHandlerModifiable inventory, int index, int xPosition, int yPosition,
                                    BooleanSupplier virtualCounts, BooleanSupplier quickCraft,
                                    BooleanSupplier clientSide) {
            super(inventory, index, xPosition, yPosition);
            this.inventory = inventory;
            this.handlerIndex = index;
            this.virtualCounts = virtualCounts;
            this.quickCraft = quickCraft;
            this.clientSide = clientSide;
        }

        /**
         * Vanilla container code mutates the stack returned by getStack()
         * directly in several click paths. A Forge item handler is only
         * authoritative after setStackInSlot(), so keep an explicit working
         * copy and commit it from onSlotChanged().
         */
        @Override
        public ItemStack getStack() {
            ItemStack stack = super.getStack();
            if (virtualCounts.getAsBoolean()) {
                // QUICK_CRAFT distributes additions. The count-one stack in a
                // virtual slot is only a display proxy; exposing it here makes
                // vanilla reserve one of the 64 carried items for that proxy
                // (64 + proxy 1 is clamped to 64), producing 127 + cursor 1.
                if (quickCraft.getAsBoolean()) return ItemStack.EMPTY;
                return stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
            }
            workingStack = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
            return workingStack;
        }

        /**
         * Answered straight from the handler's predicate. Forge's default
         * implementation probes by writing EMPTY into the slot and restoring it
         * afterwards, which destroys contents in backends whose slot holds more
         * than one vanilla stack.
         */
        @Override
        public boolean isItemValid(ItemStack stack) {
            return !stack.isEmpty()
                    && !ItemBackpack.isForbiddenBackpackContent(stack)
                    && inventory.isItemValid(handlerIndex, stack);
        }

        /**
         * Simulated insert instead of Forge's clear-and-restore probe, for the
         * same reason. Reporting the room beyond the displayed stack also lets a
         * slot that already shows a full stack keep accepting items.
         */
        @Override
        public int getItemStackLimit(ItemStack stack) {
            if (virtualCounts.getAsBoolean()) return Integer.MAX_VALUE;
            int maxInput = stack.getMaxStackSize();
            ItemStack probe = stack.copy();
            probe.setCount(maxInput);
            int accepted = maxInput - inventory.insertItem(handlerIndex, probe, true).getCount();
            return inventory.getStackInSlot(handlerIndex).getCount() + accepted;
        }

        @Override
        public void putStack(ItemStack stack) {
            if (virtualCounts.getAsBoolean()) {
                workingStack = null;
                if (clientSide.getAsBoolean()) {
                    super.putStack(stack);
                    return;
                }
                if (!quickCraft.getAsBoolean() || stack.isEmpty() || !isItemValid(stack)) return;

                ItemStack current = inventory.getStackInSlot(handlerIndex);
                if (!current.isEmpty() && (!ItemStack.areItemsEqual(current, stack)
                        || !ItemStack.areItemStackTagsEqual(current, stack))) return;

                // getStack() deliberately hides the display proxy during
                // QUICK_CRAFT, so vanilla passes the number to add rather than
                // a replacement physical stack size.
                int amount = stack.getCount();
                if (amount <= 0) return;

                ItemStack addition = stack.copy();
                addition.setCount(amount);
                inventory.insertItem(handlerIndex, addition, false);
                onSlotChanged();
                return;
            }
            if (stack.isEmpty() || isItemValid(stack)) {
                workingStack = null;
                super.putStack(stack);
            }
        }

        /**
         * The vanilla PICKUP paths withdraw items straight from the backend via
         * decrStackSize() -> extractItem() and never mutate the stack returned by
         * getStack(). Drop the cached working copy here so a following
         * onSlotChanged() cannot re-commit the stale pre-withdrawal count back
         * into storage, which would duplicate the extracted items.
         */
        @Override
        public ItemStack decrStackSize(int amount) {
            workingStack = null;
            return super.decrStackSize(amount);
        }

        @Override
        public void onSlotChanged() {
            if (virtualCounts.getAsBoolean()) {
                workingStack = null;
                super.onSlotChanged();
                return;
            }
            if (workingStack != null) {
                inventory.setStackInSlot(handlerIndex,
                        workingStack.isEmpty() ? ItemStack.EMPTY : workingStack.copy());
                workingStack = null;
            }
            super.onSlotChanged();
        }
    }

    private final class PagedBackpackItemHandler implements IItemHandlerModifiable {
        private final IBackpackInventory inventory;
        private final IntSupplier capacity;
        private int scrollRow;

        private PagedBackpackItemHandler(IBackpackInventory inventory, IntSupplier capacity) {
            this.inventory = inventory;
            this.capacity = capacity;
        }

        private int getCapacity() {
            return ContainerBackpack.this.getCapacity();
        }

        private int getBackendCapacity() {
            return Math.max(0, capacity.getAsInt());
        }

        /**
         * Non-mutating: clamping the stored value here would let a capacity
         * change shift slot indices while a click is still being resolved.
         */
        private int getScrollRow() {
            return Math.max(0, Math.min(scrollRow, getMaxScrollRows()));
        }

        private int getMaxScrollRows() {
            long rows = ((long) getCapacity() + columns - 1L) / columns;
            return (int) Math.max(0L, Math.min(Integer.MAX_VALUE, rows - visibleRows));
        }

        private void scrollRows(int amount) {
            scrollRow = Math.max(0, Math.min(getScrollRow() + amount, getMaxScrollRows()));
        }

        private void resetScroll() {
            scrollRow = 0;
        }

        private void clampScrollRow() {
            scrollRow = getScrollRow();
        }

        private void setScrollRowFromServer(int row) {
            scrollRow = Math.max(0, Math.min(row, getMaxScrollRows()));
        }

        private void applyClientBlackHolePageSlot(int pageRow, int displaySlot, ItemStack prototype, long count) {
            if (inventory == null || pageRow < 0 || displaySlot < 0 || displaySlot >= VISIBLE_SLOTS) return;
            long backendSlot = (long) pageRow * columns + displaySlot;
            if (backendSlot < 0L || backendSlot > Integer.MAX_VALUE) return;

            int slot = (int) backendSlot;
            ItemStack normalized = prototype == null || prototype.isEmpty()
                    ? ItemStack.EMPTY : prototype.copy();
            if (!normalized.isEmpty()) normalized.setCount(1);
            inventory.applyClientSyncedVirtualSlot(
                    slot, normalized, normalized.isEmpty() ? 0L : Math.max(0L, count));
        }

        private void resetClientBlackHoleStorage(int synchronizedCapacity) {
            if (inventory != null) inventory.resetClientStorageMirror(synchronizedCapacity);
        }

        private int getInventorySlot(int displaySlot) {
            return getCompartmentOffset() + getRelativeSlot(displaySlot);
        }

        private int getRelativeSlot(int displaySlot) {
            return getScrollRow() * columns + displaySlot;
        }

        private int getCompartmentOffset() {
            ItemStack stack = getBackpackStack();
            return isHiddenSmugglerCompartment() && stack.getItem() instanceof ItemSmugglerBackpack smuggler
                    ? smuggler.getVisibleSlotCount(stack) : 0;
        }

        private boolean isAvailable(int displaySlot) {
            return displaySlot >= 0 && displaySlot < columns * visibleRows
                    && getRelativeSlot(displaySlot) < getCapacity();
        }

        private int getFilledSlotCount() {
            if (inventory == null) return 0;
            if (!supportsSmugglerCompartments()) return inventory.getFilledSlotCount();

            int filled = 0;
            int start = getCompartmentOffset();
            int end = Math.min(inventory.getSlots(), start + getCapacity());
            for (int slot = start; slot < end; slot++) {
                if (!inventory.getStackInSlot(slot).isEmpty()) {
                    filled++;
                }
            }
            return filled;
        }

        private ItemStack insertItemAnywhere(ItemStack stack, boolean simulate) {
            if (ItemBackpack.isForbiddenBackpackContent(stack)) return stack;
            if (inventory == null) return stack;
            if (!supportsSmugglerCompartments()) return inventory.insertItemAnywhere(stack, simulate);

            int start = getCompartmentOffset();
            int end = Math.min(inventory.getSlots(), start + getCapacity());
            return insertIntoRange(stack, simulate, start, end);
        }

        private ItemStack insertCraftingResult(ItemStack stack, boolean simulate) {
            if (ItemBackpack.isForbiddenBackpackContent(stack)) return stack;
            if (inventory == null) return stack;
            if (!supportsSmugglerCompartments()) return inventory.insertItemAnywhere(stack, simulate);
            ItemStack backpack = getBackpackStack();
            int visibleSlots = backpack.getItem() instanceof ItemSmugglerBackpack smuggler
                    ? smuggler.getVisibleSlotCount(backpack) : 0;
            return insertIntoRange(stack, simulate, 0, Math.min(inventory.getSlots(), visibleSlots));
        }

        private ItemStack insertIntoRange(ItemStack stack, boolean simulate, int start, int end) {
            ItemStack remaining = stack;
            for (int pass = 0; pass < 2 && !remaining.isEmpty(); pass++) {
                for (int slot = start; slot < end && !remaining.isEmpty(); slot++) {
                    boolean empty = inventory.getStackInSlot(slot).isEmpty();
                    if ((pass == 0 && empty) || (pass == 1 && !empty)) continue;
                    remaining = inventory.insertItem(slot, remaining, simulate);
                }
            }
            return remaining;
        }

        private boolean supportsAutoPickup() {
            return inventory != null && inventory.supportsAutoPickup();
        }

        private boolean isAutoPickupEnabled() {
            return inventory != null && inventory.isAutoPickupEnabled();
        }

        private void setAutoPickupEnabled(boolean enabled) {
            if (inventory != null) inventory.setAutoPickupEnabled(enabled);
        }

        private boolean supportsAutoSorting() {
            return inventory != null && inventory.supportsAutoSorting();
        }

        private boolean supportsManualSorting() {
            return inventory != null;
        }

        private boolean isAutoSortEnabled() {
            return inventory != null && inventory.isAutoSortEnabled();
        }

        private void setAutoSortEnabled(boolean enabled) {
            if (inventory != null) inventory.setAutoSortEnabled(enabled);
        }

        private void sortContents() {
            if (inventory == null) return;
            if (supportsSmugglerCompartments()) {
                int start = getCompartmentOffset();
                int end = Math.min(inventory.getSlots(), start + getCapacity());
                inventory.sortContents(start, end);
            } else {
                inventory.sortContents();
            }
        }

        private boolean isInfiniteStorage() {
            return inventory != null && inventory.isInfiniteStorage();
        }

        private boolean usesVirtualLongCounts() {
            return inventory != null && inventory.usesVirtualLongCounts();
        }

        private long getTrueSlotCount(int displaySlot) {
            return isAvailable(displaySlot) && inventory != null
                    ? inventory.getTrueSlotCount(getInventorySlot(displaySlot)) : 0L;
        }

        private void setClientTrueSlotCount(int displaySlot, long count) {
            if (isAvailable(displaySlot) && inventory != null) {
                inventory.setClientTrueSlotCount(getInventorySlot(displaySlot), count);
            }
        }

        @Override
        public int getSlots() {
            return VISIBLE_SLOTS;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return isAvailable(slot) && inventory != null ? inventory.getStackInSlot(getInventorySlot(slot)) : ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (ItemBackpack.isForbiddenBackpackContent(stack)) return stack;
            return isAvailable(slot) && inventory != null ? inventory.insertItem(getInventorySlot(slot), stack, simulate) : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return isAvailable(slot) && inventory != null ? inventory.extractItem(getInventorySlot(slot), amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return isAvailable(slot) && inventory != null ? inventory.getSlotLimit(getInventorySlot(slot)) : 0;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return !ItemBackpack.isForbiddenBackpackContent(stack)
                    && isAvailable(slot) && inventory != null && inventory.isItemValid(getInventorySlot(slot), stack);
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            if ((stack.isEmpty() || !ItemBackpack.isForbiddenBackpackContent(stack)) && isAvailable(slot) && inventory != null) {
                inventory.setStackInSlot(getInventorySlot(slot), stack);
            }
        }
    }

    private static class HeldBackpackItemHandler implements IBackpackInventory {
        private final EntityPlayer player;
        private final EnumHand hand;
        private final ItemBackpack expectedItem;
        private final UUID expectedInstanceId;
        private ItemStack resolvedStack = ItemStack.EMPTY;
        private IBackpackInventory resolvedInventory;

        private HeldBackpackItemHandler(EntityPlayer player, EnumHand hand) {
            this.player = player;
            this.hand = hand;
            ItemStack backpack = player.getHeldItem(hand);
            this.expectedItem = backpack.getItem() instanceof ItemBackpack item ? item : null;
            this.expectedInstanceId = ItemBackpack.getInstanceId(backpack);
        }

        private boolean isCurrentBackpack() {
            ItemStack current = player.getHeldItem(hand);
            if (expectedItem == null || current.getItem() != expectedItem) return false;

            UUID currentId = ItemBackpack.getInstanceId(current);
            if (expectedInstanceId != null) {
                return expectedInstanceId.equals(currentId);
            }
            return player.world.isRemote;
        }

        private IBackpackInventory resolveInventory() {
            if (!isCurrentBackpack()) {
                resolvedStack = ItemStack.EMPTY;
                resolvedInventory = null;
                return null;
            }

            ItemStack current = player.getHeldItem(hand);
            int expectedCapacity = expectedItem.getStorageSlots(current);
            if (current != resolvedStack || resolvedInventory != null && !resolvedInventory.isInfiniteStorage()
                    && resolvedInventory.getCapacity() != expectedCapacity) {
                if (resolvedInventory == null || !resolvedInventory.tryRebindClientBackpack(current)) {
                    resolvedInventory = expectedItem.createInventory(current, player.world);
                }
                resolvedStack = current;
            }
            return resolvedInventory;
        }

        @Override
        public int getCapacity() {
            IBackpackInventory inventory = resolveInventory();
            return inventory == null ? 0 : inventory.getCapacity();
        }

        @Override
        public int getFilledSlotCount() {
            IBackpackInventory inventory = resolveInventory();
            return inventory == null ? 0 : inventory.getFilledSlotCount();
        }

        @Override
        public ItemStack insertItemAnywhere(ItemStack stack, boolean simulate) {
            if (ItemBackpack.isForbiddenBackpackContent(stack)) return stack;
            IBackpackInventory inventory = resolveInventory();
            return inventory == null ? stack : inventory.insertItemAnywhere(stack, simulate);
        }

        @Override
        public boolean supportsAutoPickup() {
            IBackpackInventory inventory = resolveInventory();
            return inventory != null && inventory.supportsAutoPickup();
        }

        @Override
        public boolean isAutoPickupEnabled() {
            IBackpackInventory inventory = resolveInventory();
            return inventory != null && inventory.isAutoPickupEnabled();
        }

        @Override
        public void setAutoPickupEnabled(boolean enabled) {
            IBackpackInventory inventory = resolveInventory();
            if (inventory != null) inventory.setAutoPickupEnabled(enabled);
        }

        @Override
        public boolean supportsAutoSorting() {
            IBackpackInventory inventory = resolveInventory();
            return inventory != null && inventory.supportsAutoSorting();
        }

        @Override
        public boolean isAutoSortEnabled() {
            IBackpackInventory inventory = resolveInventory();
            return inventory != null && inventory.isAutoSortEnabled();
        }

        @Override
        public void setAutoSortEnabled(boolean enabled) {
            IBackpackInventory inventory = resolveInventory();
            if (inventory != null) inventory.setAutoSortEnabled(enabled);
        }

        @Override
        public boolean supportsManualSorting() {
            IBackpackInventory inventory = resolveInventory();
            return inventory != null && inventory.supportsManualSorting();
        }

        @Override
        public void sortContents() {
            IBackpackInventory inventory = resolveInventory();
            if (inventory != null) inventory.sortContents();
        }

        @Override
        public void sortContents(int fromInclusive, int toExclusive) {
            IBackpackInventory inventory = resolveInventory();
            if (inventory != null) inventory.sortContents(fromInclusive, toExclusive);
        }

        @Override
        public boolean isInfiniteStorage() {
            IBackpackInventory inventory = resolveInventory();
            return inventory != null && inventory.isInfiniteStorage();
        }

        @Override
        public long getTrueSlotCount(int slot) {
            IBackpackInventory inventory = resolveInventory();
            return inventory != null && isAvailable(slot) ? inventory.getTrueSlotCount(slot) : 0L;
        }

        @Override
        public void setClientTrueSlotCount(int slot, long count) {
            IBackpackInventory inventory = resolveInventory();
            if (inventory != null && isAvailable(slot)) inventory.setClientTrueSlotCount(slot, count);
        }

        @Override
        public void resetClientStorageMirror(int capacity) {
            IBackpackInventory inventory = resolveInventory();
            if (inventory != null) inventory.resetClientStorageMirror(capacity);
        }

        @Override
        public void applyClientSyncedVirtualSlot(int slot, ItemStack prototype, long count) {
            IBackpackInventory inventory = resolveInventory();
            if (inventory != null && isAvailable(slot)) {
                inventory.applyClientSyncedVirtualSlot(slot, prototype, count);
            }
        }

        @Override
        public boolean usesVirtualLongCounts() {
            IBackpackInventory inventory = resolveInventory();
            return inventory != null && inventory.usesVirtualLongCounts();
        }

        @Override
        public int getSlots() {
            IBackpackInventory inventory = resolveInventory();
            return inventory == null ? 0 : inventory.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            IBackpackInventory inventory = resolveInventory();
            return isAvailable(slot) ? inventory.getStackInSlot(slot) : ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (ItemBackpack.isForbiddenBackpackContent(stack)) return stack;
            IBackpackInventory inventory = resolveInventory();
            return isAvailable(slot) ? inventory.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            IBackpackInventory inventory = resolveInventory();
            return isAvailable(slot) ? inventory.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            IBackpackInventory inventory = resolveInventory();
            return isAvailable(slot) ? inventory.getSlotLimit(slot) : 0;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            IBackpackInventory inventory = resolveInventory();
            return !ItemBackpack.isForbiddenBackpackContent(stack)
                    && isAvailable(slot) && inventory.isItemValid(slot, stack);
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            IBackpackInventory inventory = resolveInventory();
            if ((stack.isEmpty() || !ItemBackpack.isForbiddenBackpackContent(stack)) && isAvailable(slot)) {
                inventory.setStackInSlot(slot, stack);
            }
        }

        private boolean isAvailable(int slot) {
            IBackpackInventory inventory = resolveInventory();
            return inventory != null && slot >= 0 && (slot < inventory.getSlots() || inventory.isInfiniteStorage());
        }
    }

    private static class Layout {
        private final int columns;
        private final int visibleRows;
        private final int guiWidth;
        private final int contentX;
        private final int playerPanelX;

        private Layout(int baseCapacity, int capacity, boolean ownerManaged) {
            columns = columnsForCapacity(baseCapacity);
            long rows = ((long) Math.max(1, capacity) + columns - 1L) / columns;
            visibleRows = (int) Math.max(1L, Math.min(MAX_VISIBLE_ROWS, rows));
            int storageWidth = Math.max(176, columns * 18 + 16);
            guiWidth = storageWidth;
            contentX = (guiWidth - columns * 18) / 2;
            playerPanelX = (guiWidth - 176) / 2;
        }
    }
}
