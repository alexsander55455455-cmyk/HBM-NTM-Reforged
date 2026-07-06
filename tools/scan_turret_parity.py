import gzip
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
WORKSPACE = Path(__file__).resolve().parents[2]
MODBLOCKS = ROOT / "src/main/java/com/hbm/blocks/ModBlocks.java"
EE_MODBLOCKS = WORKSPACE / "мод hbmntm/NTM-Extended-GitHub/src/main/java/com/hbm/blocks/ModBlocks.java"
STRUCT_ROOTS = [
    ROOT / "src/main/resources/assets/hbm/structures",
    WORKSPACE / "NTMstructure 1.12.2 2.0 mod/extracted/assets/ntmdopolnenie/structures",
]


def turret_names(path: Path) -> set[str]:
    text = path.read_text(encoding="utf-8", errors="ignore")
    return set(re.findall(r"turret_[a-z0-9_]+", text))


def main() -> None:
    ours = {n for n in turret_names(MODBLOCKS) if not n.endswith("_ammo")}
    ee = {n for n in turret_names(EE_MODBLOCKS) if not n.endswith("_ammo")}
    missing = sorted(ee - ours)
    extra = sorted(ours - ee)

    counts: dict[str, int] = {}
    for root in STRUCT_ROOTS:
        if not root.exists():
            continue
        for nbt in root.rglob("*.nbt"):
            try:
                data = gzip.open(nbt, "rb").read().decode("latin1", errors="ignore")
            except OSError:
                continue
            for m in re.findall(r"hbm:(turret_[a-zA-Z0-9_]+)", data):
                if m.startswith("tileentity_"):
                    m = m[len("tileentity_") :]
                counts[m] = counts.get(m, 0) + 1

    print("=== EE turret blocks missing in ModBlocks ===")
    for name in missing:
        print(f"  {name}  (structures: {counts.get(name, 0)})")
    print(f"missing count: {len(missing)}")
    print()
    print("=== Port-only turret blocks ===")
    for name in extra:
        print(f"  {name}")
    print()
    print("=== Turrets referenced in structures ===")
    for name, cnt in sorted(counts.items(), key=lambda x: -x[1]):
        status = "OK" if name in ours or name.endswith("_ammo") else "MISSING"
        print(f"{cnt:5d}  {name}  [{status}]")


if __name__ == "__main__":
    main()