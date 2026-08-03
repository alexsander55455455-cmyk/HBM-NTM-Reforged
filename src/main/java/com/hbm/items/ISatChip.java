package com.hbm.items;

import com.hbm.saveddata.satellites.OrbitKey;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nullable;

public interface ISatChip {
    String ORBIT_NBT_KEY = "orbitKey";

    public static int getFreqS(ItemStack stack) {
        if(stack != null && !stack.isEmpty() && stack.getItem() instanceof ISatChip) {
            return ((ISatChip) stack.getItem()).getFreq(stack);
        }

        return 0;
    }

    public static void setFreqS(ItemStack stack, int freq) {
        if(stack != null && !stack.isEmpty() && stack.getItem() instanceof ISatChip) {
            ((ISatChip) stack.getItem()).setFreq(stack, freq);
        }
    }

    @Nullable
    static OrbitKey getOrbitKeyS(ItemStack stack) {
        if(stack == null || stack.isEmpty() || !(stack.getItem() instanceof ISatChip)) return null;
        return ((ISatChip) stack.getItem()).getOrbitKey(stack);
    }

    static void setOrbitKeyS(ItemStack stack, @Nullable OrbitKey orbitKey) {
        if(stack != null && !stack.isEmpty() && stack.getItem() instanceof ISatChip) {
            ((ISatChip) stack.getItem()).setOrbitKey(stack, orbitKey);
        }
    }

    static void copyLink(ItemStack source, ItemStack target, @Nullable OrbitKey fallbackOrbit) {
        setFreqS(target, getFreqS(source));
        OrbitKey orbitKey = getOrbitKeyS(source);
        setOrbitKeyS(target, orbitKey == null ? fallbackOrbit : orbitKey);
    }

    public default int getFreq(ItemStack stack) {
        if(stack.getTagCompound() == null) {
            stack.setTagCompound(new NBTTagCompound());
            return 0;
        }
        return stack.getTagCompound().getInteger("freq");
    }

    public default void setFreq(ItemStack stack, int freq) {
        if(stack.getTagCompound() == null) {
            stack.setTagCompound(new NBTTagCompound());
        }
        stack.getTagCompound().setInteger("freq", freq);
    }

    @Nullable
    default OrbitKey getOrbitKey(ItemStack stack) {
        if(stack == null || stack.isEmpty() || stack.getTagCompound() == null) return null;
        return OrbitKey.parse(stack.getTagCompound().getString(ORBIT_NBT_KEY));
    }

    default void setOrbitKey(ItemStack stack, @Nullable OrbitKey orbitKey) {
        if(stack == null || stack.isEmpty()) return;
        if(stack.getTagCompound() == null) stack.setTagCompound(new NBTTagCompound());
        if(orbitKey == null) stack.getTagCompound().removeTag(ORBIT_NBT_KEY);
        else stack.getTagCompound().setString(ORBIT_NBT_KEY, orbitKey.asString());
    }
}
