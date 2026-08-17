package com.hbm.compat;

import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;

public final class CursedAddonItemVisibility {

    private CursedAddonItemVisibility() {
    }

    public static boolean shouldHide(Item item) {
        if (item == null) {
            return true;
        }
        ResourceLocation name = item.getRegistryName();
        if (name == null || !"leafia".equals(name.getNamespace())) {
            return false;
        }
        if (item.getCreativeTab() == null) {
            return true;
        }

        String path = name.getPath();
        if (path.startsWith("test_")) {
            return true;
        }
        switch (path) {
            case "light_lit":
            case "light_emitter":
            case "pribris_smoke":
            case "amsp_analyzer":
            case "amsp_receiver":
            case "poster_slize":
                return true;
            default:
                return false;
        }
    }
}
