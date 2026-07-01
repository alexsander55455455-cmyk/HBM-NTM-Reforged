package com.hbm.render.tileentity;

import com.hbm.blocks.ModBlocks;
import com.hbm.interfaces.AutoRegister;
import com.hbm.tileentity.TileEntityKeypadBase;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.util.math.BlockPos;
@AutoRegister
public class RenderKeypadBase extends TileEntitySpecialRenderer<TileEntityKeypadBase> {

	@Override
	public void render(TileEntityKeypadBase te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
		if (te == null || te.isInvalid() || te.getWorld() == null) {
			return;
		}
		BlockPos pos = te.getPos();
		Block block = te.getWorld().getBlockState(pos).getBlock();
		if (block != ModBlocks.sliding_blast_door_keypad) {
			return;
		}
		GlStateManager.pushMatrix();
		GlStateManager.translate(x+0.5, y, z+0.5);
		te.keypad.client().render();
		GlStateManager.popMatrix();
	}
}
