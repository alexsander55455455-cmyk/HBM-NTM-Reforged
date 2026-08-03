package com.hbm.api.redstoneoverradio;

import java.util.Locale;

public interface IRORInteractive extends IRORInfo {

    String NAME_SEPARATOR = "!";
    String PARAM_SEPARATOR = ":";

    String EX_NULL = "Exception: Null Command";
    String EX_NAME = "Exception: Multiple Name Separators";
    String EX_FORMAT = "Exception: Parameter in Invalid Format";

    /**
     * Runs a function on the ROR component, usually causing the component to change or do something. Returns are optional.
     */
    String runRORFunction(String name, String[] params);

    /**
     * Extracts the command name from a full command string
     */
    static String getCommand(String input) {
        if (input == null || input.isEmpty()) throw new RORFunctionException(EX_NULL);
        String[] parts = input.split(NAME_SEPARATOR);
        if (parts.length <= 0 || parts.length > 2) throw new RORFunctionException(EX_NAME);
        if (parts[0].isEmpty()) throw new RORFunctionException(EX_NULL);
        return parts[0].toLowerCase(Locale.US);
    }

    /**
     * Extracts the param list from a full command string
     */
    static String[] getParams(String input) {
        if (input == null || input.isEmpty()) throw new RORFunctionException(EX_NULL);
        String[] parts = input.split(NAME_SEPARATOR);
        if (parts.length <= 0 || parts.length > 2) throw new RORFunctionException(EX_NAME);
        if (parts.length == 1) return new String[0];
        String paramList = parts[1];
        return paramList.split(PARAM_SEPARATOR);
    }

    /** Resolves a typed command to the component's declared spelling, ignoring case. */
    static String resolveFunctionName(IRORInteractive target, String input) {
        String requested = getCommand(input);
        String[] functions = target.getFunctionInfo();
        if (functions != null) {
            for (String function : functions) {
                if (function == null || !function.regionMatches(true, 0, PREFIX_FUNCTION, 0, PREFIX_FUNCTION.length())) continue;
                int separator = function.indexOf(NAME_SEPARATOR);
                String declared = function.substring(PREFIX_FUNCTION.length(), separator >= 0 ? separator : function.length());
                if (declared.equalsIgnoreCase(requested)) return PREFIX_FUNCTION + declared;
            }
        }
        return PREFIX_FUNCTION + requested;
    }

    static int parseInt(String val) {
        return parseInt(val, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    static int parseInt(String val, int min, int max) {
        int result;
        try {
            result = Integer.parseInt(val);
        } catch (Exception x) {
            try {
                result = (int) Math.round(Double.parseDouble(val));
            } catch (Exception y) {
                throw new RORFunctionException(EX_FORMAT);
            }
        }
        if (result < min || result > max) throw new RORFunctionException(EX_FORMAT);
        return result;
    }
}
