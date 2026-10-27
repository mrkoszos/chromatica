package com.mrkoszos.chromatica.gui;

import com.mrkoszos.chromatica.Chromatica;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.configuration.ConfigurationSection;

/** Literal admin branding affects presentation only, never menu or storage identity. */
public final class GuiBranding {
  private String displayName = "Chromatica";
  private boolean showCredit = true;

  public void reload(ConfigurationSection config) {
    String raw = config.getString("branding.display-name", "Chromatica");
    String clean = raw == null ? "" : raw.replaceAll("[\\p{Cntrl}§]", "").strip();
    displayName = clean.isBlank() ? "Chromatica" : clean;
    showCredit = config.getBoolean("branding.show-credit", true);
  }

  public Component title() { return Component.text(displayName); }
  public Component message(Component text) {
    return GuiUtils.text("[" + displayName + "] ", NamedTextColor.GRAY).append(text);
  }
  public boolean showCredit() { return showCredit; }
  public static Component title(Chromatica plugin) {
    return plugin.getGuiBranding() == null ? Component.text("Chromatica") : plugin.getGuiBranding().title();
  }
}
