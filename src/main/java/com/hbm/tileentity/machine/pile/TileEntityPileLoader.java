package com.hbm.tileentity.machine.pile;

import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.generic.BlockMeta;
import com.hbm.blocks.machine.pile.BlockPile;
import com.hbm.interfaces.AutoRegister;
import com.hbm.handler.CompatHandler;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemPileRodMK2;
import com.hbm.items.machine.ItemPileRodMK2.EnumPileRod;
import io.netty.buffer.ByteBuf;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.SimpleComponent;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;

@AutoRegister(name = "tileentity_pile_loader")
@Optional.InterfaceList({
        @Optional.Interface(iface = "li.cil.oc.api.network.SimpleComponent", modid = "opencomputers"),
        @Optional.Interface(iface = "com.hbm.handler.CompatHandler.OCComponent", modid = "opencomputers")
})
public class TileEntityPileLoader extends TileEntityPileDeviceBase implements IRORValueProvider, SimpleComponent, CompatHandler.OCComponent {

    public static final double SPEED = 1D / 7D;

    public double syncLevel;
    public double level;
    public double lastLevel;
    public int turnProgress;
    public boolean loading;
    public int delay;
    public boolean wasRedstone;

    public ItemStack syncStack = ItemStack.EMPTY;
    public ItemStack channelStack = ItemStack.EMPTY;
    public double channelDepletion;
    public double channelTemp;

    private final ItemStackHandler inventory = new ItemStackHandler(1) {
        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return isItemLoadable(stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            markDirty();
        }
    };

    @Override
    public void update() {
        if (world == null) return;
        if (!world.isRemote) {
            EnumFacing direction = getOrientation();
            TileEntityPileCore.PileChannel fuelChannel = findFuelChannel(direction);
            channelStack = ItemStack.EMPTY;
            channelDepletion = 0D;
            channelTemp = 0D;

            if (fuelChannel != null) {
                chanNum = ((TileEntityPileBaseMK2) world.getTileEntity(pos.offset(direction.getOpposite())))
                        .getCore().getFuelChannelNum(fuelChannel);
                if (fuelChannel.rods.length > 0) {
                    channelStack = fuelChannel.rods[fuelChannel.rods.length - 1].copy();
                }
                channelDepletion = ItemPileRodMK2.getDepletionPercent(channelStack);
                channelTemp = fuelChannel.heat;
            } else {
                chanNum = -1;
            }

            boolean redstone = world.getRedstonePower(pos.offset(direction), direction.getOpposite()) > 0;
            if (redstone && !wasRedstone && delay <= 0 && level <= 0D) loading = true;
            if (redstone != wasRedstone) markDirty();
            wasRedstone = redstone;

            if (delay > 0) {
                delay--;
            } else if (loading) {
                if (level == 0D) {
                    world.playSound(null, pos, SoundEvents.BLOCK_PISTON_EXTEND, SoundCategory.BLOCKS, 1F, 1F);
                }
                level += SPEED;
                if (level >= 1D) {
                    level = 1D;
                    loading = false;
                    delay = 5;
                }
                markDirty();
            } else {
                if (level == 1D) {
                    world.playSound(null, pos, SoundEvents.BLOCK_PISTON_CONTRACT, SoundCategory.BLOCKS, 1F, 0.75F);
                    ItemStack stack = inventory.getStackInSlot(0);
                    if (fuelChannel != null && !stack.isEmpty()) {
                        fuelChannel.loadItem(stack);
                        inventory.setStackInSlot(0, ItemStack.EMPTY);
                    }
                }
                if (level > 0D) {
                    level = Math.max(0D, level - SPEED);
                    markDirty();
                }
            }
            networkPackNT(35);
        } else {
            lastLevel = level;
            if (turnProgress > 0) {
                level += (syncLevel - level) / turnProgress;
                turnProgress--;
            } else {
                level = syncLevel;
            }
        }
    }

    @Nullable
    private TileEntityPileCore.PileChannel findFuelChannel(EnumFacing direction) {
        BlockPos channelPos = pos.offset(direction.getOpposite());
        if (world.getBlockState(channelPos).getBlock() != ModBlocks.pile_block ||
                world.getBlockState(channelPos).getValue(BlockMeta.META) != BlockPile.META_FUEL_IN) {
            return null;
        }
        TileEntity tile = world.getTileEntity(channelPos);
        if (!(tile instanceof TileEntityPileBaseMK2)) return null;
        TileEntityPileCore core = ((TileEntityPileBaseMK2) tile).getCore();
        return core == null ? null : core.getFuelChannel(channelPos);
    }

    public ItemStack getStack() {
        return inventory.getStackInSlot(0);
    }

    public void setStack(ItemStack stack) {
        inventory.setStackInSlot(0, stack == null ? ItemStack.EMPTY : stack);
    }

    public static boolean isItemLoadable(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() == ModItems.pile_rod;
    }

    @Override
    public void serialize(ByteBuf buf) {
        super.serialize(buf);
        buf.writeDouble(level);
        ByteBufUtils.writeItemStack(buf, getStack());
        ByteBufUtils.writeItemStack(buf, channelStack);
        buf.writeDouble(channelDepletion);
        buf.writeDouble(channelTemp);
    }

    @Override
    public void deserialize(ByteBuf buf) {
        super.deserialize(buf);
        double previous = syncLevel;
        syncLevel = buf.readDouble();
        syncStack = ByteBufUtils.readItemStack(buf);
        channelStack = ByteBufUtils.readItemStack(buf);
        channelDepletion = buf.readDouble();
        channelTemp = buf.readDouble();
        if (syncLevel != previous) turnProgress = 2;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        loading = nbt.getBoolean("loading");
        level = nbt.getDouble("level");
        delay = nbt.getInteger("delay");
        wasRedstone = nbt.hasKey("wasRedstone") ? nbt.getBoolean("wasRedstone") : nbt.getBoolean("redstone");
        if (nbt.hasKey("inventory", 10)) {
            inventory.deserializeNBT(nbt.getCompoundTag("inventory"));
        } else if (nbt.hasKey("stack", 10)) {
            setStack(new ItemStack(nbt.getCompoundTag("stack")));
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        nbt.setBoolean("loading", loading);
        nbt.setDouble("level", level);
        nbt.setInteger("delay", delay);
        nbt.setBoolean("wasRedstone", wasRedstone);
        nbt.setTag("inventory", inventory.serializeNBT());
        return nbt;
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
    }

    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(inventory);
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public String[] getFunctionInfo() {
        return new String[]{
                PREFIX_VALUE + "meta",
                PREFIX_VALUE + "depletion",
                PREFIX_VALUE + "deppercent",
                PREFIX_VALUE + "lifetime",
                PREFIX_VALUE + "temp"
        };
    }

    @Override
    public String provideRORValue(String name) {
        if ((PREFIX_VALUE + "meta").equals(name)) {
            return channelStack.isEmpty() ? "-1" : Integer.toString(channelStack.getMetadata());
        }
        if ((PREFIX_VALUE + "depletion").equals(name)) {
            return channelStack.isEmpty() ? "0" : Integer.toString((int) Math.round(ItemPileRodMK2.getDepletion(channelStack)));
        }
        if ((PREFIX_VALUE + "deppercent").equals(name)) {
            return Integer.toString((int) Math.round(channelDepletion));
        }
        if ((PREFIX_VALUE + "lifetime").equals(name)) {
            EnumPileRod rod = channelStack.isEmpty() ? null : ItemPileRodMK2.getRod(channelStack);
            return rod == null ? "0" : Integer.toString((int) Math.round(rod.life));
        }
        if ((PREFIX_VALUE + "temp").equals(name)) {
            return Integer.toString((int) Math.round(channelTemp));
        }
        return null;
    }

    @Override
    @Optional.Method(modid = "opencomputers")
    public String getComponentName() { return "ntm_pile_loader"; }

    @Callback(direct = true, doc = "function():number -- Returns channel temperature")
    @Optional.Method(modid = "opencomputers")
    public Object[] getTemp(Context context, Arguments args) { return new Object[] {channelTemp}; }

    @Callback(direct = true, doc = "function():number -- Returns fuel depletion percentage")
    @Optional.Method(modid = "opencomputers")
    public Object[] getDepletion(Context context, Arguments args) { return new Object[] {channelStack.isEmpty() ? 0D : channelDepletion}; }

    @Callback(direct = true, doc = "function():number -- Returns fuel lifetime")
    @Optional.Method(modid = "opencomputers")
    public Object[] getLifetime(Context context, Arguments args) { return new Object[] {channelStack.isEmpty() ? 0D : ItemPileRodMK2.getRod(channelStack).life}; }

    @Callback(direct = true, doc = "function():number -- Returns fuel metadata, or -1")
    @Optional.Method(modid = "opencomputers")
    public Object[] getType(Context context, Arguments args) { return new Object[] {channelStack.isEmpty() ? -1 : channelStack.getMetadata()}; }

    @Callback(direct = true, doc = "function():number -- Returns loading fuel metadata, or -1")
    @Optional.Method(modid = "opencomputers")
    public Object[] getLoadingType(Context context, Arguments args) { return new Object[] {getStack().isEmpty() ? -1 : getStack().getMetadata()}; }

    @Callback(direct = true, doc = "function():boolean -- Returns fuel loading state")
    @Optional.Method(modid = "opencomputers")
    public Object[] isLoading(Context context, Arguments args) { return new Object[] {loading}; }

    @Callback(limit = 4, doc = "function() -- Starts loading when the loader is retracted and ready")
    @Optional.Method(modid = "opencomputers")
    public Object[] load(Context context, Arguments args) {
        if(delay <= 0 && level <= 0D) {
            loading = true;
            markDirty();
        }
        return new Object[0];
    }

    @Override
    @Optional.Method(modid = "opencomputers")
    public String[] methods() { return new String[] {"getTemp", "getDepletion", "getLifetime", "getType", "getLoadingType", "isLoading", "load"}; }

    @Override
    @Optional.Method(modid = "opencomputers")
    public Object[] invoke(String method, Context context, Arguments args) throws Exception {
        switch(method) {
            case "getTemp": return getTemp(context, args);
            case "getDepletion": return getDepletion(context, args);
            case "getLifetime": return getLifetime(context, args);
            case "getType": return getType(context, args);
            case "getLoadingType": return getLoadingType(context, args);
            case "isLoading": return isLoading(context, args);
            case "load": return load(context, args);
            default: throw new NoSuchMethodException(method);
        }
    }
}
