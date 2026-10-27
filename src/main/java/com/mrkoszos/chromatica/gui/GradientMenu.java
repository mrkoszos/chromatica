package com.mrkoszos.chromatica.gui;

import com.mrkoszos.chromatica.Chromatica;
import java.util.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;

public class GradientMenu {
  public static final Component TITLE = Component.text("Gradient");
  public static final int[] GRADIENT_SLOTS = {
    10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34
  };
  public static final int APPLY_SLOT = 21, BACK_SLOT = 23, ADD_SLOT = 22;

  public static boolean expandedLayout(String value, java.util.logging.Logger logger) {
    if ("expanded".equalsIgnoreCase(value)) return true;
    if (value != null && !"compact".equalsIgnoreCase(value))
      logger.warning("Invalid gui.gradient.layout; using compact (valid: compact, expanded).");
    return false;
  }

  private final Chromatica plugin;

  public GradientMenu(Chromatica plugin) {
    this.plugin = plugin;
  }

  public void open(Player player) {
    var colors = plugin.getGradientSession().get(player.getUniqueId());
    if (colors == null) {
      var saved = plugin.getNameStyleManager().getStyle(player.getUniqueId()).getGradientColors();
      plugin
          .getGradientSession()
          .start(player.getUniqueId(), saved.size() >= 2 ? saved : List.of("#FF0000", "#0000FF"));
      colors = plugin.getGradientSession().get(player.getUniqueId());
    }
    int size = plugin.isExpandedGradient() || colors.size() > 7 ? 45 : 27;
    Inventory inventory = GuiUtils.inventory(plugin, size, TITLE, "GradientMenu");
    refresh(player, inventory);
    GuiUtils.open(player, inventory);
  }

  public static int capacity(Inventory inventory) {
    return inventory.getSize() >= 45 ? GRADIENT_SLOTS.length : 7;
  }

  public static int applySlot(Inventory inventory) {
    return capacity(inventory) > 7 ? 39 : APPLY_SLOT;
  }

  public static int addSlot(Inventory inventory) {
    return capacity(inventory) > 7 ? 40 : ADD_SLOT;
  }

  public static int backSlot(Inventory inventory) {
    return capacity(inventory) > 7 ? 41 : BACK_SLOT;
  }

  public void refresh(Player player, Inventory inventory) {
    inventory.clear();
    var style = plugin.getNameStyleManager().getStyle(player.getUniqueId());
    var colors = plugin.getGradientSession().get(player.getUniqueId());
    if (colors == null) {
      plugin
          .getGradientSession()
          .start(
              player.getUniqueId(),
              style.getGradientColors().size() >= 2
                  ? style.getGradientColors()
                  : List.of("#FF0000", "#0000FF"));
      colors = plugin.getGradientSession().get(player.getUniqueId());
    }
    var head = GuiHeads.playerHead(player, Material.ENDER_EYE);
    var meta = head.getItemMeta();
    meta.displayName(
        plugin
            .getNameRenderer()
            .renderGradientPreview(
                player.getName(),
                colors,
                style,
                player.hasPermission("chromatica.format.scramble")));
    head.setItemMeta(meta);
    inventory.setItem(4, head);
    for (int i = 0; i < Math.min(colors.size(), capacity(inventory)); i++) {
      String hex = colors.get(i);
      inventory.setItem(
          GRADIENT_SLOTS[i],
          GuiUtils.item(
              DyeColors.nearest(hex),
              Component.text(
                  "Color " + (i + 1), net.kyori.adventure.text.format.TextColor.fromHexString(hex)),
              GuiUtils.text(hex, NamedTextColor.GRAY),
              GuiUtils.text("Right-click: remove", NamedTextColor.RED)));
    }
    inventory.setItem(
        applySlot(inventory),
        GuiUtils.item(
            Material.KNOWLEDGE_BOOK,
            GuiUtils.text("Apply Gradient", NamedTextColor.GREEN)));
    inventory.setItem(
        backSlot(inventory),
        GuiUtils.item(Material.BARRIER, GuiUtils.text("Back", NamedTextColor.RED)));
    inventory.setItem(addSlot(inventory),
        GuiUtils.item(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE,
            GuiUtils.text("Add Color", NamedTextColor.GREEN)));
    GuiUtils.mark(
        inventory, applySlot(inventory), GuiSelection.sameColors(style, GuiSelection.colors(colors)));
    GuiUtils.fillEmptySlots(inventory, plugin);
    // Empty stops are intentionally empty, including in the second row of expanded mode.
    for (int i = colors.size(); i < capacity(inventory); i++)
      inventory.setItem(GRADIENT_SLOTS[i], null);
  }
}
