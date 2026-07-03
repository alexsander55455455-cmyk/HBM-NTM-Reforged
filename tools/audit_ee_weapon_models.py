#!/usr/bin/env python3
"""Audit EE-block firearm inventory/FP renderer parity between port and EE reference."""

from __future__ import annotations

import argparse
import csv
import json
import re
import sys
from dataclasses import dataclass
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
WORKSPACE = ROOT.parent
EE_ROOT = WORKSPACE / "мод hbmntm" / "NTM-Extended-GitHub"
EE_EXTRACTED_ASSETS = WORKSPACE / "мод hbmntm" / "extracted" / "assets" / "hbm"
PORT_ASSETS = ROOT / "src" / "main" / "resources" / "assets" / "hbm"
CREATIVE_ORDER = PORT_ASSETS / "creative_tab_order.txt"
EE_MOD_ITEMS = EE_ROOT / "src" / "main" / "java" / "com" / "hbm" / "items" / "ModItems.java"
PORT_MOD_ITEMS = ROOT / "src" / "main" / "java" / "com" / "hbm" / "items" / "ModItems.java"
EE_CLIENT_PROXY = EE_ROOT / "src" / "main" / "java" / "com" / "hbm" / "main" / "ClientProxy.java"
EE_MODEL_EVENTS = EE_ROOT / "src" / "main" / "java" / "com" / "hbm" / "main" / "ModEventHandlerClient.java"
PORT_CLIENT_PROXY = ROOT / "src" / "main" / "java" / "com" / "hbm" / "main" / "ClientProxy.java"
PORT_JAVA = ROOT / "src" / "main" / "java"
GENERATED_REGISTRAR = (
    ROOT / "build" / "generated" / "sources" / "annotationProcessor" / "java" / "main"
    / "com" / "hbm" / "generated" / "GeneratedHBMRegistrar.java"
)
REPORT = ROOT / "tools" / "ee_weapon_model_audit.csv"

# EE weaponTab indices 0-64 (CreativeTabSortVerifier.WEAPON_TAB_EE_FIREARMS)
EE_FIREARMS = (
    "ullapool_caber",
    "gun_b92",
    "gun_b93",
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
    "gun_deagle",
    "gun_flechette",
    "gun_ar15",
    "gun_supershotgun",
    "gun_jshotgun",
    "gun_ks23",
    "gun_sauer",
    "gun_calamity",
    "gun_calamity_dual",
    "gun_avenger",
    "gun_lacunae",
    "gun_bolt_action",
    "gun_bolt_action_green",
    "gun_uzi_silencer",
    "gun_uzi_saturnite",
    "gun_uzi_saturnite_silencer",
    "gun_thompson",
    "gun_rpg",
    "gun_karl",
    "gun_lever_action",
    "gun_lever_action_dark",
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
    "gun_cryolator",
    "gun_jack",
    "gun_immolator",
    "gun_osipr",
    "gun_emp",
    "gun_moist_nugget",
    "gun_super_shotgun",
    "gun_revolver_inverted",
    "gun_lever_action_sonata",
    "gun_bolt_action_saturnite",
    "gun_dampfmaschine",
    "gun_darter",
)

ITEM_RENDER_CLASS = re.compile(r"(?:ItemRender\w+|RenderGun\w+)")
MODITEM_FIELD = re.compile(r"ModItems\.(\w+)")
AUTO_REGISTER_ITEM = re.compile(r'@AutoRegister\(\s*item\s*=\s*"([^"]+)"')
REGISTRY_STRING = re.compile(r"^[a-z0-9_]+$")

ITEM_DECL = re.compile(
    r"public\s+static\s+final\s+Item\s+(\w+)\s*=\s*new\s+[^;]+?;",
    re.DOTALL,
)
STRING_LITERAL = re.compile(r'"([a-z0-9_]+)"')


@dataclass
class GunAuditRow:
    gun: str
    ee_inv_type: str
    port_inv_type: str
    ee_has_fp_renderer: bool
    port_has_fp_renderer: bool
    match: bool
    action_needed: str


def read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8-sig")


def extract_registry_from_decl(body: str, field: str) -> str:
    reg_m = re.search(r'\.setRegistryName\(\s*"([^"]+)"\s*\)', body)
    if reg_m and REGISTRY_STRING.match(reg_m.group(1)):
        return reg_m.group(1)
    trans_m = re.search(r'\.setTranslationKey\(\s*"([^"]+)"\s*\)', body)
    if trans_m and REGISTRY_STRING.match(trans_m.group(1)):
        return trans_m.group(1)
    ctor_m = re.search(r"=\s*new\s+[\w.]+(?:<[^>]+>)?\s*\(", body)
    if ctor_m:
        start = ctor_m.end()
        depth = 1
        i = start
        while i < len(body) and depth > 0:
            ch = body[i]
            if ch == "(":
                depth += 1
            elif ch == ")":
                depth -= 1
            i += 1
        args = body[start : i - 1]
        candidates = [s for s in STRING_LITERAL.findall(args) if REGISTRY_STRING.match(s)]
        if candidates:
            if field in candidates:
                return field
            return candidates[-1]
    return field


def parse_moditems_field_registry(text: str) -> dict[str, str]:
    """Map ModItems Java field name -> registry path."""
    field_to_registry: dict[str, str] = {}
    for match in ITEM_DECL.finditer(text):
        field = match.group(1)
        body = match.group(0)
        field_to_registry[field] = extract_registry_from_decl(body, field)
    return field_to_registry


def registry_to_field(field_to_registry: dict[str, str], registry: str) -> str | None:
    for field, reg in field_to_registry.items():
        if reg == registry:
            return field
    return registry if registry in field_to_registry else None


def resolve_registry_from_auto_item(
    item_key: str,
    field_to_registry: dict[str, str],
) -> str:
    if item_key in field_to_registry:
        return field_to_registry[item_key]
    return item_key


def parse_teisr_fields(java_text: str) -> set[str]:
    fields: set[str] = set()
    for line in java_text.splitlines():
        if "setTileEntityItemStackRenderer" not in line:
            continue
        match = MODITEM_FIELD.search(line)
        if match:
            fields.add(match.group(1))
    return fields


def parse_bind_teisr_fields(java_text: str) -> set[str]:
    fields: set[str] = set()
    for match in re.finditer(r"bindTeisr\(ModItems\.(\w+)", java_text):
        fields.add(match.group(1))
    return fields


def parse_register_item_renderer_fields(java_text: str) -> set[str]:
    fields: set[str] = set()
    for match in re.finditer(r"registerItemRenderer\(ModItems\.(\w+)", java_text):
        fields.add(match.group(1))
    return fields


def parse_swap_model_fields(java_text: str) -> set[str]:
    fields: set[str] = set()
    for match in re.finditer(r"swapModels(?:NoGui)?\(ModItems\.(\w+)", java_text):
        fields.add(match.group(1))
    return fields


def parse_auto_register_items(java_root: Path, field_to_registry: dict[str, str]) -> set[str]:
    registries: set[str] = set()
    for path in java_root.rglob("*.java"):
        try:
            text = path.read_text(encoding="utf-8")
        except OSError:
            continue
        if "@AutoRegister" not in text:
            continue
        for item_key in AUTO_REGISTER_ITEM.findall(text):
            registries.add(resolve_registry_from_auto_item(item_key, field_to_registry))
    return registries


def parse_fp_renderer_fields(java_text: str, *, port: bool = False) -> set[str]:
    """Fields with custom item/FP renderer registration."""
    fields: set[str] = set()
    pattern = re.compile(
        r"ModItems\.(\w+)\.setTileEntityItemStackRenderer\(\s*(?:new\s+)?"
        r"(ItemRender\w+|RenderGun\w+)",
    )
    for field, _ in pattern.findall(java_text):
        fields.add(field)
    bind_pattern = re.compile(
        r"bindTeisr\(ModItems\.(\w+)(?:,|\s)",
    )
    for field in bind_pattern.findall(java_text):
        fields.add(field)
    if port:
        for field in re.findall(r"registerItemRenderer\(ModItems\.(\w+)", java_text):
            fields.add(field)
    else:
        reg_pattern = re.compile(
            r"registerItemRenderer\(ModItems\.(\w+),\s*(?:new\s+)?"
            r"(?:com\.hbm\.render\.item\.weapon\.)?(ItemRender\w+|RenderGun\w+)",
        )
        for field, _ in reg_pattern.findall(java_text):
            fields.add(field)
    return fields


def find_item_model_json(registry: str, *asset_roots: Path) -> Path | None:
    rel = Path("models") / "item" / f"{registry}.json"
    for root in asset_roots:
        candidate = root / rel
        if candidate.is_file():
            return candidate
    return None


def is_flat_layer0_model(path: Path | None) -> bool | None:
    if path is None:
        return None
    try:
        data = json.loads(path.read_text(encoding="utf-8-sig"))
    except (OSError, json.JSONDecodeError):
        return None
    if data.get("elements"):
        return False
    if data.get("display") or data.get("overrides"):
        return False
    textures = data.get("textures") or {}
    layer_keys = [k for k in textures if k.startswith("layer")]
    if len(layer_keys) == 1 and layer_keys[0] == "layer0":
        parent = data.get("parent", "")
        if parent in ("item/generated", "minecraft:item/generated", "item/handheld", "minecraft:item/handheld"):
            return True
    if len(layer_keys) == 0 and "layer0" in textures:
        return True
    return False


def classify_inventory(
    registry: str,
    field: str | None,
    teisr_fields: set[str],
    swap_fields: set[str],
    baked_model_registries: set[str],
    asset_roots: tuple[Path, ...],
    field_to_registry: dict[str, str],
) -> str:
    uses_3d = False
    if field and field in teisr_fields:
        uses_3d = True
    if field and field in swap_fields:
        uses_3d = True
    if registry in baked_model_registries:
        uses_3d = True
    if uses_3d:
        return "3D"
    flat = is_flat_layer0_model(find_item_model_json(registry, *asset_roots))
    if flat is True:
        return "2D"
    if flat is False:
        return "3D"
    return "unknown"


def fields_for_registry(registry: str, field_to_registry: dict[str, str]) -> set[str]:
    out = set()
    for field, reg in field_to_registry.items():
        if reg == registry:
            out.add(field)
    if registry in field_to_registry:
        out.add(registry)
    return out


def registry_has_fp_renderer(
    registry: str,
    fp_fields: set[str],
    field_to_registry: dict[str, str],
) -> bool:
    return bool(fields_for_registry(registry, field_to_registry) & fp_fields)


def parse_weapon_tab_indices(path: Path, max_index: int) -> list[str]:
    guns: list[str] = []
    in_weapon = False
    for raw in read_text(path).splitlines():
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        if line.startswith("@"):
            in_weapon = line == "@weaponTab"
            continue
        if not in_weapon:
            continue
        if "=" not in line:
            continue
        name, idx_s = line.split("=", 1)
        try:
            idx = int(idx_s)
        except ValueError:
            continue
        if idx <= max_index:
            guns.append(name)
    return guns


def build_action_needed(
    gun: str,
    port_missing: bool,
    ee_inv: str,
    port_inv: str,
    ee_fp: bool,
    port_fp: bool,
) -> str:
    actions: list[str] = []
    if port_missing:
        actions.append("missing item in port")
        return "; ".join(actions)
    if ee_inv != port_inv:
        actions.append(f"inventory mismatch (EE={ee_inv} port={port_inv})")
    if ee_fp != port_fp:
        if ee_fp and not port_fp:
            actions.append("port missing FP ItemRender* registration")
        elif not ee_fp and port_fp:
            actions.append("port has extra FP renderer vs EE")
        else:
            actions.append("FP renderer mismatch")
    if not actions:
        return "none"
    return "; ".join(actions)


def audit_guns(guns: tuple[str, ...] | list[str]) -> list[GunAuditRow]:
    ee_items_text = read_text(EE_MOD_ITEMS)
    port_items_text = read_text(PORT_MOD_ITEMS)
    ee_field_to_reg = parse_moditems_field_registry(ee_items_text)
    port_field_to_reg = parse_moditems_field_registry(port_items_text)

    ee_teisr = parse_teisr_fields(read_text(EE_CLIENT_PROXY))
    ee_swap = parse_swap_model_fields(read_text(EE_MODEL_EVENTS))
    ee_baked = {"gun_b92"} if "B92BakedModel" in read_text(EE_MODEL_EVENTS) else set()
    # EE gun TEISR registrations always use ItemRender* / RenderGun* classes.
    ee_fp_fields = parse_teisr_fields(read_text(EE_CLIENT_PROXY))

    port_sources: list[str] = [read_text(PORT_CLIENT_PROXY)]
    if GENERATED_REGISTRAR.is_file():
        port_sources.append(read_text(GENERATED_REGISTRAR))
    port_java_blob = "\n".join(port_sources)

    port_teisr = parse_teisr_fields(port_java_blob)
    port_teisr |= parse_bind_teisr_fields(port_java_blob)
    port_teisr |= parse_register_item_renderer_fields(port_java_blob)
    port_teisr |= parse_auto_register_items(PORT_JAVA, port_field_to_reg)

    port_fp_fields = parse_fp_renderer_fields(port_java_blob, port=True)

    port_baked = set()
    ntm_registry = ROOT / "src" / "main" / "java" / "com" / "hbm" / "main" / "client" / "NTMClientRegistry.java"
    if ntm_registry.is_file() and "B92BakedModel" in read_text(ntm_registry):
        port_baked.add("gun_b92")

    ee_assets = (EE_ROOT / "src" / "main" / "resources" / "assets" / "hbm", EE_EXTRACTED_ASSETS)
    port_assets = (PORT_ASSETS,)

    port_registries = set(port_field_to_reg.values())

    rows: list[GunAuditRow] = []
    for gun in guns:
        ee_field = registry_to_field(ee_field_to_reg, gun)
        port_missing = gun not in port_registries
        ee_inv = classify_inventory(
            gun,
            ee_field,
            ee_teisr,
            ee_swap,
            ee_baked,
            ee_assets,
            ee_field_to_reg,
        )
        port_inv = "missing" if port_missing else classify_inventory(
            gun,
            ee_field,
            port_teisr,
            set(),
            port_baked,
            port_assets,
            port_field_to_reg,
        )
        ee_fp = registry_has_fp_renderer(gun, ee_fp_fields, ee_field_to_reg)
        port_fp = False if port_missing else registry_has_fp_renderer(
            gun, port_fp_fields, port_field_to_reg
        )
        action = build_action_needed(gun, port_missing, ee_inv, port_inv, ee_fp, port_fp)
        match = (
            not port_missing
            and ee_inv == port_inv
            and ee_fp == port_fp
        )
        rows.append(
            GunAuditRow(
                gun=gun,
                ee_inv_type=ee_inv,
                port_inv_type=port_inv,
                ee_has_fp_renderer=ee_fp,
                port_has_fp_renderer=port_fp,
                match=match,
                action_needed=action,
            )
        )
    return rows


def write_csv(rows: list[GunAuditRow], path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="") as fh:
        writer = csv.writer(fh)
        writer.writerow(
            [
                "gun",
                "ee_inv_type",
                "port_inv_type",
                "ee_has_fp_renderer",
                "port_has_fp_renderer",
                "match",
                "action_needed",
            ]
        )
        for row in rows:
            writer.writerow(
                [
                    row.gun,
                    row.ee_inv_type,
                    row.port_inv_type,
                    str(row.ee_has_fp_renderer).lower(),
                    str(row.port_has_fp_renderer).lower(),
                    str(row.match).lower(),
                    row.action_needed,
                ]
            )


def summarize(rows: list[GunAuditRow]) -> dict[str, object]:
    mismatches = [r for r in rows if not r.match]
    missing = [r.gun for r in rows if r.port_inv_type == "missing"]
    action_rank = sorted(
        (r for r in rows if r.action_needed != "none"),
        key=lambda r: (r.port_inv_type == "missing", r.action_needed),
    )
    return {
        "total": len(rows),
        "mismatch_count": len(mismatches),
        "missing_in_port": missing,
        "top_actions": action_rank[:10],
    }


def print_summary(summary: dict[str, object]) -> None:
    print(f"audited={summary['total']} mismatches={summary['mismatch_count']}")
    missing = summary["missing_in_port"]
    if missing:
        print(f"missing_in_port ({len(missing)}): {', '.join(missing)}")
    else:
        print("missing_in_port: none")
    print("top_action_needed:")
    for row in summary["top_actions"]:
        assert isinstance(row, GunAuditRow)
        print(f"  {row.gun}: {row.action_needed}")


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--guns",
        choices=("verifier", "creative_tab"),
        default="verifier",
        help="Gun list source (default: CreativeTabSortVerifier EE block)",
    )
    parser.add_argument(
        "--max-index",
        type=int,
        default=64,
        help="Max weaponTab index when --guns=creative_tab (default: 64)",
    )
    parser.add_argument(
        "--report",
        type=Path,
        default=REPORT,
        help="Output CSV path",
    )
    args = parser.parse_args(argv)

    if args.guns == "creative_tab":
        guns = parse_weapon_tab_indices(CREATIVE_ORDER, args.max_index)
        if len(guns) != len(EE_FIREARMS):
            print(
                f"warning: creative_tab returned {len(guns)} guns, expected {len(EE_FIREARMS)}",
                file=sys.stderr,
            )
    else:
        guns = list(EE_FIREARMS)

    rows = audit_guns(guns)
    write_csv(rows, args.report)
    summary = summarize(rows)
    print_summary(summary)
    print(f"report={args.report}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())