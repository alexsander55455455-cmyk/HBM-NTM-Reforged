package com.hbm.handler.jei.transfer;

import com.hbm.config.GeneralConfig;
import com.hbm.items.machine.ItemFluidIcon;
import com.hbm.main.MainRegistry;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntLists;
import mezz.jei.JustEnoughItems;
import mezz.jei.api.IJeiHelpers;
import mezz.jei.api.gui.IGuiIngredient;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.recipe.transfer.IRecipeTransferRegistry;
import mezz.jei.config.ServerInfo;
import mezz.jei.network.packets.PacketRecipeTransfer;
import mezz.jei.startup.StackHelper;
import mezz.jei.util.Translator;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

public final class HbmTransferInfo<C extends Container> implements IRecipeTransferHandler<C> {

    private static IRecipeTransferHandlerHelper handlerHelper;
    private static StackHelper stackHelper;

    private final Class<C> containerClass;
    private final int[] recipeSlots;
    private final int[] playerSlots;
    private final List<Integer> craftingSlotsList;
    private final List<Integer> inventorySlotsList;

    public HbmTransferInfo(Class<C> containerClass, int[] recipeSlots, int[] playerSlots) {
        this.containerClass = containerClass;
        Arrays.sort(recipeSlots);
        Arrays.sort(playerSlots);
        this.recipeSlots = recipeSlots;
        this.playerSlots = playerSlots;
        this.craftingSlotsList = IntLists.unmodifiable(new IntArrayList(recipeSlots));
        this.inventorySlotsList = IntLists.unmodifiable(new IntArrayList(playerSlots));
    }

    private static boolean isFluidIcon(IGuiIngredient<ItemStack> ing) {
        for (ItemStack alt : ing.getAllIngredients()) {
            if (alt != null && !alt.isEmpty() && !(alt.getItem() instanceof ItemFluidIcon)) return false;
        }
        return true;
    }

    private static boolean verboseJeiTransferLogging() {
        return GeneralConfig.debugJeiTransfer;
    }

    /**
     * Capture JEI helpers once at plugin init time, before any recipes transfer.
     */
    public static void init(IJeiHelpers helpers) {
        handlerHelper = helpers.recipeTransferHandlerHelper();
        stackHelper = (StackHelper) helpers.getStackHelper();
    }

    /**
     * One-liner registration helper.
     */
    public static <C extends Container> void register(IRecipeTransferRegistry r, Class<C> cc, String uid,
                                                      int[] recipeSlots, int[] playerSlots) {
        r.addRecipeTransferHandler(new HbmTransferInfo<>(cc, recipeSlots, playerSlots), uid);
    }

    /**
     * Build a contiguous {@code [start, start+count)} index array.
     */
    public static int[] range(int start, int count) {
        int[] a = new int[count];
        for (int i = 0; i < count; i++) a[i] = start + i;
        return a;
    }

    @Override
    public Class<C> getContainerClass() {
        return containerClass;
    }

    @Override
    public @Nullable IRecipeTransferError transferRecipe(C container, IRecipeLayout recipeLayout, EntityPlayer player,
                                                         boolean maxTransfer, boolean doTransfer) {
        if (!ServerInfo.isJeiOnServer()) {
            return handlerHelper.createUserErrorWithTooltip(
                    Translator.translateToLocal("jei.tooltip.error.recipe.transfer.no.server"));
        }

        String recipeUid = recipeLayout.getRecipeCategory().getUid();

        Map<Integer, ? extends IGuiIngredient<ItemStack>> rawIngredients = recipeLayout.getItemStacks()
                                                                                       .getGuiIngredients();
        int itemInputCount = 0;
        int fluidInputCount = 0;
        for (IGuiIngredient<ItemStack> ing : rawIngredients.values()) {
            if (!ing.isInput() || ing.getAllIngredients().isEmpty()) continue;
            if (isFluidIcon(ing)) fluidInputCount++;
            else itemInputCount++;
        }

        if (itemInputCount > recipeSlots.length) {
            logTransferFailure(recipeUid, maxTransfer, doTransfer, "internal error: itemInputCount > recipeSlots");
            return handlerHelper.createInternalError();
        }

        Map<Integer, ? extends IGuiIngredient<ItemStack>> matchIngredients;
        if (fluidInputCount == 0) {
            matchIngredients = rawIngredients;
        } else {
            Int2ObjectOpenHashMap<IGuiIngredient<ItemStack>> filtered = new Int2ObjectOpenHashMap<>(
                    rawIngredients.size() - fluidInputCount);
            for (Map.Entry<Integer, ? extends IGuiIngredient<ItemStack>> e : rawIngredients.entrySet()) {
                IGuiIngredient<ItemStack> ing = e.getValue();
                if (ing.isInput() && isFluidIcon(ing)) continue;
                filtered.put(e.getKey().intValue(), ing);
            }
            matchIngredients = filtered;
        }

        SlotValidationResult validation = filterIngredientsBySlotValidity(container, matchIngredients, recipeUid);

        if (!validation.invalidSlotGuiIndices.isEmpty()) {
            for (int guiIndex : validation.invalidSlotGuiIndices) {
                logTransferFailure(recipeUid, maxTransfer, doTransfer,
                        "invalid slot guiIndex=" + guiIndex + " reason=no valid alternative for target slot");
            }
            return handlerHelper.createUserErrorForSlots(
                    Translator.translateToLocal("jei.tooltip.error.recipe.transfer.missing"), validation.invalidSlotGuiIndices);
        }

        Int2ObjectOpenHashMap<ItemStack> availableItems = new Int2ObjectOpenHashMap<>(
                recipeSlots.length + playerSlots.length);
        int filledRecipeSlotCount = 0;
        for (int idx : recipeSlots) {
            Slot s = container.getSlot(idx);
            ItemStack stack = s.getStack();
            if (stack.isEmpty()) continue;
            if (!s.canTakeStack(player)) {
                logTransferFailure(recipeUid, maxTransfer, doTransfer, "internal error: cannot take from slot " + idx);
                return handlerHelper.createInternalError();
            }
            filledRecipeSlotCount++;
            availableItems.put(idx, stack.copy());
        }
        int emptyInventorySlotCount = 0;
        for (int idx : playerSlots) {
            ItemStack stack = container.getSlot(idx).getStack();
            if (stack.isEmpty()) emptyInventorySlotCount++;
            else availableItems.put(idx, stack.copy());
        }
        if (filledRecipeSlotCount - itemInputCount > emptyInventorySlotCount) {
            logTransferFailure(recipeUid, maxTransfer, doTransfer, "inventory full");
            return handlerHelper.createUserErrorWithTooltip(
                    Translator.translateToLocal("jei.tooltip.error.recipe.transfer.inventory.full"));
        }

        StackHelper.MatchingItemsResult match =
                stackHelper.getMatchingItems(availableItems, validation.ingredients);
        if (!match.missingItems.isEmpty()) {
            for (Integer missingGuiIndex : match.missingItems) {
                IGuiIngredient<ItemStack> ing = validation.ingredients.get(missingGuiIndex);
                logTransferFailure(recipeUid, maxTransfer, doTransfer,
                        "missing items guiIndex=" + missingGuiIndex + " alternatives=" + describeAlternatives(ing));
            }
            return handlerHelper.createUserErrorForSlots(
                    Translator.translateToLocal("jei.tooltip.error.recipe.transfer.missing"), match.missingItems);
        }

        if (doTransfer) {
            logTransferAttempt(recipeUid, maxTransfer, match, validation.recipeSlotToGuiIndex, availableItems);
            PacketRecipeTransfer packet = new PacketRecipeTransfer(match.matchingItems, craftingSlotsList,
                    inventorySlotsList, maxTransfer ? Integer.MAX_VALUE : 1, false, false);
            JustEnoughItems.getProxy().sendPacketToServer(packet);
        }
        return null;
    }

    private void logTransferFailure(String recipeUid, boolean maxTransfer, boolean doTransfer, String reason) {
        MainRegistry.logger.warn("[JEI Transfer] failed: container={} recipe={} maxTransfer={} doTransfer={} {}",
                containerClass.getName(), recipeUid, maxTransfer, doTransfer, reason);
    }

    private void logTransferAttempt(String recipeUid, boolean maxTransfer, StackHelper.MatchingItemsResult match,
                                    Int2IntOpenHashMap recipeSlotToGuiIndex,
                                    Int2ObjectOpenHashMap<ItemStack> availableItems) {
        MainRegistry.logger.info("[JEI Transfer] sending: container={} recipe={} maxTransfer={}",
                containerClass.getName(), recipeUid, maxTransfer);
        for (Map.Entry<Integer, Integer> entry : match.matchingItems.entrySet()) {
            int recipeSlotIndex = entry.getKey();
            int sourceContainerSlot = entry.getValue();
            int guiIndex = recipeSlotToGuiIndex.getOrDefault(recipeSlotIndex, -1);
            int targetContainerSlot = recipeSlotIndex < recipeSlots.length ? recipeSlots[recipeSlotIndex] : -1;
            ItemStack selected = availableItems.get(sourceContainerSlot);
            MainRegistry.logger.info(
                    "[JEI Transfer] slot: container={} recipe={} maxTransfer={} guiIndex={} targetSlot={} sourceSlot={} selected={}",
                    containerClass.getName(), recipeUid, maxTransfer, guiIndex, targetContainerSlot, sourceContainerSlot,
                    selected == null ? "null" : selected);
        }
    }

    private SlotValidationResult filterIngredientsBySlotValidity(
            C container,
            Map<Integer, ? extends IGuiIngredient<ItemStack>> matchIngredients,
            String recipeUid) {
        Int2ObjectOpenHashMap<IGuiIngredient<ItemStack>> filtered =
                new Int2ObjectOpenHashMap<>(matchIngredients.size());
        IntArrayList invalidSlotGuiIndices = new IntArrayList();
        Int2IntOpenHashMap recipeSlotToGuiIndex = new Int2IntOpenHashMap();
        int recipeSlotIndex = 0;
        SortedSet<Integer> keys = new TreeSet<>(matchIngredients.keySet());
        for (Integer key : keys) {
            IGuiIngredient<ItemStack> ing = matchIngredients.get(key);
            if (!ing.isInput() || ing.getAllIngredients().isEmpty()) {
                filtered.put(key.intValue(), ing);
                continue;
            }
            if (isFluidIcon(ing)) {
                filtered.put(key.intValue(), ing);
                continue;
            }
            if (recipeSlotIndex >= recipeSlots.length) {
                filtered.put(key.intValue(), ing);
                continue;
            }

            Slot targetSlot = container.getSlot(recipeSlots[recipeSlotIndex]);
            List<ItemStack> validAlternatives = new ArrayList<>();
            for (ItemStack alt : ing.getAllIngredients()) {
                if (alt != null && !alt.isEmpty() && targetSlot.isItemValid(alt)) {
                    validAlternatives.add(alt);
                }
            }

            if (verboseJeiTransferLogging()) {
                MainRegistry.logger.info(
                        "[JEI Transfer] slot check: container={} recipe={} guiIndex={} targetSlot={} requested={} valid={}",
                        containerClass.getName(), recipeUid, key, recipeSlots[recipeSlotIndex],
                        describeAlternatives(ing), validAlternatives);
            }

            recipeSlotToGuiIndex.put(recipeSlotIndex, key.intValue());
            if (validAlternatives.isEmpty()) {
                invalidSlotGuiIndices.add(key.intValue());
            } else {
                filtered.put(key.intValue(), new FilteredGuiIngredient(ing, validAlternatives));
            }
            recipeSlotIndex++;
        }
        return new SlotValidationResult(filtered, invalidSlotGuiIndices, recipeSlotToGuiIndex);
    }

    private static String describeAlternatives(@Nullable IGuiIngredient<ItemStack> ing) {
        if (ing == null) {
            return "[]";
        }
        List<ItemStack> alts = ing.getAllIngredients();
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < alts.size(); i++) {
            if (i > 0) sb.append(", ");
            ItemStack alt = alts.get(i);
            sb.append(alt == null || alt.isEmpty() ? "empty" : alt);
        }
        sb.append(']');
        return sb.toString();
    }

    private record SlotValidationResult(
            Map<Integer, IGuiIngredient<ItemStack>> ingredients,
            IntArrayList invalidSlotGuiIndices,
            Int2IntOpenHashMap recipeSlotToGuiIndex) {
    }

    private static final class FilteredGuiIngredient implements IGuiIngredient<ItemStack> {
        private final IGuiIngredient<ItemStack> base;
        private final List<ItemStack> filtered;

        private FilteredGuiIngredient(IGuiIngredient<ItemStack> base, List<ItemStack> filtered) {
            this.base = base;
            this.filtered = filtered;
        }

        @Override
        public @Nullable ItemStack getDisplayedIngredient() {
            return filtered.isEmpty() ? null : filtered.get(0);
        }

        @Override
        public List<ItemStack> getAllIngredients() {
            return filtered;
        }

        @Override
        public boolean isInput() {
            return base.isInput();
        }

        @Override
        public void drawHighlight(Minecraft minecraft, Color color, int xOffset, int yOffset) {
            base.drawHighlight(minecraft, color, xOffset, yOffset);
        }
    }
}