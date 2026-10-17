package com.hbm.inventory;

import com.hbm.blocks.ModBlocks;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemDrive.EnumDriveType;
import com.hbm.saveddata.satellites.SatelliteMapper;
import com.hbm.tileentity.machine.TileEntityMachineTapeDrive;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TapeDriveParityTest {
    @BeforeAll static void bootstrap() throws Exception {
        AutoCrystallizerOreDictionaryTest.bootstrap();
        com.hbm.main.MainRegistry.proxy = new com.hbm.main.ServerProxy();
        net.minecraftforge.fml.common.discovery.ASMDataTable capabilities = new net.minecraftforge.fml.common.discovery.ASMDataTable();
        capabilities.addASMData(null, net.minecraftforge.common.capabilities.CapabilityInject.class.getName(), CapabilityItemHandler.class.getName(), "ITEM_HANDLER_CAPABILITY",
                java.util.Collections.<String, Object>singletonMap("value", org.objectweb.asm.Type.getType(IItemHandler.class)));
        net.minecraftforge.common.capabilities.CapabilityManager.INSTANCE.injectCapabilities(capabilities);
        CapabilityItemHandler.register();
        assertNotNull(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY);
        GameRegistry.registerTileEntity(Drive.class, new ResourceLocation("hbm", "test_tape_drive"));
    }
    static ItemStack stack(EnumDriveType type, int count) { return new ItemStack(ModItems.drive, count, type.ordinal()); }
    @Test void oldSatelliteNbtRetainsTargetsWithoutInventingData() {
        SatelliteMapper satellite = new SatelliteMapper();
        NBTTagCompound old = new NBTTagCompound(); old.setInteger("targetX", 123); old.setInteger("targetZ", -45); old.setString("tx", "old payload");
        satellite.produceData(EnumDriveType.DISK_EMPTY, EnumDriveType.DISK_FLIGHTDATA);
        satellite.readFromNBT(old);
        assertEquals(123, satellite.getTargetX()); assertEquals(-45, satellite.getTargetZ()); assertEquals("old payload", satellite.getTransmission());
        assertFalse(satellite.hasData(null)); assertNull(satellite.driveInput); assertNull(satellite.driveOutput);
    }
    @Test void satelliteRecordRequiresBothSidesAndTheExactInput() {
        SatelliteMapper satellite = new SatelliteMapper();
        satellite.produceData(EnumDriveType.DISK_EMPTY, EnumDriveType.DISK_FLIGHTDATA);
        assertTrue(satellite.hasData(null)); assertEquals(EnumDriveType.DISK_FLIGHTDATA, satellite.getOutputData(EnumDriveType.DISK_EMPTY));
        assertNull(satellite.getOutputData(EnumDriveType.FLASH_EMPTY));
        satellite.driveOutput = null; assertFalse(satellite.hasData(null));
    }
    @Test void satelliteDataRoundtripAndConsumptionPreserveOriginalOrdinals() {
        SatelliteMapper source = new SatelliteMapper(); source.produceData(EnumDriveType.DISK_EMPTY, EnumDriveType.DISK_ORBITDATA);
        NBTTagCompound tag = new NBTTagCompound(); source.writeToNBT(tag); assertEquals(1, tag.getInteger("driveInput")); assertEquals(8, tag.getInteger("driveOutput"));
        SatelliteMapper restored = new SatelliteMapper(); restored.readFromNBT(tag);
        assertEquals(EnumDriveType.DISK_ORBITDATA, restored.getOutputData(EnumDriveType.DISK_EMPTY)); restored.consumeData();
        assertFalse(restored.hasData(null)); assertNull(restored.getOutputData(EnumDriveType.DISK_EMPTY));
        NBTTagCompound empty = new NBTTagCompound(); restored.writeToNBT(empty); assertFalse(empty.hasKey("driveInput")); assertFalse(empty.hasKey("driveOutput"));
    }
    @Test void allTwelveCheckedSlotsInsertExactlyOneDrive() {
        Drive drive = new Drive(); assertEquals(12, drive.inventory.getSlots());
        for(int i = 0; i < 12; i++) {
            assertEquals(1, drive.getCheckedInventory().getSlotLimit(i));
            assertEquals(63, drive.getCheckedInventory().insertItem(i, stack(EnumDriveType.DISK_EMPTY, 64), false).getCount());
            assertEquals(1, drive.inventory.getStackInSlot(i).getCount());
            assertEquals(1, drive.getCheckedInventory().insertItem(i, stack(EnumDriveType.DISK_EMPTY, 1), false).getCount());
        }
    }
    @Test void checkedInventoryRejectsForeignItemsButPlayerCanRetrieveBlankDrives() {
        Drive drive = new Drive(); assertEquals(1, drive.getCheckedInventory().insertItem(0, new ItemStack(Items.DIAMOND), false).getCount());
        drive.inventory.setStackInSlot(0, stack(EnumDriveType.DISK_EMPTY, 1));
        assertEquals(1, drive.getCheckedInventory().extractItem(0, 1, false).getCount());
    }
    @Test void everySideAndUnspecifiedCapabilityProtectsBlankDrives() {
        Drive drive = new Drive();
        for(EnumFacing side : new EnumFacing[]{null, EnumFacing.UP, EnumFacing.DOWN, EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.EAST, EnumFacing.WEST}) {
            IItemHandler handler = drive.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side);
            assertNotNull(handler); assertEquals(12, handler.getSlots());
            for(EnumDriveType type : new EnumDriveType[]{EnumDriveType.DISK_EMPTY, EnumDriveType.FLASH_EMPTY}) {
                drive.inventory.setStackInSlot(0, stack(type, 1)); assertTrue(handler.extractItem(0, 1, false).isEmpty()); assertEquals(1, drive.inventory.getStackInSlot(0).getCount());
            }
            assertEquals(1, handler.insertItem(1, new ItemStack(Items.DIAMOND), false).getCount());
        }
    }
    @Test void filledAndBrokenDrivesCanBeExtractedWithSimulation() {
        Drive drive = new Drive(); IItemHandler handler = drive.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, EnumFacing.UP);
        for(EnumDriveType type : EnumDriveType.values()) {
            if(type == EnumDriveType.DISK_EMPTY || type == EnumDriveType.FLASH_EMPTY) continue;
            drive.inventory.setStackInSlot(0, stack(type, 1)); assertEquals(1, handler.extractItem(0, 1, true).getCount()); assertEquals(1, drive.inventory.getStackInSlot(0).getCount());
            assertEquals(type.ordinal(), handler.extractItem(0, 1, false).getItemDamage()); assertTrue(drive.inventory.getStackInSlot(0).isEmpty());
        }
    }
    @Test void capabilityInsertionSimulationDoesNotChangeInventory() {
        Drive drive = new Drive(); IItemHandler handler = drive.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
        assertEquals(63, handler.insertItem(0, stack(EnumDriveType.DISK_EMPTY, 64), true).getCount()); assertTrue(drive.inventory.getStackInSlot(0).isEmpty());
        assertEquals(63, handler.insertItem(0, stack(EnumDriveType.DISK_EMPTY, 64), false).getCount()); assertEquals(1, drive.inventory.getStackInSlot(0).getCount());
    }
    @Test void packetDistinguishesEmptyInvalidBlankAndFilledSlots() {
        Drive drive = new Drive(); drive.inventory.setStackInSlot(0, new ItemStack(Items.DIAMOND));
        for(int i = 0; i < EnumDriveType.values().length; i++) drive.inventory.setStackInSlot(i + 1, stack(EnumDriveType.values()[i], 1));
        ByteBuf buf = Unpooled.buffer();
        try {
            drive.serialize(buf); Drive restored = new Drive(); restored.deserialize(buf); assertEquals(0, buf.readableBytes()); assertEquals(1, restored.tapes[0]);
            for(EnumDriveType type : EnumDriveType.values()) {
                int expected = type == EnumDriveType.DISK_EMPTY || type == EnumDriveType.FLASH_EMPTY ? 2 : type == EnumDriveType.DISK_BROKEN || type == EnumDriveType.FLASH_BROKEN ? 1 : 3;
                assertEquals(expected, restored.tapes[type.ordinal() + 1]);
            }
        } finally { buf.release(); }
        buf = Unpooled.buffer(); try { new Drive().serialize(buf); Drive restored = new Drive(); restored.deserialize(buf); for(byte status : restored.tapes) assertEquals(0, status); } finally { buf.release(); }
    }
    @Test void inventoryNbtRestoresAllTwelveSingleItemSlots() {
        Drive source = new Drive(); for(int i = 0; i < 12; i++) source.inventory.setStackInSlot(i, stack(EnumDriveType.values()[i % 11], 1));
        Drive restored = new Drive(); restored.readFromNBT(source.writeToNBT(new NBTTagCompound()));
        for(int i = 0; i < 12; i++) { assertEquals(i % 11, restored.inventory.getStackInSlot(i).getItemDamage()); assertEquals(1, restored.inventory.getStackInSlot(i).getCount()); assertEquals(1, restored.inventory.getSlotLimit(i)); }
    }
    @Test void fourHorizontalBoundsRetainOriginalThreeQuarterDepth() {
        double[][] bounds = {{0,0,.25,1,1,1},{0,0,0,1,1,.75},{.25,0,0,1,1,1},{0,0,0,.75,1,1}};
        for(int meta = 2; meta <= 5; meta++) {
            AxisAlignedBB box = ModBlocks.machine_tape_drive.getBoundingBox(ModBlocks.machine_tape_drive.getStateFromMeta(meta), null, BlockPos.ORIGIN);
            assertArrayEquals(bounds[meta - 2], new double[]{box.minX,box.minY,box.minZ,box.maxX,box.maxY,box.maxZ});
        }
    }
    public static class Drive extends TileEntityMachineTapeDrive { @Override public void markDirty() { } }
}
