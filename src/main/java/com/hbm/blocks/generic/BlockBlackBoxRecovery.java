package com.hbm.blocks.generic;

import com.hbm.blocks.BlockContainerBakeableNormal;
import com.hbm.blocks.ICustomBlockItem;
import com.hbm.main.MainRegistry;
import com.hbm.render.block.BlockBakeFrame;
import com.hbm.tileentity.machine.TileEntityBlackBoxRecovery;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.internal.FMLNetworkHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

/**
 * A transient, non-obtainable recovery block created for a Black Box backpack
 * after its owner dies. The backpack is the only thing this block can drop.
 */
public final class BlockBlackBoxRecovery extends BlockContainerBakeableNormal implements ICustomBlockItem {

    public BlockBlackBoxRecovery(String registryName) {
        super(
                Material.IRON,
                registryName,
                BlockBakeFrame.cubeBottomTop(
                        "black_box_recovery_top",
                        "black_box_recovery_side",
                        "black_box_recovery_top"
                )
        );
        setHardness(4.0F);
        setResistance(6_000_000.0F);
        setSoundType(SoundType.METAL);
        setCreativeTab(null);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityBlackBoxRecovery();
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                    EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (player.isSneaking()) {
            return false;
        }
        if (!world.isRemote) {
            TileEntity tile = world.getTileEntity(pos);
            if (!(tile instanceof TileEntityBlackBoxRecovery recovery) || !recovery.isUsableByPlayer(player)) {
                player.sendStatusMessage(new TextComponentTranslation("message.backpack.black_box.access_denied"), true);
                return true;
            }
            FMLNetworkHandler.openGui(
                    player,
                    MainRegistry.instance,
                    0,
                    world,
                    pos.getX(),
                    pos.getY(),
                    pos.getZ()
            );
        }
        return true;
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        if (!world.isRemote) {
            TileEntity tile = world.getTileEntity(pos);
            if (tile instanceof TileEntityBlackBoxRecovery recovery) {
                ItemStack backpack = recovery.takeBackpackForBlockRemoval();
                if (!backpack.isEmpty()) {
                    EntityItem drop = new EntityItem(
                            world,
                            pos.getX() + 0.5D,
                            pos.getY() + 0.5D,
                            pos.getZ() + 0.5D,
                            backpack
                    );
                    drop.setDefaultPickupDelay();
                    world.spawnEntity(drop);
                }
            }
        }
        super.breakBlock(world, pos, state);
    }

    /**
     * Ordinary explosions do not remove the recovery container.
     */
    @Override
    public void onBlockExploded(World world, BlockPos pos, Explosion explosion) {
    }

    @Override
    public boolean isFlammable(IBlockAccess world, BlockPos pos, EnumFacing face) {
        return false;
    }

    @Override
    public int getFlammability(IBlockAccess world, BlockPos pos, EnumFacing face) {
        return 0;
    }

    @Override
    public int getFireSpreadSpeed(IBlockAccess world, BlockPos pos, EnumFacing face) {
        return 0;
    }

    @Override
    public EnumPushReaction getPushReaction(IBlockState state) {
        return EnumPushReaction.BLOCK;
    }

    @Override
    public boolean canEntityDestroy(IBlockState state, IBlockAccess world, BlockPos pos, Entity entity) {
        return false;
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state,
                         int fortune) {
        // The stored backpack is transferred by breakBlock. The block itself
        // deliberately has no obtainable item form and never enters this list.
    }

    @Override
    public int quantityDropped(Random random) {
        return 0;
    }

    @Override
    public Item getItemDropped(IBlockState state, Random random, int fortune) {
        return Items.AIR;
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos,
                                  EntityPlayer player) {
        return ItemStack.EMPTY;
    }

    /**
     * Suppresses the default ItemBlock registration. The death handler is the
     * only supported way to place this block.
     */
    @Override
    public void registerItem() {
    }

    /**
     * There is no ItemBlock, so registering an inventory model would
     * accidentally target minecraft:air.
     */
    @Override
    @SideOnly(Side.CLIENT)
    public void registerModel() {
    }
}
