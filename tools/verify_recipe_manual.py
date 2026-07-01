#!/usr/bin/env python3
"""Deep verification for items the scanner marks as missing recipes.

Re-scans port sources with expanded output patterns, then assigns a final verdict:
  HAS_RECIPE   - craft exists (scanner false negative)
  CONFIRM_PORT - EE has craft, port truly missing
  CONFIRM_SKIP - no EE craft / intentional no-craft
  MANUAL       - needs human judgment (secrets, sedna guns, edge cases)
"""

from __future__ import annotations

import csv
import re
import sys
from collections import Counter, defaultdict
from dataclasses import dataclass, field
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tools"))

from audit_recipe_coverage import (  # noqa: E402
    EE_CRAFTING_FILES,
    EE_RECIPE_GLOBS,
    PORT_RECIPE_SCAN_DIRS,
    REVIEW_HINTS,
    SKIP_REGISTRY_EXACT,
    SKIP_REGISTRY_SUFFIXES,
    build_field_maps,
    collect_registered,
    iter_java_files,
    load_tab_order,
    scan_recipes,
    should_skip,
    source_label,
)

REPORT_MISSING = ROOT / "tools" / "recipe_coverage_port_missing.csv"
OUT_VERIFIED = ROOT / "tools" / "recipe_verification_report.csv"
OUT_SUMMARY = ROOT / "tools" / "recipe_verification_summary.txt"

# Extra patterns the base scanner misses.
DEEP_OUTPUT_PATTERNS = [
    re.compile(r"new\s+CrystallizerRecipe\(\s*Mod(?:Items|Blocks)(?:Space)?\.(\w+)", re.M),
    re.compile(r"new\s+CrystallizerRecipe\(\s*new\s+ItemStack\(\s*Mod(?:Items|Blocks)(?:Space)?\.(\w+)", re.M),
    re.compile(r"makeRecipe\([^)]*,\s*Mod(?:Items|Blocks)(?:Space)?\.(\w+)\s*\)", re.M),
    re.compile(r"makeRecipe\([^)]*,\s*new\s+ItemStack\(\s*Mod(?:Items|Blocks)(?:Space)?\.(\w+)", re.M),
    re.compile(r"registerRecipe\([^)]*,\s*Mod(?:Items|Blocks)(?:Space)?\.(\w+)", re.M),
    re.compile(r"registerRecipe\([^)]*,\s*new\s+ItemStack\(\s*Mod(?:Items|Blocks)(?:Space)?\.(\w+)", re.M),
    re.compile(r"new\s+MagicRecipe\(\s*new\s+ItemStack\(\s*Mod(?:Items|Blocks)(?:Space)?\.(\w+)", re.M),
    re.compile(r"recipes\.add\(\s*new\s+MagicRecipe\(\s*new\s+ItemStack\(\s*Mod(?:Items|Blocks)(?:Space)?\.(\w+)", re.M),
    re.compile(r"OutgasserRecipe\(\s*new\s+ItemStack\(\s*Mod(?:Items|Blocks)(?:Space)?\.(\w+)", re.M),
    re.compile(r"BreederRecipe\(\s*new\s+ItemStack\(\s*Mod(?:Items|Blocks)(?:Space)?\.(\w+)", re.M),
    re.compile(r"new\s+BreederRecipe\(\s*new\s+ItemStack\(\s*Mod(?:Items|Blocks)(?:Space)?\.(\w+)", re.M),
    re.compile(r"add(?:Helmet|Chest|Legs|Boots|Pickaxe|Axe|Shovel|Hoe|Sword)\([^,]+,\s*Mod(?:Items|Blocks)(?:Space)?\.(\w+)", re.M),
    re.compile(r"ShredderRecipe\(\s*new\s+ItemStack\(\s*Mod(?:Items|Blocks)(?:Space)?\.(\w+)", re.M),
    re.compile(r"new\s+ShredderRecipe\(\s*new\s+ItemStack\(\s*Mod(?:Items|Blocks)(?:Space)?\.(\w+)", re.M),
    re.compile(r"ArcWelderRecipe\(\s*new\s+ItemStack\(\s*Mod(?:Items|Blocks)(?:Space)?\.(\w+)", re.M),
    re.compile(r"new\s+ArcWelderRecipe\(\s*new\s+ItemStack\(\s*Mod(?:Items|Blocks)(?:Space)?\.(\w+)", re.M),
    re.compile(r"FusionRecipe\(\s*new\s+ItemStack\(\s*Mod(?:Items|Blocks)(?:Space)?\.(\w+)", re.M),
    re.compile(r"HadronRecipe\(\s*new\s+ItemStack\(\s*Mod(?:Items|Blocks)(?:Space)?\.(\w+)", re.M),
    re.compile(r"\.outputItems\(\s*Mod(?:Items|Blocks)(?:Space)?\.(\w+)", re.M),
    re.compile(r"SolidificationRecipes\.registerRecipe\([^,]+,\s*[^,]+,\s*Mod(?:Items|Blocks)(?:Space)?\.(\w+)", re.M),
    re.compile(r"SolidificationRecipes\.registerRecipe\([^,]+,\s*[^,]+,\s*new\s+ItemStack\(\s*Mod(?:Items|Blocks)(?:Space)?\.(\w+)", re.M),
    re.compile(r"GameRegistry\.addSmelting\([^,]+,\s*new\s+ItemStack\(\s*Mod(?:Items|Blocks)(?:Space)?\.(\w+)", re.M),
    re.compile(r"addRecipe\(\s*Mod(?:Items|Blocks)(?:Space)?\.(\w+)", re.M),
    re.compile(r"addRecipe\(\s*new\s+ItemStack\(\s*Mod(?:Items|Blocks)(?:Space)?\.(\w+)", re.M),
    re.compile(r"DFCRecipes\.setRecipe\([^,]+,\s*Mod(?:Items|Blocks)(?:Space)?\.(\w+)", re.M),
    re.compile(r"SolderingRecipe\(\s*new\s+ItemStack\(\s*Mod(?:Items|Blocks)(?:Space)?\.(\w+)", re.M),
    re.compile(r"PedestalRecipe\(\s*new\s+ItemStack\(\s*Mod(?:Items|Blocks)(?:Space)?\.(\w+)", re.M),
    re.compile(r"CokerRecipe\([^)]*new\s+ItemStack\(\s*Mod(?:Items|Blocks)(?:Space)?\.(\w+)", re.M),
    re.compile(r"Item\.getItemFromBlock\(\s*ModBlocks(?:Space)?\.(\w+)\s*\)", re.M),
    re.compile(r"addRecipeAuto\(\s*new\s+ItemStack\(\s*ModBlocks(?:Space)?\.(\w+)", re.M),
    re.compile(r"addShapelessAuto\(\s*new\s+ItemStack\(\s*ModBlocks(?:Space)?\.(\w+)", re.M),
    re.compile(r"addConcreteColor\([^,]+,\s*ModBlocks\.(\w+)", re.M),
    re.compile(r"AnvilConstructionRecipe\([^)]*AnvilOutput\(\s*new\s+ItemStack\(\s*Mod(?:Items|Blocks)(?:Space)?\.(\w+)", re.M),
    re.compile(r"AnvilOutput\(\s*new\s+ItemStack\(\s*Mod(?:Items|Blocks)(?:Space)?\.(\w+)", re.M),
    re.compile(r"setRecipe\([^,]+,\s*new\s+ItemStack\(\s*Mod(?:Items|Blocks)(?:Space)?\.(\w+)", re.M),
    re.compile(r"ShredderRecipes\.setRecipe\([^,]+,\s*new\s+ItemStack\(\s*Mod(?:Items|Blocks)(?:Space)?\.(\w+)", re.M),
]

SECRET_HINTS = (
    "secret", "debug", "lore", "glitch", "coin_", "medal_", "starter", "kit_",
    "spawn_", "key_", "card_", "maskman", "easter", "troll", "unused",
)
LOOT_ONLY_HINTS = (
    "fragment_", "chunk_", "ore_", "bedrock_ore", "depleted", "_hot", "_scrap",
    "waste", "fluid_icon", "fluid_tank", "fluid_barrel", "fluid_pack", "fluid_cell",
)


@dataclass
class DeepHit:
    sources: set[str] = field(default_factory=set)


def deep_scan(paths: list[Path], field_maps: dict[str, dict[str, str]]) -> dict[str, DeepHit]:
    hits: dict[str, DeepHit] = defaultdict(DeepHit)
    for path in iter_java_files(paths):
        text = path.read_text(encoding="utf-8", errors="replace")
        src = source_label(path)
        for pat in DEEP_OUTPUT_PATTERNS:
            for m in pat.finditer(text):
                field = m.group(1)
                for ns, fmap in field_maps.items():
                    reg = fmap.get(field)
                    if reg:
                        hits[f"{ns}:{reg}"].sources.add(src)
    return hits


def manual_reason(registry: str, tab: str, ee_has: bool) -> str:
    low = registry.lower()
    if any(h in low for h in SECRET_HINTS):
        return "likely_secret_or_special"
    if registry in SKIP_REGISTRY_EXACT:
        return "whitelist_exact"
    for suf in SKIP_REGISTRY_SUFFIXES:
        if registry.endswith(suf):
            return f"whitelist_suffix:{suf}"
    if any(h in low for h in LOOT_ONLY_HINTS):
        return "loot_or_processing_intermediate"
    if registry.startswith("gun_") and tab == "weaponTab":
        return "sedna_weapon_check"
    if registry.startswith("ammo_") and "container" not in registry:
        return "ammo_variant_check"
    if not ee_has:
        return "no_ee_reference_craft"
    for hint in REVIEW_HINTS:
        if hint in registry:
            return f"hint:{hint}"
    return "unclassified_edge_case"


def final_verdict(
    scanner_verdict: str,
    deep_has: bool,
    ee_has: bool,
    registry: str,
    tab: str,
) -> tuple[str, str]:
    if deep_has:
        return "HAS_RECIPE", "deep_scan_found_output"

    from audit_recipe_coverage import RegisteredItem

    skip, skip_reason = should_skip(registry, RegisteredItem("hbm", registry, registry, "item"))
    if skip:
        return "CONFIRM_SKIP", skip_reason

    reason = manual_reason(registry, tab, ee_has)

    if scanner_verdict == "PORT" and ee_has:
        return "CONFIRM_PORT", "ee_has_craft_port_missing"

    if scanner_verdict == "SKIP" or not ee_has:
        if reason in ("likely_secret_or_special", "no_ee_reference_craft", "loot_or_processing_intermediate"):
            return "CONFIRM_SKIP", reason
        return "CONFIRM_SKIP", reason

    if scanner_verdict == "REVIEW":
        return "MANUAL", reason

    return "MANUAL", reason


def main() -> int:
    tabs = load_tab_order()
    registered = collect_registered(tabs)
    field_maps = build_field_maps(registered)

    port_paths = PORT_RECIPE_SCAN_DIRS + [ROOT / "src/main/java/com/hbm/crafting/SmeltingRecipes.java"]
    ee_paths = EE_CRAFTING_FILES + [p for g in EE_RECIPE_GLOBS for p in iter_java_files([g], recipe_only=True)]

    port_hits = scan_recipes(port_paths, field_maps)
    deep_hits = deep_scan(port_paths, field_maps)
    ee_hits = scan_recipes(ee_paths, field_maps)

    missing_rows = list(csv.DictReader(REPORT_MISSING.open(encoding="utf-8")))
    out_rows: list[dict] = []

    for row in missing_rows:
        key = row["id"]
        item = registered.get(key)
        if not item:
            continue
        scanner_verdict = row["verdict"]
        deep_has = key in deep_hits
        ee_has = key in ee_hits
        fv, reason = final_verdict(scanner_verdict, deep_has, ee_has, item.registry, item.tab)

        evidence = ""
        if deep_has:
            evidence = ";".join(sorted(deep_hits[key].sources)[:2])
        elif key in port_hits:
            evidence = "scanner_hit"
        elif ee_has:
            evidence = ";".join(sorted(ee_hits[key].sources)[:2])

        out_rows.append({
            "id": key,
            "tab": item.tab,
            "kind": item.kind,
            "field": item.field,
            "scanner_verdict": scanner_verdict,
            "scanner_port_has": row["port_has_recipe"],
            "ee_has_recipe": "yes" if ee_has else "no",
            "deep_has_recipe": "yes" if deep_has else "no",
            "final_verdict": fv,
            "reason": reason,
            "evidence": evidence,
        })

    out_rows.sort(key=lambda r: (r["tab"], r["id"]))

    with OUT_VERIFIED.open("w", encoding="utf-8", newline="") as f:
        w = csv.DictWriter(f, fieldnames=list(out_rows[0].keys()))
        w.writeheader()
        w.writerows(out_rows)

    counts = Counter(r["final_verdict"] for r in out_rows)
    by_tab = defaultdict(lambda: Counter())
    for r in out_rows:
        by_tab[r["tab"] or "(no tab)"][r["final_verdict"]] += 1

    lines = [
        f"Verified missing (scanner said no recipe): {len(out_rows)}",
        "",
        "Final verdicts:",
    ]
    for k, v in counts.most_common():
        lines.append(f"  {k}: {v}")

    lines += ["", "By tab:"]
    for tab in sorted(by_tab.keys(), key=lambda t: (-sum(by_tab[t].values()), t)):
        c = by_tab[tab]
        lines.append(
            f"  {tab}: HAS={c['HAS_RECIPE']} PORT={c['CONFIRM_PORT']} "
            f"SKIP={c['CONFIRM_SKIP']} MANUAL={c['MANUAL']}"
        )

    # Category notes for CONFIRM_PORT
    port_items = [r for r in out_rows if r["final_verdict"] == "CONFIRM_PORT"]
    lines += ["", f"CONFIRM_PORT items ({len(port_items)}) — top EE sources:"]
    src_counter = Counter()
    for r in port_items:
        if r["ee_has_recipe"] == "yes":
            for part in (registered[r["id"]].registry,):
                pass
        row_ee = ee_hits.get(r["id"])
        if row_ee:
            for s in list(row_ee.sources)[:1]:
                src_counter[Path(s).name] += 1
    for name, cnt in src_counter.most_common(15):
        lines.append(f"  {name}: {cnt}")

    OUT_SUMMARY.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print("\n".join(lines))
    print(f"\nWrote {OUT_VERIFIED.name}")
    print(f"Wrote {OUT_SUMMARY.name}")
    return 0


if __name__ == "__main__":
    sys.exit(main())