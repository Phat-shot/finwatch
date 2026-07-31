#!/usr/bin/env python3
"""Generate the Play Store assets into docs/store-assets/.

Reuses the jellyfish glyph from generate_launcher_icons.py so the store
icon and feature graphic always match the launcher icon design.

Outputs:
    docs/store-assets/icon-512.png          512x512 hi-res store icon
    docs/store-assets/feature-1024x500.png  feature graphic

Run locally when the design or wording changes:
    python3 scripts/generate_store_assets.py

Font note: expects a DejaVu/Liberation Sans Bold TTF (present on typical
Linux boxes and CI runners); pass FONT_PATH env var to override.
"""
import os
import sys

from PIL import Image, ImageDraw, ImageFont

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from generate_launcher_icons import (  # noqa: E402
    BG_COLOR,
    JELLYFIN_BLUE,
    JELLYFIN_PURPLE,
    lerp_color,
    render_glyph,
)

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "docs", "store-assets")

FONT_CANDIDATES = [
    os.environ.get("FONT_PATH"),
    "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
    "/usr/share/fonts/truetype/liberation/LiberationSans-Bold.ttf",
]
FONT_REGULAR_CANDIDATES = [
    "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
    "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf",
]


def load_font(candidates, size):
    for path in candidates:
        if path and os.path.exists(path):
            return ImageFont.truetype(path, size)
    raise SystemExit("no usable TTF found; set FONT_PATH")


def store_icon(size=512):
    """Full-bleed square icon: Play masks/rounds it on its own, so unlike
    the round legacy launcher icon this one fills the whole canvas."""
    img = Image.new("RGB", (size, size), BG_COLOR)
    glyph = render_glyph(size, size / 2, size * 0.46, size * 0.30)
    img.paste(glyph, (0, 0), glyph)
    return img


def feature_graphic(width=1024, height=500):
    # Subtle diagonal darkening so the black isn't completely flat.
    img = Image.new("RGB", (width, height), BG_COLOR)
    overlay = Image.new("L", (1, height))
    for y in range(height):
        overlay.putpixel((0, y), int(18 * y / height))
    shade = Image.new("RGB", (width, height), (255, 255, 255))
    img = Image.composite(shade, img, overlay.resize((width, height)).point(lambda v: v))

    # Glyph on the right, roughly centered vertically.
    glyph_canvas = int(height * 1.15)
    glyph = render_glyph(glyph_canvas, glyph_canvas / 2, glyph_canvas * 0.42, glyph_canvas * 0.26)
    img.paste(glyph, (width - glyph_canvas + int(height * 0.06), int((height - glyph_canvas) / 2)), glyph)

    draw = ImageDraw.Draw(img)
    title_font = load_font(FONT_CANDIDATES, 96)
    sub_font = load_font(FONT_REGULAR_CANDIDATES, 40)
    x = int(width * 0.07)

    # "Finwatch" with the brand gradient applied per-letter via a mask.
    title = "Finwatch"
    title_y = int(height * 0.33)
    mask = Image.new("L", (width, height), 0)
    ImageDraw.Draw(mask).text((x, title_y), title, font=title_font, fill=255)
    gradient = Image.new("RGB", (width, height))
    grad_draw = ImageDraw.Draw(gradient)
    bbox = draw.textbbox((x, title_y), title, font=title_font)
    for gx in range(bbox[0], bbox[2] + 1):
        t = (gx - bbox[0]) / max(1, bbox[2] - bbox[0])
        grad_draw.line([(gx, bbox[1]), (gx, bbox[3])], fill=lerp_color(JELLYFIN_BLUE, JELLYFIN_PURPLE, t))
    img.paste(gradient, (0, 0), mask)

    draw.text((x, title_y + 118), "Jellyfin client for Wear OS", font=sub_font, fill=(232, 232, 232))
    draw.text((x, title_y + 172), "unofficial · open source", font=sub_font, fill=(150, 150, 150))
    return img


def main():
    os.makedirs(OUT, exist_ok=True)
    store_icon().save(os.path.join(OUT, "icon-512.png"))
    feature_graphic().save(os.path.join(OUT, "feature-1024x500.png"))
    print(f"wrote {OUT}/icon-512.png and feature-1024x500.png")


if __name__ == "__main__":
    main()
