"""Append missing blockTab entries to creative_tab_order.txt."""
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ORDER = ROOT / "src/main/resources/assets/hbm/creative_tab_order.txt"

CTOR_BLOCK_TAB = (
    re.compile(r"new\s+TritiumLamp\s*\(\s*\"[^\"]+\"\s*,\s*false\s*\)"),
    re.compile(r"new\s+BlockGlyphid(?:Spawner)?\s*\("),
    re.compile(r"new\s+BlockLayering\s*\("),
    re.compile(r"new\s+BlockRBMKSlab\s*\("),
)


def normalize_tab(tab_raw: str | None) -> str | None:
    if not tab_raw:
        return None
    tab_raw = tab_raw.strip()
    if "null" in tab_raw or "?" in tab_raw:
        return None
    if "MainRegistry." in tab_raw:
        return tab_raw.split("MainRegistry.")[-1].strip()
    if tab_raw.endswith("Tab"):
        return tab_raw
    return None


def field_chunk(text: str, start: int) -> str:
    semi = text.find(";", start)
    if semi < 0:
        return text[start : start + 8000]
    return text[start : semi + 1]


def registry_path(chunk: str, field: str) -> str:
    reg_m = re.search(r'\.setRegistryName\(\s*"([^"]+)"\s*\)', chunk)
    if reg_m:
        return reg_m.group(1)
    trans_m = re.search(r'\.setTranslationKey\(\s*"([^"]+)"\s*\)', chunk)
    if trans_m:
        return trans_m.group(1)
    ctor_m = re.search(r'new\s+[\w.]+(?:<[^>]+>)?\s*\(\s*"([^"]+)"', chunk)
    if ctor_m:
        return ctor_m.group(1)
    return field


def infer_block_tab(chunk: str) -> str | None:
    tab_m = re.search(r"\.setCreativeTab\(([^);]+)\)", chunk)
    tab = normalize_tab(tab_m.group(1)) if tab_m else None
    if tab:
        return tab
    if re.search(r"new\s+BlockBase\s*\(", chunk):
        return "controlTab"
    for pattern in CTOR_BLOCK_TAB:
        if pattern.search(chunk):
            return "blockTab"
    return None


def parse_block_tab_keys(path: Path, namespace: str = "hbm") -> list[str]:
    text = path.read_text(encoding="utf-8")
    keys: list[str] = []
    for m in re.finditer(r"public static final Block\s+(\w+)\s*=\s*", text):
        field = m.group(1)
        chunk = field_chunk(text, m.start())
        if infer_block_tab(chunk) != "blockTab":
            continue
        reg = registry_path(chunk, field)
        keys.append(reg if namespace == "hbm" else f"{namespace}:{reg}")
    return keys


def read_block_tab_section() -> tuple[set[str], int]:
    lines = ORDER.read_text(encoding="utf-8").splitlines()
    keys: list[str] = []
    end = len(lines)
    in_tab = False
    for i, line in enumerate(lines):
        stripped = line.strip()
        if stripped == "@blockTab":
            in_tab = True
            continue
        if in_tab and stripped.startswith("@"):
            end = i
            break
        if in_tab and "=" in stripped and not stripped.startswith("#"):
            keys.append(stripped.split("=", 1)[0])
    last_idx = 477
    if keys:
        for line in lines:
            if line.strip().startswith(keys[-1] + "="):
                last_idx = int(line.strip().split("=", 1)[1])
                break
    return set(keys), last_idx + 1


def main() -> None:
    declared: list[str] = []
    seen: set[str] = set()
    for path, ns in (
        (ROOT / "src/main/java/com/hbm/blocks/ModBlocks.java", "hbm"),
        (ROOT / "src/main/java/com/hbmspace/blocks/ModBlocksSpace.java", "hbmspace"),
    ):
        for key in parse_block_tab_keys(path, ns):
            if key not in seen:
                seen.add(key)
                declared.append(key)

    existing, start_idx = read_block_tab_section()
    missing = [k for k in declared if k not in existing]

    lines = ORDER.read_text(encoding="utf-8").splitlines()
    end = next(i for i, line in enumerate(lines) if i > 0 and lines[i].strip() == "@consumableTab")

    new_lines = []
    idx = start_idx
    for key in missing:
        new_lines.append(f"{key}={idx}")
        idx += 1

    out = lines[:end] + new_lines + lines[end:]
    ORDER.write_text("\n".join(out) + "\n", encoding="utf-8")
    print(f"declared_blockTab={len(declared)} existing={len(existing)} missing={len(missing)}")
    for line in new_lines[:30]:
        print(line)
    if len(new_lines) > 30:
        print(f"... +{len(new_lines) - 30} more")


if __name__ == "__main__":
    main()