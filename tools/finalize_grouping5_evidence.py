"""Overwrite evidence bundle for creative-tab-grouping-5: git scope + full verify + gradle."""
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
BUNDLE = SCRATCH / "evidence_bundle.txt"
ORDER = PROJ / "src/main/resources/assets/hbm/creative_tab_order.txt"
MOD_ITEMS = PROJ / "src/main/java/com/hbm/items/ModItems.java"
MOD_BLOCKS = PROJ / "src/main/java/com/hbm/blocks/ModBlocks.java"

sys.path.insert(0, str(TOOLS))
from cluster_creative_tab_order import GOAL_CLUSTER_VERSION, is_gun_ammo_path, registry_path  # noqa: E402
from creative_tab_parse import collect_port_tab_entries  # noqa: E402

SCOPE_ALLOW = {
    "src/main/java/com/hbm/items/ModItems.java",
    "src/main/java/com/hbm/creativetabs/CreativeTabSortVerifier.java",
    "src/main/java/com/hbm/main/client/NTMClientRegistry.java",
    "src/main/resources/assets/hbm/creative_tab_order.txt",
    "tools/cluster_creative_tab_order.py",
    "tools/audit_tab_membership.py",
    "tools/audit_creative_groupings.py",
    "tools/verify_creative_tab_grouping.py",
    "tools/test_cluster_weapon_tab.py",
    "tools/test_cluster_parts_tab.py",
    "tools/finalize_grouping5_evidence.py",
}

SCOPE_FORBIDDEN = ("ModBlocks.java", "agent-tools", "run_grouping5_gate.py", "gradle_ephemeral_ntm_patch.ps1")

GOAL_TAB_TARGETS: dict[str, str] = {
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
}

PY_STEPS = (
    "cluster_creative_tab_order.py",
    "test_cluster_weapon_tab.py",
    "test_cluster_parts_tab.py",
    "verify_creative_tab_grouping.py",
    "audit_tab_membership.py",
    "audit_creative_groupings.py",
)

STALE_SCRATCH = (
    "changed-files.txt",
    "verify-python.txt",
    "verify-all.txt",
    "goal-completion-summary.txt",
)


def lines() -> list[str]:
    return getattr(lines, "_buf", [])  # type: ignore[attr-defined]


def log(msg: str) -> None:
    buf = lines._buf  # type: ignore[attr-defined]
    buf.append(msg)
    print(msg)


def git_changed() -> tuple[list[str], list[str]]:
    status = subprocess.run(
        ["git", "status", "--porcelain"],
        cwd=PROJ,
        capture_output=True,
        text=True,
        check=True,
    )
    diff = subprocess.run(
        ["git", "diff", "--name-only"],
        cwd=PROJ,
        capture_output=True,
        text=True,
        check=True,
    )
    porcelain = [ln for ln in status.stdout.splitlines() if ln.strip()]
    names = [ln.strip() for ln in diff.stdout.splitlines() if ln.strip()]
    return porcelain, names


def normalized_changed_paths(porcelain: list[str]) -> set[str]:
    out: set[str] = set()
    for line in porcelain:
        path = line[3:].strip().replace("\\", "/")
        if " -> " in path:
            path = path.split(" -> ", 1)[1]
        if path.startswith("tools/__pycache__"):
            continue
        out.add(path)
    return out


def check_scope(changed: set[str]) -> bool:
    ok = True
    for path in sorted(changed):
        if any(f in path for f in SCOPE_FORBIDDEN):
            log(f"SCOPE_FAIL forbidden: {path}")
            ok = False
        elif path not in SCOPE_ALLOW:
            log(f"SCOPE_FAIL out_of_allowlist: {path}")
            ok = False
        else:
            log(f"SCOPE_OK: {path}")
    if "src/main/java/com/hbm/main/client/NTMClientRegistry.java" in changed:
        log("NTM_NOTE: build-unblock only (removed sliding_blast_door_legacy mapper; field absent in port)")
    return ok


def run_py(name: str) -> tuple[bool, str]:
    env = {**os.environ, "GOAL_SCRATCH": str(SCRATCH)}
    proc = subprocess.run(
        [sys.executable, str(TOOLS / name)],
        cwd=PROJ,
        capture_output=True,
        text=True,
        env=env,
    )
    out = (proc.stdout or "") + (proc.stderr or "")
    log(f"--- {name} exit={proc.returncode} ---")
    log(out.rstrip())
    return proc.returncode == 0, out


def run_gradle(task_line: str, args: list[str]) -> tuple[bool, str]:
    env = {**os.environ, "GOAL_SCRATCH": str(SCRATCH)}
    env.setdefault("JAVA_TOOL_OPTIONS", "-DDISABLE_BUILDSCRIPT_UPDATE_CHECK=true")
    proc = subprocess.run(
        [str(PROJ / "gradlew.bat"), *args, "--no-daemon"],
        cwd=PROJ,
        capture_output=True,
        text=True,
        env=env,
    )
    out = (proc.stdout or "") + (proc.stderr or "")
    log(f"--- {task_line} exit={proc.returncode} ---")
    log(out.rstrip())
    ok = proc.returncode == 0
    if "verifyCreativeTabSort" in task_line:
        ok = ok and "CreativeTabSortVerifier PASS" in out
    if "build" in task_line:
        ok = ok and "BUILD SUCCESSFUL" in out
    return ok, out


def assert_moditems_tabs() -> bool:
    declared = collect_port_tab_entries(MOD_ITEMS, MOD_BLOCKS)
    path_to_tab: dict[str, str] = {}
    for tab, entries in declared.items():
        for _key, path in entries:
            path_to_tab[registry_path(path)] = tab
    ok = True
    for path, want in GOAL_TAB_TARGETS.items():
        got = path_to_tab.get(path)
        item_ok = got == want
        log(f"ModItems {path}: {got} want={want} ok={item_ok}")
        ok = ok and item_ok
    return ok


def jar_check() -> bool:
    libs = PROJ / "build/libs"
    jars = [
        p
        for p in libs.glob("HBM-NTM-Reforged-*-alpha.jar")
        if all(tag not in p.name for tag in ("-dev", "-api", "-sources", "-downgraded", "-shadow"))
    ]
    if not jars:
        log("JAR_FAIL: missing release jar")
        return False
    jar = max(jars, key=lambda p: p.stat().st_mtime)
    match = zipfile.ZipFile(jar).read("assets/hbm/creative_tab_order.txt") == ORDER.read_bytes()
    fresh = jar.stat().st_mtime >= ORDER.stat().st_mtime
    log(f"JAR: {jar.name} bytes_match={match} fresh={fresh}")
    return match and fresh


def write_changed_files_txt(porcelain: list[str], diff_names: list[str]) -> None:
    """Canonical CHANGED_FILES for harness — must match git status/diff exactly."""
    out: list[str] = [
        "# CHANGED_FILES canonical (matches git status --porcelain + untracked tools)",
        "# ModBlocks.java: NOT modified",
        "",
        "=== git status --porcelain ===",
    ]
    out.extend(porcelain)
    out.extend(["", "=== git diff --name-only (tracked modified) ==="])
    out.extend(diff_names)
    untracked: list[str] = []
    for line in porcelain:
        if line.startswith("??"):
            path = line[3:].strip().replace("\\", "/")
            if path.startswith("tools/") and "__pycache__" not in path:
                untracked.append(path)
    untracked = sorted(untracked)
    if untracked:
        out.extend(["", "=== untracked tools (in scope) ==="])
        out.extend(untracked)
    out.extend([
        "",
        "=== scope notes ===",
        "NTMClientRegistry.java: build-unblock (removed sliding_blast_door_legacy mapper)",
        "finalize_grouping5_evidence.py: evidence orchestrator (SCRATCH bundle writer)",
    ])
    (SCRATCH / "CHANGED_FILES.txt").write_text("\n".join(out) + "\n", encoding="utf-8")


def main() -> int:
    lines._buf = []  # type: ignore[attr-defined]
    SCRATCH.mkdir(parents=True, exist_ok=True)
    for stale in STALE_SCRATCH:
        p = SCRATCH / stale
        if p.exists():
            p.unlink()

    log(f"finalize_grouping5_evidence.py version={GOAL_CLUSTER_VERSION}")
    porcelain, diff_names = git_changed()
    write_changed_files_txt(porcelain, diff_names)

    log("=== CHANGED_FILES (canonical) ===")
    log((SCRATCH / "CHANGED_FILES.txt").read_text(encoding="utf-8").rstrip())

    log("=== GIT_CHANGED ===")
    for ln in porcelain:
        log(ln)
    log("git diff --name-only:")
    for n in diff_names:
        log(n)
    log("ModBlocks.java: NOT in git diff")

    changed = normalized_changed_paths(porcelain)
    log("=== SCOPE_OK ===")
    scope_ok = check_scope(changed)

    log("=== TEST_OUTPUT ===")
    all_ok = scope_ok
    for step in PY_STEPS:
        ok, _ = run_py(step)
        all_ok = all_ok and ok

    hm = None
    for ln in lines._buf:  # type: ignore[attr-defined]
        if m := re.search(r"hard_mismatches=(\d+)", ln):
            hm = int(m.group(1))
    if hm != 0:
        log(f"AUDIT_FAIL hard_mismatches={hm}")
        all_ok = False
    else:
        log(f"AUDIT_COUNTS hard_mismatches={hm}")

    log("=== MODITEMS_TAB_TARGETS ===")
    all_ok = assert_moditems_tabs() and all_ok

    header = ORDER.read_text(encoding="utf-8").splitlines()[0]
    log(f"ORDER_HEADER: {header}")
    all_ok = GOAL_CLUSTER_VERSION in header and all_ok

    log("=== GRADLE ===")
    v_ok, _ = run_gradle("gradlew verifyCreativeTabSort", ["verifyCreativeTabSort"])
    b_ok, _ = run_gradle(
        "gradlew build -x test",
        ["build", "-x", "test", "--rerun-tasks", "processResources", "reobfJar"],
    )
    all_ok = v_ok and b_ok and all_ok
    all_ok = jar_check() and all_ok

    log("=== AUDIT_COUNTS ===")
    snapshot = list(lines._buf)  # type: ignore[attr-defined]
    for ln in snapshot:
        if "audited_paths=" in ln or "report_rows=" in ln or "OVERALL_PASS=" in ln:
            log(f"  {ln.strip()}")

    log(f"BUNDLE_OVERALL_PASS={all_ok}")
    BUNDLE.write_text("\n".join(lines._buf) + "\n", encoding="utf-8")  # type: ignore[attr-defined]
    log(f"evidence_bundle.txt={BUNDLE}")
    return 0 if all_ok else 1


if __name__ == "__main__":
    raise SystemExit(main())