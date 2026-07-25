#!/usr/bin/env python3
"""Generate the jellywear launcher icon (legacy + adaptive) into app/src/main/res.

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


def lerp_color(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def vertical_gradient(size, top_color, bottom_color):
    """A size x size RGB image fading top_color -> bottom_color."""
    column = Image.new("RGB", (1, size))
    for y in range(size):
        column.putpixel((0, y), lerp_color(top_color, bottom_color, y / max(1, size - 1)))
    return column.resize((size, size))


def draw_jelly_silhouette(draw: ImageDraw.ImageDraw, cx: float, cy: float, r: float):
    """A jellyfish glyph (dome + trailing tentacles) in solid white, meant to
    be used as an alpha mask for a gradient fill -- see render_glyph()."""
    # Dome: a smooth half-ellipse rather than a flat pieslice, for a softer
    # silhouette that reads more like Jellyfin's own rounded mark.
    draw.ellipse([cx - r, cy - r * 0.85, cx + r, cy + r * 0.55], fill=255)
    draw.rectangle([cx - r, cy - r * 0.15, cx + r, cy + r * 0.55], fill=255)

    # Tentacles: tapered, wavy strokes fading out towards their tips, drawn
    # as overlapping circles along a sine path so the width can shrink
    # smoothly (a single draw.line() call can't taper).
    tentacle_count = 5
    span = r * 1.5
    start_x = cx - span / 2
    gap = span / (tentacle_count - 1)
    for i in range(tentacle_count):
        tx = start_x + i * gap
        length = r * 1.5
        amplitude = r * 0.14 * (1 if i % 2 == 0 else -1) * (0.6 + 0.4 * abs(i - 2) / 2)
        segments = 28
        base_width = r * 0.16
        for s in range(segments + 1):
            t = s / segments
            y = cy + r * 0.35 + t * length
            x = tx + amplitude * math.sin(t * math.pi * 2.3 + i)
            width = base_width * (1 - t) ** 1.3
            if width < 0.6:
                continue
            draw.ellipse([x - width / 2, y - width / 2, x + width / 2, y + width / 2], fill=255)


def render_glyph(canvas_size: int, cx: float, cy: float, r: float) -> Image.Image:
    """Blue-to-purple gradient jellyfish, clipped to the silhouette mask and
    supersampled for smooth (antialiased) edges."""
    hi = canvas_size * SUPERSAMPLE
    mask = Image.new("L", (hi, hi), 0)
    draw_jelly_silhouette(ImageDraw.Draw(mask), cx * SUPERSAMPLE, cy * SUPERSAMPLE, r * SUPERSAMPLE)

    gradient = vertical_gradient(hi, JELLYFIN_BLUE, JELLYFIN_PURPLE)
    glyph = Image.new("RGBA", (hi, hi), (0, 0, 0, 0))
    glyph.paste(gradient, (0, 0), mask)

    # Soft highlight on the dome for a bit of shine/depth.
    highlight = Image.new("L", (hi, hi), 0)
    hd = ImageDraw.Draw(highlight)
    hr = r * SUPERSAMPLE
    hd.ellipse(
        [
            (cx - hr * 0.5) * SUPERSAMPLE,
            (cy - hr * 0.55) * SUPERSAMPLE,
            (cx - hr * 0.05) * SUPERSAMPLE,
            (cy - hr * 0.1) * SUPERSAMPLE,
        ],
        fill=90,
    )
    white_layer = Image.new("RGBA", (hi, hi), (255, 255, 255, 0))
    white_layer.putalpha(Image.composite(highlight, Image.new("L", (hi, hi), 0), mask))
    glyph = Image.alpha_composite(glyph, white_layer)

    return glyph.resize((canvas_size, canvas_size), Image.LANCZOS)


def render_icon(size: int, padding_ratio: float) -> Image.Image:
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    draw.ellipse([0, 0, size, size], fill=BG_COLOR)
    pad = size * padding_ratio
    cx, cy = size / 2, size * 0.46
    r = (size - 2 * pad) / 2.3
    glyph = render_glyph(size, cx, cy, r)
    img = Image.alpha_composite(img, glyph)
    return img


def render_foreground(size: int) -> Image.Image:
    """Transparent background, glyph only, sized for the adaptive safe zone."""
    cx, cy = size / 2, size * 0.48
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
