#!/usr/bin/env python3
"""Compare EE vs port weapon ItemRender* renderByItem bodies (normalized)."""
import re
from pathlib import Path

EE_WEAPON = Path(r"C:\Users\alex\Desktop\hbmport_1.12.2\мод hbmntm\NTM-Extended-GitHub\src\main\java\com\hbm\render\item\weapon")
PORT_WEAPON = Path(r"C:\Users\alex\Desktop\hbmport_1.12.2\hbm-x5687-1.12.2\src\main\java\com\hbm\render\item\weapon")

RENDER_MAP = {
    "gun_revolver": "ItemRenderWeaponFFColt",
    "gun_revolver_iron": "ItemRenderWeaponFFColt",
    "gun_revolver_saturnite": "ItemRenderWeaponFFColt",
    "gun_revolver_gold": "ItemRenderWeaponFFColt",
    "gun_revolver_lead": "ItemRenderWeaponFFColt",
    "gun_revolver_schrabidium": "ItemRenderWeaponFFColt",
    "gun_revolver_cursed": "ItemRenderRevolverCursed",
    "gun_revolver_nightmare": "ItemRenderRevolverNightmare",
    "gun_revolver_nightmare2": "ItemRenderRevolverNightmare",
    "gun_revolver_pip": "ItemRenderOverkill",
    "gun_revolver_nopip": "ItemRenderOverkill",
    "gun_revolver_blackjack": "ItemRenderOverkill",
    "gun_revolver_silver": "ItemRenderOverkill",
    "gun_revolver_red": "ItemRenderOverkill",
    "gun_spark": "ItemRenderOverkill",
    "gun_revolver_inverted": "ItemRenderRevolverInverted",
    "gun_ar15": "ItemRenderWeaponAR15",
    "gun_supershotgun": "ItemRenderWeaponShotty",
    "jshotgun": "ItemRenderJShotgun",
    "gun_deagle": "ItemRenderWeaponObj",
    "gun_ks23": "ItemRenderWeaponObj",
    "gun_flechette": "ItemRenderWeaponObj",
    "gun_sauer": "ItemRenderWeaponSauer",
    "gun_calamity": "ItemRenderCalamity",
    "gun_calamity_dual": "ItemRenderCalamity",
    "gun_avenger": "ItemRenderMinigun",
    "gun_lacunae": "ItemRenderMinigun",
    "gun_bolt_action": "ItemRenderGunAnim2",
    "gun_bolt_action_green": "ItemRenderGunAnim2",
    "gun_lever_action": "ItemRenderGunAnim2",
    "gun_lever_action_dark": "ItemRenderGunAnim2",
    "gun_thompson": "ItemRenderWeaponThompson",
    "gun_rpg": "ItemRenderRpg",
    "gun_karl": "ItemRenderRpg",
    "gun_hp": "ItemRenderGunHP",
    "gun_defabricator": "ItemRenderGunDefab",
    "gun_euthanasia": "ItemRenderEuthanasia",
    "gun_mp": "ItemRenderMP",
    "gun_cryolator": "ItemRenderCryolator",
    "gun_jack": "ItemRenderGunJack",
    "gun_immolator": "ItemRenderImmolator",
    "gun_lever_action_sonata": "ItemRenderGunSonata",
    "gun_bolt_action_saturnite": "ItemRenderGunSaturnite",
    "gun_dampfmaschine": "ItemRenderBullshit",
    "gun_b93": "RenderGunB93",
    "gun_vortex": "ItemRenderWeaponVortex",
    "cc_plasma_gun": "ItemRenderCCPlasmaCannon",
    "gun_zomg": "ItemRenderZOMG",
    "gun_emp": "ItemRenderEMPRay",
    "gun_osipr": "ItemRenderOSIPR",
    "gun_skystinger": "ItemRenderStinger",
    "gun_uzi_silencer": "ItemRenderUzi",
    "gun_uzi_saturnite": "ItemRenderUzi",
    "gun_uzi_saturnite_silencer": "ItemRenderUzi",
    "gun_fatman": "ItemRenderFatMan",
    "gun_proto": "ItemRenderFatMan",
    "gun_mirv": "ItemRenderMIRVLauncher",
    "gun_bf": "ItemRenderBFLauncher",
    "gun_xvl1456": "ItemRenderXVL1456",
    "gun_egon": "ItemRenderGunEgon",
}


def normalize(text: str) -> str:
    text = text.replace("RefStrings.MODID", "Tags.MODID")
    text = re.sub(r"com\.hbm\.lib\.RefStrings", "com.hbm.Tags", text)
    text = re.sub(r"GlStateManager\.(translate|rotate|scale|translated|rotated|scaled)", r"GL11.gl\1", text)
    text = re.sub(r"GL11\.gl(Translate|Rotate|Scale)f", lambda m: "GL11.gl" + m.group(1).lower() + "d", text)
    text = re.sub(r"\s+", " ", text)
    text = re.sub(r"//.*", "", text)
    return text.strip()


def extract_render_body(path: Path) -> str:
    if not path.exists():
        return ""
    src = path.read_text(encoding="utf-8", errors="replace")
    m = re.search(r"void\s+renderByItem\s*\([^)]*\)\s*\{", src)
    if not m:
        return ""
    start = m.end()
    depth = 1
    i = start
    while i < len(src) and depth:
        if src[i] == "{":
            depth += 1
        elif src[i] == "}":
            depth -= 1
        i += 1
    return normalize(src[start : i - 1])


def main():
    seen = set()
    mismatches = []
    missing_port = []
    missing_ee = []
    for gun, cls in RENDER_MAP.items():
        if cls in seen:
            continue
        seen.add(cls)
        ee = EE_WEAPON / f"{cls}.java"
        port = PORT_WEAPON / f"{cls}.java"
        if not ee.exists():
            missing_ee.append(cls)
            continue
        if not port.exists():
            missing_port.append(cls)
            continue
        ee_body = extract_render_body(ee)
        port_body = extract_render_body(port)
        if ee_body != port_body:
            mismatches.append((cls, len(ee_body), len(port_body)))

    print(f"renderer_classes={len(seen)} mismatches={len(mismatches)}")
    if missing_port:
        print("missing_in_port:", ", ".join(missing_port))
    if missing_ee:
        print("missing_in_ee:", ", ".join(missing_ee))
    for cls, el, pl in sorted(mismatches, key=lambda x: -abs(x[1] - x[2])):
        print(f"  {cls}: ee_len={el} port_len={pl} delta={pl-el}")


if __name__ == "__main__":
    main()