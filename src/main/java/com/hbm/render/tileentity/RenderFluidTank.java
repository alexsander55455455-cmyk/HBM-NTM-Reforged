package com.hbm.render.tileentity;

import com.hbm.Tags;
import com.hbm.blocks.ModBlocks;
import com.hbm.interfaces.AutoRegister;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FT_Corrosive;
import com.hbm.main.ResourceManager;
import com.hbm.render.item.ItemRenderBase;
import com.hbm.render.misc.DiamondPronter;
import com.hbm.tileentity.IPersistentNBT;
import com.hbm.tileentity.machine.TileEntityMachineFluidTank;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@AutoRegister
public class RenderFluidTank extends TileEntitySpecialRenderer<TileEntityMachineFluidTank> implements IItemRendererProvider {

    private static final String TANK_TEXTURE_PREFIX = "textures/models/tank/tank_";
    private static final String GENERIC_TANK_TEXTURE = TANK_TEXTURE_PREFIX + "none.png";
    private static final Map<String, Boolean> TANK_TEXTURE_CACHE = new ConcurrentHashMap<>();

    @Override
    public void render(TileEntityMachineFluidTank tank, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x + 0.5D, y, z + 0.5D);
        GlStateManager.enableLighting();
        GlStateManager.disableCull();

        switch (tank.getBlockMetadata() - 10) {
            case 2 -> GlStateManager.rotate(180, 0F, 1F, 0F);
            case 4 -> GlStateManager.rotate(270, 0F, 1F, 0F);
            case 3 -> GlStateManager.rotate(0, 0F, 1F, 0F);
            case 5 -> GlStateManager.rotate(90, 0F, 1F, 0F);
        }
        FluidType type = tank.tank.getTankType();

        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        bindTexture(ResourceManager.tank_tex);

        if (!tank.hasExploded) {
            ResourceManager.fluidtank.renderPart("Frame");
            bindTexture(new ResourceLocation(Tags.MODID, getTextureFromType(tank.tank.getTankType())));
            ResourceManager.fluidtank.renderPart("Tank");
        } else {
            ResourceManager.fluidtank_exploded.renderPart("Frame");
            bindTexture(ResourceManager.tank_inner_tex);
            ResourceManager.fluidtank_exploded.renderPart("TankInner");
            bindTexture(new ResourceLocation(Tags.MODID, getTextureFromType(tank.tank.getTankType())));
            ResourceManager.fluidtank_exploded.renderPart("Tank");
        }
        GL11.glColor3d(1D, 1D, 1D);
        GlStateManager.shadeModel(GL11.GL_FLAT);

        if (type != Fluids.NONE) {

            RenderHelper.disableStandardItemLighting();
            GlStateManager.pushMatrix();
            GlStateManager.translate(-0.25, 0.5, -1.501);
            GL11.glRotated(90, 0, 1, 0);
            GlStateManager.scale(1.0F, 0.375F, 0.375F);
            DiamondPronter.pront(type.poison, type.flammability, type.reactivity, type.symbol);
            GlStateManager.popMatrix();
            GlStateManager.pushMatrix();
            GlStateManager.translate(0.25, 0.5, 1.501);
            GL11.glRotated(-90, 0, 1, 0);
            GlStateManager.scale(1.0F, 0.375F, 0.375F);
            DiamondPronter.pront(type.poison, type.flammability, type.reactivity, type.symbol);
            GlStateManager.popMatrix();
        }
        GlStateManager.enableCull();
        GlStateManager.popMatrix();
        RenderHelper.enableStandardItemLighting();
    }

    @Override
    public Item getItemForRenderer() {
        return Item.getItemFromBlock(ModBlocks.machine_fluidtank);
    }

    @Override
    public ItemRenderBase getRenderer(Item item) {
        return new ItemRenderBase() {
            public void renderInventory() {
                GlStateManager.translate(0, -2, 0);
                GlStateManager.rotate(90, 0, 1, 0);
                GlStateManager.scale(3, 3, 3);
            }

            public void renderCommon(ItemStack stack) {
                GlStateManager.shadeModel(GL11.GL_SMOOTH);
                GlStateManager.disableCull();
                FluidTankNTM tank = new FluidTankNTM(Fluids.NONE, 0);
                boolean exploded = false;
                if(stack.hasTagCompound() && stack.getTagCompound().hasKey(IPersistentNBT.NBT_PERSISTENT_KEY)) {
                    tank.readFromNBT(stack.getTagCompound().getCompoundTag(IPersistentNBT.NBT_PERSISTENT_KEY), "tank");
                    exploded = stack.getTagCompound().getCompoundTag(IPersistentNBT.NBT_PERSISTENT_KEY).getBoolean("hasExploded");
                }
                bindTexture(ResourceManager.tank_tex);
                if(!exploded) {
                    ResourceManager.fluidtank.renderPart("Frame");
                    bindTexture(new ResourceLocation(Tags.MODID, getTextureFromType(tank.getTankType())));
                    ResourceManager.fluidtank.renderPart("Tank");
                } else {
                    ResourceManager.fluidtank_exploded.renderPart("Frame");
                    bindTexture(ResourceManager.tank_inner_tex); ResourceManager.fluidtank_exploded.renderPart("TankInner");
                    bindTexture(new ResourceLocation(Tags.MODID, getTextureFromType(tank.getTankType())));
                    ResourceManager.fluidtank_exploded.renderPart("Tank");
                }
                GL11.glColor3d(1D, 1D, 1D);
                GlStateManager.enableCull();
                GlStateManager.shadeModel(GL11.GL_FLAT);
            }
        };
    }

    private String getTextureFromType(FluidType type) {

        GL11.glColor3d(1D, 1D, 1D);

        if (type.renderWithTint) {
            tintTank(type);
            return GENERIC_TANK_TEXTURE;
        }

        String name = type.getName();

        if (type.isAntimatter() || (type.hasTrait(FT_Corrosive.class) && type.getTrait(FT_Corrosive.class).isHighlyCorrosive())) name = "danger";

        String texture = TANK_TEXTURE_PREFIX + name.toLowerCase(Locale.ROOT) + ".png";
        if (hasTankTexture(texture)) return texture;

        tintTank(type.getColor());
        return GENERIC_TANK_TEXTURE;
    }

    private static void tintTank(FluidType type) {
        tintTank(type.getTint());
    }

    private static void tintTank(int color) {
        double r = ((color & 0xff0000) >> 16) / 255D;
        double g = ((color & 0x00ff00) >> 8) / 255D;
        double b = (color & 0x0000ff) / 255D;
        GL11.glColor3d(r, g, b);
    }

    private static boolean hasTankTexture(String texture) {
        return TANK_TEXTURE_CACHE.computeIfAbsent(texture, path -> {
            try (IResource ignored = Minecraft.getMinecraft().getResourceManager().getResource(new ResourceLocation(Tags.MODID, path))) {
                return true;
            } catch (IOException exception) {
                return false;
            }
        });
    }
}
