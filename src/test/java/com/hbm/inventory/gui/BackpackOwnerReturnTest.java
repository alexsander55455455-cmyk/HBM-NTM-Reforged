package com.hbm.inventory.gui;

import net.minecraft.client.gui.GuiButton;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class BackpackOwnerReturnTest {
    @Test
    void closingOwnerSettingsReturnsToStorageAndCanBeRepeated() throws Exception {
        TestGui gui = (TestGui) unsafe().allocateInstance(TestGui.class);
        Field settings = GUIBackpack.class.getDeclaredField("ownerSettingsPage");
        settings.setAccessible(true);
        Field backId = GUIBackpack.class.getDeclaredField("OWNER_BACK_BUTTON");
        backId.setAccessible(true);
        Field resetId = GUIBackpack.class.getDeclaredField("OWNER_RESET_BUTTON");
        resetId.setAccessible(true);
        assertNotEquals(resetId.getInt(null), backId.getInt(null));
        for (int attempt = 0; attempt < 3; attempt++) {
            settings.setBoolean(gui, true);
            // Sending RESET_OWNER would hit the absent network and fail this test.
            gui.actionPerformed(new GuiButton(backId.getInt(null), 0, 0, "x"));
            assertFalse(settings.getBoolean(gui));
            assertEquals(attempt + 1, gui.returnCount);
        }
    }

    private static Unsafe unsafe() throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return (Unsafe) field.get(null);
    }

    private static class TestGui extends GUIBackpack {
        int returnCount;
        TestGui() { super(null); }
        @Override public void initGui() { returnCount++; }
    }
}
