package com.hbm.blocks.machine.pile;

import com.hbm.api.block.IToolable;
import com.hbm.blocks.ILookOverlay;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.generic.BlockMeta;
import com.hbm.tileentity.machine.pile.TileEntityPileControl;
import com.hbm.tileentity.machine.pile.TileEntityPileCore;
import com.hbm.tileentity.machine.pile.TileEntityPileLoader;
import com.hbm.tileentity.machine.pile.TileEntityPileVent;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;

public class BlockPileDevice extends BlockMeta implements ILookOverlay, IToolable {

    public static final int ITEM_META_LOADER = 0;
    public static final int ITEM_META_VENT = 1;
    public static final int ITEM_META_CONTROL = 2;
    public static final int BLOCK_META_LOADER = 0;
    public static final int BLOCK_META_VENT = 4;
    public static final int BLOCK_META_CONTROL = 8;

    public BlockPileDevice(String name) {
        super(Material.IRON, name, (short) 3, true);
    }

    @Override
    protected boolean useSpecialRenderer() {
        return true;
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        int baseMeta = state.getValue(META);
        baseMeta -= baseMeta % 4;
        if (baseMeta == BLOCK_META_LOADER) return new TileEntityPileLoader();
        if (baseMeta == BLOCK_META_VENT) return new TileEntityPileVent();
        if (baseMeta == BLOCK_META_CONTROL) return new TileEntityPileControl();
        return null;
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing clickedSide,
                                            float hitX, float hitY, float hitZ, int itemMeta,
                                            EntityLivingBase placer, EnumHand hand) {
        EnumFacing facing = clickedSide.getAxis().isHorizontal() ? clickedSide :
                placer.getHorizontalFacing().getOpposite();
        return getDefaultState().withProperty(META, itemMetaToBlockMeta(itemMeta) + facing.getIndex() - 2);
    }

    @Override
    public int damageDropped(IBlockState state) {
        return blockMetaToItemMeta(state.getValue(META));
    }

    public static int blockMetaToItemMeta(int meta) {
        if (meta >= BLOCK_META_CONTROL) return ITEM_META_CONTROL;
        if (meta >= BLOCK_META_VENT) return ITEM_META_VENT;
        return ITEM_META_LOADER;
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos,
                                  EntityPlayer player) {
        return new ItemStack(Item.getItemFromBlock(this), 1, damageDropped(state));
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos,
                         IBlockState state, int fortune) {
        drops.add(new ItemStack(Item.getItemFromBlock(this), 1, damageDropped(state)));
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                    EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (player.isSneaking()) return false;
        int baseMeta = state.getValue(META);
        baseMeta -= baseMeta % 4;
        if (baseMeta != BLOCK_META_LOADER) return false;
        if (world.isRemote) return true;

        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof TileEntityPileLoader)) return true;
        TileEntityPileLoader loader = (TileEntityPileLoader) tile;
        if (loader.level <= 0D && !loader.loading) {
            ItemStack held = player.getHeldItem(hand);
            if (!held.isEmpty() && loader.getStack().isEmpty() && TileEntityPileLoader.isItemLoadable(held)) {
                ItemStack inserted = held.copy();
                inserted.setCount(1);
                loader.setStack(inserted);
                if (!player.capabilities.isCreativeMode) held.shrink(1);
                world.playSound(null, pos, SoundEvents.BLOCK_PISTON_EXTEND, SoundCategory.BLOCKS, 1F, 1F);
                return true;
            }
            loader.loading = true;
            loader.markDirty();
        }
        return true;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.INVISIBLE;
    }

    @Override
    public boolean isSideSolid(IBlockState baseState, IBlockAccess world, BlockPos pos, EnumFacing side) {
        int meta = baseState.getValue(META);
        if (meta >= BLOCK_META_CONTROL || meta < BLOCK_META_VENT) {
            return side.getIndex() == meta % 4 + 2;
        }
        return false;
    }

    public static int itemMetaToBlockMeta(int meta) {
        if (meta >= ITEM_META_CONTROL) return BLOCK_META_CONTROL;
        if (meta == ITEM_META_VENT) return BLOCK_META_VENT;
        return BLOCK_META_LOADER;
    }

    public String getTranslationKey(int meta) {
        if (meta >= ITEM_META_CONTROL) return getTranslationKey() + ".control";
        if (meta == ITEM_META_VENT) return getTranslationKey() + ".vent";
        return getTranslationKey() + ".loader";
    }

    @Override
    public boolean onScrew(World world, EntityPlayer player, int x, int y, int z, EnumFacing side,
                           float hitX, float hitY, float hitZ, EnumHand hand, ToolType tool) {
        BlockPos devicePos = new BlockPos(x, y, z);
        int meta = getMetaFromState(world.getBlockState(devicePos));
        BlockPos channelPos;
        EnumFacing drillSide;
        if (meta >= BLOCK_META_CONTROL) {
            channelPos = devicePos.down();
            drillSide = EnumFacing.UP;
        } else {
            EnumFacing orientation = EnumFacing.byIndex(meta % 4 + 2);
            channelPos = devicePos.offset(orientation.getOpposite());
            drillSide = orientation;
        }
        if (world.getBlockState(channelPos).getBlock() == ModBlocks.pile_block) {
            return ((BlockPile) ModBlocks.pile_block).onScrew(world, player, channelPos.getX(),
                    channelPos.getY(), channelPos.getZ(), drillSide, hitX, hitY, hitZ, hand, tool);
        }
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void printHook(RenderGameOverlayEvent.Pre event, World world, BlockPos pos) {
        List<String> text = new ArrayList<>();
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileEntityPileLoader) {
            TileEntityPileLoader loader = (TileEntityPileLoader) tile;
            text.add("Temp: " + Math.round(loader.channelTemp) + " / " + TileEntityPileCore.MAX_HEAT + "°C");
            if (!loader.syncStack.isEmpty()) text.add("Loading: " + loader.syncStack.getDisplayName());
            if (!loader.channelStack.isEmpty()) {
                text.add("Last rod: " + loader.channelStack.getDisplayName());
                if (loader.channelDepletion > 0D) {
                    text.add("Depletion: " + Math.round(loader.channelDepletion) + "%");
                }
            }
        } else if (tile instanceof TileEntityPileControl) {
            text.add("Extraction level: " + Math.round(((TileEntityPileControl) tile).level * 100D) + "%");
        }
        if (!text.isEmpty()) {
            int itemMeta = damageDropped(world.getBlockState(pos));
            ILookOverlay.printGeneric(event, I18n.format(getTranslationKey(itemMeta) + ".name"),
                    0xffff00, 0x404000, text);
        }
    }
}
