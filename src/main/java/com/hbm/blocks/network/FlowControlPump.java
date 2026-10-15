package com.hbm.blocks.network;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ILookOverlay;
import com.hbm.blocks.ITooltipProvider;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.items.machine.IItemFluidIdentifier;
import com.hbm.main.MainRegistry;
import com.hbm.tileentity.TileEntityProxyCombo;
import com.hbm.tileentity.network.TileEntityFlowControlPump;
import com.hbm.util.BobMathUtil;
import com.hbm.util.I18nUtil;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent.Pre;
import net.minecraftforge.fml.common.network.internal.FMLNetworkHandler;

import java.util.ArrayList;
import java.util.List;

/** Adds the X5846 pump under its own ID, retaining the existing fluid_pump. */
public class FlowControlPump extends BlockDummyable implements ILookOverlay, ITooltipProvider {
    public FlowControlPump(String name) { super(Material.IRON, name); }
    @Override public TileEntity createNewTileEntity(World world, int meta) {
        return meta >= 12 ? new TileEntityFlowControlPump() : new TileEntityProxyCombo().fluid();
    }
    @Override public int[] getDimensions() { return new int[] {0, 0, 0, 0, 1, 1}; }
    @Override public int getOffset() { return 0; }
    @Override public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float x, float y, float z) {
        BlockPos core = findCore(world, pos);
        if (core == null) return false;
        TileEntity tile = world.getTileEntity(core);
        if (!(tile instanceof TileEntityFlowControlPump)) return false;
        TileEntityFlowControlPump pump = (TileEntityFlowControlPump) tile;
        ItemStack held = player.getHeldItem(hand);
        if (!held.isEmpty() && held.getItem() instanceof IItemFluidIdentifier) {
            FluidType type = ((IItemFluidIdentifier) held.getItem()).getType(world, pos.getX(), pos.getY(), pos.getZ(), held);
            if (!world.isRemote) {
                pump.tank[0].setTankType(type);
                pump.markDirty();
                player.sendMessage(new TextComponentTranslation("pump.fluid_changed", new TextComponentTranslation(type.getConditionalName())));
            }
            return true;
        }
        if (player.isSneaking()) return false;
        if (world.isRemote) FMLNetworkHandler.openGui(player, MainRegistry.instance, 0, world, core.getX(), core.getY(), core.getZ());
        return true;
    }
    @Override public void addInformation(ItemStack stack, World world, List<String> tooltip, ITooltipFlag flag) { addStandardInfo(tooltip); }
    @Override public void printHook(Pre event, World world, BlockPos pos) {
        TileEntity tile = findCoreTE(world, pos);
        if (!(tile instanceof TileEntityFlowControlPump)) return;
        TileEntityFlowControlPump pump = (TileEntityFlowControlPump) tile;
        List<String> text = new ArrayList<>();
        text.add(TextFormatting.GREEN + "-> " + TextFormatting.RESET + pump.tank[0].getTankType().getLocalizedName() + " (" + pump.tank[0].getPressure() + " PU): " + BobMathUtil.format(pump.bufferSize) + " mB/t" + TextFormatting.RED + " ->");
        text.add(I18nUtil.resolveKey("separator.priority") + " " + I18nUtil.resolveKey("separator.priority." + pump.priority.ordinal()));
        if (pump.redstone) text.add(TextFormatting.RED + I18nUtil.resolveKey("separator.redstone"));
        if (pump.tank[0].getFill() > 0) text.add(I18nUtil.resolveKey("pump.buffered", BobMathUtil.format(pump.tank[0].getFill())));
        ILookOverlay.printGeneric(event, I18nUtil.resolveKey(getTranslationKey() + ".name"), 0xffff00, 0x404000, text);
    }
}
