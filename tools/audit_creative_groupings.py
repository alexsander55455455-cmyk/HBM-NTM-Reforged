"""Creative tab grouping audit + JAR parity (gradle project tools/)."""
from __future__ import annotations

import os
import re
import subprocess
import sys
import zipfile
from pathlib import Path

PROJ = Path(__file__).resolve().parents[1]
TOOLS = Path(__file__).resolve().parent
SCRATCH = Path(os.environ.get("GOAL_SCRATCH", r"C:\Temp\grok-goal-f7963e6bf0c1\implementer"))
ORDER_SRC = PROJ / "src/main/resources/assets/hbm/creative_tab_order.txt"
sys.path.insert(0, str(TOOLS))
from cluster_creative_tab_order import (  # noqa: E402
    FIREARM_ORDER,
    GOAL_CLUSTER_VERSION,
    INF_WATER_ORDER,
    MELEE_CLUSTER_ORDER,
    is_gun_ammo_path,
    registry_path,
)


def release_jar_path() -> Path | None:
    libs = PROJ / "build/libs"
    if not libs.is_dir():
        return None
    candidates = [
        p
        for p in libs.glob("HBM-NTM-Reforged-*-alpha.jar")
        if all(
            tag not in p.name
            for tag in ("-dev", "-api", "-sources", "-downgraded", "-shadow")
        )
    ]
    if not candidates:
        return None
    return max(candidates, key=lambda p: p.stat().st_mtime)


def load_tabs(path: Path) -> dict[str, list[str]]:
    tabs: dict[str, list[str]] = {}
    tab = None
    for raw in path.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        if line.startswith("@"):
            tab = line[1:]
            tabs[tab] = []
            continue
        if tab and "=" in line:
            tabs[tab].append(line.split("=", 1)[0])
    return tabs


def contiguity(keys: list[str], prefix: str) -> tuple[int, int, int, bool]:
    hits = [(i, k) for i, k in enumerate(keys) if registry_path(k).startswith(prefix)]
    if not hits:
        return 0, -1, -1, True
    first, last = hits[0][0], hits[-1][0]
    return len(hits), first, last, last - first + 1 == len(hits)


def audit_gun_ammo_block(lines: list[str], keys: list[str]) -> bool:
    paths = [registry_path(k) for k in keys]
    ammo_hits = [i for i, p in enumerate(paths) if is_gun_ammo_path(p)]
    if not ammo_hits:
        lines.append("[ORDER] gun_ammo_block missing")
        return False
    ok = ammo_hits[-1] - ammo_hits[0] + 1 == len(ammo_hits)
    schrab = paths.index("gun_revolver_schrabidium_ammo") if "gun_revolver_schrabidium_ammo" in paths else -1
    ok = ok and schrab >= ammo_hits[0] and schrab <= ammo_hits[-1]
    lines.append(
        f"[ORDER] gun_ammo_block count={len(ammo_hits)} first={ammo_hits[0]} "
        f"last={ammo_hits[-1]} contiguous={ok}"
    )
    return ok


def audit_melee_order(lines: list[str], keys: list[str]) -> bool:
    probes = {
        "hs_sword": keys.index("hs_sword") if "hs_sword" in keys else -1,
        "shimmer_axe": keys.index("shimmer_axe") if "shimmer_axe" in keys else -1,
        "mese_pickaxe": keys.index("mese_pickaxe") if "mese_pickaxe" in keys else -1,
        "mese_gavel": keys.index("mese_gavel") if "mese_gavel" in keys else -1,
        "meteorite_sword": keys.index("meteorite_sword") if "meteorite_sword" in keys else -1,
        "meteorite_sword_seared": keys.index("meteorite_sword_seared") if "meteorite_sword_seared" in keys else -1,
    }
    ok = all(v >= 0 for v in probes.values())
    if ok:
        ok = probes["shimmer_axe"] + 1 == probes["mese_pickaxe"]
        expected = probes["mese_pickaxe"]
        for name in MELEE_CLUSTER_ORDER:
            if keys.index(name) != expected:
                ok = False
                break
            expected += 1
        ok = ok and probes["meteorite_sword"] == probes["mese_gavel"] + 1
        ok = ok and probes["meteorite_sword_seared"] > probes["meteorite_sword"]
    lines.append(f"[ORDER] melee_position_probes={probes} ok={ok}")
    return ok


def audit_unified_firearms(lines: list[str], keys: list[str]) -> bool:
    paths = [registry_path(k) for k in keys]
    fire_sub = [p for p in paths if p in FIREARM_ORDER]
    ok = fire_sub == [n for n in FIREARM_ORDER if n in paths]
    ammo_hits = [i for i, p in enumerate(paths) if is_gun_ammo_path(p)]
    if fire_sub and ammo_hits:
        ok = ok and min(ammo_hits) > max(paths.index(p) for p in fire_sub)
    lines.append(f"[ORDER] unified_firearms count={len(fire_sub)} ok={ok}")
    return ok


def audit_control_inf_water(lines: list[str], keys: list[str]) -> bool:
    inf_hits = [i for i, k in enumerate(keys) if registry_path(k).startswith("inf_water")]
    if not inf_hits:
        lines.append("[ORDER] control_inf_water missing")
        return False
    ok = inf_hits[-1] - inf_hits[0] + 1 == len(inf_hits)
    inf_paths = [registry_path(keys[i]) for i in inf_hits]
    ok = ok and inf_paths == [n for n in INF_WATER_ORDER if n in inf_paths]
    lines.append(f"[ORDER] control_inf_water paths={inf_paths} ok={ok}")
    return ok


def audit_assembly_template_hidden(lines: list[str], tabs: dict[str, list[str]]) -> bool:
    hits = [(tab, keys.index("assembly_template")) for tab, keys in tabs.items() if "assembly_template" in keys]
    ok = not hits
    lines.append(f"[ORDER] assembly_template_in_tabs={hits} hidden={ok}")
    return ok


def main() -> int:
    SCRATCH.mkdir(parents=True, exist_ok=True)
    lines: list[str] = [f"audit_creative_groupings.py version={GOAL_CLUSTER_VERSION}\n"]
    header_ok = ORDER_SRC.read_text(encoding="utf-8").splitlines()[0].startswith("# creative-tab-grouping-5")
    lines.append(f"[ORDER] header_grouping_5={header_ok}")
    tabs = load_tabs(ORDER_SRC)
    weapon_keys = tabs.get("weaponTab", [])
    all_ok = header_ok
    all_ok = audit_gun_ammo_block(lines, weapon_keys) and all_ok
    all_ok = audit_melee_order(lines, weapon_keys) and all_ok
    all_ok = audit_unified_firearms(lines, weapon_keys) and all_ok
    all_ok = audit_control_inf_water(lines, tabs.get("controlTab", [])) and all_ok
    all_ok = audit_assembly_template_hidden(lines, tabs) and all_ok

    for tab, prefix in [("partsTab", "ingot_"), ("partsTab", "nugget_"), ("partsTab", "powder_")]:
        count, first, last, ok = contiguity(tabs.get(tab, []), prefix)
        lines.append(f"[ORDER] {tab} {prefix} count={count} contiguous={ok}")
        all_ok = all_ok and ok

    jar = release_jar_path()
    if jar and jar.exists():
        match = zipfile.ZipFile(jar).read("assets/hbm/creative_tab_order.txt") == ORDER_SRC.read_bytes()
        lines.append(f"[JAR] path={jar.name} mtime={jar.stat().st_mtime} order_bytes_match_source={match}")
        all_ok = all_ok and match
    else:
        lines.append("[JAR] missing release artifact under build/libs")
        all_ok = False

    env = os.environ.copy()
    env["GOAL_SCRATCH"] = str(SCRATCH)
    env.setdefault("JAVA_TOOL_OPTIONS", "-DDISABLE_BUILDSCRIPT_UPDATE_CHECK=true")
    gradle = subprocess.run(
        [str(PROJ / "gradlew.bat"), "verifyCreativeTabSort", "--no-daemon"],
        cwd=PROJ,
        capture_output=True,
        text=True,
        env=env,
        check=False,
    )
    combined = gradle.stdout + "\n" + gradle.stderr
    (SCRATCH / "verify-creative-tab-sort.log").write_text(combined, encoding="utf-8", errors="replace")
    verifier_ok = gradle.returncode == 0 and "CreativeTabSortVerifier PASS" in combined
    lines.append(f"[JAVA] verifyCreativeTabSort exit={gradle.returncode} pass={verifier_ok}")
    all_ok = all_ok and verifier_ok

    lines.append(f"\nOVERALL_PASS={all_ok}")
    (SCRATCH / "category-audit-transcript.txt").write_text("\n".join(lines) + "\n", encoding="utf-8")
    print("\n".join(lines))
    return 0 if all_ok else 1


if __name__ == "__main__":
    raise SystemExit(main())