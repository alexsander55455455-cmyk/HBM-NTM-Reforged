package com.hbm.handler;

import com.hbm.Tags;
import com.hbm.capability.BackpackCapability;
import com.hbm.items.tool.ItemRealityErrorBackpack;
import com.hbm.lib.HBMSoundHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(value = Side.CLIENT, modid = Tags.MODID)
public final class RealityErrorClientHandler {

    public static final int MIN_DELAY_TICKS = 6_000;
    public static final int MAX_DELAY_TICKS = 12_000;
    private static final int MESSAGE_COUNT = 4;
    private static final Random RANDOM = new Random();

    private static WorldClient trackedWorld;
    private static EntityPlayerSP trackedPlayer;
    private static int ticksUntilEffect = -1;

    private RealityErrorClientHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent event) {
        if (event.phase != Phase.END) return;

        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.world != trackedWorld || minecraft.player != trackedPlayer) {
            trackedWorld = minecraft.world;
            trackedPlayer = minecraft.player;
            ticksUntilEffect = -1;
        }
        if (trackedWorld == null || trackedPlayer == null || minecraft.isGamePaused()) return;

        ItemStack equipped = BackpackCapability.getData(trackedPlayer).getEquippedBackpack();
        if (!(equipped.getItem() instanceof ItemRealityErrorBackpack)) {
            ticksUntilEffect = -1;
            return;
        }

        if (ticksUntilEffect < 0) {
            scheduleNextEffect();
            return;
        }
        if (--ticksUntilEffect > 0) return;

        playLocalEffect(minecraft);
        scheduleNextEffect();
    }

    @SubscribeEvent
    public static void onDisconnect(ClientDisconnectionFromServerEvent event) {
        reset();
    }

    @SubscribeEvent
    public static void onWorldUnload(WorldEvent.Unload event) {
        if (event.getWorld().isRemote) {
            reset();
        }
    }

    private static void playLocalEffect(Minecraft minecraft) {
        SoundEvent sound = RANDOM.nextBoolean() ? HBMSoundHandler.techBoop : HBMSoundHandler.techBleep;
        if (sound != null) {
            float pitch = 0.35F + RANDOM.nextFloat() * 1.3F;
            minecraft.getSoundHandler().playSound(PositionedSoundRecord.getRecord(sound, pitch, 0.18F));
        }

        TextComponentTranslation message = new TextComponentTranslation(
                "chat.backpack.reality_error." + RANDOM.nextInt(MESSAGE_COUNT));
        message.setStyle(new Style().setColor(TextFormatting.DARK_PURPLE).setItalic(true));
        minecraft.ingameGUI.getChatGUI().printChatMessage(message);
    }

    private static void scheduleNextEffect() {
        ticksUntilEffect = MIN_DELAY_TICKS
                + RANDOM.nextInt(MAX_DELAY_TICKS - MIN_DELAY_TICKS + 1);
    }

    private static void reset() {
        trackedWorld = null;
        trackedPlayer = null;
        ticksUntilEffect = -1;
    }
}
