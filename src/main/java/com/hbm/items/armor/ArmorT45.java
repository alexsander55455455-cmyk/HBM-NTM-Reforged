package com.hbm.items.armor;

import com.hbm.items.ModItems;
import com.hbm.items.gear.ArmorFSB;
import com.hbm.render.model.ModelT45Boots;
import com.hbm.render.model.ModelT45Chest;
import com.hbm.render.model.ModelT45Helmet;
import com.hbm.render.model.ModelT45Legs;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
public class ArmorT45 extends ArmorFSBPowered {

    private static final float DAMAGE_CAP = 20F;
    private static final float DAMAGE_MOD = 0.5F;
    private static final float BLAST_MOD = 0.5F;
    private static final float PROTECTION_YIELD = 100F;

    @SideOnly(Side.CLIENT)
    private ModelT45Helmet helmet;
    @SideOnly(Side.CLIENT)
    private ModelT45Chest plate;
    @SideOnly(Side.CLIENT)
    private ModelT45Legs legs;
    @SideOnly(Side.CLIENT)
    private ModelT45Boots boots;

    public ArmorT45(ArmorMaterial material, int renderIndex, EntityEquipmentSlot slot, long maxPower, long chargeRate, long consumption, long drain, String s) {
        super(material, renderIndex, slot, "", maxPower, chargeRate, consumption, drain, s);
    }

    public static boolean hasFullSet(EntityLivingBase entity) {
        return isEquipped(entity, EntityEquipmentSlot.HEAD, ModItems.t45_helmet)
                && isEquipped(entity, EntityEquipmentSlot.CHEST, ModItems.t45_plate)
                && isEquipped(entity, EntityEquipmentSlot.LEGS, ModItems.t45_legs)
                && isEquipped(entity, EntityEquipmentSlot.FEET, ModItems.t45_boots);
    }

    private static boolean isEquipped(EntityLivingBase entity, EntityEquipmentSlot slot, Item item) {
        ItemStack stack = entity.getItemStackFromSlot(slot);
        return !stack.isEmpty() && stack.getItem() == item && stack.getItem() instanceof ArmorFSB armor && armor.isArmorEnabled(stack);
    }

    private static boolean isFallDamage(DamageSource source) {
        return DamageSource.FALL.getDamageType().equals(source.getDamageType());
    }

    public static void handleLivingTick(EntityLivingBase entity) {
        if (entity instanceof EntityPlayer || !hasFullSet(entity)) {
            return;
        }

        ItemStack plate = entity.getItemStackFromSlot(EntityEquipmentSlot.CHEST);
        if (!(plate.getItem() instanceof ArmorT45 chestplate)) {
            return;
        }

        for (PotionEffect effect : chestplate.effects) {
            entity.addPotionEffect(new PotionEffect(effect.getPotion(), effect.getDuration(), effect.getAmplifier(), effect.getIsAmbient(), effect.doesShowParticles()));
        }
    }

    public static void handleT45Attack(LivingAttackEvent event) {
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

        if (isFallDamage(source)) {
            event.setCanceled(true);
        }
    }

    public static void handleT45Hurt(LivingHurtEvent event) {
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

        if (isFallDamage(source)) {
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

    public static void handleT45Fall(LivingFallEvent event) {
        if (hasFullSet(event.getEntityLiving())) {
            event.setDistance(0F);
        }
    }

    @Override
    public boolean isValidArmor(ItemStack stack, EntityEquipmentSlot slot, Entity entity) {
        if (stack.getItem() == ModItems.t45_helmet) {
            return slot == EntityEquipmentSlot.HEAD;
        }
        if (stack.getItem() == ModItems.t45_plate) {
            return slot == EntityEquipmentSlot.CHEST;
        }
        if (stack.getItem() == ModItems.t45_legs) {
            return slot == EntityEquipmentSlot.LEGS;
        }
        if (stack.getItem() == ModItems.t45_boots) {
            return slot == EntityEquipmentSlot.FEET;
        }
        return super.isValidArmor(stack, slot, entity);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public ModelBiped getArmorModel(EntityLivingBase entityLiving, ItemStack itemStack, EntityEquipmentSlot armorSlot, ModelBiped _default) {
        if (itemStack.getItem() == ModItems.t45_helmet && armorSlot == EntityEquipmentSlot.HEAD) {
            if (this.helmet == null) {
                this.helmet = new ModelT45Helmet();
            }
            return this.helmet;
        }
        if (itemStack.getItem() == ModItems.t45_plate && armorSlot == EntityEquipmentSlot.CHEST) {
            if (this.plate == null) {
                this.plate = new ModelT45Chest();
            }
            return this.plate;
        }
        if (itemStack.getItem() == ModItems.t45_legs && armorSlot == EntityEquipmentSlot.LEGS) {
            if (this.legs == null) {
                this.legs = new ModelT45Legs();
            }
            return this.legs;
        }
        if (itemStack.getItem() == ModItems.t45_boots && armorSlot == EntityEquipmentSlot.FEET) {
            if (this.boots == null) {
                this.boots = new ModelT45Boots();
            }
            return this.boots;
        }
        return super.getArmorModel(entityLiving, itemStack, armorSlot, _default);
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EntityEquipmentSlot slot, String type) {
        if (stack.getItem() == ModItems.t45_helmet) {
            return "hbm:textures/armor/t45helmet.png";
        }
        if (stack.getItem() == ModItems.t45_plate) {
            return "hbm:textures/armor/t45chest.png";
        }
        if (stack.getItem() == ModItems.t45_legs) {
            return "hbm:textures/armor/t45legs.png";
        }
        if (stack.getItem() == ModItems.t45_boots) {
            return "hbm:textures/armor/t45boots.png";
        }
        return super.getArmorTexture(stack, entity, slot, type);
    }

}
