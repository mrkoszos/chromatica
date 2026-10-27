package com.mrkoszos.chromatica.gui;

import com.mrkoszos.chromatica.Chromatica;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class ChromaticaMenuListener implements Listener {

  private final Chromatica plugin;

  public ChromaticaMenuListener(Chromatica plugin) {
    this.plugin = plugin;
  }

  @EventHandler
  public void onInventoryClick(InventoryClickEvent event) {

    if (!GuiUtils.isMenu(event.getView().getTopInventory(), "ChromaticaMenu")) {
      return;
    }

    event.setCancelled(true);
    if (!GuiUtils.isActionClick(event)) return;
    if (event.getRawSlot() == 44) {
      plugin
          .getServer()
          .getScheduler()
          .runTask(plugin, () -> event.getWhoClicked().closeInventory());
      return;
    }

    if (!(event.getWhoClicked() instanceof Player player)) {
      return;
    }

    if (!GuiUtils.allowed(plugin, player)) return;

    if (event.getCurrentItem() == null) {
      return;
    }

    switch (event.getRawSlot()) {
      case ChromaticaMenu.CREDIT_SLOT -> {
        if (plugin.getGuiBranding() == null || plugin.getGuiBranding().showCredit())
          player.sendMessage(
              GuiUtils.text(
                  "Chromatica " + plugin.getPluginMeta().getVersion() + " by mk",
                  NamedTextColor.GRAY));
      }
      case 19 -> { // COLOR
        GuiUtils.navigate(plugin, player, () -> new ColorMenu(plugin).open(player));
      }

      case 20 -> { // GRADIENT
        GuiUtils.navigate(plugin, player, () -> new GradientMenu(plugin).open(player));
      }

      case 22 -> { // FAVORITES
        GuiUtils.navigate(plugin, player, () -> new FavoritesMenu(plugin).open(player));
      }

      case 24 -> { // PRESETS
        GuiUtils.navigate(plugin, player, () -> new PresetMenu(plugin).open(player));
      }

      case 25 -> { // FORMATTING
        GuiUtils.navigate(plugin, player, () -> new FormattingMenu(plugin).open(player));
      }

      case 31 -> { // RESET
        if (event.getClick() == org.bukkit.event.inventory.ClickType.LEFT)
          InlineConfirmation.reset(plugin, player, player.getUniqueId(), "chromatica.use", false);
      }

      default -> {}
    }
  }
}
