"""Goal-only verification for creative tab grouping (gradle project tools/)."""
from __future__ import annotations

import os
import sys
from pathlib import Path

PROJ = Path(__file__).resolve().parents[1]
TOOLS = Path(__file__).resolve().parent
SCRATCH = Path(os.environ.get("GOAL_SCRATCH", r"C:\Temp\grok-goal-f7963e6bf0c1\implementer"))
ORDER = PROJ / "src/main/resources/assets/hbm/creative_tab_order.txt"

sys.path.insert(0, str(TOOLS))
from cluster_creative_tab_order import (  # noqa: E402
    FIREARM_ORDER,
    GOAL_CLUSTER_VERSION,
    INF_WATER_ORDER,
    MELEE_CLUSTER_ORDER,
    METEORITE_SWORD_DAMAGE_ORDER,
    PARTS_TAB_BEDROCK_JEI_BLOCK,
    WEAPON_MOD_ORDER,
    WEAPON_TAB_KITS,
    WEAPON_TAB_TOOLS,
    cluster_consumable_tab,
    cluster_control_tab,
    cluster_parts_tab,
    cluster_weapon_tab,
    is_gun_ammo_path,
    registry_path,
)
from creative_tab_parse import (  # noqa: E402
    collect_gun_factory_weapon_tab_entries,
    collect_port_tab_entries,
    collect_sedna_weapon_tab_entries,
)


def load_weapon_tab_keys() -> list[str]:
    return load_tab_keys("weaponTab")


def verify_parts_tab_regeneration_parity() -> bool:
    on_disk = load_tab_keys("partsTab")
    if not on_disk:
        print("[GOAL] parts_tab_regeneration_parity=False (empty partsTab)")
        return False
    regenerated = cluster_parts_tab(list(on_disk))
    ok = regenerated == on_disk
    print(f"[GOAL] parts_tab_regeneration_parity={ok} entries={len(on_disk)} version={GOAL_CLUSTER_VERSION}")
    return ok


def verify_consumable_tab_regeneration_parity() -> bool:
    on_disk = load_tab_keys("consumableTab")
    if not on_disk:
        print("[GOAL] consumable_tab_regeneration_parity=False (empty consumableTab)")
        return False
    regenerated = cluster_consumable_tab(list(on_disk))
    ok = regenerated == on_disk
    print(f"[GOAL] consumable_tab_regeneration_parity={ok} entries={len(on_disk)} version={GOAL_CLUSTER_VERSION}")
    return ok


def verify_weapon_tab_regeneration_parity() -> bool:
    on_disk = load_weapon_tab_keys()
    if not on_disk:
        print("[GOAL] weapon_tab_regeneration_parity=False (empty weaponTab)")
        return False
    regenerated = cluster_weapon_tab(list(on_disk))
    ok = regenerated == on_disk
    print(f"[GOAL] weapon_tab_regeneration_parity={ok} entries={len(on_disk)} version={GOAL_CLUSTER_VERSION}")
    return ok


def load_tab_keys(tab_name: str) -> list[str]:
    keys: list[str] = []
    in_tab = False
    for raw in ORDER.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        if line == f"@{tab_name}":
            in_tab = True
            continue
        if line.startswith("@") and in_tab:
            break
        if in_tab and "=" in line:
            keys.append(line.split("=", 1)[0])
    return keys


def verify_parts_prefix_probes() -> bool:
    parts = load_tab_keys("partsTab")
    ok = True
    for prefix in ("ingot_", "nugget_", "powder_"):
        hits = [i for i, k in enumerate(parts) if registry_path(k).startswith(prefix)]
        if len(hits) > 1 and hits[-1] - hits[0] + 1 != len(hits):
            ok = False
            print(f"[GOAL] partsTab {prefix} not contiguous span={hits[-1]-hits[0]+1} count={len(hits)}")
    probes = {
        name: parts.index(name) if name in parts else -1
        for name in (
            "ingot_ac227",
            "ingot_neodymium",
            "ingot_radspice",
            "ingot_strontium",
            "nugget_ac227",
            "nugget_radspice",
            "powder_ac227",
            "powder_radspice",
        )
    }
    ingot_hits = [i for i, k in enumerate(parts) if registry_path(k).startswith("ingot_")]
    nugget_hits = [i for i, k in enumerate(parts) if registry_path(k).startswith("nugget_")]
    powder_hits = [i for i, k in enumerate(parts) if registry_path(k).startswith("powder_")]
    for name in ("ingot_ac227", "ingot_neodymium", "ingot_radspice", "ingot_strontium"):
        idx = probes[name]
        if idx < 0 or idx < ingot_hits[0] or idx > ingot_hits[-1]:
            ok = False
    for name in ("nugget_ac227", "nugget_radspice"):
        idx = probes.get(name, -1)
        if idx < 0 or idx < nugget_hits[0] or idx > nugget_hits[-1]:
            ok = False
    for name in ("powder_ac227", "powder_radspice"):
        idx = probes.get(name, -1)
        if idx < 0 or idx < powder_hits[0] or idx > powder_hits[-1]:
            ok = False
    if ingot_hits:
        ingot_paths = [registry_path(parts[i]) for i in ingot_hits]
        if ingot_paths != sorted(ingot_paths):
            ok = False
            print(f"[GOAL] ingot_block_not_alpha_sorted sample={ingot_paths[:5]}..{ingot_paths[-3:]}")
    for path in parts:
        if is_gun_ammo_path(registry_path(path)):
            ok = False
            print(f"[GOAL] partsTab contains gun ammo {path}")
    print(f"[GOAL] parts_probes={probes} ok={ok}")
    return ok


def verify_consumable_filters() -> bool:
    consumable = load_tab_keys("consumableTab")
    hits = [i for i, k in enumerate(consumable) if registry_path(k).startswith("gas_mask_filter")]
    ok = len(hits) > 0 and hits[-1] - hits[0] + 1 == len(hits)
    radon = consumable.index("gas_mask_filter_radon") if "gas_mask_filter_radon" in consumable else -1
    ok = ok and radon >= 0 and radon in hits
    print(f"[GOAL] gas_mask_filter_contiguous={ok} radon={radon} block={hits[:3]}..{hits[-3:]}")
    return ok


def verify_gun_ammo_block() -> bool:
    weapon = load_weapon_tab_keys()
    paths = [registry_path(k) for k in weapon]
    ammo_hits = [i for i, p in enumerate(paths) if is_gun_ammo_path(p)]
    if not ammo_hits:
        print("[GOAL] gun_ammo_block=False missing gun_*_ammo")
        return False
    contiguous = ammo_hits[-1] - ammo_hits[0] + 1 == len(ammo_hits)
    schrab = paths.index("gun_revolver_schrabidium_ammo") if "gun_revolver_schrabidium_ammo" in paths else -1
    in_block = schrab >= ammo_hits[0] and schrab <= ammo_hits[-1]
    ok = contiguous and in_block
    print(
        f"[GOAL] gun_ammo_block contiguous={contiguous} count={len(ammo_hits)} "
        f"first={ammo_hits[0]} last={ammo_hits[-1]} schrabidium_in_block={in_block}"
    )
    return ok


def verify_weapon_tab_coverage() -> bool:
    mod_items = PROJ / "src/main/java/com/hbm/items/ModItems.java"
    mod_blocks = PROJ / "src/main/java/com/hbm/blocks/ModBlocks.java"
    declared = collect_port_tab_entries(mod_items, mod_blocks)
    weapon_declared = {registry_path(k) for k, _ in declared.get("weaponTab", [])}
    on_disk = {registry_path(k) for k in load_weapon_tab_keys()}
    missing = sorted(weapon_declared - on_disk)
    ok = not missing
    print(
        f"[GOAL] weapon_tab_coverage ok={ok} declared={len(weapon_declared)} "
        f"on_disk={len(on_disk)} missing={len(missing)}"
    )
    if missing:
        print(f"  sample_missing={missing[:12]}")
    sedna = collect_sedna_weapon_tab_entries(mod_items.parent / "weapon" / "sedna" / "factory")
    sedna_paths = {path for _, path in sedna}
    sedna_missing = sorted(sedna_paths - on_disk)
    sedna_ok = not sedna_missing
    print(f"[GOAL] sedna_weapon_tab_coverage ok={sedna_ok} missing={len(sedna_missing)}")
    if sedna_missing:
        print(f"  sedna_missing={sedna_missing[:12]}")
    gun_factory = mod_items.parent / "weapon" / "sedna" / "factory" / "GunFactory.java"
    gun_factory_paths = {path for _, path in collect_gun_factory_weapon_tab_entries(gun_factory)}
    gun_factory_missing = sorted(gun_factory_paths - on_disk)
    gun_factory_ok = not gun_factory_missing
    print(f"[GOAL] gun_factory_weapon_tab_coverage ok={gun_factory_ok} missing={len(gun_factory_missing)}")
    if gun_factory_missing:
        print(f"  gun_factory_missing={gun_factory_missing[:12]}")
    return ok and sedna_ok and gun_factory_ok


def verify_weapon_mod_block() -> bool:
    weapon = load_weapon_tab_keys()
    paths = [registry_path(k) for k in weapon]
    mod_hits = [i for i, p in enumerate(paths) if p in WEAPON_MOD_ORDER]
    ok = len(mod_hits) == len(WEAPON_MOD_ORDER)
    if mod_hits:
        ok = ok and mod_hits[-1] - mod_hits[0] + 1 == len(mod_hits)
    sub = [paths[i] for i in mod_hits] if mod_hits else []
    ok = ok and sub == [n for n in WEAPON_MOD_ORDER if n in paths]
    himars = paths.index("ammo_himars") if "ammo_himars" in paths else -1
    hs = paths.index("hs_sword") if "hs_sword" in paths else -1
    if mod_hits and himars >= 0:
        ok = ok and min(mod_hits) > himars
    if mod_hits and hs >= 0:
        ok = ok and max(mod_hits) < hs
    print(
        f"[GOAL] weapon_mod_block ok={ok} hits={mod_hits} "
        f"order={sub} after_himars={himars < min(mod_hits) if mod_hits and himars >= 0 else 'n/a'} "
        f"before_hs_sword={max(mod_hits) < hs if mod_hits and hs >= 0 else 'n/a'}"
    )
    return ok


def verify_weapon_tail_sections() -> bool:
    weapon = load_weapon_tab_keys()
    paths = [registry_path(k) for k in weapon]
    kit_hits = [i for i, p in enumerate(paths) if p in WEAPON_TAB_KITS]
    tool_hits = [i for i, p in enumerate(paths) if p in WEAPON_TAB_TOOLS]
    kits_ok = len(kit_hits) > 0 and kit_hits[-1] - kit_hits[0] + 1 == len(kit_hits)
    tools_ok = len(tool_hits) > 0 and tool_hits[-1] - tool_hits[0] + 1 == len(tool_hits)
    order_ok = not kit_hits or not tool_hits or max(kit_hits) < min(tool_hits)
    uzi_ok = "gun_uzi" in paths
    if uzi_ok:
        uzi_idx = paths.index("gun_uzi")
        silencer_idx = paths.index("gun_uzi_silencer") if "gun_uzi_silencer" in paths else -1
        uzi_ok = silencer_idx < 0 or uzi_idx < silencer_idx
    ok = kits_ok and tools_ok and order_ok and uzi_ok
    print(
        f"[GOAL] weapon_tail_sections ok={ok} kits_contiguous={kits_ok} "
        f"tools_contiguous={tools_ok} kits_before_tools={order_ok} gun_uzi_before_silencer={uzi_ok}"
    )
    return ok


def verify_parts_tab_bedrock_jei_block() -> bool:
    parts = load_tab_keys("partsTab")
    paths = [registry_path(k) for k in parts]
    hits = [i for i, p in enumerate(paths) if p in PARTS_TAB_BEDROCK_JEI_BLOCK]
    ok = len(hits) == len(PARTS_TAB_BEDROCK_JEI_BLOCK)
    if hits:
        ok = ok and hits[-1] - hits[0] + 1 == len(hits)
        sub = [paths[i] for i in hits]
        ok = ok and sub == list(PARTS_TAB_BEDROCK_JEI_BLOCK)
    print(
        f"[GOAL] parts_tab_bedrock_jei_block ok={ok} count={len(hits)} "
        f"first={hits[0] if hits else -1} last={hits[-1] if hits else -1}"
    )
    return ok


def verify_meteorite_sword_damage_order() -> bool:
    weapon = load_weapon_tab_keys()
    paths = [registry_path(k) for k in weapon]
    hits = [i for i, p in enumerate(paths) if p in METEORITE_SWORD_DAMAGE_ORDER]
    ok = len(hits) == len(METEORITE_SWORD_DAMAGE_ORDER)
    if hits:
        ok = ok and hits[-1] - hits[0] + 1 == len(hits)
        sub = [paths[i] for i in hits]
        ok = ok and sub == list(METEORITE_SWORD_DAMAGE_ORDER)
    mese_gavel = paths.index("mese_gavel") if "mese_gavel" in paths else -1
    if hits and mese_gavel >= 0:
        ok = ok and hits[0] == mese_gavel + 1
    print(
        f"[GOAL] meteorite_sword_damage_order ok={ok} count={len(hits)} "
        f"first={hits[0] if hits else -1} last={hits[-1] if hits else -1}"
    )
    return ok


def verify_unified_firearm_order() -> bool:
    weapon = load_weapon_tab_keys()
    paths = [registry_path(k) for k in weapon]
    fire_sub = [p for p in paths if p in FIREARM_ORDER]
    ok = fire_sub == [n for n in FIREARM_ORDER if n in paths]
    if ammo_hits := [i for i, p in enumerate(paths) if is_gun_ammo_path(p)]:
        last_fire = max(paths.index(p) for p in fire_sub) if fire_sub else -1
        ok = ok and min(ammo_hits) > last_fire
    print(f"[GOAL] unified_firearm_order ok={ok} firearms={len(fire_sub)}")
    return ok


def verify_control_inf_water() -> bool:
    control = load_tab_keys("controlTab")
    regenerated = cluster_control_tab(list(control))
    ok = regenerated == control
    inf_hits = [i for i, k in enumerate(control) if registry_path(k).startswith("inf_water")]
    if inf_hits:
        ok = ok and inf_hits[-1] - inf_hits[0] + 1 == len(inf_hits)
        inf_paths = [registry_path(control[i]) for i in inf_hits]
        ok = ok and inf_paths == [n for n in INF_WATER_ORDER if n in inf_paths]
    print(f"[GOAL] control_inf_water contiguous={bool(inf_hits)} ok={ok}")
    return ok


def check_creative_grouping_goal() -> bool:
    weapon = load_weapon_tab_keys()
    probes = {name: weapon.index(name) if name in weapon else -1 for name in (
        "hs_sword", "shimmer_axe", "mese_pickaxe", "mese_gavel", "meteorite_sword"
    )}
    melee_ok = all(v >= 0 for v in probes.values())
    if melee_ok:
        melee_ok = probes["shimmer_axe"] + 1 == probes["mese_pickaxe"]
        expected = probes["mese_pickaxe"]
        for name in MELEE_CLUSTER_ORDER:
            if weapon.index(name) != expected:
                melee_ok = False
                break
            expected += 1
        melee_ok = melee_ok and probes["meteorite_sword"] == probes["mese_gavel"] + 1
        meteorite_hits = [weapon.index(name) for name in METEORITE_SWORD_DAMAGE_ORDER if name in weapon]
        melee_ok = melee_ok and len(meteorite_hits) == len(METEORITE_SWORD_DAMAGE_ORDER)
        if meteorite_hits:
            melee_ok = melee_ok and meteorite_hits[-1] - meteorite_hits[0] + 1 == len(meteorite_hits)
    print(f"[GOAL] melee_probes={probes} ok={melee_ok}")
    assembly_hidden = "assembly_template" not in weapon
    for tab_section in ORDER.read_text(encoding="utf-8").split("@"):
        if "assembly_template=" in tab_section:
            assembly_hidden = False
            break
    print(f"[GOAL] assembly_template_hidden={assembly_hidden}")
    shield_hits = [i for i, k in enumerate(weapon) if registry_path(k).endswith("_shield")]
    shield_ok = len(shield_hits) > 0 and shield_hits[-1] - shield_hits[0] + 1 == len(shield_hits)
    print(f"[GOAL] shield_block_contiguous={shield_ok} count={len(shield_hits)}")
    parity_ok = (
        verify_parts_tab_regeneration_parity()
        and verify_consumable_tab_regeneration_parity()
        and verify_weapon_tab_regeneration_parity()
    )
    ok = (
        melee_ok
        and assembly_hidden
        and parity_ok
        and shield_ok
        and verify_parts_prefix_probes()
        and verify_consumable_filters()
        and verify_gun_ammo_block()
        and verify_unified_firearm_order()
        and verify_control_inf_water()
        and verify_weapon_tab_coverage()
        and verify_weapon_mod_block()
        and verify_weapon_tail_sections()
        and verify_meteorite_sword_damage_order()
        and verify_parts_tab_bedrock_jei_block()
    )
    print(f"[GOAL] creative_grouping_pass={ok}")
    return ok


def main() -> int:
    SCRATCH.mkdir(parents=True, exist_ok=True)
    ok = check_creative_grouping_goal()
    (SCRATCH / "verify-goal-result.txt").write_text(
        f"verify_creative_tab_grouping.py version={GOAL_CLUSTER_VERSION}\n"
        f"creative_grouping_pass={ok}\n",
        encoding="utf-8",
    )
    return 0 if ok else 1


if __name__ == "__main__":
    raise SystemExit(main())