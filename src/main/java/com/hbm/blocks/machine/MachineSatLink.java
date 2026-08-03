package com.hbm.blocks.machine;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ILookOverlay;
import com.hbm.items.ISatChip;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.tileentity.TileEntityProxyCombo;
import com.hbm.tileentity.machine.TileEntityMachineSatLink;
import com.hbm.util.I18nUtil;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

import java.util.ArrayList;
import java.util.List;

/** Dedicated RoR satellite ground station. The legacy ID manager remains separate. */
public class MachineSatLink extends BlockDummyable implements ILookOverlay {

    public MachineSatLink(String name) {
        super(Material.IRON, name);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        if(meta >= 12) return new TileEntityMachineSatLink();
        if(meta >= 6) return new TileEntityProxyCombo(false, false, false);
        return null;
    }

    @Override public int[] getDimensions() { return new int[] {6, 0, 1, 0, 1, 0}; }
    @Override public int getOffset() { return 0; }

    @Override
    protected void fillSpace(World world, int x, int y, int z, ForgeDirection dir, int offset) {
        super.fillSpace(world, x, y, z, dir, offset);
        ForgeDirection right = dir.getRotation(ForgeDirection.UP);
        makeExtra(world, x - dir.offsetX, y, z - dir.offsetZ);
        makeExtra(world, x + right.offsetX, y, z + right.offsetZ);
        makeExtra(world, x - dir.offsetX + right.offsetX, y, z - dir.offsetZ + right.offsetZ);
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                    EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        ItemStack held = player.getHeldItem(hand);
        if(player.isSneaking() || held.isEmpty() || !(held.getItem() instanceof ISatChip)) return false;
        if(world.isRemote) return true;
        BlockPos corePos = findCore(world, pos);
        if(corePos == null) return false;
        TileEntity tile = world.getTileEntity(corePos);
        if(!(tile instanceof TileEntityMachineSatLink)) return false;
        TileEntityMachineSatLink link = (TileEntityMachineSatLink) tile;
        link.setLink(held);
        player.sendMessage(new TextComponentString("Set frequency to " + link.getFrequency())
                .setStyle(new Style().setColor(TextFormatting.YELLOW)));
        world.playSound(null, corePos, HBMSoundHandler.techBleep, SoundCategory.BLOCKS, 1F, 1F);
        return true;
    }

    @Override
    public void printHook(RenderGameOverlayEvent.Pre event, World world, BlockPos pos) {
        BlockPos corePos = findCore(world, pos);
        if(corePos == null) return;
        TileEntity tile = world.getTileEntity(corePos);
        if(!(tile instanceof TileEntityMachineSatLink)) return;
        TileEntityMachineSatLink link = (TileEntityMachineSatLink) tile;
        List<String> text = new ArrayList<>();
        text.add("Freq: " + link.getFrequency());
        text.add("Connected: " + (link.connected ? TextFormatting.GREEN + "Yes" : TextFormatting.RED + "No"));
        ILookOverlay.printGeneric(event, I18nUtil.resolveKey(getTranslationKey() + ".name"),
                0xffff00, 0x404000, text);
    }
}
