package com.hbm.packet.toserver;

import com.hbm.handler.BackpackHandler;
import com.hbm.inventory.container.ContainerBackpack;
import com.hbm.items.tool.ItemBlackBoxBackpack;
import com.hbm.items.tool.ItemSmugglerBackpack;
import com.mojang.authlib.GameProfile;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.UUID;

public class BlackBoxAccessPacket implements IMessage {

    public static final int CYCLE_MODE = 0;
    public static final int ADD_PLAYER = 1;
    public static final int REMOVE_PLAYER = 2;
    public static final int RESET_OWNER = 3;

    private int action;
    private String value;

    public BlackBoxAccessPacket() {
    }

    public BlackBoxAccessPacket(int action, String value) {
        this.action = action;
        this.value = value == null ? "" : value;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        action = buf.readUnsignedByte();
        value = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(action);
        ByteBufUtils.writeUTF8String(buf, value);
    }

    public static class Handler implements IMessageHandler<BlackBoxAccessPacket, IMessage> {

        @Override
        public IMessage onMessage(BlackBoxAccessPacket message, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().player;
            String value = message.value == null ? "" : message.value.trim();
            if (value.length() > 64) return null;
            player.getServer().addScheduledTask(() -> handle(player, message.action, value));
            return null;
        }

        private static void handle(EntityPlayerMP player, int action, String value) {
            if (!(player.openContainer instanceof ContainerBackpack container)) return;
            ItemStack stack = container.getBackpackStack();
            boolean changed = false;
            if (stack.getItem() instanceof ItemBlackBoxBackpack blackBox && blackBox.isOwner(stack, player)) {
                changed = handleBlackBox(player, stack, blackBox, action, value);
            } else if (stack.getItem() instanceof ItemSmugglerBackpack smuggler
                    && smuggler.isOwner(stack, player)) {
                changed = handleSmuggler(player, stack, smuggler, action, value);
            }

            if (changed) {
                player.inventory.markDirty();
                BackpackHandler.syncEquipmentState(player);
                container.detectAndSendChanges();
                if (action == RESET_OWNER) {
                    player.closeScreen();
                }
            }
        }

        private static boolean handleBlackBox(EntityPlayerMP player, ItemStack stack,
                                              ItemBlackBoxBackpack blackBox, int action, String value) {
            if (action == CYCLE_MODE) {
                return blackBox.cycleAccessMode(stack, player);
            }
            if (action == RESET_OWNER) {
                return blackBox.resetOwner(stack, player);
            }
            if (action == ADD_PLAYER && !value.isEmpty()) {
                GameProfile profile = findCachedProfile(player, value);
                if (profile == null || profile.getId() == null) {
                    player.sendStatusMessage(
                            new TextComponentTranslation("message.backpack.black_box.player_not_cached", value), true);
                    return false;
                }
                return blackBox.addAllowedPlayer(stack, player, profile.getId(), profile.getName());
            }
            if (action == REMOVE_PLAYER) {
                try {
                    return blackBox.removeAllowedPlayer(stack, player, UUID.fromString(value));
                } catch (IllegalArgumentException ignored) {
                    return false;
                }
            }
            return false;
        }

        private static boolean handleSmuggler(EntityPlayerMP player, ItemStack stack,
                                              ItemSmugglerBackpack smuggler, int action, String value) {
            if (action == RESET_OWNER) {
                return smuggler.resetOwner(stack, player);
            }
            if (action == ADD_PLAYER && !value.isEmpty()) {
                GameProfile profile = findCachedProfile(player, value);
                if (profile == null || profile.getId() == null) {
                    player.sendStatusMessage(
                            new TextComponentTranslation("message.backpack.black_box.player_not_cached", value), true);
                    return false;
                }
                return smuggler.addAllowedPlayer(stack, player, profile.getId(), profile.getName());
            }
            if (action == REMOVE_PLAYER) {
                try {
                    return smuggler.removeAllowedPlayer(stack, player, UUID.fromString(value));
                } catch (IllegalArgumentException ignored) {
                    return false;
                }
            }
            return false;
        }

        private static GameProfile findCachedProfile(EntityPlayerMP owner, String name) {
            EntityPlayerMP online = owner.getServer().getPlayerList().getPlayerByUsername(name);
            if (online != null) return online.getGameProfile();

            for (String cachedName : owner.getServer().getPlayerProfileCache().getUsernames()) {
                if (cachedName.equalsIgnoreCase(name)) {
                    return owner.getServer().getPlayerProfileCache().getGameProfileForUsername(cachedName);
                }
            }
            return null;
        }
    }
}
