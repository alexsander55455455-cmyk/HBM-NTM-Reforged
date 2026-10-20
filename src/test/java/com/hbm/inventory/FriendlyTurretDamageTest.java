package com.hbm.inventory;

import com.hbm.config.ServerConfig;
import com.hbm.items.weapon.sedna.factory.GunFactory;
import com.hbm.items.weapon.sedna.factory.XFactoryTurret;
import com.hbm.main.MainRegistry;
import com.hbm.tileentity.turret.TileEntityTurretChekhov;
import com.hbm.tileentity.turret.TileEntityTurretFriendly;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class FriendlyTurretDamageTest {
    @TempDir Path configDir;
    File priorDir;
    float friendly,chekhov;
    @BeforeAll static void bootstrap() throws Exception { DataConsumerRecipeTest.bootstrap();GunFactory.init(); }
    @BeforeEach void remember() { priorDir=MainRegistry.configHbmDir;MainRegistry.configHbmDir=configDir.toFile();friendly=XFactoryTurret.TURRET_FRIENDLY_DAMAGE.get();chekhov=XFactoryTurret.TURRET_CHEKHOV_DAMAGE.get(); }
    @AfterEach void restore() { MainRegistry.configHbmDir=priorDir;XFactoryTurret.TURRET_FRIENDLY_DAMAGE.set(friendly);XFactoryTurret.TURRET_CHEKHOV_DAMAGE.set(chekhov); }
    void file(String text) throws Exception { Files.write(configDir.resolve("hbmServer.json"),text.getBytes(StandardCharsets.UTF_8)); }
    @Test void freshFriendlyDefaultIsHalfChekhov() { assertEquals(5F,new TileEntityTurretFriendly().getBaseDamage());assertEquals(10F,new TileEntityTurretChekhov().getBaseDamage()); }
    @Test void oldConfigWithoutFriendlyKeyGetsFive() throws Exception { file("{\"MINE_AP_DAMAGE\":10}");ServerConfig.initConfig();assertEquals(5F,new TileEntityTurretFriendly().getBaseDamage()); }
    @Test void explicitLegacyTenIsPreserved() throws Exception { file("{\"TURRET_FRIENDLY_DAMAGE\":10}");ServerConfig.initConfig();assertEquals(10F,new TileEntityTurretFriendly().getBaseDamage()); }
    @Test void existingTurretReadsFractionalReload() throws Exception { TileEntityTurretFriendly turret=new TileEntityTurretFriendly();file("{\"TURRET_FRIENDLY_DAMAGE\":7.25}");ServerConfig.reload();assertEquals(7.25F,turret.getBaseDamage()); }
    @Test void explicitZeroRemainsZero() throws Exception { file("{\"TURRET_FRIENDLY_DAMAGE\":0}");ServerConfig.initConfig();assertEquals(0F,new TileEntityTurretFriendly().getBaseDamage()); }
    @Test void friendlyOverrideDoesNotChangeChekhov() throws Exception { file("{\"TURRET_FRIENDLY_DAMAGE\":17.5}");ServerConfig.initConfig();assertEquals(17.5F,new TileEntityTurretFriendly().getBaseDamage());assertEquals(10F,new TileEntityTurretChekhov().getBaseDamage()); }
    @Test void omittedKeyOnReloadKeepsCurrentOverride() throws Exception { XFactoryTurret.TURRET_FRIENDLY_DAMAGE.set(12.5F);file("{}");ServerConfig.reload();assertEquals(12.5F,new TileEntityTurretFriendly().getBaseDamage()); }
    @Test void refreshAndReloadPreserveCustomValue() throws Exception { ServerConfig.initConfig();XFactoryTurret.TURRET_FRIENDLY_DAMAGE.set(3.75F);ServerConfig.refresh();XFactoryTurret.TURRET_FRIENDLY_DAMAGE.set(1F);ServerConfig.reload();assertEquals(3.75F,new TileEntityTurretFriendly().getBaseDamage()); }
}
