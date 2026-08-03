package com.hbm.items.tool;

import com.hbm.items.machine.ItemSatellite;
import com.hbm.lib.Library;
import com.hbm.saveddata.satellites.Satellite;
import com.hbm.saveddata.satellites.SatelliteSavedData;
import com.hbm.saveddata.satellites.SatelliteResolver;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

public class ItemSatDesignator extends ItemSatellite {

    public ItemSatDesignator(String regName) {
        super(regName);
    }

    @Override
    public @NotNull ActionResult<ItemStack> onItemRightClick(@NotNull World world, @NotNull EntityPlayer player, @NotNull EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);

        if (!world.isRemote) {
            SatelliteResolver.Result resolution = SatelliteResolver.resolve(world,
                    (int)Math.floor(player.posX), (int)Math.floor(player.posZ), stack, true);
            SatelliteSavedData satelliteData = resolution.getData();
            Satellite sat = resolution.getSatellite();

            if (sat != null && satelliteData != null && resolution.getContext() != null
                    && resolution.getContext().getSurfaceWorld() != null) {
                RayTraceResult pos = Library.rayTrace(player, 300, 1);
                if(pos == null || pos.getBlockPos() == null || pos.sideHit == null) {
                    return new ActionResult<>(EnumActionResult.PASS, stack);
                }
                BlockPos rayBlockPos = pos.getBlockPos();

                EnumFacing facing = pos.sideHit;
                int x = rayBlockPos.getX() + facing.getXOffset();
                int y = rayBlockPos.getY() + facing.getYOffset();
                int z = rayBlockPos.getZ() + facing.getZOffset();

                if (sat.satIface == Satellite.Interfaces.SAT_COORD) {
                    sat.onCoordAction(resolution.getContext().getSurfaceWorld(), (EntityPlayerMP) player, x, y, z);
                } else if (sat.satIface == Satellite.Interfaces.SAT_PANEL) {
                    sat.onClick(resolution.getContext().getSurfaceWorld(), (EntityPlayerMP) player, x, z);
                }
                satelliteData.markSatelliteDirty();
            }
        }

        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

}
