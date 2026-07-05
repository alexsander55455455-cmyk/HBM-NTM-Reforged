package com.hbm.items.armor;

import com.hbm.items.ModItems;
import com.hbm.items.gear.ArmorFSB;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor.ArmorMaterial;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

public class ArmorAsbestosFSB extends ArmorFSB {

    private static final float DAMAGE_THRESHOLD = 2F;

    public ArmorAsbestosFSB(ArmorMaterial material, int renderIndex, EntityEquipmentSlot slot, String texture, String name) {
        super(material, renderIndex, slot, texture, name);
    }

    public static boolean hasFullSet(EntityLivingBase entity) {
        return isEquipped(entity, EntityEquipmentSlot.HEAD, ModItems.asbestos_helmet)
                && isEquipped(entity, EntityEquipmentSlot.CHEST, ModItems.asbestos_plate)
                && isEquipped(entity, EntityEquipmentSlot.LEGS, ModItems.asbestos_legs)
                && isEquipped(entity, EntityEquipmentSlot.FEET, ModItems.asbestos_boots);
    }

    private static boolean isEquipped(EntityLivingBase entity, EntityEquipmentSlot slot, Item item) {
        ItemStack stack = entity.getItemStackFromSlot(slot);
        return !stack.isEmpty() && stack.getItem() == item && stack.getItem() instanceof ArmorFSB armor && armor.isArmorEnabled(stack);
    }

    public static void handleAsbestosAttack(LivingAttackEvent event) {
        EntityLivingBase entity = event.getEntityLiving();
        if (!hasFullSet(entity)) {
            return;
        }

        DamageSource source = event.getSource();
        if (source.isFireDamage()) {
            entity.extinguish();
            event.setCanceled(true);
            return;
        }

        if (!source.isUnblockable() && DAMAGE_THRESHOLD >= event.getAmount()) {
            event.setCanceled(true);
        }
    }

    public static void handleAsbestosHurt(LivingHurtEvent event) {
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

        if (!source.isUnblockable()) {
            event.setAmount(Math.max(0F, event.getAmount() - DAMAGE_THRESHOLD));
        }
    }

    @Override
    public boolean isValidArmor(ItemStack stack, EntityEquipmentSlot slot, Entity entity) {
        if (stack.getItem() == ModItems.asbestos_helmet) {
            return slot == EntityEquipmentSlot.HEAD;
        }
        if (stack.getItem() == ModItems.asbestos_plate) {
            return slot == EntityEquipmentSlot.CHEST;
        }
        if (stack.getItem() == ModItems.asbestos_legs) {
            return slot == EntityEquipmentSlot.LEGS;
        }
        if (stack.getItem() == ModItems.asbestos_boots) {
            return slot == EntityEquipmentSlot.FEET;
        }
        return super.isValidArmor(stack, slot, entity);
    }

}
