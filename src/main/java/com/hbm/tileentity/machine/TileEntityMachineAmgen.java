package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.blocks.ModBlocks;
import com.hbm.interfaces.AutoRegister;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.TileEntityLoadedBase;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.BlockFluidBase;
import org.jetbrains.annotations.NotNull;

@AutoRegister
public class TileEntityMachineAmgen extends TileEntityLoadedBase implements ITickable, IEnergyProviderMK2 {

    private static final long MAX_POWER = 500L;

    public long power;
    public int production = -1;
    private int counter;

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        this.power = compound.getLong("power");
    }

    @Override
    public @NotNull NBTTagCompound writeToNBT(NBTTagCompound compound) {
        compound.setLong("power", this.power);
        return super.writeToNBT(compound);
    }

    public int getHeat(World world, IBlockState state, BlockPos pos) {
        if (state == null) return 0;

        Block block = state.getBlock();
        if (block == ModBlocks.geysir_water) return 75;
        if (block == ModBlocks.geysir_chlorine) return 100;
        if (block == ModBlocks.geysir_vapor) return 50;
        if (block == ModBlocks.geysir_nether) return 500;

        int temperature = BlockFluidBase.getTemperature(world, pos);
        if (temperature == Integer.MAX_VALUE) return 0;

        temperature -= 373;
        return Math.max(temperature, 0) >> 3;
    }

    private void updateHeat() {
        int generated = 0;
        BlockPos.MutableBlockPos target = new BlockPos.MutableBlockPos();

        for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
            target.setPos(pos.getX() + direction.offsetX, pos.getY() + direction.offsetY, pos.getZ() + direction.offsetZ);
            if (!world.isBlockLoaded(target)) continue;
            generated += getHeat(world, world.getBlockState(target), target);
        }

        this.production = generated;
    }

    @Override
    public void update() {
        if (world.isRemote) return;

        long previousPower = this.power;

        if (this.production == -1 || this.counter % 80 == 0) {
            updateHeat();
        }
        this.counter++;

        this.power = Math.min(this.power + this.production, MAX_POWER);

        for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
            this.tryProvide(world, pos.getX() + direction.offsetX, pos.getY() + direction.offsetY, pos.getZ() + direction.offsetZ, direction);
        }

        if (previousPower != this.power) {
            markDirty();
        }
    }

    @Override
    public long getPower() {
        return this.power;
    }

    @Override
    public void setPower(long power) {
        this.power = power;
    }

    @Override
    public long getMaxPower() {
        return MAX_POWER;
    }
}
