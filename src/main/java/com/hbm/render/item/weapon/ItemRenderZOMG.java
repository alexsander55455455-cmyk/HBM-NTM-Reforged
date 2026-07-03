package com.hbm.render.item.weapon;

import com.hbm.Tags;
import com.hbm.render.item.TEISRBase;
import com.hbm.render.model.BakedModelTransforms;
import com.hbm.render.model.ModelZOMG;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class ItemRenderZOMG extends TEISRBase {

    private static final ResourceLocation TEXTURE = new ResourceLocation(Tags.MODID, "textures/models/weapons/modelzomg.png");

    private final ModelZOMG model = new ModelZOMG();

    @Override
    public ModelBinding createModelBinding(Item item) {
        return ModelBinding.inventoryWithGuiModel(item, BakedModelTransforms.defaultItemTransforms());
    }

    @Override
    public void renderByItem(ItemStack stack) {
        GL11.glPopMatrix();
        GlStateManager.enableRescaleNormal();
        GlStateManager.disableCull();
        Minecraft.getMinecraft().getTextureManager().bindTexture(TEXTURE);

        try {
            switch (type) {
                case FIRST_PERSON_LEFT_HAND:
                    GL11.glTranslated(-0.5D, 0.0D, -0.5D);
                    GL11.glScaled(1.4D, 1.4D, 1.4D);
                    // fall through, matching EE's left-hand transform stack
                case FIRST_PERSON_RIGHT_HAND:
                    GL11.glScaled(0.75D, 0.75D, 0.75D);
                    GL11.glTranslated(0.4D, 0.3D, 0.6D);
                    GL11.glRotated(180.0D, 1.0D, 0.0D, 0.0D);
                    GL11.glRotated(30.0D, 0.0D, 0.0D, 1.0D);
                    if (type == TransformType.FIRST_PERSON_LEFT_HAND) {
                        GL11.glRotated(120.0D, 0.0D, 0.0D, 1.0D);
                        GL11.glRotated(180.0D, 1.0D, 0.0D, 0.0D);
                    }
                    model.render(null, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0625F);
                    break;
                case THIRD_PERSON_LEFT_HAND:
                case THIRD_PERSON_RIGHT_HAND:
                case HEAD:
                case GROUND:
                case FIXED:
                    GL11.glTranslated(-0.2D, 0.0D, 0.0D);
                    GL11.glRotated(90.0D, 0.0D, 1.0D, 0.0D);
                    GL11.glRotated(180.0D, 0.0D, 0.0D, 1.0D);
                    GL11.glScaled(1.5D, 1.5D, 1.5D);
                    model.render(null, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0625F);
                    break;
                default:
                    break;
            }
        } finally {
            GlStateManager.enableCull();
            GL11.glPushMatrix();
        }
    }
}
