package com.hbm.items.machine;

import com.hbm.Tags;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.render.icon.RGBMutatorInterpolatedComponentRemap;
import com.hbm.render.icon.TextureAtlasSpriteMutatable;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@SideOnly(Side.CLIENT)
public final class ScrapTextureResolver {

    private static final String PREFIX = "items/scraps_";

    private static final Set<String> KNOWN_TEXTURES = new HashSet<>();

    private static final Map<String, String> ALIASES = new HashMap<>();

    static {
        String[] textures = {
                "actinium227", "advancedalloy", "aluminum", "americium241", "americium242", "americiumrg",
                "anybismoid", "arsenic", "beryllium", "bismuth", "boron", "bscco", "cadmium", "calcium",
                "carbon", "cdalloy", "cmbsteel", "cobalt", "cobalt60", "copper", "dineutronium", "durasteel",
                "ferrouranium", "flux", "ghiorsium336", "gold", "gold198", "hematite", "iron", "lead", "lead209",
                "lithium", "magnetizedtungsten", "malachite", "meteor", "mingrade", "neodymium", "neptunium237",
                "niobium", "obsidian", "osmiridium", "plutonium", "plutonium238", "plutonium239", "plutonium240",
                "plutonium241", "plutoniumrg", "polonium210", "radium226", "redstone", "saturnite", "schrabidate",
                "schrabidate_e", "schrabidium", "schrabidium_e", "schraranium", "silicon", "slag", "solinium",
                "starmetal", "steel", "stone", "strontium", "tantalum", "tcalloy", "technetium99", "thorium232",
                "titanium", "tungcar", "tungsten", "uranium", "uranium233", "uranium235", "uranium238", "watzmud",
                "workersalloy", "zirconium"
        };
        for (String tex : textures) {
            KNOWN_TEXTURES.add(tex);
        }

        ALIASES.put("pigiron", "iron");
        ALIASES.put("wroughtiron", "iron");
        ALIASES.put("meteoriciron", "meteor");
        ALIASES.put("bismuthbronze", "anybismoid");
        ALIASES.put("arsenicbronze", "anybismoid");
        ALIASES.put("desh", "workersalloy");
        ALIASES.put("workersalloy", "workersalloy");
        ALIASES.put("watzmud", "watzmud");
        ALIASES.put("mud", "watzmud");
        ALIASES.put("saturnite", "saturnite");
        ALIASES.put("bigmt", "saturnite");
        ALIASES.put("advancedalloy", "advancedalloy");
        ALIASES.put("durasteel", "durasteel");
        ALIASES.put("tcalloy", "tcalloy");
        ALIASES.put("cmbsteel", "cmbsteel");
        ALIASES.put("plutoniumrg", "plutoniumrg");
        ALIASES.put("americiumrg", "americiumrg");
        ALIASES.put("magnetizedtungsten", "magnetizedtungsten");
        ALIASES.put("ghiorsium336", "ghiorsium336");
        ALIASES.put("gh336", "ghiorsium336");
        ALIASES.put("pu238", "plutonium238");
        ALIASES.put("pu239", "plutonium239");
        ALIASES.put("pu240", "plutonium240");
        ALIASES.put("pu241", "plutonium241");
        ALIASES.put("u233", "uranium233");
        ALIASES.put("u235", "uranium235");
        ALIASES.put("u238", "uranium238");
        ALIASES.put("th232", "thorium232");
        ALIASES.put("thorium", "thorium232");
        ALIASES.put("thorium232", "thorium232");
        ALIASES.put("np237", "neptunium237");
        ALIASES.put("po210", "polonium210");
        ALIASES.put("tc99", "technetium99");
        ALIASES.put("ra226", "radium226");
        ALIASES.put("ac227", "actinium227");
        ALIASES.put("co60", "cobalt60");
        ALIASES.put("au198", "gold198");
        ALIASES.put("pb209", "lead209");
        ALIASES.put("am241", "americium241");
        ALIASES.put("am242", "americium242");
        ALIASES.put("sa326", "schrabidium");
        ALIASES.put("schrabidium", "schrabidium");
        ALIASES.put("sa327", "solinium");
        ALIASES.put("sbd", "schrabidate");
        ALIASES.put("srn", "schraranium");
        ALIASES.put("ferro", "ferrouranium");
        ALIASES.put("ferrouranium", "ferrouranium");
        ALIASES.put("star", "starmetal");
        ALIASES.put("alloy", "advancedalloy");
        ALIASES.put("dura", "durasteel");
        ALIASES.put("cmb", "cmbsteel");
        ALIASES.put("dnt", "dineutronium");
        ALIASES.put("magtung", "magnetizedtungsten");
        ALIASES.put("tungcar", "tungcar");
        ALIASES.put("ti", "titanium");
        ALIASES.put("cu", "copper");
        ALIASES.put("w", "tungsten");
        ALIASES.put("al", "aluminum");
        ALIASES.put("pb", "lead");
        ALIASES.put("bi", "bismuth");
        ALIASES.put("as", "arsenic");
        ALIASES.put("ta", "tantalum");
        ALIASES.put("nd", "neodymium");
        ALIASES.put("nb", "niobium");
        ALIASES.put("be", "beryllium");
        ALIASES.put("co", "cobalt");
        ALIASES.put("b", "boron");
        ALIASES.put("zr", "zirconium");
        ALIASES.put("sr", "strontium");
        ALIASES.put("ca", "calcium");
        ALIASES.put("li", "lithium");
        ALIASES.put("cd", "cadmium");
        ALIASES.put("si", "silicon");
        ALIASES.put("u", "uranium");
        ALIASES.put("pu", "plutonium");
        ALIASES.put("purg", "plutoniumrg");
        ALIASES.put("amrg", "americiumrg");
        ALIASES.put("nickel", "iron");
        ALIASES.put("stainlesssteel", "steel");
        ALIASES.put("gallium", "aluminum");
        ALIASES.put("gaas", "anybismoid");
        ALIASES.put("galliumarsenide", "anybismoid");
        ALIASES.put("conglomerate", "slag");
        ALIASES.put("iridium", "osmiridium");
        ALIASES.put("zinc", "aluminum");
        ALIASES.put("hafnium", "tungsten");
        ALIASES.put("bromine", "arsenic");
        ALIASES.put("iodine", "niobium");
        ALIASES.put("glowstone", "redstone");
        ALIASES.put("netherquartz", "stone");
        ALIASES.put("p_white", "stone");
        ALIASES.put("australium", "gold");
        ALIASES.put("tasmanite", "gold");
        ALIASES.put("ayerite", "gold");
        ALIASES.put("richmagma", "watzmud");
        ALIASES.put("semtex", "saturnite");
        ALIASES.put("galliumarsenide", "anybismoid");
        ALIASES.put("conglomerate", "slag");
        ALIASES.put("iridium", "osmiridium");
        ALIASES.put("zinc", "aluminum");
        ALIASES.put("hafnium", "tungsten");
        ALIASES.put("bromine", "arsenic");
        ALIASES.put("iodine", "niobium");
        ALIASES.put("glowstone", "redstone");
        ALIASES.put("netherquartz", "stone");
        ALIASES.put("p_white", "stone");
        ALIASES.put("australium", "gold");
        ALIASES.put("tasmanite", "gold");
        ALIASES.put("ayerite", "gold");
        ALIASES.put("richmagma", "watzmud");
        ALIASES.put("semtex", "saturnite");
    }

    private ScrapTextureResolver() {
    }

    public static String materialKey(NTMMaterial mat) {
        return mat.names[0].toLowerCase(Locale.US);
    }

    public static String resolveSuffix(NTMMaterial mat) {
        String key = materialKey(mat);
        if (ALIASES.containsKey(key)) {
            return ALIASES.get(key);
        }
        if (KNOWN_TEXTURES.contains(key)) {
            return key;
        }
        return key;
    }

    public static String getTexturePath(NTMMaterial mat) {
        return PREFIX + materialKey(mat);
    }

    public static boolean hasDedicatedTexture(NTMMaterial mat) {
        return KNOWN_TEXTURES.contains(resolveSuffix(mat));
    }

    public static boolean usesAliasTexture(NTMMaterial mat) {
        String key = materialKey(mat);
        String resolved = resolveSuffix(mat);
        return !key.equals(resolved) && KNOWN_TEXTURES.contains(resolved);
    }

    public static ResourceLocation getSourceTexture(NTMMaterial mat) {
        return new ResourceLocation(Tags.MODID, PREFIX + resolveSuffix(mat));
    }

    public static TextureAtlasSprite registerSprite(TextureMap map, NTMMaterial mat) {
        String atlasPath = getTexturePath(mat);
        ResourceLocation atlasLoc = new ResourceLocation(Tags.MODID, atlasPath);

        if (hasDedicatedTexture(mat) && materialKey(mat).equals(resolveSuffix(mat))) {
            return map.registerSprite(atlasLoc);
        }

        if (usesAliasTexture(mat)) {
            ScrapAliasSprite sprite = new ScrapAliasSprite(atlasLoc.toString(), getSourceTexture(mat));
            map.setTextureEntry(sprite);
            return sprite;
        }

        TextureAtlasSpriteMutatable sprite = new TextureAtlasSpriteMutatable(
                atlasLoc.toString(),
                new RGBMutatorInterpolatedComponentRemap(
                        0xFFFFFF, 0x505050,
                        mat.solidColorLight, mat.solidColorDark
                )
        ).withFixedBase("scraps");
        map.setTextureEntry(sprite);
        return sprite;
    }
}