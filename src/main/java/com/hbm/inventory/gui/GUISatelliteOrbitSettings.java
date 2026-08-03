package com.hbm.inventory.gui;

import com.hbm.items.ISatChip;
import com.hbm.packet.PacketDispatcher;
import com.hbm.packet.toserver.SatelliteSettingsPacket;
import com.hbm.saveddata.satellites.OrbitKey;
import com.hbm.saveddata.satellites.OrbitSettings;
import com.hbm.saveddata.satellites.Satellite;
import com.hbm.saveddata.satellites.SatelliteTypeRegistry;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;

public class GUISatelliteOrbitSettings extends GuiScreen {

    private static final int FIELD_WIDTH = 86;
    private final EnumHand hand;
    private final GuiTextField[] fields = new GuiTextField[8];
    private boolean blinking;
    private static final String[] LABEL_KEYS = {
            "gui.satellite_orbit.frequency", "gui.satellite_orbit.inclination",
            "gui.satellite_orbit.altitude", "gui.satellite_orbit.phase",
            "gui.satellite_orbit.red", "gui.satellite_orbit.green",
            "gui.satellite_orbit.blue", "gui.satellite_orbit.blink"
    };

    public GUISatelliteOrbitSettings(EnumHand hand) {
        this.hand = hand;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        buttonList.clear();
        ItemStack stack = mc.player.getHeldItem(hand);
        SatelliteTypeRegistry.Descriptor descriptor = SatelliteTypeRegistry.byItem(stack);
        Satellite sample = descriptor == null ? null : descriptor.create();
        OrbitSettings settings = OrbitSettings.readFromStack(stack, sample);
        blinking = settings.isBlinking();

        int left = width / 2 - 105;
        int top = height / 2 - 104;
        String[] values = {
                Integer.toString(ISatChip.getFreqS(stack)),
                trim(settings.getInclination()),
                trim(settings.getAltitudeKm()),
                trim(settings.getPhase()),
                Integer.toString(Math.round(settings.getRed() * 255F)),
                Integer.toString(Math.round(settings.getGreen() * 255F)),
                Integer.toString(Math.round(settings.getBlue() * 255F)),
                trim(settings.getBlinkSeconds())
        };
        for(int i = 0; i < fields.length; i++) {
            int column = i / 4;
            int row = i % 4;
            fields[i] = new GuiTextField(i, fontRenderer, left + column * 110, top + 28 + row * 31, FIELD_WIDTH, 18);
            fields[i].setText(values[i]);
            fields[i].setMaxStringLength(24);
        }

        buttonList.add(new GuiButton(103, width / 2 - 102, top + 143, 98, 20, blinkLabel()));
        buttonList.add(new GuiButton(100, width / 2 - 102, top + 168, 98, 20,
                I18n.format("gui.satellite_orbit.save")));
        buttonList.add(new GuiButton(101, width / 2 + 4, top + 168, 98, 20,
                I18n.format("gui.satellite_orbit.cancel")));
        buttonList.add(new GuiButton(102, width / 2 - 102, top + 192, 204, 20,
                I18n.format("gui.satellite_orbit.claim")));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if(button.id == 103) {
            blinking = !blinking;
            button.displayString = blinkLabel();
            return;
        }
        if(button.id == 101) {
            mc.displayGuiScreen(null);
            return;
        }
        if(button.id != 100 && button.id != 102) return;

        ItemStack stack = mc.player.getHeldItem(hand);
        SatelliteTypeRegistry.Descriptor descriptor = SatelliteTypeRegistry.byItem(stack);
        if(descriptor == null || !(stack.getItem() instanceof ISatChip)) {
            mc.displayGuiScreen(null);
            return;
        }

        Satellite sample = descriptor.create();
        OrbitSettings current = OrbitSettings.readFromStack(stack, sample);
        NBTTagCompound settings = new NBTTagCompound();
        settings.setFloat("inclination", parseFloat(fields[1], current.getInclination()));
        settings.setFloat("altitudeKm", parseFloat(fields[2], current.getAltitudeKm()));
        settings.setFloat("phase", parseFloat(fields[3], current.getPhase()));
        settings.setFloat("red", parseColor(fields[4], current.getRed()));
        settings.setFloat("green", parseColor(fields[5], current.getGreen()));
        settings.setFloat("blue", parseColor(fields[6], current.getBlue()));
        settings.setBoolean("blinking", blinking);
        settings.setFloat("blinkSeconds", parseFloat(fields[7], current.getBlinkSeconds()));
        int frequency = parseInt(fields[0], ISatChip.getFreqS(stack));
        PacketDispatcher.wrapper.sendToServer(new SatelliteSettingsPacket(hand, frequency, button.id == 102, settings));
        mc.displayGuiScreen(null);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if(keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(null);
            return;
        }
        if(keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
            for(GuiButton button : buttonList) {
                if(button.id == 100) {
                    actionPerformed(button);
                    break;
                }
            }
            return;
        }
        for(GuiTextField field : fields) field.textboxKeyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        for(GuiTextField field : fields) field.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if(wheel != 0) adjustFocusedField(wheel > 0 ? 1 : -1);
    }

    @Override
    public void updateScreen() {
        for(GuiTextField field : fields) field.updateCursorCounter();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        int left = width / 2 - 105;
        int top = height / 2 - 104;
        drawCenteredString(fontRenderer, I18n.format("gui.satellite_orbit.title"), width / 2, top, 0xFFFFFF);
        drawCenteredString(fontRenderer,
                TextFormatting.GRAY + I18n.format("gui.satellite_orbit.server_validation"),
                width / 2, top + 12, 0xAAAAAA);
        for(int i = 0; i < fields.length; i++) {
            int column = i / 4;
            int row = i % 4;
            fontRenderer.drawString(I18n.format(LABEL_KEYS[i]),
                    left + column * 110, top + 20 + row * 31, 0xC0C0C0);
            fields[i].drawTextBox();
        }
        int red = Math.round(parseColor(fields[4], 1F) * 255F);
        int green = Math.round(parseColor(fields[5], 1F) * 255F);
        int blue = Math.round(parseColor(fields[6], 1F) * 255F);
        drawRect(width / 2 + 4, top + 147, width / 2 + 102, top + 159,
                0xFF000000 | red << 16 | green << 8 | blue);
        ItemStack stack = mc.player.getHeldItem(hand);
        SatelliteTypeRegistry.Descriptor descriptor = SatelliteTypeRegistry.byItem(stack);
        Satellite sample = descriptor == null ? null : descriptor.create();
        OrbitSettings current = OrbitSettings.readFromStack(stack, sample);
        String owner = current.getOwnerName().isEmpty()
                ? I18n.format("gui.satellite_orbit.unclaimed")
                : I18n.format("gui.satellite_orbit.owner", current.getOwnerName());
        drawCenteredString(fontRenderer, owner, width / 2, top + 216, 0xA0A0A0);
        drawOrbitPreview(stack, top, red, green, blue);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private static float parseFloat(GuiTextField field, float fallback) {
        try {
            return Float.parseFloat(field.getText().replace(',', '.'));
        } catch(NumberFormatException ignored) {
            return fallback;
        }
    }

    private static int parseInt(GuiTextField field, int fallback) {
        try {
            return Integer.parseInt(field.getText());
        } catch(NumberFormatException ignored) {
            return fallback;
        }
    }

    private static float parseColor(GuiTextField field, float fallback) {
        try {
            return Math.max(0F, Math.min(255F, Float.parseFloat(field.getText().replace(',', '.')))) / 255F;
        } catch(NumberFormatException ignored) {
            return fallback;
        }
    }

    private static String trim(float value) {
        if(value == (long) value) return Long.toString((long) value);
        return Float.toString(value);
    }

    private String blinkLabel() {
        return I18n.format("gui.satellite_orbit.blink_enabled") + ": "
                + I18n.format(blinking ? "gui.satellite_orbit.on" : "gui.satellite_orbit.off");
    }

    private void adjustFocusedField(int direction) {
        for(int i = 0; i < fields.length; i++) {
            GuiTextField field = fields[i];
            if(!field.isFocused()) continue;
            if(i == 0) {
                int value = clamp(parseInt(field, 0) + direction, 0, 100_000);
                field.setText(Integer.toString(value));
                return;
            }

            float value;
            if(i == 1) value = clamp(parseFloat(field, 0F) + direction,
                    OrbitSettings.MIN_INCLINATION, OrbitSettings.MAX_INCLINATION);
            else if(i == 2) value = clamp(parseFloat(field, 100F) + direction,
                    OrbitSettings.MIN_ALTITUDE_KM, OrbitSettings.MAX_ALTITUDE_KM);
            else if(i == 3) value = clamp(parseFloat(field, 0F) + direction,
                    OrbitSettings.MIN_PHASE, OrbitSettings.MAX_PHASE);
            else if(i >= 4 && i <= 6) value = clamp(parseFloat(field, 255F) + direction, 0F, 255F);
            else value = clamp(parseFloat(field, 0.6F) + direction * 0.1F,
                        OrbitSettings.MIN_BLINK_SECONDS, OrbitSettings.MAX_BLINK_SECONDS);
            if(i == 7) value = Math.round(value * 10F) / 10F;
            field.setText(trim(value));
            return;
        }
    }

    private void drawOrbitPreview(ItemStack stack, int top, int red, int green, int blue) {
        if(width < 430) {
            drawCompactOrbitPreview(stack, top, red, green, blue);
            return;
        }

        int left = width / 2 + 116;
        int panelTop = top + 28;
        int panelRight = left + 96;
        int panelBottom = panelTop + 126;
        drawRect(left, panelTop, panelRight, panelBottom, 0xB0101010);
        drawRect(left + 1, panelTop + 1, panelRight - 1, panelBottom - 1, 0xD0202020);
        drawCenteredString(fontRenderer, I18n.format("gui.satellite_orbit.preview"),
                left + 48, panelTop + 6, 0xC0C0C0);

        int centerX = left + 48;
        int centerY = panelTop + 64;
        float altitude = clamp(parseFloat(fields[2], 100F),
                OrbitSettings.MIN_ALTITUDE_KM, OrbitSettings.MAX_ALTITUDE_KM);
        float inclination = clamp(parseFloat(fields[1], 0F),
                OrbitSettings.MIN_INCLINATION, OrbitSettings.MAX_INCLINATION);
        float phase = clamp(parseFloat(fields[3], 0F),
                OrbitSettings.MIN_PHASE, OrbitSettings.MAX_PHASE);
        int radiusX = 27 + Math.round((altitude - OrbitSettings.MIN_ALTITUDE_KM) / 6F);
        int radiusY = Math.max(5, Math.round(radiusX * (0.25F
                + 0.55F * Math.abs((float)Math.cos(Math.toRadians(inclination))))));
        int color = 0xFF000000 | red << 16 | green << 8 | blue;
        drawEllipse(centerX, centerY, radiusX, radiusY, color);
        drawBody(centerX, centerY);

        double phaseRadians = Math.toRadians(phase);
        int satelliteX = centerX + Math.round((float)Math.cos(phaseRadians) * radiusX) - 8;
        int satelliteY = centerY + Math.round((float)Math.sin(phaseRadians) * radiusY) - 8;
        itemRender.renderItemAndEffectIntoGUI(stack, satelliteX, satelliteY);

        drawCenteredString(fontRenderer, previewOrbitName(stack), centerX, panelTop + 96, 0xFFFFFF);
        drawCenteredString(fontRenderer, I18n.format("gui.satellite_orbit.scroll_hint"),
                centerX, panelTop + 110, 0x909090);
    }

    private void drawCompactOrbitPreview(ItemStack stack, int top, int red, int green, int blue) {
        int centerX = width / 2 + 53;
        int centerY = top + 153;
        int color = 0xFF000000 | red << 16 | green << 8 | blue;
        drawEllipse(centerX, centerY, 34, 5, color);
        drawBody(centerX, centerY);
        itemRender.renderItemAndEffectIntoGUI(stack, centerX + 22, centerY - 8);
    }

    private void drawEllipse(int centerX, int centerY, int radiusX, int radiusY, int color) {
        for(int degrees = 0; degrees < 360; degrees += 4) {
            double radians = Math.toRadians(degrees);
            int x = centerX + Math.round((float)Math.cos(radians) * radiusX);
            int y = centerY + Math.round((float)Math.sin(radians) * radiusY);
            drawRect(x, y, x + 1, y + 1, color);
        }
    }

    private void drawBody(int centerX, int centerY) {
        for(int y = -6; y <= 6; y++) {
            int halfWidth = (int)Math.sqrt(36 - y * y);
            drawRect(centerX - halfWidth, centerY + y,
                    centerX + halfWidth + 1, centerY + y + 1, 0xFF486A8A);
        }
    }

    private String previewOrbitName(ItemStack stack) {
        OrbitKey orbitKey = ISatChip.getOrbitKeyS(stack);
        if(orbitKey == null && mc.world != null && mc.player != null) {
            try {
                orbitKey = OrbitKey.fromWorld(mc.world,
                        (int)Math.floor(mc.player.posX), (int)Math.floor(mc.player.posZ));
            } catch(RuntimeException ignored) {
                // The client can briefly lack a body while changing dimensions.
            }
        }
        if(orbitKey == null) return I18n.format("gui.satellite_orbit.local_orbit");
        if(orbitKey.isBody()) return I18n.format("body." + orbitKey.getBodyName());
        return I18n.format("gui.satellite_orbit.dimension", orbitKey.getDimensionId());
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
