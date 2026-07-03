#!/usr/bin/env python3
"""Compare EE vs port gun ballistic configs for @weaponTab indices 0-64."""

from __future__ import annotations

import csv
import re
import sys
from collections import Counter, defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
EE_ROOT = ROOT.parent / "мод hbmntm" / "NTM-Extended-GitHub"
CREATIVE_ORDER = ROOT / "src" / "main" / "resources" / "assets" / "hbm" / "creative_tab_order.txt"
EE_MOD_ITEMS = EE_ROOT / "src" / "main" / "java" / "com" / "hbm" / "items" / "ModItems.java"
PORT_MOD_ITEMS = ROOT / "src" / "main" / "java" / "com" / "hbm" / "items" / "ModItems.java"
EE_GUNCFG = EE_ROOT / "src" / "main" / "java" / "com" / "hbm" / "handler" / "guncfg"
PORT_GUNCFG = ROOT / "src" / "main" / "java" / "com" / "hbm" / "handler" / "guncfg"
EE_BULLET_UTIL = EE_ROOT / "src" / "main" / "java" / "com" / "hbm" / "handler" / "BulletConfigSyncingUtil.java"
PORT_BULLET_UTIL = ROOT / "src" / "main" / "java" / "com" / "hbm" / "handler" / "BulletConfigSyncingUtil.java"
OUT_CSV = ROOT / "tools" / "ee_weapon_ballistics_audit.csv"

COMPARE_FIELDS = (
    "config_method",
    "rateOfFire",
    "roundsPerCycle",
    "firingMode",
    "hasSights",
    "ammoCap",
    "reloadType",
    "durability",
    "config",
)

GUN_DECL_RE = re.compile(
    r"public\s+static\s+final\s+Item\s+(\w+)\s*=\s*([^;]+);",
    re.DOTALL,
)
CONFIG_CALL_RE = re.compile(
    r"(?:com\.hbm\.handler\.guncfg\.)?(Gun\w+Factory)\.(get\w+Config\(\))(?:\.silenced\(\))?",
)
METHOD_RE = re.compile(
    r"public\s+static\s+GunConfiguration\s+(get\w+Config)\s*\(\s*\)\s*\{",
)
PARENT_LOCAL_RE = re.compile(r"GunConfiguration\s+config\s*=\s*get(\w+)\s*\(\s*\)")
HELPER_CALL_RE = re.compile(r"(\w+)\(\s*config\s*\)\s*;")
HELPER_METHOD_RE = re.compile(
    r"(?:public|private)\s+static\s+void\s+(\w+)\s*\(\s*GunConfiguration\s+config\s*\)\s*\{",
)
ASSIGN_RE = re.compile(r"config\.(\w+)\s*=\s*([^;]+);")
BULLET_ADD_RE = re.compile(r"config\.config\.add\(\s*([^)]+?)\s*\)")
BULLET_CONST_RE = re.compile(r"public\s+static\s+int\s+(\w+)\s*=\s*i\+\+;")
CONFIG_CLEAR_RE = re.compile(r"config\.config\s*=\s*new\s+ArrayList")

FIRING_MODE = {
    "GunConfiguration.FIRE_MANUAL": "FIRE_MANUAL",
    "GunConfiguration.FIRE_AUTO": "FIRE_AUTO",
    "0": "FIRE_MANUAL",
    "1": "FIRE_AUTO",
}
RELOAD_TYPE = {
    "GunConfiguration.RELOAD_NONE": "RELOAD_NONE",
    "GunConfiguration.RELOAD_FULL": "RELOAD_FULL",
    "GunConfiguration.RELOAD_SINGLE": "RELOAD_SINGLE",
    "0": "RELOAD_NONE",
    "1": "RELOAD_FULL",
    "2": "RELOAD_SINGLE",
}


def read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8", errors="replace")


def parse_weapon_tab_guns(path: Path, max_index: int = 64) -> list[str]:
    guns: list[tuple[int, str]] = []
    in_weapon = False
    for raw in read_text(path).splitlines():
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        if line == "@weaponTab":
            in_weapon = True
            continue
        if in_weapon and line.startswith("@"):
            break
        if not in_weapon or "=" not in line:
            continue
        name, idx_s = line.split("=", 1)
        idx = int(idx_s.strip())
        if idx <= max_index:
            guns.append((idx, name.strip()))
    guns.sort(key=lambda pair: pair[0])
    return [name for _, name in guns]


def extract_method_body(text: str, start: int) -> str:
    depth = 0
    i = start
    while i < len(text):
        ch = text[i]
        if ch == "{":
            depth += 1
        elif ch == "}":
            depth -= 1
            if depth == 0:
                return text[start : i + 1]
        i += 1
    return ""


def parse_bullet_constants(path: Path) -> dict[str, int]:
    order: dict[str, int] = {}
    idx = 0
    for m in BULLET_CONST_RE.finditer(read_text(path)):
        order[m.group(1)] = idx
        idx += 1
    return order


def normalize_value(field: str, raw: str) -> str:
    value = " ".join(raw.strip().split())
    if field == "firingMode":
        return FIRING_MODE.get(value, value)
    if field == "reloadType":
        return RELOAD_TYPE.get(value, value)
    if field == "hasSights":
        if value in {"true", "false"}:
            return value
        return value
    return value


def normalize_bullet_token(token: str) -> str:
    token = token.strip()
    token = token.replace("BulletConfigSyncingUtil.", "")
    return token


def parse_config_method_body(body: str, helpers: dict[str, str] | None = None) -> dict[str, str]:
    values: dict[str, str] = {}
    bullets: list[str] = []
    helpers = helpers or {}

    if CONFIG_CLEAR_RE.search(body):
        bullets = []

    for m in ASSIGN_RE.finditer(body):
        field, raw = m.group(1), m.group(2)
        if field == "config":
            continue
        if field in {"rateOfFire", "roundsPerCycle", "firingMode", "hasSights", "ammoCap", "reloadType", "durability"}:
            values[field] = normalize_value(field, raw)

    for m in BULLET_ADD_RE.finditer(body):
        bullets.append(normalize_bullet_token(m.group(1)))

    for m in HELPER_CALL_RE.finditer(body):
        helper_body = helpers.get(m.group(1), "")
        for hm in BULLET_ADD_RE.finditer(helper_body):
            bullets.append(normalize_bullet_token(hm.group(1)))

    if bullets:
        values["config"] = ",".join(bullets)
    elif "config" not in values:
        values.setdefault("config", "")

    values.setdefault("hasSights", "false")
    values.setdefault("durability", "0")
    return values


def load_factory_configs(guncfg_dir: Path) -> dict[str, dict[str, str]]:
    methods: dict[str, str] = {}
    helpers_by_class: dict[str, dict[str, str]] = {}
    for java_file in sorted(guncfg_dir.glob("Gun*.java")):
        class_name = java_file.stem
        text = read_text(java_file)
        class_helpers: dict[str, str] = {}
        for m in HELPER_METHOD_RE.finditer(text):
            class_helpers[m.group(1)] = extract_method_body(text, m.end() - 1)
        helpers_by_class[class_name] = class_helpers
        for m in METHOD_RE.finditer(text):
            method_name = m.group(1)
            body = extract_method_body(text, m.end() - 1)
            methods[f"{class_name}.{method_name}"] = body

    cache: dict[str, dict[str, str]] = {}

    def resolve(key: str, stack: set[str]) -> dict[str, str]:
        if key in cache:
            return dict(cache[key])
        if key in stack:
            return {}
        stack.add(key)
        class_name = key.split(".", 1)[0]
        body = methods.get(key, "")
        merged: dict[str, str] = {}
        parent = PARENT_LOCAL_RE.search(body)
        if parent:
            parent_key = f"{class_name}.{parent.group(1)}"
            merged = resolve(parent_key, stack)
        merged.update(parse_config_method_body(body, helpers_by_class.get(class_name, {})))
        cache[key] = dict(merged)
        stack.remove(key)
        return dict(merged)

    for key in methods:
        resolve(key, set())
    return cache


def parse_moditems_guns(path: Path) -> dict[str, dict[str, str]]:
    text = read_text(path)
    guns: dict[str, dict[str, str]] = {}
    for m in GUN_DECL_RE.finditer(text):
        field = m.group(1)
        rhs = " ".join(m.group(2).split())
        entry = {
            "field": field,
            "line": f"public static final Item {field} = {rhs};",
            "config_method": "",
            "main_config_key": "",
        }
        calls = CONFIG_CALL_RE.findall(rhs)
        if calls:
            factory, method = calls[0]
            method_name = method.replace("()", "")
            entry["config_method"] = f"{factory}.{method_name}"
            entry["main_config_key"] = f"{factory}.{method_name}"
        guns[field] = entry
    return guns


def config_values(configs: dict[str, dict[str, str]], config_key: str) -> dict[str, str]:
    if not config_key:
        return {}
    base = configs.get(config_key, {})
    out = {k: base.get(k, "") for k in COMPARE_FIELDS if k != "config_method"}
    out.setdefault("hasSights", "false")
    out.setdefault("durability", "0")
    out.setdefault("config", "")
    return out


def compare_field(ee_val: str, port_val: str) -> str:
    if ee_val == port_val:
        return "yes"
    if not ee_val and not port_val:
        return "yes"
    return "no"


def main() -> int:
    guns = parse_weapon_tab_guns(CREATIVE_ORDER, max_index=64)
    if len(guns) != 65:
        print(f"warning: expected 65 weapon-tab items 0-64, got {len(guns)}", file=sys.stderr)

    ee_items = parse_moditems_guns(EE_MOD_ITEMS)
    port_items = parse_moditems_guns(PORT_MOD_ITEMS)
    ee_configs = load_factory_configs(EE_GUNCFG)
    port_configs = load_factory_configs(PORT_GUNCFG)

    rows: list[dict[str, str]] = []
    mismatch_counter: Counter[str] = Counter()

    for gun in guns:
        ee_item = ee_items.get(gun, {})
        port_item = port_items.get(gun, {})
        ee_method = ee_item.get("config_method", "")
        port_method = port_item.get("config_method", "")

        ee_cfg = config_values(ee_configs, ee_item.get("main_config_key", ""))
        port_cfg = config_values(port_configs, port_item.get("main_config_key", ""))

        for field in COMPARE_FIELDS:
            if field == "config_method":
                ee_val = ee_method or "(none)"
                port_val = port_method or "(none)"
            else:
                ee_val = ee_cfg.get(field, "") if ee_method else "(no factory config)"
                port_val = port_cfg.get(field, "") if port_method else "(no factory config)"

            match = compare_field(ee_val, port_val)
            if match == "no":
                mismatch_counter[gun] += 1

            rows.append(
                {
                    "gun": gun,
                    "field": field,
                    "ee_value": ee_val,
                    "port_value": port_val,
                    "match": match,
                }
            )

        # traceability rows (not counted as mismatches)
        for label, ee_line, port_line in (
            ("ee_moditems_line", ee_item.get("line", "(missing)"), ""),
            ("port_moditems_line", "", port_item.get("line", "(missing)")),
        ):
            rows.append(
                {
                    "gun": gun,
                    "field": label,
                    "ee_value": ee_line,
                    "port_value": port_line,
                    "match": "",
                }
            )

    OUT_CSV.parent.mkdir(parents=True, exist_ok=True)
    with OUT_CSV.open("w", encoding="utf-8", newline="") as fh:
        writer = csv.DictWriter(fh, fieldnames=["gun", "field", "ee_value", "port_value", "match"])
        writer.writeheader()
        writer.writerows(rows)

    total_mismatches = sum(1 for r in rows if r["match"] == "no")
    guns_with_mismatches = len(mismatch_counter)
    top_guns = mismatch_counter.most_common(15)

    print(f"Audited {len(guns)} guns (@weaponTab indices 0-64)")
    print(f"CSV: {OUT_CSV}")
    print(f"Field mismatches: {total_mismatches}")
    print(f"Guns with >=1 mismatch: {guns_with_mismatches}")
    print("Top mismatched guns:")
    for gun, count in top_guns:
        print(f"  {gun}: {count}")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())