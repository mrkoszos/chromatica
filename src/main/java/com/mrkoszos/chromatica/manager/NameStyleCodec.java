package com.mrkoszos.chromatica.manager;

import com.mrkoszos.chromatica.model.NameStyle;
import com.mrkoszos.chromatica.model.StyleType;
import org.bukkit.configuration.ConfigurationSection;

public final class NameStyleCodec {
  public static final int MAX_STOPS = 21;

  public static boolean isHex(String value) {
    return value != null && value.matches("#[0-9a-fA-F]{6}");
  }

  public static NameStyle validated(NameStyle original) {
    if (original == null) throw new IllegalArgumentException("Missing style");
    NameStyle style = original.copy();
    if (style.getType() == null) throw new IllegalArgumentException("Missing style type");
    if (!isHex(style.getPrimaryColor()))
      throw new IllegalArgumentException("Invalid primary color");
    if (style.getSecondaryColor() != null && !isHex(style.getSecondaryColor()))
      throw new IllegalArgumentException("Invalid secondary color");
    var colors = style.getGradientColors();
    if (colors.size() > MAX_STOPS || colors.stream().anyMatch(color -> !isHex(color)))
      throw new IllegalArgumentException("Invalid gradient colors (maximum " + MAX_STOPS + " stops)");
    if (style.getType() == StyleType.PRESET)
      style.setType(colors.size() >= 2 ? StyleType.GRADIENT : StyleType.SOLID);
    if (style.getType() == StyleType.GRADIENT && colors.size() < 2)
      throw new IllegalArgumentException("A gradient requires 2–" + MAX_STOPS + " stops");
    return style;
  }

  private NameStyleCodec() {}

  public static void write(ConfigurationSection config, NameStyle style) {
    config.set("type", style.getType().name());
    config.set("primary-color", style.getPrimaryColor());
    config.set("secondary-color", style.getSecondaryColor());
    config.set("gradient-colors", style.getGradientColors());
    config.set("preset-id", style.getPresetId());
    config.set("bold", style.isBold());
    config.set("italic", style.isItalic());
    config.set("underlined", style.isUnderlined());
    config.set("strikethrough", style.isStrikethrough());
    config.set("scramble", style.isScramble());
  }

  public static NameStyle read(ConfigurationSection config) {
    for (String key : java.util.List.of("type", "primary-color", "secondary-color", "preset-id"))
      if (config.contains(key) && !(config.get(key) instanceof String))
        throw new IllegalArgumentException("Invalid field: " + key);
    for (String key :
        java.util.List.of("bold", "italic", "underlined", "strikethrough", "scramble"))
      if (config.contains(key) && !(config.get(key) instanceof Boolean))
        throw new IllegalArgumentException("Invalid field: " + key);
    if (config.contains("gradient-colors")
        && (!(config.get("gradient-colors") instanceof java.util.List<?> list)
            || list.stream().anyMatch(color -> !(color instanceof String))))
      throw new IllegalArgumentException("Invalid gradient-colors list");
    NameStyle style = new NameStyle();
    try {
      style.setType(StyleType.valueOf(config.getString("type", "SOLID")));
    } catch (IllegalArgumentException exception) {
      throw new IllegalArgumentException("Unknown style type", exception);
    }
    style.setPrimaryColor(config.getString("primary-color", "#FFFFFF"));
    style.setSecondaryColor(config.getString("secondary-color"));
    style.setGradientColors(config.getStringList("gradient-colors"));
    style.setPresetId(config.getString("preset-id"));
    style.setBold(config.getBoolean("bold"));
    style.setItalic(config.getBoolean("italic"));
    style.setUnderlined(config.getBoolean("underlined"));
    style.setStrikethrough(config.getBoolean("strikethrough"));
    style.setScramble(config.getBoolean("scramble"));
    return validated(style);
  }
}
