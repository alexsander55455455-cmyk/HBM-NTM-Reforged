package com.hbm.render.item.weapon;

import com.hbm.main.ResourceManager;
import com.hbm.render.item.TEISRBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

public class ItemRenderBrimstone extends TEISRBase {

    @Override
    public ModelBinding createModelBinding(Item item) {
        return bindingFullTeisr(item);
    }

    @Override
    public boolean useRegistryPerspective(Item item) {
        return true;
    }

    @Override
    public boolean doNullTransform() {
        return type == TransformType.GUI;
    }

    @Override
    public void renderByItem(ItemStack stack) {
        GL11.glPopMatrix();
        Minecraft.getMinecraft().getTextureManager().bindTexture(ResourceManager.brimstone_tex);

        switch (type) {
            case FIRST_PERSON_LEFT_HAND:
                GL11.glTranslated(2.0D, -0.15D, 0.0D);
                GL11.glRotated(-90.0D, 0.0D, 1.0D, 0.0D);
                GL11.glRotated(40.0D, 1.0D, 0.0D, 0.0D);
                break;
            case FIRST_PERSON_RIGHT_HAND:
                GL11.glTranslated(-0.9D, -0.35D, 0.0D);
                GL11.glRotated(90.0D, 0.0D, 1.0D, 0.0D);
                GL11.glRotated(40.0D, 1.0D, 0.0D, 0.0D);
                break;
            case HEAD:
            case FIXED:
            case GROUND:
            case THIRD_PERSON_LEFT_HAND:
            case THIRD_PERSON_RIGHT_HAND:
                GL11.glTranslated(0.5D, -0.75D, -0.85D);
                break;
            case GUI:
                GlStateManager.enableLighting();
                GL11.glTranslated(0.5D, 0.42D, 0.5D);
                GL11.glScaled(0.16D, 0.16D, 0.16D);
                GL11.glTranslated(0.0D, -0.85D, 0.0D);
                GL11.glRotated(-90.0D, 0.0D, 1.0D, 0.0D);
                GL11.glRotated(-40.0D, 1.0D, 0.0D, 0.0D);
                break;
            default:
                break;
        }

        GlStateManager.shadeModel(7425);
        GlStateManager.disableCull();
        ResourceManager.brimstone.renderAll();
        GlStateManager.enableCull();
        GlStateManager.shadeModel(7424);

        GL11.glPushMatrix();
    }
}