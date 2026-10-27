package com.mrkoszos.chromatica.gui;

import com.mrkoszos.chromatica.Chromatica;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class ColorSelectorMenu {

  public static final net.kyori.adventure.text.Component TITLE =
      net.kyori.adventure.text.Component.text("Select Color");

  public static final int CLOSE_SLOT = 21;
  public static final int HEX_SLOT = 22;
  public static final int BACK_SLOT = 23;

  private final Chromatica plugin;

  public ColorSelectorMenu(Chromatica plugin) {
    this.plugin = plugin;
  }

  public void open(Player player, ColorSelectionContext context, int gradientIndex) {

    plugin.getColorSelectorSession().set(player.getUniqueId(), context);
    if (context == ColorSelectionContext.GRADIENT)
      plugin.getGradientColorSelection().put(player.getUniqueId(), gradientIndex);

    Inventory inventory = GuiUtils.inventory(plugin, 27, TITLE, "ColorSelectorMenu");

    inventory.setItem(
        10,
        createColor(
            Material.RED_DYE,
            GuiUtils.text("Red", net.kyori.adventure.text.format.NamedTextColor.RED),
            "#FF5555"));

    inventory.setItem(
        11,
        createColor(
            Material.ORANGE_DYE,
            GuiUtils.text("Orange", net.kyori.adventure.text.format.NamedTextColor.GOLD),
            "#FFAA00"));

    inventory.setItem(
        12,
        createColor(
            Material.YELLOW_DYE,
            GuiUtils.text("Yellow", net.kyori.adventure.text.format.NamedTextColor.YELLOW),
            "#FFFF55"));

    inventory.setItem(
        13,
        createColor(
            Material.LIME_DYE,
            GuiUtils.text("Lime", net.kyori.adventure.text.format.NamedTextColor.GREEN),
            "#55FF55"));

    inventory.setItem(
        14,
        createColor(
            Material.CYAN_DYE,
            GuiUtils.text("Cyan", net.kyori.adventure.text.format.NamedTextColor.AQUA),
            "#55FFFF"));

    inventory.setItem(
        15,
        createColor(
            Material.BLUE_DYE,
            GuiUtils.text("Blue", net.kyori.adventure.text.format.NamedTextColor.BLUE),
            "#5555FF"));

    inventory.setItem(
        16,
        createColor(
            Material.PURPLE_DYE,
            GuiUtils.text("Purple", net.kyori.adventure.text.format.NamedTextColor.LIGHT_PURPLE),
            "#AA55FF"));

    inventory.setItem(
        HEX_SLOT,
        GuiUtils.item(
            Material.NETHER_STAR,
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

    String current = null;
    if (context == ColorSelectionContext.GRADIENT) {
      var colors = plugin.getGradientSession().get(player.getUniqueId());
      if (colors != null && gradientIndex >= 0 && gradientIndex < colors.size()) current = colors.get(gradientIndex);
    } else {
      var style = plugin.getNameStyleManager().getStyle(player.getUniqueId());
      if (style.getType() == com.mrkoszos.chromatica.model.StyleType.SOLID) current = style.getPrimaryColor();
    }
    GuiSelection.markColors(inventory, current);
    inventory.setItem(CLOSE_SLOT, GuiUtils.closeItem());
    GuiUtils.fillEmptySlots(inventory, plugin);

    GuiUtils.open(player, inventory);
  }

  private ItemStack createColor(
      Material material, net.kyori.adventure.text.Component name, String hex) {

    return GuiUtils.item(
        material, name, GuiUtils.text(hex, net.kyori.adventure.text.format.NamedTextColor.GRAY));
  }
}
