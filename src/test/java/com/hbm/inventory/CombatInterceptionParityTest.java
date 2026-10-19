package com.hbm.inventory;

import com.google.gson.JsonParser;
import com.hbm.api.entity.IRadarDetectableNT;
import com.hbm.api.entity.IRadarDetectableNT.RadarScanParams;
import com.hbm.entity.projectile.EntityMeteor;
import com.hbm.items.ModItems;
import com.hbm.items.armor.ItemModLodestone;
import com.hbm.items.gear.ArmorFSB;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.util.DamageResistanceHandler;
import com.hbm.util.DamageResistanceHandler.DamageClass;
import com.hbm.util.DamageResistanceHandler.ResistanceStats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import org.junit.jupiter.api.*;
import java.lang.reflect.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CombatInterceptionParityTest {
    @BeforeAll static void bootstrap() throws Exception { DataConsumerRecipeTest.bootstrap(); }
    private static Object allocate(Class<?> c) throws Exception {
        Class<?> u=Class.forName("sun.misc.Unsafe");Field f=u.getDeclaredField("theUnsafe");f.setAccessible(true);
        return u.getMethod("allocateInstance",Class.class).invoke(f.get(null),c);
    }
    private static class TestWorld extends WorldServer {
        List<EntityItem> items; AxisAlignedBB queried;
        TestWorld(){super(null,null,null,0,null);}
        @Override public <T extends Entity> List<T> getEntitiesWithinAABB(Class<? extends T> type,AxisAlignedBB box) {
            queried=box;List<T> result=new ArrayList<>();for(EntityItem e:items)if(type.isInstance(e)&&box.intersects(e.getEntityBoundingBox()))result.add(type.cast(e));return result;
        }
    }
    private static TestWorld world(boolean client) throws Exception {
        TestWorld w=(TestWorld)allocate(TestWorld.class);w.items=new ArrayList<>();Field side=World.class.getField("isRemote");side.setAccessible(true);side.setBoolean(w,client);return w;
    }
    private static EntityMeteor meteor(boolean client)throws Exception{EntityMeteor e=new EntityMeteor(null);e.world=world(client);return e;}
    private static DamageSource energy(DamageClass c){return BulletConfig.getDamage(null,null,c);}
    private static String category(DamageSource s)throws Exception{Method m=DamageResistanceHandler.class.getDeclaredMethod("typeToCategory",DamageSource.class);m.setAccessible(true);return (String)m.invoke(null,s);}
    @Test void realSednaEnergySourcesUseEnergyCategory()throws Exception{for(DamageClass c:new DamageClass[]{DamageClass.LASER,DamageClass.PLASMA,DamageClass.MICROWAVE,DamageClass.SUBATOMIC,DamageClass.ELECTRIC})assertEquals("EN",category(energy(c)),c.name());}
    @Test void energyNamesAreCaseIndependent()throws Exception{for(String s:new String[]{"laser","LASER","PlAsMa","MICROWAVE","electric","Subatomic"})assertEquals("EN",category(new DamageSource(s)),s);}
    @Test void explosionFireAndProjectileFlagsKeepPriority()throws Exception{assertEquals("EXPL",category(new DamageSource("laser").setExplosion()));assertEquals("FIRE",category(new DamageSource("laser").setFireDamage()));assertEquals("PHYS",category(new DamageSource("laser").setProjectile()));}
    @Test void unrelatedDamageCategoryRemainsUnchanged()throws Exception{assertEquals("custom",category(new DamageSource("custom")));assertEquals("PHYS",category(DamageSource.CACTUS));}
    private static Object resistance(String json,DamageSource s)throws Exception{ResistanceStats stats=ResistanceStats.deserialize(new JsonParser().parse(json).getAsJsonObject());Method m=ResistanceStats.class.getDeclaredMethod("getResistance",DamageSource.class);m.setAccessible(true);return m.invoke(stats,s);}
    private static float threshold(Object r)throws Exception{Field f=r.getClass().getDeclaredField("threshold");f.setAccessible(true);return f.getFloat(r);}
    @Test void energyArmorResistanceIsActuallySelected()throws Exception{assertEquals(25F,threshold(resistance("{\"category\":[[\"EN\",25,0.75]],\"other\":[1,0.1]}",energy(DamageClass.PLASMA))));}
    @Test void exactArmorResistanceStillOverridesCategory()throws Exception{assertEquals(10F,threshold(resistance("{\"exact\":[[\"laser\",10,0.75]],\"category\":[[\"EN\",25,0.75]]}",energy(DamageClass.LASER))));}
    @Test void meteorSurvivesEnergyBelow250()throws Exception{EntityMeteor e=meteor(false);e.attackEntityFrom(energy(DamageClass.LASER),249.99F);assertFalse(e.isDead);}
    @Test void meteorIsInterceptedAtExactly250()throws Exception{EntityMeteor e=meteor(false);e.attackEntityFrom(energy(DamageClass.LASER),250);assertTrue(e.isDead);}
    @Test void meteorIsInterceptedAbove250()throws Exception{EntityMeteor e=meteor(false);e.attackEntityFrom(energy(DamageClass.PLASMA),251);assertTrue(e.isDead);}
    @Test void allFiveEnergyTypesCanInterceptMeteor()throws Exception{for(DamageClass c:new DamageClass[]{DamageClass.LASER,DamageClass.PLASMA,DamageClass.MICROWAVE,DamageClass.SUBATOMIC,DamageClass.ELECTRIC}){EntityMeteor e=meteor(false);e.attackEntityFrom(energy(c),1000);assertTrue(e.isDead,c.name());}}
    @Test void physicalExplosionAndFireCannotInterceptMeteor()throws Exception{for(DamageSource s:new DamageSource[]{DamageSource.GENERIC,energy(DamageClass.PHYSICAL),energy(DamageClass.EXPLOSIVE),energy(DamageClass.FIRE)}){EntityMeteor e=meteor(false);e.attackEntityFrom(s,100000);assertFalse(e.isDead,s.getDamageType());}}
    @Test void meteorInterceptionIsServerOnly()throws Exception{EntityMeteor e=meteor(true);e.attackEntityFrom(energy(DamageClass.LASER),1000);assertFalse(e.isDead);}
    @Test void meteorDamageRetainsOriginalFalseReturn()throws Exception{EntityMeteor e=meteor(false);assertFalse(e.attackEntityFrom(energy(DamageClass.LASER),1000));assertTrue(e.isDead);}
    @Test void meteorUsesSpecialRadarBlipAndTranslation()throws Exception{IRadarDetectableNT e=(IRadarDetectableNT)meteor(false);assertEquals(IRadarDetectableNT.SPECIAL,e.getBlipLevel());assertEquals("radar.target.meteor",e.getTranslationKey());assertTrue(e.canBeSeenBy(new Object()));}
    @Test void meteorRadarAndRedstoneRespectMissileSwitch()throws Exception{IRadarDetectableNT e=(IRadarDetectableNT)meteor(false);for(boolean enabled:new boolean[]{true,false}){RadarScanParams p=new RadarScanParams(enabled,false,false,false);assertEquals(enabled,e.paramsApplicable(p));assertEquals(enabled,e.suppliesRedstone(p));}}
    @Test void meteorSafeFlagStillPersists()throws Exception{EntityMeteor e=meteor(false);e.safe=true;NBTTagCompound n=new NBTTagCompound();Method write=EntityMeteor.class.getDeclaredMethod("writeEntityToNBT",NBTTagCompound.class);write.setAccessible(true);write.invoke(e,n);assertTrue(n.getBoolean("safe"));}
    private static class LivingProbe extends EntityVillager {
        int sounds;
        LivingProbe(){super(null);}
        @Override public void playSound(SoundEvent sound,float volume,float pitch){sounds++;}
    }
    private static LivingProbe wearer(TestWorld w){LivingProbe e=new LivingProbe();e.world=w;e.setPosition(0,0,0);return e;}
    private static EntityItem item(TestWorld w,double x,double y){EntityItem e=new EntityItem(null);e.world=w;e.setPosition(x,y,0);e.motionX=e.motionY=e.motionZ=0;w.items.add(e);return e;}
    @Test void magnetIsFiftyPercentStrongerWithinOwnBalance()throws Exception{TestWorld w=world(false);LivingProbe e=wearer(w);EntityItem i=item(w,2,0);new ItemModLodestone(5,"probe").modUpdate(e,ItemStack.EMPTY);assertEquals(-0.075,i.motionX,1e-10);assertEquals(0,i.motionY,1e-10);assertEquals(0,i.motionZ,1e-10);}
    @Test void magnetMarksVelocityForTrackingPacket()throws Exception{TestWorld w=world(false);LivingProbe e=wearer(w);EntityItem i=item(w,2,0);new ItemModLodestone(5,"probe").modUpdate(e,ItemStack.EMPTY);assertTrue(i.velocityChanged);}
    @Test void magnetPreservesUpwardKick()throws Exception{TestWorld w=world(false);LivingProbe e=wearer(w);EntityItem i=item(w,2,-0.1);new ItemModLodestone(5,"probe").modUpdate(e,ItemStack.EMPTY);assertTrue(i.motionY>0.2);}
    @Test void clientMagnetDoesNotQueryOrMutateItems()throws Exception{TestWorld w=world(true);LivingProbe e=wearer(w);EntityItem i=item(w,2,0);new ItemModLodestone(5,"probe").modUpdate(e,ItemStack.EMPTY);assertNull(w.queried);assertEquals(0,i.motionX);assertFalse(i.velocityChanged);}
    @Test void magnetRetainsOwnAttractionRange()throws Exception{TestWorld w=world(false);LivingProbe e=wearer(w);EntityItem i=item(w,7,0);new ItemModLodestone(5,"probe").modUpdate(e,ItemStack.EMPTY);assertEquals(e.getEntityBoundingBox().grow(5),w.queried);assertEquals(0,i.motionX);assertFalse(i.velocityChanged);}
    private static LivingProbe armored()throws Exception{LivingProbe e=wearer(world(false));e.setItemStackToSlot(EntityEquipmentSlot.HEAD,new ItemStack(ModItems.security_helmet));e.setItemStackToSlot(EntityEquipmentSlot.CHEST,new ItemStack(ModItems.security_plate));e.setItemStackToSlot(EntityEquipmentSlot.LEGS,new ItemStack(ModItems.security_legs));e.setItemStackToSlot(EntityEquipmentSlot.FEET,new ItemStack(ModItems.security_boots));assertTrue(ArmorFSB.hasFSBArmor(e));return e;}
    private static int fall(float distance,boolean sneak,boolean full)throws Exception{LivingProbe e=armored();e.fallDistance=distance;e.setSneaking(sneak);if(!full)e.setItemStackToSlot(EntityEquipmentSlot.FEET,ItemStack.EMPTY);ArmorFSB chest=(ArmorFSB)ModItems.security_plate;SoundEvent old=chest.fall;try{chest.fall=SoundEvents.BLOCK_ANVIL_LAND;ArmorFSB.handleFall(e);return e.sounds;}finally{chest.fall=old;}}
    @Test void halfBlockLandingIsSilent()throws Exception{assertEquals(0,fall(0.5F,false,true));}
    @Test void QuarterBlockLandingIsSilent()throws Exception{assertEquals(0,fall(0.25F,false,true));}
    @Test void LandingJustBelowHalfBlockIsSilent()throws Exception{assertEquals(0,fall(0.49F,false,true));}
    @Test void LandingAboveHalfBlockKeepsSound()throws Exception{assertEquals(1,fall(0.51F,false,true));}
    @Test void ownSneakingSoundSuppressionRemains()throws Exception{assertEquals(0,fall(1F,true,true));}
    @Test void incompleteSetDoesNotPlayLandingSound()throws Exception{assertEquals(0,fall(1F,false,false));}
}
