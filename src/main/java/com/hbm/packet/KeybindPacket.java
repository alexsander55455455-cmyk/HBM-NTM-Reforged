package com.hbm.packet;

import com.hbm.handler.HbmKeybinds.EnumKeybind;
import com.hbm.handler.HbmKeybindsServer;
import com.hbm.items.gear.ArmorFSB;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class KeybindPacket implements IMessage {

	int id;
	int key;
	boolean pressed;

	public KeybindPacket() { }

	public KeybindPacket(EnumKeybind key, boolean pressed) {
		this.key = key.ordinal();
		this.pressed = pressed;
		this.id = 0;
	}
	
	public KeybindPacket(int id) {
		this.id = id;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		key = buf.readInt();
		pressed = buf.readBoolean();
		id = buf.readInt();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(key);
		buf.writeBoolean(pressed);
		buf.writeInt(id);
	}

	public static class Handler implements IMessageHandler<KeybindPacket, IMessage> {

		@Override
		public IMessage onMessage(KeybindPacket m, MessageContext ctx) {
			EntityPlayer p = ctx.getServerHandler().player;
			if(m.id == 1) {
				toggleFSBFlashlight(p);
				return null;
			}
			if(m.key < 0 || m.key >= EnumKeybind.VALUES.length) return null;

			EnumKeybind key = EnumKeybind.VALUES[m.key];
			if(key == EnumKeybind.BACKPACK || key == EnumKeybind.RELOAD) {
				boolean pressed = m.pressed;
				p.getServer().addScheduledTask(() -> HbmKeybindsServer.onPressedServer(p, key, pressed));
				return null;
			}

			HbmKeybindsServer.onPressedServer(p, key, m.pressed);
			return null;
		}

		private void toggleFSBFlashlight(EntityPlayer player) {
			if(!ArmorFSB.hasFSBArmor(player)) return;

			ItemStack stack = player.getItemStackFromSlot(EntityEquipmentSlot.CHEST);
			if(stack.isEmpty() || !(stack.getItem() instanceof ArmorFSB fsbarmor)) return;
			if(fsbarmor.flashlightPosition == null) return;

			if(!stack.hasTagCompound()) {
				stack.setTagCompound(new NBTTagCompound());
			}

			player.world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.BLOCK_LEVER_CLICK, SoundCategory.PLAYERS, 0.5F, 1);
			stack.getTagCompound().setBoolean("flActive", !stack.getTagCompound().getBoolean("flActive"));
		}
	}
}
