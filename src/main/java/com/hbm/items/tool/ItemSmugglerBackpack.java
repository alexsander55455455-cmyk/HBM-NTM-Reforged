package com.hbm.items.tool;

import com.hbm.util.I18nUtil;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class ItemSmugglerBackpack extends ItemBackpack {

    public static final int VISIBLE_SLOTS = 45;
    public static final int HIDDEN_SLOTS = 9;
    public static final int SLOTS = VISIBLE_SLOTS + HIDDEN_SLOTS;
    public static final String OWNER_TAG = "SmugglerOwner";
    public static final String OWNER_NAME_TAG = "SmugglerOwnerName";
    private static final String ALLOWLIST_TAG = "SmugglerAllowlist";
    private static final String PLAYER_TAG = "Player";
    private static final String PLAYER_NAME_TAG = "Name";

    public ItemSmugglerBackpack(String name) {
        super(name, SLOTS, 0.55D, false);
        setCreativeTab(null);
    }

    @Override
    public int getInitialViewCapacity() {
        return VISIBLE_SLOTS;
    }

    @Override
    public int getInitialViewCapacity(ItemStack stack) {
        return getVisibleSlotCount(stack);
    }

    @Override
    protected int getTooltipCapacity(ItemStack stack) {
        return getVisibleSlotCount(stack);
    }

    @Override
    protected int getTooltipFilledSlotCount(ItemStack stack) {
        return countFilledSlots(stack, 0, getVisibleSlotCount(stack));
    }

    public int getVisibleSlotCount(ItemStack stack) {
        return getStorageSlots(stack) - getHiddenSlotCount(stack);
    }

    public int getHiddenSlotCount(ItemStack stack) {
        return Math.min(HIDDEN_SLOTS, Math.max(0, getStorageSlots(stack) - 1));
    }

    public boolean bindOwner(ItemStack stack, EntityPlayer player) {
        if (stack.isEmpty() || player == null || player.world.isRemote) return false;

        NBTTagCompound tag = getOrCreateTag(stack);
        UUID playerId = player.getUniqueID();
        if (!tag.hasUniqueId(OWNER_TAG)) {
            if (tag.getBoolean(ItemBlackBoxBackpack.OWNER_TRANSFER_PENDING_TAG)) return false;
            tag.setUniqueId(OWNER_TAG, playerId);
            tag.setString(OWNER_NAME_TAG, player.getName());
            return true;
        }
        if (playerId.equals(tag.getUniqueId(OWNER_TAG))
                && !player.getName().equals(tag.getString(OWNER_NAME_TAG))) {
            tag.setString(OWNER_NAME_TAG, player.getName());
            return true;
        }
        return false;
    }

    public boolean bindOwnerFromPickup(ItemStack stack, EntityPlayer player) {
        if (stack.isEmpty() || player == null || player.world.isRemote) return false;
        return bindPendingOwnerTag(getOrCreateTag(stack), player.getUniqueID(), player.getName());
    }

    public boolean isOwner(ItemStack stack, EntityPlayer player) {
        return player != null && stack.hasTagCompound()
                && stack.getTagCompound().hasUniqueId(OWNER_TAG)
                && player.getUniqueID().equals(stack.getTagCompound().getUniqueId(OWNER_TAG));
    }

    public UUID getOwnerId(ItemStack stack) {
        NBTTagCompound tag = stack.isEmpty() ? null : stack.getTagCompound();
        return tag != null && tag.hasUniqueId(OWNER_TAG) ? tag.getUniqueId(OWNER_TAG) : null;
    }

    public String getOwnerName(ItemStack stack) {
        NBTTagCompound tag = stack.isEmpty() ? null : stack.getTagCompound();
        return tag == null ? "" : tag.getString(OWNER_NAME_TAG);
    }

    public boolean canAccessHidden(ItemStack stack, EntityPlayer player) {
        if (player == null || stack.isEmpty() || !stack.hasTagCompound()) return false;
        UUID playerId = player.getUniqueID();
        NBTTagCompound tag = stack.getTagCompound();
        if (tag.hasUniqueId(OWNER_TAG) && playerId.equals(tag.getUniqueId(OWNER_TAG))) return true;
        return containsAllowedPlayer(tag, playerId);
    }

    static boolean containsAllowedPlayer(NBTTagCompound tag, UUID playerId) {
        if (tag == null || playerId == null) return false;
        NBTTagList allowed = tag.getTagList(ALLOWLIST_TAG, Constants.NBT.TAG_COMPOUND);
        for (int index = 0; index < allowed.tagCount(); index++) {
            NBTTagCompound entry = allowed.getCompoundTagAt(index);
            if (entry.hasUniqueId(PLAYER_TAG) && playerId.equals(entry.getUniqueId(PLAYER_TAG))) {
                return true;
            }
        }
        return false;
    }

    public boolean addAllowedPlayer(ItemStack stack, EntityPlayer owner, UUID playerId, String lastKnownName) {
        if (!isOwner(stack, owner) || playerId == null || playerId.equals(owner.getUniqueID())) return false;
        NBTTagCompound tag = getOrCreateTag(stack);
        NBTTagList allowed = tag.getTagList(ALLOWLIST_TAG, Constants.NBT.TAG_COMPOUND);
        for (int index = 0; index < allowed.tagCount(); index++) {
            NBTTagCompound entry = allowed.getCompoundTagAt(index);
            if (entry.hasUniqueId(PLAYER_TAG) && playerId.equals(entry.getUniqueId(PLAYER_TAG))) {
                entry.setString(PLAYER_NAME_TAG, lastKnownName == null ? "" : lastKnownName);
                tag.setTag(ALLOWLIST_TAG, allowed);
                return true;
            }
        }
        NBTTagCompound entry = new NBTTagCompound();
        entry.setUniqueId(PLAYER_TAG, playerId);
        entry.setString(PLAYER_NAME_TAG, lastKnownName == null ? "" : lastKnownName);
        allowed.appendTag(entry);
        tag.setTag(ALLOWLIST_TAG, allowed);
        return true;
    }

    public boolean removeAllowedPlayer(ItemStack stack, EntityPlayer owner, UUID playerId) {
        if (!isOwner(stack, owner) || playerId == null) return false;
        NBTTagCompound tag = getOrCreateTag(stack);
        NBTTagList allowed = tag.getTagList(ALLOWLIST_TAG, Constants.NBT.TAG_COMPOUND);
        for (int index = 0; index < allowed.tagCount(); index++) {
            NBTTagCompound entry = allowed.getCompoundTagAt(index);
            if (entry.hasUniqueId(PLAYER_TAG) && playerId.equals(entry.getUniqueId(PLAYER_TAG))) {
                allowed.removeTag(index);
                tag.setTag(ALLOWLIST_TAG, allowed);
                return true;
            }
        }
        return false;
    }

    public List<AllowedPlayer> getAllowedPlayers(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTagCompound()) return Collections.emptyList();
        NBTTagList allowed = stack.getTagCompound().getTagList(ALLOWLIST_TAG, Constants.NBT.TAG_COMPOUND);
        List<AllowedPlayer> result = new ArrayList<>(allowed.tagCount());
        for (int index = 0; index < allowed.tagCount(); index++) {
            NBTTagCompound entry = allowed.getCompoundTagAt(index);
            if (entry.hasUniqueId(PLAYER_TAG)) {
                result.add(new AllowedPlayer(entry.getUniqueId(PLAYER_TAG), entry.getString(PLAYER_NAME_TAG)));
            }
        }
        return result;
    }

    public boolean resetOwner(ItemStack stack, EntityPlayer player) {
        if (stack.isEmpty() || player == null || player.world.isRemote || !isOwner(stack, player)) return false;
        return resetOwnerTag(getOrCreateTag(stack), player.getUniqueID());
    }

    static boolean resetOwnerTag(NBTTagCompound tag, UUID requester) {
        if (tag == null || requester == null || !tag.hasUniqueId(OWNER_TAG)
                || !requester.equals(tag.getUniqueId(OWNER_TAG))) {
            return false;
        }
        tag.removeTag(OWNER_TAG + "Most");
        tag.removeTag(OWNER_TAG + "Least");
        tag.removeTag(OWNER_NAME_TAG);
        tag.removeTag(ALLOWLIST_TAG);
        tag.setBoolean(ItemBlackBoxBackpack.OWNER_TRANSFER_PENDING_TAG, true);
        return true;
    }

    static boolean bindPendingOwnerTag(NBTTagCompound tag, UUID playerId, String playerName) {
        if (tag == null || playerId == null || tag.hasUniqueId(OWNER_TAG)
                || !tag.getBoolean(ItemBlackBoxBackpack.OWNER_TRANSFER_PENDING_TAG)) {
            return false;
        }
        tag.setUniqueId(OWNER_TAG, playerId);
        tag.setString(OWNER_NAME_TAG, playerName == null ? "" : playerName);
        tag.removeTag(ItemBlackBoxBackpack.OWNER_TRANSFER_PENDING_TAG);
        return true;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        super.addInformation(stack, world, tooltip, flag);
        tooltip.add(TextFormatting.GOLD + I18nUtil.resolveKey("desc.backpack.smuggler.compartment",
                getVisibleSlotCount(stack), getHiddenSlotCount(stack)));
        if (stack.hasTagCompound() && stack.getTagCompound().hasUniqueId(OWNER_TAG)) {
            String owner = stack.getTagCompound().getString(OWNER_NAME_TAG);
            if (owner.isEmpty()) owner = stack.getTagCompound().getUniqueId(OWNER_TAG).toString();
            tooltip.add(TextFormatting.GRAY + I18nUtil.resolveKey("desc.backpack.smuggler.owner", owner));
        } else {
            tooltip.add(TextFormatting.DARK_GRAY + I18nUtil.resolveKey("desc.backpack.smuggler.unbound"));
        }
        tooltip.add(TextFormatting.DARK_GRAY + I18nUtil.resolveKey("desc.backpack.smuggler.latch"));
    }

    private static NBTTagCompound getOrCreateTag(ItemStack stack) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        return stack.getTagCompound();
    }

    private static int countFilledSlots(ItemStack stack, int start, int end) {
        if (stack.isEmpty() || !stack.hasTagCompound()) return 0;

        NBTTagCompound inventory = stack.getTagCompound().getCompoundTag(INVENTORY_TAG);
        NBTTagList items = inventory.getTagList("Items", Constants.NBT.TAG_COMPOUND);
        boolean[] filled = new boolean[Math.max(0, end - start)];
        for (int index = 0; index < items.tagCount(); index++) {
            NBTTagCompound item = items.getCompoundTagAt(index);
            int slot = item.getInteger("Slot");
            if (slot >= start && slot < end && item.hasKey("id", Constants.NBT.TAG_STRING)) {
                filled[slot - start] = true;
            }
        }
        int count = 0;
        for (boolean occupied : filled) {
            if (occupied) count++;
        }
        return count;
    }

    public static final class AllowedPlayer {
        public final UUID uuid;
        public final String name;

        public AllowedPlayer(UUID uuid, String name) {
            this.uuid = uuid;
            this.name = name;
        }
    }
}
