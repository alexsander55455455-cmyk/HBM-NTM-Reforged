package com.hbm.items.tool;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemSmugglerBackpackAccessTest {

    @Test
    void allowlistUsesUuidAndSurvivesSerialization() {
        UUID allowed = UUID.randomUUID();
        UUID stranger = UUID.randomUUID();
        NBTTagCompound entry = new NBTTagCompound();
        entry.setUniqueId("Player", allowed);
        entry.setString("Name", "Allowed");
        NBTTagList allowlist = new NBTTagList();
        allowlist.appendTag(entry);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("SmugglerAllowlist", allowlist);

        assertTrue(ItemSmugglerBackpack.containsAllowedPlayer(tag, allowed));
        assertFalse(ItemSmugglerBackpack.containsAllowedPlayer(tag, stranger));
        assertTrue(ItemSmugglerBackpack.containsAllowedPlayer(tag.copy(), allowed));
    }

    @Test
    void resetClearsAccessButPreservesHiddenContentsUntilPickupTransfersOwnership() {
        UUID owner = UUID.randomUUID();
        UUID nextOwner = UUID.randomUUID();
        NBTTagCompound tag = new NBTTagCompound();
        tag.setUniqueId(ItemSmugglerBackpack.OWNER_TAG, owner);
        tag.setString(ItemSmugglerBackpack.OWNER_NAME_TAG, "OldOwner");
        tag.setTag("SmugglerAllowlist", new NBTTagList());
        tag.setInteger("HiddenInventorySentinel", 9);

        assertTrue(ItemSmugglerBackpack.resetOwnerTag(tag, owner));
        assertFalse(tag.hasUniqueId(ItemSmugglerBackpack.OWNER_TAG));
        assertFalse(tag.hasKey("SmugglerAllowlist"));
        assertTrue(tag.getBoolean(ItemBlackBoxBackpack.OWNER_TRANSFER_PENDING_TAG));
        assertEquals(9, tag.getInteger("HiddenInventorySentinel"));

        assertTrue(ItemSmugglerBackpack.bindPendingOwnerTag(tag, nextOwner, "NewOwner"));
        assertEquals(nextOwner, tag.getUniqueId(ItemSmugglerBackpack.OWNER_TAG));
        assertFalse(tag.getBoolean(ItemBlackBoxBackpack.OWNER_TRANSFER_PENDING_TAG));
        assertFalse(ItemSmugglerBackpack.bindPendingOwnerTag(tag, owner, "OldOwner"));
    }
}
