package com.hbm.items.armor;

import com.hbm.items.ModItems;
import com.hbm.items.gear.ArmorFSB;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor.ArmorMaterial;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class ArmorPAA extends ArmorFSB {

    private static final float DAMAGE_CAP = 4F;
    private static final float DAMAGE_MOD = 0.2F;
    private static final float BLAST_MOD = 0.5F;
    private static final float PROTECTION_YIELD = 200F;

    public ArmorPAA(ArmorMaterial material, int renderIndex, EntityEquipmentSlot slot, String texture, String name) {
        super(material, renderIndex, slot, texture, name);
    }

    public static boolean hasFullSet(EntityLivingBase entity) {
        return isEquipped(entity, EntityEquipmentSlot.HEAD, ModItems.paa_helmet)
                && isEquipped(entity, EntityEquipmentSlot.CHEST, ModItems.paa_plate)
                && isEquipped(entity, EntityEquipmentSlot.LEGS, ModItems.paa_legs)
                && isEquipped(entity, EntityEquipmentSlot.FEET, ModItems.paa_boots);
    }

    private static boolean isEquipped(EntityLivingBase entity, EntityEquipmentSlot slot, Item item) {
        ItemStack stack = entity.getItemStackFromSlot(slot);
        return !stack.isEmpty() && stack.getItem() == item && stack.getItem() instanceof ArmorFSB armor && armor.isArmorEnabled(stack);
    }

    public static void handlePAAAttack(LivingAttackEvent event) {
        EntityLivingBase entity = event.getEntityLiving();
        if (!hasFullSet(entity)) {
            return;
        }

        if (event.getSource().isFireDamage()) {
            entity.extinguish();
            event.setCanceled(true);
        }
    }

    public static void handlePAAHurt(LivingHurtEvent event) {
        EntityLivingBase entity = event.getEntityLiving();
        if (!hasFullSet(entity)) {
            return;
        }

        DamageSource source = event.getSource();
        if (source.isFireDamage()) {
            entity.extinguish();
            event.setAmount(0F);
            return;
        }

        float overflow = Math.max(0F, event.getAmount() - PROTECTION_YIELD);
        float amount = Math.min(event.getAmount(), PROTECTION_YIELD);

        amount *= DAMAGE_MOD;

        if (source.isExplosion()) {
            amount *= BLAST_MOD;
        }

        amount = Math.min(amount, DAMAGE_CAP);
        event.setAmount(Math.max(0F, amount + overflow));
    }

    public static void handleLivingTick(EntityLivingBase entity) {
        if (entity instanceof EntityPlayer || !hasFullSet(entity)) {
            return;
        }

        ItemStack plate = entity.getItemStackFromSlot(EntityEquipmentSlot.CHEST);
        if (!(plate.getItem() instanceof ArmorPAA chestplate)) {
            return;
        }

        for (PotionEffect effect : chestplate.effects) {
            entity.addPotionEffect(new PotionEffect(effect.getPotion(), effect.getDuration(), effect.getAmplifier(), effect.getIsAmbient(), effect.doesShowParticles()));
        }
    }

    @Override
    public void handleTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START || !hasFullSet(event.player)) {
            return;
        }

        for (PotionEffect effect : effects) {
            event.player.addPotionEffect(new PotionEffect(effect.getPotion(), effect.getDuration(), effect.getAmplifier(), effect.getIsAmbient(), effect.doesShowParticles()));
        }
    }

    @Override
    public boolean isValidArmor(ItemStack stack, EntityEquipmentSlot slot, Entity entity) {
        if (stack.getItem() == ModItems.paa_helmet) {
            return slot == EntityEquipmentSlot.HEAD;
        }
        if (stack.getItem() == ModItems.paa_plate) {
            return slot == EntityEquipmentSlot.CHEST;
        }
        if (stack.getItem() == ModItems.paa_legs) {
            return slot == EntityEquipmentSlot.LEGS;
        }
        if (stack.getItem() == ModItems.paa_boots) {
            return slot == EntityEquipmentSlot.FEET;
        }
        return super.isValidArmor(stack, slot, entity);
    }

}
