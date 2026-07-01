package com.hbmspace.main;

import com.google.common.collect.ImmutableMap;
import com.hbm.blocks.ILookOverlay;
import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.items.ModItems;
import com.hbm.items.special.ItemAutogen;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.main.client.NTMClientRegistry;
import com.hbm.render.icon.RGBMutatorInterpolatedComponentRemap;
import com.hbm.render.icon.TextureAtlasSpriteMutatable;
import com.hbm.render.item.ItemRenderMissilePart;
import com.hbm.render.misc.MissilePart;
import com.hbm.saveddata.satellites.SatelliteSavedData;
import com.hbm.sound.AudioWrapper;
import com.hbm.util.Clock;
import com.hbm.util.I18nUtil;
import com.hbm.util.Vec3NT;
import com.hbmspace.Tags;
import com.hbmspace.blocks.ModBlocksSpace;
import com.hbmspace.blocks.generic.BlockOre;
import com.hbmspace.capability.HbmLivingPropsSpace;
import com.hbmspace.config.SpaceConfig;
import com.hbmspace.dim.CelestialBody;
import com.hbmspace.dim.SolarSystemWorldSavedData;
import com.hbmspace.dim.SkyProviderCelestial;
import com.hbmspace.dim.WorldProviderCelestial;
import com.hbmspace.dim.orbit.OrbitalStation;
import com.hbmspace.dim.orbit.WorldProviderOrbit;
import com.hbmspace.dim.trait.CBT_War;
import com.hbmspace.dim.trait.CelestialBodyTrait;
import com.hbmspace.entity.missile.EntityRideableRocket;
import com.hbmspace.inventory.materials.MatsSpace;
import com.hbmspace.items.IDynamicModelsSpace;
import com.hbmspace.items.ModItemsSpace;
import com.hbmspace.lib.HBMSpaceSoundHandler;
import com.hbmspace.particle.ParticleGlow;
import com.hbmspace.render.misc.RocketPart;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRedstoneOre;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.model.ModelRotation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.lwjgl.opengl.GLContext;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = Tags.MODID, value = Side.CLIENT)
public class ModEventHandlerClient {

    public static boolean renderLodeStar = false;
    public static long lastStarCheck = 0L;

    @SubscribeEvent
    public static void modelBaking(ModelBakeEvent evt) {
        IDynamicModelsSpace.bakeModels(evt);
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        for (Item item : ModItemsSpace.ALL_ITEMS) {
            try {
                registerModel(item, 0);
            } catch (NullPointerException e) {
                e.printStackTrace();
                SpaceMain.logger.info("Failed to register model for {}", item.getRegistryName());
            }
        }
        for (Block block : ModBlocksSpace.ALL_BLOCKS) {
            registerBlockModel(block, 0);
        }

        IDynamicModelsSpace.registerModels();
        IDynamicModelsSpace.registerCustomStateMappers();
        registerSpaceFluidCanisterModels();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void registerMissilePartModels(ModelRegistryEvent event) {
        RocketPart.registerClientParts();

        for (MissilePart part : RocketPart.parts.values()) {
            if (part.part != null) {
                NTMClientRegistry.bindTeisr(part.part, new ItemRenderMissilePart(part));

                ModelResourceLocation loc = NTMClientRegistry.getSyntheticTeisrModelLocation(part.part);
                ModelLoader.setCustomModelResourceLocation(part.part, 0, loc);
            }
        }
    }

    @SubscribeEvent
    public static void textureStitch(TextureStitchEvent.Pre evt) {
        TextureMap map = evt.getMap();
        IDynamicModelsSpace.registerSprites(map);
    }

    private static void registerModel(Item item, int meta) {
        if(!(item instanceof IDynamicModelsSpace dyn && dyn.INSTANCES.contains(item))) {
            ModelLoader.setCustomModelResourceLocation(item, meta, new ModelResourceLocation(item.getRegistryName(), "inventory"));
        }
    }

    private static void registerBlockModel(Block block, int meta) {
        if(block instanceof IDynamicModelsSpace dyn && IDynamicModelsSpace.INSTANCES.contains(dyn)) return;
        registerModel(Item.getItemFromBlock(block), meta);
    }

    private static void registerSpaceFluidCanisterModels() {
        ModelResourceLocation canisterModel = new ModelResourceLocation("hbm:canister_fuel", "inventory");
        ModelBakery.registerItemVariants(ModItems.canister_full, canisterModel);

        FluidType[] spaceCanisterFluids = {
                com.hbmspace.inventory.fluid.Fluids.ELBOWGREASE,
                com.hbmspace.inventory.fluid.Fluids.NMASSTETRANOL,
                com.hbmspace.inventory.fluid.Fluids.BLOODGAS
        };

        for(FluidType fluid : spaceCanisterFluids) {
            if(fluid != null) {
                ModelLoader.setCustomModelResourceLocation(ModItems.canister_full, fluid.getID(), canisterModel);
            }
        }
    }

    public static float lastFogDensity;

    @SubscribeEvent
    public static void onClientWorldLoad(WorldEvent.Load event) {
        if(!event.getWorld().isRemote) return;
        resetCelestialClientState();
    }

    @SubscribeEvent
    public static void onClientWorldUnload(WorldEvent.Unload event) {
        if(!event.getWorld().isRemote) return;
        resetCelestialClientState();
    }

    private static void resetCelestialClientState() {
        lastFogDensity = 0;
        SolarSystemWorldSavedData.updateClientTraits(null);
        SatelliteSavedData.setClientSats(new Int2ObjectOpenHashMap<>());
        OrbitalStation.orbitingStations.clear();
        WorldProviderCelestial.meteors.clear();
        SkyProviderCelestial.invalidateDisplayLists();
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void thickenFog(EntityViewRenderEvent.FogDensity event) {
        if (event.getEntity().world.provider instanceof WorldProviderCelestial provider) {
            lastFogDensity = provider.fogDensity(event);
            if (lastFogDensity > 0) {
                if (GLContext.getCapabilities().GL_NV_fog_distance) {
                    GlStateManager.glFogi(34138, 34139);
                }
                GlStateManager.setFog(GlStateManager.FogMode.EXP);
                event.setDensity(lastFogDensity);
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onOverlayRender(RenderGameOverlayEvent.Pre event) {
        if (event.getType() == RenderGameOverlayEvent.ElementType.CROSSHAIRS) {
            Minecraft mc = Minecraft.getMinecraft();
            World world = mc.world;
            RayTraceResult mop = mc.objectMouseOver;
            if(mop == null || mop.typeOfHit == null) return;
            if(mop.typeOfHit == RayTraceResult.Type.ENTITY) {
                Entity entity = mop.entityHit;

                if(entity instanceof ILookOverlay) {
                    ((ILookOverlay) entity).printHook(event, world, BlockPos.ORIGIN);
                }
            }
        }
    }

    @SubscribeEvent
    public static void drawTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        List<String> list = event.getToolTip();
        if(event.getEntityPlayer() == null) return;
        /// ORES ///
        if(SpaceConfig.showOreLocations) {
            Block block = Block.getBlockFromItem(stack.getItem());
            if(block instanceof net.minecraft.block.BlockOre || block instanceof BlockRedstoneOre) {
                BlockOre ore = BlockOre.vanillaMap.get(block);
                if(ore != null) {
                    ore.addInformation(stack, event.getEntityPlayer().getEntityWorld(), list, event.getFlags());
                } else if(block == Blocks.COAL_ORE || block == ModBlocks.ore_bedrock_oil) {
                    // we don't have any celestial coal, special case
                    list.add(TextFormatting.GOLD + "Can be found on:");
                    list.add(TextFormatting.AQUA + " - " + I18nUtil.resolveKey("body.kerbin"));
                }
            }
        }
    }
    @SubscribeEvent
    public static void onModelRegistry(ModelRegistryEvent event) {
        registerReplacedBlockItemModel(ModBlocks.ore_oil_empty);
    }

    @SubscribeEvent
    public static void onTextureStitch(TextureStitchEvent.Pre event) {
        TextureMap map = event.getMap();
        ParticleGlow.particleFlare = event.getMap().registerSprite(new ResourceLocation("hbm", "particle/yelflare"));
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if(mc.world == null || mc.world.provider == null) return;

        if(!mc.isGamePaused() && event.phase == TickEvent.Phase.END) {
            for(CelestialBody body : CelestialBody.getAllBodies()) {
                if(SolarSystemWorldSavedData.getClientTraits(body.name) != null) {
                    for(CelestialBodyTrait trait : SolarSystemWorldSavedData.getClientTraits(body.name).values()) {
                        trait.update(true, body);
                    }
                }
            }

            CBT_War war = CelestialBody.getTrait(mc.world, CBT_War.class);

            if(war != null) {
                for(int i = 0; i < war.getProjectiles().size(); i++) {
                    CBT_War.Projectile projectile = war.getProjectiles().get(i);
                    if(projectile != null && projectile.getTravel() >= 18 && projectile.getTravel() <= 18) {
                        Minecraft.getMinecraft().player.playSound(HBMSoundHandler.impact, 10F, 1F);
                    }
                }
            }
        }
        if(event.phase == TickEvent.Phase.START && mc.world.provider.getDimension() == SpaceConfig.orbitDimension) {
            for(Object o : mc.world.loadedEntityList) {
                if(o instanceof EntityItem item) {
                    item.motionX *= 0.81D; // applies twice on server it seems? 0.9 * 0.9
                    item.motionY = 0.03999999910593033D;
                    item.motionZ *= 0.81D;
                }
            }
        }
    }

    private static AudioWrapper shipHum;

    @SideOnly(Side.CLIENT)
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onClientTickLast(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        World world = mc.world;
        long millis = Clock.get_ms();
        if(millis == 0) millis = System.currentTimeMillis();

        if(event.phase == TickEvent.Phase.START) {

            if(world == null) return;
            if(lastStarCheck + 200 < millis) {
                renderLodeStar = false;
                lastStarCheck = millis;

                if(player != null) {
                    Vec3NT pos = new Vec3NT(player.posX, player.posY, player.posZ);
                    Vec3NT lodestarHeading = new Vec3NT(0, 0, -1D).rotateAroundXDeg(-15).multiply(25);
                    Vec3NT nextPos = new Vec3NT(pos).add(lodestarHeading.x, lodestarHeading.y, lodestarHeading.z);
                    RayTraceResult mop = world.rayTraceBlocks(pos, nextPos, false, true, false);
                    if(mop != null && mop.typeOfHit == RayTraceResult.Type.BLOCK && world.getBlockState(mop.getBlockPos()).getBlock() == ModBlocks.glass_polarized) {
                        renderLodeStar = true;
                    }
                }
            }

            if (player != null && world.provider instanceof WorldProviderOrbit && HbmLivingPropsSpace.hasGravity(player)) {
                if (shipHum == null || !shipHum.isPlaying()) {
                    shipHum = SpaceMain.proxy.getLoopedSound(HBMSpaceSoundHandler.stationHum, SoundCategory.BLOCKS, player, 0.1f /* ClientConfig.AUDIO_SHIP_HUM_VOLUME.get() */, 5.0F, 1.0F, 10);
                    shipHum.startSound();
                }
                // TODO
                shipHum.updateVolume(0.1f /* the same config */);
                shipHum.keepAlive();
            } else if (shipHum != null) {
                shipHum.stopSound();
                shipHum = null;
            }
        }
    }

    private static boolean wasRiding;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderRidingPlayerPre(RenderPlayerEvent.Pre event) {
        wasRiding = event.getEntityPlayer().getRidingEntity() instanceof EntityRideableRocket;
        if(!wasRiding) return;

        GlStateManager.pushMatrix();

        GlStateManager.rotate(-event.getEntityPlayer().getRidingEntity().rotationPitch, 0, 0, 1);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderRidingPlayerPost(RenderPlayerEvent.Post event) {
        if(!wasRiding) return;

        GlStateManager.popMatrix();
    }

    @SubscribeEvent
    public static void onRenderTickPre(TickEvent.RenderTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;

        if (player != null && player.getRidingEntity() instanceof EntityRideableRocket) {
            mc.entityRenderer.thirdPersonDistance = 12.0F;
        } else {
            mc.entityRenderer.thirdPersonDistance = 4.0F;
        }
    }

    private static void registerReplacedBlockItemModel(Block block) {
        Item item = Item.getItemFromBlock(block);
        if (item == null || item.getRegistryName() == null) {
            return;
        }

        ModelLoader.setCustomModelResourceLocation(
                item,
                0,
                new ModelResourceLocation(block.getRegistryName(), "inventory")
        );
    }

}
