"""Audit creative tab membership: port vs EE reference + heuristics (gradle project tools/)."""
from __future__ import annotations

import csv
import os
import sys
from pathlib import Path

PROJ = Path(__file__).resolve().parents[1]
TOOLS = Path(__file__).resolve().parent
EE_MOD_ITEMS = PROJ.parent / "мод hbmntm/NTM-Extended-GitHub/src/main/java/com/hbm/items/ModItems.java"
EE_MOD_BLOCKS = PROJ.parent / "мод hbmntm/NTM-Extended-GitHub/src/main/java/com/hbm/blocks/ModBlocks.java"
PORT_MOD_ITEMS = PROJ / "src/main/java/com/hbm/items/ModItems.java"
PORT_MOD_BLOCKS = PROJ / "src/main/java/com/hbm/blocks/ModBlocks.java"
ORDER = PROJ / "src/main/resources/assets/hbm/creative_tab_order.txt"
SCRATCH = Path(os.environ.get("GOAL_SCRATCH", r"C:\Temp\grok-goal-f7963e6bf0c1\implementer"))

sys.path.insert(0, str(TOOLS))
from cluster_creative_tab_order import (  # noqa: E402
    CONTROL_BOUND_PARTS_PATHS,
    GOAL_CLUSTER_VERSION,
    MISSILE_BOUND_PARTS_PATHS,
    WEAPON_BOUND_PARTS_PATHS,
    is_gun_ammo_path,
    registry_path,
)
from creative_tab_parse import collect_port_tab_entries, parse_entries  # noqa: E402

GOAL_MOVED_ITEMS = (
    "gun_cryolator_ammo",
    "gun_dash_ammo",
    "gun_defabricator_ammo",
    "gun_emp_ammo",
    "gun_euthanasia_ammo",
    "gun_hp_ammo",
    "gun_immolator_ammo",
    "gun_jack_ammo",
    "gun_spark_ammo",
    "cc_plasma_gun",
    "turret_cheapo_ammo",
    "turret_control",
    "turret_cwis_ammo",
    "turret_flamer_ammo",
    "turret_heavy_ammo",
    "turret_light_ammo",
    "turret_rocket_ammo",
    "turret_spitfire_ammo",
    "turret_tau_ammo",
    "inf_water_mk3",
    "inf_water_mk4",
    "missile_soyuz0",
    "missile_soyuz1",
    "missile_soyuz2",
)


def load_declared_tabs(mod_items: Path, mod_blocks: Path) -> dict[str, str]:
    tabs = collect_port_tab_entries(mod_items, mod_blocks)
    declared: dict[str, str] = {}
    for tab, entries in tabs.items():
        for registry_key, path in entries:
            declared[registry_path(registry_key)] = tab
    return declared


def load_ee_tabs() -> dict[str, str]:
    if not EE_MOD_ITEMS.exists():
        return {}
    declared: dict[str, str] = {}
    for path in (EE_MOD_BLOCKS, EE_MOD_ITEMS):
        if not path.exists():
            continue
        kind = "block" if path.name.startswith("ModBlocks") else "item"
        for registry_key, reg_path, tab in parse_entries(path, kind):
            if tab:
                declared[registry_path(reg_path)] = tab
    return declared


def heuristic_tab(path: str) -> str | None:
    if path in WEAPON_BOUND_PARTS_PATHS:
        return "weaponTab"
    if path in MISSILE_BOUND_PARTS_PATHS:
        return "missileTab"
    if path in CONTROL_BOUND_PARTS_PATHS:
        return "controlTab"
    if is_gun_ammo_path(path):
        return "weaponTab"
    if path.startswith("turret_"):
        return "weaponTab"
    return None


def load_order_tabs() -> dict[str, set[str]]:
    tabs: dict[str, set[str]] = {}
    current = None
    for raw in ORDER.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        if line.startswith("@"):
            current = line[1:]
            tabs.setdefault(current, set())
            continue
        if current and "=" in line:
            tabs[current].add(line.split("=", 1)[0].strip())
    return tabs


def main() -> int:
    SCRATCH.mkdir(parents=True, exist_ok=True)
    port = load_declared_tabs(PORT_MOD_ITEMS, PORT_MOD_BLOCKS)
    ee = load_ee_tabs()
    order_tabs = load_order_tabs()

    rows: list[dict[str, str]] = []
    all_paths = sorted(set(port) | set(ee))

    for path in all_paths:
        declared = port.get(path)
        ee_tab = ee.get(path)
        heuristic = heuristic_tab(path)
        expected = ee_tab or heuristic or declared
        if ee_tab and heuristic and ee_tab != heuristic:
            expected = heuristic
        mismatch = bool(expected and declared and expected != declared)
        order_tab = None
        for tab, keys in order_tabs.items():
            if path in keys or f"hbm:{path}" in keys:
                order_tab = tab
                break
        order_mismatch = bool(declared and order_tab and declared != order_tab)
        if mismatch or order_mismatch or path in GOAL_MOVED_ITEMS:
            rows.append({
                "registry_path": path,
                "declared_tab": declared or "",
                "ee_tab": ee_tab or "",
                "heuristic_tab": heuristic or "",
                "expected_tab": expected or "",
                "order_tab": order_tab or "",
                "mismatch": str(mismatch),
                "order_mismatch": str(order_mismatch),
                "reason": "goal_moved" if path in GOAL_MOVED_ITEMS else ("decl_vs_expected" if mismatch else "order_vs_declared"),
            })

    csv_path = SCRATCH / "tab-membership-audit.csv"
    with csv_path.open("w", newline="", encoding="utf-8") as fh:
        writer = csv.DictWriter(
            fh,
            fieldnames=[
                "registry_path",
                "declared_tab",
                "ee_tab",
                "heuristic_tab",
                "expected_tab",
                "order_tab",
                "mismatch",
                "order_mismatch",
                "reason",
            ],
        )
        writer.writeheader()
        writer.writerows(rows)

    hard_mismatches = [
        r for r in rows
        if (r["mismatch"] == "True" or r["order_mismatch"] == "True")
        and (r["heuristic_tab"] or r["reason"] == "goal_moved")
    ]
    goal_ok = all(
        port.get(path) == {
            "gun_cryolator_ammo": "weaponTab",
            "gun_dash_ammo": "weaponTab",
            "gun_defabricator_ammo": "weaponTab",
            "gun_emp_ammo": "weaponTab",
            "gun_euthanasia_ammo": "weaponTab",
            "gun_hp_ammo": "weaponTab",
            "gun_immolator_ammo": "weaponTab",
            "gun_jack_ammo": "weaponTab",
            "gun_spark_ammo": "weaponTab",
            "cc_plasma_gun": "weaponTab",
            "turret_cheapo_ammo": "weaponTab",
            "turret_control": "weaponTab",
            "turret_cwis_ammo": "weaponTab",
            "turret_flamer_ammo": "weaponTab",
            "turret_heavy_ammo": "weaponTab",
            "turret_light_ammo": "weaponTab",
            "turret_rocket_ammo": "weaponTab",
            "turret_spitfire_ammo": "weaponTab",
            "turret_tau_ammo": "weaponTab",
            "inf_water_mk3": "controlTab",
            "inf_water_mk4": "controlTab",
            "missile_soyuz0": "missileTab",
            "missile_soyuz1": "missileTab",
            "missile_soyuz2": "missileTab",
        }.get(path)
        for path in GOAL_MOVED_ITEMS
    )

    summary = (
        f"audit_tab_membership.py version={GOAL_CLUSTER_VERSION}\n"
        f"audited_paths={len(all_paths)}\n"
        f"report_rows={len(rows)}\n"
        f"hard_mismatches={len(hard_mismatches)}\n"
        f"goal_moved_items_ok={goal_ok}\n"
        f"csv={csv_path}\n"
    )
    (SCRATCH / "tab-membership-summary.txt").write_text(summary, encoding="utf-8")
    print(summary)
    if hard_mismatches:
        for row in hard_mismatches[:15]:
            print(f"  MISMATCH {row['registry_path']}: declared={row['declared_tab']} expected={row['expected_tab']} order={row['order_tab']}")
    return 0 if goal_ok and not hard_mismatches else 1


if __name__ == "__main__":
    raise SystemExit(main())