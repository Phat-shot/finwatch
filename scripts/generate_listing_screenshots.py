#!/usr/bin/env python3
"""Finwatch listing screenshots, take 2: real Material Icons glyphs, Roboto,
round watch mask -- 1:1 with the actual app UI, rights-free media only."""
import math
import os
from PIL import Image, ImageDraw, ImageFont, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
S = 450
SS = 3
W = S * SS

BLUE = (0x00, 0xA4, 0xDC)
PURPLE = (0xAA, 0x5C, 0xC3)

FONT_SOURCES = {
    "MaterialIcons-Regular.ttf":
        "https://raw.githubusercontent.com/google/material-design-icons/master/font/MaterialIcons-Regular.ttf",
    "Roboto-Bold.ttf":
        "https://raw.githubusercontent.com/googlefonts/roboto-2/main/src/hinted/Roboto-Bold.ttf",
    "Roboto-Regular.ttf":
        "https://raw.githubusercontent.com/googlefonts/roboto-2/main/src/hinted/Roboto-Regular.ttf",
}


def ensure_fonts():
    """Fetch Roboto + Material Icons (both Apache-2.0) next to this script
    on first run -- they are not committed to keep the repo lean."""
    import urllib.request
    for name, url in FONT_SOURCES.items():
        path = os.path.join(HERE, name)
        if not os.path.exists(path):
            print("fetching", name)
            urllib.request.urlretrieve(url, path)


ensure_fonts()

MI = os.path.join(HERE, "MaterialIcons-Regular.ttf")
RB = os.path.join(HERE, "Roboto-Bold.ttf")
RR = os.path.join(HERE, "Roboto-Regular.ttf")

# codepoints from MaterialIcons-Regular.codepoints (same glyphs the app's
# material-icons-extended library ships)
ICONS = {"music_note": chr(0xE405), "tv": chr(0xE333), "movie": chr(0xE02C),
         "favorite": chr(0xE87D), "settings": chr(0xE8B8),
         "skip_previous": chr(0xE045), "skip_next": chr(0xE044), "pause": chr(0xE034)}


def font(path, size):
    return ImageFont.truetype(path, size)


def lerp(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def icon(d, name, cx, cy, size, fill):
    f = font(MI, int(size))
    ch = ICONS[name]
    bbox = d.textbbox((0, 0), ch, font=f)
    w, h = bbox[2] - bbox[0], bbox[3] - bbox[1]
    d.text((cx - w / 2 - bbox[0], cy - h / 2 - bbox[1]), ch, font=f, fill=fill)


def text_center(d, s, cx, y, f, fill):
    w = d.textlength(s, font=f)
    d.text((cx - w / 2, y), s, font=f, fill=fill)


def round_mask(img):
    """Clip the square UI to the round watch display, black corners."""
    m = Image.new("L", (W, W), 0)
    ImageDraw.Draw(m).ellipse([0, 0, W, W], fill=255)
    out = Image.new("RGB", (W, W), (0, 0, 0))
    out.paste(img, (0, 0), m)
    return out


def finish(img, name):
    round_mask(img).resize((S, S), Image.LANCZOS).save(name)
    print(name)


def gradient_ring(d, cx, cy, r, width):
    steps = 240
    for i in range(steps):
        a0 = 2 * math.pi * i / steps
        a1 = 2 * math.pi * (i + 1) / steps + 0.02
        col = lerp(BLUE, PURPLE, (math.sin(a0 - math.pi / 4) + 1) / 2)
        d.arc([cx - r, cy - r, cx + r, cy + r],
              start=math.degrees(a0), end=math.degrees(a1), fill=col, width=width)


def shot_home():
    img = Image.new("RGB", (W, W), (0, 0, 0))
    d = ImageDraw.Draw(img)
    tile_r = W * 0.117
    ring_w = max(2, int(W * 0.005))
    positions = [(-0.145, -0.19), (0.145, -0.19), (-0.145, 0.075), (0.145, 0.075), (0.0, 0.325)]
    names = ["music_note", "tv", "movie", "favorite", "settings"]
    for (px, py), name in zip(positions, names):
        cx, cy = W / 2 + px * W, W / 2 + py * W - 0.02 * W
        d.ellipse([cx - tile_r, cy - tile_r, cx + tile_r, cy + tile_r], fill=(17, 17, 17))
        gradient_ring(d, cx, cy, tile_r, ring_w)
        icon(d, name, cx, cy, tile_r * 1.05, BLUE)
    finish(img, "s1-home.png")


def poster(size, top, bottom, kind):
    im = Image.new("RGB", (size, size))
    dd = ImageDraw.Draw(im)
    for y in range(size):
        dd.line([(0, y), (size, y)], fill=lerp(top, bottom, y / size))
    if kind == "peaks":
        dd.polygon([(0, size), (size * 0.32, size * 0.42), (size * 0.55, size * 0.78),
                    (size * 0.75, size * 0.35), (size, size)], fill=(30, 34, 60))
        dd.ellipse([size * 0.62, size * 0.14, size * 0.82, size * 0.34], fill=(255, 240, 200))
    elif kind == "orbit":
        dd.ellipse([size * 0.2, size * 0.2, size * 0.8, size * 0.8],
                   outline=(255, 255, 255), width=max(2, size // 40))
        dd.ellipse([size * 0.42, size * 0.42, size * 0.58, size * 0.58], fill=(255, 255, 255))
        dd.ellipse([size * 0.68, size * 0.30, size * 0.78, size * 0.40], fill=(255, 220, 120))
    elif kind == "waves":
        for i in range(4):
            yy = size * (0.35 + 0.16 * i)
            pts = [(x, yy + math.sin(x / size * 6.28 + i) * size * 0.05)
                   for x in range(0, size + 8, 8)]
            dd.line(pts, fill=(240, 245, 255), width=max(2, size // 34))
    return im


def rounded(im, radius):
    m = Image.new("L", im.size, 0)
    ImageDraw.Draw(m).rounded_rectangle([0, 0, im.size[0], im.size[1]], radius=radius, fill=255)
    out = Image.new("RGBA", im.size)
    out.paste(im, (0, 0), m)
    return out


def shot_browse():
    img = Image.new("RGB", (W, W), (0, 0, 0))
    d = ImageDraw.Draw(img)
    rows = [("Charge!", ("peaks", (250, 150, 60), (120, 40, 90))),
            ("Cosmos\nLaundromat", ("orbit", (20, 24, 70), (60, 20, 90))),
            ("Spring", ("waves", (30, 90, 130), (10, 40, 70)))]
    row_h = W * 0.235
    row_gap = W * 0.035
    total = len(rows) * row_h + (len(rows) - 1) * row_gap
    y0 = (W - total) / 2
    tf = font(RB, int(W * 0.062))
    grey = (158, 158, 158)
    # partial rows peeking at top/bottom, like the scrolling list
    for extra_y, label in [(y0 - row_h - row_gap, "Agent 327"),
                           (y0 + total + row_gap, "Sprite Fright")]:
        d.rounded_rectangle([W * 0.05, extra_y, W * 0.95, extra_y + row_h],
                            radius=int(row_h / 2), fill=(28, 28, 28))
        cov = rounded(poster(int(row_h * 0.72), (90, 90, 110), (40, 40, 60), "orbit"), int(row_h * 0.14))
        img.paste(cov, (int(W * 0.095), int(extra_y + row_h * 0.14)), cov)
        d.text((W * 0.34, extra_y + row_h * 0.32), label, font=tf, fill=grey)
    for i, (title, (kind, top, bottom)) in enumerate(rows):
        ry = y0 + i * (row_h + row_gap)
        d.rounded_rectangle([W * 0.05, ry, W * 0.95, ry + row_h],
                            radius=int(row_h / 2), fill=(28, 28, 28))
        cov = rounded(poster(int(row_h * 0.72), top, bottom, kind), int(row_h * 0.14))
        img.paste(cov, (int(W * 0.095), int(ry + row_h * 0.14)), cov)
        lines = title.split("\n")
        lh = tf.size * 1.18
        ty = ry + row_h / 2 - lh * len(lines) / 2 + (lh - tf.size) / 2
        for li, line in enumerate(lines):
            d.text((W * 0.34, ty + li * lh), line, font=tf, fill=grey)
    finish(img, "s2-browse.png")


def shot_video():
    img = Image.new("RGB", (W, W), (0, 0, 0))
    d = ImageDraw.Draw(img)
    vh = int(W * 9 / 16)
    y0 = (W - vh) // 2
    for y in range(vh):
        d.line([(0, y0 + y), (W, y0 + y)], fill=lerp((250, 170, 90), (60, 40, 90), y / vh))
    d.ellipse([W * 0.62, y0 + vh * 0.18, W * 0.74, y0 + vh * 0.18 + W * 0.12], fill=(255, 235, 190))

    def ridge(base, amp, col, seed):
        pts = [(0, y0 + vh)]
        for x in range(0, W + 20, 20):
            yy = (y0 + vh * base + math.sin(x / W * 5 + seed) * vh * amp
                  + math.sin(x / W * 13 + seed * 2) * vh * amp * 0.4)
            pts.append((x, yy))
        pts.append((W, y0 + vh))
        d.polygon(pts, fill=col)

    ridge(0.55, 0.10, (90, 60, 100), 1.2)
    ridge(0.70, 0.08, (55, 38, 70), 3.1)
    ridge(0.84, 0.05, (30, 22, 40), 5.0)
    finish(img, "s3-video.png")


def shot_player():
    bg = poster(W, (30, 90, 130), (110, 40, 120), "peaks").filter(ImageFilter.GaussianBlur(W * 0.02))
    img = Image.new("RGB", (W, W))
    img.paste(bg, (0, 0))
    img.paste(Image.new("RGB", (W, W), (0, 0, 0)), (0, 0), Image.new("L", (W, W), 110))
    d = ImageDraw.Draw(img)
    # top progress pill
    d.rounded_rectangle([W * 0.44, W * 0.022, W * 0.56, W * 0.052],
                        radius=int(W * 0.015), fill=BLUE)
    text_center(d, "Wing It!", W / 2, W * 0.255, font(RB, int(W * 0.082)), (255, 255, 255))
    cy = W * 0.50
    # skip buttons in translucent dark circles
    for sx, name in [(W * 0.165, "skip_previous"), (W * 0.835, "skip_next")]:
        r = W * 0.095
        circ = Image.new("RGBA", (W, W), (0, 0, 0, 0))
        ImageDraw.Draw(circ).ellipse([sx - r, cy - r, sx + r, cy + r], fill=(15, 15, 15, 200))
        img.paste(circ, (0, 0), circ)
        d = ImageDraw.Draw(img)
        icon(d, name, sx, cy, r * 1.15, (235, 235, 235))
    icon(d, "pause", W / 2, cy, W * 0.155, (255, 255, 255))
    text_center(d, "0:42 / 6:28", W / 2, W * 0.645, font(RR, int(W * 0.055)), (232, 232, 232))
    finish(img, "s4-player.png")


if __name__ == "__main__":
    shot_home()
    shot_browse()
    shot_video()
    shot_player()
