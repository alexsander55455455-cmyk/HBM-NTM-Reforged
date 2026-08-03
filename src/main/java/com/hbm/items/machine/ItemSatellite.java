package com.hbm.items.machine;

import com.hbm.items.ISatChip;
import com.hbm.items.ItemBakedBase;
import com.hbm.items.ModItems;
import com.hbm.main.MainRegistry;
import com.hbm.saveddata.satellites.SatelliteLaunchResult;
import com.hbm.saveddata.satellites.SatelliteTypeRegistry;
import com.hbm.util.I18nUtil;
import com.hbmspace.dim.CelestialBody;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import java.util.Arrays;
import java.util.List;

public class ItemSatellite extends ItemBakedBase implements ISatChip {

	public ItemSatellite(String s) {
		super(s);
	}
	
	@Override
	public void addInformation(ItemStack stack, World worldIn, List<String> list, ITooltipFlag flagIn) {
		super.addInformation(stack, worldIn, list, flagIn);
		list.add(I18nUtil.resolveKey("desc.satellitefr", getFreq(stack)));
		SatelliteTypeRegistry.Descriptor descriptor = SatelliteTypeRegistry.byItem(stack);
		if(descriptor != null) {
			list.add(TextFormatting.GRAY + I18nUtil.resolveKey("desc.satellite.configure"));
		}

		if(this == ModItems.sat_foeq)
			list.add(I18nUtil.resolveKey("satchip.foeq"));

		if (this == ModItems.sat_gerald) {
			String[] lines = I18nUtil.resolveKeyArray("satchip.gerald.desc");
			list.addAll(Arrays.asList(lines));
		}

		if(this == ModItems.sat_laser)
			list.add(I18nUtil.resolveKey("satchip.laser"));

		if(this == ModItems.sat_mapper)
			list.add(I18nUtil.resolveKey("satchip.mapper"));

		if(this == ModItems.sat_miner)
			list.add(I18nUtil.resolveKey("satchip.miner"));

		if(this == ModItems.sat_lunar_miner)
			list.add(I18nUtil.resolveKey("satchip.lunar_miner"));

		if(this == ModItems.sat_radar)
			list.add(I18nUtil.resolveKey("satchip.radar"));

		if(this == ModItems.sat_resonator)
			list.add(I18nUtil.resolveKey("satchip.resonator"));

		if(this == ModItems.sat_scanner)
			list.add(I18nUtil.resolveKey("satchip.scanner"));

		if(descriptor != null && descriptor.canHandLaunch()) {
			list.add(TextFormatting.GOLD + I18nUtil.resolveKey("item.sat.desc.launch_by_hand"));
		}
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
}
