#!/usr/bin/env python3
"""Port missing partsTab crafts from EE reference into port Java recipe files."""

from __future__ import annotations

import csv
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
EE_CM = ROOT.parent / "мод hbmntm" / "NTM-Extended-GitHub" / "src" / "main" / "java" / "com" / "hbm" / "main" / "CraftingManager.java"
EE_OUT = ROOT.parent / "мод hbmntm" / "NTM-Extended-GitHub" / "src" / "main" / "java" / "com" / "hbm" / "inventory" / "RBMKOutgasserRecipes.java"
PORT_ACTION = ROOT / "tools" / "recipe_coverage_action_port.csv"
PORT_JAVA = ROOT / "src" / "main" / "java"
OUT_PARTS = ROOT / "src" / "main" / "java" / "com" / "hbm" / "crafting" / "EEPartsRecipes.java"
OUT_OUTGASSER = ROOT / "src" / "main" / "java" / "com" / "hbm" / "inventory" / "recipes" / "OutgasserRecipes.java"

PORT_SCAN_DIRS = [
    PORT_JAVA / "com" / "hbm" / "main" / "CraftingManager.java",
    PORT_JAVA / "com" / "hbm" / "crafting",
    PORT_JAVA / "com" / "hbm" / "inventory",
    PORT_JAVA / "com" / "hbmspace" / "inventory",
]

OUTPUT_MARKERS = [
    "addRecipeAuto(new ItemStack(ModItems.{name}",
    "addShapelessAuto(new ItemStack(ModItems.{name}",
    "addRecipeAuto(DictFrame.fromOne(ModItems.{name}",
    ".outputItems(new ItemStack(ModItems.{name}",
    "CrystallizerRecipe(ModItems.{name}",
    "new OutgasserRecipe(new ItemStack(ModItems.{name}",
    "ShredderRecipes.setRecipe(",
    "new AnvilSmithing",
    "new PedestalRecipe(new ItemStack(ModItems.{name}",
]

CM_CALL = re.compile(
    r"^\s*(?://\s*)?addRecipe(?:Auto|ShapelessAuto)?\(\s*new\s+ItemStack\(\s*ModItems\.(\w+)\b[^;]*;",
    re.MULTILINE,
)
OUTGASSER_CALL = re.compile(
    r"^\s*addRecipe\([^,]+,\s*([^,]+),\s*new\s+ItemStack\(\s*ModItems\.(\w+)(?:,\s*(\d+))?\s*\)\s*\)\s*;",
    re.MULTILINE,
)


def load_port_sources() -> str:
    chunks: list[str] = []
    for base in PORT_SCAN_DIRS:
        if base.is_file():
            chunks.append(base.read_text(encoding="utf-8", errors="replace"))
        elif base.exists():
            for path in base.rglob("*.java"):
                chunks.append(path.read_text(encoding="utf-8", errors="replace"))
    return "\n".join(chunks)


def load_parts_targets() -> list[str]:
    rows = csv.DictReader(PORT_ACTION.read_text(encoding="utf-8").splitlines())
    return [r["field"] for r in rows if r["tab"] == "partsTab"]


def port_has_output(name: str, blob: str) -> bool:
    checks = [m.format(name=name) for m in OUTPUT_MARKERS if "{name}" in m]
    checks.extend([
        f"ShredderRecipes.setRecipe(",
        f"setRecipe(new ComparableStack(ModItems.{name})",
        f"setRecipe(ModItems.{name}",
        f"recipes.put(new ComparableStack(ModItems.{name})",
        f"recipes.put(new RecipesCommon.ComparableStack(ModItems.{name})",
        f"recipes.put(new RecipesCommon.OreDictStack(",
    ])
    for needle in checks:
        if needle not in blob:
            continue
        if needle.startswith("ShredderRecipes.setRecipe("):
            # second arg must be output stack for this item
            if re.search(
                rf"ShredderRecipes\.setRecipe\([^,]+,\s*new\s+ItemStack\(\s*ModItems\.{re.escape(name)}\b",
                blob,
            ):
                return True
            continue
        if "recipes.put(new RecipesCommon.OreDictStack(" in needle:
            if re.search(
                rf"new\s+OutgasserRecipe\(\s*new\s+ItemStack\(\s*ModItems\.{re.escape(name)}\b",
                blob,
            ):
                return True
            continue
        return True
    return False


def extract_cm_recipes(names: set[str]) -> list[str]:
    ee = EE_CM.read_text(encoding="utf-8")
    lines: list[str] = []
    seen: set[str] = set()
    for m in CM_CALL.finditer(ee):
        name = m.group(1)
        if name not in names:
            continue
        call = m.group(0).strip()
        if call.startswith("//") or call in seen:
            continue
        seen.add(call)
        lines.append(f"        CraftingManager.{call}")
    return lines


def map_outgasser_input(inp: str) -> str:
    inp = inp.strip()
    if inp.endswith("()"):
        return f"new RecipesCommon.OreDictStack({inp})"
    if inp.startswith("ModItems.") or inp.startswith("ModBlocks."):
        return f"new ComparableStack({inp})"
    if inp.startswith("Items.") or inp.startswith("Blocks."):
        return f"new ComparableStack({inp})"
    if inp.startswith('"'):
        return f'new RecipesCommon.OreDictStack({inp})'
    return f"new ComparableStack({inp})"


def extract_outgasser_recipes(names: set[str]) -> list[str]:
    if not EE_OUT.exists():
        return []
    ee = EE_OUT.read_text(encoding="utf-8")
    lines: list[str] = []
    seen: set[str] = set()
    for m in OUTGASSER_CALL.finditer(ee):
        out_name = m.group(2)
        if out_name not in names:
            continue
        inp = m.group(1)
        count = m.group(3) or "1"
        line = (
            f"\t\trecipes.put({map_outgasser_input(inp)}, "
            f"new OutgasserRecipe(new ItemStack(ModItems.{out_name}, {count}), null));"
        )
        if line in seen:
            continue
        seen.add(line)
        lines.append(line)
    return lines


def write_parts_java(lines: list[str]) -> None:
    header = """package com.hbm.crafting;

import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.OreDictManager;
import com.hbm.inventory.OreDictManager.DictFrame;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.material.Mats;
import com.hbm.items.ItemEnums.EnumCircuitType;
import com.hbm.items.ModItems;
import com.hbm.main.CraftingManager;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import static com.hbm.inventory.OreDictManager.*;

/** EE parts-tab bench crafts missing from the port. */
public class EEPartsRecipes {

    public static void register() {
"""
    footer = """
    }
}
"""
    OUT_PARTS.write_text(header + "\n".join(lines) + "\n" + footer, encoding="utf-8", newline="\n")


def patch_outgasser(lines: list[str]) -> None:
    if not lines:
        return
    text = OUT_OUTGASSER.read_text(encoding="utf-8")
    marker = "\t\trecipes.put(new ComparableStack(DictFrame.fromOne(ModItems.oil_tar, ItemEnums.EnumTarType.WAX)),\tnew OutgasserRecipe(null, new FluidStack(Fluids.RADIOSOLVENT, 100)));"
    block = "\n\t\t/* EE outgasser parity */\n" + "\n".join(lines) + "\n"
    if marker not in text:
        raise RuntimeError("OutgasserRecipes marker not found")
    if "/* EE outgasser parity */" in text:
        print("Outgasser EE block already present, skipping patch")
        return
    text = text.replace(marker, marker + block)
    OUT_OUTGASSER.write_text(text, encoding="utf-8", newline="\n")


def main() -> int:
    targets = load_parts_targets()
    blob = load_port_sources()
    missing = [n for n in targets if not port_has_output(n, blob)]
    print(f"partsTab PORT listed: {len(targets)}")
    print(f"Actually missing in port: {len(missing)}")

    cm_lines = extract_cm_recipes(set(missing))
    og_lines = extract_outgasser_recipes(set(missing))
    print(f"CraftingManager recipes to add: {len(cm_lines)}")
    print(f"Outgasser recipes to add: {len(og_lines)}")

    skip_cm = {"factory_core_titanium", "factory_core_advanced"}
    cm_lines = [l for l in cm_lines if not any(f"ModItems.{name}" in l for name in skip_cm)]
    if cm_lines:
        write_parts_java(cm_lines)
        print(f"Wrote {OUT_PARTS.relative_to(ROOT)}")
    elif missing:
        print("No safe CraftingManager lines to auto-write (use --apply to force overwrite)")
    if og_lines:
        patch_outgasser(og_lines)
        print(f"Patched {OUT_OUTGASSER.relative_to(ROOT)}")

    ported = set()
    for line in cm_lines + og_lines:
        m = re.search(r"ModItems\.(\w+)", line)
        if m:
            ported.add(m.group(1))
    still = [n for n in missing if n not in ported]
    if still:
        print(f"Still need manual/other machine port ({len(still)}):")
        for n in still:
            print(f"  {n}")
    return 0


if __name__ == "__main__":
    sys.exit(main())