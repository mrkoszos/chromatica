package com.mrkoszos.chromify.gui;

import com.mrkoszos.chromify.Chromify;
import com.mrkoszos.chromify.model.NameStyle;
import com.mrkoszos.chromify.model.StyleType;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;

public class ColorSelectorListener implements Listener {

  private final Chromify plugin;

  public ColorSelectorListener(Chromify plugin) {
    this.plugin = plugin;
  }

  @EventHandler
  public void onInventoryClick(InventoryClickEvent event) {

    if (!event.getView().getTitle().equals(ColorSelectorMenu.TITLE)) {
      return;
    }

    event.setCancelled(true);

    if (!(event.getWhoClicked() instanceof Player player)) {
      return;
    }

    if (event.getCurrentItem() == null) {
      return;
    }

    Material material = event.getCurrentItem().getType();

    String hex =
        switch (material) {
          case RED_DYE -> "#FF5555";
          case ORANGE_DYE -> "#FFAA00";
          case YELLOW_DYE -> "#FFFF55";
          case LIME_DYE -> "#55FF55";
          case CYAN_DYE -> "#55FFFF";
          case BLUE_DYE -> "#5555FF";
          case PURPLE_DYE -> "#AA55FF";

          default -> null;
        };

    if (hex != null) {

      applyColor(player, hex);

      return;
    }

    if (material == Material.NETHER_STAR) {

      player.closeInventory();

      new HexColorMenu(plugin).open(player, hexColor -> applyColor(player, hexColor));

      return;
    }

    if (material == Material.BARRIER) {

      plugin.getColorSelectorSession().remove(player.getUniqueId());

      player.closeInventory();

      new GradientMenu(plugin).open(player);
    }
  }

  private void applyColor(Player player, String hex) {

    ColorSelectionContext context = plugin.getColorSelectorSession().get(player.getUniqueId());

    if (context == null) {
      return;
    }

    NameStyle style = plugin.getNameStyleManager().getStyle(player.getUniqueId());

    switch (context) {
      case GRADIENT -> {
        Integer index = plugin.getGradientColorSelection().get(player.getUniqueId());

        if (index == null) {
          return;
        }

        plugin.getGradientSession().setColor(player.getUniqueId(), index, hex);

        plugin.getGradientColorSelection().remove(player.getUniqueId());

        player.closeInventory();

        new GradientMenu(plugin).open(player);
      }
    }

    plugin.getColorSelectorSession().remove(player.getUniqueId());
  }

  private void setGradientColor(NameStyle style, int index, String hex) {

    List<String> colors = style.getGradientColors();

    while (colors.size() <= index) {
      colors.add("#FFFFFF");
    }

    colors.set(index, hex);

    style.setGradientColors(colors);

    style.setType(StyleType.GRADIENT);
  }
}
