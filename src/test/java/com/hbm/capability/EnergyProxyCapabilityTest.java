package com.hbm.capability;

import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.config.GeneralConfig;
import com.hbm.lib.CapabilityContextProvider;
import com.hbm.testutil.ForgeTestBootstrap;
import com.hbm.tileentity.TileEntityProxyEnergy;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;
import java.lang.reflect.Field;
import static org.junit.jupiter.api.Assertions.*;

class EnergyProxyCapabilityTest {
    private static Unsafe unsafe;
    private static Field energy;
    private static Object oldEnergy;
    private static double oldRate;

    @BeforeAll
    static void bootstrap() throws Exception {
        ForgeTestBootstrap.ensureServerSide();
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        unsafe = (Unsafe) field.get(null);
        energy = CapabilityEnergy.class.getDeclaredField("ENERGY");
        energy.setAccessible(true);
        oldEnergy = energy.get(null);
        unsafe.putObject(unsafe.staticFieldBase(energy), unsafe.staticFieldOffset(energy), unsafe.allocateInstance(Capability.class));
        oldRate = GeneralConfig.conversionRateHeToRF;
        GeneralConfig.conversionRateHeToRF = 2;
    }

    @AfterAll
    static void restore() {
        unsafe.putObject(unsafe.staticFieldBase(energy), unsafe.staticFieldOffset(energy), oldEnergy);
        GeneralConfig.conversionRateHeToRF = oldRate;
    }

    @Test
    void proxyExtractsFromProviderCoreAndPreservesAccessor() {
        TestCore core = new TestCore();
        TestProxy proxy = new TestProxy(core);
        proxy.setPos(new BlockPos(10, 20, 30));
        IEnergyStorage cap = proxy.getCapability(CapabilityEnergy.ENERGY, EnumFacing.NORTH);
        assertNotNull(cap);
        assertTrue(cap.canExtract());
        assertEquals(20, cap.extractEnergy(100, true));
        assertEquals(100, core.power);
        assertEquals(20, cap.extractEnergy(100, false));
        assertEquals(90, core.power);
        assertEquals(proxy.getPos(), core.accessor);
        assertEquals(BlockPos.ORIGIN, CapabilityContextProvider.getAccessor(BlockPos.ORIGIN));
    }

    @Test
    void proxyRetainsChargingAndHonorsDisabledOutput() {
        TestCore core = new TestCore();
        TestProxy proxy = new TestProxy(core);
        IEnergyStorage cap = proxy.getCapability(CapabilityEnergy.ENERGY, EnumFacing.UP);
        assertTrue(cap.canReceive());
        assertEquals(100, cap.receiveEnergy(100, false));
        assertEquals(150, core.power);
        core.output = false;
        assertEquals(0, cap.extractEnergy(100, false));
        assertEquals(150, core.power);
    }

    @Test
    void unresolvedCoreDoesNotExposeAnEnergyCapability() {
        TestProxy proxy = new TestProxy(null);
        assertFalse(proxy.hasCapability(CapabilityEnergy.ENERGY, EnumFacing.UP));
        assertNull(proxy.getCapability(CapabilityEnergy.ENERGY, EnumFacing.UP));
    }

    private static class TestProxy extends TileEntityProxyEnergy {
        final TileEntity core;
        TestProxy(TileEntity core) { this.core = core; }
        @Override public TileEntity getTE() { return core; }
    }

    private static class TestCore extends TileEntity implements IEnergyProviderMK2, IEnergyReceiverMK2 {
        long power = 100;
        boolean output = true;
        BlockPos accessor;
        @Override public boolean isLoaded() { return true; }
        @Override public long getPower() { accessor = CapabilityContextProvider.getAccessor(getPos()); return power; }
        @Override public void setPower(long value) { power = value; }
        @Override public long getMaxPower() { return 200; }
        @Override public long getProviderSpeed() { return output ? 10 : 0; }
        @Override public long getReceiverSpeed() { return 5; }
    }
}
