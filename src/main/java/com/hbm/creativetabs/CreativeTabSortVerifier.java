package com.hbm.creativetabs;

import com.hbm.handler.jei.HbmJeiIngredientSort;
import com.hbm.items.special.ItemBedrockOreNew;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Runnable verification entry point for shipped sort classes.
 * Invoked by Gradle task {@code verifyCreativeTabSort}.
 */
public final class CreativeTabSortVerifier {

	private static final List<String> TAB_KEYS = Arrays.asList(
			"blockTab",
			"consumableTab",
			"controlTab",
			"machineTab",
			"missileTab",
			"nukeTab",
			"partsTab",
			"resourceTab",
			"templateTab",
			"weaponTab"
	);

	private CreativeTabSortVerifier() {
	}

	public static void main(String[] args) throws Exception {
		net.minecraft.init.Bootstrap.register();
		Path scratch = Paths.get(System.getenv().getOrDefault(
				"GOAL_SCRATCH",
				"C:/Temp/grok-goal-a85ccd828ffa/implementer"));
		Files.createDirectories(scratch);

		verifyRegistryPathLookup();
		verifyRealModRegistryItems();
		verifyItemStackSortIndexPath();
		verifyUnknownLexicalSort();
		verifyUnknownItemStackSort();
		verifyScrambledTabsRestoreOrder();
		verifyScrambledItemStacksRestoreOrder();
		verifyBatteryAdjacencyViaItemStacks();
		verifyBatteryGetSubItemsGrouping();
		verifyCompareStacksNoIndexTieBreak();
		verifyTabPipelineCollectSortAppend();
		verifyMaterialFamilyContiguity();
		verifyWeaponTabShieldFamily();
		verifyCreativeWeaponShieldBlock();
		verifyJeiWeaponShieldBlock();
		verifyWeaponTabArtilleryAmmo();
		verifyWeaponEeCeFirearmBlocks();
		verifyWeaponModBlock();
		verifyRailgunPlasmaPlacement();
		verifyMeteoriteSwordDamageOrder();
		verifyPartsTabBedrockJeiBlock();
		verifyBedrockOreTypeMajorVariantOrder();
		verifyBedrockOreType6ProcessingChain();
		verifyEnumMultiVariantOrder();
		verifyAlloyStableRegistrationOrder();
		verifyControlTabRbmkPelletBlock();
		verifyControlTabRbmkFuelBlock();
		verifyRbmkPelletSearchContinuity();
		verifyBedrockOreFullGetSubItemsOrder();
		verifyVariantScatterSearchOrder();
		verifyMissileThrusterBlock();
		verifyControlTabSpaceRbmkFuelRods();
		verifyMachineTabRbmkBurnerPlacement();
		verifyHardDriveControlTabPlacement();
		verifySellafieldOreResourceTab();
		verifyOreMeteorResourceTab();
		verifyBlockTabConcreteColored();
		verifyBlockTabLampTritium();
		verifyCreativeTabExclusions();
		verifySearchTabGlobalOrder();
		verifyModItemsPowerArmorDeclOrder();
		writeExecutionEvidence(scratch.resolve("creative-tab-sort-execution-java.txt"));

		System.out.println("CreativeTabSortVerifier PASS");
	}

	private static void verifyRealModRegistryItems() {
		ItemStack uranium = probeStack("hbm", "ingot_uranium");
		ItemStack mapper = probeStack("hbm", "sat_mapper");
		int uraniumIdx = CreativeTabSortOrder.getSortIndex(uranium, "partsTab");
		int mapperIdx = CreativeTabSortOrder.getSortIndex(mapper, "missileTab");
		if (uraniumIdx >= 500_000) {
			throw new AssertionError("ingot_uranium registry must resolve in partsTab, got " + uraniumIdx);
		}
		if (mapperIdx >= 500_000) {
			throw new AssertionError("sat_mapper registry must resolve in missileTab, got " + mapperIdx);
		}
		System.out.println("mod_registry_paths ingot_uranium=" + uraniumIdx + " sat_mapper=" + mapperIdx);
	}

	private static void verifyBatteryGetSubItemsGrouping() {
		NonNullList<ItemStack> collected = NonNullList.create();
		collected.add(batteryStack("battery_steam", 60000L));
		collected.add(batteryStack("battery_steam", 0L));
		NonNullList<ItemStack> scrambled = NonNullList.create();
		scrambled.add(probeStack("hbm", "gun_deagle"));
		scrambled.add(collected.get(1));
		scrambled.add(probeStack("hbm", "gun_revolver"));
		scrambled.add(collected.get(0));
		CreativeTabSortHelper.sortStacks(scrambled, "controlTab");
		int first = -1;
		int last = -1;
		for (int i = 0; i < scrambled.size(); i++) {
			ResourceLocation reg = scrambled.get(i).getItem().getRegistryName();
			if (reg != null && "battery_steam".equals(reg.getPath())) {
				if (first < 0) {
					first = i;
				}
				last = i;
			}
		}
		if (first < 0 || last < 0 || first + 1 != last) {
			throw new AssertionError("battery_steam variants must stay adjacent after sortStacks");
		}
		System.out.println("battery_variants_adjacent=true item=battery_steam (simulates ItemBattery.getSubItems output)");
	}

	private static void verifyMaterialFamilyContiguity() {
		verifyPrefixBlockContiguous("partsTab", "ingot_");
		verifyPrefixBlockContiguous("partsTab", "nugget_");
		verifyPrefixBlockContiguous("partsTab", "powder_");
		verifyPrefixBlockContiguous("partsTab", "billet_");
		verifyPrefixBlockContiguous("partsTab", "mechanism_");
		verifyPrefixBlockContiguous("partsTab", "warhead_");
		verifyGunAmmoBlockContiguity();
		verifyMeleeClusterPresent();
		verifyAssemblyTemplateHidden();
		System.out.println("material_family_contiguity=true");
	}

	private static void verifyPrefixBlockContiguous(String tabKey, String prefix) {
		List<String> order = CreativeTabSortOrder.getTabRegistryOrder(tabKey);
		int first = -1;
		int last = -1;
		int count = 0;
		for (int i = 0; i < order.size(); i++) {
			String key = order.get(i);
			String path = registryPath(key);
			if (path.startsWith(prefix)) {
				count++;
				if (first < 0) {
					first = i;
				}
				last = i;
			}
		}
		if (count == 0) {
			return;
		}
		if (last - first + 1 != count) {
			throw new AssertionError(
					"tab=" + tabKey + " prefix=" + prefix + " entries=" + count + " span=" + (last - first + 1));
		}
	}

	/** creative-tab-grouping-5: all gun_*_ammo entries form one block after firearms. */
	private static boolean isGunAmmoPath(String path) {
		return path.startsWith("gun_") && path.contains("_ammo");
	}

	private static void verifyGunAmmoBlockContiguity() {
		List<String> order = CreativeTabSortOrder.getTabRegistryOrder("weaponTab");
		int first = -1;
		int last = -1;
		int count = 0;
		for (int i = 0; i < order.size(); i++) {
			String path = registryPath(order.get(i));
			if (isGunAmmoPath(path)) {
				count++;
				if (first < 0) {
					first = i;
				}
				last = i;
			}
		}
		if (count == 0) {
			throw new AssertionError("weaponTab must contain gun_*_ammo entries");
		}
		if (last - first + 1 != count) {
			throw new AssertionError(
					"weaponTab gun_*_ammo entries must be contiguous, count=" + count + " span=" + (last - first + 1));
		}
		int schrabAmmo = order.indexOf("gun_revolver_schrabidium_ammo");
		if (schrabAmmo < 0) {
			throw new AssertionError("weaponTab missing gun_revolver_schrabidium_ammo probe");
		}
		if (schrabAmmo < first || schrabAmmo > last) {
			throw new AssertionError("gun_revolver_schrabidium_ammo must sit inside gun_ammo block");
		}
		int schrabGun = order.indexOf("gun_revolver_schrabidium");
		if (schrabGun >= 0 && schrabAmmo == schrabGun + 1) {
			throw new AssertionError("grouping-5: revolver ammo must not immediately follow its gun");
		}
		System.out.println("gun_ammo_block_contiguous=true count=" + count + " first=" + first + " last=" + last);
	}

	/** EE ModItems ~819-901 firearm order; sedna-replaced ids live only in the CE block. */
	private static final String[] WEAPON_TAB_EE_FIREARMS = {
			"ullapool_caber",
			"gun_b92", "gun_b93",
			"gun_revolver_iron", "gun_revolver", "gun_revolver_saturnite", "gun_revolver_gold",
			"gun_revolver_lead", "gun_revolver_schrabidium", "gun_revolver_cursed",
			"gun_revolver_nightmare", "gun_revolver_nightmare2", "gun_revolver_pip",
			"gun_revolver_nopip", "gun_revolver_blackjack", "gun_revolver_silver", "gun_revolver_red",
			"gun_deagle", "gun_flechette", "gun_ar15", "gun_uboinik", "gun_supershotgun", "gun_jshotgun",
			"gun_ks23", "gun_sauer", "gun_calamity", "gun_calamity_dual",
			"gun_minigun", "gun_avenger", "gun_lacunae",
			"gun_bolt_action", "gun_bolt_action_green",
			"gun_uzi", "gun_uzi_silencer", "gun_uzi_saturnite", "gun_uzi_saturnite_silencer",
			"gun_mp40",
			"gun_rpg", "gun_karl",
			"gun_lever_action", "gun_lever_action_dark", "gun_hk69",
			"gun_spark", "gun_fatman", "gun_proto", "gun_mirv", "gun_bf",
			"gun_zomg", "gun_xvl1456",
			"gun_hp", "gun_defabricator", "gun_vortex", "cc_plasma_gun", "gun_egon",
			"gun_euthanasia", "gun_skystinger", "gun_mp",
			"gun_cryolator", "gun_jack", "gun_immolator",
			"gun_osipr", "gun_emp",
			"gun_moist_nugget", "gun_super_shotgun", "gun_revolver_inverted",
			"gun_lever_action_sonata", "gun_bolt_action_saturnite",
			"gun_dampfmaschine", "gun_darter",
	};

	/** CE GameRegistry.registerItem order ~5753-5822 (sedna firearms in port). */
	private static final String[] WEAPON_TAB_CE_FIREARMS = {
			"gun_pepperbox", "gun_light_revolver", "gun_light_revolver_atlas", "gun_light_revolver_dani",
			"gun_henry", "gun_henry_lincoln", "gun_greasegun",
			"gun_maresleg", "gun_maresleg_akimbo", "gun_maresleg_broken", "gun_flaregun",
			"gun_heavy_revolver", "gun_heavy_revolver_lilmac", "gun_heavy_revolver_protege",
			"gun_carbine", "gun_am180", "gun_liberator", "gun_congolake",
			"gun_flamer_sedna", "gun_flamer_topaz", "gun_flamer_daybreaker",
			"gun_uzi_sedna", "gun_uzi_akimbo", "gun_spas12", "gun_panzerschreck_sedna",
			"gun_g3", "gun_g3_zebra", "gun_chemthrower",
			"gun_amat", "gun_amat_subtlety", "gun_amat_penance", "gun_m2",
			"gun_autoshotgun", "gun_autoshotgun_shredder", "gun_autoshotgun_sexy", "gun_autoshotgun_heretic",
			"gun_quadro_sedna", "gun_lag", "gun_minigun_sedna", "gun_minigun_dual", "gun_minigun_lacunae",
			"gun_missile_launcher", "gun_tesla_cannon",
			"gun_laser_pistol", "gun_laser_pistol_pew_pew", "gun_laser_pistol_morning_glory",
			"gun_stg77", "gun_tau", "gun_lasrifle", "gun_stinger", "gun_coilgun",
			"gun_hangman", "gun_mas36", "gun_bolter_sedna", "gun_folly",
			"gun_aberrator", "gun_aberrator_eott",
			"gun_double_barrel", "gun_double_barrel_sacred_dragon", "gun_n_i_4_n_i",
			"gun_fireext", "gun_charge_thrower", "gun_drill", "gun_pa_melee", "gun_pa_ranged",
			"gun_mk108", "gun_star_f",
	};

	private static void verifyWeaponEeCeFirearmBlocks() {
		List<String> order = CreativeTabSortOrder.getTabRegistryOrder("weaponTab");
		int ammoStart = order.indexOf("gun_b92_ammo");
		if (ammoStart < 0) {
			throw new AssertionError("weaponTab missing gun_b92_ammo ammo block start");
		}
		int expectedFirearms = WEAPON_TAB_EE_FIREARMS.length + WEAPON_TAB_CE_FIREARMS.length;
		if (ammoStart != expectedFirearms) {
			throw new AssertionError("weaponTab firearm count mismatch: ammo starts at " + ammoStart
					+ " expected " + expectedFirearms);
		}
		for (int i = 0; i < WEAPON_TAB_EE_FIREARMS.length; i++) {
			String path = registryPath(order.get(i));
			if (!WEAPON_TAB_EE_FIREARMS[i].equals(path)) {
				throw new AssertionError("weaponTab EE firearm mismatch at " + i + ": expected "
						+ WEAPON_TAB_EE_FIREARMS[i] + " got " + path);
			}
			int idx = CreativeTabSortOrder.getSortIndex(probeStack("hbm", path), "weaponTab");
			if (idx != i) {
				throw new AssertionError(path + " EE sort index must be " + i + " got " + idx);
			}
		}
		int ceBase = WEAPON_TAB_EE_FIREARMS.length;
		for (int i = 0; i < WEAPON_TAB_CE_FIREARMS.length; i++) {
			String path = registryPath(order.get(ceBase + i));
			if (!WEAPON_TAB_CE_FIREARMS[i].equals(path)) {
				throw new AssertionError("weaponTab CE firearm mismatch at " + i + ": expected "
						+ WEAPON_TAB_CE_FIREARMS[i] + " got " + path);
			}
			int idx = CreativeTabSortOrder.getSortIndex(probeStack("hbm", path), "weaponTab");
			if (idx != ceBase + i) {
				throw new AssertionError(path + " CE sort index must be " + (ceBase + i) + " got " + idx);
			}
		}
		int silencer = order.indexOf("gun_uzi_silencer");
		int uzi = order.indexOf("gun_uzi");
		int uziSedna = order.indexOf("gun_uzi_sedna");
		if (silencer < 0 || uzi < 0 || uziSedna < 0) {
			throw new AssertionError("weaponTab missing gun_uzi_silencer, gun_uzi, or gun_uzi_sedna");
		}
		if (silencer >= ceBase || uzi >= ceBase || uziSedna < ceBase) {
			throw new AssertionError("gun_uzi and gun_uzi_silencer must be EE block, gun_uzi_sedna must be CE block");
		}
		System.out.println("weapon_ee_ce_firearm_blocks=true ee=" + WEAPON_TAB_EE_FIREARMS.length
				+ " ce=" + WEAPON_TAB_CE_FIREARMS.length + " ammoStart=" + ammoStart);
	}

	private static final String[] WEAPON_MOD_ORDER = {
			"weapon_mod_generic",
			"weapon_mod_special",
			"weapon_mod_caliber",
	};

	private static void verifyWeaponModBlock() {
		List<String> order = CreativeTabSortOrder.getTabRegistryOrder("weaponTab");
		int first = -1;
		int last = -1;
		int prevPos = -1;
		int prevIdx = -1;
		for (int i = 0; i < WEAPON_MOD_ORDER.length; i++) {
			String path = WEAPON_MOD_ORDER[i];
			int pos = order.indexOf(path);
			if (pos < 0) {
				throw new AssertionError("weaponTab missing " + path);
			}
			int sortIdx = CreativeTabSortOrder.getSortIndex(probeStack("hbm", path), "weaponTab");
			if (sortIdx >= CreativeTabSortOrder.UNKNOWN_SORT_INDEX) {
				throw new AssertionError(path + " must have explicit sort index");
			}
			if (prevPos >= 0) {
				if (pos != prevPos + 1) {
					throw new AssertionError("weapon_mod entries must be contiguous in tab order");
				}
				if (sortIdx <= prevIdx) {
					throw new AssertionError("weapon_mod sort indices must increase: " + path);
				}
			}
			if (first < 0) {
				first = pos;
			}
			last = pos;
			prevPos = pos;
			prevIdx = sortIdx;
		}
		int himars = order.indexOf("ammo_himars");
		int crucible = order.indexOf("crucible");
		int hsSword = order.indexOf("hs_sword");
		if (himars >= 0 && first <= himars) {
			throw new AssertionError("weapon_mod block must follow ammo_himars");
		}
		if (crucible < 0) {
			throw new AssertionError("weaponTab missing crucible");
		}
		if (last >= crucible) {
			throw new AssertionError("weapon_mod block must precede crucible");
		}
		if (hsSword >= 0 && crucible >= hsSword) {
			throw new AssertionError("crucible must precede hs_sword");
		}
		int crucibleIdx = CreativeTabSortOrder.getSortIndex(probeStack("hbm", "crucible"), "weaponTab");
		if (crucibleIdx <= prevIdx) {
			throw new AssertionError("crucible sort index must follow weapon_mod_caliber");
		}
		if (hsSword >= 0) {
			int hsIdx = CreativeTabSortOrder.getSortIndex(probeStack("hbm", "hs_sword"), "weaponTab");
			if (crucibleIdx >= hsIdx) {
				throw new AssertionError("crucible sort index must precede hs_sword");
			}
		}
		ItemStack generic0 = probeStack("hbm", "weapon_mod_generic", 0);
		ItemStack generic1 = probeStack("hbm", "weapon_mod_generic", 1);
		ItemStack special0 = probeStack("hbm", "weapon_mod_special", 0);
		ItemStack caliber0 = probeStack("hbm", "weapon_mod_caliber", 0);
		if (CreativeTabSortHelper.compareStacks(generic0, generic1, "weaponTab") >= 0) {
			throw new AssertionError("weapon_mod_generic meta variants must sort by metadata");
		}
		if (CreativeTabSortHelper.compareStacks(generic1, special0, "weaponTab") >= 0) {
			throw new AssertionError("weapon_mod_generic must precede weapon_mod_special in stack sort");
		}
		if (CreativeTabSortHelper.compareStacks(special0, caliber0, "weaponTab") >= 0) {
			throw new AssertionError("weapon_mod_special must precede weapon_mod_caliber in stack sort");
		}
		ItemStack caliber7 = probeStack("hbm", "weapon_mod_caliber", 7);
		ItemStack crucibleStack = probeStack("hbm", "crucible");
		if (CreativeTabSortHelper.compareStacks(caliber7, crucibleStack, "weaponTab") >= 0) {
			throw new AssertionError("weapon_mod_caliber meta 7 must precede crucible in stack sort");
		}
		System.out.println(
				"weapon_mod_block=true first=" + first + " last=" + last + " indices=" + prevIdx);
	}

	private static void verifyRailgunPlasmaPlacement() {
		List<String> order = CreativeTabSortOrder.getTabRegistryOrder("weaponTab");
		int fritz = order.indexOf("turret_fritz");
		int railgun = order.indexOf("railgun_plasma");
		int brandon = order.indexOf("turret_brandon");
		if (fritz < 0 || railgun < 0 || brandon < 0) {
			throw new AssertionError("weaponTab missing turret_fritz, railgun_plasma, or turret_brandon");
		}
		if (railgun != fritz + 1) {
			throw new AssertionError("railgun_plasma must immediately follow turret_fritz");
		}
		if (brandon != railgun + 1) {
			throw new AssertionError("turret_brandon must immediately follow railgun_plasma");
		}
		int fritzIdx = CreativeTabSortOrder.getSortIndex(probeStack("hbm", "turret_fritz"), "weaponTab");
		int railgunIdx = CreativeTabSortOrder.getSortIndex(probeStack("hbm", "railgun_plasma"), "weaponTab");
		int brandonIdx = CreativeTabSortOrder.getSortIndex(probeStack("hbm", "turret_brandon"), "weaponTab");
		if (fritzIdx >= railgunIdx || railgunIdx >= brandonIdx) {
			throw new AssertionError("turret_fritz < railgun_plasma < turret_brandon sort indices required");
		}
		System.out.println("railgun_plasma_placement=true fritz=" + fritz + " railgun=" + railgun);
	}

	private static void verifyAssemblyTemplateHidden() {
		for (String tabKey : TAB_KEYS) {
			List<String> order = CreativeTabSortOrder.getTabRegistryOrder(tabKey);
			if (order.contains("assembly_template")) {
				throw new AssertionError("assembly_template must not appear on tab " + tabKey);
			}
		}
		System.out.println("assembly_template_hidden=true");
	}

	private static final String[] REQUIRED_WEAPON_TAB_SHIELDS = {
			"alloy_shield",
			"cmb_shield",
			"cobalt_shield",
			"desh_shield",
			"elec_shield",
			"schrabidium_shield",
			"starmetal_shield",
			"steel_shield",
			"titanium_shield",
	};

	private static void verifyWeaponTabShieldFamily() {
		List<String> order = CreativeTabSortOrder.getTabRegistryOrder("weaponTab");
		int first = -1;
		int last = -1;
		int count = 0;
		for (int i = 0; i < order.size(); i++) {
			String path = registryPath(order.get(i));
			if (path.endsWith("_shield")) {
				count++;
				if (first < 0) {
					first = i;
				}
				last = i;
			}
		}
		if (count == 0) {
			throw new AssertionError("weaponTab must contain ModShield entries");
		}
		if (last - first + 1 != count) {
			throw new AssertionError(
					"weaponTab _shield entries must be contiguous, count=" + count + " span=" + (last - first + 1));
		}
		for (String shield : REQUIRED_WEAPON_TAB_SHIELDS) {
			int idx = CreativeTabSortOrder.getSortIndex(probeStack("hbm", shield), "weaponTab");
			if (idx >= CreativeTabSortOrder.UNKNOWN_SORT_INDEX) {
				throw new AssertionError("weaponTab missing sort index for " + shield);
			}
			int pos = order.indexOf(shield);
			if (pos < first || pos > last) {
				throw new AssertionError(shield + " must be inside weaponTab shield block");
			}
		}
		System.out.println("weapon_tab_shield_family_contiguous=true count=" + count);
	}

	private static void verifyCreativeWeaponShieldBlock() {
		ItemStack prev = null;
		for (String shield : REQUIRED_WEAPON_TAB_SHIELDS) {
			ItemStack stack = probeStack("hbm", shield);
			if (prev != null && CreativeTabSortHelper.compareStacks(prev, stack, "weaponTab") >= 0) {
				throw new AssertionError(
						"weaponTab shield order must follow order file: " + registryKey(prev) + " before " + shield);
			}
			prev = stack;
		}
		System.out.println("creative_weapon_shield_block=true count=" + REQUIRED_WEAPON_TAB_SHIELDS.length);
	}

	private static void verifyJeiWeaponShieldBlock() {
		ItemStack prev = null;
		for (String shield : REQUIRED_WEAPON_TAB_SHIELDS) {
			ItemStack stack = probeStack("hbm", shield);
			Integer idx = CreativeTabSortOrder.getExplicitSortIndex(
					stack.getItem().getRegistryName(), "weaponTab");
			if (idx == null) {
				throw new AssertionError("weaponTab missing explicit order entry for " + shield);
			}
			if (prev != null && HbmJeiIngredientSort.compare(prev, stack) >= 0) {
				throw new AssertionError(
						"JEI shield order must follow weaponTab block: " + registryKey(prev) + " before " + shield);
			}
			prev = stack;
		}
		System.out.println("jei_weapon_shield_block=true count=" + REQUIRED_WEAPON_TAB_SHIELDS.length);
	}

	private static void verifyWeaponTabArtilleryAmmo() {
		List<String> order = CreativeTabSortOrder.getTabRegistryOrder("weaponTab");
		int standardPos = order.indexOf("ammo_standard");
		int artyPos = order.indexOf("ammo_arty");
		int himarsPos = order.indexOf("ammo_himars");
		if (standardPos < 0 || artyPos < 0 || himarsPos < 0) {
			throw new AssertionError("weaponTab must contain ammo_standard, ammo_arty and ammo_himars");
		}
		int standardIdx = CreativeTabSortOrder.getSortIndex(probeStack("hbm", "ammo_standard"), "weaponTab");
		int artyIdx = CreativeTabSortOrder.getSortIndex(probeStack("hbm", "ammo_arty"), "weaponTab");
		int himarsIdx = CreativeTabSortOrder.getSortIndex(probeStack("hbm", "ammo_himars"), "weaponTab");
		if (standardIdx >= CreativeTabSortOrder.UNKNOWN_SORT_INDEX
				|| artyIdx >= CreativeTabSortOrder.UNKNOWN_SORT_INDEX
				|| himarsIdx >= CreativeTabSortOrder.UNKNOWN_SORT_INDEX) {
			throw new AssertionError("sedna ammo must have explicit sort indices");
		}
		int lastGunAmmo = -1;
		for (int i = 0; i < order.size(); i++) {
			String path = registryPath(order.get(i));
			if (path.startsWith("gun_") && path.contains("_ammo")) {
				lastGunAmmo = i;
			}
		}
		int legacyAmmo = order.indexOf("ammo_12gauge");
		int clipBf = order.indexOf("clip_bf");
		if (lastGunAmmo < 0 || legacyAmmo < 0 || clipBf < 0) {
			throw new AssertionError("weaponTab sedna ammo probes missing");
		}
		if (standardPos <= lastGunAmmo || himarsPos >= legacyAmmo) {
			throw new AssertionError("sedna ammo head must follow gun_ammo and precede legacy ammo_12gauge");
		}
		if (standardPos >= artyPos || artyPos >= himarsPos) {
			throw new AssertionError("sedna ammo order must be ammo_standard -> ammo_arty -> ammo_himars");
		}
		if (himarsPos >= clipBf) {
			throw new AssertionError("ammo_himars must precede clip_bf");
		}
		System.out.println(
				"weapon_tab_artillery_ammo=true standard=" + standardPos + " arty=" + artyPos + " himars=" + himarsPos);
	}

	private static final String[] MELEE_CLUSTER_ORDER = {
			"mese_pickaxe",
			"mese_axe",
			"dnt_sword",
			"dwarven_pickaxe",
			"mese_gavel",
	};

	private static final String[] PARTS_TAB_BEDROCK_JEI_BLOCK = {
			"bedrock_ore_new",
			"bedrock_ore_base",
			"bedrock_ore_fragment",
	};

	private static final String[] METEORITE_SWORD_DAMAGE_ORDER = {
			"meteorite_sword",
			"meteorite_sword_seared",
			"meteorite_sword_reforged",
			"meteorite_sword_hardened",
			"meteorite_sword_alloyed",
			"meteorite_sword_machined",
			"meteorite_sword_treated",
			"meteorite_sword_etched",
			"meteorite_sword_bred",
			"meteorite_sword_irradiated",
			"meteorite_sword_fused",
			"meteorite_sword_baleful",
			"meteorite_sword_warped",
			"meteorite_sword_demonic",
	};

	private static void verifyMeleeClusterPresent() {
		List<String> order = CreativeTabSortOrder.getTabRegistryOrder("weaponTab");
		int hs = order.indexOf("hs_sword");
		int shimmer = order.indexOf("shimmer_axe");
		int meteoriteBase = order.indexOf("meteorite_sword");
		if (hs < 0 || shimmer < 0 || meteoriteBase < 0) {
			throw new AssertionError("weaponTab missing melee position probes");
		}
		int mesePick = order.indexOf("mese_pickaxe");
		if (mesePick != shimmer + 1) {
			throw new AssertionError(
					"shimmer_axe must be immediately followed by mese_pickaxe, shimmer=" + shimmer + " mese_pick=" + mesePick);
		}
		int expected = mesePick;
		for (String entry : MELEE_CLUSTER_ORDER) {
			int idx = order.indexOf(entry);
			if (idx < 0) {
				throw new AssertionError("weaponTab missing melee cluster entry " + entry);
			}
			if (idx != expected) {
				throw new AssertionError(entry + " must follow melee cluster at " + expected + " got " + idx);
			}
			expected++;
		}
		int meseGavel = order.indexOf("mese_gavel");
		if (meteoriteBase != meseGavel + 1) {
			throw new AssertionError(
					"mese_gavel must be immediately followed by meteorite_sword, gavel=" + meseGavel + " meteorite=" + meteoriteBase);
		}
		int meteoriteFirst = -1;
		int meteoriteLast = -1;
		int meteoriteCount = 0;
		for (int i = 0; i < order.size(); i++) {
			String path = registryPath(order.get(i));
			if (path.equals("meteorite_sword") || path.startsWith("meteorite_sword_")) {
				meteoriteCount++;
				if (meteoriteFirst < 0) {
					meteoriteFirst = i;
				}
				meteoriteLast = i;
			}
		}
		if (meteoriteCount != METEORITE_SWORD_DAMAGE_ORDER.length) {
			throw new AssertionError(
					"weaponTab meteorite sword count must be " + METEORITE_SWORD_DAMAGE_ORDER.length + " got " + meteoriteCount);
		}
		if (meteoriteLast - meteoriteFirst + 1 != meteoriteCount) {
			throw new AssertionError("weaponTab meteorite swords must be contiguous");
		}
		System.out.println(
				"melee_cluster_order=true shimmer_adjacent_mese=" + (mesePick == shimmer + 1)
						+ " meteorite_after_mese_gavel=" + (meteoriteBase == meseGavel + 1)
						+ " meteorite_contiguous=" + (meteoriteLast - meteoriteFirst + 1 == meteoriteCount));
	}

	private static void verifyEnumMultiVariantOrder() {
		ItemStack pwr0 = probeStack("hbm", "pwr_fuel_depleted", 0);
		ItemStack pwr11 = probeStack("hbm", "pwr_fuel_depleted", 11);
		ItemStack battery0 = probeStack("hbm", "battery_pack", 0);
		ItemStack battery11 = probeStack("hbm", "battery_pack", 11);
		ItemStack watz0 = probeStack("hbm", "watz_pellet", 0);
		ItemStack watz8 = probeStack("hbm", "watz_pellet", 8);
		ItemStack rbmk0 = probeStack("hbm", "rbmk_pellet_leaus", 0);
		ItemStack rbmk8 = probeStack("hbm", "rbmk_pellet_leaus", 8);
		ItemStack mod0 = probeStack("hbm", "weapon_mod_test", 0);
		ItemStack mod9 = probeStack("hbm", "weapon_mod_test", 9);

		if (HbmJeiIngredientSort.compareForTab(pwr11, pwr0, "controlTab") <= 0
				|| HbmJeiIngredientSort.compareForTab(battery11, battery0, "controlTab") <= 0
				|| HbmJeiIngredientSort.compareForTab(watz8, watz0, "controlTab") <= 0
				|| HbmJeiIngredientSort.compareForTab(rbmk8, rbmk0, "controlTab") <= 0
				|| HbmJeiIngredientSort.compareForTab(mod9, mod0, "weaponTab") <= 0) {
			throw new AssertionError("enum-multi metadata variants must sort ascending by metadata");
		}

		NonNullList<ItemStack> pwrStacks = NonNullList.create();
		pwrStacks.add(pwr11);
		pwrStacks.add(pwr0);
		CreativeTabSortHelper.sortStacks(pwrStacks, "controlTab");
		if (pwrStacks.get(0).getMetadata() != 0 || pwrStacks.get(1).getMetadata() != 11) {
			throw new AssertionError("pwr_fuel_depleted variants must sort by metadata ascending");
		}

		NonNullList<ItemStack> batteryStacks = NonNullList.create();
		batteryStacks.add(battery11);
		batteryStacks.add(battery0);
		CreativeTabSortHelper.sortStacks(batteryStacks, "controlTab");
		if (batteryStacks.get(0).getMetadata() != 0 || batteryStacks.get(1).getMetadata() != 11) {
			throw new AssertionError("battery_pack variants must sort by metadata ascending");
		}
		System.out.println("enum_multi_variant_order=true");
	}

	private static final String[] ALLOY_REGISTRATION_ORDER = {
			"alloy_helmet",
			"alloy_plate",
			"alloy_legs",
			"alloy_boots",
			"alloy_sword",
			"alloy_pickaxe",
			"alloy_axe",
			"alloy_shovel",
			"alloy_hoe",
	};

	private static void verifyAlloyStableRegistrationOrder() {
		ItemStack axe = probeStack("hbm", "alloy_axe");
		ItemStack helmet = probeStack("hbm", "alloy_helmet");
		if (HbmJeiIngredientSort.compare(helmet, axe) >= 0
				|| HbmJeiIngredientSort.compareForTab(helmet, axe, "weaponTab") >= 0) {
			throw new AssertionError("unknown-index alloy items must follow registration order, not alphabetical");
		}

		NonNullList<ItemStack> tabStacks = NonNullList.create();
		for (String path : ALLOY_REGISTRATION_ORDER) {
			tabStacks.add(probeStack("hbm", path));
		}
		CreativeTabSortHelper.sortStacks(tabStacks, "weaponTab");
		for (int i = 0; i < ALLOY_REGISTRATION_ORDER.length; i++) {
			if (!ALLOY_REGISTRATION_ORDER[i].equals(registryKey(tabStacks.get(i)))) {
				throw new AssertionError(
						"tab sort must preserve registration order at index " + i + " got "
								+ registryKey(tabStacks.get(i)));
			}
		}

		NonNullList<ItemStack> search = NonNullList.create();
		for (String path : ALLOY_REGISTRATION_ORDER) {
			search.add(probeStack("hbm", path));
		}
		CreativeTabSearchSortHelper.sortSearchList(search);
		for (int i = 0; i < ALLOY_REGISTRATION_ORDER.length; i++) {
			if (!ALLOY_REGISTRATION_ORDER[i].equals(registryKey(search.get(i)))) {
				throw new AssertionError(
						"search must preserve registration order at index " + i + " got "
								+ registryKey(search.get(i)));
			}
		}
		System.out.println("alloy_stable_registration_order=true count=" + ALLOY_REGISTRATION_ORDER.length);
	}

	private static void verifyControlTabRbmkPelletBlock() {
		List<String> order = CreativeTabSortOrder.getTabRegistryOrder("controlTab");
		int drx = order.indexOf("rbmk_pellet_drx");
		int lecm = order.indexOf("hbmspace:rbmk_pellet_lecm");
		int ueu = order.indexOf("rbmk_fuel_ueu");
		if (drx < 0 || lecm < 0 || ueu < 0) {
			throw new AssertionError("controlTab missing rbmk pellet block entries");
		}
		if (drx >= lecm || lecm >= ueu) {
			throw new AssertionError("controlTab rbmk pellet block must precede fuel rods");
		}
		for (String path : new String[] {
				"rbmk_pellet_drx",
				"hbmspace:rbmk_pellet_lecm",
				"rbmk_fuel_hep"
		}) {
			int idx = CreativeTabSortOrder.getSortIndex(probeStackForKey(path), "controlTab");
			if (idx >= CreativeTabSortOrder.UNKNOWN_SORT_INDEX) {
				throw new AssertionError(path + " must have explicit controlTab sort index");
			}
		}
		System.out.println("control_tab_rbmk_pellet_block=true drx=" + drx + " lecm=" + lecm + " ueu=" + ueu);
	}

	private static void verifyControlTabRbmkFuelBlock() {
		List<String> order = CreativeTabSortOrder.getTabRegistryOrder("controlTab");
		int ueu = order.indexOf("rbmk_fuel_ueu");
		int test = order.indexOf("rbmk_fuel_test");
		int watz = order.indexOf("watz_pellet_depleted");
		int battery = order.indexOf("battery_pack");
		int pwr = order.indexOf("pwr_fuel_depleted");
		if (ueu < 0 || test < 0 || watz < 0 || battery < 0 || pwr < 0) {
			throw new AssertionError("controlTab missing rbmk/watz/battery/pwr entries");
		}
		if (ueu >= test || test >= watz || watz >= battery || battery >= pwr) {
			throw new AssertionError("controlTab rbmk/watz/battery/pwr block order broken");
		}
		for (String path : new String[] { "battery_pack", "pwr_fuel_depleted", "watz_pellet_depleted", "rbmk_fuel_test" }) {
			int idx = CreativeTabSortOrder.getSortIndex(probeStack("hbm", path), "controlTab");
			if (idx >= CreativeTabSortOrder.UNKNOWN_SORT_INDEX) {
				throw new AssertionError(path + " must have explicit controlTab sort index");
			}
		}
		System.out.println("control_tab_rbmk_fuel_block=true ueu=" + ueu + " test=" + test + " pwr=" + pwr);
	}

	private static void verifyRbmkPelletSearchContinuity() {
		NonNullList<ItemStack> search = NonNullList.create();
		search.add(probeStack("hbm", "rbmk_fuel_hep"));
		search.add(probeStack("hbm", "ammo_debug"));
		search.add(probeStack("hbmspace", "rbmk_pellet_lecm"));
		search.add(probeStack("hbm", "rbmk_pellet_drx", 9));
		search.add(probeStack("hbm", "rbmk_pellet_drx", 0));
		CreativeTabSearchSortHelper.sortSearchList(search);

		int drx9 = -1;
		int lecm = -1;
		for (int i = 0; i < search.size(); i++) {
			ItemStack stack = search.get(i);
			String key = registryKey(stack);
			if ("rbmk_pellet_drx".equals(key) && stack.getMetadata() == 9) {
				drx9 = i;
			} else if ("hbmspace:rbmk_pellet_lecm".equals(key)) {
				lecm = i;
			}
		}
		if (drx9 < 0 || lecm < 0 || drx9 + 1 != lecm) {
			throw new AssertionError(
					"rbmk_pellet_drx:9 must be immediately followed by hbmspace:rbmk_pellet_lecm, drx9="
							+ drx9 + " lecm=" + lecm);
		}
		if (indexOfRegistryPath(search, "ammo_debug") >= 0) {
			throw new AssertionError("search must exclude ammo_debug");
		}
		int hep = indexOfRegistryPath(search, "rbmk_fuel_hep");
		if (hep >= 0 && hep <= lecm) {
			throw new AssertionError("rbmk_fuel_hep must sort after hbmspace:rbmk_pellet_lecm");
		}
		System.out.println("rbmk_pellet_search_continuity=true");
	}

	private static void verifyBedrockOreFullGetSubItemsOrder() {
		ItemBedrockOreNew.BedrockOreType[] types = ItemBedrockOreNew.BedrockOreType.VALUES;
		ItemBedrockOreNew.BedrockOreGrade[] grades = ItemBedrockOreNew.BedrockOreGrade.VALUES;
		int[] expectedMeta = new int[types.length * grades.length];
		int k = 0;
		for (ItemBedrockOreNew.BedrockOreType type : types) {
			for (ItemBedrockOreNew.BedrockOreGrade grade : grades) {
				expectedMeta[k++] = grade.ordinal() << 8 | type.ordinal();
			}
		}

		NonNullList<ItemStack> search = NonNullList.create();
		NonNullList<ItemStack> partsTab = NonNullList.create();
		for (int i = expectedMeta.length - 1; i >= 0; i--) {
			ItemStack stack = probeStack("hbm", "bedrock_ore_new", expectedMeta[i]);
			search.add(stack);
			partsTab.add(stack.copy());
		}
		CreativeTabSearchSortHelper.sortSearchList(search);
		CreativeTabSortHelper.sortStacks(partsTab, "partsTab");

		for (int i = 0; i < expectedMeta.length; i++) {
			if (search.get(i).getMetadata() != expectedMeta[i]) {
				throw new AssertionError(
						"search bedrock full getSubItems order broken at " + i + " expected="
								+ expectedMeta[i] + " got=" + search.get(i).getMetadata());
			}
			if (partsTab.get(i).getMetadata() != expectedMeta[i]) {
				throw new AssertionError(
						"partsTab bedrock full getSubItems order broken at " + i + " expected="
								+ expectedMeta[i] + " got=" + partsTab.get(i).getMetadata());
			}
		}
		System.out.println("bedrock_ore_full_getsubitems_order=true count=" + expectedMeta.length);
	}

	private static void verifyVariantScatterSearchOrder() {
		NonNullList<ItemStack> search = NonNullList.create();
		search.add(probeStack("hbm", "pwr_fuel_depleted", 13));
		search.add(probeStack("hbm", "battery_pack", 11));
		search.add(probeStack("hbm", "watz_pellet_depleted", 20));
		search.add(probeStack("hbm", "battery_pack", 2));
		search.add(probeStack("hbm", "weapon_mod_test", 9));
		search.add(probeStack("hbm", "battery_pack", 0));
		search.add(probeStack("hbmspace", "hard_drive_full", 10));
		search.add(probeStack("hbm", "weapon_mod_test", 0));
		CreativeTabSearchSortHelper.sortSearchList(search);

		assertRegistryBlockContiguous(search, "watz_pellet_depleted", new int[] { 20 });
		assertRegistryBlockContiguous(search, "battery_pack", new int[] { 0, 2, 11 });
		assertRegistryBlockContiguous(search, "pwr_fuel_depleted", new int[] { 13 });
		assertRegistryBlockContiguous(search, "weapon_mod_test", new int[] { 0, 9 });
		assertRegistryBlockContiguous(search, "hbmspace:hard_drive_full", new int[] { 10 });

		int testIdx = indexOfRegistryPath(search, "rbmk_fuel_test");
		int gasIdx = indexOfRegistryPath(search, "gas_mask_filter");
		if (testIdx < 0 || gasIdx < 0) {
			search.add(probeStack("hbm", "rbmk_fuel_test"));
			search.add(probeStack("hbm", "gas_mask_filter"));
			CreativeTabSearchSortHelper.sortSearchList(search);
			testIdx = indexOfRegistryPath(search, "rbmk_fuel_test");
			gasIdx = indexOfRegistryPath(search, "gas_mask_filter");
		}
		if (testIdx < 0 || gasIdx < 0 || testIdx >= gasIdx) {
			throw new AssertionError("rbmk_fuel_test must precede gas_mask_filter in search order");
		}
		System.out.println("variant_scatter_search_order=true rbmk_before_gas=" + (testIdx < gasIdx));
	}

	private static void verifyMissileThrusterBlock() {
		List<String> order = CreativeTabSortOrder.getTabRegistryOrder("missileTab");
		int keroseneTriple = order.indexOf("mp_thruster_20_kerosene_triple");
		int methalox = order.indexOf("hbmspace:mp_thruster_20_methalox");
		int hydrogenTriple = order.indexOf("hbmspace:mp_thruster_20_hydrogen_triple");
		int solid = order.indexOf("mp_thruster_20_solid");
		int stability = order.indexOf("mp_stability_10_flat");
		if (keroseneTriple < 0 || methalox < 0 || hydrogenTriple < 0 || solid < 0 || stability < 0) {
			throw new AssertionError("missileTab missing thruster block entries");
		}
		if (!(keroseneTriple < methalox && methalox < hydrogenTriple && hydrogenTriple < solid && solid < stability)) {
			throw new AssertionError("missileTab 20-size thrusters must stay contiguous before stability fins");
		}
		System.out.println("missile_thruster_block=true");
	}

	private static void verifyControlTabSpaceRbmkFuelRods() {
		List<String> order = CreativeTabSortOrder.getTabRegistryOrder("controlTab");
		int drx = order.indexOf("rbmk_fuel_drx");
		int bk247 = order.indexOf("hbmspace:rbmk_fuel_bk247");
		int hecm = order.indexOf("hbmspace:rbmk_fuel_hecm");
		int test = order.indexOf("rbmk_fuel_test");
		if (drx < 0 || bk247 < 0 || hecm < 0 || test < 0) {
			throw new AssertionError("controlTab missing space rbmk fuel rod entries");
		}
		if (!(drx < bk247 && bk247 < hecm && hecm < test)) {
			throw new AssertionError("space rbmk fuel rods must follow rbmk_fuel_drx and precede rbmk_fuel_test");
		}
		System.out.println("control_tab_space_rbmk_fuel_rods=true");
	}

	private static void verifyMachineTabRbmkBurnerPlacement() {
		ItemStack heater = probeStack("hbm", "rbmk_heater");
		ItemStack burner = probeStackForKey("hbmspace:rbmk_burner");
		ItemStack reflector = probeStack("hbm", "rbmk_reflector");
		int burnerIdx = CreativeTabSortOrder.getSortIndex(burner, "machineTab");
		if (burnerIdx != 92) {
			throw new AssertionError("hbmspace:rbmk_burner must use machineTab index 92, got " + burnerIdx);
		}
		if (CreativeTabSortHelper.compareStacks(heater, burner, "machineTab") >= 0) {
			throw new AssertionError("hbmspace:rbmk_burner must sort after rbmk_heater on machineTab");
		}
		if (CreativeTabSortHelper.compareStacks(burner, reflector, "machineTab") >= 0) {
			throw new AssertionError("hbmspace:rbmk_burner must sort before rbmk_reflector on machineTab");
		}
		System.out.println("machine_tab_rbmk_burner=true");
	}

	private static void verifyHardDriveControlTabPlacement() {
		List<String> control = CreativeTabSortOrder.getTabRegistryOrder("controlTab");
		List<String> parts = CreativeTabSortOrder.getTabRegistryOrder("partsTab");
		int full = control.indexOf("hbmspace:hard_drive_full");
		int empty = control.indexOf("hbmspace:hard_drive");
		if (full < 0 || empty < 0 || empty != full + 1) {
			throw new AssertionError("hbmspace:hard_drive must immediately follow hbmspace:hard_drive_full on controlTab");
		}
		if (parts.contains("hbmspace:hard_drive")) {
			throw new AssertionError("hbmspace:hard_drive must not remain on partsTab order");
		}
		System.out.println("hard_drive_control_tab=true");
	}

	private static void verifySellafieldOreResourceTab() {
		List<String> order = CreativeTabSortOrder.getTabRegistryOrder("resourceTab");
		int meteor = order.indexOf("ore_meteor_starmetal");
		int emerald = order.indexOf("ore_sellafield_emerald");
		int radgem = order.indexOf("ore_sellafield_radgem");
		int tektite = order.indexOf("tektite");
		if (meteor < 0 || emerald < 0 || radgem < 0 || tektite < 0) {
			throw new AssertionError("resourceTab missing sellafield ore block");
		}
		if (!(meteor < emerald && radgem < tektite && emerald + 4 == radgem)) {
			throw new AssertionError("sellafield ores must follow meteor ores as one contiguous block");
		}
		System.out.println("sellafield_ore_resource_tab=true");
	}

	private static void verifyOreMeteorResourceTab() {
		List<String> order = CreativeTabSortOrder.getTabRegistryOrder("resourceTab");
		int starmetal = order.indexOf("ore_meteor_starmetal");
		int meteor = order.indexOf("ore_meteor");
		int emerald = order.indexOf("ore_sellafield_emerald");
		if (starmetal < 0 || meteor < 0 || emerald < 0) {
			throw new AssertionError("resourceTab missing ore_meteor placement probes");
		}
		if (!(starmetal < meteor && meteor + 1 == emerald)) {
			throw new AssertionError("ore_meteor must follow ore_meteor_starmetal and precede sellafield ores");
		}
		int idx = CreativeTabSortOrder.getSortIndex(probeStack("hbm", "ore_meteor"), "resourceTab");
		int starmetalIdx = CreativeTabSortOrder.getSortIndex(probeStack("hbm", "ore_meteor_starmetal"), "resourceTab");
		if (idx >= CreativeTabSortOrder.UNKNOWN_SORT_INDEX || starmetalIdx >= CreativeTabSortOrder.UNKNOWN_SORT_INDEX) {
			throw new AssertionError("ore_meteor entries must have explicit resourceTab sort indices");
		}
		if (starmetalIdx >= idx) {
			throw new AssertionError("ore_meteor_starmetal sort index must precede ore_meteor");
		}
		System.out.println("ore_meteor_resource_tab=true index=" + idx + " position=" + meteor);
	}

	private static void verifyBlockTabConcreteColored() {
		List<String> order = CreativeTabSortOrder.getTabRegistryOrder("blockTab");
		int concrete = order.indexOf("concrete_colored");
		int rebar = order.indexOf("rebar");
		int ext = order.indexOf("concrete_colored_ext");
		if (concrete < 0 || rebar < 0) {
			throw new AssertionError("blockTab missing concrete_colored block entries");
		}
		if (concrete + 1 != rebar) {
			throw new AssertionError("concrete_colored must be immediately followed by rebar");
		}
		if (ext >= 0 && ext >= concrete) {
			throw new AssertionError("concrete_colored_ext must precede concrete_colored in blockTab");
		}
		int idx = CreativeTabSortOrder.getSortIndex(probeStack("hbm", "concrete_colored"), "blockTab");
		if (idx >= CreativeTabSortOrder.UNKNOWN_SORT_INDEX) {
			throw new AssertionError("concrete_colored must have explicit blockTab sort index");
		}
		NonNullList<ItemStack> stacks = NonNullList.create();
		for (int meta = 5; meta >= 0; meta--) {
			stacks.add(probeStack("hbm", "concrete_colored", meta));
		}
		stacks.add(probeStack("hbm", "rebar"));
		CreativeTabSortHelper.sortStacks(stacks, "blockTab");
		assertRegistryBlockContiguous(stacks, "concrete_colored", new int[] {0, 1, 2, 3, 4, 5});
		System.out.println("block_tab_concrete_colored=true index=" + idx + " position=" + concrete);
	}

	private static void verifyBlockTabLampTritium() {
		List<String> order = CreativeTabSortOrder.getTabRegistryOrder("blockTab");
		int green = order.indexOf("lamp_tritium_green_off");
		int blue = order.indexOf("lamp_tritium_blue_off");
		int vinyl = order.indexOf("vinyl_tile");
		int computer = order.indexOf("deco_computer");
		if (green < 0 || blue < 0 || vinyl < 0 || computer < 0) {
			throw new AssertionError("blockTab missing lamp_tritium placement probes");
		}
		if (!(vinyl < green && green + 1 == blue && blue + 1 == computer)) {
			throw new AssertionError("lamp_tritium_off variants must be contiguous between vinyl_tile and deco_computer");
		}
		for (String path : new String[] {"lamp_tritium_green_off", "lamp_tritium_blue_off"}) {
			int idx = CreativeTabSortOrder.getSortIndex(probeStack("hbm", path), "blockTab");
			if (idx >= CreativeTabSortOrder.UNKNOWN_SORT_INDEX) {
				throw new AssertionError(path + " must have explicit blockTab sort index");
			}
		}
		System.out.println("block_tab_lamp_tritium=true green=" + green + " blue=" + blue);
	}

	private static void verifyCreativeTabExclusions() {
		if (!CreativeTabExclusions.isExcluded(probeStack("hbm", "ammo_debug"))) {
			throw new AssertionError("ammo_debug must be excluded from creative tabs");
		}
		if (!CreativeTabExclusions.isExcluded(probeStack("hbm", "pellet_rtg_depleted", 0))) {
			throw new AssertionError("pellet_rtg_depleted must be excluded from creative tabs");
		}
		if (!CreativeTabExclusions.isExcluded(probeStack("hbm", "wand_air"))) {
			throw new AssertionError("wand_air must be excluded from creative tabs");
		}
		NonNullList<ItemStack> search = NonNullList.create();
		search.add(probeStack("hbm", "pellet_rtg_depleted_bismuth"));
		search.add(probeStack("hbm", "pellet_rtg_depleted", 0));
		CreativeTabSearchSortHelper.sortSearchList(search);
		if (indexOfRegistryPath(search, "pellet_rtg_depleted") >= 0) {
			throw new AssertionError("search must exclude enum pellet_rtg_depleted variants");
		}
		if (indexOfRegistryPath(search, "pellet_rtg_depleted_bismuth") < 0) {
			throw new AssertionError("search must keep lore pellet_rtg_depleted_bismuth");
		}
		System.out.println("creative_tab_exclusions=true");
	}

	private static void assertRegistryBlockContiguous(NonNullList<ItemStack> list, String registryPath, int[] expectedMeta) {
		int first = -1;
		int last = -1;
		int count = 0;
		for (int i = 0; i < list.size(); i++) {
			ItemStack stack = list.get(i);
			String key = registryKey(stack);
			if (!registryPath.equals(key)) {
				continue;
			}
			count++;
			if (first < 0) {
				first = i;
			}
			last = i;
			if (count <= expectedMeta.length && stack.getMetadata() != expectedMeta[count - 1]) {
				throw new AssertionError(
						registryPath + " meta order broken at slot " + i + " expected="
								+ expectedMeta[count - 1] + " got=" + stack.getMetadata());
			}
		}
		if (count != expectedMeta.length || first < 0 || last - first + 1 != count) {
			throw new AssertionError(
					registryPath + " must form one contiguous metadata block, count=" + count + " span="
							+ (last - first + 1));
		}
	}

	private static void verifyBedrockOreTypeMajorVariantOrder() {
		ItemStack type6Grade0 = probeStack("hbm", "bedrock_ore_new", 6);
		ItemStack type6Grade1 = probeStack("hbm", "bedrock_ore_new", 262);
		ItemStack type6Grade2 = probeStack("hbm", "bedrock_ore_new", 518);
		ItemStack type0Grade0 = probeStack("hbm", "bedrock_ore_new", 0);
		ItemStack type1Grade0 = probeStack("hbm", "bedrock_ore_new", 1);

		NonNullList<ItemStack> stacks = NonNullList.create();
		stacks.add(type6Grade2);
		stacks.add(type0Grade0);
		stacks.add(type6Grade0);
		stacks.add(type1Grade0);
		stacks.add(type6Grade1);
		CreativeTabSortHelper.sortStacks(stacks, "partsTab");
		int[] expectedMeta = {0, 1, 6, 262, 518};
		for (int i = 0; i < expectedMeta.length; i++) {
			if (stacks.get(i).getMetadata() != expectedMeta[i]) {
				throw new AssertionError(
						"partsTab bedrock sort meta index=" + i + " expected=" + expectedMeta[i]
								+ " got=" + stacks.get(i).getMetadata());
			}
		}
		System.out.println("bedrock_ore_type_major_order=true");
	}

	private static void verifyBedrockOreType6ProcessingChain() {
		int[] expectedMeta = new int[26];
		for (int grade = 0; grade < expectedMeta.length; grade++) {
			expectedMeta[grade] = grade << 8 | 6;
		}

		int[] userChain = {6, 262, 518, 774, 1030, 1286, 1542};
		for (int i = 0; i < userChain.length; i++) {
			if (expectedMeta[i] != userChain[i]) {
				throw new AssertionError(
						"type6 user chain mismatch at " + i + " expected=" + userChain[i]
								+ " got=" + expectedMeta[i]);
			}
		}

		NonNullList<ItemStack> search = NonNullList.create();
		for (int i = expectedMeta.length - 1; i >= 0; i--) {
			search.add(probeStack("hbm", "bedrock_ore_new", expectedMeta[i]));
		}
		search.add(probeStack("hbm", "ingot_uranium"));
		CreativeTabSearchSortHelper.sortSearchList(search);
		assertRegistryBlockContiguous(search, "bedrock_ore_new", expectedMeta);

		NonNullList<ItemStack> partsTab = NonNullList.create();
		for (int i = expectedMeta.length - 1; i >= 0; i--) {
			partsTab.add(probeStack("hbm", "bedrock_ore_new", expectedMeta[i]));
		}
		CreativeTabSortHelper.sortStacks(partsTab, "partsTab");
		assertRegistryBlockContiguous(partsTab, "bedrock_ore_new", expectedMeta);

		if (expectedMeta[0] != 6 || expectedMeta[1] != 262 || expectedMeta[25] != 6406) {
			throw new AssertionError(
					"type6 chain must start at meta 6, then 262, end at 6406; got "
							+ expectedMeta[0] + "," + expectedMeta[1] + "," + expectedMeta[25]);
		}
		System.out.println("bedrock_ore_type6_chain=true grades=" + expectedMeta.length + " end_meta=" + expectedMeta[25]);
	}

	private static void verifySearchTabGlobalOrder() {
		NonNullList<ItemStack> search = buildSimulatedSearchList();
		CreativeTabSearchSortHelper.sortSearchList(search);

		ItemStack prevHbm = null;
		String prevKey = null;
		for (ItemStack stack : search) {
			if (!HbmJeiIngredientSort.isHbmSortedNamespace(stack)) {
				continue;
			}
			String key = registryKey(stack);
			if (prevHbm != null && HbmJeiIngredientSort.compare(prevHbm, stack) > 0) {
				throw new AssertionError(
						"search global HBM order broken: " + prevKey + " before " + key);
			}
			if (prevHbm != null && prevKey != null && prevKey.equals(key)
					&& HbmJeiIngredientSort.compare(prevHbm, stack) != 0) {
				throw new AssertionError("duplicate registry path must sort deterministically: " + key);
			}
			prevHbm = stack;
			prevKey = key;
		}

		int rodIdx = indexOfRegistryPath(search, "rod_weidanium");
		int schrabShieldIdx = indexOfRegistryPath(search, "schrabidium_shield");
		if (rodIdx < 0 || schrabShieldIdx < 0) {
			throw new AssertionError("search probes missing rod_weidanium or schrabidium_shield");
		}
		if (rodIdx >= schrabShieldIdx) {
			throw new AssertionError("rod_weidanium must precede schrabidium_shield in global search order");
		}
		if (Math.abs(rodIdx - schrabShieldIdx) <= 1) {
			throw new AssertionError(
					"full search list must separate rod_weidanium and schrabidium_shield after sort");
		}

		verifySearchIncreasing(search, new String[] {
				"diamond_gavel",
				"alloy_shield",
				"schrabidium_shield",
				"starmetal_shield",
				"steel_shield",
				"titanium_shield",
				"grenade_generic",
		});
		verifySearchIncreasing(search, new String[] {
				"powder_ac227",
				"powder_ac227_tiny",
		});
		verifySearchIncreasing(search, new String[] {
				"ingot_ac227",
				"ingot_neodymium",
				"ingot_radspice",
				"ingot_strontium",
		});
		verifySearchIncreasing(search, new String[] {
				"gas_mask_filter_piss",
				"gas_mask_filter_radon",
				"gas_mask_filter_rag",
		});

		System.out.println(
				"search_tab_global_order=true hbm_items=" + countHbmStacks(search)
						+ " rod_vs_shield_gap=" + Math.abs(rodIdx - schrabShieldIdx));
	}

	private static final String[] HBM_TAB_SEARCH_ORDER = {
			"partsTab",
			"controlTab",
			"templateTab",
			"resourceTab",
			"blockTab",
			"machineTab",
			"nukeTab",
			"missileTab",
			"weaponTab",
			"consumableTab",
	};

	private static NonNullList<ItemStack> buildSimulatedSearchList() {
		NonNullList<ItemStack> list = NonNullList.create();
		for (String tabKey : HBM_TAB_SEARCH_ORDER) {
			List<String> keys = CreativeTabSortOrder.getTabRegistryOrder(tabKey);
			for (int i = keys.size() - 1; i >= 0; i--) {
				list.add(probeStackForKey(keys.get(i)));
			}
		}
		return list;
	}

	private static void verifySearchIncreasing(NonNullList<ItemStack> search, String[] paths) {
		ItemStack prev = null;
		for (String path : paths) {
			ItemStack stack = probeStack("hbm", path);
			int idx = indexOfRegistryPath(search, path);
			if (idx < 0) {
				throw new AssertionError("search list missing probe " + path);
			}
			ItemStack actual = search.get(idx);
			if (prev != null && HbmJeiIngredientSort.compare(prev, actual) > 0) {
				throw new AssertionError(
						"search order for " + path + " must follow " + registryKey(prev));
			}
			prev = actual;
		}
	}

	private static int indexOfRegistryPath(NonNullList<ItemStack> list, String path) {
		for (int i = 0; i < list.size(); i++) {
			if (path.equals(registryKey(list.get(i)))) {
				return i;
			}
		}
		return -1;
	}

	private static int countHbmStacks(NonNullList<ItemStack> list) {
		int count = 0;
		for (ItemStack stack : list) {
			if (HbmJeiIngredientSort.isHbmSortedNamespace(stack)) {
				count++;
			}
		}
		return count;
	}

	private static void verifyPartsTabBedrockJeiBlock() {
		List<String> order = CreativeTabSortOrder.getTabRegistryOrder("partsTab");
		int first = -1;
		int last = -1;
		int prevPos = -1;
		for (int i = 0; i < PARTS_TAB_BEDROCK_JEI_BLOCK.length; i++) {
			String path = PARTS_TAB_BEDROCK_JEI_BLOCK[i];
			int pos = order.indexOf(path);
			if (pos < 0) {
				throw new AssertionError("partsTab missing bedrock JEI block entry " + path);
			}
			if (prevPos >= 0) {
				if (pos != prevPos + 1) {
					throw new AssertionError("partsTab bedrock JEI block must be contiguous at " + path);
				}
			}
			if (first < 0) {
				first = pos;
			}
			last = pos;
			prevPos = pos;
		}
		System.out.println(
				"parts_tab_bedrock_jei_block=true count=" + PARTS_TAB_BEDROCK_JEI_BLOCK.length
						+ " first=" + first + " last=" + last);
	}

	private static void verifyMeteoriteSwordDamageOrder() {
		List<String> order = CreativeTabSortOrder.getTabRegistryOrder("weaponTab");
		int first = -1;
		int last = -1;
		int prevPos = -1;
		int prevIdx = -1;
		for (int i = 0; i < METEORITE_SWORD_DAMAGE_ORDER.length; i++) {
			String path = METEORITE_SWORD_DAMAGE_ORDER[i];
			int pos = order.indexOf(path);
			if (pos < 0) {
				throw new AssertionError("weaponTab missing meteorite sword " + path);
			}
			int sortIdx = CreativeTabSortOrder.getSortIndex(probeStack("hbm", path), "weaponTab");
			if (sortIdx >= CreativeTabSortOrder.UNKNOWN_SORT_INDEX) {
				throw new AssertionError(path + " must have explicit sort index");
			}
			if (prevPos >= 0) {
				if (pos != prevPos + 1) {
					throw new AssertionError("meteorite swords must be contiguous in tab order at " + path);
				}
				if (sortIdx <= prevIdx) {
					throw new AssertionError("meteorite sword sort indices must increase by damage at " + path);
				}
			}
			if (first < 0) {
				first = pos;
			}
			last = pos;
			prevPos = pos;
			prevIdx = sortIdx;
		}
		int meseGavel = order.indexOf("mese_gavel");
		if (meseGavel >= 0 && first != meseGavel + 1) {
			throw new AssertionError("meteorite_sword must immediately follow mese_gavel");
		}
		ItemStack prev = null;
		for (String path : METEORITE_SWORD_DAMAGE_ORDER) {
			ItemStack stack = probeStack("hbm", path);
			if (prev != null && CreativeTabSortHelper.compareStacks(prev, stack, "weaponTab") >= 0) {
				throw new AssertionError(
						"weaponTab meteorite order must follow damage: " + registryKey(prev) + " before " + path);
			}
			prev = stack;
		}
		prev = null;
		for (String path : METEORITE_SWORD_DAMAGE_ORDER) {
			ItemStack stack = probeStack("hbm", path);
			if (prev != null && HbmJeiIngredientSort.compare(prev, stack) >= 0) {
				throw new AssertionError(
						"JEI meteorite order must follow damage: " + registryKey(prev) + " before " + path);
			}
			prev = stack;
		}
		System.out.println(
				"meteorite_sword_damage_order=true count=" + METEORITE_SWORD_DAMAGE_ORDER.length
						+ " first=" + first + " last=" + last);
	}

	private static final String[] POWER_ARMOR_SET_HELMETS = {
			"cmb_helmet",
			"schrabidium_helmet",
			"t51_helmet",
			"ajr_helmet",
			"ajro_helmet",
			"hev_helmet",
			"bj_helmet",
			"t45_helmet",
	};

	private static final String[] POWER_ARMOR_PREFIXES = {
			"cmb_",
			"schrabidium_",
			"t51_",
			"ajr_",
			"ajro_",
			"hev_",
			"bj_",
			"t45_",
	};

	private static void verifyModItemsPowerArmorDeclOrder() throws IOException {
		List<String> fields = readModItemsFieldOrder();
		int bjBoots = fields.indexOf("bj_boots");
		int t45Helmet = fields.indexOf("t45_helmet");
		int t45Boots = fields.indexOf("t45_boots");
		int rpaHelmet = fields.indexOf("rpa_helmet");
		if (bjBoots < 0 || t45Helmet < 0 || t45Boots < 0 || rpaHelmet < 0) {
			throw new AssertionError("power armor decl markers missing in ModItems.java");
		}
		if (t45Helmet != bjBoots + 1) {
			throw new AssertionError("t45_helmet must immediately follow bj_boots in ModItems.java");
		}
		if (t45Helmet >= rpaHelmet) {
			throw new AssertionError("t45 block must precede rpa_helmet in ModItems.java");
		}

		int prevHelmet = -1;
		for (String helmet : POWER_ARMOR_SET_HELMETS) {
			int idx = fields.indexOf(helmet);
			if (idx < 0) {
				throw new AssertionError("power armor helmet missing: " + helmet);
			}
			if (idx <= prevHelmet) {
				throw new AssertionError("power armor helmet order broken at " + helmet + " idx=" + idx);
			}
			prevHelmet = idx;
		}

		int cmbHelmet = fields.indexOf("cmb_helmet");
		List<Integer> powerIndices = new ArrayList<Integer>();
		for (int i = cmbHelmet; i <= t45Boots; i++) {
			String name = fields.get(i);
			if (!matchesPowerArmorPrefix(name)) {
				throw new AssertionError(
						"non-power-armor field inside power armor block: " + name + " at index " + i);
			}
			powerIndices.add(i);
		}
		if (powerIndices.size() != t45Boots - cmbHelmet + 1) {
			throw new AssertionError("power armor block span mismatch");
		}

		System.out.println(
				"moditems_power_armor_block_contiguous=true helmets="
						+ POWER_ARMOR_SET_HELMETS.length
						+ " combat_tab=registration_order_only");
	}

	private static boolean matchesPowerArmorPrefix(String fieldName) {
		for (String prefix : POWER_ARMOR_PREFIXES) {
			if (fieldName.startsWith(prefix)) {
				return true;
			}
		}
		return false;
	}

	private static List<String> readModItemsFieldOrder() throws IOException {
		Path modItems = Paths.get("src/main/java/com/hbm/items/ModItems.java");
		if (!Files.exists(modItems)) {
			modItems = Paths.get(System.getProperty("user.dir"), "src/main/java/com/hbm/items/ModItems.java");
		}
		String text = new String(Files.readAllBytes(modItems), StandardCharsets.UTF_8);
		List<String> fields = new ArrayList<String>();
		java.util.regex.Matcher m = java.util.regex.Pattern
				.compile("public static final (?:Item|Item\\w+)\\s+(\\w+)\\s*=")
				.matcher(text);
		while (m.find()) {
			fields.add(m.group(1));
		}
		return fields;
	}

	private static String registryPath(String key) {
		if (key == null) {
			return "";
		}
		int colon = key.indexOf(':');
		return colon >= 0 ? key.substring(colon + 1) : key;
	}

	private static void verifyTabPipelineCollectSortAppend() {
		final List<String> steps = new ArrayList<String>();
		HBMCreativeTab tab = new HBMCreativeTab(0, "probe", "weaponTab") {
			@Override
			protected void collectItems(NonNullList<ItemStack> list) {
				steps.add("collect");
				list.add(probeStack("hbm", "gun_deagle"));
				list.add(probeStack("hbm", "gun_revolver"));
			}

			@Override
			protected void sortStacks(NonNullList<ItemStack> list) {
				steps.add("sort");
				super.sortStacks(list);
			}

			@Override
			protected void appendTabExtras(NonNullList<ItemStack> list) {
				steps.add("append");
				list.add(probeStack("hbm", "gun_greasegun"));
			}

			@Override
			public ItemStack createIcon() {
				return ItemStack.EMPTY;
			}
		};

		NonNullList<ItemStack> list = NonNullList.create();
		tab.displayAllRelevantItems(list);
		if (!Arrays.asList("collect", "sort", "append").equals(steps)) {
			throw new AssertionError("HBMCreativeTab pipeline must be collect->sort->append, got " + steps);
		}
		if (list.isEmpty() || !"gun_greasegun".equals(registryKey(list.get(list.size() - 1)))) {
			throw new AssertionError("appendTabExtras must run after sort and leave appended item at tab end");
		}
		System.out.println("tab_pipeline_order=collect,sort,append append_after_sort=true");
	}

	private static void verifyCompareStacksNoIndexTieBreak() {
		ItemStack a = batteryStack("battery_steam", 0L);
		ItemStack b = batteryStack("battery_steam", 60000L);
		if (CreativeTabSortHelper.compareStacks(a, b, "controlTab") != 0) {
			throw new AssertionError("identical registry/metadata stacks must compare equal (no index tie-break)");
		}
	}

	private static void verifyRegistryPathLookup() {
		int decoIdx = CreativeTabSortOrder.getSortIndex(new ResourceLocation("hbm", "deco_sat_mapper"), "blockTab");
		int chipIdx = CreativeTabSortOrder.getSortIndex(new ResourceLocation("hbm", "sat_mapper"), "missileTab");
		if (decoIdx == chipIdx) {
			throw new AssertionError("deco_sat_mapper and sat_mapper must not share sort index");
		}
		if (decoIdx >= 500_000 || chipIdx >= 500_000) {
			throw new AssertionError("known items must resolve to tab-local indices");
		}
	}

	private static void verifyItemStackSortIndexPath() {
		ItemStack decoBlock = probeStack("hbm", "deco_sat_mapper");
		ItemStack satChip = probeStack("hbm", "sat_mapper");
		int directDeco = CreativeTabSortOrder.getSortIndex(new ResourceLocation("hbm", "deco_sat_mapper"), "blockTab");
		int directChip = CreativeTabSortOrder.getSortIndex(new ResourceLocation("hbm", "sat_mapper"), "missileTab");
		int stackDeco = CreativeTabSortOrder.getSortIndex(decoBlock, "blockTab");
		int stackChip = CreativeTabSortOrder.getSortIndex(satChip, "missileTab");
		if (directDeco != stackDeco || directChip != stackChip) {
			throw new AssertionError("ItemStack.getSortIndex must match ResourceLocation lookup");
		}
	}

	private static void verifyUnknownLexicalSort() {
		List<String> keys = new ArrayList<String>();
		keys.add("hbmspace:zebra_probe");
		keys.add("hbmspace:alpha_probe");
		keys.add("hbmspace:mike_probe");
		CreativeTabSortHelper.sortRegistryKeys(keys, "weaponTab");
		expectKey(keys.get(0), "hbmspace:alpha_probe");
		expectKey(keys.get(1), "hbmspace:mike_probe");
		expectKey(keys.get(2), "hbmspace:zebra_probe");
	}

	private static void verifyUnknownItemStackSort() {
		NonNullList<ItemStack> stacks = NonNullList.create();
		stacks.add(probeStack("hbmspace", "zebra_probe"));
		stacks.add(probeStack("hbmspace", "alpha_probe"));
		stacks.add(probeStack("hbmspace", "mike_probe"));
		CreativeTabSortHelper.sortStacks(stacks, "weaponTab");
		expectRegistry(stacks.get(0), "hbmspace", "zebra_probe");
		expectRegistry(stacks.get(1), "hbmspace", "alpha_probe");
		expectRegistry(stacks.get(2), "hbmspace", "mike_probe");
	}

	private static void verifyScrambledTabsRestoreOrder() {
		for (String tabKey : TAB_KEYS) {
			List<String> expected = CreativeTabSortOrder.getTabRegistryOrder(tabKey);
			if (expected.isEmpty()) {
				continue;
			}
			List<String> scrambled = new ArrayList<String>(expected);
			java.util.Collections.reverse(scrambled);
			CreativeTabSortHelper.sortRegistryKeys(scrambled, tabKey);
			for (int i = 0; i < expected.size(); i++) {
				if (!expected.get(i).equals(scrambled.get(i))) {
					throw new AssertionError(
							"tab=" + tabKey + " index=" + i + " expected=" + expected.get(i) + " actual=" + scrambled.get(i));
				}
			}
		}
	}

	private static void verifyScrambledItemStacksRestoreOrder() {
		List<String> sampleKeys = CreativeTabSortOrder.getTabRegistryOrder("weaponTab");
		if (sampleKeys.size() < 5) {
			throw new AssertionError("weaponTab sample too small");
		}
		List<String> subset = sampleKeys.subList(0, Math.min(20, sampleKeys.size()));
		NonNullList<ItemStack> stacks = NonNullList.create();
		for (int i = subset.size() - 1; i >= 0; i--) {
			stacks.add(probeStackForKey(subset.get(i)));
		}
		CreativeTabSortHelper.sortStacks(stacks, "weaponTab");
		for (int i = 0; i < subset.size(); i++) {
			String expected = subset.get(i);
			String actual = registryKey(stacks.get(i));
			if (!expected.equals(actual)) {
				throw new AssertionError("ItemStack sort mismatch index=" + i + " expected=" + expected + " actual=" + actual);
			}
		}
	}

	private static void verifyBatteryAdjacencyViaItemStacks() {
		NonNullList<ItemStack> stacks = NonNullList.create();
		stacks.add(probeStack("hbm", "gun_revolver"));
		stacks.add(batteryStack("battery_steam", 0L));
		stacks.add(probeStack("hbm", "gun_deagle"));
		stacks.add(batteryStack("battery_steam", 60000L));
		CreativeTabSortHelper.sortStacks(stacks, "controlTab");

		int firstBattery = -1;
		int lastBattery = -1;
		for (int i = 0; i < stacks.size(); i++) {
			ResourceLocation reg = stacks.get(i).getItem().getRegistryName();
			if (reg != null && "battery_steam".equals(reg.getPath())) {
				if (firstBattery < 0) {
					firstBattery = i;
				}
				lastBattery = i;
			}
		}
		if (firstBattery < 0 || lastBattery < 0 || firstBattery + 1 != lastBattery) {
			throw new AssertionError("battery_steam ItemStacks must remain adjacent after sortStacks");
		}
	}

	private static void writeExecutionEvidence(Path out) throws IOException {
		StringBuilder sb = new StringBuilder();
		sb.append("source=CreativeTabSortVerifier.main\n");
		sb.append("classes=CreativeTabSortOrder,CreativeTabSortHelper,HBMCreativeTab\n");
		sb.append("itemstack_path=CreativeTabSortHelper.sortStacks on ModItems + probe ItemStacks\n");
		sb.append("mod_registry=ingot_uranium,sat_mapper,battery_steam (shipped ItemBattery.getSubItems)\n\n");

		for (String tabKey : TAB_KEYS) {
			List<String> keys = new ArrayList<String>(CreativeTabSortOrder.getTabRegistryOrder(tabKey));
			NonNullList<ItemStack> stacks = NonNullList.create();
			for (int i = keys.size() - 1; i >= 0; i--) {
				stacks.add(probeStackForKey(keys.get(i)));
			}
			CreativeTabSortHelper.sortStacks(stacks, tabKey);
			sb.append('@').append(tabKey).append(" (").append(stacks.size()).append(" items)\n");
			for (ItemStack stack : stacks) {
				int idx = CreativeTabSortOrder.getSortIndex(stack, tabKey);
				sb.append(String.format("  %8d  %s%n", idx, registryKey(stack)));
			}
			sb.append('\n');
		}

		Files.write(out, sb.toString().getBytes(StandardCharsets.UTF_8));
	}

	private static ItemStack probeStackForKey(String key) {
		if (key.contains(":")) {
			String[] parts = key.split(":", 2);
			return probeStack(parts[0], parts[1]);
		}
		return probeStack("hbm", key);
	}

	private static ItemStack probeStack(String namespace, String path) {
		return new ItemStack(probeItem(namespace, path));
	}

	private static ItemStack probeStack(String namespace, String path, int metadata) {
		return new ItemStack(probeItem(namespace, path), 1, metadata);
	}

	private static Item probeItem(String namespace, String path) {
		Item item = new Item();
		item.setRegistryName(namespace, path);
		return item;
	}

	private static ItemStack batteryStack(String path, long charge) {
		ItemStack stack = probeStack("hbm", path);
		NBTTagCompound tag = new NBTTagCompound();
		tag.setLong("charge", charge);
		stack.setTagCompound(tag);
		return stack;
	}

	private static String registryKey(ItemStack stack) {
		ResourceLocation reg = stack.getItem().getRegistryName();
		if (reg == null) {
			return "";
		}
		return "hbm".equals(reg.getNamespace()) ? reg.getPath() : reg.toString();
	}

	private static void expectKey(String actual, String expected) {
		if (!expected.equals(actual)) {
			throw new AssertionError("expected " + expected + " got " + actual);
		}
	}

	private static void expectRegistry(ItemStack stack, String namespace, String path) {
		ResourceLocation reg = stack.getItem().getRegistryName();
		if (reg == null || !namespace.equals(reg.getNamespace()) || !path.equals(reg.getPath())) {
			throw new AssertionError("expected " + namespace + ":" + path + " got " + reg);
		}
	}
}