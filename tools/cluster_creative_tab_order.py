"""Re-cluster creative_tab_order.txt: unified firearms, common gun_ammo block, parts tail.

Shipped in gradle project tools/ — GOAL_CLUSTER_VERSION=creative-tab-grouping-5
"""
from __future__ import annotations

import os
import re
import sys
from collections import defaultdict
from pathlib import Path

PROJ = Path(__file__).resolve().parents[1]
TOOLS = Path(__file__).resolve().parent
ORDER = PROJ / "src/main/resources/assets/hbm/creative_tab_order.txt"
MOD_ITEMS = PROJ / "src/main/java/com/hbm/items/ModItems.java"
MOD_BLOCKS = PROJ / "src/main/java/com/hbm/blocks/ModBlocks.java"
SCRATCH = Path(os.environ.get("GOAL_SCRATCH", r"C:\Temp\grok-goal-f7963e6bf0c1\implementer"))
SCRATCH.mkdir(parents=True, exist_ok=True)

sys.path.insert(0, str(TOOLS))
from creative_tab_parse import collect_port_tab_entries  # noqa: E402

GOAL_CLUSTER_VERSION = "creative-tab-grouping-5"

# EE reference ModItems.java declaration order (revolvers only, guns not ammo).
REVOLVER_ORDER = (
    "gun_revolver_iron",
    "gun_revolver",
    "gun_revolver_saturnite",
    "gun_revolver_gold",
    "gun_revolver_lead",
    "gun_revolver_schrabidium",
    "gun_revolver_cursed",
    "gun_revolver_nightmare",
    "gun_revolver_nightmare2",
    "gun_revolver_pip",
    "gun_revolver_nopip",
    "gun_revolver_blackjack",
    "gun_revolver_silver",
    "gun_revolver_red",
)

# EE reference unified firearm order (CE+EE, excluding revolver family guns).
FIREARM_ORDER = (
    "gun_b92",
    "gun_b93",
    "gun_deagle",
    "gun_pepperbox",
    "gun_light_revolver",
    "gun_heavy_revolver",
    "gun_henry",
    "gun_flechette",
    "gun_ar15",
    "gun_uboinik",
    "gun_supershotgun",
    "gun_jshotgun",
    "gun_maresleg",
    "gun_liberator",
    "gun_spas12",
    "gun_autoshotgun",
    "gun_ks23",
    "gun_sauer",
    "gun_calamity",
    "gun_calamity_dual",
    "gun_minigun",
    "gun_avenger",
    "gun_lacunae",
    "gun_bolt_action",
    "gun_bolt_action_green",
    "gun_uzi",
    "gun_uzi_silencer",
    "gun_uzi_saturnite",
    "gun_uzi_saturnite_silencer",
    "gun_mp40",
    "gun_thompson",
    "gun_greasegun",
    "gun_lag",
    "gun_am180",
    "gun_g3",
    "gun_stg77",
    "gun_carbine",
    "gun_rpg",
    "gun_karl",
    "gun_panzerschreck",
    "gun_stinger",
    "gun_quadro",
    "gun_missile_launcher",
    "gun_lever_action",
    "gun_lever_action_dark",
    "gun_hk69",
    "gun_spark",
    "gun_fatman",
    "gun_proto",
    "gun_mirv",
    "gun_bf",
    "gun_zomg",
    "gun_xvl1456",
    "gun_hp",
    "gun_defabricator",
    "gun_vortex",
    "cc_plasma_gun",
    "gun_egon",
    "gun_euthanasia",
    "gun_skystinger",
    "gun_mp",
    "gun_bolter",
    "gun_cryolator",
    "gun_jack",
    "gun_immolator",
    "gun_flamer",
    "gun_chemthrower",
    "gun_osipr",
    "gun_emp",
    "gun_moist_nugget",
    "gun_super_shotgun",
    "gun_revolver_inverted",
    "gun_lever_action_sonata",
    "gun_bolt_action_saturnite",
    "gun_folly",
    "gun_dampfmaschine",
    "gun_darter",
    "crucible",
    "gun_drill",
    "gun_fireext",
    "gun_charge_thrower",
    "gun_double_barrel",
    "gun_flaregun",
    "gun_congolake",
    "gun_amat",
    "gun_m2",
    "gun_tesla_cannon",
    "gun_laser_pistol",
    "gun_lasrifle",
    "gun_tau",
    "gun_coilgun",
    "gun_pa_melee",
    "gun_pa_ranged",
    "drax",
    "drax_mk2",
    "drax_mk3",
)

WEAPON_TAB_MISC = (
    "railgun_plasma",
    "ullapool_caber",
    "charge_railgun",
    "shimmer_sledge",
    "balefire_and_steel",
)

WEAPON_TAB_KITS = (
    "grenade_kit",
    "grenade_shell",
    "grenade_filling",
    "grenade_fuze",
    "grenade_extra",
    "grenade_universal",
)

WEAPON_TAB_TOOLS = (
    "wrench",
    "wrench_flipped",
    "memespoon",
)

# Sedna ammo + artillery (GunFactory / ModItems order).
AMMO_TAB_ORDER = (
    "ammo_standard",
    "ammo_container",
    "ammo_arty",
    "ammo_himars",
)

# GunFactory registration order: generic → special → caliber.
WEAPON_MOD_ORDER = (
    "weapon_mod_generic",
    "weapon_mod_special",
    "weapon_mod_caliber",
)

INF_WATER_ORDER = (
    "inf_water",
    "inf_water_mk2",
    "inf_water_mk3",
    "inf_water_mk4",
)

CONSUMABLE_PREFIX_GROUPS = [
    "gas_mask_filter_",
    "gas_mask_filter",
]

WEAPON_TAB_MELEE = frozenset({
    "mese_pickaxe",
    "mese_axe",
    "dnt_sword",
    "dwarven_pickaxe",
})

MELEE_HEAD_ORDER = ("hs_sword", "hf_sword", "shimmer_axe")
MELEE_CLUSTER_ORDER = ("mese_pickaxe", "mese_axe", "dnt_sword", "dwarven_pickaxe", "mese_gavel")

# ModItems.java ItemSwordMeteorite damage order (ascending).
PARTS_TAB_BEDROCK_JEI_BLOCK = (
    "ore_bedrock",
    "ore_bedrock_centrifuged",
    "ore_bedrock_cleaned",
    "ore_bedrock_separated",
    "ore_bedrock_deepcleaned",
    "ore_bedrock_purified",
    "ore_bedrock_nitrated",
    "ore_bedrock_nitrocrystalline",
    "ore_bedrock_seared",
    "ore_bedrock_exquisite",
    "ore_bedrock_perfect",
    "ore_bedrock_enriched",
    "bedrock_ore_new",
    "bedrock_ore_base",
    "bedrock_ore_fragment",
)

METEORITE_SWORD_DAMAGE_ORDER = (
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
)

WEAPON_TAB_SHIELDS = {
    "alloy_shield",
    "cmb_shield",
    "cobalt_shield",
    "desh_shield",
    "elec_shield",
    "schrabidium_shield",
    "starmetal_shield",
    "steel_shield",
    "titanium_shield",
}

CONTROL_TAB_PELLETS = {
    "pellet_mercury",
    "pellet_rtg_depleted_bismuth",
    "pellet_rtg_depleted_lead",
    "pellet_rtg_depleted_mercury",
    "pellet_rtg_depleted_neptunium",
    "pellet_rtg_depleted_zirconium",
}

PARTS_PREFIX_GROUPS = [
    "ingot_",
    "nugget_",
    "powder_",
    "billet_",
    "gem_",
    "crystal_",
    "plate_",
    "wire_",
    "bolt_",
    "part_",
    "pellet_",
    "dust_",
    "scrap_",
]

PARTS_OTHER_TAIL_PREFIXES = [
    "rod_quad_",
    "rod_dual_",
    "rod_",
    "mechanism_",
    "warhead_",
    "stamp_",
    "fragment_",
    "hull_",
]

PARTS_OTHER_TAIL_EXACT = frozenset({
    "heavy_duty_element",
    "low_density_element",
})

WEAPON_BOUND_PARTS_PATHS = frozenset({
    "cc_plasma_gun",
    "gun_cryolator_ammo",
    "gun_dash_ammo",
    "gun_defabricator_ammo",
    "gun_emp_ammo",
    "gun_euthanasia_ammo",
    "gun_hp_ammo",
    "gun_immolator_ammo",
    "gun_jack_ammo",
    "gun_spark_ammo",
    "turret_cheapo_ammo",
    "turret_control",
    "turret_cwis_ammo",
    "turret_flamer_ammo",
    "turret_heavy_ammo",
    "turret_light_ammo",
    "turret_rocket_ammo",
    "turret_spitfire_ammo",
    "turret_tau_ammo",
})

MISSILE_BOUND_PARTS_PATHS = frozenset({
    "missile_soyuz0",
    "missile_soyuz1",
    "missile_soyuz2",
})

CONTROL_BOUND_PARTS_PATHS = frozenset({
    "inf_water_mk3",
    "inf_water_mk4",
})


def registry_path(key: str) -> str:
    return key.split(":", 1)[-1] if ":" in key else key


def alpha_sort_keys(keys: list[str]) -> list[str]:
    return sorted(keys, key=registry_path)


def order_by_template(keys: list[str], template: tuple[str, ...]) -> list[str]:
    by_path = {registry_path(k): k for k in keys}
    out: list[str] = []
    placed: set[str] = set()
    for name in template:
        key = by_path.get(name)
        if key and name not in placed:
            out.append(key)
            placed.add(name)
    for key in alpha_sort_keys(keys):
        if registry_path(key) not in placed:
            out.append(key)
    return out


def is_gun_ammo_path(path: str) -> bool:
    return path.startswith("gun_") and "_ammo" in path


def is_revolver_gun_path(path: str) -> bool:
    return path.startswith("gun_revolver") and not path.endswith("_ammo") and path != "gun_revolver_inverted"


def is_firearm_path(path: str) -> bool:
    if is_revolver_gun_path(path):
        return False
    if is_gun_ammo_path(path):
        return False
    if path.startswith("gun_"):
        return True
    return path in {"cc_plasma_gun", "crucible", "drax", "drax_mk2", "drax_mk3"}


def meteorite_sort_key(key: str) -> tuple[int, str]:
    path = registry_path(key)
    try:
        return (METEORITE_SWORD_DAMAGE_ORDER.index(path), path)
    except ValueError:
        return (len(METEORITE_SWORD_DAMAGE_ORDER), path)


def order_melee_section(swords: list[str], melee_tools: list[str]) -> list[str]:
    by_path = {registry_path(k): k for k in swords + melee_tools}
    head = [by_path[name] for name in MELEE_HEAD_ORDER if name in by_path]
    cluster = [by_path[name] for name in MELEE_CLUSTER_ORDER if name in by_path]
    meteorite_block = sorted(
        (k for k in swords if registry_path(k) == "meteorite_sword" or registry_path(k).startswith("meteorite_sword_")),
        key=meteorite_sort_key,
    )
    placed = {registry_path(k) for k in head + cluster + meteorite_block}
    other = [k for k in swords if registry_path(k) not in placed]
    return head + cluster + meteorite_block + other


def cluster_weapon_tab(keys: list[str]) -> list[str]:
    """Reorder weaponTab: revolvers, unified firearms, gun_ammo, ammo, clips, melee, shields."""
    revolvers: list[str] = []
    guns: list[str] = []
    gun_ammo: list[str] = []
    clips: list[str] = []
    ammo: list[str] = []
    swords: list[str] = []
    gavels: list[str] = []
    shields: list[str] = []
    melee_tools: list[str] = []
    grenades: list[str] = []
    turrets: list[str] = []
    misc: list[str] = []
    kits: list[str] = []
    tools: list[str] = []
    explosives: list[str] = []
    weapon_mods: list[str] = []
    other: list[str] = []

    for key in keys:
        path = registry_path(key)
        if path in WEAPON_MOD_ORDER:
            weapon_mods.append(key)
        elif is_revolver_gun_path(path):
            revolvers.append(key)
        elif path in WEAPON_TAB_MELEE or path == "mese_gavel":
            melee_tools.append(key)
        elif path.endswith("_shield") or path == "alloy_shield":
            shields.append(key)
        elif path.endswith("_gavel") or path == "schrabidium_hammer":
            gavels.append(key)
        elif is_gun_ammo_path(path):
            gun_ammo.append(key)
        elif is_firearm_path(path):
            guns.append(key)
        elif path.startswith("ammo_") or path.startswith("clip_"):
            ammo.append(key)
        elif path.endswith("_sword") or path.endswith("_pickaxe") or path.endswith("_axe") or path.startswith("meteorite_sword"):
            swords.append(key)
        elif path in WEAPON_TAB_KITS:
            kits.append(key)
        elif path.startswith("grenade_") or path.startswith("weaponized_"):
            grenades.append(key)
        elif path.startswith("turret_") or path.startswith("disperser_") or path.startswith("glyphid_"):
            turrets.append(key)
        elif path in WEAPON_TAB_MISC:
            misc.append(key)
        elif path in WEAPON_TAB_TOOLS:
            tools.append(key)
        elif path.startswith("stick_"):
            explosives.append(key)
        else:
            other.append(key)

    revolver_block = order_by_template(revolvers, REVOLVER_ORDER)
    firearm_block = order_by_template(guns, FIREARM_ORDER)
    melee_block = order_melee_section(swords, melee_tools)
    ammo_block = order_by_template(ammo, AMMO_TAB_ORDER)
    weapon_mod_block = order_by_template(weapon_mods, WEAPON_MOD_ORDER)

    return (
        revolver_block
        + firearm_block
        + alpha_sort_keys(gun_ammo)
        + ammo_block
        + clips
        + weapon_mod_block
        + melee_block
        + gavels
        + alpha_sort_keys(shields)
        + grenades
        + turrets
        + order_by_template(misc, WEAPON_TAB_MISC)
        + order_by_template(kits, WEAPON_TAB_KITS)
        + order_by_template(tools, WEAPON_TAB_TOOLS)
        + alpha_sort_keys(explosives)
        + other
    )


def cluster_parts_other_tail(other_keys: list[str]) -> list[str]:
    hbmspace = [k for k in other_keys if k.startswith("hbmspace:")]
    rest = [k for k in other_keys if not k.startswith("hbmspace:")]
    exact_hits = [k for k in rest if registry_path(k) in PARTS_OTHER_TAIL_EXACT]
    prefix_pool = [k for k in rest if registry_path(k) not in PARTS_OTHER_TAIL_EXACT]
    structured = cluster_by_prefix(prefix_pool, PARTS_OTHER_TAIL_PREFIXES)
    return structured + alpha_sort_keys(exact_hits) + alpha_sort_keys(hbmspace)


def cluster_parts_tab(keys: list[str]) -> list[str]:
    """Cluster partsTab: prefix families, then structured __other__ tail."""
    buckets: dict[str, list[str]] = {p: [] for p in PARTS_PREFIX_GROUPS}
    buckets["__other__"] = []
    for key in keys:
        path = registry_path(key)
        placed = False
        for prefix in PARTS_PREFIX_GROUPS:
            if path.startswith(prefix):
                buckets[prefix].append(key)
                placed = True
                break
        if not placed:
            buckets["__other__"].append(key)

    out: list[str] = []
    for prefix in PARTS_PREFIX_GROUPS:
        out.extend(alpha_sort_keys(buckets[prefix]))
    out.extend(cluster_parts_other_tail(buckets["__other__"]))
    return out


def cluster_consumable_tab(keys: list[str]) -> list[str]:
    filter_keys = [k for k in keys if registry_path(k).startswith("gas_mask_filter")]
    if not filter_keys:
        return keys
    filters = alpha_sort_keys(filter_keys)
    out: list[str] = []
    inserted = False
    for key in keys:
        if registry_path(key).startswith("gas_mask_filter"):
            if not inserted:
                out.extend(filters)
                inserted = True
            continue
        out.append(key)
    return out


def cluster_control_tab(keys: list[str]) -> list[str]:
    inf_keys = [k for k in keys if registry_path(k).startswith("inf_water")]
    if not inf_keys:
        return keys
    ordered_inf = order_by_template(inf_keys, INF_WATER_ORDER)
    out: list[str] = []
    inserted = False
    for key in keys:
        if registry_path(key).startswith("inf_water"):
            if not inserted:
                out.extend(ordered_inf)
                inserted = True
            continue
        out.append(key)
    return out


def load_tabs(path: Path) -> dict[str, list[str]]:
    tabs: dict[str, list[str]] = {}
    current = None
    for raw in path.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        if line.startswith("@"):
            current = line[1:]
            tabs[current] = []
            continue
        key = line.split("=", 1)[0].strip()
        if current:
            tabs[current].append(key)
    return tabs


def write_tabs(path: Path, tabs: dict[str, list[str]]) -> None:
    lines: list[str] = [
        f"# {GOAL_CLUSTER_VERSION} shipped cluster_creative_tab_order.py (hbm-x5687-1.12.2/tools)",
    ]
    for tab in sorted(tabs.keys()):
        lines.append(f"@{tab}")
        for i, key in enumerate(tabs[tab]):
            lines.append(f"{key}={i}")
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def cluster_by_prefix(keys: list[str], prefix_groups: list[str]) -> list[str]:
    buckets: dict[str, list[str]] = {p: [] for p in prefix_groups}
    buckets["__other__"] = []
    for key in keys:
        path = registry_path(key)
        placed = False
        for prefix in prefix_groups:
            if path.startswith(prefix):
                buckets[prefix].append(key)
                placed = True
                break
        if not placed:
            buckets["__other__"].append(key)

    out: list[str] = []
    for prefix in prefix_groups:
        out.extend(alpha_sort_keys(buckets[prefix]))
    out.extend(buckets["__other__"])
    return out


def load_hidden_registry_paths() -> set[str]:
    from creative_tab_parse import extract_registry_path, field_declaration_chunk

    hidden: set[str] = set()
    text = MOD_ITEMS.read_text(encoding="utf-8")
    pat = r"public static final (?:Item|Item\w+)\s+(\w+)\s*=\s*"
    for m in re.finditer(pat, text):
        field = m.group(1)
        chunk = field_declaration_chunk(text, m.start())
        if not re.search(r"\.setCreativeTab\(\s*null\s*\)", chunk):
            continue
        path = extract_registry_path(chunk, field)
        if path:
            hidden.add(path)
    return hidden


def purge_hidden_items(tabs: dict[str, list[str]], hidden: set[str]) -> list[str]:
    removed: list[str] = []
    for tab, keys in list(tabs.items()):
        kept: list[str] = []
        for key in keys:
            if registry_path(key) in hidden:
                removed.append(key)
            else:
                kept.append(key)
        tabs[tab] = kept
    return removed


def load_declared_tabs() -> dict[str, str]:
    declared: dict[str, str] = {}
    port_tabs = collect_port_tab_entries(MOD_ITEMS, MOD_BLOCKS)
    for tab, entries in port_tabs.items():
        for registry_key, path in entries:
            key = registry_path(registry_key)
            declared[key] = tab
    return declared


def sync_tab_membership(tabs: dict[str, list[str]], declared: dict[str, str]) -> list[tuple[str, str]]:
    inserted: list[tuple[str, str]] = []
    for key, tab in declared.items():
        if tab not in tabs:
            tabs[tab] = []
        present = set(tabs[tab])
        if key not in present and f"hbm:{key}" not in present:
            tabs[tab].append(key)
            inserted.append((tab, key))
    return inserted


# Block removed from ModBlocks; strip from order on regen (grouping-5 scope).
STALE_ORDER_PATHS = frozenset({"sliding_blast_door_legacy"})


def purge_stale_order_paths(tabs: dict[str, list[str]]) -> list[str]:
    removed: list[str] = []
    for tab, keys in list(tabs.items()):
        kept: list[str] = []
        for key in keys:
            if registry_path(key) in STALE_ORDER_PATHS:
                removed.append(key)
            else:
                kept.append(key)
        tabs[tab] = kept
    return removed


def reconcile_tabs_with_declarations(tabs: dict[str, list[str]], declared: dict[str, str]) -> None:
    misplaced: dict[str, list[str]] = defaultdict(list)
    for tab, keys in list(tabs.items()):
        keep: list[str] = []
        for key in keys:
            path = registry_path(key)
            want = declared.get(path) or declared.get(key)
            if want and want != tab:
                misplaced[want].append(key)
            else:
                keep.append(key)
        tabs[tab] = keep
    for tab, keys in misplaced.items():
        if tab not in tabs:
            tabs[tab] = []
        for key in keys:
            if key not in tabs[tab] and registry_path(key) not in {registry_path(k) for k in tabs[tab]}:
                tabs[tab].append(key)


def apply_cross_tab_moves(tabs: dict[str, list[str]]) -> None:
    parts = tabs.get("partsTab", [])
    weapon = tabs.get("weaponTab", [])
    control = tabs.get("controlTab", [])
    missile = tabs.get("missileTab", [])

    moved_melee = [k for k in parts if registry_path(k) in WEAPON_TAB_MELEE]
    for k in moved_melee:
        parts.remove(k)
        if k not in weapon:
            weapon.append(k)

    moved_shields = [k for k in parts if registry_path(k) in WEAPON_TAB_SHIELDS]
    for k in moved_shields:
        parts.remove(k)
        if k not in weapon:
            weapon.append(k)

    for path_set, dest, bucket in (
        (WEAPON_BOUND_PARTS_PATHS, weapon, weapon),
        (MISSILE_BOUND_PARTS_PATHS, missile, missile),
        (CONTROL_BOUND_PARTS_PATHS, control, control),
    ):
        moved = [k for k in parts if registry_path(k) in path_set]
        for k in moved:
            parts.remove(k)
            if k not in dest:
                bucket.append(k)

    moved_pellets_parts = [k for k in parts if registry_path(k) in CONTROL_TAB_PELLETS]
    for k in moved_pellets_parts:
        parts.remove(k)
        if k not in control:
            control.append(k)

    moved_pellets_weapon = [k for k in weapon if registry_path(k) in CONTROL_TAB_PELLETS]
    for k in moved_pellets_weapon:
        weapon.remove(k)
        if k not in control:
            control.append(k)

    tabs["partsTab"] = parts
    tabs["weaponTab"] = weapon
    tabs["controlTab"] = control
    tabs["missileTab"] = missile


def audit_contiguity(keys: list[str], prefix: str) -> list[tuple[str, int]]:
    hits = [(i, k) for i, k in enumerate(keys) if registry_path(k).startswith(prefix)]
    if len(hits) <= 1:
        return []
    gaps = []
    for j in range(1, len(hits)):
        prev_i, prev_k = hits[j - 1]
        cur_i, cur_k = hits[j]
        if cur_i != prev_i + 1:
            gaps.append((prev_k, cur_i - prev_i - 1))
    return gaps


def gun_ammo_paths(keys: list[str]) -> list[str]:
    return [registry_path(k) for k in keys if is_gun_ammo_path(registry_path(k))]


def main() -> None:
    declared = load_declared_tabs()
    tabs = load_tabs(ORDER)
    inserted = sync_tab_membership(tabs, declared)
    reconcile_tabs_with_declarations(tabs, declared)
    stale = purge_stale_order_paths(tabs)
    removed = purge_hidden_items(tabs, load_hidden_registry_paths())
    apply_cross_tab_moves(tabs)

    tabs["partsTab"] = cluster_parts_tab(tabs["partsTab"])
    tabs["consumableTab"] = cluster_consumable_tab(tabs.get("consumableTab", []))
    tabs["weaponTab"] = cluster_weapon_tab(tabs["weaponTab"])

    control = tabs.get("controlTab", [])
    rtg = [k for k in control if k.startswith("pellet_rtg") or registry_path(k) in CONTROL_TAB_PELLETS]
    non_rtg = [k for k in control if k not in rtg]
    tabs["controlTab"] = cluster_control_tab(rtg + non_rtg)

    write_tabs(ORDER, tabs)

    report_lines = [f"cluster_creative_tab_order.py audit version={GOAL_CLUSTER_VERSION}\n"]
    if inserted:
        report_lines.append(f"sync_tab_membership inserted={len(inserted)}")
        for tab, key in inserted[:20]:
            report_lines.append(f"  +{tab} {key}")
    else:
        report_lines.append("sync_tab_membership inserted=0")
    if stale:
        report_lines.append(f"purge_stale removed={len(stale)}")
        for key in stale[:10]:
            report_lines.append(f"  -stale {key}")
    if removed:
        report_lines.append(f"purge_hidden removed={len(removed)}")
        for key in removed[:10]:
            report_lines.append(f"  -{key}")

    weapon = tabs.get("weaponTab", [])
    gun_ammo_hits = [(i, k) for i, k in enumerate(weapon) if is_gun_ammo_path(registry_path(k))]
    if gun_ammo_hits:
        contiguous = gun_ammo_hits[-1][0] - gun_ammo_hits[0][0] + 1 == len(gun_ammo_hits)
        report_lines.append(
            f"  gun_ammo_block count={len(gun_ammo_hits)} first={gun_ammo_hits[0][0]} "
            f"last={gun_ammo_hits[-1][0]} contiguous={contiguous}"
        )

    for tab, keys in sorted(tabs.items()):
        for prefix in ("ingot_", "powder_", "nugget_", "gas_mask_filter"):
            gaps = audit_contiguity(keys, prefix)
            report_lines.append(f"{tab} {prefix} gaps={len(gaps)}")
            if gaps:
                report_lines.append(f"  sample_gap_after={gaps[0]}")
        if tab == "controlTab":
            inf_hits = [i for i, k in enumerate(keys) if registry_path(k).startswith("inf_water")]
            if inf_hits:
                report_lines.append(
                    f"  inf_water_block count={len(inf_hits)} contiguous="
                    f"{inf_hits[-1] - inf_hits[0] + 1 == len(inf_hits)} paths={[registry_path(keys[i]) for i in inf_hits]}"
                )
        if tab == "weaponTab":
            paths = [registry_path(k) for k in keys]
            rev_sub = [p for p in paths if is_revolver_gun_path(p)]
            fire_sub = [p for p in paths if is_firearm_path(p)]
            report_lines.append(f"  revolvers={len(rev_sub)} firearms={len(fire_sub)}")
            if gun_ammo_hits and rev_sub:
                first_ammo = gun_ammo_hits[0][0]
                last_rev = max(paths.index(p) for p in rev_sub)
                last_fire = max((paths.index(p) for p in fire_sub), default=-1)
                report_lines.append(
                    f"  ammo_after_firearms={first_ammo > last_fire} last_rev={last_rev} last_fire={last_fire}"
                )
            shield_hits = [(i, k) for i, k in enumerate(keys) if registry_path(k).endswith("_shield")]
            if shield_hits:
                report_lines.append(
                    f"  weapon_shields count={len(shield_hits)} first={shield_hits[0][0]} last={shield_hits[-1][0]}"
                )

    out = SCRATCH / "sort-audit.txt"
    out.write_text("\n".join(report_lines) + "\n", encoding="utf-8")
    print(out.read_text(encoding="utf-8"))


if __name__ == "__main__":
    main()