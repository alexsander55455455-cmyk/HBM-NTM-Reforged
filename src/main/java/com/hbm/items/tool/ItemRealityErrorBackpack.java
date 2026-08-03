package com.hbm.items.tool;

import com.hbm.config.BackpackConfig;
import com.hbm.inventory.IBackpackInventory;
import com.hbm.inventory.RealityErrorBackpackInventory;
import com.hbm.util.I18nUtil;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

public class ItemRealityErrorBackpack extends ItemBackpack {

    public static final int SLOTS = 1;
    private static final String GLITCH_GLYPHS = "#@!?/\\[]{}<>";
    private static final TextFormatting[] GLITCH_COLORS = {
            TextFormatting.DARK_PURPLE,
            TextFormatting.LIGHT_PURPLE,
            TextFormatting.AQUA,
            TextFormatting.RED,
            TextFormatting.YELLOW,
            TextFormatting.WHITE
    };

    public ItemRealityErrorBackpack(String name) {
        super(name, SLOTS, 1D, false);
        setCreativeTab(null);
    }

    @Override
    public IBackpackInventory createInventory(ItemStack backpack, @Nullable World world) {
        if (BackpackConfig.usesSparseStorage(backpack)) return super.createInventory(backpack, world);
        return RealityErrorBackpackInventory.create(this, backpack, world);
    }

    @Override
    public int getFilledSlotCount(ItemStack stack) {
        if (BackpackConfig.usesSparseStorage(stack)) return super.getFilledSlotCount(stack);
        return RealityErrorBackpackInventory.countStoredValues(stack);
    }

    @Override
    public int getDroppedLavaSurvivalTicks() {
        return -1;
    }

    @Override
    public @NotNull String getItemStackDisplayName(@NotNull ItemStack stack) {
        return glitchText(super.getItemStackDisplayName(stack));
    }

    public static String glitchText(String text) {
        String plain = TextFormatting.getTextWithoutFormattingCodes(text);
        if (plain == null || plain.isEmpty()) {
            return "";
        }

        long frame = System.currentTimeMillis() / 90L;
        Random random = new Random(frame ^ (long) plain.hashCode() * 0x9E3779B97F4A7C15L);
        StringBuilder glitched = new StringBuilder(plain.length() * 3 + 24);
        appendNoise(glitched, random);
        for (int index = 0; index < plain.length(); index++) {
            char character = plain.charAt(index);
            if (!Character.isWhitespace(character) && random.nextInt(4) == 0) {
                character = GLITCH_GLYPHS.charAt(random.nextInt(GLITCH_GLYPHS.length()));
            }
            glitched.append(GLITCH_COLORS[random.nextInt(GLITCH_COLORS.length)]);
            glitched.append(character);
        }
        appendNoise(glitched, random);
        return glitched.append(TextFormatting.RESET).toString();
    }

    private static void appendNoise(StringBuilder target, Random random) {
        int length = 2 + random.nextInt(3);
        for (int index = 0; index < length; index++) {
            target.append(GLITCH_COLORS[random.nextInt(GLITCH_COLORS.length)]);
            target.append(GLITCH_GLYPHS.charAt(random.nextInt(GLITCH_GLYPHS.length())));
        }
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        super.addInformation(stack, world, tooltip, flag);
        tooltip.add(TextFormatting.LIGHT_PURPLE + I18nUtil.resolveKey("desc.backpack.reality_error.item_name"));
    }
}
