package com.hbm.items.tool;

import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.inventory.gui.GUIRTTYDesignator;
import com.hbm.items.IItemControlReceiver;
import com.hbm.items.ItemBakedBase;
import com.hbm.lib.Library;
import com.hbm.main.MainRegistry;
import com.hbm.tileentity.IGUIProvider;
import com.hbm.tileentity.network.RTTYSystem;
import com.hbm.util.EnumUtil;
import com.hbm.util.I18nUtil;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.internal.FMLNetworkHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

public class ItemRTTYDesignator extends ItemBakedBase implements IItemControlReceiver, IGUIProvider {

    public static final String KEY_CHANNEL = "chan";

    public ItemRTTYDesignator(String name) {
        super(name);
        setMaxStackSize(1);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (player.isSneaking()) {
            if (world.isRemote) FMLNetworkHandler.openGui(player, MainRegistry.instance, 0, world, hand.ordinal(), -1, 0);
        } else if (!world.isRemote && stack.hasTagCompound()) {
            String channel = stack.getTagCompound().getString(KEY_CHANNEL);
            if (!channel.isEmpty()) {
                Vec3d start = Library.getPosition(1, player).add(0, player.eyeHeight, 0);
                Vec3d end = start.add(player.getLook(1).scale(300));
                // A diagonal 300-block ray can cross over 500 voxel boundaries.
                RayTraceResult hit = Library.rayTraceBlocks(world, start, end, false, false, true, 600);
                if (hit != null && hit.typeOfHit == RayTraceResult.Type.BLOCK && hit.sideHit != null) {
                    BlockPos pos = hit.getBlockPos().offset(hit.sideHit);
                    RTTYSystem.broadcast(world, channel, pos.getX() + IRORInteractive.PARAM_SEPARATOR + pos.getY() + IRORInteractive.PARAM_SEPARATOR + pos.getZ());
                }
            }
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> list, ITooltipFlag flag) {
        String channel = stack.hasTagCompound() ? stack.getTagCompound().getString(KEY_CHANNEL) : "";
        list.add(channel.isEmpty() ? TextFormatting.RED + I18nUtil.resolveKey("desc.rttyDesignator.noChannel") : TextFormatting.YELLOW + I18nUtil.resolveKey("desc.rttyDesignator.channel", channel));
        list.add(I18nUtil.resolveKey("desc.rttyDesignator.configure"));
    }

    @Override
    public void receiveControl(ItemStack stack, NBTTagCompound data) {
        if (data.hasKey(KEY_CHANNEL, 8)) {
            if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
            stack.getTagCompound().setString(KEY_CHANNEL, data.getString(KEY_CHANNEL));
        }
    }

    @Override
    public Container provideContainer(int ID, EntityPlayer player, World world, int x, int y, int z) {
        return null;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public GuiScreen provideGUI(int ID, EntityPlayer player, World world, int x, int y, int z) {
        EnumHand hand = x >= 0 && x < EnumUtil.HANDS.length ? EnumUtil.HANDS[x] : EnumHand.MAIN_HAND;
        return new GUIRTTYDesignator(player.getHeldItem(hand), hand);
    }
}
