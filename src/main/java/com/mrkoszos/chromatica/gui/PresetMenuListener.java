package com.mrkoszos.chromatica.gui;

import com.mrkoszos.chromatica.Chromatica;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class PresetMenuListener implements Listener {
  private final Chromatica plugin;

  public PresetMenuListener(Chromatica plugin) {
    this.plugin = plugin;
  }

  @EventHandler
  public void onInventoryClick(InventoryClickEvent event) {
    if (!GuiUtils.isMenu(event.getView().getTopInventory(), "PresetMenu")) return;
    event.setCancelled(true);
    if (!GuiUtils.isActionClick(event) || !(event.getWhoClicked() instanceof Player player)) return;
    if (!player.hasPermission("chromatica.use")) {
      GuiSounds.play(plugin, player, GuiSounds.Event.ERROR);
      return;
    }
    int slot = event.getRawSlot();
    if (slot == PresetMenu.PREMIUM_SLOT || slot == PresetMenu.SEASONAL_SLOT
        || (slot == PresetMenu.COMMUNITY_SLOT && !plugin.isCommunityPresetsEnabled())) {
      String category = slot == PresetMenu.PREMIUM_SLOT ? "Premium"
          : slot == PresetMenu.SEASONAL_SLOT ? "Seasonal" : "Community";
      player.sendActionBar(net.kyori.adventure.text.Component.text(
          category + " Presets are currently unavailable.",
          net.kyori.adventure.text.format.NamedTextColor.YELLOW));
      GuiSounds.play(plugin, player, GuiSounds.Event.ERROR);
      return;
    }
    if (slot != PresetMenu.BACK_SLOT && slot != PresetMenu.SERVER_SLOT
        && slot != PresetMenu.COMMUNITY_SLOT) return;
    if (slot == PresetMenu.COMMUNITY_SLOT && !plugin.isCommunityPresetsEnabled()) return;
    GuiSounds.play(plugin, player, GuiSounds.Event.NAVIGATE);
    plugin
        .getServer()
        .getScheduler()
        .runTask(
            plugin,
            () -> {
              if (!player.isOnline() || !GuiUtils.allowed(plugin, player)) return;
              if (slot == PresetMenu.BACK_SLOT) new ChromaticaMenu(plugin).open(player);
              else if (slot == PresetMenu.SERVER_SLOT)
                new PresetBrowserMenu(plugin, false).open(player, 0);
              else if (slot == PresetMenu.COMMUNITY_SLOT && plugin.isCommunityPresetsEnabled())
                new PresetBrowserMenu(plugin, true).open(player, 0);
            });
  }
}
