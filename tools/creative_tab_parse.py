"""Parse ModItems/ModBlocks declarations into (creativeTab, registryPath) pairs."""
import re
from pathlib import Path

VANILLA_TABS = {
    "CreativeTabs.COMBAT",
    "CreativeTabs.MISC",
    "CreativeTabs.TOOLS",
    "CreativeTabs.MATERIALS",
    "CreativeTabs.FOOD",
    "CreativeTabs.BUILDING_BLOCKS",
    "CreativeTabs.DECORATIONS",
    "CreativeTabs.TRANSPORTATION",
    "CreativeTabs.REDSTONE",
    "null",
    "NONE",
}

REGISTRY_STRING = re.compile(r"^[a-z0-9_]+$")

CTOR_TAB_INFERENCE = (
    (re.compile(r"new\s+(?:com\.hbm\.items\.gear\.)?ModShield\s*\("), "weaponTab"),
    (re.compile(r"new\s+ItemAmmoHIMARS\s*\("), "weaponTab"),
    (re.compile(r"new\s+ItemAmmoArty\s*\("), "weaponTab"),
)

# Sedna guns: declared in ModItems as `public static Item gun_x;`, registered in XFactory*.java.
SEDNA_FACTORY_GUN_RE = re.compile(
    r"new\s+ItemGun(?:BaseNT|Drill|Chemthrower|Stinger|PA|ChargeThrower|NI4NI)\("
    r"[^)]*?WeaponQuality\.(A_SIDE|SPECIAL|UTILITY)\s*,\s*\"([a-z0-9_]+)\"",
    re.DOTALL,
)

# GunFactory.java: ItemEnumMulti registrations with weaponTab (ammo_standard, weapon_mod_*).
GUN_FACTORY_WEAPON_TAB_RE = re.compile(
    r"=\s*new\s+ItemEnumMulti(?:<[^>]*>)?\(\s*\"([a-z0-9_]+)\"[^;]*"
    r"\.setCreativeTab\(MainRegistry\.weaponTab\)",
    re.DOTALL,
)

HBM_TAB_KEYS = {
    "blockTab",
    "consumableTab",
    "controlTab",
    "machineTab",
    "missileTab",
    "nukeTab",
    "partsTab",
    "resourceTab",
    "templateTab",
    "weaponTab",
}


def normalize_tab(tab_raw):
    tab_raw = tab_raw.strip()
    if "?" in tab_raw:
        return None
    if tab_raw in VANILLA_TABS:
        return None
    if "MainRegistry." in tab_raw:
        return tab_raw.split("MainRegistry.")[-1].strip()
    if tab_raw.endswith("Tab"):
        return tab_raw
    return None


def extract_registry_path(chunk, field_name):
    reg_m = re.search(r'\.setRegistryName\(\s*"([^"]+)"\s*\)', chunk)
    if reg_m:
        return reg_m.group(1)

    trans_m = re.search(r'\.setTranslationKey\(\s*"([^"]+)"\s*\)', chunk)
    if trans_m and REGISTRY_STRING.match(trans_m.group(1)):
        return trans_m.group(1)

    baked_m = re.search(r'new\s+ItemBakedBase\s*\(\s*"([^"]+)"', chunk)
    if baked_m and REGISTRY_STRING.match(baked_m.group(1)):
        return baked_m.group(1)

    new_m = re.search(r'=\s*new\s+[\w.]+(?:<[^>]+>)?\s*\(', chunk)
    if new_m:
        start = new_m.end()
        depth = 1
        i = start
        while i < len(chunk) and depth > 0:
            c = chunk[i]
            if c == "(":
                depth += 1
            elif c == ")":
                depth -= 1
            i += 1
        args = chunk[start:i - 1]
        strings = re.findall(r'"([^"]+)"', args)
        candidates = [s for s in strings if REGISTRY_STRING.match(s)]
        if candidates:
            if field_name in candidates:
                return field_name
            return candidates[-1]

    if re.search(
        rf'new\s+(?:Item|Block)Base\s*\([^)]*"{re.escape(field_name)}"',
        chunk,
    ):
        return field_name
    if re.search(
        rf'new\s+ItemBattery\s*\([^)]*"{re.escape(field_name)}"',
        chunk,
    ):
        return field_name
    if re.search(
        rf'new\s+ItemSelfcharger\s*\([^)]*"{re.escape(field_name)}"',
        chunk,
    ):
        return field_name

    if re.search(r"=\s*new\s+ItemPWRFuel\s*\(\s*\)", chunk):
        return "pwr_fuel"

    return None


def infer_tab_from_ctor(chunk: str) -> str | None:
    for pattern, tab in CTOR_TAB_INFERENCE:
        if pattern.search(chunk):
            return tab
    return None


def field_declaration_chunk(text: str, start: int) -> str:
    semi = text.find(";", start)
    if semi < 0:
        return text[start : start + 4000]
    return text[start : semi + 1]


def parse_entries(path, kind, namespace="hbm"):
    text = Path(path).read_text(encoding="utf-8")
    entries = []
    if kind == "item":
        pat = r"public static final (?:Item|Item\w+)\s+(\w+)\s*=\s*"
    else:
        pat = r"public static final Block\s+(\w+)\s*=\s*"
    for m in re.finditer(pat, text):
        field_name = m.group(1)
        chunk = field_declaration_chunk(text, m.start())
        tab_m = re.search(r"\.setCreativeTab\(([^);]+)\)", chunk)
        tab = normalize_tab(tab_m.group(1)) if tab_m else None
        if not tab:
            tab = infer_tab_from_ctor(chunk)
        registry_path = extract_registry_path(chunk, field_name)
        if not registry_path:
            continue
        if namespace != "hbm":
            registry_key = f"{namespace}:{registry_path}"
        else:
            registry_key = registry_path
        entries.append((registry_key, registry_path, tab))
    return entries


def collect_gun_factory_weapon_tab_entries(gun_factory: Path) -> list[tuple[str, str]]:
    """Registry paths for GunFactory ItemEnumMulti items on weaponTab."""
    entries: list[tuple[str, str]] = []
    seen: set[str] = set()
    if not gun_factory.is_file():
        return entries
    text = gun_factory.read_text(encoding="utf-8")
    for match in GUN_FACTORY_WEAPON_TAB_RE.finditer(text):
        reg_path = match.group(1)
        if reg_path in seen:
            continue
        seen.add(reg_path)
        entries.append((reg_path, reg_path))
    return entries


def collect_sedna_weapon_tab_entries(factory_dir: Path) -> list[tuple[str, str]]:
    """Registry paths for sedna guns that land on weaponTab (A_SIDE / SPECIAL / UTILITY)."""
    entries: list[tuple[str, str]] = []
    seen: set[str] = set()
    if not factory_dir.is_dir():
        return entries
    for path in sorted(factory_dir.glob("*.java")):
        text = path.read_text(encoding="utf-8")
        for match in SEDNA_FACTORY_GUN_RE.finditer(text):
            reg_path = match.group(2)
            if reg_path in seen:
                continue
            seen.add(reg_path)
            entries.append((reg_path, reg_path))
    return entries


def collect_port_tab_entries(
    mod_items: Path,
    mod_blocks: Path,
    *,
    space_items: Path | None = None,
    space_blocks: Path | None = None,
) -> dict[str, list[tuple[str, str]]]:
    raw: list[tuple[str, str, str | None]] = []
    raw.extend(parse_entries(mod_blocks, "block"))
    raw.extend(parse_entries(mod_items, "item"))
    if space_blocks and space_blocks.exists():
        raw.extend(parse_entries(space_blocks, "block", "hbmspace"))
    if space_items and space_items.exists():
        raw.extend(parse_entries(space_items, "item", "hbmspace"))

    tabs: dict[str, list[tuple[str, str]]] = {k: [] for k in HBM_TAB_KEYS}
    seen: dict[str, set[str]] = {k: set() for k in HBM_TAB_KEYS}
    for registry_key, path, tab in raw:
        if not tab or tab not in HBM_TAB_KEYS:
            continue
        if registry_key in seen[tab]:
            continue
        tabs[tab].append((registry_key, path))
        seen[tab].add(registry_key)

    sedna_factory = mod_items.parent / "weapon" / "sedna" / "factory"
    for registry_key, path in collect_sedna_weapon_tab_entries(sedna_factory):
        if registry_key not in seen["weaponTab"]:
            tabs["weaponTab"].append((registry_key, path))
            seen["weaponTab"].add(registry_key)

    gun_factory = sedna_factory / "GunFactory.java"
    for registry_key, path in collect_gun_factory_weapon_tab_entries(gun_factory):
        if registry_key not in seen["weaponTab"]:
            tabs["weaponTab"].append((registry_key, path))
            seen["weaponTab"].add(registry_key)

    return tabs