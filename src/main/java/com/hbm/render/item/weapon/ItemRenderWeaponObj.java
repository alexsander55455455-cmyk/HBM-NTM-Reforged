package com.hbm.render.item.weapon;

import com.hbm.Tags;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.ItemGunBase;
import com.hbm.main.ResourceManager;
import com.hbm.render.anim.HbmAnimations;
import com.hbm.render.item.TEISRBase;
import com.hbm.render.model.BakedModelTransforms;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class ItemRenderWeaponObj extends TEISRBase {

    @Override
    public ModelBinding createModelBinding(Item item) {
        if (item == ModItems.gun_hk69 || item == ModItems.gun_ks23 || item == ModItems.gun_flechette) {
            return bindingFullTeisr(item);
        }
        return binding2dGui(item);
    }

    @Override
    public boolean useRegistryPerspective(Item item) {
        return item == ModItems.gun_hk69 || item == ModItems.gun_ks23 || item == ModItems.gun_flechette;
    }

    @Override
    public void renderByItem(ItemStack stack) {
        GL11.glPopMatrix();
        GlStateManager.enableRescaleNormal();

        Item item = stack.getItem();
        if (item == ModItems.gun_hk69) {
            Minecraft.getMinecraft().getTextureManager().bindTexture(ResourceManager.hk69_tex);
        } else if (item == ModItems.gun_deagle) {
            Minecraft.getMinecraft().getTextureManager().bindTexture(ResourceManager.deagle_tex);
        } else if (item == ModItems.gun_ks23) {
            Minecraft.getMinecraft().getTextureManager().bindTexture(ResourceManager.ks23_tex);
        } else if (item == ModItems.gun_flamer) {
            Minecraft.getMinecraft().getTextureManager().bindTexture(ResourceManager.flamer_tex);
        } else if (item == ModItems.gun_sauer) {
            Minecraft.getMinecraft().getTextureManager().bindTexture(ResourceManager.sauer_tex);
        }

        switch (type) {
            case FIRST_PERSON_LEFT_HAND:
            case FIRST_PERSON_RIGHT_HAND:
                if (type == TransformType.FIRST_PERSON_LEFT_HAND) {
                    if (item == ModItems.gun_hk69) {
                        GL11.glTranslated(2.0D, 0.0D, -0.4D);
                        GL11.glRotated(10.0D, 0.0D, 1.0D, 0.0D);
                        GL11.glTranslated(-1.0D, 0.0D, -0.2D);
                        GL11.glRotated(-180.0D, 0.0D, 1.0D, 0.0D);
                        GL11.glRotated(-90.0D, 0.0D, 1.0D, 0.0D);
                        GL11.glRotated(-25.0D, 1.0D, 0.0D, 0.0D);
                        GL11.glRotated(-5.0D, 0.0D, 1.0D, 0.0D);
                    } else if (item == ModItems.gun_deagle) {
                        GL11.glTranslated(0.0D, 0.0D, 0.2D);
                        GL11.glRotated(20.0D, 0.0D, 0.0D, 1.0D);
                        GL11.glRotated(95.0D, 0.0D, 1.0D, 0.0D);
                        GL11.glScaled(0.2D, 0.2D, 0.2D);
                    } else if (item == ModItems.gun_ks23 || item == ModItems.gun_sauer) {
                        GL11.glTranslated(-0.1D, 0.25D, 0.5D);
                        GL11.glRotated(30.0D, 0.0D, 0.0D, 1.0D);
                        GL11.glRotated(95.0D, 0.0D, 1.0D, 0.0D);
                    } else if (item == ModItems.gun_flamer) {
                        GL11.glTranslated(0.3D, -0.6D, 0.0D);
                        GL11.glRotated(26.0D, 0.0D, 0.0D, 1.0D);
                        GL11.glRotated(95.0D, 0.0D, 1.0D, 0.0D);
                        GL11.glScaled(0.5D, 0.5D, 0.5D);
                    } else if (item == ModItems.gun_flechette) {
                        GL11.glTranslated(0.5D, -1.0D, 0.3D);
                        GL11.glRotated(25.0D, 0.0D, 0.0D, 1.0D);
                        GL11.glRotated(185.0D, 0.0D, 1.0D, 0.0D);
                        GL11.glScaled(0.25D, 0.25D, 0.25D);
                        
                        double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL",
                                type == TransformType.FIRST_PERSON_LEFT_HAND ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND);
                        GL11.glTranslated(recoil[0], recoil[1], recoil[2]);
                    }
                } else {
                    // type is FIRST_PERSON_RIGHT_HAND
                    if (item == ModItems.gun_hk69) {
                        GL11.glTranslated(-1.0D, 0.0D, -0.2D);
                        if (ItemGunBase.isAimingForRender(stack)) {
                            GL11.glTranslated(0.5D, 0.2D, 1.14D);
                            GL11.glRotated(5.0D, 0.0D, 1.0D, 0.0D);
                        }
                        GL11.glRotated(-90.0D, 0.0D, 1.0D, 0.0D);
                        GL11.glRotated(-25.0D, 1.0D, 0.0D, 0.0D);
                        GL11.glRotated(-5.0D, 0.0D, 1.0D, 0.0D);
                    } else if (item == ModItems.gun_deagle) {
                        GL11.glTranslated(0.0D, 0.0D, 0.2D);
                        if (ItemGunBase.isAimingForRender(stack)) {
                            GL11.glTranslated(0.0D, 0.2D, 0.72D);
                            GL11.glRotated(10.0D, 0.0D, 1.0D, 0.0D);
                        }
                        GL11.glRotated(260.0D, 0.0D, 1.0D, 0.0D);
                        GL11.glRotated(-20.0D, 1.0D, 0.0D, 0.0D);
                        GL11.glScaled(0.2D, 0.2D, 0.2D);
                    } else if (item == ModItems.gun_ks23 || item == ModItems.gun_sauer) {
                        GL11.glTranslated(-0.1D, 0.3D, 0.4D);
                        if (ItemGunBase.isAimingForRender(stack)) {
                            GL11.glTranslated(-0.2D, 0.25D, 0.53D);
                            GL11.glRotated(10.0D, 0.0D, 1.0D, 0.0D);
                        }
                        GL11.glRotated(260.0D, 0.0D, 1.0D, 0.0D);
                        GL11.glRotated(-25.0D, 1.0D, 0.0D, 0.0D);
                    } else if (item == ModItems.gun_flamer) {
                        GL11.glTranslated(-0.5D, -0.5D, 0.0D);
                        if (ItemGunBase.isAimingForRender(stack)) {
                            GL11.glTranslated(0.0D, 0.15D, 0.53D);
                        }
                        GL11.glRotated(265.0D, 0.0D, 1.0D, 0.0D);
                        GL11.glRotated(-25.0D, 1.0D, 0.0D, 0.0D);
                        GL11.glScaled(0.5D, 0.5D, 0.5D);
                    } else if (item == ModItems.gun_flechette) {
                        GL11.glTranslated(-0.5D, -1.0D, 0.3D);
                        if (ItemGunBase.isAimingForRender(stack)) {
                            GL11.glTranslated(-0.8D, 0.55D, 0.7D);
                            GL11.glRotated(6.0D, 0.0D, 1.0D, 0.0D);
                        }
                        GL11.glRotated(-25.0D, 0.0D, 0.0D, 1.0D);
                        GL11.glRotated(-5.0D, 0.0D, 1.0D, 0.0D);
                        GL11.glScaled(0.25D, 0.25D, 0.25D);
                        
                        double[] recoil = HbmAnimations.getRelevantTransformation("RECOIL",
                                type == TransformType.FIRST_PERSON_LEFT_HAND ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND);
                        GL11.glTranslated(recoil[0], recoil[1], recoil[2]);
                    }
                }
                break;

            case THIRD_PERSON_LEFT_HAND:
            case THIRD_PERSON_RIGHT_HAND:
            case GROUND:
            case FIXED:
            case HEAD:
                if (type == TransformType.THIRD_PERSON_LEFT_HAND || type == TransformType.THIRD_PERSON_RIGHT_HAND || type == TransformType.GROUND) {
                    if (item == ModItems.gun_hk69) {
                        GL11.glTranslated(0.0D, -0.2D, 0.5D);
                    }
                }
                if (item == ModItems.gun_hk69) {
                    GL11.glTranslated(0.0D, -0.3D, -0.5D);
                    GL11.glRotated(180.0D, 0.0D, 1.0D, 0.0D);
                } else if (item == ModItems.gun_deagle) {
                    GL11.glTranslated(0.0D, -0.3D, 0.0D);
                    GL11.glRotated(180.0D, 0.0D, 1.0D, 0.0D);
                    GL11.glScaled(0.2D, 0.2D, 0.2D);
                } else if (item == ModItems.gun_ks23 || item == ModItems.gun_sauer) {
                    GL11.glTranslated(0.0D, -0.15D, -1.3D);
                    GL11.glRotated(180.0D, 0.0D, 1.0D, 0.0D);
                    GL11.glScaled(1.25D, 1.25D, 1.25D);
                } else if (item == ModItems.gun_flamer) {
                    GL11.glTranslated(0.0D, -0.62D, 0.0D);
                    GL11.glRotated(180.0D, 0.0D, 1.0D, 0.0D);
                    GL11.glScaled(0.35D, 0.35D, 0.35D);
                } else if (item == ModItems.gun_flechette) {
                    GL11.glTranslated(0.0D, -1.1D, -1.0D);
                    GL11.glRotated(270.0D, 0.0D, 1.0D, 0.0D);
                    GL11.glScaled(0.2D, 0.2D, 0.2D);
                }
                break;

            case GUI:
                GlStateManager.enableLighting();
                if (item == ModItems.gun_hk69) {
                    GL11.glScaled(0.5D, 0.5D, 0.5D);
                    GL11.glTranslatef(-0.2F, -0.1F, 0.0F);
                    GL11.glRotatef(-90.0F, 0.0F, 1.0F, 0.0F);
                    GL11.glRotatef(-40.0F, 1.0F, 0.0F, 0.0F);
                } else if (item == ModItems.gun_deagle) {
                    GL11.glScaled(0.15D, 0.15D, 0.15D);
                    GL11.glTranslatef(-0.5F, -0.5F, 0.0F);
                    GL11.glRotatef(-90.0F, 0.0F, 1.0F, 0.0F);
                    GL11.glRotatef(-40.0F, 1.0F, 0.0F, 0.0F);
                } else if (item == ModItems.gun_ks23 || item == ModItems.gun_sauer) {
                    GL11.glScaled(0.5D, 0.5D, 0.5D);
                    GL11.glTranslatef(-0.5F, 0.6F, 0.0F);
                    GL11.glRotatef(-90.0F, 0.0F, 1.0F, 0.0F);
                    GL11.glRotatef(-40.0F, 1.0F, 0.0F, 0.0F);
                } else if (item == ModItems.gun_flamer) {
                    GL11.glScaled(0.12D, 0.12D, 0.12D);
                    GL11.glTranslatef(-0.2F, -1.2F, 0.0F);
                    GL11.glRotatef(-90.0F, 0.0F, 1.0F, 0.0F);
                    GL11.glRotatef(-40.0F, 1.0F, 0.0F, 0.0F);
                } else if (item == ModItems.gun_flechette) {
                    GL11.glScaled(0.07D, 0.07D, 0.07D);
                    GL11.glTranslatef(-3.75F, -2.0F, 0.0F);
                    GL11.glRotatef(-40.0F, 0.0F, 0.0F, 1.0F);
                }
                break;

            default:
                break;
        }

        if (item == ModItems.gun_hk69) {
            GlStateManager.shadeModel(7425);
            ResourceManager.hk69.renderAll();
            GlStateManager.shadeModel(7424);
        } else if (item == ModItems.gun_deagle) {
            GlStateManager.shadeModel(7425);
            ResourceManager.deagle.renderAll();
            GlStateManager.shadeModel(7424);
        } else if (item == ModItems.gun_ks23) {
            GlStateManager.shadeModel(7425);
            ResourceManager.ks23.renderAll();
            GlStateManager.shadeModel(7424);
        } else if (item == ModItems.gun_sauer) {
            GlStateManager.shadeModel(7425);
            ResourceManager.sauer.renderAll();
            GlStateManager.shadeModel(7424);
        } else if (item == ModItems.gun_flamer) {
            GlStateManager.shadeModel(7425);
            ResourceManager.flamer.renderAll();
            GlStateManager.shadeModel(7424);
        } else if (item == ModItems.gun_flechette) {
            GlStateManager.shadeModel(7425);
            renderFlechette();
            GlStateManager.shadeModel(7424);
        }

        GlStateManager.enableLighting();
        GL11.glPushMatrix();
    }

    public void renderFlechette() {
        Minecraft.getMinecraft().getTextureManager().bindTexture(ResourceManager.flechette_body);
        ResourceManager.flechette.renderPart("body");
        
        Minecraft.getMinecraft().getTextureManager().bindTexture(ResourceManager.flechette_chamber);
        ResourceManager.flechette.renderPart("chamber");
        
        Minecraft.getMinecraft().getTextureManager().bindTexture(ResourceManager.flechette_barrel);
        ResourceManager.flechette.renderPart("barrel");
        
        Minecraft.getMinecraft().getTextureManager().bindTexture(ResourceManager.flechette_gren_tube);
        ResourceManager.flechette.renderPart("gren_tube");
        
        Minecraft.getMinecraft().getTextureManager().bindTexture(ResourceManager.flechette_grenades);
        ResourceManager.flechette.renderPart("grenades");
        
        Minecraft.getMinecraft().getTextureManager().bindTexture(ResourceManager.flechette_pivot);
        ResourceManager.flechette.renderPart("pivot");
        
        Minecraft.getMinecraft().getTextureManager().bindTexture(ResourceManager.flechette_top);
        ResourceManager.flechette.renderPart("top");
        
        Minecraft.getMinecraft().getTextureManager().bindTexture(ResourceManager.flechette_drum);
        ResourceManager.flechette.renderPart("drum");
        
        Minecraft.getMinecraft().getTextureManager().bindTexture(ResourceManager.flechette_base);
        ResourceManager.flechette.renderPart("base");
        
        Minecraft.getMinecraft().getTextureManager().bindTexture(ResourceManager.flechette_trigger);
        ResourceManager.flechette.renderPart("trigger");
        
        Minecraft.getMinecraft().getTextureManager().bindTexture(ResourceManager.flechette_stock);
        ResourceManager.flechette.renderPart("stock");
    }
}
