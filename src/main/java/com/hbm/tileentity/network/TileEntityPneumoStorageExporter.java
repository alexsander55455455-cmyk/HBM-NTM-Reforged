package com.hbm.tileentity.network;

import com.hbm.api.ntl.StackCache;
import com.hbm.api.ntl.StackCache.CacheSlot;
import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.interfaces.AutoRegister;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.container.ContainerPneumoStorageExporter;
import com.hbm.inventory.gui.GUIPneumoStorageExporter;
import com.hbm.tileentity.IControlReceiverFilter;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

@AutoRegister
public class TileEntityPneumoStorageExporter extends TileEntityPneumaticMachineBase
        implements IRORInteractive, IControlReceiver, IControlReceiverFilter {

    public static final int MODE_AS_MUCH_AS_POSSIBLE = 0;
    public static final int MODE_FULL_STACK = 1;
    public static final int MODE_FULL_REQUEST = 2;
    public static final int SLOT_DELAY = 10;
    private static final int[] SLOT_ACCESS = { 9, 10, 11, 12, 13, 14, 15, 16, 17 };

    public boolean continuousRequest;
    public boolean rorConfiguredMode;
    public int requestMode;
    public short[][] rorFilters = new short[9][3];
    public int[] slotDelay = new int[9];
    public boolean lastRedstone;

    public TileEntityPneumoStorageExporter() {
        super(18);
    }

    @Override public String getDefaultName() { return "container.pneumoStorageExporter"; }
    @Override public int[] getAccessibleSlotsFromSide(EnumFacing side) { return SLOT_ACCESS.clone(); }
    @Override public boolean canInsertItem(int slot, ItemStack stack) { return false; }
    @Override public boolean canExtractItem(int slot, ItemStack stack, int amount) { return slot >= 9; }

    @Override
    public void update() {
        super.update();
        if (world == null || world.isRemote) return;

        for (int i = 0; i < slotDelay.length; i++) if (slotDelay[i] > 0) slotDelay[i]--;
        boolean redstone = world.isBlockPowered(pos);
        if (continuousRequest) doRequest(false);
        else if (redstone && !lastRedstone) doRequest(true);
        lastRedstone = redstone;
        networkPackNT(15);
    }

    public void doRequest(boolean force) {
        if (requestMode != MODE_FULL_REQUEST) {
            for (int i = 0; i < 9; i++) if (!requestSlot(i, force)) slotDelay[i] = SLOT_DELAY;
            return;
        }
        if (cache == null || cache.hasExpired) return;

        if (!force) {
            for (int i = 0; i < 9; i++) if (getFilter(i) != null && slotDelay[i] > 0) return;
        }

        Map<Long, Long> totalRequests = new LinkedHashMap<>();
        for (int i = 0; i < 9; i++) {
            short[] filter = getFilter(i);
            if (filter == null) continue;
            Item item = Item.getItemById(Short.toUnsignedInt(filter[0]));
            if (item == null) return;
            int meta = Short.toUnsignedInt(filter[1]);
            int requested = Short.toUnsignedInt(filter[2]);
            ItemStack output = inventory.getStackInSlot(i + 9);
            int existing = output.isEmpty() ? 0 : output.getCount();
            if (!output.isEmpty() && (output.getItem() != item || output.getMetadata() != meta || output.hasTagCompound())) return;
            ItemStack requestedStack = new ItemStack(item, 1, meta);
            if (requestedStack.getMaxStackSize() - existing < requested) {
                slotDelay[i] = SLOT_DELAY;
                return;
            }
            long identity = StackCache.getStackIdentity(item, meta, null);
            totalRequests.merge(identity, (long) requested, Long::sum);
        }

        for (Map.Entry<Long, Long> request : totalRequests.entrySet()) {
            CacheSlot available = cache.cacheSlots.get(request.getKey());
            if (available == null || available.stacksize < request.getValue()) {
                for (int i = 0; i < 9; i++) if (getFilter(i) != null) slotDelay[i] = SLOT_DELAY;
                return;
            }
        }

        for (int i = 0; i < 9; i++) {
            short[] filter = getFilter(i);
            if (filter == null) continue;
            Item item = Item.getItemById(Short.toUnsignedInt(filter[0]));
            int requested = Short.toUnsignedInt(filter[2]);
            ItemStack output = inventory.getStackInSlot(i + 9);
            int existing = output.isEmpty() ? 0 : output.getCount();
            ItemStack requestedStack = new ItemStack(item, 1, Short.toUnsignedInt(filter[1]));
            int pulled = (int) cache.consumeItemsAndReturnQuantity(requestedStack, requested);
            if (pulled > 0) {
                requestedStack.setCount(existing + pulled);
                inventory.setStackInSlot(i + 9, requestedStack);
            }
        }
        markDirty();
    }

    public boolean requestSlot(int slot, boolean force) {
        if (slot < 0 || slot >= 9 || (!force && slotDelay[slot] > 0)) return true;
        if (cache == null || cache.hasExpired) return false;
        short[] filter = getFilter(slot);
        if (filter == null) return false;

        Item item = Item.getItemById(Short.toUnsignedInt(filter[0]));
        if (item == null) return false;
        int meta = Short.toUnsignedInt(filter[1]);
        int requested = Short.toUnsignedInt(filter[2]);
        ItemStack output = inventory.getStackInSlot(slot + 9);
        int existing = output.isEmpty() ? 0 : output.getCount();
        if (!output.isEmpty() && (output.getItem() != item || output.getMetadata() != meta || output.hasTagCompound())) return false;

        ItemStack requestedStack = new ItemStack(item, 1, meta);
        int capacity = requestedStack.getMaxStackSize() - existing;
        if (capacity < requested && requestMode != MODE_AS_MUCH_AS_POSSIBLE) return false;
        CacheSlot available = cache.getSlotFromStack(item, meta, null);
        if (available == null || available.stacksize <= 0) return false;
        if (available.stacksize < requested && requestMode != MODE_AS_MUCH_AS_POSSIBLE) return false;

        int pull = (int) Math.min(Math.min((long) requested, available.stacksize), capacity);
        int pulled = (int) cache.consumeItemsAndReturnQuantity(requestedStack, pull);
        if (pulled <= 0) return false;
        requestedStack.setCount(existing + pulled);
        inventory.setStackInSlot(slot + 9, requestedStack);
        markDirty();
        return true;
    }

    public short[] getFilter(int slot) {
        if (slot < 0 || slot >= 9) return null;
        if (rorConfiguredMode) {
            int id = Short.toUnsignedInt(rorFilters[slot][0]);
            int amount = Short.toUnsignedInt(rorFilters[slot][2]);
            return id > 0 && amount > 0 && Item.getItemById(id) != null ? rorFilters[slot] : null;
        }
        ItemStack filter = inventory.getStackInSlot(slot);
        if (filter.isEmpty()) return null;
        return new short[] { (short) Item.getIdFromItem(filter.getItem()), (short) filter.getMetadata(), (short) filter.getCount() };
    }

    public long getAvailability(int itemId, int meta) {
        if (cache == null || cache.hasExpired) return 0;
        long identity = StackCache.getStackIdentity(itemId, meta, null);
        if (identity == StackCache.getNullIdentity()) return 0;
        CacheSlot slot = cache.cacheSlots.get(identity);
        return slot == null ? 0 : slot.stacksize;
    }

    @Override public boolean hasPermission(EntityPlayer player) { return isUseableByPlayer(player); }

    @Override
    public void receiveControl(NBTTagCompound data) {
        if (data.hasKey("continuous")) continuousRequest = !continuousRequest;
        if (data.hasKey("request")) requestMode = (requestMode + 1) % 3;
        if (data.hasKey("ror")) rorConfiguredMode = !rorConfiguredMode;
        if (data.hasKey("slot")) setFilterContents(data);
        markDirty();
        dataChanged();
    }

    @Override public int[] getFilterSlots() { return new int[] { 0, 9 }; }
    @Override public void nextMode(int i) { }

    @Override
    public void setFilterContents(NBTTagCompound nbt) {
        int slot = nbt.getInteger("slot");
        if (slot < 0 || slot >= 9) return;
        ItemStack item = new ItemStack(nbt.getCompoundTag("stack"));
        inventory.setStackInSlot(slot, item);
        markDirty();
    }

    @Override
    public String[] getFunctionInfo() {
        return new String[] {
                PREFIX_FUNCTION + "setfilter" + NAME_SEPARATOR + "slot" + PARAM_SEPARATOR + "itemid" + PARAM_SEPARATOR + "itemmeta" + PARAM_SEPARATOR + "amount",
                PREFIX_FUNCTION + "setcontinuous" + NAME_SEPARATOR + "on/off",
                PREFIX_FUNCTION + "request",
                PREFIX_FUNCTION + "requestslot" + NAME_SEPARATOR + "slot",
                PREFIX_FUNCTION + "checkavailability" + NAME_SEPARATOR + "itemid" + PARAM_SEPARATOR + "itemmeta" + PARAM_SEPARATOR + "returnchannel"
        };
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        if ((PREFIX_FUNCTION + "setfilter").equals(name) && params.length == 4) {
            int slot = IRORInteractive.parseInt(params[0], 1, 9) - 1;
            int itemId = IRORInteractive.parseInt(params[1], 0, Short.MAX_VALUE);
            int meta = IRORInteractive.parseInt(params[2], 0, Short.MAX_VALUE);
            int amount = IRORInteractive.parseInt(params[3], 1, 64);
            rorFilters[slot][0] = (short) itemId;
            rorFilters[slot][1] = (short) meta;
            rorFilters[slot][2] = (short) amount;
            markDirty();
            return null;
        }
        if ((PREFIX_FUNCTION + "setcontinuous").equals(name) && params.length == 1) {
            if ("on".equals(params[0])) continuousRequest = true;
            else if ("off".equals(params[0])) continuousRequest = false;
            else throw new com.hbm.api.redstoneoverradio.RORFunctionException(EX_FORMAT);
            markDirty();
            return null;
        }
        if ((PREFIX_FUNCTION + "request").equals(name) && params.length == 0) {
            doRequest(true);
            return null;
        }
        if ((PREFIX_FUNCTION + "requestslot").equals(name) && params.length == 1) {
            int slot = IRORInteractive.parseInt(params[0], 1, 9) - 1;
            if (!requestSlot(slot, true)) slotDelay[slot] = SLOT_DELAY;
            return null;
        }
        if ((PREFIX_FUNCTION + "checkavailability").equals(name) && params.length == 3) {
            int itemId = IRORInteractive.parseInt(params[0], 0, Short.MAX_VALUE);
            int meta = IRORInteractive.parseInt(params[1], 0, Short.MAX_VALUE);
            RTTYSystem.broadcast(world, params[2], Long.toString(getAvailability(itemId, meta)));
            return null;
        }
        return null;
    }

    @Override
    public void serialize(ByteBuf buf) {
        super.serialize(buf);
        buf.writeBoolean(continuousRequest);
        buf.writeBoolean(rorConfiguredMode);
        buf.writeByte(requestMode);
        for (short[] filter : rorFilters) for (short value : filter) buf.writeShort(value);
    }

    @Override
    public void deserialize(ByteBuf buf) {
        super.deserialize(buf);
        continuousRequest = buf.readBoolean();
        rorConfiguredMode = buf.readBoolean();
        requestMode = normalizeRequestMode(buf.readByte());
        for (int i = 0; i < 9; i++) for (int j = 0; j < 3; j++) rorFilters[i][j] = buf.readShort();
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        continuousRequest = nbt.getBoolean("continuousRequest");
        rorConfiguredMode = nbt.getBoolean("rorConfiguredMode");
        requestMode = normalizeRequestMode(nbt.getByte("requestMode"));
        rorFilters = new short[9][3];
        for (int i = 0; i < 9; i++) for (int j = 0; j < 3; j++) rorFilters[i][j] = nbt.getShort("filter_" + i + "_" + j);
        lastRedstone = nbt.getBoolean("lastRedstone");
        slotDelay = normalizeSlotDelays(nbt.getIntArray("slotDelay"));
    }

    static int normalizeRequestMode(int mode) {
        return Math.max(MODE_AS_MUCH_AS_POSSIBLE, Math.min(MODE_FULL_REQUEST, mode));
    }

    static int[] normalizeSlotDelays(int[] stored) {
        int[] sanitized = Arrays.copyOf(stored, 9);
        for (int i = 0; i < sanitized.length; i++) sanitized[i] = Math.max(0, sanitized[i]);
        return sanitized;
    }

    @Override
    public @NotNull NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        nbt.setBoolean("continuousRequest", continuousRequest);
        nbt.setBoolean("rorConfiguredMode", rorConfiguredMode);
        nbt.setByte("requestMode", (byte) requestMode);
        for (int i = 0; i < 9; i++) for (int j = 0; j < 3; j++) nbt.setShort("filter_" + i + "_" + j, rorFilters[i][j]);
        nbt.setBoolean("lastRedstone", lastRedstone);
        nbt.setIntArray("slotDelay", Arrays.copyOf(slotDelay, 9));
        return super.writeToNBT(nbt);
    }

    @Override public Container provideContainer(int ID, EntityPlayer player, World world, int x, int y, int z) {
        return new ContainerPneumoStorageExporter(player.inventory, this);
    }

    @Override @SideOnly(Side.CLIENT)
    public GuiScreen provideGUI(int ID, EntityPlayer player, World world, int x, int y, int z) {
        return new GUIPneumoStorageExporter(player.inventory, this);
    }
}
