"""Crisp Play Store assets — render huge, then LANCZOS down."""
from pathlib import Path
import numpy as np
import matplotlib.pyplot as plt
from matplotlib.patches import Circle
from PIL import Image

OUT = Path("store-assets")
OUT.mkdir(exist_ok=True)

BG = "#0C0E12"
BG_RGB = (12, 14, 18)
CYAN = "#33E8FF"
GREY = "#5A6270"
TEXT = "#F4F6F8"
MUTED = "#8A9099"
# Soft single-line accent for feature graphic (no transparency halo)
SOFT = "#1A3A40"


def bezier_points(p0, c1, p1, n=120):
    t = np.linspace(0, 1, n)
    x = (1 - t) ** 2 * p0[0] + 2 * (1 - t) * t * c1[0] + t ** 2 * p1[0]
    y = (1 - t) ** 2 * p0[1] + 2 * (1 - t) * t * c1[1] + t ** 2 * p1[1]
    return np.column_stack([x, y])


def mark_xy():
    a = bezier_points((28, 78), (42, 40), (54, 58), 150)
    b = bezier_points((54, 58), (66, 76), (82, 30), 150)[1:]
    return np.vstack([a, b])


def render_and_down(fig, out_path, final_size, render_scale=4):
    """Save at render_scale x resolution, then downsample for clean AA."""
    tmp = OUT / "_tmp_hires.png"
    w_in, h_in = fig.get_size_inches()
    dpi = 100 * render_scale
    fig.savefig(tmp, dpi=dpi, facecolor=BG, edgecolor="none", pad_inches=0)
    plt.close(fig)
    hi = Image.open(tmp).convert("RGB")
    # Crop to exact expected pixel size if matplotlib added padding
    expected = (int(w_in * dpi), int(h_in * dpi))
    if hi.size != expected:
        canvas = Image.new("RGB", expected, BG_RGB)
        canvas.paste(hi, ((expected[0] - hi.size[0]) // 2, (expected[1] - hi.size[1]) // 2))
        hi = canvas
    lo = hi.resize(final_size, Image.Resampling.LANCZOS)
    lo.save(out_path, "PNG", optimize=True)
    tmp.unlink(missing_ok=True)


# ── App icon ──────────────────────────────────────────────────────
fig, ax = plt.subplots(figsize=(5.12, 5.12), dpi=100)
fig.patch.set_facecolor(BG)
ax.set_facecolor(BG)
ax.set_aspect("equal")
ax.set_xlim(-58, 58)
ax.set_ylim(-58, 58)
ax.axis("off")
fig.subplots_adjust(0, 0, 1, 1)

pts = mark_xy()
scale = 1.5
xs = (pts[:, 0] - 54) * scale
ys = -((pts[:, 1] - 54) * scale)
ax.plot(xs, ys, color=CYAN, linewidth=10, solid_capstyle="round", solid_joinstyle="round",
        antialiased=True, zorder=2)

for ox, oy, filled, r in [(28, 78, False, 5.2), (54, 58, False, 5.2), (82, 30, True, 6.8)]:
    x = (ox - 54) * scale
    y = -((oy - 54) * scale)
    if filled:
        ax.add_patch(Circle((x, y), r * scale / 5.5, facecolor=CYAN, edgecolor="none",
                            zorder=4, antialiased=True))
    else:
        ax.add_patch(Circle((x, y), r * scale / 5.5, facecolor="none", edgecolor=GREY,
                            linewidth=2.8, zorder=4, antialiased=True))

render_and_down(fig, OUT / "app-icon-512.png", (512, 512), render_scale=6)
print("wrote app-icon-512.png")

# ── Feature graphic — ONE solid soft line, centered wordmark ─────
fig, ax = plt.subplots(figsize=(10.24, 5.0), dpi=100)
fig.patch.set_facecolor(BG)
ax.set_facecolor(BG)
ax.set_xlim(0, 1024)
ax.set_ylim(0, 500)
ax.axis("off")
fig.subplots_adjust(0, 0, 1, 1)

pts = mark_xy()
xs = (pts[:, 0] - 54) * 7.0 + 640
ys = -((pts[:, 1] - 54) * 7.0) + 255
# Single solid muted stroke (no alpha = no double-halo)
ax.plot(xs, ys, color=SOFT, linewidth=36, solid_capstyle="round", solid_joinstyle="round",
        antialiased=True, zorder=1)

ax.text(430, 270, "Line", fontsize=78, fontweight="bold", color=TEXT,
        ha="right", va="center", zorder=5, fontfamily="Segoe UI")
ax.text(438, 270, "Draw", fontsize=78, fontweight="bold", color=CYAN,
        ha="left", va="center", zorder=5, fontfamily="Segoe UI")
ax.text(512, 185, "ONE LINE. FULL FOCUS.", fontsize=18, color=MUTED,
        ha="center", va="center", zorder=5, fontfamily="Segoe UI")

render_and_down(fig, OUT / "feature-graphic-1024x500.png", (1024, 500), render_scale=6)
print("wrote feature-graphic-1024x500.png")
