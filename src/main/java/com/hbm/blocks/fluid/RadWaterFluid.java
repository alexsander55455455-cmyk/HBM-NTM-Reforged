package com.hbm.blocks.fluid;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.Fluid;

import java.awt.Color;

public class RadWaterFluid extends Fluid {

	public RadWaterFluid(String name) {
		super(name,
				new ResourceLocation("minecraft", "blocks/water_still"),
				new ResourceLocation("minecraft", "blocks/water_flow"),
				new Color(0x3F7A97));
	}
}