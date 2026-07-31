#!/usr/bin/env python3
"""Generate the Finwatch launcher icon (legacy + adaptive) into app/src/main/res.

The glyph is the Finwatch mascot: a shark fin wearing a round smartwatch
as a pirate eye patch, the watch screen showing a comic eye. Drawn in
Jellyfin's brand gradient colors (explicitly permitted by their branding
policy; the mark itself is our own design).

Run once locally when the icon design changes:
    python3 scripts/generate_launcher_icons.py
"""
import math
import os

from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "app", "src", "main", "res")

# density -> legacy launcher png size (48dp baseline)
DENSITIES = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192,
}

# Adaptive icon foreground/background layers are exported at a fixed 432x432
# "full bleed" canvas (108dp at xxxhdpi), with content kept inside the
# center ~66dp safe zone.
ADAPTIVE_SIZE = 432

# Supersampling factor: PIL's ImageDraw has no antialiasing, so everything is
# rendered at this multiple and downsampled with LANCZOS at the end.
SUPERSAMPLE = 4

BG_COLOR = (0x00, 0x00, 0x00)  # matches the app's own black background

# Jellyfin's own brand colors (see presentation/theme/Color.kt -- kept in
# sync with JellyfinBlue/JellyfinPurple there).
JELLYFIN_BLUE = (0x00, 0xA4, 0xDC)
JELLYFIN_PURPLE = (0xAA, 0x5C, 0xC3)

STRAP_COLOR = (24, 24, 30)
CASE_COLOR = (225, 225, 232)
SCREEN_COLOR = (10, 10, 14)
SCLERA_COLOR = (245, 245, 248)
PUPIL_COLOR = (8, 8, 10)

# Strap endpoints and the watch's position along it, in the mascot's unit
# square. The watch center sits exactly on the strap line.
STRAP_P1 = (0.02, 0.58)
STRAP_P2 = (0.90, 0.16)
WATCH_T = 0.48
WATCH_RADIUS = 0.135  # of unit width
PUPIL_DIR = (-0.35, -0.25)  # comic eye glancing up-left


def lerp_color(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def bezier(p0, p1, p2, p3, steps=60):
    pts = []
    for s in range(steps + 1):
        t = s / steps
        mt = 1 - t
        pts.append((
            mt**3 * p0[0] + 3 * mt**2 * t * p1[0] + 3 * mt * t**2 * p2[0] + t**3 * p3[0],
            mt**3 * p0[1] + 3 * mt**2 * t * p1[1] + 3 * mt * t**2 * p2[1] + t**3 * p3[1],
        ))
    return pts


def fin_outline(scale_x, scale_y):
    """Shark-fin silhouette, unit coords scaled by the given factors."""
    def P(x, y):
        return (x * scale_x, y * scale_y)
    pts = []
    # leading (left) edge: bottom-left up to the tip, bulging left
    pts += bezier(P(0.20, 0.93), P(0.10, 0.62), P(0.24, 0.28), P(0.58, 0.10))
    # tip roundover
    pts += bezier(P(0.58, 0.10), P(0.66, 0.06), P(0.70, 0.10), P(0.70, 0.16))
    # trailing (right) edge: concave down to bottom-right
    pts += bezier(P(0.70, 0.16), P(0.62, 0.42), P(0.68, 0.68), P(0.86, 0.93))
    # bottom: gentle curve back to start
    pts += bezier(P(0.86, 0.93), P(0.64, 0.99), P(0.42, 0.99), P(0.20, 0.93))
    return pts


def vertical_gradient(size, top_color, bottom_color):
    """A size x size RGB image fading top_color -> bottom_color."""
    column = Image.new("RGB", (1, size))
    for y in range(size):
        column.putpixel((0, y), lerp_color(top_color, bottom_color, y / max(1, size - 1)))
    return column.resize((size, size))


def render_glyph(canvas_size: int, cx: float, cy: float, r: float) -> Image.Image:
    """The fin-with-watch mascot on a transparent canvas_size square,
    scaled so the whole mark fits a circle of radius ~1.15*r around
    (cx, cy) -- same contract the old jellyfish glyph had."""
    hi = canvas_size * SUPERSAMPLE
    # The mascot's unit square maps to a box of side S centered on the
    # fin's visual center (0.5, 0.525).
    S = 1.95 * r * SUPERSAMPLE
    ox = cx * SUPERSAMPLE - 0.5 * S
    oy = cy * SUPERSAMPLE - 0.525 * S

    def U(x, y):
        return (ox + x * S, oy + y * S)

    glyph = Image.new("RGBA", (hi, hi), (0, 0, 0, 0))

    # fin: gradient clipped to silhouette
    mask = Image.new("L", (hi, hi), 0)
    ImageDraw.Draw(mask).polygon(
        [(ox + px * S, oy + py * S) for (px, py) in fin_outline(1, 1)], fill=255)
    gradient = Image.new("RGBA", (hi, hi), (0, 0, 0, 0))
    grad_sq = vertical_gradient(int(S) or 1, JELLYFIN_BLUE, JELLYFIN_PURPLE)
    gradient.paste(grad_sq, (int(ox), int(oy)))
    glyph.paste(gradient, (0, 0), mask)

    d = ImageDraw.Draw(glyph)

    # strap, clipped to the fin
    strap = Image.new("L", (hi, hi), 0)
    ImageDraw.Draw(strap).line([U(*STRAP_P1), U(*STRAP_P2)], fill=255, width=int(0.058 * S))
    band_mask = Image.composite(strap, Image.new("L", (hi, hi), 0), mask)
    band_layer = Image.new("RGBA", (hi, hi), STRAP_COLOR + (255,))
    glyph.paste(band_layer, (0, 0), band_mask)

    # watch center on the strap line
    wx = STRAP_P1[0] + (STRAP_P2[0] - STRAP_P1[0]) * WATCH_T
    wy = STRAP_P1[1] + (STRAP_P2[1] - STRAP_P1[1]) * WATCH_T
    cxp, cyp = U(wx, wy)
    rr = WATCH_RADIUS * S

    # side button at ~1:30, a radial pill behind the case
    ba = math.radians(-48)
    bx, by = cxp + rr * 1.06 * math.cos(ba), cyp + rr * 1.06 * math.sin(ba)
    ux, uy = math.cos(ba), math.sin(ba)
    vx, vy = -uy, ux
    L, T = 0.030 * S, 0.017 * S
    d.polygon(
        [(bx + ux * L + vx * T, by + uy * L + vy * T),
         (bx + ux * L - vx * T, by + uy * L - vy * T),
         (bx - ux * L - vx * T, by - uy * L - vy * T),
         (bx - ux * L + vx * T, by - uy * L + vy * T)],
        fill=CASE_COLOR,
    )

    # slim case, dark screen
    d.ellipse([cxp - rr, cyp - rr, cxp + rr, cyp + rr], fill=CASE_COLOR)
    rs = rr * 0.90
    d.ellipse([cxp - rs, cyp - rs, cxp + rs, cyp + rs], fill=SCREEN_COLOR)

    # comic eye on the screen
    ex = cxp + PUPIL_DIR[0] * rs * 0.14
    ey = cyp + PUPIL_DIR[1] * rs * 0.14
    re = rs * 0.60
    d.ellipse([ex - re, ey - re, ex + re, ey + re], fill=SCLERA_COLOR)
    ix = ex + PUPIL_DIR[0] * re * 0.40
    iy = ey + PUPIL_DIR[1] * re * 0.40
    ri = re * 0.52
    d.ellipse([ix - ri, iy - ri, ix + ri, iy + ri], fill=JELLYFIN_BLUE)
    rp = ri * 0.58
    d.ellipse([ix - rp, iy - rp, ix + rp, iy + rp], fill=PUPIL_COLOR)
    rg = rp * 0.42
    gx, gy = ix - rp * 0.35, iy - rp * 0.40
    d.ellipse([gx - rg, gy - rg, gx + rg, gy + rg], fill=(255, 255, 255))
    # subtle screen reflection
    d.arc([cxp - rs * 0.86, cyp - rs * 0.86, cxp + rs * 0.86, cyp + rs * 0.86],
          start=200, end=245, fill=(75, 75, 85), width=int(0.012 * S))

    return glyph.resize((canvas_size, canvas_size), Image.LANCZOS)


def render_icon(size: int, padding_ratio: float) -> Image.Image:
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    draw.ellipse([0, 0, size, size], fill=BG_COLOR)
    pad = size * padding_ratio
    cx, cy = size / 2, size * 0.48
    r = (size - 2 * pad) / 2.3
    glyph = render_glyph(size, cx, cy, r)
    img = Image.alpha_composite(img, glyph)
    return img


def render_foreground(size: int) -> Image.Image:
    """Transparent background, glyph only, sized for the adaptive safe zone."""
    cx, cy = size / 2, size * 0.50
    r = size * 0.185
    return render_glyph(size, cx, cy, r)


def main():
    for density, size in DENSITIES.items():
        out_dir = os.path.join(RES, f"mipmap-{density}")
        os.makedirs(out_dir, exist_ok=True)
        icon = render_icon(size, padding_ratio=0.08)
        icon.save(os.path.join(out_dir, "ic_launcher.png"))

        round_icon = render_icon(size, padding_ratio=0.08)
        round_icon.save(os.path.join(out_dir, "ic_launcher_round.png"))

    # adaptive icon layers (xxxhdpi-equivalent canvas, referenced by the
    # anydpi-v26 XML so Android scales them per-density itself)
    adaptive_dir = os.path.join(RES, "mipmap-xxxhdpi")
    os.makedirs(adaptive_dir, exist_ok=True)

    bg = Image.new("RGBA", (ADAPTIVE_SIZE, ADAPTIVE_SIZE), (0, 0, 0, 0))
    ImageDraw.Draw(bg).rectangle([0, 0, ADAPTIVE_SIZE, ADAPTIVE_SIZE], fill=BG_COLOR)
    bg.save(os.path.join(adaptive_dir, "ic_launcher_background.png"))

    fg = render_foreground(ADAPTIVE_SIZE)
    fg.save(os.path.join(adaptive_dir, "ic_launcher_foreground.png"))

    print("Launcher icons written to", RES)


if __name__ == "__main__":
    main()
