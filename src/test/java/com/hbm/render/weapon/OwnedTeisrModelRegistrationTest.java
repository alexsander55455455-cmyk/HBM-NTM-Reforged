package com.hbm.render.weapon;

import com.hbm.items.ClaimedModelLocationRegistry;
import com.hbm.main.client.NTMClientRegistry;
import com.hbm.render.item.ItemRenderMissilePart;
import com.hbm.render.item.TEISRBase;
import com.hbm.render.item.weapon.ItemRenderGunEgon;
import com.hbm.render.item.weapon.ItemRenderJShotgun;
import com.hbm.render.item.weapon.sedna.ItemRenderMinigunDual;
import com.hbm.testutil.ForgeTestBootstrap;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.Item;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class OwnedTeisrModelRegistrationTest {
    @BeforeAll
    static void bootstrap() {
        ForgeTestBootstrap.ensureServerSide();
        Bootstrap.register();
    }

    @Test
    void dualMinigunDoesNotLoadLegacyUvIcon() throws Exception {
        Item item = new Item().setRegistryName("hbm", "registration_test_dual_minigun");
        ItemRenderMinigunDual renderer = new ItemRenderMinigunDual();
        try {
            Set<String> variants = register(item, renderer);
            assertEquals(1, variants.size());
            assertTrue(variants.contains(NTMClientRegistry.getSyntheticTeisrModelLocation(item).toString()));
            assertFalse(variants.contains(item.getRegistryName() + "#inventory"));
            assertSame(renderer, item.getTileEntityItemStackRenderer());
            assertSame(ItemCameraTransforms.DEFAULT, renderer.createModelBinding(item).getTransforms());
            assertTrue(renderer.useIdentityTransform(item));
        } finally {
            clear(item);
        }
    }

    @Test
    void twoDimensionalGuiBindingKeepsItsIconTexture() throws Exception {
        Item item = new Item().setRegistryName("hbm", "registration_test_gui_icon");
        TEISRBase renderer = new TEISRBase();
        try {
            Set<String> variants = register(item, renderer);
            assertEquals(1, variants.size());
            assertTrue(renderer.createModelBinding(item).useBaseModelInGui());
            assertTrue(renderer.loadModel(item, NTMClientRegistry.getSyntheticTeisrModelLocation(item))
                    .getTextures().stream().anyMatch(texture -> texture.toString().equals("hbm:items/registration_test_gui_icon")));
        } finally {
            clear(item);
        }
    }

    @Test
    void spaceRegistrationKeepsMissilePartRendererWithoutLegacyModel() throws Exception {
        Item item = new Item().setRegistryName("hbmspace", "registration_test_thruster");
        ItemRenderMissilePart renderer = new ItemRenderMissilePart(null);
        try {
            NTMClientRegistry.bindTeisr(item, renderer);
            Method method = com.hbmspace.main.ModEventHandlerClient.class.getDeclaredMethod("registerModel", Item.class, int.class);
            method.setAccessible(true);
            method.invoke(null, item, 0);
            Set<String> registered = variants().get(item.delegate);
            assertEquals(1, registered.size());
            assertTrue(registered.contains(NTMClientRegistry.getSyntheticTeisrModelLocation(item).toString()));
            assertSame(renderer, item.getTileEntityItemStackRenderer());
            assertFalse(renderer.createModelBinding(item).useBaseModelInGui());
        } finally {
            clear(item);
        }
    }

    @Test
    void egonUsesItsExistingPerspectiveWithoutLegacyModel() throws Exception {
        assertOwnedPerspective(new ItemRenderGunEgon(), "egon");
    }

    @Test
    void jShotgunUsesItsExistingPerspectiveWithoutLegacyModel() throws Exception {
        assertOwnedPerspective(new ItemRenderJShotgun(), "jshotgun");
    }

    private static void assertOwnedPerspective(TEISRBase renderer, String path) throws Exception {
        Item item = new Item().setRegistryName("hbm", "registration_test_" + path);
        try {
            Set<String> variants = register(item, renderer);
            assertEquals(1, variants.size());
            assertTrue(variants.contains(NTMClientRegistry.getSyntheticTeisrModelLocation(item).toString()));
            assertFalse(variants.contains(item.getRegistryName() + "#inventory"));
            assertTrue(renderer.useFMMPerspective(item));
            assertFalse(renderer.createModelBinding(item).useBaseModelInGui());
            assertSame(renderer, item.getTileEntityItemStackRenderer());
        } finally {
            clear(item);
        }
    }

    private static Set<String> register(Item item, TEISRBase renderer) throws Exception {
        NTMClientRegistry.bindTeisr(item, renderer);
        Method method = NTMClientRegistry.class.getDeclaredMethod("registerOwnedTeisrModelLocations");
        method.setAccessible(true);
        method.invoke(null);
        return variants().get(item.delegate);
    }

    @SuppressWarnings("unchecked")
    private static Map<Object, Set<String>> variants() throws Exception {
        Field field = ModelBakery.class.getDeclaredField("customVariantNames");
        field.setAccessible(true);
        return (Map<Object, Set<String>>) field.get(null);
    }

    private static void clear(Item item) throws Exception {
        ClaimedModelLocationRegistry.unregisterTeisrBinding(item);
        variants().remove(item.delegate);
    }
}
