package com.mrkoszos.chromatica.gui;

import com.mrkoszos.chromatica.Chromatica;
import com.mrkoszos.chromatica.model.NameStyle;
import com.mrkoszos.chromatica.model.StyleType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class ColorMenuListener implements Listener {

  private final Chromatica plugin;

  public ColorMenuListener(Chromatica plugin) {
    this.plugin = plugin;
  }

  @EventHandler
  public void onInventoryClick(InventoryClickEvent event) {

    if (!event.getView().getTitle().equals(ColorMenu.TITLE)) {
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

    NameStyle style = plugin.getNameStyleManager().getStyle(player.getUniqueId());

    switch (material) {
      case RED_DYE -> setColor(player, style, "#FF5555", "red");

      case ORANGE_DYE -> setColor(player, style, "#FFAA00", "orange");

      case YELLOW_DYE -> setColor(player, style, "#FFFF55", "yellow");

      case LIME_DYE -> setColor(player, style, "#55FF55", "lime");

      case CYAN_DYE -> setColor(player, style, "#55FFFF", "cyan");

      case BLUE_DYE -> setColor(player, style, "#5555FF", "blue");

      case PURPLE_DYE -> setColor(player, style, "#AA55FF", "purple");

      case NETHER_STAR -> {
        player.closeInventory();

        new HexColorMenu(plugin)
                .open(
                        player,
                        hex -> {
                          style.setType(StyleType.SOLID);

                          style.setPrimaryColor(hex);

                          plugin.getNameStyleManager()
                                  .saveStyle(player.getUniqueId());

                          player.sendActionBar(
                                  Component.text("Your name color has been changed.", NamedTextColor.GREEN));
                        });
      }

      case BARRIER -> {
        player.closeInventory();

        new ChromaticaMenu(plugin).open(player);
      }

      default -> {}
    }
  }

  private void setColor(Player player, NameStyle style, String hex, String name) {

    style.setType(StyleType.SOLID);
    style.setPrimaryColor(hex);

    plugin.getNameStyleManager()
            .saveStyle(player.getUniqueId());

    player.sendActionBar(
            Component.text("Your name color is now " + name + ".", NamedTextColor.GREEN));

    player.closeInventory();
  }
}