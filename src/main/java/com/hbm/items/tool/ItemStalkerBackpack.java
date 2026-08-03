package com.hbm.items.tool;

import com.hbm.handler.BackpackHandler;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.util.I18nUtil;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;
import java.util.List;

public class ItemStalkerBackpack extends ItemBackpack {

    public static final int SLOTS = 45;
    public static final String EXPEDITION_NUMBER_TAG = "StalkerExpeditionNumber";
    private static final double ACTIVE_THRESHOLD = 1.0E-5D;
    private static final int MIN_CLICK_INTERVAL = 8;
    private static final int MAX_CLICK_INTERVAL = 60;
    private static final int STEALTH_DURATION_TICKS = 45 * 20;
    private static final int STEALTH_COOLDOWN_TICKS = 60 * 20;

    public ItemStalkerBackpack(String name) {
        super(name, SLOTS, 0.85D, false);
        setCreativeTab(null);
    }

    @Override
    public boolean onBackpackTick(EntityPlayer player, ItemStack stack, boolean equipped) {
        if (player.world.isRemote) return false;
        boolean changed = false;
        if (!stack.hasTagCompound()
                || !stack.getTagCompound().hasKey(EXPEDITION_NUMBER_TAG, Constants.NBT.TAG_INT)) {
            getOrCreateTag(stack).setInteger(EXPEDITION_NUMBER_TAG,
                    100_000 + player.world.rand.nextInt(900_000));
            changed = true;
        }

        if (equipped) {
            double radiation = BackpackHandler.getDetectorRadiation(player);
            if (radiation > ACTIVE_THRESHOLD) {
                int interval = getClickInterval(radiation);
                long offset = Math.floorMod(player.getUniqueID().getLeastSignificantBits(), interval);
                if (Math.floorMod(player.world.getTotalWorldTime(), interval) == offset) {
                    int soundIndex = Math.min(HBMSoundHandler.geigerSounds.length - 1,
                            Math.max(0, (int) Math.floor(Math.log10(radiation + 1D) * 2D)));
                    player.world.playSound(null, player.posX, player.posY, player.posZ,
                            HBMSoundHandler.geigerSounds[soundIndex], SoundCategory.PLAYERS,
                            0.16F, 0.68F + player.world.rand.nextFloat() * 0.08F);
                }
            }
        }
        return changed;
    }

    public boolean activateStealth(EntityPlayer player) {
        if (player.world.isRemote || player.getCooldownTracker().hasCooldown(this)) return false;

        player.addPotionEffect(new PotionEffect(
                MobEffects.INVISIBILITY, STEALTH_DURATION_TICKS, 1, false, false));
        player.getCooldownTracker().setCooldown(this, STEALTH_DURATION_TICKS + STEALTH_COOLDOWN_TICKS);
        return true;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        super.addInformation(stack, world, tooltip, flag);
        if (stack.hasTagCompound()
                && stack.getTagCompound().hasKey(EXPEDITION_NUMBER_TAG, Constants.NBT.TAG_INT)) {
            tooltip.add(TextFormatting.GRAY + I18nUtil.resolveKey("desc.backpack.stalker.expedition",
                    String.format("%06d", stack.getTagCompound().getInteger(EXPEDITION_NUMBER_TAG))));
        } else {
            tooltip.add(TextFormatting.DARK_GRAY + I18nUtil.resolveKey("desc.backpack.stalker.expedition_unknown"));
        }
        tooltip.add(TextFormatting.GRAY + I18nUtil.resolveKey("desc.backpack.stalker.detector"));
        tooltip.add(TextFormatting.AQUA + I18nUtil.resolveKey("desc.backpack.stalker.stealth"));
        tooltip.add(TextFormatting.DARK_GRAY + I18nUtil.resolveKey("desc.backpack.stalker.lore"));
    }

    private static int getClickInterval(double radiation) {
        double scaled = MAX_CLICK_INTERVAL / (1D + Math.sqrt(radiation));
        return Math.max(MIN_CLICK_INTERVAL, Math.min(MAX_CLICK_INTERVAL, (int) Math.ceil(scaled)));
    }

    private static NBTTagCompound getOrCreateTag(ItemStack stack) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        return stack.getTagCompound();
    }
}
