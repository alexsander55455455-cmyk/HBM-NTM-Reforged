package com.hbm.items.tool;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemBlackBoxBackpackAccessTest {

    @Test
    void ownerAllowlistAndEveryoneModesUseUuidOnly() {
        UUID owner = UUID.randomUUID();
        UUID allowed = UUID.randomUUID();
        UUID stranger = UUID.randomUUID();
        NBTTagCompound tag = new NBTTagCompound();
        tag.setUniqueId("hbmBlackBoxOwner", owner);

        assertTrue(ItemBlackBoxBackpack.canAccessLockedTag(tag, owner));
        assertFalse(ItemBlackBoxBackpack.canAccessLockedTag(tag, allowed));
        assertFalse(ItemBlackBoxBackpack.canAccessLockedTag(tag, stranger));

        NBTTagCompound entry = new NBTTagCompound();
        entry.setUniqueId("Player", allowed);
        entry.setString("Name", "A renamed player");
        NBTTagList allowlist = new NBTTagList();
        allowlist.appendTag(entry);
        tag.setTag("hbmBlackBoxAllowlist", allowlist);
        tag.setInteger("hbmBlackBoxAccessMode", ItemBlackBoxBackpack.AccessMode.ALLOWLIST.ordinal());

        assertTrue(ItemBlackBoxBackpack.canAccessLockedTag(tag, owner));
        assertTrue(ItemBlackBoxBackpack.canAccessLockedTag(tag, allowed));
        assertFalse(ItemBlackBoxBackpack.canAccessLockedTag(tag, stranger));

        tag.setInteger("hbmBlackBoxAccessMode", ItemBlackBoxBackpack.AccessMode.EVERYONE.ordinal());
        assertTrue(ItemBlackBoxBackpack.canAccessLockedTag(tag, stranger));

        NBTTagCompound reloaded = tag.copy();
        assertTrue(ItemBlackBoxBackpack.canAccessLockedTag(reloaded, owner));
        assertTrue(ItemBlackBoxBackpack.canAccessLockedTag(reloaded, allowed));
        assertTrue(ItemBlackBoxBackpack.canAccessLockedTag(reloaded, stranger));
    }

    @Test
    void resetPreservesStorageAndRequiresARealPendingPickupBeforeRebinding() {
        UUID owner = UUID.randomUUID();
        UUID nextOwner = UUID.randomUUID();
        NBTTagCompound tag = new NBTTagCompound();
        tag.setUniqueId("hbmBlackBoxOwner", owner);
        tag.setString("hbmBlackBoxOwnerName", "OldOwner");
        tag.setInteger("hbmBlackBoxAccessMode", ItemBlackBoxBackpack.AccessMode.EVERYONE.ordinal());
        tag.setTag("hbmBlackBoxAllowlist", new NBTTagList());
        tag.setInteger("InventorySentinel", 37);

        assertFalse(ItemBlackBoxBackpack.resetOwnerTag(tag, nextOwner));
        assertTrue(ItemBlackBoxBackpack.resetOwnerTag(tag, owner));
        assertFalse(tag.hasUniqueId("hbmBlackBoxOwner"));
        assertFalse(tag.hasKey("hbmBlackBoxOwnerName"));
        assertFalse(tag.hasKey("hbmBlackBoxAllowlist"));
        assertFalse(tag.hasKey("hbmBlackBoxAccessMode"));
        assertTrue(tag.getBoolean(ItemBlackBoxBackpack.OWNER_TRANSFER_PENDING_TAG));
        assertEquals(37, tag.getInteger("InventorySentinel"));

        assertTrue(ItemBlackBoxBackpack.bindPendingOwnerTag(tag, nextOwner, "NewOwner"));
        assertEquals(nextOwner, tag.getUniqueId("hbmBlackBoxOwner"));
        assertEquals("NewOwner", tag.getString("hbmBlackBoxOwnerName"));
        assertFalse(tag.getBoolean(ItemBlackBoxBackpack.OWNER_TRANSFER_PENDING_TAG));
        assertFalse(ItemBlackBoxBackpack.bindPendingOwnerTag(tag, owner, "OldOwner"));
    }
}
