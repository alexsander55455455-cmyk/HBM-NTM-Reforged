package com.hbm.render.weapon;

import com.hbm.items.ClaimedModelLocationRegistry;
import com.hbm.main.client.NTMClientRegistry;
import com.hbm.render.item.ItemRenderBoltgun;
import com.hbm.render.item.FancyMissingModelPerspective;
import com.hbm.render.item.ItemRenderMissilePart;
import com.hbm.render.item.TEISRBase;
import com.hbm.render.item.weapon.ItemRenderGunEgon;
import com.hbm.render.item.weapon.ItemRenderJShotgun;
import com.hbm.render.item.weapon.ItemRenderWeaponBolter;
import com.hbm.render.item.weapon.sedna.ItemRenderMinigunDual;
import com.hbm.testutil.ForgeTestBootstrap;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.BuiltInModel;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.Item;
import net.minecraft.util.registry.IRegistry;
import net.minecraft.util.registry.RegistrySimple;
import net.minecraftforge.common.model.TRSRTransformation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import javax.vecmath.Matrix4f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

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
    void boltgunUsesExtendedPerspectiveOnEveryModelBake() throws Exception {
        Item item = new Item().setRegistryName("hbm", "registration_test_boltgun");
        ItemRenderBoltgun renderer = new ItemRenderBoltgun();
        try {
            Set<String> variants = register(item, renderer);
            assertEquals(1, variants.size());
            assertFalse(variants.contains(item.getRegistryName() + "#inventory"));
            assertSame(renderer, item.getTileEntityItemStackRenderer());
            ClaimedModelLocationRegistry.ITeisrBinding binding = ClaimedModelLocationRegistry.getTeisrBinding(item);
            IRegistry<ModelResourceLocation, IBakedModel> models = new RegistrySimple<>();
            models.putObject(binding.getModelLocation(), new BuiltInModel(ItemCameraTransforms.DEFAULT, ItemOverrideList.NONE));
            models.putObject(new ModelResourceLocation(item.getRegistryName(), "inventory"), new BuiltInModel(ItemCameraTransforms.DEFAULT, ItemOverrideList.NONE));
            Method wrap = NTMClientRegistry.class.getDeclaredMethod("wrapTeisrBinding", binding.getClass(), IRegistry.class);
            wrap.setAccessible(true);
            for (int bake = 0; bake < 2; bake++) {
                wrap.invoke(null, binding, models);
                IBakedModel model = models.getObject(binding.getModelLocation());
                assertTrue(model instanceof FancyMissingModelPerspective);
                assertPerspective(model, ItemCameraTransforms.TransformType.FIRST_PERSON_RIGHT_HAND,
                        new Vector3f(-0.5f, 0.5f, -0.5f), new Quat4f(1, 1, 1, 1));
                assertPerspective(model, ItemCameraTransforms.TransformType.FIRST_PERSON_LEFT_HAND,
                        new Vector3f(-0.62f, 0.5f, -0.5f), new Quat4f(1, -1, -1, 1));
                assertPerspective(model, ItemCameraTransforms.TransformType.GUI, null, new Quat4f(1, 1, 1, 1));
            }
        } finally {
            clear(item);
        }
    }

    private static void assertPerspective(IBakedModel model, ItemCameraTransforms.TransformType type,
                                          Vector3f position, Quat4f rotation) {
        Matrix4f expected = new TRSRTransformation(position, rotation, null, null).getMatrix();
        assertTrue(expected.epsilonEquals(model.handlePerspective(type).getRight(), 0.00001f));
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

    @Test
    void bolterUsesItsGuiRendererWithoutLoadingUvSheet() throws Exception {
        Item item = new Item().setRegistryName("hbm", "gun_bolter");
        ItemRenderWeaponBolter renderer = new ItemRenderWeaponBolter();
        try {
            assertEquals(1, register(item, renderer).size());
            assertFalse(renderer.createModelBinding(item).useBaseModelInGui());
            assertFalse(renderer.loadModel(item, NTMClientRegistry.getSyntheticTeisrModelLocation(item))
                    .getTextures().stream().anyMatch(texture -> texture.toString().equals("hbm:items/gun_bolter")));
            assertSame(renderer, item.getTileEntityItemStackRenderer());
        } finally {
            clear(item);
        }
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
