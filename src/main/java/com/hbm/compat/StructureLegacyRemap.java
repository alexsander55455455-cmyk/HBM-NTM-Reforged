package com.hbm.compat;

import com.hbm.Tags;
import com.hbm.blocks.BlockEnums;
import com.hbm.main.MainRegistry;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Shared legacy block/item/tile-entity remapping for structure NBT.
 * Used by Reforged NBTStructure worldgen and optional ntmdopolnenie compat.
 */
public final class StructureLegacyRemap {

    private static final Map<String, String> SUBSTITUTIONS = new HashMap<>();

    private static final String[] LEGACY_BRICK_SLABS = {
            Tags.MODID + ":reinforced_stone_slab",
            Tags.MODID + ":reinforced_brick_slab",
            Tags.MODID + ":brick_obsidian_slab",
            Tags.MODID + ":brick_light_slab",
            Tags.MODID + ":brick_compound_slab",
            Tags.MODID + ":brick_asbestos_slab",
            Tags.MODID + ":brick_fire_slab"
    };
    private static final String[] LEGACY_BRICK_DOUBLE_SLABS = {
            Tags.MODID + ":reinforced_stone_double_slab",
            Tags.MODID + ":reinforced_brick_double_slab",
            Tags.MODID + ":brick_obsidian_double_slab",
            Tags.MODID + ":brick_light_double_slab",
            Tags.MODID + ":brick_compound_double_slab",
            Tags.MODID + ":brick_asbestos_double_slab",
            Tags.MODID + ":brick_fire_double_slab"
    };
    private static final String[] LEGACY_CONCRETE_BRICK_SLABS = {
            Tags.MODID + ":brick_concrete_slab",
            Tags.MODID + ":brick_concrete_mossy_slab",
            Tags.MODID + ":brick_concrete_cracked_slab",
            Tags.MODID + ":brick_concrete_broken_slab",
            Tags.MODID + ":ducrete_brick_slab"
    };
    private static final String[] LEGACY_CONCRETE_BRICK_DOUBLE_SLABS = {
            Tags.MODID + ":brick_concrete_double_slab",
            Tags.MODID + ":brick_concrete_mossy_double_slab",
            Tags.MODID + ":brick_concrete_cracked_double_slab",
            Tags.MODID + ":brick_concrete_broken_double_slab",
            Tags.MODID + ":ducrete_brick_double_slab"
    };

    static {
        SUBSTITUTIONS.put(Tags.MODID + ":tile.ore_coal_oil", "minecraft:coal_ore");
        SUBSTITUTIONS.put(Tags.MODID + ":ore_coal_oil", "minecraft:coal_ore");
        SUBSTITUTIONS.put(Tags.MODID + ":fluid_duct_neo", Tags.MODID + ":fluid_duct_mk2");
        SUBSTITUTIONS.put(Tags.MODID + ":rail_narrow", "minecraft:rail");
        SUBSTITUTIONS.put(Tags.MODID + ":ff_fludi_duct_mk2", Tags.MODID + ":ff_fluid_duct_mk2");
        SUBSTITUTIONS.put(Tags.MODID + ":turret_ciws", Tags.MODID + ":turret_cwis");

        for (EnumDyeColor color : EnumDyeColor.values()) {
            String dye = color.getName();
            SUBSTITUTIONS.put(
                    Tags.MODID + ":concrete_" + dye + "_stairs",
                    Tags.MODID + ":concrete_colored_stairs_" + dye);
        }
    }

    private StructureLegacyRemap() {
    }

    public static @Nullable String remapLegacyBlockName(String name) {
        String substituted = SUBSTITUTIONS.get(name);
        if (substituted != null) {
            return substituted;
        }

        String prefix = Tags.MODID + ":tile.";
        if (name.startsWith(prefix)) {
            String stripped = Tags.MODID + ":" + name.substring(prefix.length());
            return SUBSTITUTIONS.getOrDefault(stripped, stripped);
        }

        return null;
    }

    public static @Nullable LegacyBlockDefinition remapLegacyBlockDefinition(String name, int meta) {
        String normalized = remapLegacyBlockName(name);
        if (normalized == null) {
            normalized = name;
        }

        LegacyBlockDefinition cap = remapLegacyBlockCap(normalized);
        if (cap != null) {
            return cap;
        }

        return switch (normalized) {
            case Tags.MODID + ":brick_slab" -> remapLegacySlab(meta, LEGACY_BRICK_SLABS, true);
            case Tags.MODID + ":brick_double_slab" -> remapLegacySlab(meta, LEGACY_BRICK_DOUBLE_SLABS, false);
            case Tags.MODID + ":concrete_brick_slab" -> remapLegacySlab(meta, LEGACY_CONCRETE_BRICK_SLABS, true);
            case Tags.MODID + ":concrete_brick_double_slab" -> remapLegacySlab(meta, LEGACY_CONCRETE_BRICK_DOUBLE_SLABS, false);
            default -> null;
        };
    }

    private static @Nullable LegacyBlockDefinition remapLegacyBlockCap(String normalized) {
        if (!normalized.startsWith(Tags.MODID + ":block_cap_")) {
            return null;
        }

        String suffix = normalized.substring((Tags.MODID + ":block_cap_").length()).toUpperCase(Locale.US);
        for (BlockEnums.EnumBlockCapType type : BlockEnums.EnumBlockCapType.VALUES) {
            if (type.name().equals(suffix)) {
                return new LegacyBlockDefinition(Tags.MODID + ":block_cap", type.ordinal());
            }
        }

        return null;
    }

    public static void fixLegacyBlockNames(NBTBase tag) {
        if (tag instanceof NBTTagCompound compound) {
            if (compound.hasKey("block", Constants.NBT.TAG_STRING)) {
                int meta = compound.hasKey("meta", Constants.NBT.TAG_ANY_NUMERIC) ? compound.getInteger("meta") : 0;
                LegacyBlockDefinition remappedDefinition = remapLegacyBlockDefinition(compound.getString("block"), meta);
                if (remappedDefinition != null) {
                    compound.setString("block", remappedDefinition.name);
                    compound.setInteger("meta", remappedDefinition.meta);
                } else {
                    String remapped = remapLegacyBlockName(compound.getString("block"));
                    if (remapped != null) {
                        compound.setString("block", remapped);
                    }
                }
            }

            for (String key : compound.getKeySet()) {
                fixLegacyBlockNames(compound.getTag(key));
            }
            return;
        }

        if (tag instanceof NBTTagList list) {
            for (int i = 0; i < list.tagCount(); i++) {
                fixLegacyBlockNames(list.get(i));
            }
        }
    }

    public static void fixLegacyItemStackIds(NBTBase tag, @Nullable Map<Short, String> idPalette) {
        if (tag instanceof NBTTagCompound compound) {
            if (looksLikeLegacyItemStack(compound)) {
                String remapped = remapLegacyItemId(compound, idPalette);
                if (remapped != null && !remapped.isEmpty()) {
                    compound.setString("id", remapped);
                }
            }

            for (String key : compound.getKeySet()) {
                fixLegacyItemStackIds(compound.getTag(key), idPalette);
            }
            return;
        }

        if (tag instanceof NBTTagList list) {
            for (int i = 0; i < list.tagCount(); i++) {
                fixLegacyItemStackIds(list.get(i), idPalette);
            }
        }
    }

    public static void sanitizeTileEntityNbt(NBTTagCompound teNbt, @Nullable Map<Short, String> idPalette) {
        if (teNbt == null) {
            return;
        }

        fixLegacyBlockNames(teNbt);
        fixLegacyItemStackIds(teNbt, idPalette);
        remapLegacyTileEntityId(teNbt);
        synthesizeInventoryWrapper(teNbt, idPalette);
    }

    public static void remapVanillaStructureNbt(NBTTagCompound compound) {
        if (compound == null) {
            return;
        }

        if (compound.hasKey("palette", Constants.NBT.TAG_LIST)) {
            NBTTagList palette = compound.getTagList("palette", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < palette.tagCount(); i++) {
                NBTTagCompound entry = palette.getCompoundTagAt(i);
                if (!entry.hasKey("Name", Constants.NBT.TAG_STRING)) {
                    continue;
                }
                String name = entry.getString("Name");
                LegacyBlockDefinition remappedDefinition = remapLegacyBlockDefinition(name, 0);
                if (remappedDefinition != null) {
                    entry.setString("Name", remappedDefinition.name);
                } else {
                    String remapped = remapLegacyBlockName(name);
                    if (remapped != null) {
                        entry.setString("Name", remapped);
                    }
                }
            }
        }

        if (compound.hasKey("blocks", Constants.NBT.TAG_LIST)) {
            NBTTagList blocks = compound.getTagList("blocks", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < blocks.tagCount(); i++) {
                NBTTagCompound block = blocks.getCompoundTagAt(i);
                if (block.hasKey("nbt", Constants.NBT.TAG_COMPOUND)) {
                    sanitizeTileEntityNbt(block.getCompoundTag("nbt"), null);
                }
            }
        }
    }

    public static Block resolveBlockName(String name) {
        LegacyBlockDefinition remappedDefinition = remapLegacyBlockDefinition(name, 0);
        if (remappedDefinition != null) {
            Block block = Block.getBlockFromName(remappedDefinition.name);
            if (block != null) {
                return block;
            }
        }

        String remapped = remapLegacyBlockName(name);
        if (remapped != null) {
            Block block = Block.getBlockFromName(remapped);
            if (block != null) {
                return block;
            }
        }

        Block block = Block.getBlockFromName(name);
        if (block != null) {
            return block;
        }

        return Blocks.AIR;
    }

    private static void remapLegacyTileEntityId(NBTTagCompound teNbt) {
        if (!teNbt.hasKey("id", Constants.NBT.TAG_STRING)) {
            return;
        }

        String id = teNbt.getString("id");
        if (id.startsWith(Tags.MODID + ":tileentity_")) {
            return;
        }
        if (id.startsWith("tileentity_")) {
            teNbt.setString("id", Tags.MODID + ":" + id);
        }
    }

    private static void synthesizeInventoryWrapper(NBTTagCompound teNbt, @Nullable Map<Short, String> idPalette) {
        if (teNbt.hasKey("inventory", Constants.NBT.TAG_COMPOUND)) {
            return;
        }

        String listKey = null;
        if (teNbt.hasKey("items", Constants.NBT.TAG_LIST)) {
            listKey = "items";
        } else if (teNbt.hasKey("Items", Constants.NBT.TAG_LIST)) {
            listKey = "Items";
        }
        if (listKey == null) {
            return;
        }

        NBTTagList oldList = teNbt.getTagList(listKey, Constants.NBT.TAG_COMPOUND);
        if (oldList.tagCount() == 0) {
            return;
        }

        NBTTagList newItems = new NBTTagList();
        for (int i = 0; i < oldList.tagCount(); i++) {
            NBTTagCompound oldStack = oldList.getCompoundTagAt(i);
            int slot = getSlot(oldStack);
            if (slot < 0) {
                continue;
            }

            String idString = resolveItemId(oldStack, idPalette);
            if (idString == null || idString.isEmpty()) {
                continue;
            }

            NBTTagCompound newStack = new NBTTagCompound();
            newStack.setInteger("Slot", slot);
            newStack.setString("id", idString);
            if (oldStack.hasKey("Count", Constants.NBT.TAG_BYTE)) {
                newStack.setByte("Count", oldStack.getByte("Count"));
            } else {
                newStack.setByte("Count", (byte) 1);
            }
            if (oldStack.hasKey("Damage", Constants.NBT.TAG_SHORT)) {
                newStack.setShort("Damage", oldStack.getShort("Damage"));
            } else {
                newStack.setShort("Damage", (short) 0);
            }
            if (oldStack.hasKey("tag", Constants.NBT.TAG_COMPOUND)) {
                newStack.setTag("tag", oldStack.getCompoundTag("tag"));
            }
            if (oldStack.hasKey("ForgeCaps", Constants.NBT.TAG_COMPOUND)) {
                newStack.setTag("ForgeCaps", oldStack.getCompoundTag("ForgeCaps"));
            }

            newItems.appendTag(newStack);
        }

        if (newItems.tagCount() == 0) {
            return;
        }

        NBTTagCompound invTag = new NBTTagCompound();
        invTag.setTag("Items", newItems);
        teNbt.setTag("inventory", invTag);
    }

    private static boolean looksLikeLegacyItemStack(NBTTagCompound tag) {
        if (!tag.hasKey("id")) {
            return false;
        }

        return tag.hasKey("Count", Constants.NBT.TAG_BYTE)
                || tag.hasKey("Damage", Constants.NBT.TAG_SHORT)
                || tag.hasKey("tag", Constants.NBT.TAG_COMPOUND)
                || tag.hasKey("ForgeCaps", Constants.NBT.TAG_COMPOUND)
                || tag.hasKey("Slot", Constants.NBT.TAG_BYTE)
                || tag.hasKey("Slot", Constants.NBT.TAG_INT)
                || tag.hasKey("slot", Constants.NBT.TAG_BYTE)
                || tag.hasKey("slot", Constants.NBT.TAG_INT);
    }

    private static @Nullable String remapLegacyItemId(NBTTagCompound itemTag, @Nullable Map<Short, String> idPalette) {
        if (itemTag.hasKey("id", Constants.NBT.TAG_STRING)) {
            String idString = itemTag.getString("id");
            if (idString.isEmpty()) {
                return null;
            }

            Item item = Item.getByNameOrId(idString);
            if (item != null && item.getRegistryName() != null) {
                return item.getRegistryName().toString();
            }

            return idString.contains(":") ? idString : null;
        }

        if (itemTag.hasKey("id", Constants.NBT.TAG_SHORT)) {
            return mapId(itemTag.getShort("id"), idPalette);
        }

        if (itemTag.hasKey("id", Constants.NBT.TAG_INT)) {
            return mapId(itemTag.getInteger("id"), idPalette);
        }

        return null;
    }

    private static @Nullable String resolveItemId(NBTTagCompound oldStack, @Nullable Map<Short, String> idPalette) {
        if (oldStack.hasKey("id", Constants.NBT.TAG_STRING)) {
            return oldStack.getString("id");
        }
        if (oldStack.hasKey("id", Constants.NBT.TAG_SHORT)) {
            return mapId(oldStack.getShort("id"), idPalette);
        }
        if (oldStack.hasKey("id", Constants.NBT.TAG_INT)) {
            return mapId(oldStack.getInteger("id"), idPalette);
        }
        return null;
    }

    private static int getSlot(NBTTagCompound tag) {
        int id = tag.getTagId("slot");
        if (id != 0) {
            if (id == Constants.NBT.TAG_BYTE) {
                return tag.getByte("slot") & 0xFF;
            } else if (id == Constants.NBT.TAG_INT) {
                return tag.getInteger("slot");
            }
        } else {
            id = tag.getTagId("Slot");
            if (id != 0) {
                if (id == Constants.NBT.TAG_BYTE) {
                    return tag.getByte("Slot") & 0xFF;
                } else if (id == Constants.NBT.TAG_INT) {
                    return tag.getInteger("Slot");
                }
            }
        }
        return -1;
    }

    private static @Nullable String mapId(int legacyId, @Nullable Map<Short, String> idPalette) {
        if (legacyId >= Short.MIN_VALUE && legacyId <= Short.MAX_VALUE && idPalette != null) {
            String name = idPalette.get((short) legacyId);
            if (name != null && !name.isEmpty()) {
                return name;
            }
        }

        Item item = Item.getItemById(legacyId);
        if (item != null && item.getRegistryName() != null) {
            return item.getRegistryName().toString();
        }

        MainRegistry.logger.debug("[StructureLegacyRemap] Could not resolve legacy item id {}", legacyId);
        return null;
    }

    private static @Nullable LegacyBlockDefinition remapLegacySlab(int meta, String[] variants, boolean preserveHalf) {
        int variant = meta & 7;
        if (variant < 0 || variant >= variants.length) {
            return null;
        }

        return new LegacyBlockDefinition(variants[variant], preserveHalf ? meta & 8 : 0);
    }

    public static final class LegacyBlockDefinition {
        public final String name;
        public final int meta;

        private LegacyBlockDefinition(String name, int meta) {
            this.name = name;
            this.meta = meta;
        }
    }
}