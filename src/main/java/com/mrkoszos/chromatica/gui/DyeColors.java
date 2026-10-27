package com.mrkoszos.chromatica.gui;

import org.bukkit.Material;

/** Exact Chromatica palette identity first; otherwise hue-weighted distance in D65 OKLab.
 * Reference palette: Minecraft vanilla dye diffuse RGB values (DyeColor).
 * Stable array order breaks ties. Icons never modify stored HEX values.
 */
public final class DyeColors {
  private DyeColors() {}
  private static final Material[] DYES = {
    Material.WHITE_DYE, Material.ORANGE_DYE, Material.MAGENTA_DYE, Material.LIGHT_BLUE_DYE,
    Material.YELLOW_DYE, Material.LIME_DYE, Material.PINK_DYE, Material.GRAY_DYE,
    Material.LIGHT_GRAY_DYE, Material.CYAN_DYE, Material.PURPLE_DYE, Material.BLUE_DYE,
    Material.BROWN_DYE, Material.GREEN_DYE, Material.RED_DYE, Material.BLACK_DYE
  };
  private static final int[] RGB = {0xF9FFFE, 0xF9801D, 0xC74EBD, 0x3AB3DA,
    0xFED83D, 0x80C71F, 0xF38BAA, 0x474F52, 0x9D9D97, 0x169C9C, 0x8932B8,
    0x3C44AA, 0x835432, 0x5E7C16, 0xB02E26, 0x1D1D21};

  public static Material nearest(String hex) {
    if (!com.mrkoszos.chromatica.manager.NameStyleCodec.isHex(hex)) return Material.WHITE_DYE;
    int rgb = Integer.parseInt(hex.substring(1), 16);
    Material[] standard = {Material.RED_DYE, Material.ORANGE_DYE, Material.YELLOW_DYE,
      Material.LIME_DYE, Material.CYAN_DYE, Material.BLUE_DYE, Material.PURPLE_DYE};
    for (int i = 0; i < standard.length; i++)
      if (hex.equalsIgnoreCase(GuiSelection.paletteHex(10 + i))) return standard[i];
    double[] color = lab(rgb);
    double best = Double.POSITIVE_INFINITY;
    int selected = 0;
    for (int i = 0; i < RGB.length; i++) {
      double[] reference = lab(RGB[i]);
      double distance = 0;
      for (int channel = 0; channel < 3; channel++) {
        double delta = color[channel] - reference[channel];
        distance += delta * delta;
      }
      // Separate lightness/chroma from hue; weight hue to keep vivid reds red
      // despite the vanilla red dye's much lower brightness and saturation.
      double chromaDelta = Math.hypot(color[1], color[2]) - Math.hypot(reference[1], reference[2]);
      double lightnessDelta = color[0] - reference[0];
      double hueSquared = Math.max(0, distance - lightnessDelta * lightnessDelta - chromaDelta * chromaDelta);
      distance += 3 * hueSquared;
      if (distance < best) { best = distance; selected = i; }
    }
    return DYES[selected];
  }

  private static double linear(int channel) {
    double value = channel / 255.0;
    return value <= .04045 ? value / 12.92 : Math.pow((value + .055) / 1.055, 2.4);
  }

  private static double[] lab(int rgb) {
    double r = linear(rgb >> 16 & 255), g = linear(rgb >> 8 & 255), b = linear(rgb & 255);
    double l = Math.cbrt(.4122214708 * r + .5363325363 * g + .0514459929 * b);
    double m = Math.cbrt(.2119034982 * r + .6806995451 * g + .1073969566 * b);
    double s = Math.cbrt(.0883024619 * r + .2817188376 * g + .6299787005 * b);
    return new double[] {
      .2104542553 * l + .7936177850 * m - .0040720468 * s,
      1.9779984951 * l - 2.4285922050 * m + .4505937099 * s,
      .0259040371 * l + .7827717662 * m - .8086757660 * s
    };
  }
}
