package com.hbm.blocks.network;

import com.hbm.blocks.ILookOverlay;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.tileentity.network.TileEntityPipeExhaustAnchor;
import com.hbm.tileentity.network.TileEntityPneumaticPipeAnchor;
import com.hbm.util.I18nUtil;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import java.util.ArrayList;
import java.util.List;

public class NetworkPipeAnchor extends FluidPipeAnchor {

    private final boolean pneumatic;

    public NetworkPipeAnchor(String name, boolean pneumatic) {
        super(name);
        this.pneumatic = pneumatic;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return pneumatic ? new TileEntityPneumaticPipeAnchor() : new TileEntityPipeExhaustAnchor();
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return createTileEntity(world, getStateFromMeta(meta));
    }

    @Override
    public void changeTypeRecursively(World world, BlockPos pos, FluidType prevType, FluidType type, int loopsRemaining) {
        // Fixed networks do not accept fluid identifiers or propagate their settings.
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void printHook(RenderGameOverlayEvent.Pre event, World world, BlockPos pos) {
        List<String> text = new ArrayList<>();
        if (!pneumatic) {
            for (FluidType smoke : new FluidType[]{Fluids.SMOKE, Fluids.SMOKE_LEADED, Fluids.SMOKE_POISON}) {
                text.add("&[" + smoke.getColor() + "&]" + smoke.getLocalizedName());
            }
        }
        ILookOverlay.printGeneric(event, I18nUtil.resolveKey(getTranslationKey() + ".name"), 0xffff00, 0x404000, text);
    }
}
