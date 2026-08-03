package com.hbm.packet.toserver;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SatelliteSettingsPacketContractTest {

    @Test
    void packetRoundTripPreservesRequestAndSettings() {
        NBTTagCompound settings = new NBTTagCompound();
        settings.setFloat("inclination", -180F);
        settings.setFloat("altitudeKm", 125F);
        settings.setFloat("phase", 359F);
        settings.setFloat("red", 12F / 255F);
        settings.setFloat("green", 128F / 255F);
        settings.setFloat("blue", 1F);
        settings.setBoolean("blinking", true);
        settings.setFloat("blinkSeconds", 0.3F);

        SatelliteSettingsPacket original = new SatelliteSettingsPacket(EnumHand.OFF_HAND, 100_000, true, settings);
        ByteBuf buffer = Unpooled.buffer();
        original.toBytes(buffer);
        SatelliteSettingsPacket restored = new SatelliteSettingsPacket();
        restored.fromBytes(buffer);

        assertEquals(EnumHand.OFF_HAND.ordinal(), restored.handOrdinal);
        assertEquals(100_000, restored.frequency);
        assertTrue(restored.claimOwner);
        assertEquals(settings, restored.settings);
    }

    @Test
    void handAndFrequencyValidationRejectsUntrustedBounds() {
        assertTrue(SatelliteSettingsPacket.isValidHandOrdinal(EnumHand.MAIN_HAND.ordinal()));
        assertTrue(SatelliteSettingsPacket.isValidHandOrdinal(EnumHand.OFF_HAND.ordinal()));
        assertFalse(SatelliteSettingsPacket.isValidHandOrdinal(-1));
        assertFalse(SatelliteSettingsPacket.isValidHandOrdinal(255));
        assertEquals(0, SatelliteSettingsPacket.clampFrequency(Integer.MIN_VALUE));
        assertEquals(100_000, SatelliteSettingsPacket.clampFrequency(Integer.MAX_VALUE));
    }

    @Test
    void satellitePacketDiscriminatorsStayAtTheEndOfTheProjectPacketBlock() throws Exception {
        Path sourcePath = Path.of(System.getProperty("user.dir"))
                .resolve("src/main/java/com/hbm/packet/PacketDispatcher.java");
        String source = Files.readString(sourcePath, StandardCharsets.UTF_8);
        int previous = source.indexOf("TETurretCIWSPacket.Handler.class");
        int settings = source.indexOf("SatelliteSettingsPacket.Handler.class");
        int snapshot = source.indexOf("SatelliteSnapshotPacket.Handler.class");
        int listeners = source.indexOf("for (IPacketRegisterListener listener");
        assertTrue(previous >= 0 && previous < settings);
        assertTrue(settings < snapshot);
        assertTrue(snapshot < listeners);
    }

    @Test
    void settingsGuiKeepsThePlannedOrbitPreviewAndScrollControls() throws Exception {
        Path sourcePath = Path.of(System.getProperty("user.dir"))
                .resolve("src/main/java/com/hbm/inventory/gui/GUISatelliteOrbitSettings.java");
        String source = Files.readString(sourcePath, StandardCharsets.UTF_8);
        assertTrue(source.contains("drawOrbitPreview(stack"));
        assertTrue(source.contains("renderItemAndEffectIntoGUI(stack"));
        assertTrue(source.contains("Mouse.getEventDWheel()"));
        assertTrue(source.contains("OrbitSettings.MIN_ALTITUDE_KM"));
        assertTrue(source.contains("OrbitSettings.MAX_ALTITUDE_KM"));
    }
}
