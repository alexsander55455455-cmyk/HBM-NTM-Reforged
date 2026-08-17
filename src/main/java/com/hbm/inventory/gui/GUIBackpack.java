package com.hbm.inventory.gui;

import com.hbm.handler.HbmKeybinds;
import com.hbm.inventory.container.ContainerBackpack;
import com.hbm.items.tool.ItemBlackBoxBackpack;
import com.hbm.items.tool.ItemRealityErrorBackpack;
import com.hbm.items.tool.ItemSmugglerBackpack;
import com.hbm.lib.Library;
import com.hbm.packet.PacketDispatcher;
import com.hbm.packet.toserver.BlackBoxAccessPacket;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.config.GuiUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class GUIBackpack extends GuiContainer {
    private static final ResourceLocation INVENTORY_TEXTURE = new ResourceLocation("minecraft", "textures/gui/container/inventory.png");
    private static final ResourceLocation BACKPACK_SLOT_ICON = new ResourceLocation("hbm", "textures/gui/backpack_slot_icon.png");
    private static final int HIDDEN_SLOT_POSITION = -10000;
    private static final int BLACK_HOLE_TEXT_COLOR = 0xFF55FF;
    private static final int BLACK_HOLE_SEARCH_HINT_COLOR = 0x882288;
    private static final int POCKET_HOLE_TEXT_COLOR = 0x55FFFF;
    private static final int POCKET_HOLE_DETAIL_COLOR = 0x22DDEB;
    private static final int POCKET_HOLE_SEARCH_HINT_COLOR = 0x187E93;
    private static final int BLACK_BOX_MODE_BUTTON = 100;
    private static final int BLACK_BOX_ADD_BUTTON = 101;
    private static final int BLACK_BOX_PREVIOUS_PAGE = 102;
    private static final int BLACK_BOX_NEXT_PAGE = 103;
    private static final int OWNER_RESET_BUTTON = 104;
    private static final int BLACK_BOX_REMOVE_BASE = 200;
    private static final int BLACK_BOX_VISIBLE_PLAYERS = 4;
    private static final int OWNER_SETTINGS_HEIGHT = 210;

    private final ContainerBackpack backpack;
    private final List<Slot> searchResults = new ArrayList<>();
    private GuiButton autoPickup;
    private GuiButton autoSort;
    private GuiButton manualSort;
    private GuiButton workbench;
    private GuiButton upgradeDrawer;
    private GuiButton smugglerCompartment;
    private GuiTextField searchField;
    private GuiTextField blackBoxPlayerField;
    private GuiButton blackBoxMode;
    private GuiButton blackBoxAdd;
    private GuiButton blackBoxPreviousPage;
    private GuiButton blackBoxNextPage;
    private GuiButton ownerReset;
    private final List<GuiButton> blackBoxRemoveButtons = new ArrayList<>();
    private int blackBoxAccessPage;
    private boolean ownerSettingsPage;
    private int ownerTextY;
    private int ownerAllowedLabelY;
    private int ownerListY;
    private int ownerPagerY;
    private int searchX;
    private int searchY;
    private int searchWidth;

    public GUIBackpack(ContainerBackpack backpack) {
        super(backpack);
        this.backpack = backpack;
        this.xSize = backpack.getGuiWidth();
        this.ySize = backpack.getGuiHeight();
    }

    @Override
    public void initGui() {
        String previousSearch = searchField == null ? "" : searchField.getText();
        if (ownerSettingsPage && !backpack.isBackpackOwner()) ownerSettingsPage = false;
        backpack.refreshLayout();
        this.xSize = backpack.getGuiWidth();
        this.ySize = ownerSettingsPage ? OWNER_SETTINGS_HEIGHT : backpack.getGuiHeight();
        super.initGui();
        buttonList.clear();
        Keyboard.enableRepeatEvents(true);
        resetControlReferences();
        backpack.setOwnerSettingsView(ownerSettingsPage);
        if (ownerSettingsPage) {
            initOwnerAccessControls();
            return;
        }
        int gridWidth = backpack.getColumns() * 18;
        searchWidth = Math.min(120, gridWidth - 4);
        if (backpack.getMaxScrollRows() > 0) {
            String scrollCount = backpack.isInfiniteStorage() ? "\u221E"
                    : String.valueOf(backpack.getMaxScrollRows() + 1);
            String rowLabel = I18n.format("container.hbm_backpack.scroll",
                    backpack.getScrollRow() + 1, scrollCount);
            searchWidth = Math.min(searchWidth,
                    Math.max(36, gridWidth - fontRenderer.getStringWidth(rowLabel) - 8));
        }
        searchX = guiLeft + backpack.getContentX();
        searchY = guiTop + backpack.getSearchRelativeY();
        searchField = new GuiTextField(0, fontRenderer, searchX, searchY, searchWidth, 12);
        searchField.setTextColor(backpack.isPocketHoleBackpack()
                ? POCKET_HOLE_TEXT_COLOR
                : backpack.isInfiniteStorage() ? BLACK_HOLE_TEXT_COLOR : 0xE0E0E0);
        searchField.setDisabledTextColour(0x777777);
        searchField.setEnableBackgroundDrawing(false);
        searchField.setMaxStringLength(64);
        searchField.setText(previousSearch);
        int automationX = guiLeft + xSize - 106;
        autoPickup = addButton(new GuiButton(
                ContainerBackpack.TOGGLE_AUTO_PICKUP, automationX,
                guiTop + backpack.getAutoPickupButtonY(), 100, 20, ""));
        autoSort = addButton(new GuiButton(
                ContainerBackpack.TOGGLE_AUTO_SORT, automationX,
                guiTop + backpack.getAutoSortButtonY(), 100, 20, ""));
        manualSort = addButton(new GuiButton(
                ContainerBackpack.SORT_CONTENTS, automationX,
                guiTop + backpack.getActionButtonY(), 100, 20, ""));
        workbench = addButton(new ArrowButton(
                ContainerBackpack.TOGGLE_WORKBENCH, guiLeft, guiTop, false));
        upgradeDrawer = addButton(new GuiButton(
                ContainerBackpack.TOGGLE_UPGRADE_DRAWER, guiLeft - 20, guiTop + 6, 18, 20, "<"));
        updateAutomationButtons();
        updateUpgradeDrawerButton();
        int latchX = guiLeft + 6;
        smugglerCompartment = addButton(new GuiButton(
                ContainerBackpack.TOGGLE_SMUGGLER_COMPARTMENT, latchX,
                guiTop + backpack.getActionButtonY(), Math.max(20, automationX - latchX - 2), 20, ""));
        updateSmugglerCompartmentButton();
    }

    private void resetControlReferences() {
        autoPickup = null;
        autoSort = null;
        manualSort = null;
        workbench = null;
        upgradeDrawer = null;
        smugglerCompartment = null;
        searchField = null;
        blackBoxPlayerField = null;
        blackBoxMode = null;
        blackBoxAdd = null;
        blackBoxPreviousPage = null;
        blackBoxNextPage = null;
        ownerReset = null;
        blackBoxRemoveButtons.clear();
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        super.onGuiClosed();
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        backpack.refreshLayout();
        int expectedHeight = ownerSettingsPage ? OWNER_SETTINGS_HEIGHT : backpack.getGuiHeight();
        if (xSize != backpack.getGuiWidth() || ySize != expectedHeight) {
            initGui();
            return;
        }
        if (ownerSettingsPage) backpack.setOwnerSettingsView(true);
        if (searchField != null) searchField.updateCursorCounter();
        if (blackBoxPlayerField != null) blackBoxPlayerField.updateCursorCounter();
        updateAutomationButtons();
        updateUpgradeDrawerButton();
        updateSmugglerCompartmentButton();
        updateOwnerAccessControls();
        applySearchPresentation();
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == ContainerBackpack.SORT_CONTENTS
                && backpack.usesServerAuthoritativeBlackHolePages()) {
            if (backpack.beginBlackHolePageRequest()) {
                mc.playerController.sendEnchantPacket(inventorySlots.windowId, button.id);
            }
        } else if (button.id == ContainerBackpack.TOGGLE_AUTO_PICKUP
                || button.id == ContainerBackpack.TOGGLE_AUTO_SORT
                || button.id == ContainerBackpack.SORT_CONTENTS
                || button.id == ContainerBackpack.TOGGLE_WORKBENCH
                || button.id == ContainerBackpack.TOGGLE_UPGRADE_DRAWER) {
            mc.playerController.sendEnchantPacket(inventorySlots.windowId, button.id);
        } else if (button.id == ContainerBackpack.TOGGLE_SMUGGLER_COMPARTMENT) {
            mc.playerController.sendEnchantPacket(inventorySlots.windowId, button.id);
        } else if (button.id == BLACK_BOX_MODE_BUTTON) {
            PacketDispatcher.wrapper.sendToServer(new BlackBoxAccessPacket(BlackBoxAccessPacket.CYCLE_MODE, ""));
        } else if (button.id == BLACK_BOX_ADD_BUTTON) {
            String name = blackBoxPlayerField == null ? "" : blackBoxPlayerField.getText().trim();
            if (!name.isEmpty()) {
                PacketDispatcher.wrapper.sendToServer(new BlackBoxAccessPacket(BlackBoxAccessPacket.ADD_PLAYER, name));
                blackBoxPlayerField.setText("");
            }
        } else if (button.id == BLACK_BOX_PREVIOUS_PAGE) {
            blackBoxAccessPage = Math.max(0, blackBoxAccessPage - 1);
            updateOwnerAccessControls();
        } else if (button.id == BLACK_BOX_NEXT_PAGE) {
            blackBoxAccessPage++;
            updateOwnerAccessControls();
        } else if (button.id >= BLACK_BOX_REMOVE_BASE
                && button.id < BLACK_BOX_REMOVE_BASE + BLACK_BOX_VISIBLE_PLAYERS) {
            AllowedEntry allowed = getVisibleAllowedPlayer(button.id - BLACK_BOX_REMOVE_BASE);
            if (allowed != null) {
                PacketDispatcher.wrapper.sendToServer(new BlackBoxAccessPacket(
                        BlackBoxAccessPacket.REMOVE_PLAYER, allowed.uuid.toString()));
            }
        } else if (button.id == OWNER_RESET_BUTTON) {
            PacketDispatcher.wrapper.sendToServer(new BlackBoxAccessPacket(BlackBoxAccessPacket.RESET_OWNER, ""));
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        if (ownerSettingsPage) return;
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0 || backpack.getMaxScrollRows() == 0) return;

        int mouseX = Mouse.getEventX() * width / mc.displayWidth;
        int mouseY = height - Mouse.getEventY() * height / mc.displayHeight - 1;
        int left = guiLeft + backpack.getContentX();
        int right = left + backpack.getColumns() * 18;
        int top = guiTop + backpack.getContentY();
        int bottom = top + backpack.getVisibleRows() * 18;
        if (mouseX < left || mouseX >= right || mouseY < top || mouseY >= bottom) return;

        int action = wheel > 0 ? ContainerBackpack.SCROLL_UP : ContainerBackpack.SCROLL_DOWN;
        if ((action == ContainerBackpack.SCROLL_UP && backpack.getScrollRow() == 0)
                || (action == ContainerBackpack.SCROLL_DOWN && backpack.getScrollRow() >= backpack.getMaxScrollRows())) return;
        if (backpack.usesServerAuthoritativeBlackHolePages()) {
            if (backpack.beginBlackHolePageRequest()) {
                mc.playerController.sendEnchantPacket(inventorySlots.windowId, action);
            }
            return;
        }
        mc.playerController.sendEnchantPacket(inventorySlots.windowId, action);
    }

    @Override
    public void handleKeyboardInput() throws IOException {
        char eventCharacter = Keyboard.getEventCharacter();
        if ((Keyboard.getEventKey() == 0 && eventCharacter >= ' ') || Keyboard.getEventKeyState()) {
            keyTyped(eventCharacter, Keyboard.getEventKey());
        }
        // Minecraft.runTickKeyboard() already calls dispatchKeypresses() for
        // this event. GuiScreen calls it a second time, which creates two F2
        // screenshots while a backpack screen is open.
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == HbmKeybinds.backpackKey.getKeyCode()
                && (searchField == null || !searchField.isFocused())
                && (blackBoxPlayerField == null || !blackBoxPlayerField.isFocused())) {
            mc.player.closeScreen();
            return;
        }
        if (keyCode == Keyboard.KEY_R
                && backpack.isBackpackOwner()
                && (blackBoxPlayerField == null || !blackBoxPlayerField.isFocused())) {
            ownerSettingsPage = !ownerSettingsPage;
            initGui();
            return;
        }
        if (ownerSettingsPage) {
            if (blackBoxPlayerField != null && blackBoxPlayerField.textboxKeyTyped(typedChar, keyCode)) {
                return;
            }
            super.keyTyped(typedChar, keyCode);
            return;
        }
        if (searchField != null && searchField.textboxKeyTyped(typedChar, keyCode)) {
            applySearchPresentation();
            return;
        }
        if (blackBoxPlayerField != null && blackBoxPlayerField.textboxKeyTyped(typedChar, keyCode)) {
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (ownerSettingsPage) {
            if (blackBoxPlayerField != null) blackBoxPlayerField.mouseClicked(mouseX, mouseY, mouseButton);
            super.mouseClicked(mouseX, mouseY, mouseButton);
            return;
        }
        if (searchField != null) searchField.mouseClicked(mouseX, mouseY, mouseButton);
        if (blackBoxPlayerField != null) blackBoxPlayerField.mouseClicked(mouseX, mouseY, mouseButton);
        applySearchPresentation();
        if (isSearchResultGap(mouseX, mouseY)) return;
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void handleMouseClick(Slot slot, int slotId, int mouseButton, ClickType type) {
        if (ownerSettingsPage) return;
        if (slotId >= 0 && !backpack.isSlotActive(slotId)) return;
        int contentStart = backpack.getContentStart();
        boolean virtualCounts = backpack.usesVirtualLongCounts();
        boolean blackHolePages = backpack.usesServerAuthoritativeBlackHolePages();
        boolean contentSlot = slotId >= contentStart
                && slotId < contentStart + ContainerBackpack.VISIBLE_SLOTS;

        if (virtualCounts) {
            if (type == ClickType.QUICK_CRAFT) {
                int dragEvent = mouseButton & 3;
                if (dragEvent == 0
                        && (backpack.isVirtualClickPending()
                        || blackHolePages && backpack.isBlackHolePagePending())) {
                    return;
                }
                if (dragEvent == 2) {
                    if (backpack.isVirtualClickPending()
                            || blackHolePages && backpack.isBlackHolePagePending()) {
                        return;
                    }
                    if (blackHolePages && !backpack.beginBlackHolePageRequest()) return;
                    if (!backpack.beginVirtualClickRequest()) return;
                }
            } else {
                if (backpack.isVirtualClickPending()
                        || blackHolePages && backpack.isBlackHolePagePending()) {
                    return;
                }
                if (blackHolePages && contentSlot && !backpack.beginBlackHolePageRequest()) return;
                if (!backpack.beginVirtualClickRequest()) return;
            }
        }
        super.handleMouseClick(slot, slotId, mouseButton, type);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (!ownerSettingsPage) {
            renderHoveredToolTip(mouseX, mouseY);
        } else if (ownerReset != null
                && mouseX >= ownerReset.x && mouseX < ownerReset.x + ownerReset.width
                && mouseY >= ownerReset.y && mouseY < ownerReset.y + ownerReset.height) {
            drawHoveringText(I18n.format("container.hbm_backpack.reset_owner"), mouseX, mouseY);
        }
    }

    @Override
    protected void renderToolTip(ItemStack stack, int mouseX, int mouseY) {
        boolean realityError = isRealityErrorContentHovered();
        long exactVirtualCount = getHoveredVirtualCount();
        if (!realityError && exactVirtualCount <= 0L) {
            super.renderToolTip(stack, mouseX, mouseY);
            return;
        }

        FontRenderer itemFont = stack.getItem().getFontRenderer(stack);
        GuiUtils.preItemToolTip(stack);
        try {
            List<String> tooltip = getItemToolTip(stack);
            if (realityError && !tooltip.isEmpty()) {
                for (int line = 0; line < tooltip.size(); line++) {
                    tooltip.set(line, ItemRealityErrorBackpack.glitchText(tooltip.get(line)));
                }
            }
            if (exactVirtualCount > 0L) {
                tooltip.add((backpack.isPocketHoleBackpack() ? TextFormatting.AQUA : TextFormatting.LIGHT_PURPLE)
                        + I18n.format(
                        "container.hbm_backpack.exact_count",
                        TextFormatting.WHITE + Long.toString(exactVirtualCount)));
            }
            drawHoveringText(tooltip, mouseX, mouseY, itemFont == null ? fontRenderer : itemFont);
        } finally {
            GuiUtils.postItemToolTip();
        }
    }

    private boolean isRealityErrorContentHovered() {
        Slot hovered = getSlotUnderMouse();
        return backpack.isRealityErrorBackpack()
                && hovered != null
                && hovered == inventorySlots.getSlot(backpack.getContentStart());
    }

    private long getHoveredVirtualCount() {
        if (!backpack.usesVirtualLongCounts()) return -1L;
        Slot hovered = getSlotUnderMouse();
        if (hovered == null) return -1L;
        int displaySlot = hovered.slotNumber - backpack.getContentStart();
        return backpack.getTrueSlotCount(displaySlot);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        if (ownerSettingsPage) {
            drawOwnerAccessText();
            return;
        }
        boolean infinite = backpack.isInfiniteStorage();
        boolean pocketHole = backpack.isPocketHoleBackpack();
        boolean realityError = backpack.isRealityErrorBackpack();
        int mainTextColor = pocketHole ? POCKET_HOLE_TEXT_COLOR
                : infinite ? BLACK_HOLE_TEXT_COLOR : 0xE0E0E0;
        int detailTextColor = pocketHole ? POCKET_HOLE_DETAIL_COLOR
                : infinite ? BLACK_HOLE_TEXT_COLOR : 0x9A9A9A;
        if (backpack.isWorkbenchView()) {
            String workbenchTitle = fontRenderer.trimStringToWidth(
                    I18n.format("container.hbm_backpack.workbench"), ContainerBackpack.WORKBENCH_PANEL_WIDTH - 8);
            fontRenderer.drawString(glitchIfNeeded(workbenchTitle, realityError),
                    ContainerBackpack.WORKBENCH_PANEL_LEFT + 4, 8, mainTextColor);
        }
        int titleX = backpack.isEquippedView() ? 30 : backpack.getContentX();
        String title = fitHeaderText(backpack.getBackpackName(), titleX, 6, realityError);
        fontRenderer.drawString(title, titleX, 6, mainTextColor);
        String capacity = infinite ? "\u221E" : String.valueOf(backpack.getCapacity());
        String capacityText = fitHeaderText(
                I18n.format("container.hbm_backpack.capacity", capacity), titleX, 20, realityError);
        fontRenderer.drawString(capacityText, titleX, 20, detailTextColor);

        int filled = backpack.getFilledSlotCount();
        float percent = infinite || backpack.getCapacity() == 0
                ? 0F
                : Library.roundFloat(filled * 100F / backpack.getCapacity(), 1);
        TextFormatting filledColor = pocketHole ? TextFormatting.AQUA
                : infinite ? TextFormatting.LIGHT_PURPLE
                : percent >= 75F ? TextFormatting.RED : percent < 25F ? TextFormatting.GREEN : TextFormatting.YELLOW;
        TextFormatting capacityColor = pocketHole ? TextFormatting.DARK_AQUA
                : infinite ? TextFormatting.LIGHT_PURPLE
                : percent >= 75F ? TextFormatting.DARK_RED : percent < 25F ? TextFormatting.DARK_GREEN : TextFormatting.GOLD;
        String filledText = I18n.format("container.hbm_backpack.filled", filledColor + String.valueOf(filled), capacityColor + capacity);
        if (!infinite) {
            String filledWithPercent = filledText + " " + filledColor + "(" + percent + "%)";
            int availableWidth = getAvailableHeaderWidth(titleX, 31);
            filledText = fontRenderer.getStringWidth(glitchIfNeeded(filledWithPercent, realityError)) <= availableWidth
                    ? filledWithPercent : filledText;
        }
        filledText = fitHeaderText(filledText, titleX, 31, realityError);
        fontRenderer.drawString(filledText, titleX, 31, detailTextColor);

        if (backpack.getMaxScrollRows() > 0) {
            String scrollCount = infinite ? "\u221E" : String.valueOf(backpack.getMaxScrollRows() + 1);
            String scroll = I18n.format("container.hbm_backpack.scroll", backpack.getScrollRow() + 1, scrollCount);
            int scrollY = backpack.getSearchRelativeY() + 3;
            String displayedScroll = glitchIfNeeded(scroll, realityError);
            fontRenderer.drawString(displayedScroll,
                    xSize - 8 - fontRenderer.getStringWidth(displayedScroll), scrollY, detailTextColor);
        }
        String inventoryLabel = glitchIfNeeded(I18n.format("container.inventory"), realityError);
        int playerHeaderX = backpack.getPlayerPanelX() + 8;
        int playerHeaderY = backpack.getPlayerPanelY() + 5;
        fontRenderer.drawString(inventoryLabel, playerHeaderX, playerHeaderY, mainTextColor);
        if (backpack.isBackpackOwner()) {
            int hintWidth = 160 - fontRenderer.getStringWidth(inventoryLabel) - 6;
            String hint = fontRenderer.trimStringToWidth(
                    I18n.format("container.hbm_backpack.owner_settings_hint"), hintWidth);
            fontRenderer.drawString(
                    hint,
                    backpack.getPlayerPanelX() + 168 - fontRenderer.getStringWidth(hint),
                    playerHeaderY,
                    0x909090);
        }
        drawTrueSlotCounts();
    }

    private String glitchIfNeeded(String text, boolean realityError) {
        return realityError ? ItemRealityErrorBackpack.glitchText(text) : text;
    }

    private String fitHeaderText(String text, int textX, int textY, boolean realityError) {
        String displayed = glitchIfNeeded(text, realityError);
        return fontRenderer.trimStringToWidth(displayed, getAvailableHeaderWidth(textX, textY));
    }

    /**
     * Slots that hold more items than a vanilla stack can express draw their real
     * count here. The stack itself is capped for the network, so the vanilla
     * number would understate the contents.
     */
    private void drawTrueSlotCounts() {
        int contentStart = backpack.getContentStart();
        for (int displaySlot = 0; displaySlot < ContainerBackpack.VISIBLE_SLOTS; displaySlot++) {
            Slot slot = inventorySlots.getSlot(contentStart + displaySlot);
            if (slot.xPos <= HIDDEN_SLOT_POSITION || slot.getStack().isEmpty()) continue;

            long trueCount = backpack.getTrueSlotCount(displaySlot);
            if (trueCount <= 1L) continue;

            String label = formatCompactCount(trueCount);
            int labelWidth = fontRenderer.getStringWidth(label);
            float scale = labelWidth <= 16 ? 1F : 16F / labelWidth;
            GlStateManager.pushMatrix();
            GlStateManager.translate(slot.xPos + 17F, slot.yPos + 9F, 300F);
            GlStateManager.scale(scale, scale, 1F);
            GlStateManager.disableDepth();
            fontRenderer.drawStringWithShadow(label, -labelWidth, 0F, 0xFFFFFF);
            GlStateManager.enableDepth();
            GlStateManager.popMatrix();
        }
    }

    private static String formatCompactCount(long count) {
        if (count < 1000L) return String.valueOf(count);
        String[] suffixes = {"", "K", "M", "B", "T", "Q", "E"};
        double value = count;
        int suffix = 0;
        while (value >= 999.5D && suffix < suffixes.length - 1) {
            value /= 1000D;
            suffix++;
        }
        String number = String.format(Locale.ROOT, value < 100D ? "%.1f" : "%.0f", value);
        if (number.endsWith(".0")) number = number.substring(0, number.length() - 2);
        return number + suffixes[suffix];
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        if (ownerSettingsPage) {
            drawDefaultBackground();
            GlStateManager.color(1F, 1F, 1F, 1F);
            drawPanel(guiLeft, guiTop, guiLeft + xSize, guiTop + ySize);
            drawOwnerPlayerField();
            GlStateManager.color(1F, 1F, 1F, 1F);
            GlStateManager.enableTexture2D();
            return;
        }
        applySearchPresentation();
        drawDefaultBackground();
        GlStateManager.color(1F, 1F, 1F, 1F);
        if (backpack.isUpgradeDrawerOpen()) {
            drawPanel(guiLeft - ContainerBackpack.UPGRADE_DRAWER_WIDTH, guiTop + 3,
                    guiLeft, guiTop + backpack.getUpgradeDrawerBottom());
        }
        if (backpack.isWorkbenchView()) {
            drawPanel(guiLeft + ContainerBackpack.WORKBENCH_PANEL_LEFT,
                    guiTop + ContainerBackpack.WORKBENCH_PANEL_TOP,
                    guiLeft + ContainerBackpack.WORKBENCH_PANEL_RIGHT,
                    guiTop + ContainerBackpack.WORKBENCH_PANEL_BOTTOM);
        }
        drawPanel(guiLeft, guiTop, guiLeft + xSize, guiTop + backpack.getBackpackPaneHeight());
        drawPanel(guiLeft + backpack.getPlayerPanelX(), guiTop + backpack.getPlayerPanelY(),
                guiLeft + backpack.getPlayerPanelX() + 176, guiTop + ySize);

        if (backpack.isEquippedView()) {
            drawSlot(guiLeft + 8, guiTop + 18);
            if (!inventorySlots.getSlot(0).getHasStack()) {
                drawBackpackSlotIcon(guiLeft + 8, guiTop + 18);
            }
        }

        if (hasSearchQuery()) {
            for (Slot slot : searchResults) {
                drawSlot(guiLeft + slot.xPos, guiTop + slot.yPos);
            }
        } else {
            int visibleSlots = backpack.getVisibleSlotCount();
            for (int slot = 0; slot < visibleSlots; slot++) {
                int row = slot / backpack.getColumns();
                int column = slot % backpack.getColumns();
                drawSlot(guiLeft + backpack.getContentX() + column * 18, guiTop + backpack.getContentY() + row * 18);
            }
        }

        if (backpack.isWorkbenchView()) {
            for (int slot = 0; slot < 10; slot++) {
                Slot crafting = inventorySlots.getSlot(backpack.getCraftStart() + slot);
                if (crafting.xPos > HIDDEN_SLOT_POSITION) {
                    drawSlot(guiLeft + crafting.xPos, guiTop + crafting.yPos);
                }
            }
        }

        if (backpack.isUpgradeDrawerOpen()) {
            for (int slot = 0; slot < backpack.getUpgradeSlotCount(); slot++) {
                Slot upgrade = inventorySlots.getSlot(backpack.getUpgradeStart() + slot);
                drawSlot(guiLeft + upgrade.xPos, guiTop + upgrade.yPos);
            }
        }

        int playerX = guiLeft + backpack.getPlayerPanelX() + 8;
        int playerY = guiTop + backpack.getPlayerY();
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                drawSlot(playerX + column * 18, playerY + row * 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            drawSlot(playerX + column * 18, playerY + 58);
        }

        drawScrollbar();
        drawSearchBox();
        GlStateManager.color(1F, 1F, 1F, 1F);
        GlStateManager.enableTexture2D();
    }

    private void initOwnerAccessControls() {
        if (!backpack.isBackpackOwner()) return;
        boolean blackBox = backpack.isBlackBoxBackpack();
        int x = guiLeft + 8;
        int innerWidth = xSize - 16;
        ownerReset = addButton(new GuiButton(
                OWNER_RESET_BUTTON, guiLeft + xSize - 26, guiTop + 5, 20, 20, "x"));
        if (blackBox) {
            blackBoxMode = addButton(new GuiButton(
                    BLACK_BOX_MODE_BUTTON, x, guiTop + 28, innerWidth, 20, ""));
            ownerTextY = 53;
            ownerAllowedLabelY = 65;
            blackBoxPlayerField = new GuiTextField(
                    1, fontRenderer, x, guiTop + 76, innerWidth - 34, 14);
            ownerListY = 100;
        } else {
            ownerTextY = 31;
            ownerAllowedLabelY = 48;
            blackBoxPlayerField = new GuiTextField(
                    1, fontRenderer, x, guiTop + 59, innerWidth - 34, 14);
            ownerListY = 83;
        }
        blackBoxPlayerField.setMaxStringLength(16);
        blackBoxAdd = addButton(new GuiButton(
                BLACK_BOX_ADD_BUTTON,
                x + innerWidth - 32, blackBoxPlayerField.y - 3, 32, 20, "+"));
        for (int row = 0; row < BLACK_BOX_VISIBLE_PLAYERS; row++) {
            blackBoxRemoveButtons.add(addButton(new GuiButton(
                    BLACK_BOX_REMOVE_BASE + row,
                    x + innerWidth - 20, guiTop + ownerListY + row * 17, 20, 14, "x")));
        }
        ownerPagerY = ownerListY + BLACK_BOX_VISIBLE_PLAYERS * 17 + 1;
        blackBoxPreviousPage = addButton(new GuiButton(
                BLACK_BOX_PREVIOUS_PAGE, x, guiTop + ownerPagerY, 24, 20, "<"));
        blackBoxNextPage = addButton(new GuiButton(
                BLACK_BOX_NEXT_PAGE, x + innerWidth - 24, guiTop + ownerPagerY, 24, 20, ">"));
        updateOwnerAccessControls();
    }

    private void updateOwnerAccessControls() {
        if (!ownerSettingsPage || blackBoxPreviousPage == null) return;
        ItemStack stack = backpack.getBackpackStack();
        List<AllowedEntry> allowed = getAllowedPlayers();
        int pages = Math.max(1, (allowed.size() + BLACK_BOX_VISIBLE_PLAYERS - 1) / BLACK_BOX_VISIBLE_PLAYERS);
        blackBoxAccessPage = Math.max(0, Math.min(blackBoxAccessPage, pages - 1));
        if (blackBoxMode != null && stack.getItem() instanceof ItemBlackBoxBackpack blackBox) {
            blackBoxMode.displayString = I18n.format(
                    "container.hbm_backpack.black_box.mode."
                            + blackBox.getAccessMode(stack).name().toLowerCase(Locale.ROOT));
        }
        blackBoxPreviousPage.enabled = blackBoxAccessPage > 0;
        blackBoxNextPage.enabled = blackBoxAccessPage + 1 < pages;
        for (int row = 0; row < blackBoxRemoveButtons.size(); row++) {
            GuiButton remove = blackBoxRemoveButtons.get(row);
            remove.visible = getVisibleAllowedPlayer(row) != null;
            remove.enabled = remove.visible;
        }
    }

    private AllowedEntry getVisibleAllowedPlayer(int row) {
        List<AllowedEntry> allowed = getAllowedPlayers();
        int index = blackBoxAccessPage * BLACK_BOX_VISIBLE_PLAYERS + row;
        return index >= 0 && index < allowed.size() ? allowed.get(index) : null;
    }

    private List<AllowedEntry> getAllowedPlayers() {
        ItemStack stack = backpack.getBackpackStack();
        List<AllowedEntry> result = new ArrayList<>();
        if (stack.getItem() instanceof ItemBlackBoxBackpack blackBox) {
            for (ItemBlackBoxBackpack.AllowedPlayer player : blackBox.getAllowedPlayers(stack)) {
                result.add(new AllowedEntry(player.uuid, player.name));
            }
        } else if (stack.getItem() instanceof ItemSmugglerBackpack smuggler) {
            for (ItemSmugglerBackpack.AllowedPlayer player : smuggler.getAllowedPlayers(stack)) {
                result.add(new AllowedEntry(player.uuid, player.name));
            }
        }
        return result;
    }

    private void drawOwnerAccessText() {
        ItemStack stack = backpack.getBackpackStack();
        int x = 8;
        String title = fontRenderer.trimStringToWidth(
                I18n.format("container.hbm_backpack.owner_settings"), xSize - 44);
        fontRenderer.drawString(title, x, 8, 0xE0E0E0);
        String ownerName = "";
        if (stack.getItem() instanceof ItemBlackBoxBackpack blackBox) {
            ownerName = blackBox.getOwnerName(stack);
        } else if (stack.getItem() instanceof ItemSmugglerBackpack smuggler) {
            ownerName = smuggler.getOwnerName(stack);
        }
        fontRenderer.drawString(
                I18n.format("container.hbm_backpack.black_box.owner", ownerName),
                x, ownerTextY, 0xB0B0B0);
        fontRenderer.drawString(
                I18n.format("container.hbm_backpack.black_box.add_player"),
                x, ownerAllowedLabelY, 0xB0B0B0);
        for (int row = 0; row < BLACK_BOX_VISIBLE_PLAYERS; row++) {
            AllowedEntry allowed = getVisibleAllowedPlayer(row);
            if (allowed == null) continue;
            String name = fontRenderer.trimStringToWidth(
                    allowed.name == null || allowed.name.isEmpty() ? allowed.uuid.toString() : allowed.name,
                    xSize - 40);
            fontRenderer.drawString(name, x, ownerListY + 3 + row * 17, 0xE0E0E0);
        }
        List<AllowedEntry> allowed = getAllowedPlayers();
        int pages = Math.max(1, (allowed.size() + BLACK_BOX_VISIBLE_PLAYERS - 1) / BLACK_BOX_VISIBLE_PLAYERS);
        String page = (blackBoxAccessPage + 1) + "/" + pages;
        fontRenderer.drawString(
                page, xSize / 2 - fontRenderer.getStringWidth(page) / 2, ownerPagerY + 6, 0x888888);
        String backHint = fontRenderer.trimStringToWidth(
                I18n.format("container.hbm_backpack.owner_settings_back"), xSize - 16);
        fontRenderer.drawString(backHint, x, ySize - 13, 0x909090);
    }

    private void drawOwnerPlayerField() {
        if (blackBoxPlayerField == null) return;
        int x = blackBoxPlayerField.x;
        int y = blackBoxPlayerField.y;
        drawRect(x - 1, y - 1, x + blackBoxPlayerField.width + 1, y + blackBoxPlayerField.height + 1, 0xFF777777);
        drawRect(x, y, x + blackBoxPlayerField.width, y + blackBoxPlayerField.height, 0xFF101010);
        blackBoxPlayerField.drawTextBox();
    }

    private void updateAutomationButtons() {
        String theme = backpack.isPocketHoleBackpack()
                ? TextFormatting.AQUA.toString()
                : backpack.isInfiniteStorage() ? TextFormatting.LIGHT_PURPLE.toString() : "";
        if (autoPickup != null) {
            autoPickup.y = guiTop + backpack.getAutoPickupButtonY();
            autoPickup.visible = backpack.supportsAutoPickup();
            autoPickup.enabled = autoPickup.visible;
            autoPickup.displayString = theme + I18n.format(backpack.isAutoPickupEnabled()
                    ? "container.hbm_backpack.magnet_on"
                    : "container.hbm_backpack.magnet_off");
        }
        if (autoSort != null) {
            autoSort.y = guiTop + backpack.getAutoSortButtonY();
            autoSort.visible = backpack.supportsAutoSorting();
            autoSort.enabled = autoSort.visible && !backpack.isBlackHolePagePending();
            autoSort.displayString = theme + I18n.format(backpack.isAutoSortEnabled()
                    ? "container.hbm_backpack.sort_on"
                    : "container.hbm_backpack.sort_off");
        }
        if (manualSort != null) {
            manualSort.y = guiTop + backpack.getActionButtonY();
            manualSort.visible = backpack.supportsManualSorting();
            manualSort.enabled = manualSort.visible && !backpack.isBlackHolePagePending();
            String text = theme + I18n.format("container.hbm_backpack.sort_now");
            manualSort.displayString = glitchIfNeeded(text, backpack.isRealityErrorBackpack());
        }
        if (workbench != null) {
            boolean workbenchView = backpack.isWorkbenchView();
            workbench.visible = backpack.supportsWorkbench()
                    && (workbenchView || backpack.isUpgradeDrawerOpen());
            workbench.enabled = workbench.visible;
            workbench.x = guiLeft + backpack.getWorkbenchButtonX();
            workbench.y = guiTop + backpack.getWorkbenchButtonY();
            ((ArrowButton) workbench).setOpen(workbenchView);
        }
    }

    private void updateUpgradeDrawerButton() {
        if (upgradeDrawer == null) return;
        boolean visible = backpack.getUpgradeSlotCount() > 0 && !backpack.isWorkbenchView();
        boolean open = backpack.isUpgradeDrawerOpen();
        upgradeDrawer.visible = visible;
        upgradeDrawer.enabled = visible;
        upgradeDrawer.x = guiLeft - (open ? ContainerBackpack.UPGRADE_DRAWER_WIDTH + 20 : 20);
        upgradeDrawer.y = guiTop + 6;
        upgradeDrawer.displayString = open ? ">" : "<";
    }

    private void updateSmugglerCompartmentButton() {
        if (smugglerCompartment != null) {
            smugglerCompartment.y = guiTop + backpack.getActionButtonY();
            boolean canAccessHidden = backpack.canAccessSmugglerHiddenCompartment();
            smugglerCompartment.visible = canAccessHidden;
            smugglerCompartment.enabled = smugglerCompartment.visible;
            smugglerCompartment.displayString = I18n.format(backpack.isHiddenSmugglerCompartment()
                    ? "container.hbm_backpack.smuggler_hidden"
                    : "container.hbm_backpack.smuggler_main");
        }
    }

    private void drawScrollbar() {
        int maxScroll = backpack.getMaxScrollRows();
        if (maxScroll == 0) return;

        int x = guiLeft + backpack.getContentX() + backpack.getColumns() * 18 + 3;
        int y = guiTop + backpack.getContentY();
        int trackHeight = backpack.getVisibleRows() * 18;
        int totalRows = maxScroll + backpack.getVisibleRows();
        int thumbHeight = Math.max(12, trackHeight * backpack.getVisibleRows() / totalRows);
        int thumbY = y + (int) ((long) (trackHeight - thumbHeight) * backpack.getScrollRow() / maxScroll);
        drawRect(x, y, x + 3, y + trackHeight, 0xFF101010);
        drawRect(x, thumbY, x + 3, thumbY + thumbHeight, 0xFF9A9A9A);
    }

    private void drawPanel(int left, int top, int right, int bottom) {
        drawRect(left, top, right, bottom, 0xE0101010);
        drawRect(left + 2, top + 2, right - 2, bottom - 2, 0xE02E2E2E);
    }

    private void drawSlot(int x, int y) {
        GlStateManager.color(1F, 1F, 1F, 1F);
        GlStateManager.enableTexture2D();
        mc.getTextureManager().bindTexture(INVENTORY_TEXTURE);
        Gui.drawModalRectWithCustomSizedTexture(x - 1, y - 1, 7, 83, 18, 18, 256, 256);
        drawRect(x - 1, y - 1, x + 17, y + 17, 0x44000000);
        GlStateManager.color(1F, 1F, 1F, 1F);
        GlStateManager.enableTexture2D();
    }

    private void drawBackpackSlotIcon(int x, int y) {
        GlStateManager.color(1F, 1F, 1F, 1F);
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        mc.getTextureManager().bindTexture(BACKPACK_SLOT_ICON);
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 0, 16, 16, 16, 16);
        GlStateManager.color(1F, 1F, 1F, 1F);
    }

    private void drawSearchBox() {
        if (searchField == null) return;
        int border = searchField.isFocused() ? 0xFF9A9A9A : 0xFF111111;
        drawRect(searchX - 1, searchY - 1, searchX + searchWidth + 1, searchY + 13, border);
        drawRect(searchX, searchY, searchX + searchWidth, searchY + 12, 0xE0101010);
        GlStateManager.pushMatrix();
        GlStateManager.translate(0F, 2F, 0F);
        searchField.drawTextBox();
        GlStateManager.popMatrix();
        if (!searchField.isFocused() && searchField.getText().isEmpty()) {
            fontRenderer.drawString(glitchIfNeeded(
                            I18n.format("container.hbm_backpack.search"), backpack.isRealityErrorBackpack()),
                    searchX + 3, searchY + 4,
                    backpack.isPocketHoleBackpack() ? POCKET_HOLE_SEARCH_HINT_COLOR
                            : backpack.isInfiniteStorage() ? BLACK_HOLE_SEARCH_HINT_COLOR : 0x777777);
        }
    }

    private boolean hasSearchQuery() {
        return searchField != null && !searchField.getText().trim().isEmpty();
    }

    private boolean isSearchResultGap(int mouseX, int mouseY) {
        if (!hasSearchQuery()) return false;
        int relativeX = mouseX - guiLeft - backpack.getContentX();
        int relativeY = mouseY - guiTop - backpack.getContentY();
        if (relativeX < 0 || relativeY < 0
                || relativeX >= backpack.getColumns() * 18
                || relativeY >= backpack.getVisibleRows() * 18) {
            return false;
        }
        for (Slot slot : searchResults) {
            int left = slot.xPos - backpack.getContentX();
            int top = slot.yPos - backpack.getContentY();
            if (relativeX >= left && relativeX < left + 16 && relativeY >= top && relativeY < top + 16) {
                return false;
            }
        }
        return true;
    }

    /**
     * Repositions only this client's visible slots. The slot object and its index remain unchanged,
     * so a click is still sent to the server for the item that was actually shown.
     */
    private void applySearchPresentation() {
        if (ownerSettingsPage) return;
        int visibleSlots = backpack.getVisibleSlotCount();
        int contentStart = backpack.getContentStart();
        searchResults.clear();

        if (!hasSearchQuery()) {
            for (int displaySlot = 0; displaySlot < ContainerBackpack.VISIBLE_SLOTS; displaySlot++) {
                Slot slot = inventorySlots.getSlot(contentStart + displaySlot);
                if (displaySlot < visibleSlots) {
                    slot.xPos = backpack.getContentX() + displaySlot % backpack.getColumns() * 18;
                    slot.yPos = backpack.getContentY() + displaySlot / backpack.getColumns() * 18;
                } else {
                    slot.xPos = HIDDEN_SLOT_POSITION;
                    slot.yPos = HIDDEN_SLOT_POSITION;
                }
            }
            return;
        }

        List<SearchMatch> matches = new ArrayList<>();
        for (int displaySlot = 0; displaySlot < visibleSlots; displaySlot++) {
            Slot slot = inventorySlots.getSlot(contentStart + displaySlot);
            int score = getSearchScore(slot.getStack());
            if (score >= 0) {
                matches.add(new SearchMatch(slot, displaySlot, score));
            }
        }
        matches.sort(Comparator.comparingInt((SearchMatch match) -> match.score)
                .thenComparingInt(match -> match.originalPosition));

        for (int displaySlot = 0; displaySlot < ContainerBackpack.VISIBLE_SLOTS; displaySlot++) {
            Slot slot = inventorySlots.getSlot(contentStart + displaySlot);
            slot.xPos = HIDDEN_SLOT_POSITION;
            slot.yPos = HIDDEN_SLOT_POSITION;
        }
        for (int presentationSlot = 0; presentationSlot < matches.size(); presentationSlot++) {
            Slot slot = matches.get(presentationSlot).slot;
            slot.xPos = backpack.getContentX() + presentationSlot % backpack.getColumns() * 18;
            slot.yPos = backpack.getContentY() + presentationSlot / backpack.getColumns() * 18;
            searchResults.add(slot);
        }
    }

    private int getSearchScore(ItemStack stack) {
        if (stack.isEmpty() || !hasSearchQuery()) return -1;

        String query = searchField.getText().trim().toLowerCase(Locale.ROOT);
        String name = TextFormatting.getTextWithoutFormattingCodes(stack.getDisplayName()).toLowerCase(Locale.ROOT);
        if (name.equals(query)) return 0;
        if (name.startsWith(query)) return 100;
        int nameIndex = name.indexOf(query);

        if (stack.getItem().getRegistryName() == null) return nameIndex >= 0 ? 300 + nameIndex : -1;
        String registry = stack.getItem().getRegistryName().toString().toLowerCase(Locale.ROOT);
        if (registry.equals(query)) return 200;
        if (registry.startsWith(query)) return 250;
        if (nameIndex >= 0) return 300 + nameIndex;
        int registryIndex = registry.indexOf(query);
        return registryIndex >= 0 ? 400 + registryIndex : -1;
    }

    private static final class SearchMatch {
        private final Slot slot;
        private final int originalPosition;
        private final int score;

        private SearchMatch(Slot slot, int originalPosition, int score) {
            this.slot = slot;
            this.originalPosition = originalPosition;
            this.score = score;
        }
    }

    private int getAvailableHeaderWidth(int textX, int textY) {
        int right = xSize - 8;
        GuiButton[] controls = {autoPickup, autoSort, manualSort, smugglerCompartment};
        for (GuiButton control : controls) {
            if (control == null || !control.visible) continue;
            int top = control.y - guiTop;
            if (textY < top + control.height && textY + fontRenderer.FONT_HEIGHT > top) {
                right = Math.min(right, control.x - guiLeft - 4);
            }
        }
        return Math.max(0, right - textX);
    }

    private static final class ArrowButton extends GuiButton {
        private boolean open;

        private ArrowButton(int id, int x, int y, boolean open) {
            super(id, x, y, ContainerBackpack.WORKBENCH_ARROW_WIDTH,
                    ContainerBackpack.WORKBENCH_ARROW_HEIGHT, "");
            this.open = open;
        }

        private void setOpen(boolean open) {
            this.open = open;
        }

        @Override
        public void drawButton(Minecraft minecraft, int mouseX, int mouseY, float partialTicks) {
            super.drawButton(minecraft, mouseX, mouseY, partialTicks);
            if (!visible) return;
            Gui.drawRect(x, y, x + width, y + 1, 0xFF111111);
            Gui.drawRect(x, y + height - 1, x + width, y + height, 0xFF111111);
            Gui.drawRect(x, y + 1, x + 1, y + height - 1, 0xFF111111);
            Gui.drawRect(x + width - 1, y + 1, x + width, y + height - 1, 0xFF111111);

            boolean isHovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;

            if (isHovered) {
                // Top 1px inner highlight (blue)
                Gui.drawRect(x + 1, y + 1, x + width - 1, y + 2, 0xFF8A9AE8);
                // Left 1px inner highlight (blue)
                Gui.drawRect(x + 1, y + 1, x + 2, y + height - 1, 0xFF8A9AE8);

                // Bottom 1px inner shadow (dark blue)
                Gui.drawRect(x + 1, y + height - 2, x + width - 1, y + height - 1, 0xFF4A5996);
                // Right 1px inner shadow (dark blue)
                Gui.drawRect(x + width - 2, y + 1, x + width - 1, y + height - 1, 0xFF4A5996);

                // Top-right and bottom-left corner pixels: smooth blue transition (matching Screenshot 2)
                Gui.drawRect(x + width - 2, y + 1, x + width - 1, y + 2, 0xFF6A79B8);
                Gui.drawRect(x + 1, y + height - 2, x + 2, y + height - 1, 0xFF6A79B8);
            } else {
                // Top 1px inner highlight (light grey)
                Gui.drawRect(x + 1, y + 1, x + width - 1, y + 2, 0xFFC6C6C6);
                // Left 1px inner highlight (light grey)
                Gui.drawRect(x + 1, y + 1, x + 2, y + height - 1, 0xFFC6C6C6);

                // Bottom 1px inner shadow (dark grey)
                Gui.drawRect(x + 1, y + height - 2, x + width - 1, y + height - 1, 0xFF555555);
                // Right 1px inner shadow (dark grey)
                Gui.drawRect(x + width - 2, y + 1, x + width - 1, y + height - 1, 0xFF555555);

                // Top-right and bottom-left corner pixels: soft smooth transition (matching Screenshot 2)
                Gui.drawRect(x + width - 2, y + 1, x + width - 1, y + 2, 0xFF888888);
                Gui.drawRect(x + 1, y + height - 2, x + 2, y + height - 1, 0xFF888888);
            }

            int color = enabled ? (isHovered ? 0xFFFFFFE0 : 0xFFE0E0E0) : 0xFF777777;
            int centerX = x + width / 2;
            int centerY = y + height / 2;
            Gui.drawRect(centerX - 3, centerY, centerX + 4, centerY + 1, color);
            if (open) {
                Gui.drawRect(centerX + 3, centerY, centerX + 4, centerY + 1, color);
                Gui.drawRect(centerX + 2, centerY - 1, centerX + 3, centerY + 2, color);
                Gui.drawRect(centerX + 1, centerY - 2, centerX + 2, centerY + 3, color);
            } else {
                Gui.drawRect(centerX - 3, centerY, centerX - 2, centerY + 1, color);
                Gui.drawRect(centerX - 2, centerY - 1, centerX - 1, centerY + 2, color);
                Gui.drawRect(centerX - 1, centerY - 2, centerX, centerY + 3, color);
            }
        }
    }

    private static final class AllowedEntry {
        private final java.util.UUID uuid;
        private final String name;

        private AllowedEntry(java.util.UUID uuid, String name) {
            this.uuid = uuid;
            this.name = name;
        }
    }
}
