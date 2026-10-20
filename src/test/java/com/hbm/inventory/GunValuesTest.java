package com.hbm.inventory;

import com.hbm.config.RunningConfig.ConfigWrapper;
import com.hbm.config.ServerConfig;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.factory.GunFactory;
import com.hbm.items.weapon.sedna.mods.WeaponModTestDamage;
import com.hbm.items.weapon.sedna.mods.XWeaponModManager;
import com.hbm.main.MainRegistry;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.io.File;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class GunValuesTest {
    @TempDir Path configDir;
    private File priorDir;
    private final Map<ConfigWrapper, Object> prior = new IdentityHashMap<>();

    @BeforeAll static void bootstrap() throws Exception {
        DataConsumerRecipeTest.bootstrap();
        GunFactory.init();
    }

    @BeforeEach void remember() throws Exception {
        priorDir = MainRegistry.configHbmDir;
        MainRegistry.configHbmDir = configDir.toFile();
        try { init(); } catch(ClassNotFoundException missingOnBaseline) { }
        for(ConfigWrapper value : ServerConfig.configMap.values()) prior.put(value, value.get());
    }

    @AfterEach void restore() {
        prior.forEach(ConfigWrapper::set);
        MainRegistry.configHbmDir = priorDir;
    }

    private static void init() throws Exception {
        Class.forName("com.hbm.items.weapon.sedna.factory.GunValues").getMethod("initDefaults").invoke(null);
    }

    private static ConfigWrapper<Float> setting(String name) throws Exception {
        init();
        ConfigWrapper value = ServerConfig.configMap.get(name);
        assertNotNull(value, name);
        assertTrue(value.get() instanceof Float, name);
        return value;
    }

    private static Receiver receiver(ConfigWrapper<Float> value) throws Exception {
        Receiver r = new Receiver(0);
        Receiver.class.getMethod("dmg", ConfigWrapper.class).invoke(r, value);
        new GunConfig().rec(r);
        return r;
    }

    private static Receiver scalar(float value) {
        Receiver r = new Receiver(0).dmg(value);
        new GunConfig().rec(r);
        return r;
    }

    private void file(String json) throws Exception {
        Files.write(configDir.resolve("hbmServer.json"), json.getBytes(StandardCharsets.UTF_8));
    }

    private static Set<String> keys() {
        Set<String> result = new HashSet<>();
        for(String k : ServerConfig.configMap.keySet())
            if(k.startsWith("GUN_") || k.startsWith("TURRET_") || k.startsWith("MELEE_") || k.startsWith("RANGED_NCRPA_")) result.add(k);
        return result;
    }

    @Test void existingScalarDamageStillWorks() { assertEquals(12.5F, scalar(12.5F).getBaseDamage(ItemStack.EMPTY)); }
    @Test void existingScalarSetterStillReplacesItsValue() { assertEquals(3F, scalar(12F).dmg(3F).getBaseDamage(ItemStack.EMPTY)); }
    @Test void configurationValueIsUsed() throws Exception { assertEquals(8F, receiver(new ConfigWrapper<>(8F)).getBaseDamage(ItemStack.EMPTY)); }
    @Test void anExistingReceiverReadsUpdates() throws Exception { ConfigWrapper<Float> v=new ConfigWrapper<>(8F); Receiver r=receiver(v); v.set(17F); assertEquals(17F,r.getBaseDamage(ItemStack.EMPTY)); }
    @Test void zeroConfiguredDamageRemainsZero() throws Exception { assertEquals(0F,receiver(new ConfigWrapper<>(0F)).getBaseDamage(ItemStack.EMPTY)); }
    @Test void fractionalConfiguredDamageIsPreserved() throws Exception { assertEquals(.125F,receiver(new ConfigWrapper<>(.125F)).getBaseDamage(ItemStack.EMPTY)); }
    @Test void legacyConstantAndConfigAreAdditive() throws Exception { assertEquals(7F,receiver(new ConfigWrapper<>(5F)).dmg(2F).getBaseDamage(ItemStack.EMPTY)); }
    @Test void receiverUpdateDoesNotNeedAReplacementStack() throws Exception { ConfigWrapper<Float> v=new ConfigWrapper<>(3F); Receiver r=receiver(v); ItemStack s=new ItemStack(ModItems.diamond_gavel); v.set(9F); assertEquals(9F,r.getBaseDamage(s)); }
    @Test void sharedConfigurationUpdatesEveryBoundReceiver() throws Exception { ConfigWrapper<Float> v=new ConfigWrapper<>(3F); Receiver a=receiver(v),b=receiver(v);v.set(19F);assertEquals(19F,a.getBaseDamage(ItemStack.EMPTY));assertEquals(19F,b.getBaseDamage(ItemStack.EMPTY)); }
    @Test void independentBarrelsKeepSeparateValues() throws Exception { ConfigWrapper<Float> a=new ConfigWrapper<>(3F),b=new ConfigWrapper<>(5F);Receiver left=receiver(a),right=receiver(b);a.set(20F);assertEquals(20F,left.getBaseDamage(ItemStack.EMPTY));assertEquals(5F,right.getBaseDamage(ItemStack.EMPTY)); }
    @Test void readingDamageDoesNotWriteConfigurationIntoItemNBT() throws Exception { ItemStack s=new ItemStack(ModItems.diamond_gavel);receiver(new ConfigWrapper<>(15F)).getBaseDamage(s);assertFalse(s.hasTagCompound()); }
    @Test void existingWeaponModifierRunsAfterConfiguredBaseDamage() throws Exception {
        int id=987654; assertFalse(XWeaponModManager.idToMod.containsKey(id));
        new WeaponModTestDamage(id,"test");
        try { ItemStack s=new ItemStack(ModItems.diamond_gavel);NBTTagCompound tag=new NBTTagCompound();tag.setIntArray(XWeaponModManager.KEY_MOD_LIST+0,new int[]{id});s.setTagCompound(tag);ConfigWrapper<Float> v=new ConfigWrapper<>(8F);Receiver r=receiver(v);assertEquals(12F,r.getBaseDamage(s));v.set(12F);assertEquals(18F,r.getBaseDamage(s)); }
        finally { XWeaponModManager.idToMod.remove(id); }
    }
    @Test void allRegisteredCombatSettingsAreFloatsAndDistinct() throws Exception { init();assertEquals(91,keys().size());Set<Object> wrappers=Collections.newSetFromMap(new IdentityHashMap<>());for(String k:keys()){ConfigWrapper<?> v=ServerConfig.configMap.get(k);assertTrue(v.get() instanceof Float,k);assertTrue(wrappers.add(v),k);} }
    @Test void registrationDoesNotResetAnEditedValue() throws Exception { ConfigWrapper<Float> v=setting("GUN_ASSAULT_RIFLE_DAMAGE");v.set(21F);init();assertSame(v,ServerConfig.configMap.get("GUN_ASSAULT_RIFLE_DAMAGE"));assertEquals(21F,v.get()); }
    @Test void oldServerFileKeepsExistingSettingsAndAddsGunDefaults() throws Exception { file("{\"MINE_AP_DAMAGE\":13.25}");ServerConfig.initConfig();assertEquals(13.25F,ServerConfig.MINE_AP_DAMAGE.get());assertEquals(5F,setting("GUN_ASSAULT_RIFLE_DAMAGE").get());assertEquals(91,keys().size()); }
    @Test void initialServerFileContainsRealFloatSettings() throws Exception { ServerConfig.initConfig();String s=new String(Files.readAllBytes(configDir.resolve("hbmServer.json")),StandardCharsets.UTF_8);assertTrue(s.contains("\"GUN_ASSAULT_RIFLE_DAMAGE\""));assertTrue(s.contains("\"MELEE_RPA_SWINGS_DAMAGE\""));assertTrue(s.contains("\"TURRET_HOWARD_DAMAGE\"")); }
    @Test void serverFileReloadUpdatesAnExistingReceiver() throws Exception { Receiver r=receiver(setting("GUN_ASSAULT_RIFLE_DAMAGE"));file("{\"GUN_ASSAULT_RIFLE_DAMAGE\":17.5}");ServerConfig.reload();assertEquals(17.5F,r.getBaseDamage(ItemStack.EMPTY)); }
    @Test void fileReloadKeepsAkimboBarrelsIndependent() throws Exception { Receiver left=receiver(setting("GUN_UZI_AKIMBO_LEFT_DAMAGE")),right=receiver(setting("GUN_UZI_AKIMBO_RIGHT_DAMAGE"));file("{\"GUN_UZI_AKIMBO_LEFT_DAMAGE\":11.5,\"GUN_UZI_AKIMBO_RIGHT_DAMAGE\":4.25}");ServerConfig.reload();assertEquals(11.5F,left.getBaseDamage(ItemStack.EMPTY));assertEquals(4.25F,right.getBaseDamage(ItemStack.EMPTY)); }
    @Test void zeroDamageSurvivesFileReload() throws Exception { Receiver r=receiver(setting("GUN_BOLTER_DAMAGE"));file("{\"GUN_BOLTER_DAMAGE\":0}");ServerConfig.reload();assertEquals(0F,r.getBaseDamage(ItemStack.EMPTY)); }
    @Test void omittedSettingsKeepTheirCurrentValueOnReload() throws Exception { ConfigWrapper<Float> v=setting("GUN_BOLTER_DAMAGE");v.set(23F);file("{}");ServerConfig.reload();assertEquals(23F,v.get()); }
    @Test void refreshPersistsTheCurrentRuntimeValue() throws Exception { setting("GUN_BOLTER_DAMAGE").set(23.75F);ServerConfig.refresh();setting("GUN_BOLTER_DAMAGE").set(1F);ServerConfig.reload();assertEquals(23.75F,setting("GUN_BOLTER_DAMAGE").get()); }
    @Test void friendlyTurretDefaultIsFive() throws Exception { assertEquals(5F,setting("TURRET_FRIENDLY_DAMAGE").get()); }
    @Test void additionalAttackSettingsHaveIndependentValues() throws Exception { setting("GUN_TAU_CANNON_CHARGE_DAMAGE").set(7F);assertEquals(25F,setting("GUN_TAU_CANNON_DAMAGE").get());setting("GUN_HANGMAN_SMACK_DAMAGE").set(19F);assertEquals(25F,setting("GUN_HANGMAN_DAMAGE").get()); }
    @Test void allActualGunReceiversReadTheirLiveSetting() throws Exception {
        init();Field config=Receiver.class.getDeclaredField("baseDamageConfig_DNA");config.setAccessible(true);Set<Object> used=Collections.newSetFromMap(new IdentityHashMap<>());
        for(Field field:ModItems.class.getFields()) if(Modifier.isStatic(field.getModifiers()) && field.get(null) instanceof ItemGunBaseNT) {
            ItemGunBaseNT gun=(ItemGunBaseNT)field.get(null);
            for(int i=0;i<gun.getConfigCount();i++) for(Receiver r:gun.getConfig(null,i).getReceivers(ItemStack.EMPTY)) {
                ConfigWrapper<Float> v=(ConfigWrapper<Float>)config.get(r);if(v==null)continue;used.add(v);float old=v.get();try{v.set(old+13.25F);assertEquals(old+13.25F,r.getBaseDamage(ItemStack.EMPTY),field.getName());}finally{v.set(old);}
            }
        }
        assertEquals(69,used.size());
    }
    @Test void serverConfigFileCanBeReplacedImmediatelyAfterReload() throws Exception { file("{}");ServerConfig.reload();Path original=configDir.resolve("hbmServer.json"),moved=configDir.resolve("replaced.json");Files.move(original,moved);assertTrue(Files.exists(moved)); }
}
