package com.hbm.blocks.network;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ILookOverlay;
import com.hbm.blocks.ITooltipProvider;
import com.hbm.main.MainRegistry;
import com.hbm.tileentity.TileEntityProxyPowerPort;
import com.hbm.tileentity.network.TileEntityNetworkSeparator;
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
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent.Pre;
import net.minecraftforge.fml.common.network.internal.FMLNetworkHandler;

import java.util.ArrayList;
import java.util.List;

/** X5846 separator; the existing cable diode keeps its own ID and behavior. */
public class BlockNetworkSeparator extends BlockDummyable implements ILookOverlay, ITooltipProvider {
    public BlockNetworkSeparator(String name) { super(Material.IRON, name); }
    @Override public TileEntity createNewTileEntity(World world, int meta) {
        if (meta >= 12) return new TileEntityNetworkSeparator();
        return new TileEntityProxyPowerPort();
    }
    @Override public int[] getDimensions() { return new int[] {1, 0, 0, 0, 1, 1}; }
    @Override public int getOffset() { return 0; }
    @Override public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float x, float y, float z) {
        if (player.isSneaking()) return false;
        if (world.isRemote) {
            BlockPos core = findCore(world, pos);
            if (core == null) return false;
            FMLNetworkHandler.openGui(player, MainRegistry.instance, 0, world, core.getX(), core.getY(), core.getZ());
        }
        return true;
    }
    @Override public void addInformation(ItemStack stack, World world, List<String> tooltip, ITooltipFlag flag) { addStandardInfo(tooltip); }
    @Override public void printHook(Pre event, World world, BlockPos pos) {
        TileEntity te = findCoreTE(world, pos);
        if (!(te instanceof TileEntityNetworkSeparator separator)) return;
        List<String> text = new ArrayList<>();
        text.add(BobMathUtil.getShortNumber(separator.deltaTick[19]) + " HE/t, " + BobMathUtil.getShortNumber(separator.deltaSecond) + " HE/s");
        text.add(I18nUtil.resolveKey("separator.limit") + " " + BobMathUtil.getShortNumber(separator.configuredLimit) + " HE/t");
        text.add(I18nUtil.resolveKey("separator.priority") + " " + I18nUtil.resolveKey("separator.priority." + separator.priority.ordinal()));
        if (separator.redstone) text.add(TextFormatting.RED + I18nUtil.resolveKey("separator.redstone"));
        ILookOverlay.printGeneric(event, I18nUtil.resolveKey(getTranslationKey() + ".name"), 0xffff00, 0x404000, text);
    }
}
