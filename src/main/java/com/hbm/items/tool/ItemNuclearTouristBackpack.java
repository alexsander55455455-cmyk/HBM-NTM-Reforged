package com.hbm.items.tool;

import com.hbm.capability.HbmLivingProps;
import com.hbm.items.ModItems;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.util.I18nUtil;
import com.hbm.util.InventoryUtil;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

public class ItemNuclearTouristBackpack extends ItemBackpack {

    public static final int SLOTS = 36;
    public static final String ACTIVE_TAG = "NuclearTouristActive";
    private static final double ACTIVE_THRESHOLD = 1.0E-5D;
    private static final int MIN_CLICK_INTERVAL = 6;
    private static final int MAX_CLICK_INTERVAL = 40;

    public ItemNuclearTouristBackpack(String name) {
        super(name, SLOTS, 0.90D, false);
        setCreativeTab(null);
    }

    @Override
    public boolean onBackpackTick(EntityPlayer player, ItemStack stack, boolean equipped) {
        if (player.world.isRemote) return false;

        double radiation = equipped ? Math.max(0D, HbmLivingProps.getRadBuf(player)) : 0D;
        boolean active = radiation > ACTIVE_THRESHOLD;
        boolean wasActive = stack.hasTagCompound() && stack.getTagCompound().getBoolean(ACTIVE_TAG);
        boolean changed = active != wasActive;

        if (changed) {
            if (active) {
                getOrCreateTag(stack).setBoolean(ACTIVE_TAG, true);
            } else if (stack.hasTagCompound()) {
                stack.getTagCompound().removeTag(ACTIVE_TAG);
            }
        }

        if (active && !hasDedicatedDetector(player)) {
            int interval = getClickInterval(radiation);
            long offset = Math.floorMod(player.getUniqueID().getLeastSignificantBits(), interval);
            if (Math.floorMod(player.world.getTotalWorldTime(), interval) == offset) {
                int soundIndex = Math.min(HBMSoundHandler.geigerSounds.length - 1,
                        Math.max(0, (int) Math.floor(Math.log10(radiation + 1D) * 2D)));
                player.world.playSound(null, player.posX, player.posY, player.posZ,
                        HBMSoundHandler.geigerSounds[soundIndex], SoundCategory.PLAYERS,
                        0.35F, 0.95F + player.world.rand.nextFloat() * 0.1F);
            }
        }
        return changed;
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return stack.hasTagCompound() && stack.getTagCompound().getBoolean(ACTIVE_TAG);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        super.addInformation(stack, world, tooltip, flag);
        tooltip.add(TextFormatting.GREEN + I18nUtil.resolveKey("desc.backpack.nuclear_tourist.detector"));
        tooltip.add(TextFormatting.DARK_GRAY + I18nUtil.resolveKey("desc.backpack.nuclear_tourist.no_power"));
    }

    private static int getClickInterval(double radiation) {
        double scaled = MAX_CLICK_INTERVAL / (1D + Math.sqrt(radiation));
        return Math.max(MIN_CLICK_INTERVAL, Math.min(MAX_CLICK_INTERVAL, (int) Math.ceil(scaled)));
    }

    private static boolean hasDedicatedDetector(EntityPlayer player) {
        return InventoryUtil.hasItem(player, ModItems.geiger_counter)
                || InventoryUtil.hasItem(player, ModItems.dosimeter);
    }

    private static NBTTagCompound getOrCreateTag(ItemStack stack) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        return stack.getTagCompound();
    }
}
