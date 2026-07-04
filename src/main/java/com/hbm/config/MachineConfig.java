package com.hbm.config;

import com.hbm.interfaces.IDoor;
import com.hbm.inventory.fluid.FluidType;
import net.minecraftforge.common.config.Configuration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;

public class MachineConfig {

    protected static boolean scaleRTGPower = false;
    protected static boolean doRTGsDecay = true;
    protected static boolean disableMachines = false;
    //TODO: handle like on 1.7
    //mlbv: 1.7 hardcodes it to 6kB
    public static int crateByteSize = 8192;
    public static int rbmkJumpTemp = 1250;
    public static HashMap<String, IDoor.Mode> doorConf = new HashMap<>();

    public static int uuMixerFluidRatio = 100;
    public static boolean uuMixerFluidListIsWhitelist = false;
    public static HashSet<String> blacklistedMixerFluids = new HashSet<>();

    public static boolean isFluidAllowed(FluidType type) {
        if(type == null) return false;
        boolean isInList = blacklistedMixerFluids.contains(type.getFFName());
        if(uuMixerFluidListIsWhitelist) return isInList;
        return !isInList;
    }

    public static void loadFromConfig(Configuration config) {

        final String CATEGORY_MACHINE = CommonConfig.CATEGORY_MACHINES;

        scaleRTGPower = CommonConfig.createConfigBool(config, CATEGORY_MACHINE, "9.01_scaleRTGPower", "Should RTG/Betavoltaic fuel power scale down as it decays?", false);
        doRTGsDecay = CommonConfig.createConfigBool(config, CATEGORY_MACHINE, "9.02_doRTGsDecay", "Should RTG/Betavoltaic fuel decay at all?", true);
        disableMachines = CommonConfig.createConfigBool(config, CATEGORY_MACHINE, "9.00_disableMachines", "Prevent mod from registering any Machines? (WARNING: THIS WILL BREAK PREEXISTING WORLDS)", false);

        doorConf.clear();
        String[] doorConfStr = config.get(CATEGORY_MACHINE, "9.99_CE_02_doorConf", new String[]{},
                                             "Configuration for door modes. Format: 'modid:door_name:MODE' (e.g. 'hbm:vault_door:REDSTONE') or 'ALL:MODE'. Modes: DEFAULT, TOOLABLE, REDSTONE")
                                     .getStringList();
        for (String entry : doorConfStr) {
            if (entry == null) continue;
            String trimmed = entry.trim();
            if (trimmed.isEmpty()) continue;
            String[] split = trimmed.split(":");
            try {
                if (split.length == 2 && "ALL".equalsIgnoreCase(split[0])) {
                    IDoor.Mode mode = IDoor.Mode.valueOf(split[1].toUpperCase(Locale.ROOT));
                    doorConf.put("ALL", mode);
                } else if (split.length == 3) {
                    IDoor.Mode mode = IDoor.Mode.valueOf(split[2].toUpperCase(Locale.ROOT));
                    doorConf.put(split[0] + ":" + split[1], mode);
                }
            } catch (IllegalArgumentException e) {
                // Ignore invalid modes
            }
        }

        uuMixerFluidRatio = CommonConfig.createConfigInt(config, CATEGORY_MACHINE, "9.24_uuMixerFluidRatio", "How much mB of UU-Matter is used per mB of output fluid", 100);
        uuMixerFluidListIsWhitelist = CommonConfig.createConfigBool(config, CATEGORY_MACHINE, "9.25_uuMixerFluidListIsWhitelist", "If true then the following list of fluids is a whitelist. Otherwise it is a blacklist", false);
        blacklistedMixerFluids = CommonConfig.createConfigHashSet(config, CATEGORY_MACHINE, "9.26_blacklistedUUMixerFluids", "List of fluids that can not be made by UU Mixer. - <fluid> (String)", String.class, new String[]{"liquid_osmiridium", "experience"});
    }
}
