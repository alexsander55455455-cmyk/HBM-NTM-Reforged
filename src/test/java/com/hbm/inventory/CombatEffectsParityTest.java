package com.hbm.inventory;

import com.hbm.capability.HbmLivingCapability;
import com.hbm.capability.HbmLivingCapability.IEntityHbmProps;
import com.hbm.capability.HbmLivingProps;
import com.hbm.config.GeneralConfig;
import com.hbm.config.RadiationConfig;
import com.hbm.handler.EntityEffectHandler;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.*;
import com.hbm.items.weapon.sedna.factory.*;
import com.hbm.lib.ModDamageSource;
import com.hbm.util.DamageResistanceHandler.DamageClass;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.capabilities.Capability;
import org.junit.jupiter.api.*;
import java.lang.reflect.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CombatEffectsParityTest {
    private static DataParameter<Float> HEALTH;
    @BeforeAll static void bootstrap() throws Exception { DataConsumerRecipeTest.bootstrap(); GunFactory.init();Field f=EntityLivingBase.class.getDeclaredField("HEALTH");f.setAccessible(true);HEALTH=(DataParameter<Float>)f.get(null); }
    private boolean rads,contamination;
    @BeforeEach void enable(){rads=GeneralConfig.enableRads;contamination=RadiationConfig.enableContamination;GeneralConfig.enableRads=true;RadiationConfig.enableContamination=true;}
    @AfterEach void restore(){GeneralConfig.enableRads=rads;RadiationConfig.enableContamination=contamination;}
    private static Object allocate(Class<?> c)throws Exception{Class<?> u=Class.forName("sun.misc.Unsafe");Field f=u.getDeclaredField("theUnsafe");f.setAccessible(true);return u.getMethod("allocateInstance",Class.class).invoke(f.get(null),c);}
    private static World world(boolean remote)throws Exception{World w=(World)allocate(WorldServer.class);Field f=World.class.getField("isRemote");f.setAccessible(true);f.setBoolean(w,remote);f=World.class.getField("rand");f.setAccessible(true);f.set(w,new Random(){@Override public int nextInt(int bound){return 1;}});return w;}
    private static class PlayerProbe extends EntityPlayer {
        IEntityHbmProps props;float healthAfterAttack;int attacks,deaths;boolean immune;DamageSource lastSource;float lastAmount;
        PlayerProbe(){super(null,null);}
        @Override public boolean isSpectator(){return false;}
        @Override public boolean isCreative(){return false;}
        @Override public boolean isEntityInvulnerable(DamageSource source){return immune;}
        @Override public boolean attackEntityFrom(DamageSource source,float amount){attacks++;lastSource=source;lastAmount=amount;setHealth(healthAfterAttack);return true;}
        @Override public void setHealth(float value){getDataManager().set(HEALTH,value);}
        @Override public void notifyDataManagerChange(DataParameter<?> key){}
        @Override public void onDeath(DamageSource source){deaths++;}
        @Override public boolean hasCapability(Capability<?> cap,EnumFacing facing){return true;}
        @Override public <T>T getCapability(Capability<T> cap,EnumFacing facing){return (T)props;}
    }
    private static class MobProbe extends EntityZombie {
        IEntityHbmProps props;int attacks,deaths;
        MobProbe(){super(null);}
        @Override public boolean isEntityInvulnerable(DamageSource source){return false;}
        @Override public boolean attackEntityFrom(DamageSource source,float amount){attacks++;return true;}
        @Override public void setHealth(float value){getDataManager().set(HEALTH,value);}
        @Override public void notifyDataManagerChange(DataParameter<?> key){}
        @Override public void onDeath(DamageSource source){deaths++;}
        @Override public boolean hasCapability(Capability<?> cap,EnumFacing facing){return true;}
        @Override public <T>T getCapability(Capability<T> cap,EnumFacing facing){return (T)props;}
    }
    private static void healthData(EntityLivingBase entity)throws Exception{Field f=Entity.class.getDeclaredField("dataManager");f.setAccessible(true);EntityDataManager manager=new EntityDataManager(entity);f.set(entity,manager);manager.register(HEALTH,20F);}
    private static PlayerProbe player(double radiation,float postHitHealth)throws Exception{PlayerProbe p=(PlayerProbe)allocate(PlayerProbe.class);p.world=world(false);healthData(p);p.props=new HbmLivingCapability.EntityHbmProps();p.props.setRads(radiation);p.healthAfterAttack=postHitHealth;return p;}
    private static void apply(Object entity)throws Exception{Method m=EntityEffectHandler.class.getDeclaredMethod("handleRadiationEffect",net.minecraft.entity.EntityLivingBase.class);m.setAccessible(true);m.invoke(null,entity);}
    @Test void radiationDoesNotKillPlayerAgainAfterRevival()throws Exception{PlayerProbe p=player(1000,1);apply(p);assertEquals(1,p.getHealth());assertEquals(0,p.deaths);assertEquals(1,p.attacks);}
    @Test void radiationPreservesSpecialReviveHealth()throws Exception{PlayerProbe p=player(1000,8);apply(p);assertEquals(8,p.getHealth());assertEquals(0,p.deaths);}
    @Test void fatalRadiationStillDealsOneThousandDamage()throws Exception{PlayerProbe p=player(1000,0);apply(p);assertEquals(1,p.attacks);assertSame(ModDamageSource.radiation,p.lastSource);assertEquals(1000F,p.lastAmount);assertEquals(0,p.getHealth());assertEquals(0,p.deaths);}
    @Test void fatalRadiationClearsAccumulatedDose()throws Exception{PlayerProbe p=player(1000,1);apply(p);assertEquals(0,HbmLivingProps.getRadiation(p));}
    @Test void belowFatalThresholdDoesNotDealFatalHit()throws Exception{PlayerProbe p=player(999,1);apply(p);assertEquals(0,p.attacks);assertEquals(20,p.getHealth());assertEquals(999,HbmLivingProps.getRadiation(p));}
    @Test void clientCannotApplyFatalRadiation()throws Exception{PlayerProbe p=player(1000,1);p.world=world(true);apply(p);assertEquals(0,p.attacks);assertEquals(1000,HbmLivingProps.getRadiation(p));}
    @Test void disabledRadiationRemainsDisabled()throws Exception{PlayerProbe p=player(1000,1);GeneralConfig.enableRads=false;apply(p);assertEquals(0,p.attacks);}
    @Test void invulnerablePlayerStillRejectsRadiation()throws Exception{PlayerProbe p=player(1000,1);p.immune=true;apply(p);assertEquals(0,p.attacks);}
    @Test void nonplayerFatalFallbackRemainsEffective()throws Exception{MobProbe p=(MobProbe)allocate(MobProbe.class);p.world=world(false);healthData(p);p.props=new HbmLivingCapability.EntityHbmProps();p.props.setRads(1000);apply(p);assertEquals(1,p.attacks);assertEquals(0,p.getHealth());assertEquals(1,p.deaths);assertEquals(0,HbmLivingProps.getRadiation(p));}
    @Test void stingerUsesSpecialWeaponQuality(){assertEquals(ItemGunBaseNT.WeaponQuality.SPECIAL,((ItemGunBaseNT)ModItems.gun_stinger).quality);}
    @Test void stingerKeepsOwnReceiverAndDamageConfiguration(){ItemGunBaseNT g=(ItemGunBaseNT)ModItems.gun_stinger;assertEquals(1,g.getConfigCount());assertEquals(1,g.getConfig(null,0).getReceivers(ItemStack.EMPTY).length);assertEquals(XFactoryRocket.GUN_STINGER_DAMAGE.get(),g.getConfig(null,0).getReceivers(ItemStack.EMPTY)[0].getBaseDamage(ItemStack.EMPTY));}
    private static DamageClass damage(BulletConfig c){return c.dmgClass;}
    @Test void allSixShredderSecondaryProjectilesUsePlasma()throws Exception{for(BulletConfig c:new BulletConfig[]{XFactory12ga.g12_sub,XFactory12ga.g12_sub_slug,XFactory12ga.g12_sub_flechette,XFactory12ga.g12_sub_magnum,XFactory12ga.g12_sub_explosive,XFactory12ga.g12_sub_phosphorus})assertEquals(DamageClass.PLASMA,damage(c));}
    @Test void initialShredderBeamsKeepOriginalLaserType()throws Exception{for(BulletConfig c:new BulletConfig[]{XFactory12ga.g12_shredder,XFactory12ga.g12_shredder_slug,XFactory12ga.g12_shredder_flechette,XFactory12ga.g12_shredder_magnum,XFactory12ga.g12_shredder_explosive,XFactory12ga.g12_shredder_phosphorus})assertEquals(DamageClass.LASER,damage(c));}
    @Test void regularShotgunPelletsKeepOwnPhysicalType()throws Exception{assertEquals(DamageClass.PHYSICAL,damage(XFactory12ga.g12));}
}
