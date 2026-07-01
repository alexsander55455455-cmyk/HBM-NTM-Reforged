#!/usr/bin/env python3
"""Audit HBM port items with no recipe output vs EE reference crafts.

Outputs CSV buckets:
  PORT   - reference has craft, port does not (candidate to port)
  SKIP   - reference also has no craft (secrets, loot, intentional)
  REVIEW - ambiguous / port-only CE item / whitelist edge case
  OK     - port has at least one recipe output
"""

from __future__ import annotations

import csv
import re
import sys
from collections import defaultdict
from dataclasses import dataclass, field
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PORT_JAVA = ROOT / "src" / "main" / "java"
EE_ROOT = ROOT.parent / "мод hbmntm" / "NTM-Extended-GitHub" / "src" / "main" / "java"
ORDER_FILE = ROOT / "src" / "main" / "resources" / "assets" / "hbm" / "creative_tab_order.txt"

REPORT_ALL = ROOT / "tools" / "recipe_coverage_report.csv"
REPORT_PORT_MISSING = ROOT / "tools" / "recipe_coverage_port_missing.csv"
REPORT_PORT = ROOT / "tools" / "recipe_coverage_action_port.csv"
REPORT_SKIP = ROOT / "tools" / "recipe_coverage_skip.csv"
REPORT_REVIEW = ROOT / "tools" / "recipe_coverage_review.csv"

# Intentional no-craft / non-craftable content (base ids without meta variants).
SKIP_REGISTRY_SUFFIXES = (
    "_debug",
)
SKIP_REGISTRY_EXACT = {
    "ammo_debug",
    "gun_debug",
    "gun_folly",
    "gun_aberrator",
    "gun_aberrator_eott",
    "gun_super_shotgun",
    "gun_revolver_inverted",
    "pellet_rtg_depleted",
    "item_secret",
    "fluid_icon",
    "fluid_duct",
    "fluid_tank",
    "fluid_barrel",
    "fluid_pack",
    "fluid_cell",
    "fluid_identifier",
    "bedrock_ore_fragment",
    "nuclear_waste_long",
    "nuclear_waste_long_tiny",
    "nuclear_waste_long_depleted",
    "nuclear_waste_long_depleted_tiny",
}

# Registry paths that are intermediate / processing-only in practice (manual review, not auto PORT).
REVIEW_HINTS = (
    "depleted",
    "_scrap",
    "_waste",
    "_hot",
    "_exhausted",
    "chunk_",
    "ore_",
    "bedrock_ore",
    "fluid_",
    "custom_missile",
    "missile_",
    "ammo_container",
)

ITEM_DECL = re.compile(
    r"public\s+static\s+(?:final\s+)?Item\s+(\w+)\s*=\s*new\s+[^;]+;",
    re.DOTALL,
)
BLOCK_DECL = re.compile(
    r"public\s+static\s+(?:final\s+)?Block\s+(\w+)\s*=\s*new\s+[^;]+;",
    re.DOTALL,
)
STRING_LITERAL = re.compile(r'"([a-z][a-z0-9_]*)"')
SPACE_ITEM = re.compile(
    r"public\s+static\s+(?:final\s+)?Item\s+(\w+)\s*=\s*new\s+[^;]+;",
    re.DOTALL,
)
SPACE_BLOCK = re.compile(
    r"public\s+static\s+(?:final\s+)?Block\s+(\w+)\s*=\s*new\s+[^;]+;",
    re.DOTALL,
)

OUTPUT_PATTERNS = [
    # Crafting table outputs (first ItemStack in call)
    re.compile(
        r"(?:addRecipeAuto|addShapelessAuto|addRecipe|addShapeless)"
        r"\(\s*(?:DictFrame\.fromOne\(\s*)?new\s+ItemStack\(\s*(Mod(?:Items|Blocks)(?:Space)?)\.(\w+)",
        re.MULTILINE,
    ),
    re.compile(
        r"(?:addRecipeAuto|addShapelessAuto|addRecipe|addShapeless)"
        r"\(\s*DictFrame\.fromOne\(\s*(Mod(?:Items|Blocks)(?:Space)?)\.(\w+)",
        re.MULTILINE,
    ),
    # Machine / generic recipe outputs
    re.compile(r"\.outputItems\(\s*new\s+ItemStack\(\s*(Mod(?:Items|Blocks)(?:Space)?)\.(\w+)", re.MULTILINE),
    re.compile(r"\.setIcon\(\s*(Mod(?:Items|Blocks)(?:Space)?)\.(\w+)", re.MULTILINE),
    re.compile(
        r"DFCRecipes\.setRecipe\([^,]+,\s*(?:new\s+ItemStack\(\s*)?(?:Mod(?:Items|Blocks)(?:Space)?)\.(\w+),\s*new\s+ItemStack\(\s*(Mod(?:Items|Blocks)(?:Space)?)\.(\w+)",
        re.MULTILINE,
    ),
    re.compile(
        r"new\s+PedestalRecipe\(\s*new\s+ItemStack\(\s*(Mod(?:Items|Blocks)(?:Space)?)\.(\w+)",
        re.MULTILINE,
    ),
    re.compile(
        r"new\s+AnvilSmithing(?:Hot)?Recipe\([^,]+,\s*new\s+ItemStack\(\s*(Mod(?:Items|Blocks)(?:Space)?)\.(\w+)",
        re.MULTILINE,
    ),
    re.compile(
        r"MagicRecipes\.register\(\s*new\s+ItemStack\(\s*(Mod(?:Items|Blocks)(?:Space)?)\.(\w+)",
        re.MULTILINE,
    ),
    re.compile(
        r"addSmelting\(\s*new\s+ItemStack\(\s*(Mod(?:Items|Blocks)(?:Space)?)\.(\w+)",
        re.MULTILINE,
    ),
]

RECIPE_FILE_HINT = re.compile(r"(?:Recipes|CraftingManager|Smelting)\.java$", re.IGNORECASE)

EE_CRAFTING_FILES = [
    EE_ROOT / "com" / "hbm" / "main" / "CraftingManager.java",
]
EE_RECIPE_GLOBS = [
    EE_ROOT / "com" / "hbm" / "inventory" / "recipes",
    EE_ROOT / "com" / "hbm" / "inventory",
]

PORT_RECIPE_SCAN_DIRS = [
    PORT_JAVA / "com" / "hbm" / "main" / "CraftingManager.java",
    PORT_JAVA / "com" / "hbm" / "crafting",
    PORT_JAVA / "com" / "hbm" / "inventory",
    PORT_JAVA / "com" / "hbmspace" / "inventory",
    PORT_JAVA / "com" / "hbmspace" / "crafting",
]


@dataclass
class RegisteredItem:
    namespace: str
    registry: str
    field: str
    kind: str  # item | block
    tab: str = ""
    note: str = ""


@dataclass
class RecipeHit:
    sources: set[str] = field(default_factory=set)


def load_tab_order() -> dict[str, str]:
    tabs: dict[str, str] = {}
    current = ""
    for raw in ORDER_FILE.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        if line.startswith("@"):
            current = line[1:]
            continue
        if "=" in line:
            path, _ = line.split("=", 1)
            tabs[path.strip()] = current
    return tabs


def registry_from_decl(body: str, field: str) -> str:
    strings = STRING_LITERAL.findall(body)
    for s in reversed(strings):
        if s != field and not s.startswith("Enum") and s.islower():
            return s
    return field


def parse_registrations(path: Path, namespace: str, item_re: re.Pattern, block_re: re.Pattern) -> list[RegisteredItem]:
    if not path.exists():
        return []
    text = path.read_text(encoding="utf-8", errors="replace")
    out: list[RegisteredItem] = []
    for m in item_re.finditer(text):
        field = m.group(1)
        reg = registry_from_decl(m.group(0), field)
        out.append(RegisteredItem(namespace, reg, field, "item"))
    for m in block_re.finditer(text):
        field = m.group(1)
        reg = registry_from_decl(m.group(0), field)
        out.append(RegisteredItem(namespace, reg, field, "block"))
    return out


def collect_registered(tabs: dict[str, str]) -> dict[str, RegisteredItem]:
    items: dict[str, RegisteredItem] = {}
    sources = [
        (PORT_JAVA / "com" / "hbm" / "items" / "ModItems.java", "hbm", ITEM_DECL, BLOCK_DECL),
        (PORT_JAVA / "com" / "hbm" / "blocks" / "ModBlocks.java", "hbm", ITEM_DECL, BLOCK_DECL),
        (PORT_JAVA / "com" / "hbmspace" / "items" / "ModItemsSpace.java", "hbmspace", SPACE_ITEM, SPACE_BLOCK),
        (PORT_JAVA / "com" / "hbmspace" / "blocks" / "ModBlocksSpace.java", "hbmspace", SPACE_ITEM, SPACE_BLOCK),
    ]
    for path, ns, item_re, block_re in sources:
        for reg_item in parse_registrations(path, ns, item_re, block_re):
            key = f"{reg_item.namespace}:{reg_item.registry}"
            if key in items:
                continue
            reg_item.tab = tabs.get(reg_item.registry, tabs.get(f"{ns}:{reg_item.registry}", ""))
            items[key] = reg_item
    return items


def iter_java_files(paths: list[Path], *, recipe_only: bool = False) -> list[Path]:
    files: list[Path] = []
    for base in paths:
        if base.is_file():
            files.append(base)
        elif base.exists():
            for path in sorted(base.rglob("*.java")):
                if recipe_only and not RECIPE_FILE_HINT.search(path.name):
                    continue
                files.append(path)
    return files


def extract_outputs(text: str, source: str, field_maps: dict[str, dict[str, str]], hits: dict[str, RecipeHit]) -> None:
    patterns = list(OUTPUT_PATTERNS)
    if RECIPE_FILE_HINT.search(source):
        patterns.append(
            re.compile(r"new\s+ItemStack\(\s*(Mod(?:Items|Blocks)(?:Space)?)\.(\w+)", re.MULTILINE)
        )
    for pat in patterns:
        for m in pat.finditer(text):
            groups = m.groups()
            if len(groups) == 2:
                mod_class, field = groups
            elif len(groups) == 4:
                # DFC: input field ignored, output is last pair
                mod_class, field = groups[2], groups[3]
            else:
                continue
            ns = "hbmspace" if "Space" in mod_class else "hbm"
            fmap = field_maps.get(ns, {})
            registry = fmap.get(field)
            if not registry:
                continue
            key = f"{ns}:{registry}"
            hits[key].sources.add(source)


def build_field_maps(registered: dict[str, RegisteredItem]) -> dict[str, dict[str, str]]:
    maps: dict[str, dict[str, str]] = defaultdict(dict)
    for item in registered.values():
        maps[item.namespace][item.field] = item.registry
    return maps


def source_label(path: Path) -> str:
    try:
        return str(path.relative_to(ROOT)).replace("\\", "/")
    except ValueError:
        return str(path).replace("\\", "/")


def scan_recipes(paths: list[Path], field_maps: dict[str, dict[str, str]]) -> dict[str, RecipeHit]:
    hits: dict[str, RecipeHit] = defaultdict(RecipeHit)
    for path in iter_java_files(paths):
        extract_outputs(
            path.read_text(encoding="utf-8", errors="replace"),
            source_label(path),
            field_maps,
            hits,
        )
    return hits


def should_skip(registry: str, item: RegisteredItem) -> tuple[bool, str]:
    if registry in SKIP_REGISTRY_EXACT:
        return True, "whitelist_exact"
    for suf in SKIP_REGISTRY_SUFFIXES:
        if registry.endswith(suf):
            return True, f"whitelist_suffix:{suf}"
    if registry.startswith("gun_") and "lore" in item.note.lower():
        return True, "lore_gun"
    return False, ""


def classify(
    item: RegisteredItem,
    port_hits: dict[str, RecipeHit],
    ee_hits: dict[str, RecipeHit],
) -> tuple[str, str]:
    key = f"{item.namespace}:{item.registry}"
    port_has = key in port_hits
    ee_has = key in ee_hits

    if port_has:
        src = ";".join(sorted(port_hits[key].sources)[:3])
        if len(port_hits[key].sources) > 3:
            src += f";+{len(port_hits[key].sources) - 3}"
        return "OK", src

    skip, skip_reason = should_skip(item.registry, item)
    if skip:
        return "SKIP", skip_reason

    if ee_has:
        return "PORT", ";".join(sorted(ee_hits[key].sources)[:3])

    for hint in REVIEW_HINTS:
        if hint in item.registry:
            return "REVIEW", f"hint:{hint}"

    # CE/sedna-only items won't be in EE CraftingManager
    if item.registry.startswith("gun_") and item.tab == "weaponTab":
        return "REVIEW", "sedna_or_ee_weapon_check"

    return "SKIP", "no_reference_craft"


def write_csv(path: Path, rows: list[dict]) -> None:
    if not rows:
        path.write_text("", encoding="utf-8")
        return
    with path.open("w", encoding="utf-8", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=list(rows[0].keys()))
        writer.writeheader()
        writer.writerows(rows)


def main() -> int:
    tabs = load_tab_order()
    registered = collect_registered(tabs)
    field_maps = build_field_maps(registered)

    port_paths = PORT_RECIPE_SCAN_DIRS + [PORT_JAVA / "com" / "hbm" / "crafting" / "SmeltingRecipes.java"]
    ee_paths = EE_CRAFTING_FILES + [p for g in EE_RECIPE_GLOBS for p in iter_java_files([g], recipe_only=True)]

    port_hits = scan_recipes(port_paths, field_maps)
    ee_hits = scan_recipes(ee_paths, field_maps)

    rows: list[dict] = []
    buckets: dict[str, list[dict]] = defaultdict(list)

    tab_order = {t: i for i, t in enumerate([
        "partsTab", "controlTab", "templateTab", "resourceTab", "blockTab",
        "machineTab", "nukeTab", "missileTab", "weaponTab", "consumableTab", "",
    ])}

    for key in sorted(registered.keys(), key=lambda k: (registered[k].namespace, registered[k].registry)):
        item = registered[key]
        verdict, detail = classify(item, port_hits, ee_hits)
        row = {
            "id": key,
            "tab": item.tab,
            "kind": item.kind,
            "field": item.field,
            "verdict": verdict,
            "port_has_recipe": "yes" if key in port_hits else "no",
            "ee_has_recipe": "yes" if key in ee_hits else "no",
            "detail": detail,
        }
        rows.append(row)
        if verdict != "OK":
            buckets[verdict].append(row)

    rows.sort(key=lambda r: (tab_order.get(r["tab"], 99), r["id"]))
    for v in buckets:
        buckets[v].sort(key=lambda r: (tab_order.get(r["tab"], 99), r["id"]))

    write_csv(REPORT_ALL, rows)
    write_csv(REPORT_PORT_MISSING, [r for r in rows if r["port_has_recipe"] == "no"])
    write_csv(REPORT_PORT, buckets["PORT"])
    write_csv(REPORT_SKIP, buckets["SKIP"])
    write_csv(REPORT_REVIEW, buckets["REVIEW"])

    total = len(rows)
    ok = sum(1 for r in rows if r["verdict"] == "OK")
    port_missing = sum(1 for r in rows if r["port_has_recipe"] == "no")
    print(f"Registered items: {total}")
    print(f"Port has recipe output: {ok}")
    print(f"Port missing recipe: {port_missing}")
    print(f"  PORT (add craft): {len(buckets['PORT'])}")
    print(f"  SKIP (intentional): {len(buckets['SKIP'])}")
    print(f"  REVIEW (manual): {len(buckets['REVIEW'])}")
    print(f"Wrote {REPORT_ALL.name}")
    print(f"Wrote {REPORT_PORT_MISSING.name}")
    print(f"Wrote {REPORT_PORT.name}")
    return 0


if __name__ == "__main__":
    sys.exit(main())