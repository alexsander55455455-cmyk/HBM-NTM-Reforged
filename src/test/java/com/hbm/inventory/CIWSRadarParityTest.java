package com.hbm.inventory;

import com.hbm.entity.projectile.EntityArtilleryRocket;
import com.hbm.entity.projectile.EntityArtilleryShell;
import com.hbm.inventory.recipes.AmmoPressRecipes.AmmoPressRecipe;
import com.hbm.inventory.recipes.EEAmmoPressRecipes;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.factory.XFactoryTurret;
import com.hbm.tileentity.IRadarCommandReceiver;
import com.hbm.tileentity.turret.TileEntityTurretBaseNT;
import com.hbm.tileentity.turret.TileEntityTurretHoward;
import com.hbm.util.CompatExternal;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.lang.reflect.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CIWSRadarParityTest {
    @BeforeAll static void bootstrap() throws Exception {
        DataConsumerRecipeTest.bootstrap();
        XFactoryTurret.dgk_normal=new BulletConfig().setItem(new ItemStack(ModItems.ammo_dgk));
    }
    // Replace unrelated visibility/network work; exercise the real radar command
    // state machine, range hook and existing BaseNT update path independently.
    private static class Probe extends TileEntityTurretHoward {
        boolean acceptable=true,clear=true;Vec3d turned;
        @Override public Vec3d getTurretPos(){return Vec3d.ZERO;}
        @Override public Vec3d getEntityPos(Entity e){return new Vec3d(e.posX,e.posY,e.posZ);}
        @Override public boolean entityInLOS(Entity e){return clear;}
        @Override public boolean entityAcceptableTarget(Entity e){return acceptable;}
        @Override public void turnTowards(Vec3d p){turned=p;}
        @Override protected void updateConnections(){}
        @Override public void networkPackNT(int range){}
    }
    private static World world(boolean remote)throws Exception {
        Class<?> type=Class.forName("sun.misc.Unsafe");Field f=type.getDeclaredField("theUnsafe");f.setAccessible(true);Object u=f.get(null);
        World w=(World)type.getMethod("allocateInstance",Class.class).invoke(u,WorldServer.class);Field side=World.class.getField("isRemote");side.setAccessible(true);side.setBoolean(w,remote);return w;
    }
    private static Probe turret()throws Exception{Probe p=new Probe();p.setWorld(world(false));p.isOn=true;return p;}
    private static EntityArtilleryShell shell(double x){EntityArtilleryShell e=new EntityArtilleryShell(null);e.setPosition(x,0,0);return e;}
    private static IRadarCommandReceiver radar(Probe p){assertTrue(p instanceof IRadarCommandReceiver,"Howard must accept radar linker commands");return (IRadarCommandReceiver)p;}
    private static double range(Probe p,Entity e)throws Exception{Method m=TileEntityTurretBaseNT.class.getDeclaredMethod("getTargetTrackingRange",Entity.class);m.setAccessible(true);return ((Number)m.invoke(p,e)).doubleValue();}
    private static int health(Entity e)throws Exception{return e.getClass().getField("health").getInt(e);}
    @Test void howardImplementsExistingRadarContract()throws Exception{radar(turret());}
    @Test void beltStackLimitIsFour(){assertEquals(4,new ItemStack(ModItems.ammo_dgk).getMaxStackSize());}
    @Test void ownBaseRangesRemain300And48()throws Exception{Probe p=turret();assertEquals(300,p.getDecetorRange());p.targetPlayers=true;assertEquals(48,p.getDecetorRange());}
    @Test void radarAcquiresOutsideAutonomousRange()throws Exception{Probe p=turret();Entity e=shell(900);assertTrue(radar(p).sendCommandEntity(e));assertSame(e,p.target);}
    @Test void radarAcquiresExactlyFourTimesOwnRange()throws Exception{Probe p=turret();Entity e=shell(1200);assertTrue(radar(p).sendCommandEntity(e));assertSame(e,p.target);}
    @Test void beyondRadarAcquisitionRangeOnlyTurns()throws Exception{Probe p=turret();assertTrue(radar(p).sendCommandEntity(shell(1201)));assertNull(p.target);assertNotNull(p.turned);}
    @Test void disabledTurretRejectsRadar()throws Exception{Probe p=turret();p.isOn=false;assertFalse(radar(p).sendCommandEntity(shell(30)));assertFalse(radar(p).sendCommandPosition(1,2,3));}
    @Test void radarDoesNotStealExistingTarget()throws Exception{Probe p=turret();Entity old=shell(20);p.target=old;assertFalse(radar(p).sendCommandEntity(shell(30)));assertSame(old,p.target);}
    @Test void radarRejectsDeadAndNullEntities()throws Exception{Probe p=turret();Entity e=shell(30);e.isDead=true;assertFalse(radar(p).sendCommandEntity(e));assertFalse(radar(p).sendCommandEntity(null));}
    @Test void radarRetainsExistingTargetTypeFilter()throws Exception{Probe p=turret();p.acceptable=false;assertFalse(radar(p).sendCommandEntity(shell(30)));assertNull(p.target);}
    @Test void radarRetainsLineOfSightFilter()throws Exception{Probe p=turret();p.clear=false;assertFalse(radar(p).sendCommandEntity(shell(30)));assertNull(p.target);}
    @Test void radarPositionCommandTurnsExistingAim()throws Exception{Probe p=turret();assertTrue(radar(p).sendCommandPosition(1,2,3));assertEquals(new Vec3d(1,2,3),p.turned);assertNull(p.target);}
    @Test void autonomousTrackingRangeRetainsTenPercentMargin()throws Exception{Probe p=turret();assertEquals(330,range(p,shell(30)),.000001);}
    @Test void designatedTargetRetainsOriginalRadarTrackingExemption()throws Exception{Probe p=turret();Entity e=shell(900);radar(p).sendCommandEntity(e);assertTrue(range(p,e)>1200);}
    @Test void unrelatedTargetCannotBorrowRadarExemption()throws Exception{Probe p=turret();radar(p).sendCommandEntity(shell(900));assertEquals(330,range(p,shell(30)),.000001);}
    @Test void targetLossClearsDesignationThroughExistingUpdate()throws Exception{Probe p=turret();Entity e=shell(900);radar(p).sendCommandEntity(e);p.target=null;p.isOn=false;p.update();assertEquals(330,range(p,e),.000001);}
    @Test void otherTurretsKeepExistingArtilleryFilter(){TileEntityTurretBaseNT p=new TileEntityTurretBaseNT(){@Override public String getDefaultName(){return "test";}@Override public long getMaxPower(){return 1000;}@Override public void updateFiringTick(){}@Override protected List<Integer> getAmmoList(){return Collections.emptyList();}};assertFalse(p.entityAcceptableTarget(shell(30)));}
    @Test void howardAcceptsArtilleryShell(){TileEntityTurretHoward p=new TileEntityTurretHoward();assertTrue(p.entityAcceptableTarget(shell(30)));}
    @Test void howardAcceptsArtilleryRocket(){TileEntityTurretHoward p=new TileEntityTurretHoward();assertTrue(p.entityAcceptableTarget(new EntityArtilleryRocket(null)));}
    @Test void machineTargetSwitchStillBlocksArtillery(){TileEntityTurretHoward p=new TileEntityTurretHoward();p.targetMachines=false;assertFalse(p.entityAcceptableTarget(shell(30)));}
    @Test void existingCompatibilityBlacklistStillBlocksArtillery(){TileEntityTurretHoward p=new TileEntityTurretHoward();CompatExternal.turretTargetBlacklist.add(EntityArtilleryShell.class);try{assertFalse(p.entityAcceptableTarget(shell(30)));}finally{CompatExternal.turretTargetBlacklist.remove(EntityArtilleryShell.class);}}
    @Test void originalProjectileHealthIs50And100()throws Exception{assertEquals(50,health(shell(30)));assertEquals(100,health(new EntityArtilleryRocket(null)));}
    @Test void serverProjectileDamageUsesIntegerAmount()throws Exception{EntityArtilleryShell e=shell(30);e.world=world(false);assertTrue(e.attackEntityFrom(DamageSource.GENERIC,3.9F));assertEquals(47,health(e));assertFalse(e.isDead);}
    @Test void clientProjectileDamageCannotChangeHealth()throws Exception{EntityArtilleryRocket e=new EntityArtilleryRocket(null);e.world=world(true);e.attackEntityFrom(DamageSource.GENERIC,100);assertEquals(100,health(e));assertFalse(e.isDead);}
    @Test void projectileInvulnerabilityRemainsEffective()throws Exception{EntityArtilleryShell e=shell(30);e.world=world(false);e.setEntityInvulnerable(true);assertFalse(e.attackEntityFrom(DamageSource.GENERIC,50));assertEquals(50,health(e));}
    @Test void lethalInterceptionRemovesProjectileWithoutImpact()throws Exception{EntityArtilleryShell e=shell(30);e.world=world(false);assertTrue(e.attackEntityFrom(DamageSource.GENERIC,50));assertTrue(e.isDead);assertTrue(health(e)<=0);}
    @Test void projectileHealthPersistsWithOldSaveDefault()throws Exception{EntityArtilleryRocket e=new EntityArtilleryRocket(null);e.world=world(false);e.attackEntityFrom(DamageSource.GENERIC,25);NBTTagCompound n=new NBTTagCompound();e.writeEntityToNBT(n);EntityArtilleryRocket loaded=new EntityArtilleryRocket(null);loaded.readEntityFromNBT(n);assertEquals(75,health(loaded));n.removeTag("health");EntityArtilleryRocket old=new EntityArtilleryRocket(null);old.readEntityFromNBT(n);assertEquals(100,health(old));}
    private static List<AmmoPressRecipe> belts(){List<AmmoPressRecipe> all=new ArrayList<>();EEAmmoPressRecipes.register(all);List<AmmoPressRecipe> out=new ArrayList<>();for(AmmoPressRecipe r:all)if(r.output.getItem()==ModItems.ammo_dgk)out.add(r);return out;}
    @Test void ownLegacyBeltRecipeRemainsAvailable(){assertTrue(belts().stream().anyMatch(r->Arrays.stream(r.input).filter(Objects::nonNull).count()==9));}
    @Test void originalBeltRecipeAddsThreeFiveUnitInputs(){assertTrue(belts().stream().anyMatch(r->r.input[1]!=null&&r.input[1].stacksize==5&&r.input[4]!=null&&r.input[4].stacksize==5&&r.input[7]!=null&&r.input[7].stacksize==5&&Arrays.stream(r.input).filter(Objects::nonNull).count()==3));}
    @Test void newVariantIsAppendedToPreserveExistingRecipeIndices(){List<AmmoPressRecipe> all=new ArrayList<>();EEAmmoPressRecipes.register(all);AmmoPressRecipe r=all.get(all.size()-1);assertSame(ModItems.ammo_dgk,r.output.getItem());assertEquals(5,r.input[1].stacksize);assertEquals(5,r.input[4].stacksize);assertEquals(5,r.input[7].stacksize);}
    @Test void oldOversizedBeltStacksRetainCountOnSave(){ItemStack old=new ItemStack(ModItems.ammo_dgk,64);NBTTagCompound n=old.writeToNBT(new NBTTagCompound());assertEquals(64,new ItemStack(n).getCount());}
}
