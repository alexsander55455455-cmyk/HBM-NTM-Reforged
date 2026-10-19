package com.hbm.inventory;

import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.RORFunctionException;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.factory.GunFactory;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.hud.IHUDComponent;
import com.hbm.items.weapon.sedna.impl.ItemGunStinger;
import com.hbm.tileentity.turret.TileEntityTurretArty;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import org.junit.jupiter.api.*;
import java.lang.reflect.Field;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CombatControlsParityTest {
    @BeforeAll static void bootstrap() throws Exception { DataConsumerRecipeTest.bootstrap(); GunFactory.init(); net.minecraft.tileentity.TileEntity.register("c03c_test_arty", Artillery.class); }
    private static class Artillery extends TileEntityTurretArty {
        int dirty;
        @Override public void markChanged() { dirty++; }
        @Override public Vec3d getTurretPos() { return Vec3d.ZERO; }
        int queued() { return targetQueue.size(); }
        Vec3d firstTarget() { return targetQueue.get(0); }
    }
    private static void radio(Artillery tile, String command) {
        tile.runRORFunction(IRORInteractive.resolveFunctionName(tile, command), IRORInteractive.getParams(command));
    }
    @Test void radioAdvertisesNewModeFunction() { assertTrue(Arrays.asList(new Artillery().getFunctionInfo()).contains("FUN:setmode!mode")); }
    @Test void radioRetainsAllInheritedFunctions() {
        List<String> functions=Arrays.asList(new Artillery().getFunctionInfo());
        for(String name:new String[]{"setactive", "targetplayers", "targetanimals", "targetmobs", "targetmachines", "addwhitelist", "removewhitelist", "enqueue"})
            assertEquals(1,functions.stream().filter(f->f.startsWith("FUN:"+name+"!")).count(),name);
        assertEquals(9,functions.size());
    }
    @Test void radioCanSelectAllThreeModesAndMarksChunkChanged() {
        Artillery tile=new Artillery();
        for(int mode:new int[]{2,1,0}) { radio(tile,"setmode!"+mode); assertEquals(mode,tile.mode); }
        assertEquals(3,tile.dirty);
    }
    @Test void mixedCaseRadioCommandResolvesDeclaredFunction() { Artillery tile=new Artillery(); radio(tile,"SeTmOdE!2"); assertEquals(2,tile.mode); }
    @Test void invalidRadioModeCannotMutateModeOrDirtyChunk() {
        Artillery tile=new Artillery();tile.mode=1;
        for(String value:new String[]{"-1","3","abc","2147483647"}) {
            assertThrows(RORFunctionException.class,()->radio(tile,"setmode!"+value));assertEquals(1,tile.mode);assertEquals(0,tile.dirty);
        }
    }
    @Test void modeCommandRequiresExactlyOneParameter() {
        Artillery tile=new Artillery();tile.mode=1;radio(tile,"setmode");radio(tile,"setmode!2:0");assertEquals(1,tile.mode);assertEquals(0,tile.dirty);
    }
    @Test void unknownCommandDoesNotChangeMode() { Artillery tile=new Artillery();tile.mode=1;radio(tile,"unknown!2");assertEquals(1,tile.mode);assertEquals(0,tile.dirty); }
    @Test void inheritedActiveAndTargetingCommandsStillWork() {
        Artillery tile=new Artillery();radio(tile,"setactive!1");assertTrue(tile.isOn());radio(tile,"targetplayers!1");assertTrue(tile.targetPlayers);radio(tile,"setactive!0");assertFalse(tile.isOn());assertEquals(3,tile.dirty);
    }
    @Test void manualModeKeepsInheritedCoordinateQueue() {
        Artillery tile=new Artillery();radio(tile,"setmode!2");radio(tile,"enqueue!100:64:200");assertEquals(1,tile.queued());assertEquals(new Vec3d(100.5,64,200.5),tile.firstTarget());assertEquals(2,tile.mode);
    }
    @Test void radioSelectionSurvivesExistingNbtFormat() {
        Artillery tile=new Artillery();radio(tile,"setmode!2");NBTTagCompound tag=tile.writeToNBT(new NBTTagCompound());assertEquals(2,tag.getShort("mode"));Artillery loaded=new Artillery();loaded.readFromNBT(tag);assertEquals(2,loaded.mode);
    }
    @Test void radioSelectionUsesExistingClientSync() {
        Artillery tile=new Artillery();radio(tile,"setmode!1");ByteBuf buf=Unpooled.buffer();try {tile.serialize(buf);Artillery client=new Artillery();client.deserialize(buf);assertEquals(1,client.mode);assertFalse(buf.isReadable());}finally{buf.release();}
    }
    private static IHUDComponent record(int height,List<Integer> offsets,List<Integer> indices) {
        return new IHUDComponent() {
            public int getComponentHeight(EntityPlayer player,ItemStack stack){return height;}
            public void renderHUDComponent(RenderGameOverlayEvent.Pre event,RenderGameOverlayEvent.ElementType type,EntityPlayer player,ItemStack stack,int offset,int index){offsets.add(offset);indices.add(index);}
        };
    }
    private static void render(GunConfig... configs) throws Exception {
        ItemGunStinger gun=(ItemGunStinger)ModItems.gun_stinger;Field f=ItemGunBaseNT.class.getDeclaredField("configs_DNA");f.setAccessible(true);Object before=f.get(gun);
        try {f.set(gun,configs);gun.renderHUD(null,RenderGameOverlayEvent.ElementType.TEXT,null,new ItemStack(gun),EnumHand.MAIN_HAND);}finally{f.set(gun,before);}
    }
    @Test void hudStacksThreeComponentsByTheirActualHeights() throws Exception {
        List<Integer> offsets=new ArrayList<>(),indices=new ArrayList<>();render(new GunConfig().hud(record(18,offsets,indices),record(7,offsets,indices),record(12,offsets,indices)));assertEquals(Arrays.asList(0,18,25),offsets);assertEquals(Arrays.asList(0,0,0),indices);
    }
    @Test void hudStartsEachGunConfigurationAtZero() throws Exception {
        List<Integer> offsets=new ArrayList<>(),indices=new ArrayList<>();render(new GunConfig().hud(record(18,offsets,indices),record(7,offsets,indices)),new GunConfig().hud(record(12,offsets,indices),record(5,offsets,indices)));assertEquals(Arrays.asList(0,18,0,12),offsets);assertEquals(Arrays.asList(0,0,1,1),indices);
    }
    @Test void hudAllowsConfigurationWithoutComponents() throws Exception { assertDoesNotThrow(()->render(new GunConfig())); }
    @Test void hudZeroHeightComponentDoesNotPushFollowingComponent() throws Exception {
        List<Integer> offsets=new ArrayList<>(),indices=new ArrayList<>();render(new GunConfig().hud(record(18,offsets,indices),record(0,offsets,indices),record(7,offsets,indices)));assertEquals(Arrays.asList(0,18,18),offsets);
    }
    @Test void hudKeepsRegisteredWeaponConfigurationAfterRendering() throws Exception {
        ItemGunBaseNT gun=(ItemGunBaseNT)ModItems.gun_stinger;GunConfig before=gun.getConfig(null,0);render(new GunConfig());assertSame(before,gun.getConfig(null,0));assertEquals(1,gun.getConfigCount());
    }
}
