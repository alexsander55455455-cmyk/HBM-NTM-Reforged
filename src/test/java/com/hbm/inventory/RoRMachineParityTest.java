package com.hbm.inventory;

import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.api.redstoneoverradio.RORFunctionException;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemZirnoxRod;
import com.hbm.items.machine.ItemZirnoxRod.EnumZirnoxType;
import com.hbm.tileentity.machine.TileEntityPWRController;
import com.hbm.tileentity.machine.TileEntityReactorZirnox;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

class RoRMachineParityTest {
    @BeforeAll static void bootstrap() throws Exception { AutocalTest.bootstrap(); }

    static class Zirnox extends TileEntityReactorZirnox {
        @Override public void markDirty() { }
    }
    static class PWR extends TileEntityPWRController {
        @Override public void markDirty() { }
        @Override public void markChanged() { }
    }
    ItemStack rod(EnumZirnoxType type, int lifetime) {
        ItemStack rod = new ItemStack(ModItems.rod_zirnox, 1, type.ordinal());
        ItemZirnoxRod.setLifeTime(rod, lifetime);
        return rod;
    }
    void command(IRORInteractive tile, String command) {
        tile.runRORFunction(IRORInteractive.resolveFunctionName(tile, command), IRORInteractive.getParams(command));
    }
    String value(IRORValueProvider tile, String name) {
        return tile.provideRORValue(IRORValueProvider.resolveValueName(tile, name));
    }
    @Test void declaresNewZirnoxCommandsAndValues() {
        assertTrue(Arrays.asList(new Zirnox().getFunctionInfo()).containsAll(Arrays.asList(
                "VAL:slottype", "VAL:slotdep", "VAL:slotmaxdep", "FUN:setslot!id (0 to 23)")));
    }
    @Test void defaultsToFirstRod() {
        Zirnox tile = new Zirnox(); tile.inventory.setStackInSlot(0, rod(EnumZirnoxType.NATURAL_URANIUM_FUEL, 1234));
        assertEquals("NATURAL_URANIUM_FUEL", value(tile, "slottype"));
        assertEquals("1234", value(tile, "slotdep")); assertEquals("250000", value(tile, "slotmaxdep"));
    }
    @Test void allTwentyFourFuelSlotsCanBeSelected() {
        Zirnox tile = new Zirnox();
        for (int i = 0; i < 24; i++) tile.inventory.setStackInSlot(i, rod(EnumZirnoxType.URANIUM_FUEL, 100 + i));
        for (int i = 0; i < 24; i++) {
            command(tile, "setslot!" + i);
            assertEquals("URANIUM_FUEL", value(tile, "slottype")); assertEquals("" + (100 + i), value(tile, "slotdep"));
        }
    }
    @Test void readsEveryRealFuelTypeAndItsOwnLifetime() {
        Zirnox tile = new Zirnox();
        for (EnumZirnoxType type : EnumZirnoxType.VALUES) {
            if (type.breeding) continue;
            tile.inventory.setStackInSlot(0, rod(type, 42));
            assertEquals(type.name(), value(tile, "slottype"));
            assertEquals("42", value(tile, "slotdep")); assertEquals("" + type.maxLife, value(tile, "slotmaxdep"));
        }
    }
    void noFuel(Zirnox tile) {
        assertEquals("", value(tile, "slottype"));
        assertNull(value(tile, "slotdep")); assertNull(value(tile, "slotmaxdep"));
    }
    @Test void emptySlotHasOriginalEmptyAndNullResults() { noFuel(new Zirnox()); }
    @Test void unrelatedItemIsNotFuel() {
        Zirnox tile = new Zirnox(); tile.inventory.setStackInSlot(0, new ItemStack(Items.STICK)); noFuel(tile);
    }
    @Test void breedingRodsAreExcludedLikeOriginal() {
        Zirnox tile = new Zirnox();
        for (EnumZirnoxType type : EnumZirnoxType.VALUES) if (type.breeding) {
            tile.inventory.setStackInSlot(0, rod(type, 100)); noFuel(tile);
        }
    }
    @Test void badSlotNumbersLeaveSelectionUnchanged() {
        Zirnox tile = new Zirnox(); tile.inventory.setStackInSlot(23, rod(EnumZirnoxType.URANIUM_FUEL, 77));
        command(tile, "setslot!23");
        for (String invalid : new String[]{"-1", "24", "100000000000", "bad", "1.5", "NaN"}) {
            command(tile, "setslot!" + invalid); assertEquals("77", value(tile, "slotdep"));
        }
        command(tile, "setslot"); assertEquals("77", value(tile, "slotdep"));
    }
    @Test void redstoneLocksSlotSelectionAndReactorState() {
        Zirnox tile = new Zirnox(); tile.inventory.setStackInSlot(0, rod(EnumZirnoxType.URANIUM_FUEL, 12));
        tile.setRedstonePowered(true); command(tile, "setslot!23"); command(tile, "setstate!1");
        assertEquals("12", value(tile, "slotdep")); assertFalse(tile.isOn);
        tile.setRedstonePowered(false); command(tile, "setstate!1"); assertTrue(tile.isOn);
    }
    @Test void slotQueriesSeeCurrentFuelWithoutChangingLifetime() {
        Zirnox tile = new Zirnox(); ItemStack fuel = rod(EnumZirnoxType.URANIUM_FUEL, 456);
        tile.inventory.setStackInSlot(0, fuel); NBTTagCompound before = fuel.writeToNBT(new NBTTagCompound());
        value(tile, "slottype"); value(tile, "slotdep"); value(tile, "slotmaxdep");
        assertEquals(before, fuel.writeToNBT(new NBTTagCompound()));
        ItemZirnoxRod.setLifeTime(fuel, 457); assertEquals("457", value(tile, "slotdep"));
        tile.inventory.setStackInSlot(0, ItemStack.EMPTY); noFuel(tile);
    }
    @Test void commandAndValueResolutionPreserveMixedCaseSupport() {
        Zirnox tile = new Zirnox(); tile.inventory.setStackInSlot(23, rod(EnumZirnoxType.URANIUM_FUEL, 19));
        command(tile, "SeTsLoT!23"); assertEquals("19", value(tile, "SlOtDeP"));
    }
    @Test void existingZirnoxTelemetryAndVentingStillWork() {
        Zirnox tile = new Zirnox(); tile.heat = 100000; tile.pressure = 100000;
        tile.water.setFill(123); tile.steam.setFill(456); tile.carbonDioxide.setFill(1300);
        assertEquals("800", value(tile, "heat")); assertEquals("30", value(tile, "pressure"));
        assertEquals("123", value(tile, "water")); assertEquals("456", value(tile, "steam"));
        command(tile, "ventco2"); assertEquals("300", value(tile, "co2"));
        command(tile, "ventco2"); assertEquals("0", value(tile, "co2"));
    }
    void pwrPercent(int percent) {
        PWR tile = new PWR(); command(tile, "setrods!" + percent);
        assertEquals(100 - percent, tile.rodTarget);
        tile.rodLevel = tile.rodTarget; assertEquals("" + percent, value(tile, "rods"));
        tile.connections = 0; tile.connectionsControlled = 10;
        assertEquals(tile.connectinFunc(percent / 10D), tile.getTotalProcessMultiplier(), 0.0001);
    }
    @Test void pwrZeroPercentWithdrawnStopsControlledReaction() { pwrPercent(0); }
    @Test void pwrFullWithdrawalAllowsControlledReaction() { pwrPercent(100); }
    @Test void pwrHalfWithdrawalMatchesReportedValueAndPhysics() { pwrPercent(50); }
    @Test void pwrKeepsRoundedInputSupport() {
        PWR tile = new PWR(); command(tile, "SeTrOdS!25.6"); assertEquals(74, tile.rodTarget);
    }
    @Test void invalidPwrPercentageDoesNotMutateTarget() {
        PWR tile = new PWR(); tile.rodTarget = 61;
        for (String invalid : new String[]{"-1", "101", "bad"}) {
            assertThrows(RORFunctionException.class, () -> command(tile, "setrods!" + invalid));
            assertEquals(61, tile.rodTarget);
        }
    }
    @Test void pwrGuiUsesExistingInternalInsertionPercentage() {
        PWR tile = new PWR(); NBTTagCompound control = new NBTTagCompound(); control.setInteger("control", 25);
        tile.receiveControl(control); assertEquals(25, tile.rodTarget);
    }
}
