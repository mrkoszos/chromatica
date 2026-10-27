package com.mrkoszos.chromatica.gui;

import com.mrkoszos.chromatica.Chromatica;
import com.mrkoszos.chromatica.model.*;
import java.util.*;

/** Current visual state, with an ephemeral tie-breaker for identical saved styles. */
public final class GuiSelection {
  private GuiSelection() {}

  private record Choice(String source, String id, NameStyle snapshot) {}

  private static final Map<Chromatica, Map<UUID, Map<String, Choice>>> CHOICES =
      new WeakHashMap<>();

  public static void remember(
      Chromatica plugin, UUID player, String source, String id, NameStyle style) {
    var choices =
        CHOICES
            .computeIfAbsent(plugin, ignored -> new HashMap<>())
            .computeIfAbsent(player, ignored -> new HashMap<>());
    choices.put(source, new Choice(source, id, style.copy()));
    // Preset catalogues are exclusive; favorite identity may coexist with its source preset.
    if (source.equals("server")) choices.remove("community");
    else if (source.equals("community")) choices.remove("server");
  }

  public static void clear(Chromatica plugin, UUID player) {
    var choices = CHOICES.get(plugin);
    if (choices != null) choices.remove(player);
  }

  public static boolean solid(NameStyle style, String hex) {
    return style.getType() == StyleType.SOLID && hexEquals(style.getPrimaryColor(), hex);
  }

  private static final String[] PALETTE = {
    "#FF5555", "#FFAA00", "#FFFF55", "#55FF55", "#55FFFF", "#5555FF", "#AA55FF"
  };

  public static String paletteHex(int slot) {
    return slot >= 10 && slot <= 16 ? PALETTE[slot - 10] : null;
  }

  public static void markColors(org.bukkit.inventory.Inventory inventory, String current) {
    String[] palette = PALETTE;
    boolean standard = false;
    for (int i = 0; i < palette.length; i++) {
      boolean selected = hexEquals(current, palette[i]);
      GuiUtils.mark(inventory, 10 + i, selected);
      standard |= selected;
    }
    if (!GuiUtils.isMenu(inventory, "ColorMenu"))
      GuiUtils.mark(
          inventory, 22, current != null && current.matches("^#[0-9a-fA-F]{6}$") && !standard);
  }

  private static boolean hexEquals(String a, String b) {
    return a != null && b != null && a.equalsIgnoreCase(b);
  }

  public static boolean sameColors(NameStyle a, NameStyle b) {
    if (a == null || b == null || a.getType() != b.getType()) return false;
    if (a.getType() == StyleType.SOLID) return hexEquals(a.getPrimaryColor(), b.getPrimaryColor());
    var first = a.getGradientColors();
    var second = b.getGradientColors();
    if (first == null || second == null || first.size() != second.size()) return false;
    for (int i = 0; i < first.size(); i++)
      if (!hexEquals(first.get(i), second.get(i))) return false;
    return true;
  }

  public static boolean sameStyle(NameStyle a, NameStyle b) {
    return sameColors(a, b)
        && a.isScramble() == b.isScramble()
        && a.isBold() == b.isBold()
        && a.isItalic() == b.isItalic()
        && a.isUnderlined() == b.isUnderlined()
        && a.isStrikethrough() == b.isStrikethrough();
  }

  public static NameStyle colors(List<String> colors) {
    NameStyle style = new NameStyle();
    if (colors.size() == 1) style.setPrimaryColor(colors.getFirst());
    else {
      style.setType(StyleType.GRADIENT);
      style.setGradientColors(colors);
    }
    return style;
  }

  /**
   * Evaluate the entire catalogue before pagination, so duplicates cannot glow on different pages.
   */
  public static String selected(
      Chromatica plugin,
      UUID player,
      String source,
      NameStyle active,
      Map<String, NameStyle> candidates,
      boolean colorsOnly) {
    var choices = CHOICES.get(plugin);
    var selections = choices == null ? null : choices.get(player);
    Choice choice = selections == null ? null : selections.get(source);
    if (choice != null && matches(active, choice.snapshot, colorsOnly)) {
      return matches(active, candidates.get(choice.id), colorsOnly) ? choice.id : null;
    }
    if (selections != null && (source.equals("server") || source.equals("community"))) {
      Choice other = selections.get(source.equals("server") ? "community" : "server");
      if (other != null && matches(active, other.snapshot, colorsOnly)) return null;
    }
    if (source.equals("server") && active.getPresetId() != null) {
      String id = active.getPresetId();
      return matches(active, candidates.get(id), colorsOnly) ? id : null;
    }
    return candidates.entrySet().stream()
        .filter(entry -> matches(active, entry.getValue(), colorsOnly))
        .map(Map.Entry::getKey)
        .findFirst()
        .orElse(null);
  }

  private static boolean matches(NameStyle a, NameStyle b, boolean colorsOnly) {
    return colorsOnly ? sameColors(a, b) : sameStyle(a, b);
  }
}
