package dev.kyluua.utilitiesscarce.render;

import dev.kyluua.utilitiesscarce.config.UtilitiesScarceConfig.ColorMode;
import net.minecraft.util.Mth;

/**
 * Colour for one highlight.
 *
 * <p>Static mode hands back the configured ARGB value untouched. Rainbow mode
 * walks the hue wheel on a wall-clock timer, so it animates at the same rate
 * regardless of frame rate, and offsets each target's hue a little so a crowd
 * reads as a gradient rather than one flat colour.
 */
public final class Palette {
	private static final double MILLIS_PER_SECOND = 1000.0D;

	private Palette() {
	}

	public static int resolve(ColorMode mode, int staticColor, double speed, double spread,
			int alpha, int index) {
		if (mode != ColorMode.RAINBOW) {
			return staticColor;
		}

		return rainbow(speed, spread * index, alpha);
	}

	public static int rainbow(double cyclesPerSecond, double offset, int alpha) {
		double speed = Math.max(0.01D, cyclesPerSecond);
		double cycleMillis = MILLIS_PER_SECOND / speed;
		double phase = (System.currentTimeMillis() % (long) Math.max(1.0D, cycleMillis)) / cycleMillis;
		float hue = (float) wrap(phase + offset);

		return withAlpha(hsvToRgb(hue, 1.0F, 1.0F), alpha);
	}

	public static int withAlpha(int rgb, int alpha) {
		return (Mth.clamp(alpha, 0, 255) << 24) | (rgb & 0xFFFFFF);
	}

	private static double wrap(double value) {
		double wrapped = value % 1.0D;
		return wrapped < 0.0D ? wrapped + 1.0D : wrapped;
	}

	/** Plain HSV to packed RGB; no dependency on the game's colour helpers. */
	private static int hsvToRgb(float hue, float saturation, float value) {
		float scaled = hue * 6.0F;
		int sector = (int) scaled % 6;
		float fraction = scaled - (int) scaled;

		float p = value * (1.0F - saturation);
		float q = value * (1.0F - fraction * saturation);
		float t = value * (1.0F - (1.0F - fraction) * saturation);

		float red;
		float green;
		float blue;

		switch (sector) {
			case 0 -> {
				red = value;
				green = t;
				blue = p;
			}
			case 1 -> {
				red = q;
				green = value;
				blue = p;
			}
			case 2 -> {
				red = p;
				green = value;
				blue = t;
			}
			case 3 -> {
				red = p;
				green = q;
				blue = value;
			}
			case 4 -> {
				red = t;
				green = p;
				blue = value;
			}
			default -> {
				red = value;
				green = p;
				blue = q;
			}
		}

		return (channel(red) << 16) | (channel(green) << 8) | channel(blue);
	}

	private static int channel(float value) {
		return Mth.clamp((int) (value * 255.0F + 0.5F), 0, 255);
	}
}
