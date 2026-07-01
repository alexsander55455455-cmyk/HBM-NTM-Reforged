"""Unit tests for cluster_parts_tab and cluster_consumable_tab (gradle project tools/)."""
from __future__ import annotations

import sys
from pathlib import Path

TOOLS = Path(__file__).resolve().parent
PROJ = TOOLS.parent
ORDER = PROJ / "src/main/resources/assets/hbm/creative_tab_order.txt"

sys.path.insert(0, str(TOOLS))

from cluster_creative_tab_order import (  # noqa: E402
    GOAL_CLUSTER_VERSION,
    WEAPON_BOUND_PARTS_PATHS,
    cluster_consumable_tab,
    cluster_parts_tab,
    is_gun_ammo_path,
    registry_path,
)

COMPLAINED_INGOTS = (
    "ingot_ac227",
    "ingot_neodymium",
    "ingot_radspice",
    "ingot_strontium",
)
COMPLAINED_NUGGETS = ("nugget_ac227", "nugget_radspice")
COMPLAINED_POWDERS = (
    "powder_ac227",
    "powder_ac227_tiny",
    "powder_iron_tiny",
    "powder_radspice",
    "powder_radspice_tiny",
)

FORBIDDEN_PARTS_PATHS = frozenset(WEAPON_BOUND_PARTS_PATHS) | frozenset({
    "missile_soyuz0",
    "missile_soyuz1",
    "missile_soyuz2",
    "inf_water_mk3",
    "inf_water_mk4",
})


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


def prefix_span(keys: list[str], prefix: str) -> tuple[int, int, list[int]]:
    hits = [i for i, k in enumerate(keys) if registry_path(k).startswith(prefix)]
    if not hits:
        return -1, -1, hits
    return hits[0], hits[-1], hits


def assert_alpha_sorted(keys: list[str], indices: list[int]) -> None:
    paths = [registry_path(keys[i]) for i in indices]
    assert paths == sorted(paths), paths


def test_parts_tab_regeneration_parity_on_disk() -> None:
    on_disk = load_tab_keys("partsTab")
    assert on_disk, "partsTab empty"
    regenerated = cluster_parts_tab(list(on_disk))
    assert regenerated == on_disk, "partsTab must equal cluster_parts_tab(on_disk)"


def test_ingot_block_alpha() -> None:
    parts = load_tab_keys("partsTab")
    first, last, ingot_hits = prefix_span(parts, "ingot_")
    assert ingot_hits, "missing ingot_ block"
    assert_alpha_sorted(parts, ingot_hits)
    for name in COMPLAINED_INGOTS:
        idx = parts.index(name)
        assert first <= idx <= last, (name, idx, first, last)


def test_nugget_and_powder_blocks() -> None:
    parts = load_tab_keys("partsTab")
    n_first, n_last, n_hits = prefix_span(parts, "nugget_")
    p_first, p_last, p_hits = prefix_span(parts, "powder_")
    assert_alpha_sorted(parts, n_hits)
    assert_alpha_sorted(parts, p_hits)
    for name in COMPLAINED_NUGGETS:
        idx = parts.index(name)
        assert n_first <= idx <= n_last, name
    for name in COMPLAINED_POWDERS:
        idx = parts.index(name)
        assert p_first <= idx <= p_last, name


def test_parts_tab_excludes_weapon_bound_items() -> None:
    parts = load_tab_keys("partsTab")
    paths = {registry_path(k) for k in parts}
    for path in FORBIDDEN_PARTS_PATHS:
        assert path not in paths, f"{path} must not be on partsTab"
    for path in paths:
        assert not is_gun_ammo_path(path), f"{path} gun ammo must not be on partsTab"


def test_consumable_filters_cluster() -> None:
    consumable = load_tab_keys("consumableTab")
    regenerated = cluster_consumable_tab(list(consumable))
    assert regenerated == consumable
    hits = [i for i, k in enumerate(consumable) if registry_path(k).startswith("gas_mask_filter")]
    assert hits and hits[-1] - hits[0] + 1 == len(hits)
    assert "gas_mask_filter_radon" in consumable
    assert consumable.index("gas_mask_filter_radon") in hits
    filter_paths = [registry_path(consumable[i]) for i in hits]
    assert filter_paths == sorted(filter_paths)


def test_cluster_parts_scrambled_exotic_ingots() -> None:
    keys = [
        "inf_water_mk4",
        "ingot_strontium",
        "crystal_coal",
        "ingot_steel",
        "ingot_ac227",
        "ingot_neodymium",
        "ingot_radspice",
        "nugget_radspice",
        "powder_radspice_tiny",
        "powder_ac227",
        "rod_ac227",
        "hull_big_steel",
        "hbmspace:flesh",
    ]
    out = cluster_parts_tab(keys)
    paths = [registry_path(k) for k in out]
    ingot_slice = [p for p in paths if p.startswith("ingot_")]
    assert ingot_slice == sorted(ingot_slice), ingot_slice
    assert paths.index("ingot_ac227") < paths.index("ingot_steel") < paths.index("ingot_strontium")
    assert paths.index("crystal_coal") > paths.index("powder_ac227")
    assert paths.index("rod_ac227") > paths.index("crystal_coal")
    assert paths.index("hull_big_steel") > paths.index("rod_ac227")
    assert out[-1] == "hbmspace:flesh"


def main() -> int:
    assert GOAL_CLUSTER_VERSION == "creative-tab-grouping-5"
    test_parts_tab_regeneration_parity_on_disk()
    test_ingot_block_alpha()
    test_nugget_and_powder_blocks()
    test_parts_tab_excludes_weapon_bound_items()
    test_consumable_filters_cluster()
    test_cluster_parts_scrambled_exotic_ingots()
    print(f"test_cluster_parts_tab PASS version={GOAL_CLUSTER_VERSION}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())