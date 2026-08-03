package com.hbm.items.tool;

import com.hbm.capability.BackpackCapability;
import com.hbm.util.I18nUtil;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.enchantment.EnchantmentProtection;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Explosion;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.world.ExplosionEvent;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;

public class ItemSapperBackpack extends ItemBackpack {

    public static final int SLOTS = 55;
    private static final float SMALL_BLAST_DAMAGE = 60F;
    private static final float HEAVY_BLAST_DAMAGE = 160F;
    private static final float EXTREME_BLAST_DAMAGE = 300F;
    private static final ThreadLocal<Boolean> HANDLING_VANILLA_EXPLOSION =
            ThreadLocal.withInitial(() -> false);

    public ItemSapperBackpack(String name) {
        super(name, SLOTS, 0.80D, false);
        setCreativeTab(null);
    }

    @Override
    public boolean protectsDroppedItemDamage(DamageSource source, float amount) {
        return source != null && source.isExplosion();
    }

    /**
     * Replaces vanilla explosion handling only for a player wearing this
     * backpack, so damage and knockback use the same strength-dependent scale.
     */
    public static void handleExplosionDetonation(ExplosionEvent.Detonate event) {
        World world = event.getWorld();
        if (world.isRemote) return;

        Explosion explosion = event.getExplosion();
        double diameter = explosion.size * 2D;
        if (diameter <= 0D) return;

        Vec3d center = explosion.getPosition();
        Iterator<Entity> entities = event.getAffectedEntities().iterator();
        while (entities.hasNext()) {
            Entity entity = entities.next();
            if (!(entity instanceof EntityPlayer player)
                    || entity.isImmuneToExplosions()
                    || !hasEquippedSapper(player)) {
                continue;
            }

            double normalizedDistance = entity.getDistance(
                    center.x, center.y, center.z) / diameter;
            if (normalizedDistance > 1D) continue;

            double directionX = entity.posX - center.x;
            double directionY = entity.posY + entity.getEyeHeight() - center.y;
            double directionZ = entity.posZ - center.z;
            double directionLength = MathHelper.sqrt(
                    directionX * directionX + directionY * directionY + directionZ * directionZ);
            if (directionLength == 0D) continue;

            directionX /= directionLength;
            directionY /= directionLength;
            directionZ /= directionLength;
            double density = world.getBlockDensity(center, entity.getEntityBoundingBox());
            double exposure = (1D - normalizedDistance) * density;
            float unprotectedDamage = (float) ((int) (
                    (exposure * exposure + exposure) * 0.5D * 7D * diameter + 1D));
            float protectionMultiplier = getExplosionMultiplier(unprotectedDamage);

            HANDLING_VANILLA_EXPLOSION.set(true);
            try {
                entity.attackEntityFrom(
                        DamageSource.causeExplosionDamage(explosion),
                        unprotectedDamage * protectionMultiplier);
            } finally {
                HANDLING_VANILLA_EXPLOSION.set(false);
            }

            double knockback = EnchantmentProtection.getBlastDamageReduction(
                    player, exposure) * protectionMultiplier;
            entity.motionX += directionX * knockback;
            entity.motionY += directionY * knockback;
            entity.motionZ += directionZ * knockback;

            if (!player.isSpectator()
                    && (!player.isCreative() || !player.capabilities.isFlying)) {
                explosion.getPlayerKnockbackMap().put(player, new Vec3d(
                        directionX * exposure * protectionMultiplier,
                        directionY * exposure * protectionMultiplier,
                        directionZ * exposure * protectionMultiplier));
            }
            entities.remove();
        }
    }

    /** Covers HBM/custom explosion damage paths that do not create a vanilla ExplosionEvent. */
    public static void handleExplosionDamage(LivingHurtEvent event) {
        if (HANDLING_VANILLA_EXPLOSION.get()
                || !event.getSource().isExplosion()
                || !(event.getEntityLiving() instanceof EntityPlayer player)
                || !hasEquippedSapper(player)) {
            return;
        }
        event.setAmount(event.getAmount() * getExplosionMultiplier(event.getAmount()));
    }

    static float getExplosionMultiplier(float damage) {
        if (!Float.isFinite(damage) || damage <= 0F) return 1F;
        if (damage <= SMALL_BLAST_DAMAGE) return 0.05F;
        if (damage <= HEAVY_BLAST_DAMAGE) {
            return interpolate(damage, SMALL_BLAST_DAMAGE, HEAVY_BLAST_DAMAGE, 0.05F, 0.75F);
        }
        if (damage <= EXTREME_BLAST_DAMAGE) {
            return interpolate(damage, HEAVY_BLAST_DAMAGE, EXTREME_BLAST_DAMAGE, 0.75F, 1F);
        }
        return 1F;
    }

    private static float interpolate(float value, float start, float end, float from, float to) {
        float fraction = (value - start) / (end - start);
        return from + (to - from) * fraction;
    }

    private static boolean hasEquippedSapper(EntityPlayer player) {
        return BackpackCapability.getData(player).getEquippedBackpack().getItem()
                instanceof ItemSapperBackpack;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        super.addInformation(stack, world, tooltip, flag);
        tooltip.add(TextFormatting.AQUA + I18nUtil.resolveKey("desc.backpack.sapper.dropped_explosion_proof"));
        tooltip.add(TextFormatting.DARK_GRAY + I18nUtil.resolveKey("desc.backpack.sapper.player_protection"));
    }
}
