package com.hbm.inventory;

import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.entity.projectile.EntityArtilleryRocket;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemTurretBiometry;
import com.hbm.tileentity.turret.TileEntityTurretArty;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CombatGuidanceParityTest {
    @BeforeAll static void bootstrap() throws Exception { DataConsumerRecipeTest.bootstrap(); net.minecraft.tileentity.TileEntity.register("c03d_test_arty", Artillery.class); }
    private static void terminal(EntityArtilleryRocket rocket,Vec3d delta,double momentum) throws Exception {
        EntityArtilleryRocket.class.getMethod("disableSteering",Vec3d.class,double.class).invoke(rocket,delta,momentum);
    }
    private static EntityArtilleryRocket rocket() { return new EntityArtilleryRocket(null); }
    private static Vec3d motion(EntityArtilleryRocket rocket) { return new Vec3d(rocket.motionX,rocket.motionY,rocket.motionZ); }
    @Test void newlyConstructedRocketStartsWithGuidance() { EntityArtilleryRocket r=rocket();assertNotNull(r.targeting);assertNotNull(r.steering);assertEquals(0F,r.getGravityVelocity()); }
    @Test void terminalApproachClearsBothGuidanceStrategies() throws Exception { EntityArtilleryRocket r=rocket();terminal(r,new Vec3d(3,4,0),10);assertNull(r.targeting);assertNull(r.steering); }
    @Test void terminalApproachKeepsSpeedAndPointsTowardTarget() throws Exception { EntityArtilleryRocket r=rocket();terminal(r,new Vec3d(3,4,0),10);assertEquals(new Vec3d(6,8,0),motion(r));assertEquals(10,motion(r).length(),0.00001); }
    @Test void repeatedTerminalApproachCannotTurnRocketAround() throws Exception { EntityArtilleryRocket r=rocket();terminal(r,new Vec3d(3,4,0),10);terminal(r,new Vec3d(-3,-4,0),100);assertEquals(new Vec3d(6,8,0),motion(r)); }
    @Test void eitherRemainingGuidanceStrategyIsDisabled() throws Exception {
        EntityArtilleryRocket a=rocket(),b=rocket();a.steering=null;b.targeting=null;
        for(EntityArtilleryRocket r:new EntityArtilleryRocket[]{a,b}) { terminal(r,new Vec3d(1,0,0),5);assertNull(r.targeting);assertNull(r.steering);assertEquals(new Vec3d(5,0,0),motion(r)); }
    }
    @Test void alreadyUnguidedRocketKeepsItsExistingVelocity() throws Exception { EntityArtilleryRocket r=rocket();r.steering=null;r.targeting=null;r.motionX=2;r.motionY=-3;r.motionZ=4;terminal(r,new Vec3d(-1,0,0),20);assertEquals(new Vec3d(2,-3,4),motion(r)); }
    @Test void ExactTargetPositionDoesNotProduceNanVelocity() throws Exception { EntityArtilleryRocket r=rocket();terminal(r,Vec3d.ZERO,25);assertEquals(Vec3d.ZERO,motion(r)); }
    @Test void terminalApproachRestoresExistingBallisticGravity() throws Exception { EntityArtilleryRocket r=rocket();terminal(r,new Vec3d(1,0,0),25);assertEquals(0.01F,r.getGravityVelocity()); }
    @Test void terminalVelocityRespectsExistingMotionMultiplier() throws Exception {
        EntityArtilleryRocket r=new EntityArtilleryRocket(null){@Override protected double motionMult(){return 2;}};terminal(r,new Vec3d(3,4,0),20);assertEquals(new Vec3d(6,8,0),motion(r));
    }
    private static class Artillery extends TileEntityTurretArty {
        int dirty;
        @Override public void markChanged() { dirty++; }
    }
    private static ItemStack chip(String... names) { ItemStack stack=new ItemStack(ModItems.turret_chip);for(String name:names)ItemTurretBiometry.addName(stack,name);return stack; }
    private static Artillery artillery(ItemStack stack) { Artillery a=new Artillery();a.inventory.setStackInSlot(0,stack);return a; }
    private static void radio(Artillery a,String command) { a.runRORFunction(IRORInteractive.resolveFunctionName(a,command),IRORInteractive.getParams(command)); }
    @Test void absentChipStillReturnsNullWhitelist() { assertNull(artillery(ItemStack.EMPTY).getWhitelist()); }
    @Test void installedEmptyChipReturnsMutableEmptyList() {
        Artillery a=artillery(chip());List<String> names=a.getWhitelist();assertNotNull(names);assertTrue(names.isEmpty());assertSame(names,a.getWhitelist());assertDoesNotThrow(()->{names.add("probe");names.remove("probe");});
    }
    @Test void radioAddWithoutChipIsHarmless() { Artillery a=artillery(ItemStack.EMPTY);assertDoesNotThrow(()->radio(a,"addwhitelist!probe"));assertNull(a.getWhitelist());assertEquals(0,a.dirty); }
    @Test void radioRemoveWithoutChipIsHarmless() { Artillery a=artillery(ItemStack.EMPTY);assertDoesNotThrow(()->radio(a,"removewhitelist!probe"));assertNull(a.getWhitelist());assertEquals(0,a.dirty); }
    @Test void wrongAiSlotItemCannotBeModifiedByWhitelistCommands() {
        ItemStack stack=new ItemStack(ModItems.ingot_steel);Artillery a=artillery(stack);assertDoesNotThrow(()->radio(a,"addwhitelist!probe"));assertDoesNotThrow(()->radio(a,"removewhitelist!probe"));assertSame(stack,a.inventory.getStackInSlot(0));assertFalse(stack.hasTagCompound());
    }
    @Test void radioCanAddFirstNameToBlankChip() { Artillery a=artillery(chip());radio(a,"AdDwHiTeLiSt!Alpha");assertEquals(Collections.singletonList("Alpha"),a.getWhitelist());assertArrayEquals(new String[]{"Alpha"},ItemTurretBiometry.getNames(a.inventory.getStackInSlot(0))); }
    @Test void radioDoesNotDuplicateNames() { Artillery a=artillery(chip("Alpha"));radio(a,"addwhitelist!Alpha");assertEquals(Collections.singletonList("Alpha"),a.getWhitelist());assertArrayEquals(new String[]{"Alpha"},ItemTurretBiometry.getNames(a.inventory.getStackInSlot(0))); }
    @Test void radioRemovalUpdatesChipAndCachedNames() { Artillery a=artillery(chip("Alpha","Beta"));List<String> cached=a.getWhitelist();radio(a,"removewhitelist!Beta");assertSame(cached,a.getWhitelist());assertEquals(Collections.singletonList("Alpha"),cached);assertArrayEquals(new String[]{"Alpha"},ItemTurretBiometry.getNames(a.inventory.getStackInSlot(0))); }
    @Test void removingUnknownNameKeepsExistingEntries() { Artillery a=artillery(chip("Alpha"));radio(a,"removewhitelist!missing");assertEquals(Collections.singletonList("Alpha"),a.getWhitelist()); }
    @Test void ownMutableCacheTracksSubsequentAddAndRemove() { Artillery a=artillery(chip("Alpha"));List<String> cached=a.getWhitelist();radio(a,"addwhitelist!Beta");assertEquals(Arrays.asList("Alpha","Beta"),cached);radio(a,"removewhitelist!Alpha");assertEquals(Collections.singletonList("Beta"),cached);assertSame(cached,a.getWhitelist()); }
    @Test void replacingChipInvalidatesOwnCachedNames() { Artillery a=artillery(chip("Alpha"));a.getWhitelist();a.inventory.setStackInSlot(0,chip());assertNotNull(a.getWhitelist());assertTrue(a.getWhitelist().isEmpty()); }
    @Test void existingWhitelistNbtFormatSurvivesRoundTrip() {
        Artillery a=artillery(chip("Alpha","Beta"));NBTTagCompound saved=a.writeToNBT(new NBTTagCompound());Artillery loaded=new Artillery();loaded.readFromNBT(saved);assertEquals(Arrays.asList("Alpha","Beta"),loaded.getWhitelist());assertArrayEquals(new String[]{"Alpha","Beta"},ItemTurretBiometry.getNames(loaded.inventory.getStackInSlot(0)));
    }
    @Test void inheritedArtilleryRadioModeStillWorks() { Artillery a=artillery(ItemStack.EMPTY);radio(a,"setmode!2");assertEquals(2,a.mode);assertEquals(1,a.dirty); }
    @Test void missingWhitelistParameterDoesNothing() { Artillery a=artillery(ItemStack.EMPTY);assertDoesNotThrow(()->radio(a,"addwhitelist"));assertDoesNotThrow(()->radio(a,"removewhitelist"));assertEquals(0,a.dirty); }
}
