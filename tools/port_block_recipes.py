#!/usr/bin/env python3
"""Port missing blockTab crafts from EE CraftingManager into EEBlockRecipes.java."""

from __future__ import annotations

import csv
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
EE_CM = ROOT.parent / "мод hbmntm" / "NTM-Extended-GitHub" / "src" / "main" / "java" / "com" / "hbm" / "main" / "CraftingManager.java"
PORT_ACTION = ROOT / "tools" / "recipe_coverage_action_port.csv"
PORT_JAVA = ROOT / "src" / "main" / "java"
OUT_BLOCKS = ROOT / "src" / "main" / "java" / "com" / "hbm" / "crafting" / "EEBlockRecipes.java"

PORT_SCAN_DIRS = [
    PORT_JAVA / "com" / "hbm" / "main" / "CraftingManager.java",
    PORT_JAVA / "com" / "hbm" / "crafting",
    PORT_JAVA / "com" / "hbm" / "inventory",
    PORT_JAVA / "com" / "hbmspace" / "inventory",
]

CM_ITEM = re.compile(
    r"^\s*(?://\s*)?addRecipe(?:Auto|ShapelessAuto)?\(\s*new\s+ItemStack\(\s*ModItems\.(\w+)\b[^;]*;",
    re.MULTILINE,
)
CM_BLOCK = re.compile(
    r"^\s*(?://\s*)?addRecipe(?:Auto|ShapelessAuto)?\([^;]*?ModBlocks\.(\w+)\b[^;]*;",
    re.MULTILINE,
)
EE_CM_BLOCK = re.compile(
    r"^\s*(?://\s*)?addRecipe(?:Auto|ShapelessAuto)?\(\s*(?:new\s+ItemStack\(\s*)?(?:Item\.getItemFromBlock\(\s*)?ModBlocks\.(\w+)\b",
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


def load_block_targets() -> list[str]:
    rows = csv.DictReader(PORT_ACTION.read_text(encoding="utf-8").splitlines())
    return [r["field"] for r in rows if r["tab"] == "blockTab"]


def port_has_block_output(name: str, blob: str) -> bool:
    patterns = [
        rf"new\s+ItemStack\(\s*ModBlocks\.{re.escape(name)}\b",
        rf"new\s+ItemStack\(\s*Item\.getItemFromBlock\(\s*ModBlocks\.{re.escape(name)}\b",
        rf"Item\.getItemFromBlock\(\s*ModBlocks\.{re.escape(name)}\b",
        rf"\.outputItems\(\s*new\s+ItemStack\(\s*ModBlocks\.{re.escape(name)}\b",
        rf"new\s+OutgasserRecipe\(\s*new\s+ItemStack\(\s*ModBlocks\.{re.escape(name)}\b",
        rf"addRecipe\([^,]+,\s*[^,]+,\s*new\s+ItemStack\(\s*ModBlocks\.{re.escape(name)}\b",
        rf"CrystallizerRecipe\(\s*ModBlocks\.{re.escape(name)}\b",
        rf"makeRecipe\([^,]+,\s*[^,]+,\s*ModBlocks\.{re.escape(name)}\b",
    ]
    return any(re.search(p, blob) for p in patterns)


def extract_ee_recipes(names: set[str]) -> list[str]:
    ee = EE_CM.read_text(encoding="utf-8")
    lines: list[str] = []
    seen: set[str] = set()
    for m in EE_CM_BLOCK.finditer(ee):
        block = m.group(1)
        if block not in names:
            continue
        start = m.start()
        line_end = ee.find(";", m.end())
        call = ee[start:line_end + 1].strip()
        call = re.sub(r"^\s*//\s*", "", call)
        if call.startswith("//") or call in seen:
            continue
        seen.add(call)
        lines.append(f"        CraftingManager.{call}")
    return lines


def write_block_java(lines: list[str]) -> None:
    header = """package com.hbm.crafting;

import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.OreDictManager.DictFrame;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.material.Mats;
import com.hbm.items.ItemEnums.EnumCircuitType;
import com.hbm.items.ModItems;
import com.hbm.main.CraftingManager;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import static com.hbm.inventory.OreDictManager.*;

/** EE block-tab bench crafts missing from the port. */
public class EEBlockRecipes {

    public static void register() {
"""
    footer = """
    }
}
"""
    OUT_BLOCKS.write_text(header + "\n".join(lines) + "\n" + footer, encoding="utf-8", newline="\n")


def main() -> int:
    targets = load_block_targets()
    blob = load_port_sources()
    missing = [n for n in targets if not port_has_block_output(n, blob)]
    print(f"blockTab PORT listed: {len(targets)}")
    print(f"Actually missing in port: {len(missing)}")
    if missing:
        print("Missing:", ", ".join(missing))

    lines = extract_ee_recipes(set(missing))
    print(f"CraftingManager recipes to add: {len(lines)}")
    if lines:
        print("Extracted lines (manual EEBlockRecipes.java is source of truth; script does not overwrite by default)")
    else:
        print("Nothing to extract")

    ported = set()
    for line in lines:
        m = re.search(r"ModBlocks\.(\w+)", line)
        if m:
            ported.add(m.group(1))
    still = [n for n in missing if n not in ported]
    if still:
        print(f"Still unported ({len(still)}): {', '.join(still)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())