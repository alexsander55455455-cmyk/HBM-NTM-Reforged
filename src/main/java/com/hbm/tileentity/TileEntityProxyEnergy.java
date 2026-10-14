package com.hbm.tileentity;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.capability.NTMEnergyCapabilityWrapper;
import com.hbm.interfaces.AutoRegister;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import org.jetbrains.annotations.NotNull;

//can be used as a soruce too since the core TE handles that anyway
@AutoRegister
public class TileEntityProxyEnergy extends TileEntityProxyBase implements IEnergyReceiverMK2 {

	@Override
	public void setPower(long i) {

		TileEntity te = getTE();

		if(te instanceof IEnergyReceiverMK2) {
			((IEnergyReceiverMK2) te).setPower(i);
		}
	}

	@Override
	public long getPower() {

		TileEntity te = getTE();

		if(te instanceof IEnergyReceiverMK2) {
			return ((IEnergyReceiverMK2) te).getPower();
		}

		return 0;
	}

	@Override
	public long getMaxPower() {

		TileEntity te = getTE();

		if(te instanceof IEnergyReceiverMK2) {
			return ((IEnergyReceiverMK2) te).getMaxPower();
		}

		return 0;
	}

	@Override
	public boolean hasCapability(@NotNull Capability<?> capability, EnumFacing facing) {
		if (capability == CapabilityEnergy.ENERGY) {
			return getTE() instanceof IEnergyHandlerMK2;
		}
		return super.hasCapability(capability, facing);
	}

	@Override
	public <T> T getCapability(@NotNull Capability<T> capability, EnumFacing facing) {
		if (capability == CapabilityEnergy.ENERGY) {
			TileEntity core = getTE();
			return core instanceof IEnergyHandlerMK2
					? CapabilityEnergy.ENERGY.cast(new NTMEnergyCapabilityWrapper(core, getPos()))
					: null;
		}
		return super.getCapability(capability, facing);
	}

}
