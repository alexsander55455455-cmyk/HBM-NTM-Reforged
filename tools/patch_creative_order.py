"""Apply batch creative_tab_order.txt fixes."""
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ORDER = ROOT / "src/main/resources/assets/hbm/creative_tab_order.txt"

MISSILE_BLOCK = """\
mp_thruster_10_kerosene=126
mp_thruster_10_solid=127
mp_thruster_10_xenon=128
mp_thruster_15_kerosene=129
mp_thruster_15_kerosene_dual=130
mp_thruster_15_kerosene_triple=131
mp_thruster_15_solid=132
mp_thruster_15_solid_hexdecuple=133
mp_thruster_15_hydrogen=134
mp_thruster_15_hydrogen_dual=135
mp_thruster_15_balefire_short=136
mp_thruster_15_balefire=137
mp_thruster_15_balefire_large=138
mp_thruster_15_balefire_large_rad=139
mp_thruster_20_kerosene=140
mp_thruster_20_kerosene_dual=141
mp_thruster_20_kerosene_triple=142
hbmspace:mp_thruster_20_methalox=143
hbmspace:mp_thruster_20_methalox_dual=144
hbmspace:mp_thruster_20_methalox_triple=145
hbmspace:mp_thruster_20_hydrogen=146
hbmspace:mp_thruster_20_hydrogen_dual=147
hbmspace:mp_thruster_20_hydrogen_triple=148
mp_thruster_20_solid=149
mp_thruster_20_solid_multi=150
mp_thruster_20_solid_multier=151
mp_stability_10_flat=152
mp_stability_10_cruise=153
mp_stability_10_space=154
mp_stability_15_flat=155
mp_stability_15_thin=156
mp_stability_15_soyuz=157
mp_fuselage_10_kerosene=158
mp_fuselage_10_kerosene_camo=159
mp_fuselage_10_kerosene_desert=160
mp_fuselage_10_kerosene_sky=161
mp_fuselage_10_kerosene_flames=162
mp_fuselage_10_kerosene_insulation=163
mp_fuselage_10_kerosene_sleek=164
mp_fuselage_10_kerosene_metal=165
mp_fuselage_10_kerosene_taint=166
mp_fuselage_10_solid=167
mp_fuselage_10_solid_flames=168
mp_fuselage_10_solid_insulation=169
mp_fuselage_10_solid_sleek=170
mp_fuselage_10_solid_soviet_glory=171
mp_fuselage_10_solid_cathedral=172
mp_fuselage_10_solid_moonlit=173
mp_fuselage_10_solid_battery=174
mp_fuselage_10_solid_duracell=175
mp_fuselage_10_xenon=176
mp_fuselage_10_xenon_bhole=177
mp_fuselage_10_long_kerosene=178
mp_fuselage_10_long_kerosene_camo=179
mp_fuselage_10_long_kerosene_desert=180
mp_fuselage_10_long_kerosene_sky=181
mp_fuselage_10_long_kerosene_flames=182
mp_fuselage_10_long_kerosene_insulation=183
mp_fuselage_10_long_kerosene_sleek=184
mp_fuselage_10_long_kerosene_metal=185
mp_fuselage_10_long_kerosene_dash=186
mp_fuselage_10_long_kerosene_taint=187
mp_fuselage_10_long_kerosene_vap=188
mp_fuselage_10_long_solid=189
mp_fuselage_10_long_solid_flames=190
mp_fuselage_10_long_solid_insulation=191
mp_fuselage_10_long_solid_sleek=192
mp_fuselage_10_long_solid_soviet_glory=193
mp_fuselage_10_long_solid_bullet=194
mp_fuselage_10_long_solid_silvermoonlight=195
mp_fuselage_10_15_kerosene=196
mp_fuselage_10_15_solid=197
mp_fuselage_10_15_hydrogen=198
mp_fuselage_10_15_balefire=199
mp_fuselage_15_kerosene=200
mp_fuselage_15_kerosene_camo=201
mp_fuselage_15_kerosene_desert=202
mp_fuselage_15_kerosene_sky=203
mp_fuselage_15_kerosene_insulation=204
mp_fuselage_15_kerosene_metal=205
mp_fuselage_15_kerosene_decorated=206
mp_fuselage_15_kerosene_steampunk=207
mp_fuselage_15_kerosene_polite=208
mp_fuselage_15_kerosene_blackjack=209
mp_fuselage_15_kerosene_lambda=210
mp_fuselage_15_kerosene_minuteman=211
mp_fuselage_15_kerosene_pip=212
mp_fuselage_15_kerosene_taint=213
mp_fuselage_15_kerosene_yuck=214
mp_fuselage_15_solid_insulation=215
mp_fuselage_15_solid_desh=216
mp_fuselage_15_solid_soviet_glory=217
mp_fuselage_15_solid_soviet_stank=218
mp_fuselage_15_solid_faust=219
mp_fuselage_15_solid_silvermoonlight=220
mp_fuselage_15_solid_snowy=221
mp_fuselage_15_solid_panorama=222
mp_fuselage_15_solid_roses=223
mp_fuselage_15_solid_mimi=224
mp_fuselage_15_hydrogen_cathedral=225
mp_fuselage_15_20_kerosene_magnusson=226
mp_warhead_15_nuclear_shark=227
hbmspace:rp_f_20_12=228
hbmspace:rp_f_20_6=229
hbmspace:rp_f_20_3=230
hbmspace:rp_f_20_1=231
hbmspace:rp_l_20=232
hbmspace:mp_fuselage_20_hydrazine=233"""

RBMK_RODS = """\
hbmspace:rbmk_fuel_bk247=337
hbmspace:rbmk_fuel_lecm=338
hbmspace:rbmk_fuel_mecm=339
hbmspace:rbmk_fuel_hecm=340"""

SELLAFIELD_ORES = """\
ore_sellafield_emerald=86
ore_sellafield_uranium_scorched=87
ore_sellafield_schrabidium=88
ore_sellafield_diamond=89
ore_sellafield_radgem=90"""

SHIFT_AFTER_RBMK_DRX = {
    "rbmk_fuel_test": 341,
    "watz_pellet": 342,
    "watz_pellet_depleted": 343,
    "battery_pack": 344,
    "pwr_fuel_depleted": 345,
    "hbmspace:hard_drive_full": 346,
    "hbmspace:hard_drive": 347,
}


def shift_index_line(line: str, delta: int, min_index: int) -> str:
    if "=" not in line or line.strip().startswith("@") or line.strip().startswith("#"):
        return line
    key, value = line.split("=", 1)
    idx = int(value)
    if idx >= min_index:
        return f"{key}={idx + delta}"
    return line


def main() -> None:
    text = ORDER.read_text(encoding="utf-8")
    lines = text.splitlines()

    out: list[str] = []
    current_tab: str | None = None
    i = 0
    while i < len(lines):
        line = lines[i]
        stripped = line.strip()

        if stripped.startswith("@"):
            current_tab = stripped[1:]
            out.append(line)
            i += 1
            continue

        if stripped == "missile_soyuz2=125":
            out.append(line)
            out.extend(MISSILE_BLOCK.splitlines())
            i += 1
            continue

        if stripped == "rbmk_fuel_drx=336":
            out.append(line)
            out.extend(RBMK_RODS.splitlines())
            i += 1
            continue

        if current_tab == "controlTab" and stripped.split("=", 1)[0] in SHIFT_AFTER_RBMK_DRX:
            key = stripped.split("=", 1)[0]
            if key == "hbmspace:hard_drive":
                i += 1
                continue
            out.append(f"{key}={SHIFT_AFTER_RBMK_DRX[key]}")
            i += 1
            continue

        if stripped == "ore_meteor_starmetal=85":
            out.append(line)
            out.extend(SELLAFIELD_ORES.splitlines())
            i += 1
            continue

        if stripped.startswith("hbmspace:hard_drive=") and current_tab == "partsTab":
            i += 1
            continue

        if stripped == "hbmspace:hard_drive_full=346" or stripped.endswith("hard_drive_full=342"):
            out.append(line)
            out.append("hbmspace:hard_drive=347")
            i += 1
            continue

        if current_tab == "resourceTab" and "=" in stripped:
            out.append(shift_index_line(line, 5, 86))
            i += 1
            continue

        out.append(line)
        i += 1

    ORDER.write_text("\n".join(out) + "\n", encoding="utf-8")
    print("patched", ORDER)


if __name__ == "__main__":
    main()