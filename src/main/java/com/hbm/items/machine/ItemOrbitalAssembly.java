package com.hbm.items.machine;

import com.hbm.items.ISatChip;
import com.hbm.items.ItemEnumMulti;
import com.hbm.saveddata.satellites.SatelliteLaunchResult;
import com.hbm.saveddata.satellites.SatelliteTypeRegistry;
import com.hbm.util.I18nUtil;
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

public class ItemOrbitalAssembly extends ItemEnumMulti<ItemOrbitalAssembly.EnumOrbitalAssembly> implements ISatChip {
    public enum EnumOrbitalAssembly { CRYSTAL_CIRCUIT }

    public ItemOrbitalAssembly(String name) {
        super(name, EnumOrbitalAssembly.values(), true, "orbital_assembly");
        setMaxStackSize(1);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if(player.isSneaking() || !CelestialBody.inOrbit(world)) return new ActionResult<>(EnumActionResult.PASS, stack);
        if(world.isRemote) return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        SatelliteLaunchResult result = SatelliteTypeRegistry.orbit(world, stack.copy(), getFreq(stack), player.posX, player.posY, player.posZ, null);
        if(!result.isSuccess()) {
            player.sendMessage(new TextComponentString(TextFormatting.RED + "Satellite launch failed: " + result.name()));
            return new ActionResult<>(EnumActionResult.FAIL, stack);
        }
        if(!player.capabilities.isCreativeMode) stack.shrink(1);
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        tooltip.add(I18nUtil.resolveKey("desc.satellitefr", getFreq(stack)));
        tooltip.add(TextFormatting.GRAY + I18nUtil.resolveKey("desc.orbital_assembly"));
        tooltip.add(TextFormatting.DARK_GRAY + "Mass: " + SatelliteTypeRegistry.getPayloadMass(stack) + " kg");
        tooltip.add(TextFormatting.GOLD + I18nUtil.resolveKey("item.sat.desc.launch_by_hand"));
    }
}
