package com.hbm.render.item;

import com.hbm.items.machine.ItemCrucibleTemplate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Keyboard;

public class CrucibleTemplateRender extends TileEntityItemStackRenderer {

    public static final CrucibleTemplateRender INSTANCE = new CrucibleTemplateRender();

    public ItemCameraTransforms.TransformType type;
    public IBakedModel itemModel;

    @Override
    public void renderByItem(ItemStack stack) {
        try {
            if (stack.getItem() instanceof ItemCrucibleTemplate && (type == null || type == ItemCameraTransforms.TransformType.GUI)) {
                if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
                    GlStateManager.pushMatrix();
                    GlStateManager.pushAttrib();
                    GlStateManager.translate(0.5F, 0.5F, 0.0F);
                    GlStateManager.enableLighting();
                    ItemStack icon = ItemCrucibleTemplate.getIcon(stack);
                    if (!icon.isEmpty()) {
                        IBakedModel model = Minecraft.getMinecraft().getRenderItem().getItemModelWithOverrides(
                                icon, Minecraft.getMinecraft().world, Minecraft.getMinecraft().player);
                        model = net.minecraftforge.client.ForgeHooksClient.handleCameraTransforms(
                                model, ItemCameraTransforms.TransformType.GUI, false);
                        Minecraft.getMinecraft().getRenderItem().renderItem(icon, model);
                    }
                    GlStateManager.popAttrib();
                    GlStateManager.popMatrix();
                } else if (itemModel != null) {
                    GlStateManager.translate(0.5F, 0.5F, 0.0F);
                    Minecraft.getMinecraft().getRenderItem().renderItem(stack, itemModel);
                }
            } else if (itemModel != null) {
                Minecraft.getMinecraft().getRenderItem().renderItem(stack, itemModel);
            }
        } catch (IndexOutOfBoundsException ignored) {
        }
    }
}