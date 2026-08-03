package com.hbm.saveddata.satellites;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.item.ItemStack;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Validated visual and ownership settings attached to an orbiting satellite.
 */
public final class OrbitSettings {

    public static final String ITEM_NBT_KEY = "hbmOrbitSettings";
    public static final float MIN_INCLINATION = -180F;
    public static final float MAX_INCLINATION = 180F;
    public static final float MIN_ALTITUDE_KM = 80F;
    public static final float MAX_ALTITUDE_KM = 125F;
    public static final float MIN_PHASE = 0F;
    public static final float MAX_PHASE = 360F;
    public static final float MIN_BLINK_SECONDS = 0.3F;
    public static final float MAX_BLINK_SECONDS = 1F;

    private float inclination;
    private float altitudeKm = 100F;
    private float phase;
    private float red = 1F;
    private float green = 1F;
    private float blue = 1F;
    private boolean blinking;
    private float blinkSeconds = 0.6F;
    @Nullable
    private UUID ownerUuid;
    private String ownerName = "";

    public static OrbitSettings defaultsFor(@Nullable Satellite satellite) {
        OrbitSettings settings = new OrbitSettings();
        if(satellite != null) {
            float[] color = satellite.getColor();
            if(color != null && color.length >= 3) {
                settings.red = clamp(color[0], 0F, 1F);
                settings.green = clamp(color[1], 0F, 1F);
                settings.blue = clamp(color[2], 0F, 1F);
            }
        }
        return settings;
    }

    public OrbitSettings copy() {
        OrbitSettings copy = new OrbitSettings();
        copy.inclination = inclination;
        copy.altitudeKm = altitudeKm;
        copy.phase = phase;
        copy.red = red;
        copy.green = green;
        copy.blue = blue;
        copy.blinking = blinking;
        copy.blinkSeconds = blinkSeconds;
        copy.ownerUuid = ownerUuid;
        copy.ownerName = ownerName;
        return copy;
    }

    public void validate() {
        inclination = finiteClamp(inclination, 0F, MIN_INCLINATION, MAX_INCLINATION);
        altitudeKm = finiteClamp(altitudeKm, 100F, MIN_ALTITUDE_KM, MAX_ALTITUDE_KM);
        phase = normalizePhase(phase);
        red = finiteClamp(red, 1F, 0F, 1F);
        green = finiteClamp(green, 1F, 0F, 1F);
        blue = finiteClamp(blue, 1F, 0F, 1F);
        blinkSeconds = finiteClamp(blinkSeconds, 0.6F, MIN_BLINK_SECONDS, MAX_BLINK_SECONDS);
        if(ownerName == null) ownerName = "";
        if(ownerName.length() > 64) ownerName = ownerName.substring(0, 64);
    }

    public NBTTagCompound writeToNBT() {
        validate();
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setFloat("inclination", inclination);
        nbt.setFloat("altitudeKm", altitudeKm);
        nbt.setFloat("phase", phase);
        nbt.setFloat("red", red);
        nbt.setFloat("green", green);
        nbt.setFloat("blue", blue);
        nbt.setBoolean("blinking", blinking);
        nbt.setFloat("blinkSeconds", blinkSeconds);
        if(ownerUuid != null) {
            nbt.setLong("ownerMost", ownerUuid.getMostSignificantBits());
            nbt.setLong("ownerLeast", ownerUuid.getLeastSignificantBits());
        }
        nbt.setString("ownerName", ownerName);
        return nbt;
    }

    public static OrbitSettings readFromNBT(@Nullable NBTTagCompound nbt, @Nullable Satellite satellite) {
        OrbitSettings settings = defaultsFor(satellite);
        if(nbt == null || nbt.getKeySet().isEmpty()) return settings;

        if(nbt.hasKey("inclination", 99)) settings.inclination = nbt.getFloat("inclination");
        else if(nbt.hasKey("satInclination", 99)) settings.inclination = nbt.getFloat("satInclination");
        if(nbt.hasKey("altitudeKm", 99)) settings.altitudeKm = nbt.getFloat("altitudeKm");
        else if(nbt.hasKey("satAltitude", 99)) settings.altitudeKm = nbt.getFloat("satAltitude");
        if(nbt.hasKey("phase", 99)) settings.phase = nbt.getFloat("phase");
        else if(nbt.hasKey("satPhaseOffset", 99)) settings.phase = nbt.getFloat("satPhaseOffset");
        if(nbt.hasKey("red", 99)) settings.red = nbt.getFloat("red");
        else if(nbt.hasKey("satColorR", 99)) settings.red = nbt.getFloat("satColorR");
        if(nbt.hasKey("green", 99)) settings.green = nbt.getFloat("green");
        else if(nbt.hasKey("satColorG", 99)) settings.green = nbt.getFloat("satColorG");
        if(nbt.hasKey("blue", 99)) settings.blue = nbt.getFloat("blue");
        else if(nbt.hasKey("satColorB", 99)) settings.blue = nbt.getFloat("satColorB");
        if(nbt.hasKey("blinking")) settings.blinking = nbt.getBoolean("blinking");
        else if(nbt.hasKey("satIsBlinking")) settings.blinking = nbt.getBoolean("satIsBlinking");
        if(nbt.hasKey("blinkSeconds", 99)) settings.blinkSeconds = nbt.getFloat("blinkSeconds");
        else if(nbt.hasKey("satBlink", 99)) settings.blinkSeconds = nbt.getFloat("satBlink");
        if(nbt.hasKey("ownerMost", 99) && nbt.hasKey("ownerLeast", 99)) {
            settings.ownerUuid = new UUID(nbt.getLong("ownerMost"), nbt.getLong("ownerLeast"));
        }
        settings.ownerName = nbt.hasKey("ownerName", 8)
                ? nbt.getString("ownerName") : nbt.getString("satOwner");
        settings.validate();
        return settings;
    }

    public static OrbitSettings readFromStack(ItemStack stack, @Nullable Satellite satellite) {
        if(stack == null || stack.isEmpty() || stack.getTagCompound() == null) {
            return defaultsFor(satellite);
        }
        NBTTagCompound stackTag = stack.getTagCompound();
        if(stackTag.hasKey(ITEM_NBT_KEY, 10)) {
            return readFromNBT(stackTag.getCompoundTag(ITEM_NBT_KEY), satellite);
        }

        // Read item settings written by the 1.7.10 Space fork without mutating
        // the old stack. The next explicit save writes the current schema.
        if(stackTag.hasKey("satInclination", 99) || stackTag.hasKey("satAltitude", 99)
                || stackTag.hasKey("satPhaseOffset", 99) || stackTag.hasKey("satBlink", 99)) {
            NBTTagCompound legacy = new NBTTagCompound();
            if(stackTag.hasKey("satInclination", 99)) legacy.setFloat("inclination", stackTag.getFloat("satInclination"));
            if(stackTag.hasKey("satAltitude", 99)) legacy.setFloat("altitudeKm", stackTag.getFloat("satAltitude"));
            if(stackTag.hasKey("satPhaseOffset", 99)) legacy.setFloat("phase", stackTag.getFloat("satPhaseOffset"));
            if(stackTag.hasKey("satColorR", 99)) legacy.setFloat("red", stackTag.getFloat("satColorR"));
            if(stackTag.hasKey("satColorG", 99)) legacy.setFloat("green", stackTag.getFloat("satColorG"));
            if(stackTag.hasKey("satColorB", 99)) legacy.setFloat("blue", stackTag.getFloat("satColorB"));
            if(stackTag.hasKey("satIsBlinking")) legacy.setBoolean("blinking", stackTag.getBoolean("satIsBlinking"));
            if(stackTag.hasKey("satBlink", 99)) legacy.setFloat("blinkSeconds", stackTag.getFloat("satBlink"));
            if(stackTag.hasKey("satOwner", 8)) legacy.setString("ownerName", stackTag.getString("satOwner"));
            return readFromNBT(legacy, satellite);
        }
        return defaultsFor(satellite);
    }

    public void writeToStack(ItemStack stack) {
        if(stack == null || stack.isEmpty()) return;
        if(stack.getTagCompound() == null) stack.setTagCompound(new NBTTagCompound());
        stack.getTagCompound().setTag(ITEM_NBT_KEY, writeToNBT());
    }

    public float getInclination() { return inclination; }
    public void setInclination(float value) { inclination = value; }
    public float getAltitudeKm() { return altitudeKm; }
    public void setAltitudeKm(float value) { altitudeKm = value; }
    public float getPhase() { return phase; }
    public void setPhase(float value) { phase = value; }
    public float getRed() { return red; }
    public float getGreen() { return green; }
    public float getBlue() { return blue; }
    public void setColor(float r, float g, float b) { red = r; green = g; blue = b; }
    public boolean isBlinking() { return blinking; }
    public void setBlinking(boolean value) { blinking = value; }
    public float getBlinkSeconds() { return blinkSeconds; }
    public void setBlinkSeconds(float value) { blinkSeconds = value; }
    @Nullable public UUID getOwnerUuid() { return ownerUuid; }
    public String getOwnerName() { return ownerName; }
    public void setOwner(@Nullable UUID uuid, @Nullable String name) {
        ownerUuid = uuid;
        ownerName = name == null ? "" : name;
    }

    private static float finiteClamp(float value, float fallback, float min, float max) {
        return Float.isNaN(value) || Float.isInfinite(value) ? fallback : clamp(value, min, max);
    }

    private static float normalizePhase(float value) {
        if(Float.isNaN(value) || Float.isInfinite(value)) return 0F;
        float normalized = value % MAX_PHASE;
        return normalized < 0F ? normalized + MAX_PHASE : normalized;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
