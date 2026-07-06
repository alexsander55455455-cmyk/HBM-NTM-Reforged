import gzip
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
WORKSPACE = Path(__file__).resolve().parents[2]
STRUCT_ROOTS = [
    ROOT / "src/main/resources/assets/hbm/structures",
    WORKSPACE / "NTMstructure 1.12.2 2.0 mod/extracted/assets/ntmdopolnenie/structures",
    WORKSPACE / "мод hbmntm/extracted/assets/hbm/structures",
]
MODBLOCKS = ROOT / "src/main/java/com/hbm/blocks/ModBlocks.java"
EE_MODBLOCKS = WORKSPACE / "мод hbmntm/NTM-Extended-GitHub/src/main/java/com/hbm/blocks/ModBlocks.java"


def extract_registered(path: Path) -> set[str]:
    text = path.read_text(encoding="utf-8", errors="ignore")
    names = set(re.findall(r'new \w+\([^,]+,\s*"([a-z0-9_]+)"', text))
    names |= set(re.findall(r'"([a-z0-9_]+)"\)\.set', text))
    return names


def main() -> None:
    ours = extract_registered(MODBLOCKS)
    ee = extract_registered(EE_MODBLOCKS) if EE_MODBLOCKS.exists() else set()
    ee_only = ee - ours

    block_counts: dict[str, int] = {}
    for struct_root in STRUCT_ROOTS:
        if not struct_root.exists():
            print(f"SKIP missing folder: {struct_root}")
            continue
        for nbt in struct_root.rglob("*.nbt"):
            try:
                data = gzip.open(nbt, "rb").read().decode("latin1", errors="ignore")
            except OSError:
                continue
            for m in re.findall(r"hbm:([a-zA-Z0-9_]+)", data):
                if m.startswith("tileentity_"):
                    m = m[len("tileentity_") :]
                block_counts[m] = block_counts.get(m, 0) + 1

    missing: list[tuple[str, int, str]] = []
    for name, cnt in sorted(block_counts.items(), key=lambda x: -x[1]):
        if name in ours:
            continue
        if name in ee:
            missing.append((name, cnt, "ee_has_not_ported"))
        elif name in ee_only:
            missing.append((name, cnt, "ee_only"))
        else:
            missing.append((name, cnt, "unknown"))

    print("=== Blocks in structures missing from our ModBlocks ===")
    for name, cnt, kind in missing:
        print(f"{cnt:6d}  {name}  ({kind})")
    print(f"total: {len(missing)}")
    for t in ("turret_cwis", "turret_cheapo"):
        print(f"{t}: {block_counts.get(t, 0)} uses, registered={t in ours}")


if __name__ == "__main__":
    main()