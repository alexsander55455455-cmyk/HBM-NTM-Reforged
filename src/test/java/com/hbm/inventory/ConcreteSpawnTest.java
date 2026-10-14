package com.hbm.inventory;

import com.hbm.blocks.ModBlocks;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import com.hbm.testutil.ForgeTestBootstrap;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConcreteSpawnTest {
    @BeforeAll
    static void bootstrap() throws Exception {
        ForgeTestBootstrap.ensureServerSide();
        if (!Bootstrap.isRegistered()) Bootstrap.register();
        if (!ForgeRegistries.BLOCKS.containsKey(new ResourceLocation("hbm:concrete"))) AutoCrystallizerOreDictionaryTest.bootstrap();
    }
    @Test void ordinaryConcreteDoesNotAllowSpawning() {
        assertFalse(ModBlocks.concrete.canEntitySpawn(ModBlocks.concrete.getDefaultState(), null));
    }
    @Test void smoothConcreteDoesNotAllowSpawning() {
        assertFalse(ModBlocks.concrete_smooth.canEntitySpawn(ModBlocks.concrete_smooth.getDefaultState(), null));
    }
    @Test void ordinaryStoneAndReinforcedBrickKeepTheirBehavior() {
        assertTrue(Blocks.STONE.canEntitySpawn(Blocks.STONE.getDefaultState(), null));
        assertTrue(ModBlocks.reinforced_brick.canEntitySpawn(ModBlocks.reinforced_brick.getDefaultState(), null));
    }
}
