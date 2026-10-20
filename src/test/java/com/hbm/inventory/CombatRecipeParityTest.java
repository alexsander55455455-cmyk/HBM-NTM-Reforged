package com.hbm.inventory;

import com.hbm.blocks.ModBlocks;
import com.hbm.config.GeneralConfig;
import com.hbm.crafting.ArmorRecipes;
import com.hbm.crafting.WeaponRecipes;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.PrecAssRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.items.ItemEnums.EnumCircuitType;
import com.hbm.items.ItemEnums.EnumLegendaryType;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemBlueprints;
import com.hbm.items.weapon.grenade.ItemGrenadeFilling.EnumGrenadeFilling;
import com.hbm.items.weapon.sedna.factory.GunFactory;
import com.hbm.main.CraftingManager;
import com.hbm.tileentity.machine.TileEntityMachinePrecAss;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.OreDictionary;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import static com.hbm.inventory.OreDictManager.*;
import static org.junit.jupiter.api.Assertions.*;

// Each 528/expensive combination runs in its own Java 8 process.
class CombatRecipeParityTest {
    static final String POOL = "discover..rpa";
    static final String[] NAMES = {"precass.rpahelmet", "precass.rpaplate", "precass.rpalegs", "precass.rpaboots"};
    static Item[] armor() { return new Item[]{ModItems.rpa_helmet, ModItems.rpa_plate, ModItems.rpa_legs, ModItems.rpa_boots}; }

    @BeforeAll static void bootstrap() throws Exception {
        DataConsumerRecipeTest.bootstrap();
        GunFactory.init();
        for(Item item : ModItems.ALL_ITEMS) if(!ForgeRegistries.ITEMS.containsKey(item.getRegistryName())) ForgeRegistries.ITEMS.register(item);
        GeneralConfig.enable528 = Boolean.getBoolean("combatrecipes.mode528");
        GeneralConfig.enableExpensiveMode = Boolean.getBoolean("combatrecipes.expensive");
        CraftingManager.addCrafting(); ArmorRecipes.register(); WeaponRecipes.register();
        PrecAssRecipes.INSTANCE.registerDefaults();
    }
    static GenericRecipe rpa(int i) { GenericRecipe r=PrecAssRecipes.INSTANCE.recipeNameMap.get(NAMES[i]); assertNotNull(r,NAMES[i]); return r; }
    static ItemStack ore(String key) { assertFalse(OreDictionary.getOres(key).isEmpty(),key); return OreDictionary.getOres(key).get(0).copy(); }
    static IRecipe crafting(ItemStack output) {
        List<IRecipe> matches=new ArrayList<>();
        for(IRecipe r:ForgeRegistries.RECIPES.getValuesCollection()) if(ItemStack.areItemsEqual(r.getRecipeOutput(),output)) matches.add(r);
        assertEquals(1,matches.size(),output.toString()); return matches.get(0);
    }
    static InventoryCrafting grid(ItemStack... slots) {
        InventoryCrafting grid=new InventoryCrafting(new Container(){@Override public boolean canInteractWith(EntityPlayer p){return true;}},3,3);
        for(int i=0;i<slots.length;i++) grid.setInventorySlotContents(i,slots[i]); return grid;
    }
    static void crafts(IRecipe r,InventoryCrafting grid) {
        assertTrue(r.matches(grid,null)); assertTrue(ItemStack.areItemStacksEqual(r.getRecipeOutput(),r.getCraftingResult(grid)));
        for(int i=0;i<9;i++) if(!grid.getStackInSlot(i).isEmpty()) {
            ItemStack saved=grid.getStackInSlot(i);grid.setInventorySlotContents(i,new ItemStack(Items.DIAMOND));
            assertFalse(r.matches(grid,null),"Wrong slot "+i);grid.setInventorySlotContents(i,saved);
        }
    }
    @Test void antimatterBarrelUsesTwoDenseBsccoWires() {
        ItemStack p=ore(BIGMT.plate()),w=ore(BSCCO.wireDense());
        crafts(crafting(new ItemStack(ModBlocks.barrel_antimatter)),grid(p,w,p,p,ItemStack.EMPTY,p,p,w,p));
    }
    @Test void bayonetUsesRegularSteelAndThreeBolts() {
        crafts(crafting(new ItemStack(ModItems.weapon_mod_special,1,GunFactory.EnumModSpecial.BAYONET.ordinal())),
                grid(ItemStack.EMPTY,ItemStack.EMPTY,ore(STEEL.plate()),ore(STEEL.bolt()),ore(STEEL.bolt()),ore(STEEL.bolt())));
    }
    @Test void nuclearDetCordUsesFourReflectors() {
        ItemStack p=new ItemStack(ModItems.neutron_reflector),f=new ItemStack(ModItems.circuit,1,EnumCircuitType.CONTROLLER.ordinal()),d=new ItemStack(ModBlocks.det_charge);
        crafts(crafting(new ItemStack(ModBlocks.det_nuke)),grid(p,f,p,d,new ItemStack(ModItems.man_core),d,p,f,p));
    }
    @Test void nuclearDemoUsesReflectorsAndRegularNuclearKeepsWeaponSteel() {
        ItemStack t=new ItemStack(ModItems.ball_tatb),p=ore(PU239.nugget()),c=new ItemStack(ModItems.neutron_reflector);
        crafts(crafting(new ItemStack(ModItems.grenade_filling,1,EnumGrenadeFilling.NUCLEAR_DEMO.ordinal())),grid(t,p,t,c,p,c,t,p,t));
        ItemStack steel=ore(WEAPONSTEEL.plate());
        crafts(crafting(new ItemStack(ModItems.grenade_filling,1,EnumGrenadeFilling.NUCLEAR.ordinal())),grid(ItemStack.EMPTY,t,ItemStack.EMPTY,steel,p,steel,ItemStack.EMPTY,t,ItemStack.EMPTY));
    }
    @Test void precisionRpaHasExactTimingOutputsPoolAndNoFluids() {
        for(int i=0;i<4;i++) {
            GenericRecipe r=rpa(i);assertEquals(3600,r.duration);assertEquals(25000,r.power);
            assertArrayEquals(new String[]{POOL},r.getPools());assertNull(r.inputFluid);assertNull(r.outputFluid);
            assertTrue(ItemStack.areItemStacksEqual(new ItemStack(armor()[i]),r.outputItem[0].getSingle()));
            assertTrue(r.inputItem.length<=9);for(AStack a:r.inputItem) assertFalse(a.getStackList().isEmpty());
        }
    }
    @Test void precisionRpaCostsAndChestLegendaryTierAreExact() {
        int[][] counts={{12,4,4,8},{24,8,8,8,16,1},{24,8,8,16},{12,4,8}};
        for(int i=0;i<4;i++) {GenericRecipe r=rpa(i);assertEquals(counts[i].length,r.inputItem.length);for(int j=0;j<counts[i].length;j++)assertEquals(counts[i][j],r.inputItem[j].stacksize);assertTrue(r.inputItem[0].matchesRecipe(new ItemStack(ModItems.plate_armor_ajr,counts[i][0]),false));}
        assertTrue(rpa(1).inputItem[5].matchesRecipe(new ItemStack(ModItems.parts_legendary,1,EnumLegendaryType.TIER2.ordinal()),false));
        assertFalse(rpa(1).inputItem[5].matchesRecipe(new ItemStack(ModItems.parts_legendary,1,EnumLegendaryType.TIER1.ordinal()),false));
    }
    @Test void discoverableBlueprintRoundtripSelectsAllFourRecipes() {
        assertEquals(Arrays.asList(NAMES),GenericRecipes.blueprintPools.get(POOL));
        ItemStack blueprint=new ItemStack(ItemBlueprints.make(POOL).serializeNBT());assertEquals(POOL,ItemBlueprints.grabPool(blueprint));
        for(int i=0;i<4;i++)assertSame(rpa(i),GenericRecipes.pooledBlueprints.get(NAMES[i]));
    }
    static TileEntityMachinePrecAss machine(GenericRecipe r) {
        TileEntityMachinePrecAss te=new TileEntityMachinePrecAss();te.power=100_000_000;
        for(int i=0;i<r.inputItem.length;i++){ItemStack s=r.inputItem[i].getStackList().get(0).copy();s.setCount(r.inputItem[i].stacksize);te.inventory.setStackInSlot(4+i,s);}
        te.assemblerModule.setRecipe(r.getInternalName(),false);return te;
    }
    @Test void realMachineModuleConsumesExactInputsPowerAndProducesArmor() {
        for(int i=0;i<4;i++) {
            GenericRecipe r=rpa(i);TileEntityMachinePrecAss te=machine(r);
            for(int tick=0;tick<3601&&te.inventory.getStackInSlot(13).isEmpty();tick++)te.assemblerModule.update(1,1,true,ItemBlueprints.make(POOL));
            assertTrue(ItemStack.areItemStacksEqual(new ItemStack(armor()[i]),te.inventory.getStackInSlot(13)));
            for(int slot=4;slot<13;slot++)assertTrue(te.inventory.getStackInSlot(slot).isEmpty());
            assertTrue(te.power==10_000_000||te.power==9_975_000); // Floating point progress may complete on tick 3601.
        }
    }
    @Test void machineRejectsMissingBlueprintInsufficientPowerAndWrongLegendaryTier() {
        GenericRecipe r=rpa(1);TileEntityMachinePrecAss te=machine(r);te.assemblerModule.update(3600,1,true,ItemStack.EMPTY);assertEquals("null",te.assemblerModule.recipe);assertEquals(100_000_000,te.power);
        te=machine(r);te.power=24999;te.assemblerModule.update(3600,1,true,ItemBlueprints.make(POOL));assertTrue(te.inventory.getStackInSlot(13).isEmpty());assertEquals(24999,te.power);
        te=machine(r);te.inventory.setStackInSlot(9,new ItemStack(ModItems.parts_legendary,1,EnumLegendaryType.TIER1.ordinal()));te.assemblerModule.update(3600,1,true,ItemBlueprints.make(POOL));assertTrue(te.inventory.getStackInSlot(13).isEmpty());
    }
    @Test void legacyArmorCraftsAndFullCatalogArePreserved() throws Exception {
        for(Item a:armor())assertNotNull(crafting(new ItemStack(a)));
        String path=System.getProperty("combatrecipes.catalog");if(path==null)return;
        List<String> rows=new ArrayList<>();for(IRecipe r:ForgeRegistries.RECIPES.getValuesCollection()){
            StringBuilder s=new StringBuilder(r.getRegistryName()+"|"+r.getClass().getName()+"|"+r.getRecipeOutput().serializeNBT());
            r.getIngredients().forEach(a->{s.append('|');for(ItemStack v:a.getMatchingStacks())s.append(v.serializeNBT()).append(';');});rows.add(s.toString());
        }Files.write(Paths.get(path),rows,StandardCharsets.UTF_8);
    }
}
