package com.hbm.items.tool;

import com.hbm.util.I18nUtil;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.DamageSource;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class ItemBlackBoxBackpack extends ItemBackpack {

    public static final int SLOTS = 54;
    private static final int DROPPED_LIFESPAN_TICKS = 12 * 60 * 60 * 20;
    private static final long DROPPED_LIFESPAN_MILLIS = 12L * 60L * 60L * 1_000L;
    private static final String DROPPED_AT_TAG = "hbmBlackBoxDroppedAt";
    private static final String OWNER_TAG = "hbmBlackBoxOwner";
    private static final String OWNER_NAME_TAG = "hbmBlackBoxOwnerName";
    private static final String DEATH_LOCK_TAG = "hbmBlackBoxDeathLocked";
    private static final String ACCESS_MODE_TAG = "hbmBlackBoxAccessMode";
    private static final String ALLOWLIST_TAG = "hbmBlackBoxAllowlist";
    public static final String OWNER_TRANSFER_PENDING_TAG = "OwnerTransferPending";
    private static final String PLAYER_TAG = "Player";
    private static final String PLAYER_NAME_TAG = "Name";

    public ItemBlackBoxBackpack(String name) {
        super(name, SLOTS, 1D, false);
        setCreativeTab(null);
    }

    @Override
    public @NotNull String getItemStackDisplayName(@NotNull ItemStack stack) {
        return TextFormatting.DARK_GRAY + super.getItemStackDisplayName(stack) + TextFormatting.RESET;
    }

    @Override
    public boolean protectsDroppedItemDamage(DamageSource source, float amount) {
        return source != null && (source.isFireDamage() || source.isExplosion());
    }

    @Override
    public int getDroppedLavaSurvivalTicks() {
        return 300 * 20;
    }

    public boolean bindOwner(ItemStack stack, EntityPlayer player) {
        if (stack.isEmpty() || player == null || player.world.isRemote) return false;
        NBTTagCompound tag = getOrCreateTag(stack);
        if (tag.hasUniqueId(OWNER_TAG) || tag.getBoolean(OWNER_TRANSFER_PENDING_TAG)) return false;
        tag.setUniqueId(OWNER_TAG, player.getUniqueID());
        tag.setString(OWNER_NAME_TAG, player.getName());
        return true;
    }

    public boolean bindOwnerFromPickup(ItemStack stack, EntityPlayer player) {
        if (stack.isEmpty() || player == null || player.world.isRemote) return false;
        return bindPendingOwnerTag(getOrCreateTag(stack), player.getUniqueID(), player.getName());
    }

    public void lockForDeath(ItemStack stack, EntityPlayer owner) {
        if (stack.isEmpty() || owner == null || owner.world.isRemote) return;
        NBTTagCompound tag = getOrCreateTag(stack);
        if (!tag.hasUniqueId(OWNER_TAG) && !tag.getBoolean(OWNER_TRANSFER_PENDING_TAG)) {
            tag.setUniqueId(OWNER_TAG, owner.getUniqueID());
            tag.setString(OWNER_NAME_TAG, owner.getName());
        }
        tag.setBoolean(DEATH_LOCK_TAG, true);
    }

    public boolean unlockIfReturnedToOwner(ItemStack stack, EntityPlayer player) {
        if (!isDeathLocked(stack) || !isOwner(stack, player)) return false;
        getOrCreateTag(stack).removeTag(DEATH_LOCK_TAG);
        return true;
    }

    public boolean isDeathLocked(ItemStack stack) {
        NBTTagCompound tag = getStorageTag(stack);
        return tag != null && tag.getBoolean(DEATH_LOCK_TAG);
    }

    public boolean isOwner(ItemStack stack, EntityPlayer player) {
        return player != null && isOwner(stack, player.getUniqueID());
    }

    public boolean isOwner(ItemStack stack, UUID playerId) {
        NBTTagCompound tag = getStorageTag(stack);
        return tag != null && playerId != null
                && tag.hasUniqueId(OWNER_TAG)
                && playerId.equals(tag.getUniqueId(OWNER_TAG));
    }

    public UUID getOwnerId(ItemStack stack) {
        NBTTagCompound tag = getStorageTag(stack);
        return tag != null && tag.hasUniqueId(OWNER_TAG) ? tag.getUniqueId(OWNER_TAG) : null;
    }

    public String getOwnerName(ItemStack stack) {
        NBTTagCompound tag = getStorageTag(stack);
        return tag == null ? "" : tag.getString(OWNER_NAME_TAG);
    }

    public boolean isOwnerTransferPending(ItemStack stack) {
        NBTTagCompound tag = getStorageTag(stack);
        return tag != null && tag.getBoolean(OWNER_TRANSFER_PENDING_TAG);
    }

    public boolean resetOwner(ItemStack stack, EntityPlayer player) {
        if (stack.isEmpty() || player == null || player.world.isRemote) return false;
        return resetOwnerTag(getOrCreateTag(stack), player.getUniqueID());
    }

    public AccessMode getAccessMode(ItemStack stack) {
        NBTTagCompound tag = getStorageTag(stack);
        int ordinal = tag == null ? 0 : tag.getInteger(ACCESS_MODE_TAG);
        AccessMode[] modes = AccessMode.values();
        return modes[Math.max(0, Math.min(ordinal, modes.length - 1))];
    }

    public boolean canAccess(ItemStack stack, EntityPlayer player) {
        if (!isDeathLocked(stack)) return true;
        if (player == null) return false;
        return canAccessLockedTag(getStorageTag(stack), player.getUniqueID());
    }

    static boolean canAccessLockedTag(NBTTagCompound tag, UUID playerId) {
        if (tag == null || playerId == null || !tag.hasUniqueId(OWNER_TAG)) return false;
        if (playerId.equals(tag.getUniqueId(OWNER_TAG))) return true;

        int ordinal = tag.getInteger(ACCESS_MODE_TAG);
        AccessMode[] modes = AccessMode.values();
        AccessMode mode = modes[Math.max(0, Math.min(ordinal, modes.length - 1))];
        if (mode == AccessMode.EVERYONE) return true;
        if (mode != AccessMode.ALLOWLIST) return false;

        NBTTagList allowed = tag.getTagList(ALLOWLIST_TAG, Constants.NBT.TAG_COMPOUND);
        for (int index = 0; index < allowed.tagCount(); index++) {
            NBTTagCompound entry = allowed.getCompoundTagAt(index);
            if (entry.hasUniqueId(PLAYER_TAG) && playerId.equals(entry.getUniqueId(PLAYER_TAG))) {
                return true;
            }
        }
        return false;
    }

    public boolean cycleAccessMode(ItemStack stack, EntityPlayer player) {
        if (!isOwner(stack, player)) return false;
        AccessMode next = AccessMode.values()[(getAccessMode(stack).ordinal() + 1) % AccessMode.values().length];
        getOrCreateTag(stack).setInteger(ACCESS_MODE_TAG, next.ordinal());
        return true;
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
        NBTTagCompound tag = getStorageTag(stack);
        if (tag == null) return Collections.emptyList();
        NBTTagList allowed = tag.getTagList(ALLOWLIST_TAG, Constants.NBT.TAG_COMPOUND);
        List<AllowedPlayer> result = new ArrayList<>(allowed.tagCount());
        for (int index = 0; index < allowed.tagCount(); index++) {
            NBTTagCompound entry = allowed.getCompoundTagAt(index);
            if (entry.hasUniqueId(PLAYER_TAG)) {
                result.add(new AllowedPlayer(entry.getUniqueId(PLAYER_TAG), entry.getString(PLAYER_NAME_TAG)));
            }
        }
        return result;
    }

    static boolean resetOwnerTag(NBTTagCompound tag, UUID requester) {
        if (tag == null || requester == null || !tag.hasUniqueId(OWNER_TAG)
                || !requester.equals(tag.getUniqueId(OWNER_TAG))) {
            return false;
        }
        removeUniqueId(tag, OWNER_TAG);
        tag.removeTag(OWNER_NAME_TAG);
        tag.removeTag(ALLOWLIST_TAG);
        tag.removeTag(ACCESS_MODE_TAG);
        tag.setBoolean(OWNER_TRANSFER_PENDING_TAG, true);
        return true;
    }

    static boolean bindPendingOwnerTag(NBTTagCompound tag, UUID playerId, String playerName) {
        if (tag == null || playerId == null || tag.hasUniqueId(OWNER_TAG)
                || !tag.getBoolean(OWNER_TRANSFER_PENDING_TAG)) {
            return false;
        }
        tag.setUniqueId(OWNER_TAG, playerId);
        tag.setString(OWNER_NAME_TAG, playerName == null ? "" : playerName);
        tag.removeTag(OWNER_TRANSFER_PENDING_TAG);
        return true;
    }

    private static void removeUniqueId(NBTTagCompound tag, String key) {
        tag.removeTag(key + "Most");
        tag.removeTag(key + "Least");
    }

    private static NBTTagCompound getStorageTag(ItemStack stack) {
        return stack.isEmpty() ? null : stack.getTagCompound();
    }

    private static NBTTagCompound getOrCreateTag(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        return tag;
    }

    @Override
    public int getEntityLifespan(ItemStack itemStack, World world) {
        return DROPPED_LIFESPAN_TICKS;
    }

    @Override
    public boolean onEntityItemUpdate(EntityItem entityItem) {
        if (entityItem.world.isRemote) {
            return false;
        }

        NBTTagCompound entityData = entityItem.getEntityData();
        long now = System.currentTimeMillis();
        if (!entityData.hasKey(DROPPED_AT_TAG, Constants.NBT.TAG_LONG)) {
            entityData.setLong(DROPPED_AT_TAG, now);
        } else if (now - entityData.getLong(DROPPED_AT_TAG) >= DROPPED_LIFESPAN_MILLIS) {
            entityItem.setDead();
        }
        return false;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        super.addInformation(stack, world, tooltip, flag);
        tooltip.add(TextFormatting.GOLD + I18nUtil.resolveKey("desc.backpack.black_box.death_container"));
        tooltip.add(TextFormatting.AQUA + I18nUtil.resolveKey("desc.backpack.black_box.dropped_protection"));
        String owner = getOwnerName(stack);
        if (!owner.isEmpty()) {
            tooltip.add(TextFormatting.GRAY + I18nUtil.resolveKey("desc.backpack.black_box.owner") + ": " + owner);
        }
        if (isDeathLocked(stack)) {
            tooltip.add(TextFormatting.DARK_RED + I18nUtil.resolveKey("desc.backpack.black_box.locked"));
        }
    }

    public enum AccessMode {
        OWNER_ONLY,
        ALLOWLIST,
        EVERYONE
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
