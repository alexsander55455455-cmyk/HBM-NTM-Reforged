"""Verify global flattened JEI sort order matches creative_tab_order.txt grouping."""
from __future__ import annotations

import sys
from pathlib import Path

PROJ = Path(__file__).resolve().parents[1]
ORDER = PROJ / "src/main/resources/assets/hbm/creative_tab_order.txt"

TAB_PRIORITY = (
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
)


def load_tab_paths() -> dict[str, list[str]]:
    tabs: dict[str, list[str]] = {}
    current: str | None = None
    rows: dict[str, list[tuple[str, int]]] = {}
    for raw in ORDER.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        if line.startswith("@"):
            current = line[1:]
            rows[current] = []
            continue
        if current and "=" in line:
            path, idx = line.split("=", 1)
            rows[current].append((path, int(idx)))
    for tab, entries in rows.items():
        entries.sort(key=lambda x: x[1])
        tabs[tab] = [p for p, _ in entries]
    return tabs


def global_positions(tabs: dict[str, list[str]]) -> dict[str, int]:
    pos: dict[str, int] = {}
    offset = 0
    for tab in TAB_PRIORITY:
        entries = tabs.get(tab, [])
        for i, path in enumerate(entries):
            pos[path] = offset + i
        offset += len(entries)
    return pos


def path_tail(path: str) -> str:
    return path.split(":", 1)[-1]


def has_prefix(path: str, prefix: str) -> bool:
    return path_tail(path).startswith(prefix)


def prefix_block_ok(tab_paths: list[str], prefix: str, probes: list[str]) -> bool:
    hits = [(i, p) for i, p in enumerate(tab_paths) if has_prefix(p, prefix)]
    if not hits:
        return False
    first, last = hits[0][0], hits[-1][0]
    if last - first + 1 != len(hits):
        return False
    for probe in probes:
        if probe not in tab_paths:
            return False
        idx = tab_paths.index(probe)
        if idx < first or idx > last:
            return False
    return True


def in_block(tab_paths: list[str], probes: list[str], start_path: str, end_path: str) -> bool:
    if start_path not in tab_paths or end_path not in tab_paths:
        return False
    lo = tab_paths.index(start_path)
    hi = tab_paths.index(end_path)
    if lo > hi:
        lo, hi = hi, lo
    for probe in probes:
        if probe not in tab_paths:
            return False
        idx = tab_paths.index(probe)
        if idx < lo or idx > hi:
            return False
    return True


def before(a: str, b: str, pos: dict[str, int]) -> bool:
    return a in pos and b in pos and pos[a] < pos[b]


def main() -> int:
    tabs = load_tab_paths()
    pos = global_positions(tabs)
    parts = tabs.get("partsTab", [])
    weapon = tabs.get("weaponTab", [])
    ok = True

    if not prefix_block_ok(parts, "powder_", ["powder_ac227", "powder_radspice", "powder_iron_tiny"]):
        print("FAIL powder probes not in contiguous powder_ block on partsTab")
        ok = False
    else:
        print("PASS powder block on partsTab")

    if not before("powder_radspice_tiny", "pocket_ptsd", pos):
        print("FAIL powders should precede pocket_ptsd in global order")
        ok = False
    else:
        print("PASS powders before pocket_ptsd")

    if not prefix_block_ok(parts, "ingot_", ["ingot_ac227", "ingot_neodymium", "ingot_radspice", "ingot_strontium"]):
        print("FAIL ingot probes not in contiguous ingot_ block")
        ok = False
    else:
        print("PASS ingot block on partsTab")

    if not before("ingot_strontium", "inf_water_mk4", pos):
        print("FAIL ingots should precede inf_water_mk4")
        ok = False
    else:
        print("PASS ingots before inf_water_mk4")

    if not prefix_block_ok(parts, "nugget_", ["nugget_ac227", "nugget_radspice"]):
        print("FAIL nugget probes not in contiguous nugget_ block")
        ok = False
    else:
        print("PASS nugget block on partsTab")

    if not before("nugget_radspice", "missile_soyuz2", pos):
        print("FAIL nuggets should precede missile_soyuz2")
        ok = False
    else:
        print("PASS nuggets before missile_soyuz2")

    if not prefix_block_ok(weapon, "_shield", ["alloy_shield", "steel_shield", "titanium_shield"]):
        # shields end with _shield or are alloy_shield
        shield_hits = [(i, p) for i, p in enumerate(weapon) if p.endswith("_shield")]
        probes_ok = all(
            any(p == probe for _, p in shield_hits)
            for probe in ["alloy_shield", "steel_shield", "titanium_shield"]
        )
        contiguous = (
            len(shield_hits) > 0
            and shield_hits[-1][0] - shield_hits[0][0] + 1 == len(shield_hits)
            and probes_ok
        )
        if not contiguous:
            print("FAIL shield block not contiguous on weaponTab")
            ok = False
        else:
            print("PASS shield block on weaponTab")
    else:
        print("PASS shield block on weaponTab")

    if not in_block(weapon, ["mese_gavel"], "shimmer_axe", "meteorite_sword_seared"):
        print("FAIL mese_gavel not in melee cluster on weaponTab")
        ok = False
    else:
        print("PASS melee cluster on weaponTab")

    if not before("ff_fluid_duct", "mese_gavel", pos):
        print("FAIL templateTab (ff_fluid_duct) should precede weaponTab melee in global order")
        ok = False
    else:
        print("PASS templateTab before melee cluster")

    if not in_block(weapon, ["ammo_arty", "ammo_himars"], "ammo_357_desh", "turret_arty"):
        print("FAIL artillery ammo not in weaponTab ammo section")
        ok = False
    else:
        print("PASS artillery ammo block")

    print(f"OVERALL: {'PASS' if ok else 'FAIL'}")
    return 0 if ok else 1


if __name__ == "__main__":
    raise SystemExit(main())