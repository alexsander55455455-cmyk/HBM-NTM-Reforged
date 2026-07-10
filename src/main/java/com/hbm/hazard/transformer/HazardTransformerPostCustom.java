package com.hbm.hazard.transformer;

import com.hbm.hazard.HazardEntry;
import com.hbm.inventory.RecipesCommon;
import com.hbm.main.MainRegistry;
import com.hbm.util.ItemStackUtil;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import static com.hbm.util.ContaminationUtil.NTM_NEUTRON_NBT_KEY;

public class HazardTransformerPostCustom implements IHazardTransformer {

    private static final Object2DoubleOpenHashMap<Item> ITEM_MULTIPLIERS = new Object2DoubleOpenHashMap<>();
    private static final Map<Item, ObjectArrayList<BiFunction<ItemStack, List<HazardEntry>, List<HazardEntry>>>> ITEM_POST = new Object2ObjectOpenHashMap<>();
    private static final Map<StackKey, ObjectArrayList<BiFunction<ItemStack, List<HazardEntry>, List<HazardEntry>>>> STACK_POST = new Object2ObjectOpenHashMap<>();
    /** item/meta keys that have at least one meta-only (nbt=null) stack post transformer */
    private static final Object2IntOpenHashMap<RecipesCommon.ComparableStack> STACK_POST_GENERIC_CANDIDATES = new Object2IntOpenHashMap<>();
    /** item/meta keys that have at least one NBT-sensitive stack post transformer */
    private static final Object2IntOpenHashMap<RecipesCommon.ComparableStack> STACK_POST_NBT_CANDIDATES = new Object2IntOpenHashMap<>();

    static {
        ITEM_MULTIPLIERS.defaultReturnValue(1.0);
        STACK_POST_GENERIC_CANDIDATES.defaultReturnValue(0);
        STACK_POST_NBT_CANDIDATES.defaultReturnValue(0);
    }

    private static void incrementStackPostCandidate(StackKey key) {
        Object2IntOpenHashMap<RecipesCommon.ComparableStack> map = key.nbt() == null ? STACK_POST_GENERIC_CANDIDATES : STACK_POST_NBT_CANDIDATES;
        map.addTo(key.base(), 1);
    }

    private static void decrementStackPostCandidate(StackKey key) {
        Object2IntOpenHashMap<RecipesCommon.ComparableStack> map = key.nbt() == null ? STACK_POST_GENERIC_CANDIDATES : STACK_POST_NBT_CANDIDATES;
        int count = map.addTo(key.base(), -1);
        if (count <= 0) {
            map.removeInt(key.base());
        }
    }

    private static boolean hasStackPostCandidate(RecipesCommon.ComparableStack base, boolean nbtSensitive) {
        Object2IntOpenHashMap<RecipesCommon.ComparableStack> map = nbtSensitive ? STACK_POST_NBT_CANDIDATES : STACK_POST_GENERIC_CANDIDATES;
        return map.getInt(base) > 0;
    }

    private static List<HazardEntry> safeApply(BiFunction<ItemStack, List<HazardEntry>, List<HazardEntry>> fn, ItemStack stack, List<HazardEntry> input) {
        try {
            return fn.apply(stack, Collections.unmodifiableList(input));
        } catch (Throwable t) {
            MainRegistry.logger.debug("PostTransformer exception", t);
            return input;
        }
    }

    public static void addItemPost(Item item, BiFunction<ItemStack, List<HazardEntry>, List<HazardEntry>> fn) {
        ITEM_POST.computeIfAbsent(item, k -> new ObjectArrayList<>()).add(fn);
    }

    public static void removeItemPost(Item item, BiFunction<ItemStack, List<HazardEntry>, List<HazardEntry>> fn) {
        ObjectArrayList<BiFunction<ItemStack, List<HazardEntry>, List<HazardEntry>>> list = ITEM_POST.get(item);
        if (list != null) {
            list.remove(fn);
            if (list.isEmpty()) ITEM_POST.remove(item);
        }
    }

    public static void addStackPost(StackKey key, BiFunction<ItemStack, List<HazardEntry>, List<HazardEntry>> fn) {
        ObjectArrayList<BiFunction<ItemStack, List<HazardEntry>, List<HazardEntry>>> list = STACK_POST.computeIfAbsent(key, k -> new ObjectArrayList<>());
        if (list.isEmpty()) {
            incrementStackPostCandidate(key);
        }
        list.add(fn);
    }

    public static void removeStackPost(StackKey key, BiFunction<ItemStack, List<HazardEntry>, List<HazardEntry>> fn) {
        ObjectArrayList<BiFunction<ItemStack, List<HazardEntry>, List<HazardEntry>>> list = STACK_POST.get(key);
        if (list != null) {
            list.remove(fn);
            if (list.isEmpty()) {
                STACK_POST.remove(key);
                decrementStackPostCandidate(key);
            }
        }
    }

    public static void setItemMultiplier(Item item, double multiplier) {
        ITEM_MULTIPLIERS.put(item, multiplier);
    }

    public static void clearItemMultiplier(Item item) {
        ITEM_MULTIPLIERS.removeDouble(item);
    }

    public static boolean hasItemMultiplier(Item item) {
        return ITEM_MULTIPLIERS.containsKey(item);
    }

    public static double getItemMultiplier(Item item) {
        return ITEM_MULTIPLIERS.getDouble(item);
    }

    private static void applyStackPostList(StackKey key, ItemStack stack, List<HazardEntry> entries) {
        List<BiFunction<ItemStack, List<HazardEntry>, List<HazardEntry>>> stackFns = STACK_POST.get(key);
        if (stackFns == null || stackFns.isEmpty()) return;

        List<HazardEntry> current = entries;
        for (BiFunction<ItemStack, List<HazardEntry>, List<HazardEntry>> fn : stackFns) {
            List<HazardEntry> next = safeApply(fn, stack, current);
            if (next != null) current = next;
        }
        if (current != entries) {
            entries.clear();
            entries.addAll(current);
        }
    }

    @Override
    public void transformPre(ItemStack stack, List<HazardEntry> entries) {
    }

    @Override
    public void transformPost(ItemStack stack, List<HazardEntry> entries) {
        if (stack == null || stack.isEmpty()) return;

        final Item item = stack.getItem();
        RecipesCommon.ComparableStack compStack = ItemStackUtil.comparableStackFrom(stack).makeSingular();
        boolean hasItemLevel = ITEM_MULTIPLIERS.containsKey(item) || ITEM_POST.containsKey(item);
        boolean hasGenericStackPost = hasStackPostCandidate(compStack, false);
        boolean hasNbtStackPost = hasStackPostCandidate(compStack, true);
        if (!hasItemLevel && !hasGenericStackPost && !hasNbtStackPost) {
            return;
        }

        // 1) Apply item-wide multiplier, if present
        if (ITEM_MULTIPLIERS.containsKey(item)) {
            double mult = ITEM_MULTIPLIERS.getDouble(item);
            if (Double.isNaN(mult)) mult = 1.0;
            if (Math.abs(mult - 1.0) > 1e-6) {
                List<HazardEntry> scaled = new ArrayList<>(entries.size());
                for (HazardEntry e : entries) {
                    if (e != null) scaled.add(e.clone(mult));
                }
                entries.clear();
                entries.addAll(scaled);
            }
        }

        // 2) Apply item-level post transforms (NBT-agnostic)
        List<BiFunction<ItemStack, List<HazardEntry>, List<HazardEntry>>> itemFns = ITEM_POST.get(item);
        if (itemFns != null && !itemFns.isEmpty()) {
            List<HazardEntry> current = entries;
            for (BiFunction<ItemStack, List<HazardEntry>, List<HazardEntry>> fn : itemFns) {
                List<HazardEntry> next = safeApply(fn, stack, current);
                if (next != null) current = next;
            }
            if (current != entries) {
                entries.clear();
                entries.addAll(current);
            }
        }

        // 3) Apply stack-level post transforms: generic (meta-only) first, then NBT-sensitive.
        if (hasGenericStackPost) {
            applyStackPostList(StackKey.of(stack, false), stack, entries);
        }
        if (hasNbtStackPost && stack.hasTagCompound()) {
            StackKey nbtKey = StackKey.of(stack, true);
            if (nbtKey.nbt() != null) {
                applyStackPostList(nbtKey, stack, entries);
            }
        }
    }

    public record StackKey(RecipesCommon.ComparableStack base, NBTTagCompound nbt) {
        private static NBTTagCompound copyStackTagSansNeutron(ItemStack stack) {
            if (!stack.hasTagCompound()) {
                return null;
            }
            try {
                NBTTagCompound copy = stack.getTagCompound().copy();
                if (copy.hasKey(NTM_NEUTRON_NBT_KEY)) {
                    copy.removeTag(NTM_NEUTRON_NBT_KEY);
                }
                return copy.isEmpty() ? null : copy;
            } catch (ConcurrentModificationException e) {
                return null;
            }
        }

        public static StackKey of(ItemStack stack, boolean respectNbt) {
            RecipesCommon.ComparableStack cs = ItemStackUtil.comparableStackFrom(stack).makeSingular();
            NBTTagCompound tag = respectNbt ? copyStackTagSansNeutron(stack) : null;
            return new StackKey(cs, tag);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof StackKey that)) return false;
            if (!base.equals(that.base)) return false;
            if (nbt == null && that.nbt == null) return true;
            if (nbt == null || that.nbt == null) return false;
            return nbt.equals(that.nbt); // mlbv: yes NBTUtil.areNBTEquals exists, but it would violate equals contract
        }

        @Override
        public int hashCode() {
            int h = base.hashCode();
            if (nbt != null) {
                h = 31 * h + nbt.hashCode();
            }
            return h;
        }

        @Override
        public String toString() {
            return "StackKey{base=" + base + ", nbt=" + (nbt == null ? "null" : nbt) + "}";
        }
    }
}
