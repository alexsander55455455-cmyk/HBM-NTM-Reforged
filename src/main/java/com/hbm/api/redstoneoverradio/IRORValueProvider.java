package com.hbm.api.redstoneoverradio;

import java.util.Locale;

public interface IRORValueProvider extends IRORInfo {

    /** Grabs the specified value from this ROR component, operations should not cause any changes with the component itself */
    String provideRORValue(String name);

    /** Resolves a typed value name to the component's declared spelling, ignoring case. */
    static String resolveValueName(IRORValueProvider target, String input) {
        String requested = input == null ? "" : input;
        if (requested.regionMatches(true, 0, PREFIX_VALUE, 0, PREFIX_VALUE.length())) {
            requested = requested.substring(PREFIX_VALUE.length());
        }
        String[] values = target.getFunctionInfo();
        if (values != null) {
            for (String value : values) {
                if (value == null || !value.regionMatches(true, 0, PREFIX_VALUE, 0, PREFIX_VALUE.length())) continue;
                String declared = value.substring(PREFIX_VALUE.length());
                if (declared.equalsIgnoreCase(requested)) return PREFIX_VALUE + declared;
            }
        }
        return PREFIX_VALUE + requested.toLowerCase(Locale.US);
    }
}
