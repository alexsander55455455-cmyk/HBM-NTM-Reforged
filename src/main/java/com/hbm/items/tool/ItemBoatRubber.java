package com.hbm.items.tool;

import com.hbm.entity.item.EntityBoatRubber;
import com.hbm.items.ModItems;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

public class ItemBoatRubber extends Item {

    public ItemBoatRubber(String s) {
        this.maxStackSize = 1;
        this.setTranslationKey(s);
        this.setRegistryName(s);
        this.setCreativeTab(CreativeTabs.TRANSPORTATION);
        ModItems.ALL_ITEMS.add(this);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        float pitch = player.prevRotationPitch + (player.rotationPitch - player.prevRotationPitch) * 1.0F;
        float yaw = player.prevRotationYaw + (player.rotationYaw - player.prevRotationYaw) * 1.0F;
        double posX = player.prevPosX + (player.posX - player.prevPosX) * 1.0D;
        double posY = player.prevPosY + (player.posY - player.prevPosY) * 1.0D + player.getEyeHeight();
        double posZ = player.prevPosZ + (player.posZ - player.prevPosZ) * 1.0D;
        Vec3d start = new Vec3d(posX, posY, posZ);
        float compZ = MathHelper.cos(-yaw * 0.017453292F - (float) Math.PI);
        float compX = MathHelper.sin(-yaw * 0.017453292F - (float) Math.PI);
        float mult = -MathHelper.cos(-pitch * 0.017453292F);
        float lookY = MathHelper.sin(-pitch * 0.017453292F);
        float lookX = compX * mult;
        float lookZ = compZ * mult;
        Vec3d end = start.add(lookX * 5.0D, lookY * 5.0D, lookZ * 5.0D);
        RayTraceResult mop = world.rayTraceBlocks(start, end, true);

        if (mop == null) {
            return new ActionResult<>(EnumActionResult.PASS, stack);
        }

        Vec3d look = player.getLook(1.0F);
        boolean blocked = false;
        List<Entity> entities = world.getEntitiesWithinAABBExcludingEntity(player,
                player.getEntityBoundingBox().expand(look.x * 5.0D, look.y * 5.0D, look.z * 5.0D).grow(1.0D));

        for (Entity entity : entities) {
            if (entity.canBeCollidedWith()) {
                AxisAlignedBB box = entity.getEntityBoundingBox().grow(entity.getCollisionBorderSize());
                if (box.contains(start)) {
                    blocked = true;
                    break;
                }
            }
        }

        if (blocked) {
            return new ActionResult<>(EnumActionResult.PASS, stack);
        }

        if (mop.typeOfHit != RayTraceResult.Type.BLOCK) {
            return new ActionResult<>(EnumActionResult.PASS, stack);
        }

        Block block = world.getBlockState(mop.getBlockPos()).getBlock();
        boolean onWater = block == Blocks.WATER || block == Blocks.FLOWING_WATER;
        EntityBoatRubber boat = new EntityBoatRubber(world,
                mop.hitVec.x,
                onWater ? mop.hitVec.y - 0.12D : mop.hitVec.y,
                mop.hitVec.z);
        boat.rotationYaw = player.rotationYaw;

        if (!world.getCollisionBoxes(boat, boat.getEntityBoundingBox().grow(-0.1D)).isEmpty()) {
            return new ActionResult<>(EnumActionResult.FAIL, stack);
        }

        if (!world.isRemote) {
            world.spawnEntity(boat);
        }

        if (!player.capabilities.isCreativeMode) {
            stack.shrink(1);
        }

        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }
}