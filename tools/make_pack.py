#!/usr/bin/env python3
"""
Erzeugt das Resource Pack (ClansPack.zip) für das Clans-Plugin.

Alle Texturen werden hier per Code gezeichnet (16x16 Pixel-Art). Du kannst sie
danach in assets/clans/textures/item/ durch eigene Texturen ersetzen, solange die
Dateinamen gleich bleiben.

Aufruf:  python3 tools/make_pack.py  [ausgabe.zip]
"""
import io
import json
import sys
import zipfile
import os
from PIL import Image, ImageDraw, ImageFilter, ImageFont

S = 16
NS = "clans"

# --- Farben ------------------------------------------------------------
WHITE = (240, 240, 245, 255)
GREY = (150, 150, 162, 255)
DGREY = (84, 84, 96, 255)
GOLD = (255, 190, 40, 255)
GOLD2 = (205, 135, 20, 255)
RED = (232, 52, 62, 255)
GREEN = (72, 214, 92, 255)
BLUE = (84, 140, 240, 255)
LBLUE = (130, 180, 255, 255)
BROWN = (146, 92, 52, 255)
CYAN = (96, 224, 244, 255)
SKIN = (242, 204, 164, 255)
BLACK = (16, 16, 20, 255)
OUTLINE = (12, 12, 16, 255)

ORANGE = (240, 160, 32, 255)
PANEL = (21, 21, 27, 255)

# id, r, g, b  (gleich wie ClanColor.java)
COLORS = [
    ("red", 255, 85, 85), ("darkred", 170, 0, 0), ("orange", 255, 170, 0),
    ("yellow", 255, 255, 85), ("green", 85, 255, 85), ("darkgreen", 0, 170, 0),
    ("aqua", 85, 255, 255), ("darkaqua", 0, 170, 170), ("blue", 85, 85, 255),
    ("darkblue", 0, 0, 170), ("pink", 255, 85, 255), ("purple", 170, 0, 170),
    ("white", 255, 255, 255), ("gray", 170, 170, 170), ("darkgray", 85, 85, 85),
]

files = {}  # pfad -> bytes


def put_png(path, img):
    b = io.BytesIO()
    img.save(b, "PNG")
    files[path] = b.getvalue()


def put_json(path, obj):
    files[path] = json.dumps(obj, indent=2).encode("utf-8")


def clamp(v):
    return max(0, min(255, int(v)))


def shade(c, f):
    return (clamp(c[0] * f), clamp(c[1] * f), clamp(c[2] * f), 255)


def new():
    return Image.new("RGBA", (S, S), (0, 0, 0, 0))


def outline(img):
    """Dunkler 1px-Rand um die gezeichnete Form."""
    src = img.load()
    out = img.copy()
    o = out.load()
    for y in range(S):
        for x in range(S):
            if src[x, y][3] != 0:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < S and 0 <= ny < S and src[nx, ny][3] != 0:
                    o[x, y] = OUTLINE
                    break
    return out


# --- Icons (gezeichnet im Bereich 3..12) --------------------------------
def i_overview(d):
    d.polygon([(3, 3), (12, 3), (12, 8), (8, 13), (7, 13), (3, 8)], fill=BLUE)
    d.polygon([(4, 4), (7, 4), (7, 11), (4, 8)], fill=LBLUE)
    d.rectangle((7, 6, 8, 7), fill=GOLD)


def i_members(d):
    d.ellipse((3, 3, 7, 7), fill=SKIN)
    d.rectangle((2, 8, 8, 12), fill=BLUE)
    d.ellipse((9, 4, 13, 8), fill=shade(SKIN, 0.85))
    d.rectangle((9, 9, 13, 12), fill=shade(BLUE, 0.7))


def i_allies(d):
    d.ellipse((4, 3, 11, 10), fill=GOLD)
    d.rectangle((3, 9, 12, 11), fill=GOLD)
    d.rectangle((7, 2, 8, 3), fill=GOLD2)
    d.rectangle((7, 12, 8, 13), fill=GOLD2)


def i_color(d):
    d.ellipse((2, 3, 13, 12), fill=(196, 138, 78, 255))
    d.rectangle((4, 5, 5, 6), fill=RED)
    d.rectangle((7, 4, 8, 5), fill=GREEN)
    d.rectangle((10, 5, 11, 6), fill=BLUE)
    d.rectangle((10, 8, 11, 9), fill=GOLD)


def i_clans(d):
    d.rectangle((3, 3, 12, 12), fill=(158, 62, 52, 255))
    d.rectangle((5, 4, 11, 11), fill=WHITE)
    d.line((6, 6, 10, 6), fill=GREY)
    d.line((6, 8, 10, 8), fill=GREY)
    d.line((6, 10, 9, 10), fill=GREY)


def i_invites(d):
    d.rectangle((2, 4, 13, 11), fill=WHITE)
    d.line((2, 4, 7, 8), fill=GREY)
    d.line((13, 4, 8, 8), fill=GREY)
    d.rectangle((11, 2, 12, 5), fill=GREEN)
    d.rectangle((10, 3, 13, 4), fill=GREEN)


def i_home(d):
    d.rectangle((4, 3, 5, 13), fill=BROWN)
    d.polygon([(6, 3), (13, 5), (6, 8)], fill=RED)


def i_plus(d):
    d.rectangle((7, 3, 8, 12), fill=GREEN)
    d.rectangle((3, 7, 12, 8), fill=GREEN)


def i_check(d):
    d.line((3, 8, 6, 11), fill=GREEN, width=2)
    d.line((6, 11, 12, 4), fill=GREEN, width=2)


def i_cross(d):
    d.line((4, 4, 11, 11), fill=RED, width=2)
    d.line((11, 4, 4, 11), fill=RED, width=2)


def i_kick(d):
    d.ellipse((3, 3, 8, 8), fill=SKIN)
    d.rectangle((2, 9, 8, 12), fill=BLUE)
    d.line((10, 9, 13, 12), fill=RED, width=2)
    d.line((13, 9, 10, 12), fill=RED, width=2)


def i_crown(d):
    d.polygon([(3, 10), (3, 5), (6, 8), (8, 4), (10, 8), (13, 5), (13, 10)], fill=GOLD)
    d.rectangle((3, 10, 13, 12), fill=GOLD2)
    d.point((8, 7), fill=RED)


def i_chat(d):
    d.rectangle((3, 3, 12, 9), fill=WHITE)
    d.polygon([(5, 9), (5, 12), (8, 9)], fill=WHITE)
    for x in (5, 8, 10):
        d.point((x, 6), fill=DGREY)


def i_invite(d):
    d.ellipse((3, 3, 7, 7), fill=SKIN)
    d.rectangle((2, 8, 8, 12), fill=BLUE)
    d.rectangle((11, 6, 12, 12), fill=GREEN)
    d.rectangle((9, 8, 14, 9), fill=GREEN)


def i_tnt(d):
    d.rectangle((3, 4, 12, 12), fill=RED)
    d.rectangle((3, 7, 12, 9), fill=WHITE)
    d.rectangle((7, 2, 8, 4), fill=BROWN)


def i_door(d):
    d.rectangle((3, 3, 9, 13), fill=BROWN)
    d.point((8, 8), fill=GOLD)
    d.line((9, 8, 12, 8), fill=GOLD)
    d.polygon([(11, 6), (14, 8), (11, 10)], fill=GOLD)


def i_up(d):
    d.polygon([(8, 3), (13, 9), (10, 9), (10, 13), (6, 13), (6, 9), (3, 9)], fill=WHITE)


def i_down(d):
    d.polygon([(8, 13), (13, 7), (10, 7), (10, 3), (6, 3), (6, 7), (3, 7)], fill=WHITE)


def i_left(d):
    d.polygon([(3, 8), (9, 3), (9, 6), (13, 6), (13, 10), (9, 10), (9, 13)], fill=WHITE)


def i_reset(d):
    d.ellipse((3, 3, 12, 12), outline=RED, width=2)
    d.line((4, 4, 11, 11), fill=RED, width=2)


def i_hex(d):
    d.line((6, 3, 5, 12), fill=WHITE, width=1)
    d.line((10, 3, 9, 12), fill=WHITE, width=1)
    d.line((3, 6, 12, 6), fill=WHITE, width=1)
    d.line((3, 9, 12, 9), fill=WHITE, width=1)


def i_info(d):
    d.ellipse((3, 3, 12, 12), fill=BLUE)
    d.rectangle((7, 7, 8, 11), fill=WHITE)
    d.rectangle((7, 4, 8, 5), fill=WHITE)


def i_sword(d):
    d.line((5, 11, 12, 4), fill=CYAN, width=2)
    d.line((3, 9, 7, 13), fill=GOLD2, width=2)
    d.line((3, 13, 5, 11), fill=BROWN, width=2)


def i_skull(d):
    d.ellipse((3, 3, 12, 10), fill=WHITE)
    d.rectangle((5, 10, 10, 12), fill=WHITE)
    d.rectangle((5, 6, 6, 7), fill=BLACK)
    d.rectangle((9, 6, 10, 7), fill=BLACK)


def i_kd(d):
    d.rectangle((3, 9, 5, 12), fill=GOLD)
    d.rectangle((7, 6, 9, 12), fill=GREEN)
    d.rectangle((11, 3, 13, 12), fill=CYAN)


def i_pickaxe(d):
    d.line((4, 12, 11, 5), fill=BROWN, width=2)
    d.line((3, 5, 8, 3), fill=GREY, width=2)
    d.line((8, 3, 12, 7), fill=GREY, width=2)


def i_close(d):
    d.line((4, 4, 11, 11), fill=WHITE, width=2)
    d.line((11, 4, 4, 11), fill=WHITE, width=2)


def i_lock(d):
    d.rectangle((4, 7, 11, 12), fill=GREY)
    d.arc((5, 2, 10, 9), 180, 360, fill=GREY, width=2)
    d.point((7, 9), fill=BLACK)


def i_preview(d):
    d.polygon([(3, 5), (10, 5), (13, 8), (10, 11), (3, 11)], fill=WHITE)
    d.rectangle((4, 7, 5, 8), fill=GREY)


def i_logo(d):
    i_crown(d)


def i_bold(d):
    d.rectangle((4, 3, 6, 12), fill=WHITE)
    d.rectangle((4, 3, 10, 5), fill=WHITE)
    d.rectangle((4, 7, 10, 8), fill=WHITE)
    d.rectangle((4, 10, 10, 12), fill=WHITE)
    d.rectangle((9, 4, 11, 7), fill=WHITE)
    d.rectangle((9, 8, 12, 11), fill=WHITE)


def i_italic(d):
    d.line((9, 3, 6, 12), fill=WHITE, width=2)
    d.line((7, 3, 12, 3), fill=WHITE, width=1)
    d.line((3, 12, 8, 12), fill=WHITE, width=1)


def i_gradient(d):
    stops = [(255, 190, 40), (255, 84, 84), (255, 85, 255), (85, 140, 255)]
    for x in range(3, 13):
        t = (x - 3) / 9 * (len(stops) - 1)
        i = min(int(t), len(stops) - 2)
        f = t - i
        c = tuple(int(stops[i][k] + (stops[i + 1][k] - stops[i][k]) * f) for k in range(3)) + (255,)
        d.rectangle((x, 4, x, 11), fill=c)
    d.rectangle((3, 4, 12, 5), fill=(255, 255, 255, 90))


# name -> (zeichenfunktion, stil)
ICONS = {
    "overview": (i_overview, "card"), "members": (i_members, "card"), "allies": (i_allies, "card"),
    "color": (i_color, "card"), "clans": (i_clans, "card"), "invites": (i_invites, "card"),
    "home": (i_home, "card"), "plus": (i_plus, "card"), "check": (i_check, "card"),
    "cross": (i_cross, "card"), "kick": (i_kick, "card"), "crown": (i_crown, "card"),
    "chat": (i_chat, "card"), "invite": (i_invite, "card"), "tnt": (i_tnt, "card"),
    "door": (i_door, "card"), "arrow_up": (i_up, "card"), "arrow_down": (i_down, "card"),
    "arrow_left": (i_left, "card"), "reset": (i_reset, "card"), "hex": (i_hex, "card"),
    "info": (i_info, "card"), "sword": (i_sword, "card"), "skull": (i_skull, "card"),
    "kd": (i_kd, "card"), "pickaxe": (i_pickaxe, "card"), "close": (i_close, "red"),
    "lock": (i_lock, "card"), "preview": (i_preview, "card"), "logo": (i_logo, "header"),
    "bold": (i_bold, "card"), "italic": (i_italic, "card"), "gradient": (i_gradient, "card"),
}
TAB_ICONS = ["overview", "members", "allies", "color", "clans", "invites", "home"]


# --- Hochauflösende 3D-Kacheln --------------------------------------------
K = 4            # Texturauflösung: 1 Slot = 64x64 px
T = S * K
FONT_PATH = os.path.join(os.path.dirname(os.path.abspath(__file__)), "Silkscreen-Bold.ttf")  # OFL-Lizenz

C_GREEN = (96, 232, 84, 255)
C_GOLD = (255, 200, 48, 255)
C_RED = (255, 84, 84, 255)
C_AQUA = (96, 228, 255, 255)
C_WHITE = (240, 240, 245, 255)
C_GRAY = (160, 162, 170, 255)


def font(size):
    return ImageFont.truetype(FONT_PATH, size)


def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3)) + (255,)


def epx(img):
    """Scale2x: verdoppelt die Pixel-Art-Auflösung mit sauberen Diagonalen."""
    w, h = img.size
    src = img.load()
    out = Image.new("RGBA", (w * 2, h * 2), (0, 0, 0, 0))
    o = out.load()

    def g(x, y, d):
        return src[x, y] if 0 <= x < w and 0 <= y < h else d

    for y in range(h):
        for x in range(w):
            p = src[x, y]
            a, b, c, dd = g(x, y - 1, (0, 0, 0, 0)), g(x + 1, y, (0, 0, 0, 0)), g(x - 1, y, (0, 0, 0, 0)), g(x, y + 1, (0, 0, 0, 0))
            p1 = p2 = p3 = p4 = p
            if c == a and c != dd and a != b:
                p1 = a
            if a == b and a != c and b != dd:
                p2 = b
            if dd == c and dd != b and c != a:
                p3 = c
            if b == dd and b != a and dd != c:
                p4 = dd
            o[2 * x, 2 * y], o[2 * x + 1, 2 * y], o[2 * x, 2 * y + 1], o[2 * x + 1, 2 * y + 1] = p1, p2, p3, p4
    return out


def bevel_art(art):
    """3D-Licht: helle Kanten oben/links, dunkle unten/rechts, sanfter Verlauf von oben nach unten."""
    n = art.size[0]
    src = art.load()
    out = art.copy()
    o = out.load()

    def empty(x, y):
        return not (0 <= x < n and 0 <= y < n) or src[x, y][3] == 0

    for y in range(n):
        for x in range(n):
            p = src[x, y]
            if p[3] == 0:
                continue
            f = 1.14 - 0.26 * (y / n)
            if empty(x, y - 1):
                f *= 1.38
            if empty(x - 1, y):
                f *= 1.18
            if empty(x, y + 1):
                f *= 0.66
            if empty(x + 1, y):
                f *= 0.80
            o[x, y] = (clamp(p[0] * f), clamp(p[1] * f), clamp(p[2] * f), 255)
    return out


def outline_n(img, col=(8, 8, 12, 255)):
    n = img.size[0]
    src = img.load()
    out = img.copy()
    o = out.load()
    for y in range(n):
        for x in range(n):
            if src[x, y][3] != 0:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < n and 0 <= ny < n and src[nx, ny][3] != 0:
                    o[x, y] = col
                    break
    return out


def icon_img(draw_fn, size):
    """Icon als 3D-Pixelart (32er Raster) mit Kantenlicht und weichem Schlagschatten."""
    art = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    draw_fn(ImageDraw.Draw(art))
    art = epx(art)
    art = bevel_art(art)
    art = outline_n(art)
    big = art.resize((size, size), Image.NEAREST)
    alpha = big.split()[3].point(lambda a: 150 if a else 0)
    shadow = Image.new("RGBA", big.size, (0, 0, 0, 0))
    shadow.putalpha(alpha)
    shadow = shadow.filter(ImageFilter.GaussianBlur(size / 36))
    out = Image.new("RGBA", big.size, (0, 0, 0, 0))
    out.alpha_composite(shadow, (size // 40, size // 22))
    out.alpha_composite(big)
    return out


def card_base(w, h, sel=False, b=None):
    """Dunkle Karte mit Verlauf, abgesetzter Kante und 3D-Rahmen (hell oben/links, dunkel unten/rechts)."""
    b = b or (4 if w >= 128 else 3)
    img = Image.new("RGBA", (w, h), PANEL)
    d = ImageDraw.Draw(img)
    edge = (255, 196, 52, 255) if sel else (10, 10, 12, 255)
    d.rectangle((0, 0, w - 1, h - 1), fill=edge)
    top, bot = ((62, 56, 40), (40, 36, 26)) if sel else ((46, 48, 52), (30, 31, 34))
    for y in range(b, h - b):
        d.line((b, y, w - 1 - b, y), fill=lerp(top, bot, (y - b) / max(1, h - 2 * b - 1)))
    hi = (255, 226, 140, 255) if sel else (86, 90, 98, 255)
    lo = (150, 100, 20, 255) if sel else (16, 17, 19, 255)
    d.rectangle((b, b, w - 1 - b, b + 1), fill=hi)           # Licht oben
    d.rectangle((b, b, b + 1, h - 1 - b), fill=hi)           # Licht links
    d.rectangle((b, h - b - 2, w - 1 - b, h - 1 - b), fill=lo)   # Schatten unten
    d.rectangle((w - b - 2, b, w - 1 - b, h - 1 - b), fill=lo)   # Schatten rechts
    for (x, y) in ((0, 0), (w - 1, 0), (0, h - 1), (w - 1, h - 1)):  # abgerundete Ecken
        sx, sy = (1 if x == 0 else -1), (1 if y == 0 else -1)
        for i in range(b):
            for j in range(b - i):
                img.putpixel((x + sx * i, y + sy * j), PANEL)
    return img


def text_c(img, txt, cx, y, size, fill, maxw, min_size=8):
    while size > min_size and font(size).getlength(txt) > maxw:
        size -= 1
    f = font(size)
    d = ImageDraw.Draw(img)
    d.text((cx + 1, y + 1), txt, font=f, fill=(0, 0, 0, 255), anchor="mt")
    d.text((cx, y), txt, font=f, fill=fill, anchor="mt")


def paste_center(card, ic, cx, cy):
    card.paste(ic, (int(cx - ic.size[0] / 2), int(cy - ic.size[1] / 2)), ic)


def tile_png(name, img):
    put_png(f"assets/{NS}/textures/item/{name}.png", img)


def model(name):
    # 1 Slot = 16 GUI-Pixel -> 18: Kachel füllt die komplette Slot-Zelle, keine grauen Fugen
    put_json(f"assets/{NS}/models/item/{name}.json", {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": f"{NS}:item/{name}"},
        "display": {"gui": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [1.14, 1.14, 1.14]}},
    })


def item_def(name, conditional):
    if conditional:
        d = {"model": {
            "type": "minecraft:condition",
            "property": "minecraft:custom_model_data",
            "index": 0,
            "on_true": {"type": "minecraft:model", "model": f"{NS}:item/{name}_sel"},
            "on_false": {"type": "minecraft:model", "model": f"{NS}:item/{name}"},
        }}
    else:
        d = {"model": {"type": "minecraft:model", "model": f"{NS}:item/{name}"}}
    d["oversized_in_gui"] = True  # sonst schneidet Minecraft die Kachel auf 16px ab -> Fugen zwischen den Slots
    put_json(f"assets/{NS}/items/{name}.json", d)


def emit_pair(name, make):
    """make(sel) -> Bild; erzeugt name + name_sel samt Modellen und Item-Definition."""
    tile_png(name, make(False))
    tile_png(name + "_sel", make(True))
    model(name)
    model(name + "_sel")
    item_def(name, True)


def emit_plain(name, img):
    tile_png(name, img)
    model(name)
    item_def(name, False)


def solid(c, n=T):
    return Image.new("RGBA", (n, n), c)


def split_emit(base, img, cols, rows):
    """Schneidet ein Bild in Slot-Kacheln: base_<r><c>(+_sel wird vom Aufrufer als eigenes Bild geliefert)."""
    raise NotImplementedError


# --- Beschriftungen auf den Karten (statisch, wie im Vorbild) ---------------
BIG_LABELS = {
    "plus": ("GRÜNDEN", "NEUER CLAN", C_GREEN), "invites": ("EINLADUNGEN", "ANSEHEN", C_GOLD),
    "clans": ("ALLE CLANS", "SERVER", C_AQUA), "info": ("HINWEIS", "", C_GOLD),
    "sword": ("KILLS", "STATISTIK", C_GREEN), "skull": ("TODE", "STATISTIK", C_RED),
    "kd": ("K/D", "STATISTIK", C_GOLD), "pickaxe": ("BLÖCKE", "STATISTIK", C_AQUA),
    "chat": ("CLAN-CHAT", "NACHRICHT", C_AQUA), "invite": ("EINLADEN", "SPIELER", C_GREEN),
    "tnt": ("AUFLÖSEN", "CLAN", C_RED), "lock": ("GESPERRT", "", C_GRAY),
    "door": ("VERLASSEN", "CLAN", C_RED), "check": ("JA", "BESTÄTIGEN", C_GREEN),
    "cross": ("NEIN", "ABBRECHEN", C_RED), "color": ("CLANFARBE", "FARBVERLAUF", C_GOLD),
}
TAB_LABELS = {
    "overview": "ÜBERSICHT", "members": "MITGLIEDER", "allies": "ALLIANZEN",
    "clans": "ALLE CLANS", "home": "START", "invites": "EINLADUNGEN",
}


def big_card(draw_fn, label, sel):
    img = card_base(128, 128, sel)
    ic = icon_img(draw_fn, 100)
    has_text = label is not None and label[0]
    paste_center(img, ic, 64, 44 if has_text else 62)
    if has_text:
        text_c(img, label[0], 64, 90, 16, label[2], 112, 9)
        if label[1]:
            text_c(img, label[1], 64, 110, 10, C_GRAY, 112, 8)
    return img


def emit_big(name, draw_fn, label):
    for sel in (False, True):
        img = big_card(draw_fn, label, sel)
        for r in (0, 1):
            for c in (0, 1):
                tile_png(f"big_{name}_{r}{c}" + ("_sel" if sel else ""),
                         img.crop((c * T, r * T, c * T + T, r * T + T)))
    for r in (0, 1):
        for c in (0, 1):
            model(f"big_{name}_{r}{c}")
            model(f"big_{name}_{r}{c}_sel")
            item_def(f"big_{name}_{r}{c}", True)


def tall_card(draw_fn, label, sel):
    img = card_base(T, 2 * T, sel)
    paste_center(img, icon_img(draw_fn, 64), 32, 46)
    text_c(img, label[0], 32, 96, 13, label[2], 56, 8)
    return img


def emit_tall(name, draw_fn, label):
    """1 Slot breit, 2 Slots hoch (für Einzel-Hinweise exakt in der Fenstermitte)."""
    for sel in (False, True):
        img = tall_card(draw_fn, label, sel)
        for r in (0, 1):
            tile_png(f"tall_{name}_{r}" + ("_sel" if sel else ""), img.crop((0, r * T, T, r * T + T)))
    for r in (0, 1):
        model(f"tall_{name}_{r}")
        model(f"tall_{name}_{r}_sel")
        item_def(f"tall_{name}_{r}", True)


def small_card(draw_fn, style, sel):
    img = card_base(T, T, sel)
    ic = icon_img(draw_fn, 60)
    paste_center(img, ic, 32, 31)
    return img


def tab_card(draw_fn, label, sel):
    img = card_base(128, T, sel)
    ic = icon_img(draw_fn, 48)
    paste_center(img, ic, 22, 31)
    text_c(img, label, 86, 25, 13, C_GOLD if sel else C_WHITE, 66, 8)
    return img


def emit_tab(name, draw_fn, label):
    for sel in (False, True):
        img = tab_card(draw_fn, label, sel)
        for c in (0, 1):
            tile_png(f"tab_{name}_{c}" + ("_sel" if sel else ""), img.crop((c * T, 0, c * T + T, T)))
    for c in (0, 1):
        model(f"tab_{name}_{c}")
        model(f"tab_{name}_{c}_sel")
        item_def(f"tab_{name}_{c}", True)


def banner_art(col):
    def f(d):
        c = (col[0], col[1], col[2], 255)
        d.rectangle((3, 2, 4, 13), fill=BROWN)
        d.rectangle((5, 3, 12, 9), fill=c)
        d.polygon([(5, 10), (12, 10), (12, 12), (8, 10)], fill=shade(c, 0.8))
        d.rectangle((5, 3, 12, 4), fill=shade(c, 1.2))
        d.rectangle((5, 8, 12, 9), fill=shade(c, 0.85))
    return f


def swatch_art(col):
    def f(d):
        c = (col[0], col[1], col[2], 255)
        d.rectangle((3, 3, 12, 12), fill=c)
        d.rectangle((3, 3, 12, 4), fill=shade(c, 1.25))
        d.rectangle((3, 11, 12, 12), fill=shade(c, 0.75))
    return f


def make_container():
    """Das komplette Fenster-Panel (176x222) in 4-facher Auflösung, links oben in einem 1024x1024-Bild."""
    k = 4
    img = Image.new("RGBA", (256 * k, 256 * k), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    def R(x0, y0, x1, y1, fill):
        d.rectangle((x0 * k, y0 * k, (x1 + 1) * k - 1, (y1 + 1) * k - 1), fill=fill)

    W, H = 176, 222
    R(0, 0, W - 1, H - 1, (8, 8, 10, 255))
    R(1, 1, W - 2, H - 2, (52, 54, 60, 255))
    R(2, 2, W - 3, H - 3, PANEL)
    # Titelleiste: rot wie im Vorbild, mit Lichtkante oben und dunkler Kante unten
    for y in range(2, 17):
        t = (y - 2) / 14
        R(2, y, W - 3, y, lerp((238, 36, 54), (186, 20, 36), t))
    R(2, 2, W - 3, 2, (255, 120, 130, 255))
    R(2, 16, W - 3, 16, (110, 10, 22, 255))
    # Slot-Mulden mit Tiefe
    cells = [(7 + 18 * c, 17 + 18 * r) for r in range(6) for c in range(9)]
    cells += [(7 + 18 * c, 140 + 18 * r) for r in range(3) for c in range(9)]
    cells += [(7 + 18 * c, 198) for c in range(9)]
    for x, y in cells:
        R(x, y, x + 17, y + 17, (44, 46, 52, 255))
        R(x + 1, y + 1, x + 17, y + 17, (10, 10, 12, 255))
        R(x + 1, y + 1, x + 16, y + 16, (22, 23, 26, 255))
    for (x, y, dx, dy) in ((0, 0, 1, 1), (W - 1, 0, -1, 1), (0, H - 1, 1, -1), (W - 1, H - 1, -1, -1)):
        for ox, oy in ((0, 0), (1, 0), (0, 1)):
            R(x + dx * ox, y + dy * oy, x + dx * ox, y + dy * oy, (0, 0, 0, 0))
    return img


PANEL_COLS, PANEL_ROWS = 4, 5      # Kacheln des Panels (je 44x48 GUI-Pixel)
PANEL_W, PANEL_H = 176, 222


def panel_char(r, c):
    return chr(0xE010 + r * 0x10 + c)


def make_panel_font():
    """
    Das dunkle Fenster wird NICHT mehr als globale Container-Textur ersetzt (das färbte jede Truhe auf dem Server
    schwarz), sondern als Schrift-Grafik in den Fenstertitel gelegt. Dadurch sieht nur das Clan-Fenster so aus.
    Eine Font-Grafik darf maximal 256x256 px groß sein -> das Panel wird in 4x5 Kacheln geschnitten.
    """
    panel = make_container().crop((0, 0, PANEL_W * K, PANEL_H * K))
    tw, th = PANEL_W // PANEL_COLS * K, 48 * K           # 176x192 px pro Kachel
    providers = []
    for r in range(PANEL_ROWS):
        row = Image.new("RGBA", (tw * PANEL_COLS, th), (0, 0, 0, 0))
        part = panel.crop((0, r * th, panel.width, min(panel.height, (r + 1) * th)))
        row.paste(part, (0, 0))
        put_png(f"assets/{NS}/textures/font/panel_r{r}.png", row)
        providers.append({
            "type": "bitmap",
            "file": f"{NS}:font/panel_r{r}.png",
            "ascent": 13 - 48 * r,      # Titel liegt bei y=6, Grundlinie bei +7 -> Oberkante des Panels = Fensterrand
            "height": 48,
            "chars": ["".join(panel_char(r, c) for c in range(PANEL_COLS))],
        })
    providers.append({"type": "space", "advances": {
        "\ue001": -1,      # Kacheln liegen sonst 1px auseinander
        "\ue002": -8,      # Titelanfang (x=8) zurück an den Fensterrand
        "\ue003": -176,    # zurück an den linken Rand für die nächste Reihe
        "\ue004": -168,    # zurück auf x=8, dort beginnt der normale Titeltext
    }})
    put_json(f"assets/{NS}/font/gui.json", {"providers": providers})


def sprite(path, img, meta):
    put_png(f"assets/{path}.png", img)
    put_json(f"assets/{path}.png.mcmeta", {"gui": {"scaling": meta}})


def make_sprites():
    """Hover-Hervorhebung (Vanilla zeichnet sie beim Überfahren eines Slots) und Tooltip-Stil für die Clan-GUI."""
    # Slot-Hover: unsichtbar. Vanilla markiert nur den einzelnen Slot; bei 2x2-Karten wirkt das unsauber.
    # (Rückmeldung gibt der Tooltip und das Aufleuchten der ganzen Karte beim Klick.)
    back = Image.new("RGBA", (24, 24), (0, 0, 0, 0))
    front = Image.new("RGBA", (24, 24), (0, 0, 0, 0))
    ns = {"type": "nine_slice", "width": 24, "height": 24, "border": 4}
    sprite("minecraft/textures/gui/sprites/container/slot_highlight_back", back, ns)
    sprite("minecraft/textures/gui/sprites/container/slot_highlight_front", front, ns)
    # Tooltip (wie Vanilla: 100x100, Rand 8px transparent, Fläche ab Pixel 8, Rahmenlinie bei Pixel 9)
    bg = Image.new("RGBA", (100, 100), (0, 0, 0, 0))
    bd = ImageDraw.Draw(bg)
    bd.rectangle((8, 8, 91, 91), fill=(8, 8, 11, 245))
    bd.rectangle((8, 8, 91, 91), outline=(0, 0, 0, 255), width=1)
    sprite(f"{NS}/textures/gui/sprites/tooltip/gui_background", bg,
           {"type": "nine_slice", "width": 100, "height": 100, "border": 9})
    fr = Image.new("RGBA", (100, 100), (0, 0, 0, 0))
    for y in range(9, 91):
        t = (y - 9) / 81
        col = lerp((255, 190, 56), (220, 48, 60), t)
        fr.putpixel((9, y), col)
        fr.putpixel((90, y), col)
    for x in range(9, 91):
        fr.putpixel((x, 9), (255, 190, 56, 255))
        fr.putpixel((x, 90), (220, 48, 60, 255))
    sprite(f"{NS}/textures/gui/sprites/tooltip/gui_frame", fr,
           {"type": "nine_slice", "width": 100, "height": 100, "border": 10, "stretch_inner": True})


def build():
    # 1x1-Karten mit 3D-Icons
    for name, (fn, style) in ICONS.items():
        if name in ("close", "logo"):
            continue
        emit_pair(name, lambda sel, fn=fn, style=style: small_card(fn, style, sel))
    emit_pair("close", lambda sel: small_card(ICONS["close"][0], "card", sel))
    # Große 2x2-Karten mit Beschriftung
    for name, (fn, style) in ICONS.items():
        if name in BIG_LABELS:
            emit_big(name, fn, BIG_LABELS[name])
    emit_tall("info", ICONS["info"][0], ("HINWEIS", "", C_GOLD))
    emit_tall("lock", ICONS["lock"][0], ("GESPERRT", "", C_GRAY))
    emit_tall("plus", ICONS["plus"][0], ("ALLIANZ", "", C_AQUA))
    # Tabs (2 Slots breit, Icon + Text)
    for name, label in TAB_LABELS.items():
        emit_tab(name, ICONS[name][0], label)
    # Clanfarben: Banner (klein + groß) + Farbfelder
    for cid, r, g, b in COLORS:
        emit_pair("banner_" + cid, lambda sel, a=banner_art((r, g, b)): small_card(a, "card", sel))
        emit_pair("swatch_" + cid, lambda sel, a=swatch_art((r, g, b)): small_card(a, "card", sel))
        emit_big("banner_" + cid, banner_art((r, g, b)), ("CLAN", "INFO", C_GOLD))

    # Hintergrund-Kacheln (statisch, passend zum Panel der Container-Textur)
    emit_plain("bg", solid(PANEL))
    bar = solid((22, 22, 28, 255))
    ImageDraw.Draw(bar).rectangle((0, T - 3, T - 1, T - 1), fill=(46, 48, 56, 255))
    emit_plain("bar", bar)
    foot = solid((14, 14, 18, 255))
    ImageDraw.Draw(foot).rectangle((0, 0, T - 1, 2), fill=(46, 48, 56, 255))
    emit_plain("foot", foot)
    emit_plain("header", solid((226, 30, 45, 255)))

    track = solid(PANEL)
    td = ImageDraw.Draw(track)
    td.rectangle((26, 0, 37, T - 1), fill=(10, 10, 12, 255))
    td.rectangle((28, 0, 35, T - 1), fill=(34, 35, 40, 255))
    emit_plain("scroll_track", track)
    handle = solid(PANEL)
    hd = ImageDraw.Draw(handle)
    hd.rectangle((22, 3, 41, T - 4), fill=(8, 8, 10, 255))
    hd.rectangle((24, 5, 39, T - 6), fill=(236, 160, 24, 255))
    hd.rectangle((24, 5, 39, 9), fill=(255, 222, 130, 255))
    hd.rectangle((24, T - 10, 39, T - 6), fill=(168, 104, 12, 255))
    emit_plain("scroll_handle", handle)

    make_panel_font()
    make_sprites()

    put_json("pack.mcmeta", {"pack": {
        "description": "Clans GUI",
        "pack_format": 46,
        "supported_formats": {"min_inclusive": 46, "max_inclusive": 99},
        "min_format": 46,
        "max_format": 99,
    }})
    logo = Image.new("RGBA", (64, 64), (226, 30, 45, 255))
    logo.paste(icon_img(i_crown, 56), (4, 4), icon_img(i_crown, 56))
    put_png("pack.png", logo)


def main():
    out = sys.argv[1] if len(sys.argv) > 1 else "ClansPack.zip"
    build()
    with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as z:
        for path in sorted(files):
            z.writestr(path, files[path])
    print(f"{len(files)} Dateien -> {out}")


if __name__ == "__main__":
    main()
