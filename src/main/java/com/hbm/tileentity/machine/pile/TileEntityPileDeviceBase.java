package com.hbm.tileentity.machine.pile;

import com.hbm.tileentity.TileEntityTickingBase;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.EnumFacing;

public abstract class TileEntityPileDeviceBase extends TileEntityTickingBase {

    public int chanNum = -1;

    public EnumFacing getOrientation() {
        return EnumFacing.byIndex(getBlockMetadata() % 4 + 2);
    }

    @Override
    public void serialize(ByteBuf buf) {
        super.serialize(buf);
        buf.writeInt(chanNum);
    }

    @Override
    public void deserialize(ByteBuf buf) {
        super.deserialize(buf);
        chanNum = buf.readInt();
    }

    @Override
    public String getInventoryName() {
        return "container.pile_device";
    }
}
