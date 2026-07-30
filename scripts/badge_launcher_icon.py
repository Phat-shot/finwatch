#!/usr/bin/env python3
"""Stamp a "BETA" ribbon onto the Finwatch launcher icon for the test build.

Runs as a CI step (only for the `beta` flavor / `test` branch) *before*
`./gradlew assembleBetaRelease`. It copies the prod launcher icons from
app/src/main/res into app/src/beta/res, badged with a ribbon, so Gradle's
normal flavor source-set precedence (beta overrides main) picks up the
badged icon only for the beta variant. Nothing here is committed to the
repo — it's regenerated fresh on every CI run.

Usage:
    python3 scripts/badge_launcher_icon.py
"""
import os

from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MAIN_RES = os.path.join(ROOT, "app", "src", "main", "res")
BETA_RES = os.path.join(ROOT, "app", "src", "beta", "res")

RIBBON_COLOR = (255, 140, 0, 235)
RIBBON_TEXT_COLOR = (35, 18, 0, 255)

# (density dir, filenames to badge)
LEGACY_FILES = ["ic_launcher.png", "ic_launcher_round.png"]
LEGACY_DENSITIES = ["mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi"]
ADAPTIVE_DENSITY = "xxxhdpi"
ADAPTIVE_FILES = ["ic_launcher_foreground.png", "ic_launcher_background.png"]


def _load_font(size: int) -> ImageFont.ImageFont:
    try:
        return ImageFont.load_default(size=size)
    except TypeError:
        # Older Pillow without the `size` kwarg on load_default().
        return ImageFont.load_default()


def add_ribbon(img: Image.Image) -> Image.Image:
    img = img.convert("RGBA")
    size = img.width
    overlay = Image.new("RGBA", (size, size), (0, 0, 0, 0))

    ribbon_h = max(1, size // 6)
    ribbon = Image.new("RGBA", (size * 2, ribbon_h), (0, 0, 0, 0))
    draw = ImageDraw.Draw(ribbon)
    draw.rectangle([0, 0, ribbon.width, ribbon.height], fill=RIBBON_COLOR)

    font = _load_font(int(ribbon_h * 0.62))
    text = "BETA"
    bbox = draw.textbbox((0, 0), text, font=font)
    tw, th = bbox[2] - bbox[0], bbox[3] - bbox[1]
    draw.text(
        ((ribbon.width - tw) / 2, (ribbon.height - th) / 2 - bbox[1]),
        text,
        font=font,
        fill=RIBBON_TEXT_COLOR,
    )

    ribbon = ribbon.rotate(-45, expand=True, resample=Image.BICUBIC)
    rx = size // 2 - ribbon.width // 2
    ry = int(size * 0.10) - ribbon.height // 2
    overlay.alpha_composite(ribbon, dest=(rx, ry))

    return Image.alpha_composite(img, overlay)


def main():
    for density in LEGACY_DENSITIES:
        src_dir = os.path.join(MAIN_RES, f"mipmap-{density}")
        dst_dir = os.path.join(BETA_RES, f"mipmap-{density}")
        os.makedirs(dst_dir, exist_ok=True)
        for name in LEGACY_FILES:
            src = os.path.join(src_dir, name)
            if not os.path.isfile(src):
                continue
            badged = add_ribbon(Image.open(src))
            badged.save(os.path.join(dst_dir, name))

    src_dir = os.path.join(MAIN_RES, f"mipmap-{ADAPTIVE_DENSITY}")
    dst_dir = os.path.join(BETA_RES, f"mipmap-{ADAPTIVE_DENSITY}")
    os.makedirs(dst_dir, exist_ok=True)
    for name in ADAPTIVE_FILES:
        src = os.path.join(src_dir, name)
        if not os.path.isfile(src):
            continue
        if name == "ic_launcher_background.png":
            # Background layer stays plain; badge goes on the foreground only.
            Image.open(src).convert("RGBA").save(os.path.join(dst_dir, name))
            continue
        badged = add_ribbon(Image.open(src))
        badged.save(os.path.join(dst_dir, name))

    print("Badged beta launcher icons written to", BETA_RES)


if __name__ == "__main__":
    main()
