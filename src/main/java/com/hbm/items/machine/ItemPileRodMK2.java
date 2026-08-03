package com.hbm.items.machine;

import com.hbm.items.ItemEnumMulti;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;

/**
 * Fuel and utility rods for the second-generation Chicago Pile.
 *
 * <p>Depletion is stored on the stack because rods may be moved through loaders,
 * dropped, saved in the reactor and inserted again without losing their state.</p>
 */
public class ItemPileRodMK2 extends ItemEnumMulti<ItemPileRodMK2.EnumPileRod> {

    public static final String KEY_NBT_DEPLETION = "depletion";

    public ItemPileRodMK2(String registryName) {
        super(registryName, EnumPileRod.values(), true, true);
        setMaxStackSize(1);
    }

    public enum EnumPileRod {
        RA226BE(1D),
        PO210BE(1D),
        ZR(0D, 0D, 0D, 2),
        NU(1D, 25_000D, 0.25D, 4),
        PU239(1D, 500D, 0.5D, 5),
        RGP(1D, 1_000D, 0.5D, 6),
        WASTE(1D, 0D, 1.5D, 6);

        public final double reactionMult;
        public final double life;
        public final double heatMult;
        public final double neutronSource;
        public final int turnsInto;

        EnumPileRod(double neutronSource) {
            this.neutronSource = neutronSource;
            this.reactionMult = 0D;
            this.life = 0D;
            this.heatMult = 0D;
            this.turnsInto = ordinal();
        }

        EnumPileRod(double reactionMult, double life, double heatMult, int turnsInto) {
            this.neutronSource = 0D;
            this.reactionMult = reactionMult;
            this.life = life;
            this.heatMult = heatMult;
            this.turnsInto = turnsInto;
        }
    }

    public static EnumPileRod getRod(ItemStack stack) {
        int meta = stack.isEmpty() ? 0 : stack.getMetadata();
        EnumPileRod[] rods = EnumPileRod.values();
        return rods[Math.max(0, Math.min(meta, rods.length - 1))];
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        EnumPileRod rod = getRod(stack);
        if (rod.life > 0D) {
            tooltip.add(TextFormatting.GREEN + I18n.format(
                    "desc.pile_rod.lifetime", (int) Math.round(rod.life)));
            double depletion = getDepletionPercent(stack);
            if (depletion > 0D) {
                tooltip.add(TextFormatting.YELLOW + I18n.format(
                        "desc.pile_rod.depletion", String.format(Locale.US, "%.1f", depletion)));
            }
        }
        String key = getTranslationKey(stack) + ".desc";
        tooltip.add(TextFormatting.YELLOW + net.minecraft.client.resources.I18n.format(key));
    }

    @Override
    public boolean showDurabilityBar(ItemStack stack) {
        return getDurabilityForDisplay(stack) > 0D;
    }

    @Override
    public double getDurabilityForDisplay(ItemStack stack) {
        EnumPileRod rod = getRod(stack);
        return rod.life <= 0D ? 0D : Math.min(1D, getDepletion(stack) / rod.life);
    }

    public static double getDepletionPercent(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0D;
        EnumPileRod rod = getRod(stack);
        return rod.life <= 0D ? 0D : Math.min(100D, getDepletion(stack) * 100D / rod.life);
    }

    public static double getDepletion(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasTagCompound()) return 0D;
        return stack.getTagCompound().getDouble(KEY_NBT_DEPLETION);
    }

    public static void setDepletion(ItemStack stack, double depletion) {
        if (stack == null || stack.isEmpty()) return;
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        tag.setDouble(KEY_NBT_DEPLETION, Math.max(0D, depletion));
    }

    public static double getReactivity(ItemStack stack, double incomingFlux) {
        EnumPileRod rod = getRod(stack);
        double outgoingFlux = rod.neutronSource;
        if (rod.reactionMult > 0D) {
            outgoingFlux += squirt(incomingFlux) * rod.reactionMult;
        }
        return outgoingFlux;
    }

    private static double squirt(double value) {
        return Math.sqrt(value + 1D / ((value + 2D) * (value + 2D))) - 1D / (value + 2D);
    }

    public static double getHeatPerNeutron(ItemStack stack) {
        return getRod(stack).heatMult;
    }

    public static ItemStack react(ItemStack original, double incomingFlux) {
        if (original == null || original.isEmpty()) return ItemStack.EMPTY;
        EnumPileRod rod = getRod(original);
        if (rod.life <= 0D) return original;

        double depletion = getDepletion(original) + Math.max(0D, incomingFlux);
        if (depletion < rod.life) {
            setDepletion(original, depletion);
            return original;
        }
        return new ItemStack(original.getItem(), 1, rod.turnsInto);
    }
}
