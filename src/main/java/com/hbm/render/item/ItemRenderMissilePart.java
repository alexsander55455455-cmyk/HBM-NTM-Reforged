package com.hbm.render.item;

import com.hbm.render.misc.MissilePart;
import net.minecraft.client.Minecraft;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11; import net.minecraft.client.renderer.GlStateManager;

public class ItemRenderMissilePart extends TEISRBase {

	MissilePart part;
	
	public ItemRenderMissilePart(MissilePart part) { 
		this.part = part;
	}

	@Override
	public ModelBinding createModelBinding(Item item) {
		return bindingFullTeisr(item);
	}
	
	@Override
	public void renderByItem(ItemStack item) {
		if(part == null)
			return;

		GlStateManager.pushMatrix();
		GlStateManager.translate(-0.25, 0.25, -0.25);
		
		switch(type) {
		case THIRD_PERSON_LEFT_HAND:
		case THIRD_PERSON_RIGHT_HAND:
			GlStateManager.translate(0.2, -0.25, 0.6);
		case FIRST_PERSON_LEFT_HAND:
		case FIRST_PERSON_RIGHT_HAND:
			GlStateManager.translate(0.5, 0, 0);
			
		case GROUND:
		case HEAD:
		case FIXED:
			double s = 0.4;
			GL11.glScaled(s, s, s);
			
			/*if(part.type.name().equals(PartType.FINS.name())) {
				GlStateManager.translate(0, 0, 0);
				//GL11.glRotated(-45, 1, 0, 0);
			}*/
			
			Minecraft.getMinecraft().renderEngine.bindTexture(part.texture);
			part.model.renderAll();
			
			break;
			
		case GUI:
			
			double height = part.guiheight;
			
			if(height == 0D)
				height = 4D;
			
			double size = 10;
			double scale = size / height;
			
			GlStateManager.translate(height / 2 * scale, 0, 0);
			GlStateManager.translate(-4.1, 0.1, 0);
			
			GL11.glRotated(225, 0, 0, 1);
			GL11.glRotated(215, 1, 0, 0);
			//System.out.println(scale/0.62);
		
			//Drillgon200: Same thing as for the missles, I found 0.62 was a good scale number for one part, then divided its scale by that and got
			//this number.
			GL11.glScaled(scale/16.129032258064516, scale/16.129032258064516, scale/16.129032258064516);
			
			/*if(part.type.name().equals(PartType.FINS.name())) {
				GlStateManager.translate(0, 0, 0);
				//GL11.glRotated(-45, 1, 0, 0);
			}*/

			GlStateManager.rotate(System.currentTimeMillis() / 25 % 360, 0, -1, 0);
			Minecraft.getMinecraft().renderEngine.bindTexture(part.texture);
			part.model.renderAll();
			
			break;
		default: break;
		}
		
		GlStateManager.popMatrix();
	}
}
