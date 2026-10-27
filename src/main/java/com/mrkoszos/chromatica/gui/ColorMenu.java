package com.mrkoszos.chromatica.gui;

import com.mrkoszos.chromatica.Chromatica;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class ColorMenu {

  public static final net.kyori.adventure.text.Component TITLE =
      net.kyori.adventure.text.Component.text("Color");

  public static final int HEX_SLOT = 17;
  public static final int BACK_SLOT = 22;

  private final Chromatica plugin;

  public ColorMenu(Chromatica plugin) {
    this.plugin = plugin;
  }

  public void open(Player player) {

    Inventory inventory = GuiUtils.inventory(plugin, 27, TITLE, "ColorMenu");

    inventory.setItem(
        10,
        GuiUtils.item(
            Material.RED_DYE,
            GuiUtils.text("Red", net.kyori.adventure.text.format.NamedTextColor.RED)));

    inventory.setItem(
        11,
        GuiUtils.item(
            Material.ORANGE_DYE,
            GuiUtils.text("Orange", net.kyori.adventure.text.format.NamedTextColor.GOLD)));

    inventory.setItem(
        12,
        GuiUtils.item(
            Material.YELLOW_DYE,
            GuiUtils.text("Yellow", net.kyori.adventure.text.format.NamedTextColor.YELLOW)));

    inventory.setItem(
        13,
        GuiUtils.item(
            Material.LIME_DYE,
            GuiUtils.text("Lime", net.kyori.adventure.text.format.NamedTextColor.GREEN)));

    inventory.setItem(
        14,
        GuiUtils.item(
            Material.CYAN_DYE,
            GuiUtils.text("Cyan", net.kyori.adventure.text.format.NamedTextColor.AQUA)));

    inventory.setItem(
        15,
        GuiUtils.item(
            Material.BLUE_DYE,
            GuiUtils.text("Blue", net.kyori.adventure.text.format.NamedTextColor.BLUE)));

    inventory.setItem(
        16,
        GuiUtils.item(
            Material.PURPLE_DYE,
            GuiUtils.text("Purple", net.kyori.adventure.text.format.NamedTextColor.LIGHT_PURPLE)));

    inventory.setItem(
        HEX_SLOT,
        GuiUtils.item(
            Material.LIME_STAINED_GLASS_PANE,
            GuiUtils.text("Custom Color", net.kyori.adventure.text.format.NamedTextColor.YELLOW)));

    inventory.setItem(
        BACK_SLOT,
        GuiUtils.item(
            Material.BARRIER,
            GuiUtils.text("Back", net.kyori.adventure.text.format.NamedTextColor.RED)));

    /*
     * =========================
     *        BACKGROUND
     * =========================
     */

    var style = plugin.getNameStyleManager().getStyle(player.getUniqueId());
    GuiSelection.markColors(
        inventory,
        style.getType() == com.mrkoszos.chromatica.model.StyleType.SOLID
            ? style.getPrimaryColor()
            : null);
    var head = GuiHeads.playerHead(player, Material.ENDER_EYE);
    var meta = head.getItemMeta();
    meta.displayName(
        plugin
            .getNameRenderer()
            .render(player.getName(), style, player.hasPermission("chromatica.format.scramble")));
    head.setItemMeta(meta);
    inventory.setItem(4, head);
    GuiUtils.fillEmptySlots(inventory, plugin);

    GuiUtils.open(player, inventory);
  }
}
