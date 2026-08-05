#!/usr/bin/env python3
"""Recreate the four Finwatch listing screenshots with rights-free content.

Faithful to the real app UI (layout, colors, typography) but every piece of
media artwork is generated here: no film posters, no album covers.
"""
import math
from PIL import Image, ImageDraw, ImageFont, ImageFilter

S = 450          # output size (Play: 1:1, >=384)
SS = 3           # supersample
W = S * SS

BLUE = (0x00, 0xA4, 0xDC)
PURPLE = (0xAA, 0x5C, 0xC3)
FONT_B = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
FONT_R = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"


def fb(size):
    return ImageFont.truetype(FONT_B, size)


def fr(size):
    return ImageFont.truetype(FONT_R, size)


def lerp(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def canvas():
    return Image.new("RGB", (W, W), (0, 0, 0))


def finish(img, name):
    img.resize((S, S), Image.LANCZOS).save(name)
    print(name)


def gradient_ring(d, cx, cy, r, width):
    steps = 240
    for i in range(steps):
        a0 = 2 * math.pi * i / steps
        a1 = 2 * math.pi * (i + 1) / steps + 0.01
        col = lerp(BLUE, PURPLE, (math.sin(a0) + 1) / 2)
        d.arc([cx - r, cy - r, cx + r, cy + r],
              start=math.degrees(a0), end=math.degrees(a1), fill=col, width=width)


# ---------- icons (cyan glyphs, matching the app's home tiles) ----------

def icon_note(d, cx, cy, s):
    d.ellipse([cx - s * 0.42, cy + s * 0.06, cx + s * 0.02, cy + s * 0.42], fill=BLUE)
    d.rectangle([cx - s * 0.04, cy - s * 0.42, cx + 0.045 * s, cy + s * 0.28], fill=BLUE)
    d.polygon([(cx - s * 0.04, cy - s * 0.42), (cx + s * 0.42, cy - s * 0.30),
               (cx + s * 0.42, cy - s * 0.12), (cx - s * 0.04, cy - s * 0.24)], fill=BLUE)


def icon_tv(d, cx, cy, s):
    d.rounded_rectangle([cx - s * 0.44, cy - s * 0.34, cx + s * 0.44, cy + s * 0.22],
                        radius=int(s * 0.08), outline=BLUE, width=int(s * 0.10))
    d.rectangle([cx - s * 0.16, cy + s * 0.26, cx + s * 0.16, cy + s * 0.36], fill=BLUE)


def icon_clapper(d, cx, cy, s):
    d.rounded_rectangle([cx - s * 0.44, cy - s * 0.16, cx + s * 0.44, cy + s * 0.38],
                        radius=int(s * 0.06), fill=BLUE)
    d.polygon([(cx - s * 0.44, cy - s * 0.20), (cx + s * 0.44, cy - s * 0.42),
               (cx + s * 0.44, cy - s * 0.24), (cx - s * 0.44, cy - s * 0.02)], fill=BLUE)


def icon_heart(d, cx, cy, s):
    r = s * 0.26
    cy0 = cy - s * 0.10
    d.ellipse([cx - 2 * r + r * 0.15, cy0 - r, cx + r * 0.15, cy0 + r], fill=BLUE)
    d.ellipse([cx - r * 0.15, cy0 - r, cx + 2 * r - r * 0.15, cy0 + r], fill=BLUE)
    d.polygon([(cx - 1.78 * r, cy0 + r * 0.42), (cx + 1.78 * r, cy0 + r * 0.42),
               (cx, cy0 + 2.1 * r)], fill=BLUE)


def icon_gear(d, cx, cy, s):
    r = s * 0.30
    for i in range(8):
        a = i * math.pi / 4
        tx, ty = cx + (r + s * 0.10) * math.cos(a), cy + (r + s * 0.10) * math.sin(a)
        d.ellipse([tx - s * 0.09, ty - s * 0.09, tx + s * 0.09, ty + s * 0.09], fill=BLUE)
    d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=BLUE)
    d.ellipse([cx - r * 0.45, cy - r * 0.45, cx + r * 0.45, cy + r * 0.45], fill=(17, 17, 17))


def shot_home():
    img = canvas()
    d = ImageDraw.Draw(img)
    tile_r = W * 0.118
    ring_w = int(W * 0.006)
    positions = [(-0.145, -0.175), (0.145, -0.175), (-0.145, 0.09), (0.145, 0.09), (0.0, 0.335)]
    icons = [icon_note, icon_tv, icon_clapper, icon_heart, icon_gear]
    for (px, py), ic in zip(positions, icons):
        cx, cy = W / 2 + px * W, W / 2 + py * W - 0.045 * W
        d.ellipse([cx - tile_r, cy - tile_r, cx + tile_r, cy + tile_r], fill=(17, 17, 17))
        gradient_ring(d, cx, cy, tile_r, ring_w)
        ic(d, cx, cy, tile_r * 1.05)
    finish(img, "s1-home.png")


# ---------- generated, rights-free "posters" ----------

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
        dd.ellipse([size * 0.2, size * 0.2, size * 0.8, size * 0.8], outline=(255, 255, 255), width=max(2, size // 40))
        dd.ellipse([size * 0.42, size * 0.42, size * 0.58, size * 0.58], fill=(255, 255, 255))
        dd.ellipse([size * 0.68, size * 0.30, size * 0.78, size * 0.40], fill=(255, 220, 120))
    elif kind == "waves":
        for i in range(4):
            yy = size * (0.35 + 0.16 * i)
            pts = [(x, yy + math.sin(x / size * 6.28 + i) * size * 0.05) for x in range(0, size + 8, 8)]
            dd.line(pts, fill=(240, 245, 255), width=max(2, size // 34))
    return im


def rounded(im, radius):
    m = Image.new("L", im.size, 0)
    ImageDraw.Draw(m).rounded_rectangle([0, 0, im.size[0], im.size[1]], radius=radius, fill=255)
    out = Image.new("RGBA", im.size)
    out.paste(im, (0, 0), m)
    return out


def shot_browse():
    img = canvas()
    d = ImageDraw.Draw(img)
    rows = [("Charge!", ("peaks", (250, 150, 60), (120, 40, 90))),
            ("Cosmos Laundromat", ("orbit", (20, 24, 70), (60, 20, 90))),
            ("Spring", ("waves", (30, 90, 130), (10, 40, 70)))]
    row_h = W * 0.22
    row_gap = W * 0.045
    total = len(rows) * row_h + (len(rows) - 1) * row_gap
    y0 = (W - total) / 2
    # partial rows top/bottom, like the real scrolling list
    for extra_y, label in [(y0 - row_h - row_gap, "Agent 327"), (y0 + total + row_gap, "Sprite Fright")]:
        d.rounded_rectangle([W * 0.08, extra_y, W * 0.92, extra_y + row_h], radius=int(row_h / 2), fill=(26, 26, 26))
        d.text((W * 0.36, extra_y + row_h * 0.36), label, font=fb(int(W * 0.055)), fill=(150, 150, 150))
    for i, (title, (kind, top, bottom)) in enumerate(rows):
        ry = y0 + i * (row_h + row_gap)
        d.rounded_rectangle([W * 0.08, ry, W * 0.92, ry + row_h], radius=int(row_h / 2), fill=(26, 26, 26))
        cov = rounded(poster(int(row_h * 0.76), top, bottom, kind), int(row_h * 0.16))
        img.paste(cov, (int(W * 0.115), int(ry + row_h * 0.12)), cov)
        # wrap long titles onto two lines like the real list
        font = fb(int(W * 0.062))
        words = title.split()
        if d.textlength(title, font=font) > W * 0.50 and len(words) > 1:
            d.text((W * 0.36, ry + row_h * 0.16), words[0], font=font, fill=(210, 210, 210))
            d.text((W * 0.36, ry + row_h * 0.52), " ".join(words[1:]), font=font, fill=(210, 210, 210))
        else:
            d.text((W * 0.36, ry + row_h * 0.33), title, font=font, fill=(210, 210, 210))
    finish(img, "s2-browse.png")


def shot_video():
    img = canvas()
    d = ImageDraw.Draw(img)
    # 16:9 letterboxed procedural dusk landscape
    vh = int(W * 9 / 16)
    y0 = (W - vh) // 2
    for y in range(vh):
        t = y / vh
        d.line([(0, y0 + y), (W, y0 + y)], fill=lerp((250, 170, 90), (60, 40, 90), t))
    # sun
    d.ellipse([W * 0.62, y0 + vh * 0.18, W * 0.74, y0 + vh * 0.18 + W * 0.12], fill=(255, 235, 190))
    # mountain layers
    def ridge(base, amp, col, seed):
        pts = [(0, y0 + vh)]
        for x in range(0, W + 20, 20):
            yy = y0 + vh * base + math.sin(x / W * 5 + seed) * vh * amp + math.sin(x / W * 13 + seed * 2) * vh * amp * 0.4
            pts.append((x, yy))
        pts.append((W, y0 + vh))
        d.polygon(pts, fill=col)
    ridge(0.55, 0.10, (90, 60, 100), 1.2)
    ridge(0.70, 0.08, (55, 38, 70), 3.1)
    ridge(0.84, 0.05, (30, 22, 40), 5.0)
    finish(img, "s3-video.png")


def shot_player():
    # background: blurred generated "cover art"
    bg = poster(W, (30, 90, 130), (110, 40, 120), "peaks").filter(ImageFilter.GaussianBlur(W * 0.02))
    img = Image.new("RGB", (W, W))
    img.paste(bg, (0, 0))
    dark = Image.new("L", (W, W), 110)
    img.paste(Image.new("RGB", (W, W), (0, 0, 0)), (0, 0), dark)
    d = ImageDraw.Draw(img)
    # top progress pill
    d.rounded_rectangle([W * 0.42, W * 0.015, W * 0.58, W * 0.045], radius=int(W * 0.015), fill=BLUE)
    # title
    title = "Wing It!"
    font = fb(int(W * 0.085))
    tw = d.textlength(title, font=font)
    d.text(((W - tw) / 2, W * 0.24), title, font=font, fill=(255, 255, 255))
    # transport controls
    cy = W * 0.50
    for sx, kind in [(W * 0.18, "prev"), (W * 0.82, "next")]:
        r = W * 0.085
        d.ellipse([sx - r, cy - r, sx + r, cy + r], fill=(20, 20, 20, 180))
        tri = r * 0.45
        sgn = -1 if kind == "prev" else 1
        d.polygon([(sx + sgn * tri * 0.9, cy), (sx - sgn * tri * 0.5, cy - tri),
                   (sx - sgn * tri * 0.5, cy + tri)], fill=(235, 235, 235))
        d.rectangle([sx + sgn * tri * 0.9 - (W * 0.008 if sgn > 0 else 0),
                     cy - tri, sx + sgn * tri * 0.9 + (W * 0.008 if sgn < 0 else 0) + (W * 0.008 if sgn > 0 else 0), cy + tri], fill=(235, 235, 235))
    # pause
    bw, bh = W * 0.030, W * 0.10
    d.rounded_rectangle([W / 2 - bw * 1.8, cy - bh / 2, W / 2 - bw * 0.8, cy + bh / 2], radius=int(bw / 2), fill=(255, 255, 255))
    d.rounded_rectangle([W / 2 + bw * 0.8, cy - bh / 2, W / 2 + bw * 1.8, cy + bh / 2], radius=int(bw / 2), fill=(255, 255, 255))
    # time readout
    t = "0:42 / 6:28"
    font2 = fr(int(W * 0.058))
    tw2 = d.textlength(t, font=font2)
    d.text(((W - tw2) / 2, W * 0.66), t, font=font2, fill=(235, 235, 235))
    finish(img, "s4-player.png")


if __name__ == "__main__":
    shot_home()
    shot_browse()
    shot_video()
    shot_player()
