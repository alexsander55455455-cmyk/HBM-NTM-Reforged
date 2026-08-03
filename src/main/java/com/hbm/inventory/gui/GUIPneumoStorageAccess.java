package com.hbm.inventory.gui;

import com.hbm.Tags;
import com.hbm.inventory.container.ContainerPneumoStorageAccess;
import com.hbm.inventory.container.ContainerPneumoStorageAccess.SlotPneumo;
import com.hbm.tileentity.network.TileEntityPneumoStorageAccess;
import com.hbm.util.BobMathUtil;
import com.hbm.util.I18nUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.Slot;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.IOException;

public class GUIPneumoStorageAccess extends GuiInfoContainer {

    private static final ResourceLocation TEXTURE = new ResourceLocation(Tags.MODID, "textures/gui/storage/gui_pneumatic_access.png");
    private static int sorting;
    private static boolean startFocused;

    private final TileEntityPneumoStorageAccess access;
    private final ContainerPneumoStorageAccess container;
    private GuiTextField search;
    private int scrollIndex;
    private int scrollBounds = 1;
    private boolean wasClicking;
    private boolean draggingScroll;
    private boolean wasMouseInGui;

    public GUIPneumoStorageAccess(InventoryPlayer inventory, TileEntityPneumoStorageAccess access) {
        super(new ContainerPneumoStorageAccess(inventory, access));
        this.container = (ContainerPneumoStorageAccess) inventorySlots;
        this.access = access;
        this.xSize = 210;
        this.ySize = 251;
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);
        search = new GuiTextField(0, fontRenderer, guiLeft + 79, guiTop + 127, 86, 12);
        search.setTextColor(0xFFFFFF);
        search.setDisabledTextColour(0xA0A0A0);
        search.setEnableBackgroundDrawing(false);
        search.setMaxStringLength(50);
        search.setText("");
        search.setFocused(startFocused);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        wasMouseInGui = checkClick(mouseX, mouseY, 0, 0, xSize, ySize);
        scrollBounds = Math.max(1, (int) Math.ceil(container.getStackCount() / 8D - 6D));
        setScroll(MathHelper.clamp(scrollIndex, 0, scrollBounds));

        boolean clicking = Mouse.isButtonDown(0);
        if (!clicking) draggingScroll = false;
        if (!wasClicking && clicking && mouseX >= guiLeft + 187 && mouseX < guiLeft + 201
                && mouseY > guiTop + 16 && mouseY <= guiTop + 124) draggingScroll = true;
        if (draggingScroll) {
            double fraction = MathHelper.clamp(mouseY - guiTop - 24, 0, 92) / 92D;
            setScroll((int) Math.round(scrollBounds * fraction));
        }
        wasClicking = clicking;

        super.drawScreen(mouseX, mouseY, partialTicks);
        drawCustomInfoStat(mouseX, mouseY, guiLeft + 7, guiTop + 7, 18, 18, mouseX, mouseY,
                new String[] { "Sorting: " + TextFormatting.YELLOW + "Amount" });
        drawCustomInfoStat(mouseX, mouseY, guiLeft + 7, guiTop + 25, 18, 18, mouseX, mouseY,
                new String[] { "Sorting: " + TextFormatting.YELLOW + "Item ID" });
        drawCustomInfoStat(mouseX, mouseY, guiLeft + 7, guiTop + 43, 18, 18, mouseX, mouseY,
                new String[] { "Sorting: " + TextFormatting.YELLOW + "Name" });
        drawCustomInfoStat(mouseX, mouseY, guiLeft + 7, guiTop + 61, 18, 18, mouseX, mouseY,
                new String[] { "Sorting: " + TextFormatting.YELLOW + "Internal name" });
        drawCustomInfoStat(mouseX, mouseY, guiLeft + 7, guiTop + 79, 18, 18, mouseX, mouseY,
                new String[] { "Focus search by default: " + (startFocused ? TextFormatting.GREEN + "ON" : TextFormatting.RED + "OFF") });
        drawCustomInfoStat(mouseX, mouseY, guiLeft + 7, guiTop + 97, 18, 18, mouseX, mouseY,
                new String[] { "Include internal names in search: " + (container.detailedSearch ? TextFormatting.GREEN + "ON" : TextFormatting.RED + "OFF") });
        renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (checkClick(mouseX, mouseY, 7, 7, 18, 18)) setSort(0);
        if (checkClick(mouseX, mouseY, 7, 25, 18, 18)) setSort(1);
        if (checkClick(mouseX, mouseY, 7, 43, 18, 18)) setSort(2);
        if (checkClick(mouseX, mouseY, 7, 61, 18, 18)) setSort(3);
        if (checkClick(mouseX, mouseY, 7, 79, 18, 18)) { playClick(); startFocused = !startFocused; }
        if (checkClick(mouseX, mouseY, 7, 97, 18, 18)) {
            playClick();
            container.detailedSearch = !container.detailedSearch;
            container.setSearchString(search.getText());
        }
        search.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private void setSort(int value) {
        playClick();
        sorting = value;
        scrollIndex = 0;
        if (value == 0) container.setSorter(ContainerPneumoStorageAccess.SORT_BY_STACK_SIZE);
        if (value == 1) container.setSorter(ContainerPneumoStorageAccess.SORT_BY_ID);
        if (value == 2) container.setSorter(ContainerPneumoStorageAccess.SORT_BY_LOCALIZED);
        if (value == 3) container.setSorter(ContainerPneumoStorageAccess.SORT_BY_INTERNAL);
    }

    private void playClick() {
        mc.getSoundHandler().playSound(PositionedSoundRecord.getRecord(SoundEvents.UI_BUTTON_CLICK, 1F, 1F));
    }

    @Override
    public void handleMouseInput() throws IOException {
        int direction = Mouse.getEventDWheel();
        if (direction != 0 && wasMouseInGui) {
            setScroll(getScroll() - Integer.signum(direction));
            return;
        }
        super.handleMouseInput();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (search.textboxKeyTyped(typedChar, keyCode)) {
            scrollIndex = 0;
            container.setSearchString(search.getText());
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String name = I18nUtil.resolveKey("container.pneumoStorageAccess");
        fontRenderer.drawString(name, 122 - fontRenderer.getStringWidth(name) / 2, 5, 4210752);
        fontRenderer.drawString(I18nUtil.resolveKey("container.inventory"), 42, ySize - 94, 4210752);

        GL11.glPushMatrix();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableDepth();
        GL11.glScaled(0.5D, 0.5D, 1D);
        for (Slot slot : inventorySlots.inventorySlots) {
            if (!(slot instanceof SlotPneumo pneumo) || !pneumo.getHasStack()) continue;
            String label = BobMathUtil.getShortNumber(pneumo.amount);
            int x = (pneumo.xPos + 16) * 2 - fontRenderer.getStringWidth(label);
            int y = (pneumo.yPos + 16) * 2 - fontRenderer.FONT_HEIGHT;
            fontRenderer.drawStringWithShadow(label, x, y, -1);
        }
        GlStateManager.enableDepth();
        GL11.glPopMatrix();
        RenderHelper.enableGUIStandardItemLighting();
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GL11.glColor4f(1F, 1F, 1F, 1F);
        Minecraft.getMinecraft().getTextureManager().bindTexture(TEXTURE);
        drawTexturedModalRect(guiLeft + 34, guiTop, 0, 0, 176, ySize);
        drawTexturedModalRect(guiLeft, guiTop, 176, 15, 32, 122);
        drawTexturedModalRect(guiLeft + 7, guiTop + 7 + sorting * 18, 208, 0, 18, 18);
        if (startFocused) drawTexturedModalRect(guiLeft + 7, guiTop + 79, 208, 18, 18, 18);
        if (container.detailedSearch) drawTexturedModalRect(guiLeft + 7, guiTop + 97, 208, 18, 18, 18);
        drawTexturedModalRect(guiLeft + 34 + 154, guiTop + getScrollBarY(), draggingScroll ? 188 : 176, 0, 12, 15);
        GL11.glPushMatrix();
        GL11.glTranslated(0, 2, 0);
        search.drawTextBox();
        GL11.glPopMatrix();
    }

    private int getScrollBarY() {
        double progress = scrollBounds <= 0 ? 0D : (double) container.listingStart / scrollBounds;
        return 17 + (int) (MathHelper.clamp(progress, 0D, 1D) * 91D);
    }

    private int getScroll() { return MathHelper.clamp(scrollIndex, 0, scrollBounds); }

    private void setScroll(int value) {
        int clamped = MathHelper.clamp(value, 0, scrollBounds);
        if (clamped == scrollIndex && container.listingStart == clamped) return;
        scrollIndex = clamped;
        container.setListingStart(clamped);
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        super.onGuiClosed();
    }
}
