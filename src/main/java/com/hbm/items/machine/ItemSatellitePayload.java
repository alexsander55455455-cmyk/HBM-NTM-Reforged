package com.hbm.items.machine;

import com.hbm.items.ISatChip;
import com.hbm.items.ItemEnumMulti;
import com.hbm.saveddata.satellites.SatelliteLaunchResult;
import com.hbm.saveddata.satellites.SatelliteTypeRegistry;
import com.hbm.util.I18nUtil;
import com.hbm.main.MainRegistry;
import com.hbmspace.dim.CelestialBody;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Consolidated satellite payload item used by the current satellite registry.
 *
 * <p>The metadata order intentionally matches the official 1.7.10 item. Do not
 * reorder these values: recipes, old worlds and rocket payload NBT depend on it.</p>
 */
public class ItemSatellitePayload extends ItemEnumMulti<ItemSatellitePayload.EnumSatelliteType> implements ISatChip {

    public ItemSatellitePayload(String registryName) {
        super(registryName, EnumSatelliteType.values(), true, true);
        setMaxStackSize(1);
    }

    public enum EnumSatelliteType {
        SPY,
        SCANNER,
        RADAR,
        MINER_ASTRO,
        MINER_LUNAR,
        PRECISION_LASER,
        DEATH_RAY,
        XENIUM_RESONATOR,
        RELAY,
        DETECTOR,
        RAY_SCAN
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        SatelliteTypeRegistry.Descriptor descriptor = SatelliteTypeRegistry.byItem(stack);
        if(descriptor == null) return new ActionResult<>(EnumActionResult.PASS, stack);

        if(player.isSneaking()) {
            if(world.isRemote) MainRegistry.proxy.openSatelliteOrbitSettings(hand);
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }

        if(!descriptor.canHandLaunch() || !CelestialBody.inOrbit(world)) {
            return new ActionResult<>(EnumActionResult.PASS, stack);
        }
        if(world.isRemote) return new ActionResult<>(EnumActionResult.SUCCESS, stack);

        SatelliteLaunchResult result = SatelliteTypeRegistry.orbit(
                world, stack.copy(), getFreq(stack), player.posX, player.posY, player.posZ, null);
        if(!result.isSuccess()) {
            player.sendMessage(new TextComponentString(TextFormatting.RED + "Satellite launch failed: " + result.name()));
            return new ActionResult<>(EnumActionResult.FAIL, stack);
        }
        if(!player.capabilities.isCreativeMode) stack.shrink(1);
        player.sendMessage(new TextComponentString("Satellite launched successfully!"));
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        tooltip.add(I18nUtil.resolveKey("desc.satellitefr", getFreq(stack)));
        SatelliteTypeRegistry.Descriptor descriptor = SatelliteTypeRegistry.byItem(stack);
        if(descriptor != null) {
            tooltip.add(TextFormatting.GRAY + I18nUtil.resolveKey("desc.satellite.configure"));
            tooltip.add(TextFormatting.DARK_GRAY + "Mass: " + descriptor.getMass() + " kg");
            if(descriptor.canHandLaunch()) {
                tooltip.add(TextFormatting.GOLD + I18nUtil.resolveKey("item.sat.desc.launch_by_hand"));
            }
        }
    }
}
