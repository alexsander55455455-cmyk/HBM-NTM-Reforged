package com.hbm.blocks.machine.pile;

import com.hbm.tileentity.machine.pile.TileEntityPileCore;
import com.hbm.tileentity.machine.pile.TileEntityPileBaseMK2;
import com.hbm.items.machine.ItemPileRodMK2;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.init.Bootstrap;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class ChicagoPileContractTest {

    static {
        Bootstrap.register();
        TileEntity.register("hbm:chicago_pile_contract_test", TileEntityPileCore.class);
    }

    @Test
    void acceptsMaximumSpanButRejectsSixteenBlocks() {
        assertTrue(BlockPileBrick.isSpanWithinLimit(2, 2, BlockPile.MAX_V_SIZE));
        assertTrue(BlockPileBrick.isSpanWithinLimit(7, 7, BlockPile.MAX_V_SIZE));
        assertFalse(BlockPileBrick.isSpanWithinLimit(8, 7, BlockPile.MAX_V_SIZE));
        assertFalse(BlockPileBrick.isSpanWithinLimit(-1, 2, BlockPile.MAX_V_SIZE));
    }

    @Test
    void sizeAndOrientationSurviveNbtRoundTrip() {
        TileEntityPileCore original = new TileEntityPileCore().setupSize(
                7, 7, 7, 7, 15, EnumFacing.SOUTH,
                new BlockPos(-7, -7, 0), new BlockPos(7, 7, 14));
        NBTTagCompound nbt = original.writeToNBT(new NBTTagCompound());

        TileEntityPileCore restored = new TileEntityPileCore();
        restored.readFromNBT(nbt);

        assertEquals(15, restored.height);
        assertEquals(15, restored.width);
        assertEquals(15, restored.depth);
        assertEquals(EnumFacing.SOUTH, restored.assemblyFacing);
        assertEquals(TileEntityPileCore.PileOrientation.NORTH_SOUTH, restored.orientation);
        assertEquals(0, restored.fuelChannels.size());
    }

    @Test
    void channelCountsAcrossSignedByteBoundarySurviveNbtRoundTrip() {
        for(int channelCount : new int[]{0, 127, 128, TileEntityPileCore.MAX_CHANNELS}) {
            TileEntityPileCore core = new TileEntityPileCore();
            core.readFromNBT(pileNbt(channelCount));
            assertEquals(channelCount, core.fuelChannels.size());

            NBTTagCompound written = core.writeToNBT(new NBTTagCompound());
            assertEquals(channelCount, written.getTagList("fuelChannels", 10).tagCount());

            TileEntityPileCore reloaded = new TileEntityPileCore();
            reloaded.readFromNBT(written);
            assertEquals(channelCount, reloaded.fuelChannels.size());
            if(channelCount > 0) {
                TileEntityPileCore.PileChannel last = reloaded.fuelChannels.get(channelCount - 1);
                assertEquals(432.5D, last.heat);
                assertEquals(777, last.air);
                assertEquals(0.25D, last.control);
            }
        }
    }

    @Test
    void corruptedChannelCountsAndLengthsAreBounded() {
        NBTTagCompound input = pileNbt(TileEntityPileCore.MAX_CHANNELS + 10);
        input.getTagList("fuelChannels", 10).getCompoundTagAt(0).setInteger("length", Integer.MAX_VALUE);
        TileEntityPileCore core = new TileEntityPileCore();
        core.readFromNBT(input);

        assertEquals(TileEntityPileCore.MAX_CHANNELS, core.fuelChannels.size());
        assertEquals(BlockPile.MAX_H_SIZE, core.fuelChannels.get(0).rods.length);
    }

    @Test
    void legacySignedByteChannelCountsAreRecoveredAsUnsigned() {
        for(int channelCount : new int[]{127, 128, TileEntityPileCore.MAX_CHANNELS}) {
            NBTTagCompound legacy = legacyPileNbt(channelCount);
            TileEntityPileCore core = new TileEntityPileCore();
            core.readFromNBT(legacy);

            assertEquals(channelCount, core.fuelChannels.size());
            assertEquals(432.5D, core.fuelChannels.get(channelCount - 1).heat);
        }
    }

    @Test
    void verticalControlChannelAndFuelSurviveNbtReload() {
        NBTTagCompound input = pileNbt(1);
        NBTTagCompound fuel = input.getTagList("fuelChannels", 10).getCompoundTagAt(0);
        NBTTagCompound savedRod = new NBTTagCompound();
        savedRod.setInteger("slot", 0);
        new ItemStack(Items.STICK).writeToNBT(savedRod);
        NBTTagList savedRods = new NBTTagList();
        savedRods.appendTag(savedRod);
        fuel.setTag("items", savedRods);

        NBTTagCompound control = new NBTTagCompound();
        control.setInteger("x", -1);
        control.setInteger("y", 78);
        control.setInteger("z", 0);
        control.setInteger("direction", EnumFacing.DOWN.getIndex());
        control.setInteger("length", 15);
        control.setDouble("control", 0.25D);
        control.setTag("items", new NBTTagList());
        NBTTagList controls = new NBTTagList();
        controls.appendTag(control);
        input.setTag("controlChannels", controls);

        TileEntityPileCore restored = new TileEntityPileCore();
        assertDoesNotThrow(() -> restored.readFromNBT(input));
        assertEquals(1, restored.fuelChannels.size());
        assertSame(Items.STICK, restored.fuelChannels.get(0).rods[0].getItem());
        assertEquals(1, restored.controlChannels.size());
        assertEquals(EnumFacing.DOWN, restored.controlChannels.get(0).direction);
        assertTrue(Arrays.stream(restored.segments)
                .anyMatch(segment -> segment != null &&
                        segment.type == TileEntityPileCore.PileChannelType.CONTROL));
    }

    @Test
    void pileAndDeviceMetadataRangesRemainStableAndDisjoint() {
        assertEquals(0, BlockPile.META_DUMMY);
        assertEquals(1, BlockPile.META_CORE);
        assertEquals(2, BlockPile.META_CHANNEL);
        assertEquals(8, BlockPile.META_EDGE);
        assertEquals(BlockPileDevice.BLOCK_META_LOADER,
                BlockPileDevice.itemMetaToBlockMeta(BlockPileDevice.ITEM_META_LOADER));
        assertEquals(BlockPileDevice.BLOCK_META_VENT,
                BlockPileDevice.itemMetaToBlockMeta(BlockPileDevice.ITEM_META_VENT));
        assertEquals(BlockPileDevice.BLOCK_META_CONTROL,
                BlockPileDevice.itemMetaToBlockMeta(BlockPileDevice.ITEM_META_CONTROL));
        assertTrue(BlockPileDevice.BLOCK_META_LOADER + 3 < BlockPileDevice.BLOCK_META_VENT);
        assertTrue(BlockPileDevice.BLOCK_META_VENT + 3 < BlockPileDevice.BLOCK_META_CONTROL);
    }

    @Test
    void orientedPileDeviceMetadataMapsBackToCanonicalItems() {
        for(int meta = BlockPileDevice.BLOCK_META_LOADER;
            meta < BlockPileDevice.BLOCK_META_VENT; meta++) {
            assertEquals(BlockPileDevice.ITEM_META_LOADER, BlockPileDevice.blockMetaToItemMeta(meta));
        }
        for(int meta = BlockPileDevice.BLOCK_META_VENT;
            meta < BlockPileDevice.BLOCK_META_CONTROL; meta++) {
            assertEquals(BlockPileDevice.ITEM_META_VENT, BlockPileDevice.blockMetaToItemMeta(meta));
        }
        for(int meta = BlockPileDevice.BLOCK_META_CONTROL;
            meta < BlockPileDevice.BLOCK_META_CONTROL + 4; meta++) {
            assertEquals(BlockPileDevice.ITEM_META_CONTROL, BlockPileDevice.blockMetaToItemMeta(meta));
        }
    }

    @Test
    void drillingMetadataChangesKeepThePilePartLinkedToItsCore() {
        IBlockState dummy = Blocks.STONE.getStateFromMeta(0);
        IBlockState channel = Blocks.STONE.getStateFromMeta(1);
        IBlockState foreign = Blocks.DIRT.getDefaultState();
        TileEntityPileBaseMK2 part = new TileEntityPileBaseMK2();

        assertFalse(part.shouldRefresh(null, BlockPos.ORIGIN, dummy, channel),
                "Changing pile metadata must not replace the linked tile entity");
        assertTrue(part.shouldRefresh(null, BlockPos.ORIGIN, channel, foreign),
                "Changing the actual block must still remove the pile tile entity");
    }

    @Test
    void rodDepletionPersistsUntilNormalFuelConversion() {
        Item item = new Item();
        ItemStack rod = new ItemStack(item, 1, ItemPileRodMK2.EnumPileRod.NU.ordinal());
        ItemPileRodMK2.setDepletion(rod, 125D);

        ItemStack partiallyUsed = ItemPileRodMK2.react(rod, 25D);
        assertSame(rod, partiallyUsed);
        assertEquals(150D, ItemPileRodMK2.getDepletion(partiallyUsed));

        ItemStack converted = ItemPileRodMK2.react(partiallyUsed, ItemPileRodMK2.EnumPileRod.NU.life);
        assertEquals(ItemPileRodMK2.EnumPileRod.PU239.ordinal(), converted.getMetadata());
        assertEquals(0D, ItemPileRodMK2.getDepletion(converted));
    }

    private static NBTTagCompound pileNbt(int channelCount) {
        NBTTagCompound pile = new NBTTagCompound();
        pile.setInteger("pileDataVersion", 2);
        pile.setInteger("height", 15);
        pile.setInteger("width", 15);
        pile.setInteger("depth", 15);
        pile.setInteger("left", 7);
        pile.setInteger("right", 7);
        pile.setInteger("up", 7);
        pile.setInteger("orientation", TileEntityPileCore.PileOrientation.NORTH_SOUTH.ordinal());
        pile.setInteger("assemblyFacing", EnumFacing.SOUTH.getIndex());
        NBTTagList channels = new NBTTagList();
        for(int index = 0; index < channelCount; index++) {
            NBTTagCompound channel = new NBTTagCompound();
            channel.setInteger("x", index);
            channel.setInteger("y", 64);
            channel.setInteger("z", 0);
            channel.setInteger("direction", EnumFacing.SOUTH.getIndex());
            channel.setInteger("length", 15);
            channel.setDouble("heat", 432.5D);
            channel.setDouble("incomingNeutrons", 12.75D);
            channel.setInteger("air", 777);
            channel.setDouble("control", 0.25D);
            channel.setTag("items", new NBTTagList());
            channels.appendTag(channel);
        }
        pile.setTag("fuelChannels", channels);
        pile.setTag("ventilationChannels", new NBTTagList());
        pile.setTag("controlChannels", new NBTTagList());
        return pile;
    }

    private static NBTTagCompound legacyPileNbt(int channelCount) {
        NBTTagCompound pile = new NBTTagCompound();
        pile.setInteger("height", 15);
        pile.setInteger("width", 15);
        pile.setInteger("depth", 15);
        pile.setInteger("left", 7);
        pile.setInteger("right", 7);
        pile.setInteger("up", 7);
        pile.setInteger("orientation", TileEntityPileCore.PileOrientation.NORTH_SOUTH.ordinal());
        pile.setByte("fc", (byte) channelCount);
        pile.setByte("vc", (byte) 0);
        pile.setByte("cc", (byte) 0);
        for(int index = 0; index < channelCount; index++) {
            String name = "f" + index;
            pile.setInteger(name + "_x", index);
            pile.setInteger(name + "_y", 64);
            pile.setInteger(name + "_z", 0);
            pile.setByte(name + "_d", (byte) EnumFacing.SOUTH.getIndex());
            pile.setDouble(name + "heat", 432.5D);
            pile.setDouble(name + "neutrons", 12.75D);
            pile.setTag(name + "items", new NBTTagList());
        }
        return pile;
    }
}
