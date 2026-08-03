package com.hbmspace.handler;

import com.hbm.util.BufferUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.Constants;

/** Exact full-stack codec shared by every RocketStruct persistence path. */
final class RocketPayloadCodec {

    static final String NBT_KEY = "capsuleStack";

    private RocketPayloadCodec() { }

    static ItemStack single(ItemStack stack) {
        if(stack == null || stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack copy = stack.copy();
        copy.setCount(1);
        return copy;
    }

    static void writeToNBT(NBTTagCompound nbt, ItemStack stack) {
        ItemStack payload = single(stack);
        if(!payload.isEmpty()) nbt.setTag(NBT_KEY, payload.writeToNBT(new NBTTagCompound()));
    }

    static ItemStack readFromNBT(NBTTagCompound nbt) {
        if(nbt == null || !nbt.hasKey(NBT_KEY, Constants.NBT.TAG_COMPOUND)) return ItemStack.EMPTY;
        return single(new ItemStack(nbt.getCompoundTag(NBT_KEY)));
    }

    static void writeToByteBuffer(ByteBuf buffer, ItemStack stack) {
        BufferUtil.writeItemStack(buffer, single(stack));
    }

    static ItemStack readFromByteBuffer(ByteBuf buffer) {
        return single(BufferUtil.readItemStack(buffer));
    }
}
