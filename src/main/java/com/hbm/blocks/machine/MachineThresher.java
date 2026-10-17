package com.hbm.blocks.machine;

import com.hbm.api.block.IToolable;
import com.hbm.blocks.ILookOverlay;
import com.hbm.blocks.ITooltipProvider;
import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.items.machine.IItemFluidIdentifier;
import com.hbm.tileentity.machine.TileEntityMachineThresher;
import com.hbm.util.I18nUtil;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.*;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent.Pre;
import java.util.ArrayList;
import java.util.List;

public class MachineThresher extends BlockContainer implements ILookOverlay, ITooltipProvider, IToolable {
    public static final PropertyDirection FACING = BlockHorizontal.FACING;

    public MachineThresher(String name) {
        super(Material.IRON);
        setRegistryName(name);
        setTranslationKey(name);
        setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
        ModBlocks.ALL_BLOCKS.add(this);
    }

    @Override public TileEntity createNewTileEntity(World world, int meta) { return new TileEntityMachineThresher(); }
    @Override protected BlockStateContainer createBlockState() { return new BlockStateContainer(this, FACING); }
    @Override public int getMetaFromState(IBlockState state) { return state.getValue(FACING).getIndex(); }
    @Override public IBlockState getStateFromMeta(int meta) {
        EnumFacing facing = EnumFacing.byIndex(meta);
        return getDefaultState().withProperty(FACING, facing.getAxis() == EnumFacing.Axis.Y ? EnumFacing.NORTH : facing);
    }
    @Override public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        world.setBlockState(pos, state.withProperty(FACING, EnumFacing.fromAngle(placer.rotationYaw)), 2);
    }
    @Override public EnumBlockRenderType getRenderType(IBlockState state) { return EnumBlockRenderType.ENTITYBLOCK_ANIMATED; }
    @Override public boolean isOpaqueCube(IBlockState state) { return false; }
    @Override public boolean isFullCube(IBlockState state) { return false; }

    @Override public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side, float x, float y, float z) {
        if(world.isRemote || player.isSneaking()) return true;
        ItemStack held = player.getHeldItem(hand);
        if(!held.isEmpty() && held.getItem() instanceof IItemFluidIdentifier) {
            FluidType type = ((IItemFluidIdentifier) held.getItem()).getType(world, pos.getX(), pos.getY(), pos.getZ(), held);
            TileEntity tile = world.getTileEntity(pos);
            if(tile instanceof TileEntityMachineThresher && TileEntityMachineThresher.acceptsFuel(type)) {
                ((TileEntityMachineThresher) tile).tank.setTankType(type);
                tile.markDirty();
                player.sendMessage(new TextComponentString("Changed type to ").setStyle(new Style().setColor(TextFormatting.YELLOW))
                        .appendSibling(new TextComponentTranslation(type.getConditionalName())).appendSibling(new TextComponentString("!")));
                return true;
            }
        }
        return false;
    }

    @Override public boolean onScrew(World world, EntityPlayer player, int x, int y, int z, EnumFacing side, float hitX, float hitY, float hitZ, EnumHand hand, ToolType tool) {
        if(tool != ToolType.SCREWDRIVER) return false;
        TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));
        if(!(tile instanceof TileEntityMachineThresher)) return false;
        TileEntityMachineThresher thresher = (TileEntityMachineThresher) tile;
        thresher.isSuspended = !thresher.isSuspended;
        thresher.markDirty();
        return true;
    }

    @Override public void printHook(Pre event, World world, BlockPos pos) {
        TileEntity tile = world.getTileEntity(pos);
        if(!(tile instanceof TileEntityMachineThresher)) return;
        TileEntityMachineThresher thresher = (TileEntityMachineThresher) tile;
        List<String> text = new ArrayList<>();
        text.add(thresher.tank.getTankType().getLocalizedName() + ": " + thresher.tank.getFill() + "/" + thresher.tank.getMaxFill() + "mB");
        if(thresher.isSuspended) text.add(TextFormatting.RED + "! " + I18nUtil.resolveKey(getTranslationKey() + ".suspended") + " !");
        ILookOverlay.printGeneric(event, I18nUtil.resolveKey(getTranslationKey() + ".name"), 0xffff00, 0x404000, text);
    }
    @Override public void addInformation(ItemStack stack, World world, List<String> tooltip, ITooltipFlag flag) { addStandardInfo(tooltip); }
}
