package com.hbmspace.lib;

import zone.rong.mixinbooter.ILateMixinLoader;

import java.util.Arrays;
import java.util.List;

@SuppressWarnings("unused")
public class ModMixinLoader implements ILateMixinLoader {
    @Override
    public List<String> getMixinConfigs() {
        return Arrays.asList("hbmspace.mod.mixin.json", "hbmspace.jei.late.mixin.json");
    }
}
