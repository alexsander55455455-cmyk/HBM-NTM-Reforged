package com.hbm.items.tool;

import com.hbm.capability.BackpackCapability;
import com.hbm.handler.BackpackHandler;
import com.hbm.util.I18nUtil;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.event.entity.living.LivingAttackEvent;

import javax.annotation.Nullable;
import java.util.List;

public class ItemAshBackpack extends ItemBackpack {

    public static final int SLOTS = 27;
    public static final int FIRE_RESISTANCE_TICKS = 100;
    public static final int COOLDOWN_TICKS = 1_200;
    public static final String COOLDOWN_UNTIL_TAG = "AshFireCooldownUntil";

    public ItemAshBackpack(String name) {
        super(name, SLOTS, 0.75D, false);
        setCreativeTab(null);
    }

    @Override
    public boolean protectsDroppedItemDamage(DamageSource source, float amount) {
        return source != null && source.isFireDamage();
    }

    @Override
    public int getDroppedLavaSurvivalTicks() {
        return -1;
    }

    @Override
    public boolean onBackpackTick(EntityPlayer player, ItemStack stack, boolean equipped) {
        if (player.world.isRemote || !equipped) return false;

        long now = player.world.getTotalWorldTime();
        NBTTagCompound tag = stack.hasTagCompound() ? stack.getTagCompound() : null;
        long cooldownUntil = tag != null && tag.hasKey(COOLDOWN_UNTIL_TAG, Constants.NBT.TAG_LONG)
                ? tag.getLong(COOLDOWN_UNTIL_TAG)
                : 0L;
        boolean changed = false;

        // A copied item or a clock rollback must not leave the ability locked
        // for an unbounded amount of time.
        if (cooldownUntil < 0L || cooldownUntil - now > COOLDOWN_TICKS) {
            if (tag != null) {
                tag.removeTag(COOLDOWN_UNTIL_TAG);
            }
            cooldownUntil = 0L;
            changed = true;
        }

        return changed;
    }

    public static void handleFireAttack(LivingAttackEvent event) {
        if (event.isCanceled() || !event.getSource().isFireDamage()
                || !(event.getEntityLiving() instanceof EntityPlayer player)
                || player.world.isRemote) {
            return;
        }

        ItemStack stack = BackpackCapability.getData(player).getEquippedBackpack();
        if (!(stack.getItem() instanceof ItemAshBackpack)) return;

        long now = player.world.getTotalWorldTime();
        NBTTagCompound tag = stack.hasTagCompound() ? stack.getTagCompound() : null;
        long cooldownUntil = tag != null && tag.hasKey(COOLDOWN_UNTIL_TAG, Constants.NBT.TAG_LONG)
                ? tag.getLong(COOLDOWN_UNTIL_TAG)
                : 0L;
        if (now < cooldownUntil) return;

        player.addPotionEffect(new PotionEffect(MobEffects.FIRE_RESISTANCE,
                FIRE_RESISTANCE_TICKS, 0, true, false));
        getOrCreateTag(stack).setLong(COOLDOWN_UNTIL_TAG, now + COOLDOWN_TICKS);
        event.setCanceled(true);
        BackpackHandler.syncEquipmentState(player);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        super.addInformation(stack, world, tooltip, flag);
        tooltip.add(TextFormatting.GOLD + I18nUtil.resolveKey("desc.backpack.ash.fire_resistance",
                FIRE_RESISTANCE_TICKS / 20, COOLDOWN_TICKS / 20));
        tooltip.add(TextFormatting.AQUA + I18nUtil.resolveKey("desc.backpack.ash.dropped_fireproof"));
    }

    private static NBTTagCompound getOrCreateTag(ItemStack stack) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        return stack.getTagCompound();
    }
}
