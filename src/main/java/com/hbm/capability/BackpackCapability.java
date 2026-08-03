package com.hbm.capability;

import com.hbm.items.tool.ItemBackpack;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;

import java.util.concurrent.Callable;

public class BackpackCapability {

    public interface IBackpackData {
        ItemStack getEquippedBackpack();
        void setEquippedBackpack(ItemStack stack);
    }

    public static class BackpackData implements IBackpackData {
        public static final Callable<IBackpackData> FACTORY = BackpackData::new;
        private ItemStack equippedBackpack = ItemStack.EMPTY;

        @Override
        public ItemStack getEquippedBackpack() {
            return equippedBackpack;
        }

        @Override
        public void setEquippedBackpack(ItemStack stack) {
            if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof ItemBackpack)) {
                equippedBackpack = ItemStack.EMPTY;
                return;
            }

            equippedBackpack = stack.copy();
            equippedBackpack.setCount(1);
        }
    }

    public static class BackpackDataStorage implements Capability.IStorage<IBackpackData> {
        @Override
        public NBTBase writeNBT(Capability<IBackpackData> capability, IBackpackData instance, EnumFacing side) {
            NBTTagCompound tag = new NBTTagCompound();
            ItemStack stack = instance.getEquippedBackpack();
            if (!stack.isEmpty()) {
                tag.setTag("equipped", stack.serializeNBT());
            }
            return tag;
        }

        @Override
        public void readNBT(Capability<IBackpackData> capability, IBackpackData instance, EnumFacing side, NBTBase nbt) {
            if (nbt instanceof NBTTagCompound tag && tag.hasKey("equipped")) {
                instance.setEquippedBackpack(new ItemStack(tag.getCompoundTag("equipped")));
            } else {
                instance.setEquippedBackpack(ItemStack.EMPTY);
            }
        }
    }

    public static class BackpackDataProvider implements ICapabilitySerializable<NBTBase> {
        public static final IBackpackData DUMMY = new IBackpackData() {
            @Override
            public ItemStack getEquippedBackpack() {
                return ItemStack.EMPTY;
            }

            @Override
            public void setEquippedBackpack(ItemStack stack) {
            }
        };

        @CapabilityInject(IBackpackData.class)
        public static Capability<IBackpackData> BACKPACK_CAP = null;

        private final IBackpackData instance = BACKPACK_CAP.getDefaultInstance();

        @Override
        public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
            return capability == BACKPACK_CAP;
        }

        @Override
        public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
            return capability == BACKPACK_CAP ? BACKPACK_CAP.cast(instance) : null;
        }

        @Override
        public NBTBase serializeNBT() {
            return BACKPACK_CAP.getStorage().writeNBT(BACKPACK_CAP, instance, null);
        }

        @Override
        public void deserializeNBT(NBTBase nbt) {
            BACKPACK_CAP.getStorage().readNBT(BACKPACK_CAP, instance, null, nbt);
        }
    }

    public static IBackpackData getData(EntityPlayer player) {
        if (player != null && player.hasCapability(BackpackDataProvider.BACKPACK_CAP, null)) {
            IBackpackData data = player.getCapability(BackpackDataProvider.BACKPACK_CAP, null);
            if (data != null) return data;
        }
        return BackpackDataProvider.DUMMY;
    }
}
