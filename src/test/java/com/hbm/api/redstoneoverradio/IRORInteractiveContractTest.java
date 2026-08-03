package com.hbm.api.redstoneoverradio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IRORInteractiveContractTest {

    @Test
    void commandNamesAreResolvedCaseInsensitively() {
        IRORInteractive target = new IRORInteractive() {
            @Override
            public String[] getFunctionInfo() {
                return new String[] { PREFIX_FUNCTION + "setRecipe!index", PREFIX_FUNCTION + "toggle" };
            }

            @Override
            public String runRORFunction(String name, String[] params) {
                return null;
            }
        };

        assertEquals("FUN:setRecipe", IRORInteractive.resolveFunctionName(target, "SeTrEcIpE!2"));
        assertEquals("FUN:toggle", IRORInteractive.resolveFunctionName(target, "TOGGLE"));
    }

    @Test
    void numericArgumentsUseOriginalRoundingAndBounds() {
        assertEquals(3, IRORInteractive.parseInt("2.5"));
        assertEquals(-1, IRORInteractive.parseInt("-1.5"));
        assertEquals(7, IRORInteractive.parseInt("7", 0, 10));
        assertThrows(RORFunctionException.class, () -> IRORInteractive.parseInt("11", 0, 10));
        assertThrows(RORFunctionException.class, () -> IRORInteractive.parseInt("not-a-number"));
    }

    @Test
    void malformedCommandsReturnControlledErrors() {
        assertThrows(RORFunctionException.class, () -> IRORInteractive.getCommand(null));
        assertThrows(RORFunctionException.class, () -> IRORInteractive.getCommand(""));
        assertThrows(RORFunctionException.class, () -> IRORInteractive.getCommand("one!two!three"));
    }

    @Test
    void valueNamesAreResolvedCaseInsensitively() {
        IRORValueProvider target = new IRORValueProvider() {
            @Override
            public String[] getFunctionInfo() {
                return new String[] { PREFIX_VALUE + "burnRate" };
            }

            @Override
            public String provideRORValue(String name) {
                return null;
            }
        };

        assertEquals("VAL:burnRate", IRORValueProvider.resolveValueName(target, "BURNRATE"));
        assertEquals("VAL:burnRate", IRORValueProvider.resolveValueName(target, "val:burnrate"));
    }
}
