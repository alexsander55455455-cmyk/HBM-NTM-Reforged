package com.hbmspace.handler;

import com.hbm.util.BufferUtil;
import com.hbm.saveddata.satellites.OrbitKey;
import com.hbm.saveddata.satellites.OrbitSettings;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.init.Items;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

class RocketStructContractTest {

    static {
        Bootstrap.register();
    }

    @Test
    void nonMissileLegacyCapsuleIdDoesNotCauseClassCast() {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeInt(Item.getIdFromItem(Items.STICK));
        BufferUtil.writeItemStack(buffer, ItemStack.EMPTY);
        buffer.writeInt(0);
        buffer.writeInt(0);

        RocketStruct restored = RocketStruct.readFromByteBuffer(buffer);
        assertNull(restored.capsule);
        assertEquals(0, restored.stages.size());
    }

    @Test
    void oversizedStageAndIssueCountsAreRejectedBeforeAllocation() {
        ByteBuf stages = Unpooled.buffer();
        stages.writeInt(0);
        BufferUtil.writeItemStack(stages, ItemStack.EMPTY);
        stages.writeInt(RocketStruct.MAX_STAGES + 1);
        assertThrows(IllegalArgumentException.class, () -> RocketStruct.readFromByteBuffer(stages));

        ByteBuf issues = Unpooled.buffer();
        issues.writeInt(0);
        BufferUtil.writeItemStack(issues, ItemStack.EMPTY);
        issues.writeInt(0);
        issues.writeInt(RocketStruct.MAX_SYNC_ISSUES + 1);
        assertThrows(IllegalArgumentException.class, () -> RocketStruct.readFromByteBuffer(issues));
    }

    @Test
    void fullPayloadStackSurvivesTheCodecUsedByRocketStruct() {
        ItemStack payload = configuredPayload();

        NBTTagCompound rocketNbt = new NBTTagCompound();
        RocketPayloadCodec.writeToNBT(rocketNbt, payload);
        assertTrue(rocketNbt.hasKey("capsuleStack"));
        assertFalse(rocketNbt.hasKey("freq"));
        assertTrue(ItemStack.areItemStacksEqual(payload, RocketPayloadCodec.readFromNBT(rocketNbt)));

        ByteBuf buffer = Unpooled.buffer();
        RocketPayloadCodec.writeToByteBuffer(buffer, payload);
        assertTrue(ItemStack.areItemStacksEqual(payload, RocketPayloadCodec.readFromByteBuffer(buffer)));
    }

    @Test
    void assemblerEntityAndCustomRocketUseCanonicalPayloadStack() throws Exception {
        Path root = Path.of(System.getProperty("user.dir")).resolve("src/main/java");
        String assembly = Files.readString(root.resolve(
                "com/hbmspace/tileentity/machine/TileEntityMachineRocketAssembly.java"), StandardCharsets.UTF_8);
        String customRocket = Files.readString(root.resolve(
                "com/hbmspace/items/weapon/ItemCustomRocket.java"), StandardCharsets.UTF_8);
        String entity = Files.readString(root.resolve(
                "com/hbmspace/entity/missile/EntityRideableRocket.java"), StandardCharsets.UTF_8);
        String rocketStruct = Files.readString(root.resolve(
                "com/hbmspace/handler/RocketStruct.java"), StandardCharsets.UTF_8);
        assertTrue(assembly.contains("ItemStack capsuleStack = rocket.getCapsuleStack()"));
        assertTrue(assembly.contains("inventory.setStackInSlot(0, capsuleStack)"));
        assertTrue(customRocket.contains("rocket.writeToNBT(stack.getTagCompound())"));
        assertTrue(customRocket.contains("return RocketStruct.readFromNBT(stack.getTagCompound())"));
        assertTrue(entity.contains("DP_ROCKET_CAPSULE_STACK"));
        assertFalse(assembly.contains("rocket.satFreq"));
        assertFalse(entity.contains("rocket.satFreq"));
        assertTrue(rocketStruct.contains("legacySatFreq = nbt.getInteger(\"freq\")"));
        assertFalse(rocketStruct.contains("nbt.setInteger(\"freq\""));
    }

    private static ItemStack configuredPayload() {
        ItemStack payload = new ItemStack(Items.FIREWORKS, 4);
        payload.setTagCompound(new NBTTagCompound());
        payload.getTagCompound().setInteger("freq", 4321);
        payload.getTagCompound().setString("orbitKey", OrbitKey.body("duna").asString());
        OrbitSettings settings = new OrbitSettings();
        settings.setInclination(-47F);
        settings.setAltitudeKm(123F);
        settings.setPhase(359F);
        settings.setColor(12F / 255F, 128F / 255F, 250F / 255F);
        settings.setBlinking(true);
        settings.setBlinkSeconds(0.9F);
        settings.setOwner(UUID.fromString("b2bc68f9-3707-4bf8-9b9a-df705203cfdb"), "Payload owner");
        settings.writeToStack(payload);
        payload.getTagCompound().setString("customPayloadState", "preserve-me");
        payload.setCount(1);
        return payload;
    }
}
