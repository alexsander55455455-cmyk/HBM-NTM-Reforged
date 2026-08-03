package com.hbm.hazard;

import com.hbm.blocks.ModBlocks;
import com.hbm.config.GeneralConfig;
import com.hbm.hazard.modifier.HazardModifierFuelRadiation;
import com.hbm.hazard.modifier.HazardModifierRBMKHot;
import com.hbm.hazard.modifier.HazardModifierRBMKRadiation;
import com.hbm.hazard.modifier.HazardModifierRTGRadiation;
import com.hbm.hazard.transformer.HazardTransformerForgeFluid;
import com.hbm.hazard.transformer.HazardTransformerPostCustom;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import com.hbm.hazard.transformer.HazardTransformerRadiationContainer;
import com.hbm.hazard.transformer.HazardTransformerRadiationME;
import com.hbm.hazard.transformer.HazardTransformerRadiationNBT;
import com.hbm.hazard.type.*;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.material.MaterialShapes;

import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemBreedingRod;
import com.hbm.items.machine.ItemPileRodMK2.EnumPileRod;
import com.hbm.items.machine.ItemWatzPellet;
import com.hbm.items.machine.ItemZirnoxRod;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import static com.hbm.blocks.ModBlocks.*;
import static com.hbm.inventory.OreDictManager.*;
import static com.hbm.items.ModItems.*;
import static com.hbm.items.machine.ItemPWRFuel.EnumPWRFuel;
import static com.hbm.items.machine.ItemZirnoxRodDepleted.EnumZirnoxTypeDepleted;

@SuppressWarnings("unused") //shut the fuck up
public class HazardRegistry {

	//CO60		             5a		β−	030.00Rad/s	Spicy
	//SR90		            29a		β−	015.00Rad/s Spicy
	//TC99		       211,000a		β−	002.75Rad/s	Spicy
	//I181		           192h		β−	150.00Rad/s	2 much spice :(
	//XE135		             9h		β−	aaaaaaaaaaaaaaaa
	//CS137		            30a		β−	020.00Rad/s	Spicy
	//AU198		            64h		β−	500.00Rad/s	2 much spice :(
	//PB209		             3h		β−	10,000.00Rad/s mama mia my face is melting off
	//AT209		             5h		β+	like 7.5k or sth idk bruv`
	//PO210		           138d		α	075.00Rad/s	Spicy
	//RA226		         1,600a		α	007.50Rad/s
	//AC227		            22a		β−	030.00Rad/s Spicy
	//TH232		14,000,000,000a		α	000.10Rad/s
	//U233		       160,000a		α	005.00Rad/s
	//U235		   700,000,000a		α	001.00Rad/s
	//U238		 4,500,000,000a		α	000.25Rad/s
	//NP237		     2,100,000a		α	002.50Rad/s
	//PU238		            88a		α	010.00Rad/s	Spicy
	//PU239		        24,000a		α	005.00Rad/s
	//PU240		         6,600a		α	007.50Rad/s
	//PU241		            14a		β−	025.00Rad/s	Spicy
	//AM241		           432a		α	008.50Rad/s
	//AM242		           141a		β−	009.50Rad/s

	//simplified groups for ReC compat
	public static final float gen_S = 10_000F;
	public static final float gen_H = 2_000F;
	public static final float gen_10D = 100F;
	public static final float gen_100D = 80F;
	public static final float gen_1Y = 50F;
	public static final float gen_10Y = 30F;
	public static final float gen_100Y = 10F;
	public static final float gen_1K = 7.5F;
	public static final float gen_10K = 6.25F;
	public static final float gen_100K = 5F;
	public static final float gen_1M = 2.5F;
	public static final float gen_10M = 1.5F;
	public static final float gen_100M = 1F;
	public static final float gen_1B = 0.5F;
	public static final float gen_10B = 0.1F;

	public static final float co60 = 30.0F;
	public static final float sr90 = 15.0F;
	public static final float tc99 = 2.75F;
	public static final float i131 = 150.0F;
	public static final float xe135 = 1250.0F;
	public static final float cs137 = 20.0F;
	public static final float au198 = 500.0F;
	public static final float pb209 = 10000.0F;
	public static final float at209 = 7500.0F;
	public static final float po210 = 75.0F;
	public static final float ra226 = 7.5F;
	public static final float ac227 = 30.0F;
	public static final float th232 = 0.1F;
	public static final float thf = 1.75F;
	public static final float u = 0.35F;
	public static final float u233 = 5.0F;
	public static final float u235 = 1.0F;
	public static final float u238 = 0.25F;
	public static final float uf = 0.5F;
	public static final float uzh = 0.125F;
	public static final float np237 = 2.5F;
	public static final float npf = 1.5F;
	public static final float pu = 7.5F;
	public static final float purg = 6.25F;
	public static final float pu238 = 10.0F;
	public static final float pu239 = 5.0F;
	public static final float pu240 = 7.5F;
	public static final float pu241 = 25.0F;
	public static final float puf = 4.25F;
	public static final float am241 = 8.5F;
	public static final float am242 = 9.5F;
	public static final float amrg = 9.0F;
	public static final float amf = 4.75F;
	public static final float mox = 2.5F;
	public static final float sa326 = 15.0F;
	public static final float sa327 = 17.5F;
	public static final float saf = 5.85F;
	public static final float sas3 = 5F;
	public static final float gh336 = 5.0F;
	public static final float radspice = 20_000.0F;
	public static final float unof = 10_000.0F;
	public static final float ts = 120.0F;
	public static final float mud = 15.0F;
	public static final float radsource_mult = 3.0F;
	public static final float pobe = po210 * radsource_mult;
	public static final float rabe = ra226 * radsource_mult;
	public static final float pube = pu238 * radsource_mult;
	public static final float zfb_bi = u235 * 0.35F;
	public static final float zfb_pu241 = pu241 * 0.5F;
	public static final float zfb_am_mix = amrg * 0.5F;
	public static final float bf = 300_000.0F;
	public static final float bfb = 500_000.0F;

	public static final float sr = sa326 * 0.1F;
	public static final float sb = sa326 * 0.2F;
	public static final float trx = 25.0F;
	public static final float trn = 0.1F;
	public static final float wst = 450F;
	public static final float wstv = 150F;
	public static final float yc = u * 1.2F;
	public static final float fo = 10F;

	public static final float nugget = 0.1F;
	public static final float ingot = 1.0F;
	public static final float gem = 1.0F;
	public static final float plate = ingot;
	public static final float plateCast = plate * 3;
	public static final float powder_mult = 3.0F;
	public static final float powder = ingot * powder_mult;
	public static final float powder_tiny = nugget * powder_mult;
	public static final float ore = ingot * 0.05F;
	public static final float block = 10.0F;
	public static final float crystal = block;
	public static final float billet = 0.5F;
	public static final float rtg = billet * 3;
	public static final float rod = 0.5F;
	public static final float rod_dual = rod * 2;
	public static final float rod_quad = rod * 4;
	public static final float rod_rbmk = rod * 8;
	public static final float magt = nugget * 0.5F * sa326 * ingot;
	public static final float tcalloy = 0.07F * ingot;
	public static final float ferro = u238 * 0.7F * ingot;
	public static final float wire = ingot * 0.1F;

	public static final IHazardType RADIATION = new HazardTypeRadiation();
	public static final IHazardType CONTAMINATING = new HazardTypeContaminating();
	public static final IHazardType DIGAMMA = new HazardTypeDigamma();
	public static final IHazardType HOT = new HazardTypeHot();
	public static final IHazardType BLINDING = new HazardTypeBlinding();
	public static final IHazardType ASBESTOS = new HazardTypeAsbestos();
	public static final IHazardType COAL = new HazardTypeCoal();
	public static final IHazardType HYDROACTIVE = new HazardTypeHydroactive();
	public static final IHazardType EXPLOSIVE = new HazardTypeExplosive();
	public static final IHazardType TOXIC = new HazardTypeToxic();
	public static final IHazardType COLD = new HazardTypeCold();

	public static void registerItems() {

		HazardSystem.register(Items.GUNPOWDER, makeData(EXPLOSIVE, 1F));
		HazardSystem.register(Blocks.TNT, makeData(EXPLOSIVE, 4F));
		HazardSystem.register(Items.PUMPKIN_PIE, makeData(EXPLOSIVE, 1F));

		HazardSystem.register(ball_dynamite, makeData(EXPLOSIVE, 2F));
		HazardSystem.register(stick_dynamite, makeData(EXPLOSIVE, 1F));
		HazardSystem.register(stick_tnt, makeData(EXPLOSIVE, 1.5F));
		HazardSystem.register(stick_semtex, makeData(EXPLOSIVE, 2.5F));
		HazardSystem.register(stick_c4, makeData(EXPLOSIVE, 2.5F));

		HazardSystem.register(cordite, makeData(EXPLOSIVE, 2F));
		HazardSystem.register(ballistite, makeData(EXPLOSIVE, 1F));

		HazardSystem.register("dustCoal", makeData(COAL, powder));
		HazardSystem.register("dustTinyCoal", makeData(COAL, powder_tiny));
		HazardSystem.register("dustLignite", makeData(COAL, powder));
		HazardSystem.register("dustTinyLignite", makeData(COAL, powder_tiny));

		HazardSystem.register(insert_polonium, makeData(RADIATION, 100F));

		HazardSystem.register(demon_core_open, makeData(RADIATION, 5F).addEntry(new HazardTypeDangerousDrop((entityItem, level) ->{
            if(!entityItem.world.isRemote && entityItem.onGround) {
                entityItem.setItem(new ItemStack(ModItems.demon_core_closed));
                entityItem.world.spawnEntity(new EntityItem(entityItem.world, entityItem.posX, entityItem.posY, entityItem.posZ, new ItemStack(ModItems.screwdriver)));
            }
        })));
		HazardSystem.register(demon_core_closed, makeData(RADIATION, 10_000_000F));
		HazardSystem.register(lamp_demon, makeData(RADIATION, 10_000_000F));

		HazardSystem.register(new ItemStack(cell, 1, Fluids.TRITIUM.getID()), makeData(RADIATION, 0.001F));
		HazardSystem.register(new ItemStack(cell, 1, Fluids.SAS3.getID()), makeData().addEntry(RADIATION, sas3).addEntry(BLINDING, 60F));
        HazardSystem.register(cell_balefire, makeData(RADIATION, 50F));
        HazardSystem.register(powder_balefire, makeData(RADIATION, bf * powder).addEntry(HOT, 6F).addEntry(CONTAMINATING, 500F));
        HazardSystem.register(egg_balefire_shard, makeData(RADIATION, bf * nugget));
        HazardSystem.register(egg_balefire, makeData(RADIATION, bf * ingot));

		HazardSystem.register(powder_ice, makeData(COLD, 5F));
		HazardSystem.register(thermo_unit_endo, makeData(COLD, 2F));
		HazardSystem.register(thermo_unit_exo, makeData(HOT, 10F));

		HazardSystem.register(solid_fuel_bf, makeData(RADIATION, 1000)); //roughly the amount of the balefire shard diluted in 250mB of rocket fuel
		HazardSystem.register(solid_fuel_presto_bf, makeData(RADIATION, 2000));
		HazardSystem.register(solid_fuel_presto_triplet_bf, makeData(RADIATION, 6000));

		HazardSystem.register(nuclear_waste_long, makeData(RADIATION, 50F).addEntry(CONTAMINATING, 7F));
		HazardSystem.register(nuclear_waste_long_tiny, makeData(RADIATION, 5F).addEntry(CONTAMINATING, 2F));
		HazardSystem.register(nuclear_waste_short, makeData(RADIATION, 300F).addEntry(HOT, 5F).addEntry(CONTAMINATING, 17F));
		HazardSystem.register(nuclear_waste_short_tiny, makeData(RADIATION, 30F).addEntry(HOT, 5F).addEntry(CONTAMINATING, 5F));
		HazardSystem.register(nuclear_waste_long_depleted, makeData(RADIATION, 5F).addEntry(CONTAMINATING, 4F));
		HazardSystem.register(nuclear_waste_long_depleted_tiny, makeData(RADIATION, 0.5F).addEntry(CONTAMINATING, 2F));
		HazardSystem.register(nuclear_waste_short_depleted, makeData(RADIATION, 30F).addEntry(CONTAMINATING, 5F));
		HazardSystem.register(nuclear_waste_short_depleted_tiny, makeData(RADIATION, 3F).addEntry(CONTAMINATING, 2F));

		HazardSystem.register(scrap_nuclear, makeData(RADIATION, 1F));
		HazardSystem.register(trinitite, makeData(RADIATION, trn * ingot).addEntry(CONTAMINATING, 5));
		HazardSystem.register(block_trinitite, makeData(RADIATION, trn * block));
		HazardSystem.register(nuclear_waste, makeData(RADIATION, wst * powder).addEntry(CONTAMINATING, 21F));
		HazardSystem.register(yellow_barrel, makeData(RADIATION, wst * ingot * 10));
		HazardSystem.register(billet_nuclear_waste, makeData(RADIATION, wst * billet).addEntry(CONTAMINATING, wst * billet));
		HazardSystem.register(nuclear_waste_tiny, makeData(RADIATION, wst * powder_tiny).addEntry(CONTAMINATING, 7F));
		HazardSystem.register(nuclear_waste_vitrified, makeData(RADIATION, wstv * powder).addEntry(CONTAMINATING, 15F));
		HazardSystem.register(nuclear_waste_vitrified_tiny, makeData(RADIATION, wstv * powder_tiny).addEntry(CONTAMINATING, 5F));
		HazardSystem.register(block_waste, makeData(RADIATION, wst * block));
		HazardSystem.register(block_waste_painted, makeData(RADIATION, wst * block));
		HazardSystem.register(block_waste_vitrified, makeData(RADIATION, wstv * block));
		HazardSystem.register(ancient_scrap, makeData(RADIATION, 150F));
		HazardSystem.register(block_corium, makeData(RADIATION, 10000F));
		HazardSystem.register(block_corium_cobble, makeData(RADIATION, 1000F));

		HazardSystem.register(new ItemStack(sellafield, 1, 0), makeData(RADIATION, 5F));
		HazardSystem.register(new ItemStack(sellafield, 1, 1), makeData(RADIATION, 10F));
		HazardSystem.register(new ItemStack(sellafield, 1, 2), makeData(RADIATION, 20F));
		HazardSystem.register(new ItemStack(sellafield, 1, 3), makeData(RADIATION, 40F).addEntry(HOT, 5F));
		HazardSystem.register(new ItemStack(sellafield, 1, 4), makeData(RADIATION, 80F).addEntry(HOT, 5F));
		HazardSystem.register(new ItemStack(sellafield, 1, 5), makeData(RADIATION, 500F).addEntry(HOT, 5F));

		HazardSystem.register(baleonitite_slaked, makeData(RADIATION, 25F));
		HazardSystem.register(baleonitite_0, makeData(RADIATION, 50F));
		HazardSystem.register(baleonitite_1, makeData(RADIATION, 100F));
		HazardSystem.register(baleonitite_2, makeData(RADIATION, 200F));
		HazardSystem.register(baleonitite_3, makeData(RADIATION, 400F).addEntry(HOT, 5F));
		HazardSystem.register(baleonitite_4, makeData(RADIATION, 800F).addEntry(HOT, 5F));
		HazardSystem.register(baleonitite_core, makeData(RADIATION, 5000F).addEntry(HOT, 5F));

		HazardSystem.register(new ItemStack(ModBlocks.ore_sellafield_radgem), makeData(RADIATION, 25F));
		HazardSystem.register(new ItemStack(ModItems.gem_rad), makeData(RADIATION, 25F));

		HazardSystem.register(sellafield_slaked, makeData(RADIATION, 2.5F));
		HazardSystem.register(sellafield_bedrock, makeData(RADIATION, 2.5F));
		HazardSystem.register(sellafield_0, makeData(RADIATION, 5F));
		HazardSystem.register(sellafield_1, makeData(RADIATION, 10F));
		HazardSystem.register(sellafield_2, makeData(RADIATION, 20F));
		HazardSystem.register(sellafield_3, makeData(RADIATION, 40F).addEntry(HOT, 5F));
		HazardSystem.register(sellafield_4, makeData(RADIATION, 80F).addEntry(HOT, 5F));
		HazardSystem.register(sellafield_core, makeData(RADIATION, 500F).addEntry(HOT, 5F));

		HazardSystem.register(glowing_stew, makeData(RADIATION, 2F));
		HazardSystem.register(mush, makeData(RADIATION, 1F));
		HazardSystem.register(mush_block, makeData(RADIATION, 4F));
		HazardSystem.register(mush_block_stem, makeData(RADIATION, 4F));
		HazardSystem.register(waste_leaves, makeData(RADIATION, 0.15F));

		HazardSystem.register(waste_ice, makeData(RADIATION, 20F));

		for (int i = 0; i < 7; i++) {
			HazardSystem.register(new ItemStack(waste_mycelium, 1, i), makeData(RADIATION, (i + 1) * 7.5F));
			HazardSystem.register(new ItemStack(waste_earth, 1, i), makeData(RADIATION, (i + 1) * 4F));
			HazardSystem.register(new ItemStack(waste_dirt, 1, i), makeData(RADIATION, (i + 1) * 1F));
			HazardSystem.register(new ItemStack(waste_gravel, 1, i), makeData(RADIATION, (i + 1) * 2.5F));
			HazardSystem.register(new ItemStack(waste_sandstone, 1, i), makeData(RADIATION, (i + 1) * 2.5F));
			HazardSystem.register(new ItemStack(waste_sand, 1, i), makeData(RADIATION, (i + 1) * 5F));
			HazardSystem.register(new ItemStack(waste_grass_tall, 1, i), makeData(RADIATION, (i + 1) * 2.5F));
			HazardSystem.register(new ItemStack(waste_sandstone_red, 1, i), makeData(RADIATION, (i + 1) * 2.5F));
			HazardSystem.register(new ItemStack(waste_sand_red, 1, i), makeData(RADIATION, (i + 1) * 5F));
			HazardSystem.register(new ItemStack(waste_terracotta, 1, i), makeData(RADIATION, (i + 1) * 2.5F));
			HazardSystem.register(new ItemStack(waste_trinitite, 1, i), makeData(RADIATION, (i + 1) * 10F));
			HazardSystem.register(new ItemStack(waste_trinitite_red, 1, i), makeData(RADIATION, (i + 1) * 10F));
			HazardSystem.register(new ItemStack(waste_snow, 1, i), makeData(RADIATION, (i + 1) * 0.15F));
			HazardSystem.register(new ItemStack(waste_snow_block, 1, i), makeData(RADIATION, (i + 1) * 1.5F));
		}

		HazardSystem.register(block_tritium, makeData(RADIATION, 4.5F));
		HazardSystem.register(brick_jungle_ooze, makeData(RADIATION, 10F));
		HazardSystem.register(sand_lead, makeData(TOXIC, 3F));
		HazardSystem.register(glass_lead, makeData(TOXIC, 3F));
		HazardSystem.register(glass_uranium, makeData(RADIATION, u * nugget));
		HazardSystem.register(glass_trinitite, makeData(RADIATION, trn * 10F));
		HazardSystem.register(glass_polonium, makeData(RADIATION, po210 * nugget));
		HazardSystem.register(sand_uranium, makeData(RADIATION, u * nugget));
		HazardSystem.register(sand_polonium, makeData(RADIATION, po210 * nugget).addEntry(HOT, 10F));
		HazardSystem.register(sand_gold198, makeData(RADIATION, au198 * block * powder_mult).addEntry(HOT, 10F));
		HazardSystem.register(block_schrabidium_cluster, makeData(RADIATION, 70F).addEntry(BLINDING, 30F));
		HazardSystem.register(block_euphemium_cluster, makeData(RADIATION, 50F).addEntry(BLINDING, 20F));
		HazardSystem.register(volcano_core, makeData(HOT, 20F));
		HazardSystem.register(block_meteor_molten, makeData(HOT, 4F));
		HazardSystem.register(arc_electrode_burnt, makeData(HOT, 2F));
		HazardSystem.register(ore_tikite, makeData(RADIATION, trx * ore).addEntry(COLD, 1));

		HazardSystem.register(insert_du, makeData(RADIATION, u238 * block));
		HazardSystem.register(insert_ferrouranium, makeData(RADIATION, ferro * 4F));
		HazardSystem.register(insert_ghiorsium, makeData(RADIATION, gh336 * 4F));
		HazardSystem.register(nugget_unobtainium_greater, makeData(RADIATION, unof * nugget));
		HazardSystem.register(billet_unobtainium, makeData(RADIATION, unof * billet));
		HazardSystem.register(billet_zfb_bismuth, makeData(RADIATION, zfb_bi * billet));
		HazardSystem.register(billet_zfb_pu241, makeData(RADIATION, zfb_pu241 * billet));
		HazardSystem.register(billet_zfb_am_mix, makeData(RADIATION, zfb_am_mix * billet));

		HazardSystem.register(bomb_waffle, makeData(RADIATION, 5F));
		HazardSystem.register(schnitzel_vegan, makeData(RADIATION, 600F));
		HazardSystem.register(cotton_candy, makeData(RADIATION, 0.25F));
		HazardSystem.register(apple_schrabidium, makeData(RADIATION, 12F));
		HazardSystem.register(balefire_scrambled, makeData(RADIATION, radspice * powder + bf));
		HazardSystem.register(balefire_and_ham, makeData(RADIATION, 2F * radspice * powder + bf));
		HazardSystem.register(ingot_radspice, makeData(RADIATION, radspice * ingot).addEntry(HOT, 15F).addEntry(BLINDING, 50F));
		HazardSystem.register(nugget_radspice, makeData(RADIATION, radspice * nugget).addEntry(HOT, 15F).addEntry(BLINDING, 50F));
		HazardSystem.register(powder_radspice, makeData(RADIATION, radspice * powder).addEntry(HOT, 15F).addEntry(BLINDING, 50F));
		HazardSystem.register(powder_radspice_tiny, makeData(RADIATION, radspice * powder_tiny).addEntry(HOT, 15F).addEntry(BLINDING, 50F));
		HazardSystem.register(levitation_unit, makeData(RADIATION, sa326 * nugget * 2F));

		HazardSystem.register(part_plutonium, makeData(RADIATION, pu * powder * 0.25F));
		HazardSystem.register(pellet_charged, makeData(RADIATION, 420F));
		HazardSystem.register(particle_digamma, makeData(RADIATION, 100F).addEntry(DIGAMMA, 0.3333F));

		HazardSystem.register(powder_paleogenite_tiny, makeData(DIGAMMA, 0.0005F));
		HazardSystem.register(powder_paleogenite, makeData(DIGAMMA, 0.005F));
		HazardSystem.register(powder_impure_osmiridium, makeData(DIGAMMA, 0.010F));

		HazardSystem.register(assembly_schrabidium, makeData(RADIATION, sa326 / 6F));
		HazardSystem.register(assembly_lead, makeData(RADIATION, 0.1F));
		HazardSystem.register(gun_revolver_schrabidium_ammo, makeData(RADIATION, sa326 / 6F));
		HazardSystem.register(gun_revolver_lead_ammo, makeData(RADIATION, 0.1F));
		HazardSystem.register(blades_schrabidium, makeData(RADIATION, sa326 * ingot * 5F));
		HazardSystem.register(stamp_schrabidium_flat, makeData(RADIATION, sa326 * ingot * 3F));
		HazardSystem.register(stamp_schrabidium_plate, makeData(RADIATION, sa326 * ingot * 3F));
		HazardSystem.register(stamp_schrabidium_wire, makeData(RADIATION, sa326 * ingot * 3F));
		HazardSystem.register(stamp_schrabidium_circuit, makeData(RADIATION, sa326 * ingot * 3F));

		HazardSystem.register(anvil_ferrouranium, makeData(RADIATION, ferro * 10F));
		HazardSystem.register(anvil_schrabidate, makeData(RADIATION, sb * 10F));
		HazardSystem.register(anvil_osmiridium, makeData(DIGAMMA, 0.4F));
		HazardSystem.register(ash_digamma, makeData(DIGAMMA, 0.001F));
		HazardSystem.register(digamma_matter, makeData(DIGAMMA, 0.2F));
		HazardSystem.register(hadron_coil_magtung, makeData(RADIATION, magt * 2F));
		HazardSystem.register(hadron_coil_schrabidium, makeData(RADIATION, magt * 2F + sa326 * 2F));
		HazardSystem.register(hadron_coil_schrabidate, makeData(RADIATION, sa326 * 2F + sb * 2F));

		if (GeneralConfig.enable528) {
			HazardSystem.register(gun_revolver_schrabidium, makeData(RADIATION, sa326 * block * 2F + sa326 * wire));
			HazardSystem.register(schrabidium_hammer, makeData(RADIATION, sa326 * block * 35F));
			HazardSystem.register(schrabidium_helmet, makeData(RADIATION, sa326 * ingot * 5F));
			HazardSystem.register(schrabidium_plate, makeData(RADIATION, sa326 * ingot * 8F));
			HazardSystem.register(schrabidium_legs, makeData(RADIATION, sa326 * ingot * 7F));
			HazardSystem.register(schrabidium_boots, makeData(RADIATION, sa326 * ingot * 4F));
			HazardSystem.register(schrabidium_sword, makeData(RADIATION, sa326 * block));
			HazardSystem.register(balefire_and_steel, makeData(RADIATION, bf * nugget));
		}

		HazardSystem.register(nugget_mercury, makeData(TOXIC, 60F));
		HazardSystem.register(bottle_mercury, makeData(TOXIC, 480F));
		HazardSystem.register(pellet_mercury, makeData(TOXIC, 30F));



		registerOtherFuel(rod_zirnox, ItemZirnoxRod.EnumZirnoxType.NATURAL_URANIUM_FUEL.ordinal(), u * rod_dual, wst * rod_dual * 11.5F, false);
		registerOtherFuel(rod_zirnox, ItemZirnoxRod.EnumZirnoxType.URANIUM_FUEL.ordinal(), uf * rod_dual, wst * rod_dual * 10F, false);
		registerOtherFuel(rod_zirnox, ItemZirnoxRod.EnumZirnoxType.TH232_FUEL.ordinal(), th232 * rod_dual, thf * rod_dual, false);
		registerOtherFuel(rod_zirnox, ItemZirnoxRod.EnumZirnoxType.THORIUM_FUEL.ordinal(), thf * rod_dual, wst * rod_dual * 7.5F, false);
		registerOtherFuel(rod_zirnox, ItemZirnoxRod.EnumZirnoxType.MOX_FUEL.ordinal(), mox * rod_dual, wst * rod_dual * 10F, false);
		registerOtherFuel(rod_zirnox, ItemZirnoxRod.EnumZirnoxType.PLUTONIUM_FUEL.ordinal(), puf * rod_dual, wst * rod_dual * 12.5F, false);
		registerOtherFuel(rod_zirnox, ItemZirnoxRod.EnumZirnoxType.U233_FUEL.ordinal(), u233 * rod_dual, wst * rod_dual * 10F, false);
		registerOtherFuel(rod_zirnox, ItemZirnoxRod.EnumZirnoxType.U235_FUEL.ordinal(), u235 * rod_dual, wst * rod_dual * 11F, false);
		registerOtherFuel(rod_zirnox, ItemZirnoxRod.EnumZirnoxType.LES_FUEL.ordinal(), saf * rod_dual, wst * rod_dual * 15F, false);
		registerOtherFuel(rod_zirnox, ItemZirnoxRod.EnumZirnoxType.LITHIUM_FUEL.ordinal(), 0, 0.001F * rod_dual, false);
		registerOtherFuel(rod_zirnox, ItemZirnoxRod.EnumZirnoxType.ZFB_MOX_FUEL.ordinal(), mox * rod_dual, wst * rod_dual * 5F, false);

		HazardSystem.register(new ItemStack(rod_zirnox_depleted, 1, EnumZirnoxTypeDepleted.NATURAL_URANIUM_FUEL.ordinal()), makeData(RADIATION, wst * rod_dual * 11.5F));
		HazardSystem.register(new ItemStack(rod_zirnox_depleted, 1, EnumZirnoxTypeDepleted.URANIUM_FUEL.ordinal()), makeData(RADIATION, wst * rod_dual * 10F));
		HazardSystem.register(new ItemStack(rod_zirnox_depleted, 1, EnumZirnoxTypeDepleted.THORIUM_FUEL.ordinal()), makeData(RADIATION, wst * rod_dual * 7.5F));
		HazardSystem.register(new ItemStack(rod_zirnox_depleted, 1, EnumZirnoxTypeDepleted.MOX_FUEL.ordinal()), makeData(RADIATION, wst * rod_dual * 10F));
		HazardSystem.register(new ItemStack(rod_zirnox_depleted, 1, EnumZirnoxTypeDepleted.PLUTONIUM_FUEL.ordinal()), makeData(RADIATION, wst * rod_dual * 12.5F));
		HazardSystem.register(new ItemStack(rod_zirnox_depleted, 1, EnumZirnoxTypeDepleted.U233_FUEL.ordinal()), makeData(RADIATION, wst * rod_dual * 10F));
		HazardSystem.register(new ItemStack(rod_zirnox_depleted, 1, EnumZirnoxTypeDepleted.U235_FUEL.ordinal()), makeData(RADIATION, wst * rod_dual * 11F));
		HazardSystem.register(new ItemStack(rod_zirnox_depleted, 1, EnumZirnoxTypeDepleted.LES_FUEL.ordinal()), makeData().addEntry(RADIATION, wst * rod_dual * 15F).addEntry(BLINDING, 20F));
		HazardSystem.register(new ItemStack(rod_zirnox_depleted, 1, EnumZirnoxTypeDepleted.ZFB_MOX_FUEL.ordinal()), makeData(RADIATION, wst * rod_dual * 5F));

		HazardSystem.register(rod_zirnox_tritium, makeData(RADIATION, 0.001F * rod_dual));

		HazardSystem.register(waste_uranium, makeData(RADIATION, 15F).addEntry(CONTAMINATING, 4F));
		HazardSystem.register(waste_thorium, makeData(RADIATION, 10F).addEntry(CONTAMINATING, 3F));
		HazardSystem.register(waste_plutonium, makeData(RADIATION, 20F).addEntry(CONTAMINATING, 5F));
		HazardSystem.register(waste_mox, makeData(RADIATION, 17.5F).addEntry(CONTAMINATING, 3F));
		HazardSystem.register(waste_schrabidium, makeData(RADIATION, 40F).addEntry(BLINDING, 50F).addEntry(CONTAMINATING, 6F));
		HazardSystem.register(waste_uranium_hot, makeData(RADIATION, 15F).addEntry(HOT, 5F).addEntry(CONTAMINATING, 4F));
		HazardSystem.register(waste_thorium_hot, makeData(RADIATION, 10F).addEntry(HOT, 5F).addEntry(CONTAMINATING, 3F));
		HazardSystem.register(waste_plutonium_hot, makeData(RADIATION, 20F).addEntry(HOT, 5F).addEntry(CONTAMINATING, 5F));
		HazardSystem.register(waste_mox_hot, makeData(RADIATION, 17.5F).addEntry(HOT, 5F).addEntry(CONTAMINATING, 3F));
		HazardSystem.register(waste_schrabidium_hot, makeData(RADIATION, 40F).addEntry(HOT, 5F).addEntry(BLINDING, 50F).addEntry(CONTAMINATING, 6F));

		registerOtherWasteContaminating(waste_natural_uranium, wst * billet * 11.5F, 15);
		registerOtherWasteContaminating(waste_u233, wst * billet * 10F, 15);
		registerOtherWasteContaminating(waste_u235, wst * billet * 11F, 15);
		registerOtherWasteContaminating(waste_zfb_mox, wst * billet * 5F, 10);

		registerOtherFuel(plate_fuel_u233, u233 * ingot, wst * ingot * 13F, false);
		registerOtherFuel(plate_fuel_u235, u235 * ingot, wst * ingot * 10F, false);
		registerOtherFuel(plate_fuel_mox, mox * ingot, wst * ingot * 16F, false);
		registerOtherFuel(plate_fuel_pu239, pu239 * ingot, wst * ingot * 13.5F, false);
		registerOtherFuel(plate_fuel_sa326, sa326 * ingot, wst * ingot * 10F, true);
		registerOtherFuel(plate_fuel_ra226be, rabe * billet, pobe * nugget * 3, false);
		registerOtherFuel(plate_fuel_pu238be, pube * billet, pube * nugget * 1, false);

		registerOtherWaste(waste_plate_u233, wst * ingot * 13F);
		registerOtherWaste(waste_plate_u235, wst * ingot * 10F);
		registerOtherWaste(waste_plate_mox, wst * ingot * 16F);
		registerOtherWaste(waste_plate_pu239, wst * ingot * 13.5F);
		registerOtherWaste(waste_plate_sa326, wst * ingot * 10F);
		registerRadSourceWaste(waste_plate_ra226be, pobe * nugget * 3);
		registerRadSourceWaste(waste_plate_pu238be, pube * nugget * 1);

		HazardSystem.register(debris_graphite, makeData().addEntry(RADIATION, 700F).addEntry(HOT, 5F));
		HazardSystem.register(debris_metal, makeData(RADIATION, 50F));
		HazardSystem.register(debris_fuel, makeData().addEntry(RADIATION, 150000F).addEntry(HOT, 5F));
		HazardSystem.register(debris_concrete, makeData(RADIATION, 30F));
		HazardSystem.register(debris_exchanger, makeData(RADIATION, 25F));
		HazardSystem.register(debris_shrapnel, makeData(RADIATION, 2.5F));
		HazardSystem.register(debris_element, makeData(RADIATION, 100F));

		HazardSystem.register(nugget_uranium_fuel, makeData(RADIATION, uf * nugget));
		HazardSystem.register(billet_uranium_fuel, makeData(RADIATION, uf * billet));
		HazardSystem.register(ingot_uranium_fuel, makeData(RADIATION, uf * ingot));
		HazardSystem.register(block_uranium_fuel, makeData(RADIATION, uf * block));
		HazardSystem.register(billet_uzh, makeData(RADIATION, uzh * billet));

		HazardSystem.register(nugget_plutonium_fuel, makeData(RADIATION, puf * nugget));
		HazardSystem.register(billet_plutonium_fuel, makeData(RADIATION, puf * billet));
		HazardSystem.register(ingot_plutonium_fuel, makeData(RADIATION, puf * ingot));
		HazardSystem.register(block_plutonium_fuel, makeData(RADIATION, puf * block));

		HazardSystem.register(nugget_thorium_fuel, makeData(RADIATION, thf * nugget));
		HazardSystem.register(billet_thorium_fuel, makeData(RADIATION, thf * billet));
		HazardSystem.register(ingot_thorium_fuel, makeData(RADIATION, thf * ingot));
		HazardSystem.register(block_thorium_fuel, makeData(RADIATION, thf * block));

		HazardSystem.register(nugget_neptunium_fuel, makeData(RADIATION, npf * nugget));
		HazardSystem.register(billet_neptunium_fuel, makeData(RADIATION, npf * billet));
		HazardSystem.register(ingot_neptunium_fuel, makeData(RADIATION, npf * ingot));

		HazardSystem.register(nugget_mox_fuel, makeData(RADIATION, mox * nugget));
		HazardSystem.register(billet_mox_fuel, makeData(RADIATION, mox * billet));
		HazardSystem.register(ingot_mox_fuel, makeData(RADIATION, mox * ingot));
		HazardSystem.register(block_mox_fuel, makeData(RADIATION, mox * block));

		HazardSystem.register(nugget_americium_fuel, makeData(RADIATION, amf * nugget));
		HazardSystem.register(billet_americium_fuel, makeData(RADIATION, amf * billet));
		HazardSystem.register(ingot_americium_fuel, makeData(RADIATION, amf * ingot));

		HazardSystem.register(nugget_schrabidium_fuel, makeData().addEntry(RADIATION, saf * nugget).addEntry(BLINDING, 5F * nugget));
		HazardSystem.register(billet_schrabidium_fuel, makeData().addEntry(RADIATION, saf * billet).addEntry(BLINDING, 5F * billet));
		HazardSystem.register(ingot_schrabidium_fuel, makeData().addEntry(RADIATION, saf * ingot).addEntry(BLINDING, 5F * ingot));
		HazardSystem.register(block_schrabidium_fuel, makeData().addEntry(RADIATION, saf * block).addEntry(BLINDING, 5F * block));

		HazardSystem.register(nugget_hes, makeData(RADIATION, saf * nugget));
		HazardSystem.register(billet_hes, makeData(RADIATION, saf * billet));
		HazardSystem.register(ingot_hes, makeData(RADIATION, saf * ingot));

		HazardSystem.register(nugget_les, makeData(RADIATION, saf * nugget));
		HazardSystem.register(billet_les, makeData(RADIATION, saf * billet));
		HazardSystem.register(ingot_les, makeData(RADIATION, saf * ingot));

		HazardSystem.register(billet_balefire_gold, makeData(RADIATION, au198 * billet));
		HazardSystem.register(billet_flashlead, makeData().addEntry(RADIATION, pb209 * 1.25F * billet).addEntry(HOT, 7F));
		HazardSystem.register(billet_po210be, makeData(RADIATION, pobe * billet));
		HazardSystem.register(billet_ra226be, makeData(RADIATION, rabe * billet));
		HazardSystem.register(billet_pu238be, makeData(RADIATION, pube * billet));

		HazardSystem.register(pellet_rtg, makeData(RADIATION, rtg * pu238).addEntry(HOT, 5F));
		HazardSystem.register(pellet_rtg_weak, makeData(RADIATION, rtg * u238));
		HazardSystem.register(pellet_rtg_radium, makeData(RADIATION, rtg * ra226));
		HazardSystem.register(pellet_rtg_strontium, makeData(RADIATION, rtg * sr90).addEntry(HOT, 5F));
		HazardSystem.register(pellet_rtg_cobalt, makeData(RADIATION, rtg * co60).addEntry(HOT, 5F));
		HazardSystem.register(pellet_rtg_actinium, makeData(RADIATION, rtg * ac227).addEntry(HOT, 5F));
		HazardSystem.register(pellet_rtg_polonium, makeData(RADIATION, rtg * po210).addEntry(HOT, 15F));
		HazardSystem.register(pellet_rtg_lead, makeData(RADIATION, rtg * pb209).addEntry(HOT, 15F));
		HazardSystem.register(pellet_rtg_gold, makeData(RADIATION, rtg * au198).addEntry(HOT, 15F));
		HazardSystem.register(pellet_rtg_americium, makeData(RADIATION, rtg * am241).addEntry(HOT, 10F));
		HazardSystem.register(pellet_rtg_balefire, makeData(RADIATION, rtg * bf * 2F).addEntry(HOT, 20F));
		HazardSystem.register(pellet_rtg_depleted_bismuth, makeData(RADIATION, 1F));
		HazardSystem.register(pellet_rtg_depleted_lead, makeData(RADIATION, 0.5F));
		HazardSystem.register(pellet_rtg_depleted_mercury, makeData(RADIATION, 4.25F));
		HazardSystem.register(pellet_rtg_depleted_neptunium, makeData(RADIATION, 3.75F).addEntry(HOT, 5F));
		HazardSystem.register(pellet_rtg_depleted_zirconium, makeData(RADIATION, 2F));

		HazardSystem.register(new ItemStack(ModItems.pile_rod, 1, EnumPileRod.RA226BE.ordinal()), makeData(RADIATION, rabe * billet * 3));
		HazardSystem.register(new ItemStack(ModItems.pile_rod, 1, EnumPileRod.PO210BE.ordinal()), makeData(RADIATION, pobe * billet * 3));
		HazardSystem.register(new ItemStack(ModItems.pile_rod, 1, EnumPileRod.NU.ordinal()), makeData(RADIATION, u * billet * 3));
		HazardSystem.register(new ItemStack(ModItems.pile_rod, 1, EnumPileRod.PU239.ordinal()), makeData(RADIATION, pu239 * billet * 3));
		HazardSystem.register(new ItemStack(ModItems.pile_rod, 1, EnumPileRod.RGP.ordinal()), makeData(RADIATION, purg * billet * 3));
		HazardSystem.register(new ItemStack(ModItems.pile_rod, 1, EnumPileRod.WASTE.ordinal()), makeData(RADIATION, wst * billet * 3));

		HazardSystem.register(pile_rod_uranium, makeData(RADIATION, u * billet * 3));
		HazardSystem.register(pile_rod_pu239, makeData(RADIATION, !GeneralConfig.enable528 ? purg * billet + pu239 * billet + u * billet : purg * billet + pu239 * billet + wst * billet));
		HazardSystem.register(pile_rod_plutonium, makeData(RADIATION, pu * billet * 3F));
		HazardSystem.register(pile_rod_source, makeData(RADIATION, rabe * billet * 3));

		registerBreedingRodRadiation(ItemBreedingRod.BreedingRodType.TRITIUM, 0.001F);
		registerBreedingRodRadiation(ItemBreedingRod.BreedingRodType.CO60, co60);
		registerBreedingRodRadiation(ItemBreedingRod.BreedingRodType.RA226, ra226);
		registerBreedingRodRadiation(ItemBreedingRod.BreedingRodType.AC227, ac227);
		registerBreedingRodRadiation(ItemBreedingRod.BreedingRodType.TH232, th232);
		registerBreedingRodRadiation(ItemBreedingRod.BreedingRodType.THF, thf);
		registerBreedingRodRadiation(ItemBreedingRod.BreedingRodType.U235, u235);
		registerBreedingRodRadiation(ItemBreedingRod.BreedingRodType.NP237, np237);
		registerBreedingRodRadiation(ItemBreedingRod.BreedingRodType.U238, u238);
		registerBreedingRodRadiation(ItemBreedingRod.BreedingRodType.PU238, pu238); //it's in a container :)
		registerBreedingRodRadiation(ItemBreedingRod.BreedingRodType.PU239, pu239);
		registerBreedingRodRadiation(ItemBreedingRod.BreedingRodType.RGP, purg);
		registerBreedingRodRadiation(ItemBreedingRod.BreedingRodType.WASTE, wst);
		registerBreedingRodRadiation(ItemBreedingRod.BreedingRodType.URANIUM, u);

		registerLegacyRods(rod_th232, rod_dual_th232, rod_quad_th232, th232);
		registerLegacyRods(rod_uranium, rod_dual_uranium, rod_quad_uranium, u);
		registerLegacyRods(rod_u233, rod_dual_u233, rod_quad_u233, u233);
		registerLegacyRods(rod_u235, rod_dual_u235, rod_quad_u235, u235);
		registerLegacyRods(rod_u238, rod_dual_u238, rod_quad_u238, u238);
		registerLegacyRods(rod_plutonium, rod_dual_plutonium, rod_quad_plutonium, pu);
		registerLegacyRods(rod_pu238, rod_dual_pu238, rod_quad_pu238, pu238);
		registerLegacyRods(rod_pu239, rod_dual_pu239, rod_quad_pu239, pu239);
		registerLegacyRods(rod_pu240, rod_dual_pu240, rod_quad_pu240, pu240);
		registerLegacyRods(rod_rgp, rod_dual_rgp, rod_quad_rgp, purg);
		registerLegacyRods(rod_neptunium, rod_dual_neptunium, rod_quad_neptunium, np237);
		registerLegacyRods(rod_polonium, rod_dual_polonium, rod_quad_polonium, po210);
		registerLegacyRods(rod_schrabidium, rod_dual_schrabidium, rod_quad_schrabidium, sa326, 0, 50);
		registerLegacyRods(rod_solinium, rod_dual_solinium, rod_quad_solinium, sa327, 0, 50);
		registerLegacyRods(rod_balefire, rod_dual_balefire, rod_quad_balefire, bf);
		registerLegacyRods(rod_balefire_blazing, rod_dual_balefire_blazing, rod_quad_balefire_blazing, bf * 2, 5, 0);

		registerLegacyRods(rod_thorium_fuel, rod_dual_thorium_fuel, rod_quad_thorium_fuel, thf);
		registerLegacyRods(rod_uranium_fuel, rod_dual_uranium_fuel, rod_quad_uranium_fuel, uf);
		registerLegacyRods(rod_plutonium_fuel, rod_dual_plutonium_fuel, rod_quad_plutonium_fuel, puf);
		registerLegacyRods(rod_mox_fuel, rod_dual_mox_fuel, rod_quad_mox_fuel, mox);
		registerLegacyRods(rod_schrabidium_fuel, rod_dual_schrabidium_fuel, rod_quad_schrabidium_fuel, saf, 0, 50);

		registerLegacyRods(rod_thorium_fuel_depleted, rod_dual_thorium_fuel_depleted, rod_quad_thorium_fuel_depleted, 12 * thf, 10, 0);
		registerLegacyRods(rod_uranium_fuel_depleted, rod_dual_uranium_fuel_depleted, rod_quad_uranium_fuel_depleted, 12 * uf, 10, 0);
		registerLegacyRods(rod_plutonium_fuel_depleted, rod_dual_plutonium_fuel_depleted, rod_quad_plutonium_fuel_depleted, 12 * puf, 10, 0);
		registerLegacyRods(rod_mox_fuel_depleted, rod_dual_mox_fuel_depleted, rod_quad_mox_fuel_depleted, 12 * mox, 10, 0);
		registerLegacyRods(rod_schrabidium_fuel_depleted, rod_dual_schrabidium_fuel_depleted, rod_quad_schrabidium_fuel_depleted, 12 * saf, 10, 50);

		registerLegacyRods(rod_waste, rod_dual_waste, rod_quad_waste, wst);
		registerLegacyRods(rod_tritium, rod_dual_tritium, rod_quad_tritium, 1);
		registerLegacyRods(rod_ac227, rod_dual_ac227, rod_quad_ac227, ac227);
		registerLegacyRods(rod_co60, rod_dual_co60, rod_quad_co60, co60);
		registerLegacyRods(rod_ra226, rod_dual_ra226, rod_quad_ra226, ra226);

		registerRBMKRod(rbmk_fuel_ueu, u * rod_rbmk, wst * rod_rbmk * 20F);
		registerRBMKRod(rbmk_fuel_meu, uf * rod_rbmk, wst * rod_rbmk * 21.5F);
		registerRBMKRod(rbmk_fuel_heu233, u233 * rod_rbmk, wst * rod_rbmk * 31F);
		registerRBMKRod(rbmk_fuel_heu235, u235 * rod_rbmk, wst * rod_rbmk * 30F);
		registerRBMKRod(rbmk_fuel_uzh, uzh * rod_rbmk, wst * rod_rbmk * 20F);
		registerRBMKRod(rbmk_fuel_thmeu, thf * rod_rbmk, wst * rod_rbmk * 17.5F);
		registerRBMKRod(rbmk_fuel_lep, puf * rod_rbmk, wst * rod_rbmk * 25F);
		registerRBMKRod(rbmk_fuel_mep, purg * rod_rbmk, wst * rod_rbmk * 30F);
		registerRBMKRod(rbmk_fuel_hep239, pu239 * rod_rbmk, wst * rod_rbmk * 32.5F);
		registerRBMKRod(rbmk_fuel_hep241, pu241 * rod_rbmk, wst * rod_rbmk * 35F);
		registerRBMKRod(rbmk_fuel_lea, amf * rod_rbmk, wst * rod_rbmk * 26F);
		registerRBMKRod(rbmk_fuel_mea, amrg * rod_rbmk, wst * rod_rbmk * 30.5F);
		registerRBMKRod(rbmk_fuel_hea241, am241 * rod_rbmk, wst * rod_rbmk * 33.5F);
		registerRBMKRod(rbmk_fuel_hea242, am242 * rod_rbmk, wst * rod_rbmk * 34F);
		registerRBMKRod(rbmk_fuel_men, npf * rod_rbmk, wst * rod_rbmk * 22.5F);
		registerRBMKRod(rbmk_fuel_hen, np237 * rod_rbmk, wst * rod_rbmk * 30F);
		registerRBMKRod(rbmk_fuel_mox, mox * rod_rbmk, wst * rod_rbmk * 25.5F);
		registerRBMKRod(rbmk_fuel_les, saf * rod_rbmk, wst * rod_rbmk * 24.5F);
		registerRBMKRod(rbmk_fuel_mes, saf * rod_rbmk, wst * rod_rbmk * 30F);
		registerRBMKRod(rbmk_fuel_hes, saf * rod_rbmk, wst * rod_rbmk * 50F);
		registerRBMKRod(rbmk_fuel_leaus, 0F, wst * rod_rbmk * 37.5F);
		registerRBMKRod(rbmk_fuel_heaus, 0F, wst * rod_rbmk * 32.5F);
		registerRBMKRod(rbmk_fuel_po210be, pobe * rod_rbmk, pobe * rod_rbmk * 0.1F, true);
		registerRBMKRod(rbmk_fuel_ra226be, rabe * rod_rbmk, rabe * rod_rbmk * 0.4F, true);
		registerRBMKRod(rbmk_fuel_pu238be, pube * rod_rbmk, wst * rod_rbmk * 2.5F);
		registerRBMKRod(rbmk_fuel_balefire_gold, au198 * rod_rbmk, bf * rod_rbmk * 0.5F, true);
		registerRBMKRod(rbmk_fuel_flashlead, pb209 * 1.25F * rod_rbmk, pb209 * nugget * 0.05F * rod_rbmk, true);
		registerRBMKRod(rbmk_fuel_balefire, bf * rod_rbmk, bf * rod_rbmk * 100F, true);
		registerRBMKRod(rbmk_fuel_zfb_bismuth, pu241 * rod_rbmk * 0.1F, wst * rod_rbmk * 5F);
		registerRBMKRod(rbmk_fuel_zfb_pu241, pu239 * rod_rbmk * 0.1F, wst * rod_rbmk * 7.5F);
		registerRBMKRod(rbmk_fuel_zfb_am_mix, pu241 * rod_rbmk * 0.1F, wst * rod_rbmk * 10F);
		registerRBMK(rbmk_fuel_drx, bf * rod_rbmk, bf * rod_rbmk * 100F, true, true, 0, 1F/3F);
		//registerRBMKRod(rbmk_fuel_curve, saf * rod_rbmk * np237 * rod_rbmk, wst * rod_rbmk * 35F);

		registerRBMKPellet(rbmk_pellet_ueu, u * billet, wst * billet * 20F);
		registerRBMKPellet(rbmk_pellet_meu, uf * billet, wst * billet * 21.5F);
		registerRBMKPellet(rbmk_pellet_heu233, u233 * billet, wst * billet * 31F);
		registerRBMKPellet(rbmk_pellet_heu235, u235 * billet, wst * billet * 30F);
		registerRBMKPellet(rbmk_pellet_uzh, uzh * billet, wst * billet * 20F);
		registerRBMKPellet(rbmk_pellet_thmeu, thf * billet, wst * billet * 17.5F);
		registerRBMKPellet(rbmk_pellet_lep, puf * billet, wst * billet * 25F);
		registerRBMKPellet(rbmk_pellet_mep, purg * billet, wst * billet * 30F);
		registerRBMKPellet(rbmk_pellet_hep239, pu239 * billet, wst * billet * 32.5F);
		registerRBMKPellet(rbmk_pellet_hep241, pu241 * billet, wst * billet * 35F);
		registerRBMKPellet(rbmk_pellet_lea, amf * billet, wst * billet * 26F);
		registerRBMKPellet(rbmk_pellet_mea, amrg * billet, wst * billet * 30.5F);
		registerRBMKPellet(rbmk_pellet_hea241, am241 * billet, wst * billet * 33.5F);
		registerRBMKPellet(rbmk_pellet_hea242, am242 * billet, wst * billet * 34F);
		registerRBMKPellet(rbmk_pellet_men, npf * billet, wst * billet * 22.5F);
		registerRBMKPellet(rbmk_pellet_hen, np237 * billet, wst * billet * 30F);
		registerRBMKPellet(rbmk_pellet_mox, mox * billet, wst * billet * 25.5F);
		registerRBMKPellet(rbmk_pellet_les, saf * billet, wst * billet * 24.5F);
		registerRBMKPellet(rbmk_pellet_mes, saf * billet, wst * billet * 30F);
		registerRBMKPellet(rbmk_pellet_hes, saf * billet, wst * billet * 50F);
		registerRBMKPellet(rbmk_pellet_leaus, 0F, wst * billet * 37.5F);
		registerRBMKPellet(rbmk_pellet_heaus, 0F, wst * billet * 32.5F);
		registerRBMKPellet(rbmk_pellet_po210be, pobe * billet, pobe * billet * 0.1F, true);
		registerRBMKPellet(rbmk_pellet_ra226be, rabe * billet, rabe * billet * 0.4F, true);
		registerRBMKPellet(rbmk_pellet_pu238be, pube * billet, wst * 1.5F);
		registerRBMKPellet(rbmk_pellet_balefire_gold, au198 * billet, bf * billet * 0.5F, true);
		registerRBMKPellet(rbmk_pellet_flashlead, pb209 * 1.25F * billet, pb209 * nugget * 0.05F, true);
		registerRBMKPellet(rbmk_pellet_balefire, bf * billet, bf * billet * 100F, true);
		registerRBMKPellet(rbmk_pellet_zfb_bismuth, pu241 * billet * 0.1F, wst * billet * 5F);
		registerRBMKPellet(rbmk_pellet_zfb_pu241, pu239 * billet * 0.1F, wst * billet * 7.5F);
		registerRBMKPellet(rbmk_pellet_zfb_am_mix, pu241 * billet * 0.1F, wst * billet * 10F);
		registerRBMKPellet(rbmk_pellet_drx, bf * billet, bf * billet * 100F, true, 0F, 1F/24F);

		HazardSystem.register(DictFrame.fromOne(ModItems.watz_pellet, ItemWatzPellet.EnumWatzType.SCHRABIDIUM), makeData(RADIATION, sa326 * ingot * 4));
		HazardSystem.register(DictFrame.fromOne(ModItems.watz_pellet, ItemWatzPellet.EnumWatzType.HES), makeData(RADIATION, saf * ingot * 4));
		HazardSystem.register(DictFrame.fromOne(ModItems.watz_pellet, ItemWatzPellet.EnumWatzType.MES), makeData(RADIATION, saf * ingot * 4));
		HazardSystem.register(DictFrame.fromOne(ModItems.watz_pellet, ItemWatzPellet.EnumWatzType.LES), makeData(RADIATION, saf * ingot * 4));
		HazardSystem.register(DictFrame.fromOne(ModItems.watz_pellet, ItemWatzPellet.EnumWatzType.HEN), makeData(RADIATION, np237 * ingot * 4));
		HazardSystem.register(DictFrame.fromOne(ModItems.watz_pellet, ItemWatzPellet.EnumWatzType.MEU), makeData(RADIATION, uf * ingot * 4));
		HazardSystem.register(DictFrame.fromOne(ModItems.watz_pellet, ItemWatzPellet.EnumWatzType.MEP), makeData(RADIATION, purg * ingot * 4));
		HazardSystem.register(DictFrame.fromOne(ModItems.watz_pellet, ItemWatzPellet.EnumWatzType.DU), makeData(RADIATION, u238 * ingot * 4));
		HazardSystem.register(DictFrame.fromOne(ModItems.watz_pellet, ItemWatzPellet.EnumWatzType.NQD), makeData(RADIATION, u235 * ingot * 4));
		HazardSystem.register(DictFrame.fromOne(ModItems.watz_pellet, ItemWatzPellet.EnumWatzType.NQR), makeData(RADIATION, pu239 * ingot * 4));


		registerPWRFuel(EnumPWRFuel.MEU, uf * billet * 2);
		registerPWRFuel(EnumPWRFuel.HEU233, u233 * billet * 2);
		registerPWRFuel(EnumPWRFuel.HEU235, u235 * billet * 2);
		registerPWRFuel(EnumPWRFuel.MEN, npf * billet * 2);
		registerPWRFuel(EnumPWRFuel.HEN237, np237 * billet * 2);
		registerPWRFuel(EnumPWRFuel.MOX, mox * billet * 2);
		registerPWRFuel(EnumPWRFuel.MEP, purg * billet * 2);
		registerPWRFuel(EnumPWRFuel.HEP239, pu239 * billet * 2);
		registerPWRFuel(EnumPWRFuel.HEP241, pu241 * billet * 2);
		registerPWRFuel(EnumPWRFuel.MEA, amrg * billet * 2);
		registerPWRFuel(EnumPWRFuel.HEA242, am242 * billet * 2);
		registerPWRFuel(EnumPWRFuel.HES326, sa326 * billet * 2);
		registerPWRFuel(EnumPWRFuel.HES327, sa327 * billet * 2);
		registerPWRFuel(EnumPWRFuel.BFB_AM_MIX, amrg * billet);
		registerPWRFuel(EnumPWRFuel.BFB_PU241, pu241 * billet);

		HazardSystem.register(powder_yellowcake, makeData(RADIATION, yc * powder).addEntry(CONTAMINATING, yc * powder));
		HazardSystem.register(block_yellowcake, makeData(RADIATION, yc * block * powder_mult));
		HazardSystem.register(ModItems.fallout, makeData(RADIATION, fo * powder).addEntry(CONTAMINATING, 4F));
		HazardSystem.register(ModBlocks.fallout, makeData(RADIATION, fo * powder * 2F));
		HazardSystem.register(block_fallout, makeData(RADIATION, fo * powder * block));
		HazardSystem.register(powder_caesium, makeData().addEntry(HYDROACTIVE, 1F).addEntry(HOT, 3F));

		HazardSystem.register(brick_asbestos, makeData(ASBESTOS, 1F));
		HazardSystem.register(tile_lab_broken, makeData(ASBESTOS, 1F));
		HazardSystem.register(powder_coltan_ore, makeData(ASBESTOS, 3F));

		HazardSystem.register(frozen_grass, makeData(COLD, 3));
		HazardSystem.register(frozen_log, makeData(COLD, 2));
		HazardSystem.register(frozen_planks, makeData(COLD, 2));
		HazardSystem.register(frozen_dirt, makeData(COLD, 1));

		//crystals
		HazardSystem.register(crystal_phosphorus, makeData(HOT, 2F * crystal));
		HazardSystem.register(crystal_trixite, makeData(RADIATION, trx * crystal).addEntry(COLD, 2));

		//nuke parts
		HazardSystem.register(boy_propellant, makeData(EXPLOSIVE, 2F));

		HazardSystem.register(gadget_core, makeData(RADIATION, pu239 * nugget * 10));
		HazardSystem.register(boy_target, makeData(RADIATION, u235 * ingot * 2));
		HazardSystem.register(boy_bullet, makeData(RADIATION, u235 * ingot));
		HazardSystem.register(man_core, makeData(RADIATION, pu239 * nugget * 10));
		HazardSystem.register(mike_core, makeData(RADIATION, u238 * nugget * 10));
		HazardSystem.register(tsar_core, makeData(RADIATION, pu239 * nugget * 15));

		HazardSystem.register(fleija_propellant, makeData().addEntry(RADIATION, 15F).addEntry(EXPLOSIVE, 8F).addEntry(BLINDING, 50F));
		HazardSystem.register(fleija_core, makeData(RADIATION, 10F));

		HazardSystem.register(solinium_propellant, makeData(EXPLOSIVE, 10F));
		HazardSystem.register(solinium_core, makeData().addEntry(RADIATION, ts * 5F + sa327 * 3F).addEntry(BLINDING, 45F));

		HazardSystem.register(nuke_fstbmb, makeData(DIGAMMA, 0.01F));
		//HazardSystem.register(DictFrame.fromOne(ModItems.holotape_image, EnumHoloImage.HOLO_RESTORED), makeData(DIGAMMA, 1F));
		//HazardSystem.register(holotape_damaged, makeData(DIGAMMA, 1_000F));

		/*
		 * Blacklist
		 */
		// EE parity: radioactive ores (uranium, thorium, planet analogues) keep ore-dict radiation from OreDictManager
	}

	public static void registerFluids() {
		registerFluid("radwater_fluid", 4, 0);
		registerFluid("toxic_fluid", 1000, 0, 50, 0, 0);
		registerFluid("mud_fluid", 400, 0, 800, 0, 0);
		registerFluid("schrabidic", 700, 20);
		registerFluid("corium_fluid", 10000, 0);
		registerFluid("liquid_osmiridium", 20, 0, 0, 0, 0.005F);
		registerFluidTemps();
	}

	private static void registerFluid(String f, float rads, float blind) {
		registerFluid(f, rads, blind, 0, 0, 0);
	}

	private static void registerFluid(String f, float rads, float blind, float tox, float expl, float dig) {
		Fluid fluid = FluidRegistry.getFluid(f);
		if (fluid == null) return;
		int temp = fluid.getTemperature() - 273;
		ObjectArrayList<HazardEntry> entries = new ObjectArrayList<>();

		if (rads > 0) entries.add(new HazardEntry(RADIATION, rads));
		if (temp > 100) entries.add(new HazardEntry(HOT, Math.min(50, temp / 50F)));
		if (temp < -60) entries.add(new HazardEntry(COLD, Math.abs(temp / 20F)));
		if (tox > 0) entries.add(new HazardEntry(TOXIC, tox));
		if (blind > 0) entries.add(new HazardEntry(BLINDING, blind));
		if (expl > 0) entries.add(new HazardEntry(EXPLOSIVE, expl));
		if (dig > 0) entries.add(new HazardEntry(DIGAMMA, dig));
		if (!entries.isEmpty())
			HazardTransformerForgeFluid.FLUID_HAZARDS.put(f, entries);
	}

	private static void registerFluidTemps() {
		for (Fluid fluid : FluidRegistry.getRegisteredFluids().values()) {
			String name = fluid.getName();
			if (HazardTransformerForgeFluid.FLUID_HAZARDS.containsKey(name)) continue;

			int temp = fluid.getTemperature() - 273;
			ObjectArrayList<HazardEntry> entries = new ObjectArrayList<>();
			if (temp > 100) entries.add(new HazardEntry(HOT, Math.min(50, temp / 50F)));
			if (temp < -60) entries.add(new HazardEntry(COLD, Math.abs(temp / 20F)));
			if (!entries.isEmpty())
				HazardTransformerForgeFluid.FLUID_HAZARDS.put(name, entries);
		}
	}

	public static void registerTrafos() {
		HazardSystem.trafos.add(new HazardTransformerRadiationNBT());
		HazardSystem.trafos.add(new HazardTransformerForgeFluid());

		if(!(GeneralConfig.enableLBSM && GeneralConfig.enableLBSMSafeCrates))	HazardSystem.trafos.add(new HazardTransformerRadiationContainer());
		if(!(GeneralConfig.enableLBSM && GeneralConfig.enableLBSMSafeMEDrives))	HazardSystem.trafos.add(new HazardTransformerRadiationME());
		// Keep custom post transformer at the end
		HazardSystem.trafos.add(new HazardTransformerPostCustom());
	}

	private static HazardData makeData() { return new HazardData(); }
	private static HazardData makeData(IHazardType hazard) { return new HazardData().addEntry(hazard); }
	private static HazardData makeData(IHazardType hazard, float level) { return new HazardData().addEntry(hazard, level); }
	private static HazardData makeData(IHazardType hazard, float level, boolean override) { return new HazardData().addEntry(hazard, level, override); }

	private static void registerPWRFuel(EnumPWRFuel fuel, float baseRad) {
		HazardSystem.register(DictFrame.fromOne(ModItems.pwr_fuel, fuel), makeData(RADIATION, baseRad));
		HazardSystem.register(DictFrame.fromOne(ModItems.pwr_fuel_hot, fuel), makeData(RADIATION, baseRad * 10).addEntry(HOT, 5));
		HazardSystem.register(DictFrame.fromOne(ModItems.pwr_fuel_depleted, fuel), makeData(RADIATION, baseRad * 10));
	}

	private static void registerRBMKPellet(Item pellet, float base, float dep) { registerRBMKPellet(pellet, base, dep, false, 0F, 0F); }
	private static void registerRBMKPellet(Item pellet, float base, float dep, boolean linear) { registerRBMKPellet(pellet, base, dep, linear, 0F, 0F); }
	private static void registerRBMKPellet(Item pellet, float base, float dep, boolean linear, float blinding, float digamma) {

		HazardData data = new HazardData();
		data.addEntry(new HazardEntry(RADIATION, base).addMod(new HazardModifierRBMKRadiation(dep, linear)));
		if(blinding > 0) data.addEntry(new HazardEntry(BLINDING, blinding));
		if(digamma > 0) data.addEntry(new HazardEntry(DIGAMMA, digamma).addMod(new HazardModifierRBMKRadiation(digamma * 10F, linear)));
		HazardSystem.register(pellet, data);
	}

	private static void registerRBMKRod(Item rod, float base, float dep) { registerRBMK(rod, base, dep, true, false, 0F, 0F); }
	private static void registerRBMKRod(Item rod, float base, float dep, float blinding) { registerRBMK(rod, base, dep, true, false, blinding, 0F); }
	private static void registerRBMKRod(Item rod, float base, float dep, boolean linear) { registerRBMK(rod, base, dep, true, linear, 0F, 0F); }

	private static void registerRBMK(Item rod, float base, float dep, boolean hot, boolean linear, float blinding, float digamma) {

		HazardData data = new HazardData();
		data.addEntry(new HazardEntry(RADIATION, base).addMod(new HazardModifierRBMKRadiation(dep, linear)));
		if(hot) data.addEntry(new HazardEntry(HOT, 0).addMod(new HazardModifierRBMKHot()));
		if(blinding > 0) data.addEntry(new HazardEntry(BLINDING, blinding));
		if(digamma > 0) data.addEntry(new HazardEntry(DIGAMMA, digamma).addMod(new HazardModifierRBMKRadiation(digamma * 10F, linear)));
		HazardSystem.register(rod, data);
	}

	private static void registerBreedingRodRadiation(ItemBreedingRod.BreedingRodType type, float base) {
		HazardSystem.register(new ItemStack(ModItems.rod, 1, type.ordinal()), makeData(RADIATION, base));
		HazardSystem.register(new ItemStack(ModItems.rod_dual, 1, type.ordinal()), makeData(RADIATION, base * rod_dual));
		HazardSystem.register(new ItemStack(ModItems.rod_quad, 1, type.ordinal()), makeData(RADIATION, base * rod_quad));
	}

	private static void registerLegacyRods(Item single, Item dual, Item quad, float base) {
		registerLegacyRods(single, dual, quad, base, 0, 0);
	}

	private static void registerLegacyRods(Item single, Item dual, Item quad, float base, float hot, float blind) {
		registerLegacyRod(single, base * rod, hot * rod, blind * rod);
		registerLegacyRod(dual, base * rod_dual, hot * rod_dual, blind * rod_dual);
		registerLegacyRod(quad, base * rod_quad, hot * rod_quad, blind * rod_quad);
	}

	private static void registerLegacyRod(Item item, float rads, float hot, float blind) {
		HazardData data = makeData(RADIATION, rads);
		if (hot > 0) data.addEntry(HOT, hot);
		if (blind > 0) data.addEntry(BLINDING, blind);
		HazardSystem.register(item, data);
	}

	private static void registerOtherFuel(Item fuel, float base, float target, boolean blinding) {

		HazardData data = new HazardData();
		data.addEntry(new HazardEntry(RADIATION, base).addMod(new HazardModifierFuelRadiation(target)));
		if(blinding)
			data.addEntry(BLINDING, 20F);
		HazardSystem.register(fuel, data);
	}

	private static void registerOtherFuel(Item fuel, int meta, float base, float target, boolean blinding) {

		HazardData data = new HazardData();
		data.addEntry(new HazardEntry(RADIATION, base).addMod(new HazardModifierFuelRadiation(target)));
		if(blinding)
			data.addEntry(BLINDING, 20F);
		HazardSystem.register(new ItemStack(fuel, 1, meta), data);
	}

	private static void registerRTGPellet(Item pellet, float base, float target) { registerRTGPellet(pellet, base, target, 0, 0); }
	private static void registerRTGPellet(Item pellet, float base, float target, float hot) { registerRTGPellet(pellet, base, target, hot, 0); }

	private static void registerRTGPellet(Item pellet, float base, float target, float hot, float blinding) {
		HazardData data = new HazardData();
		data.addEntry(new HazardEntry(RADIATION, base).addMod(new HazardModifierRTGRadiation(target)));
		if(hot > 0) data.addEntry(new HazardEntry(HOT, hot));
		if(blinding > 0) data.addEntry(new HazardEntry(BLINDING, blinding));
		HazardSystem.register(pellet, data);
	}

	private static void registerOtherWaste(Item waste, float base) {
		HazardSystem.register(new ItemStack(waste, 1, 0), makeData(RADIATION, base * 0.075F));

		HazardData data = new HazardData();
		data.addEntry(new HazardEntry(RADIATION, base));
		data.addEntry(new HazardEntry(HOT, 5F));
		HazardSystem.register(new ItemStack(waste, 1, 1), data);
	}

	private static void registerOtherWasteContaminating(Item waste, float base, float contaminating) {
		HazardSystem.register(new ItemStack(waste, 1, 0), makeData(RADIATION, base * 0.075F).addEntry(CONTAMINATING, contaminating));

		HazardData data = new HazardData();
		data.addEntry(new HazardEntry(RADIATION, base));
		data.addEntry(new HazardEntry(HOT, 5F));
		data.addEntry(new HazardEntry(CONTAMINATING, contaminating));
		HazardSystem.register(new ItemStack(waste, 1, 1), data);
	}

	private static void registerRadSourceWaste(Item waste, float base) {
		HazardSystem.register(new ItemStack(waste, 1, 0), makeData(RADIATION, base));

		HazardData data = new HazardData();
		data.addEntry(new HazardEntry(RADIATION, base));
		data.addEntry(new HazardEntry(HOT, 5F));
		HazardSystem.register(new ItemStack(waste, 1, 1), data);
	}

    //mlbv: must run after OreDictManager.registerOres();
    public static void registerContaminatingDrops() {
        addContaminatingDrop(TCALLOY.dust(), 0.07F * powder);
        addContaminatingDrop(TH232.dust(), th232 * powder);
        addContaminatingDrop(U.dust(), u * powder);
        addContaminatingDrop(PU.dust(), pu * powder);
        addContaminatingDrop(NP237.dust(), np237 * powder);
        addContaminatingDrop(PO210.dust(), po210 * powder);
        addContaminatingDrop(SA326.dust(), sa326 * powder);
        addContaminatingDrop(SBD.dust(), sb * powder);
        addContaminatingDrop(TS.dust(), 120F * powder);
        addContaminatingDrop(CO60.dust(), co60 * powder);
        addContaminatingDrop(CO60.dustTiny(), co60 * powder_tiny);
        addContaminatingDrop(AU198.dust(), au198 * powder);
        addContaminatingDrop(RA226.dust(), ra226 * powder);
        addContaminatingDrop(AC227.dust(), ac227 * powder);
        addContaminatingDrop(AC227.dustTiny(), ac227 * powder_tiny);
        addContaminatingDrop(SR90.dust(), sr90 * powder);
        addContaminatingDrop(SR90.dustTiny(), sr90 * powder_tiny);
        addContaminatingDrop(AT209.dust(), at209 * powder);
        addContaminatingDrop(AT209.dustTiny(), at209 * powder_tiny);
        addContaminatingDrop(PB209.dust(), pb209 * powder);
        addContaminatingDrop(PB209.dustTiny(), pb209 * powder);
        addContaminatingDrop(I131.dust(), i131 * powder);
        addContaminatingDrop(I131.dustTiny(), i131 * powder_tiny);
        addContaminatingDrop(XE135.dust(), xe135 * powder);
        addContaminatingDrop(XE135.dustTiny(), xe135 * powder_tiny);
        addContaminatingDrop(CS137.dust(), cs137 * powder);
        addContaminatingDrop(CS137.dustTiny(), cs137 * powder_tiny);
        addContaminatingDrop(powder_balefire, bf * powder);
    }

    private static void addContaminatingDrop(final String ore, final double contaminating) {
        HazardSystem.oreMap.computeIfPresent(ore, (k, v) -> v.addEntry(CONTAMINATING, contaminating));
    }

    private static void addContaminatingDrop(final Item item, final double contaminating) {
        HazardSystem.itemMap.computeIfPresent(item, (k, v) -> v.addEntry(CONTAMINATING, contaminating));
    }
}
