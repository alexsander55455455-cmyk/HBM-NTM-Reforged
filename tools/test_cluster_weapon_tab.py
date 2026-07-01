"""Unit tests for cluster_weapon_tab (creative-tab-grouping-5: gun_ammo contiguity, no revolver-family contiguity)."""
from __future__ import annotations

import sys
from pathlib import Path

TOOLS = Path(__file__).resolve().parent
sys.path.insert(0, str(TOOLS))

from cluster_creative_tab_order import (  # noqa: E402
    FIREARM_ORDER,
    GOAL_CLUSTER_VERSION,
    cluster_weapon_tab,
    is_gun_ammo_path,
    registry_path,
)


def test_gun_ammo_block_contiguous() -> None:
    keys = [
        "gun_revolver_schrabidium",
        "gun_revolver_schrabidium_ammo",
        "clip_revolver_schrabidium",
        "gun_b92",
        "gun_revolver_iron",
        "gun_revolver_iron_ammo",
        "gun_osipr_ammo",
        "gun_cryolator_ammo",
    ]
    out = cluster_weapon_tab(keys)
    ammo_hits = [i for i, k in enumerate(out) if is_gun_ammo_path(registry_path(k))]
    assert len(ammo_hits) == 4, out
    assert ammo_hits[-1] - ammo_hits[0] + 1 == len(ammo_hits), out
    schrab_gun = out.index("gun_revolver_schrabidium")
    schrab_ammo = out.index("gun_revolver_schrabidium_ammo")
    assert schrab_ammo != schrab_gun + 1, out


def test_melee_immediately_after_shimmer_before_meteorite() -> None:
    keys = [
        "hs_sword",
        "hf_sword",
        "shimmer_axe",
        "meteorite_sword",
        "meteorite_sword_seared",
        "mese_pickaxe",
        "mese_axe",
        "dnt_sword",
        "dwarven_pickaxe",
        "mese_gavel",
        "gun_deagle",
    ]
    out = cluster_weapon_tab(keys)
    shimmer = out.index("shimmer_axe")
    assert out[shimmer + 1] == "mese_pickaxe"
    block = out[shimmer + 1 : shimmer + 6]
    assert block == [
        "mese_pickaxe",
        "mese_axe",
        "dnt_sword",
        "dwarven_pickaxe",
        "mese_gavel",
    ], block
    assert out[out.index("mese_gavel") + 1] == "meteorite_sword"
    assert out.index("meteorite_sword_seared") > out.index("meteorite_sword")


def test_unified_firearm_order() -> None:
    keys = [
        "gun_revolver",
        "gun_b92",
        "gun_fatman",
        "gun_deagle",
        "gun_darter",
        "gun_ar15",
        "crucible",
        "gun_b93",
        "gun_osipr_ammo",
        "gun_revolver_ammo",
    ]
    out = cluster_weapon_tab(keys)
    paths = [registry_path(k) for k in out]
    fire_sub = [p for p in paths if p in FIREARM_ORDER]
    assert fire_sub == [n for n in FIREARM_ORDER if n in paths], (paths, fire_sub)
    assert paths.index("gun_b92") < paths.index("gun_deagle"), paths
    first_ammo = paths.index("gun_osipr_ammo")
    last_fire = max(paths.index(p) for p in fire_sub)
    assert first_ammo > last_fire, paths
    assert paths.index("gun_revolver_ammo") == paths.index("gun_osipr_ammo") + 1, paths


def test_shields_sorted_together() -> None:
    keys = ["desh_shield", "hs_sword", "elec_shield", "cobalt_shield", "steel_shield"]
    out = cluster_weapon_tab(keys)
    shield_idx = [out.index(k) for k in ("cobalt_shield", "desh_shield", "elec_shield", "steel_shield")]
    assert shield_idx == sorted(shield_idx), out
    assert out.index("hs_sword") < shield_idx[0], out


def main() -> int:
    assert GOAL_CLUSTER_VERSION == "creative-tab-grouping-5"
    test_gun_ammo_block_contiguous()
    test_melee_immediately_after_shimmer_before_meteorite()
    test_unified_firearm_order()
    test_shields_sorted_together()
    print(f"test_cluster_weapon_tab PASS version={GOAL_CLUSTER_VERSION}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())