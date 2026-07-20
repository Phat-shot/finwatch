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

BG_COLOR = (0x1B, 0x1F, 0x3B)  # deep indigo
JELLY_COLOR = (0x8B, 0x6C, 0xFF)  # jellyfin-esque purple
JELLY_HILIGHT = (0xB9, 0xA7, 0xFF)


def draw_jelly(draw: ImageDraw.ImageDraw, cx: float, cy: float, r: float):
    """A simple jellyfish glyph: a dome + wavy tentacles."""
    # dome
    draw.pieslice(
        [cx - r, cy - r, cx + r, cy + r * 0.4],
        180,
        360,
        fill=JELLY_COLOR,
    )
    draw.ellipse(
        [cx - r * 0.55, cy - r * 0.55, cx - r * 0.1, cy - r * 0.05],
        fill=JELLY_HILIGHT,
    )
    # tentacles
    tentacle_count = 5
    span = r * 1.7
    start_x = cx - span / 2
    gap = span / (tentacle_count - 1)
    for i in range(tentacle_count):
        tx = start_x + i * gap
        points = []
        length = r * 1.15
        segments = 10
        amplitude = r * 0.16 * (1 if i % 2 == 0 else -1)
        for s in range(segments + 1):
            t = s / segments
            y = cy + t * length
            x = tx + amplitude * math.sin(t * math.pi * 2.2)
            points.append((x, y))
        draw.line(points, fill=JELLY_COLOR, width=max(2, int(r * 0.09)))


def render_icon(size: int, padding_ratio: float) -> Image.Image:
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    pad = size * padding_ratio
    draw.ellipse([0, 0, size, size], fill=BG_COLOR)
    cx, cy = size / 2, size * 0.42
    r = (size - 2 * pad) / 2.1
    draw_jelly(draw, cx, cy, r)
    return img


def render_foreground(size: int) -> Image.Image:
    """Transparent background, glyph only, sized for the adaptive safe zone."""
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    cx, cy = size / 2, size * 0.46
    r = size * 0.20
    draw_jelly(draw, cx, cy, r)
    return img


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
