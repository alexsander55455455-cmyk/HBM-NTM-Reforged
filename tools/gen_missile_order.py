"""Generate missing missileTab entries for creative_tab_order.txt."""
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent


def extract_missile_items(path: Path, ns: str = "hbm") -> list[str]:
    text = path.read_text(encoding="utf-8")
    items: list[str] = []
    pat = r"public static final (?:Item\w*|Item)\s+(\w+)\s*=\s*"
    for m in re.finditer(pat, text):
        field = m.group(1)
        semi = text.find(";", m.start())
        chunk = text[m.start() : semi + 1]
        reg_m = re.search(r'"([a-z0-9_]+)"', chunk)
        reg = reg_m.group(1) if reg_m else field
        is_missile = (
            "ItemMissile" in chunk
            or "ItemCustomMissilePart" in chunk
            or "ItemSatelliteSpace" in chunk
        )
        tab_m = re.search(r"\.setCreativeTab\(([^)]+)\)", chunk)
        tab = tab_m.group(1) if tab_m else None
        if tab and "null" in tab:
            continue
        if not (is_missile or (tab and "missileTab" in tab)):
            continue
        key = reg if ns == "hbm" else f"{ns}:{reg}"
        copy_m = re.search(r'\.copy\("([a-z0-9_]+)"\)', chunk)
        if copy_m:
            key = copy_m.group(1) if ns == "hbm" else f"{ns}:{copy_m.group(1)}"
        items.append(key)
    return items


def main() -> None:
    hbm_path = ROOT / "src/main/java/com/hbm/items/ModItems.java"
    space_path = ROOT / "src/main/java/com/hbmspace/items/ModItemsSpace.java"
    order_path = ROOT / "src/main/resources/assets/hbm/creative_tab_order.txt"

    hbm_all = extract_missile_items(hbm_path)
    space_all = extract_missile_items(space_path, "hbmspace")

    # Missile parts block starts at mp_thruster_10_kerosene in ModItems declaration order.
    start = hbm_all.index("mp_thruster_10_kerosene")
    hbm_tail = hbm_all[start:]

    # Space missile-only items not already in the main order before sat_war block.
    space_tail = [
        x
        for x in space_all
        if x.startswith("hbmspace:mp_")
        or x.startswith("hbmspace:rp_f")
        or x in ("hbmspace:rp_l_20",)
    ]

    order_text = order_path.read_text(encoding="utf-8")
    existing = set()
    in_missile = False
    for line in order_text.splitlines():
        line = line.strip()
        if line == "@missileTab":
            in_missile = True
            continue
        if line.startswith("@") and in_missile:
            break
        if in_missile and "=" in line and not line.startswith("#"):
            existing.add(line.split("=", 1)[0])

    missing_hbm = [x for x in hbm_tail if x not in existing]
    missing_space = [x for x in space_tail if x not in existing]

    base = 126
    lines = []
    for key in missing_hbm + missing_space:
        lines.append(f"{key}={base}")
        base += 1

    print(f"missing_hbm={len(missing_hbm)} missing_space={len(missing_space)}")
    for line in lines:
        print(line)


if __name__ == "__main__":
    main()