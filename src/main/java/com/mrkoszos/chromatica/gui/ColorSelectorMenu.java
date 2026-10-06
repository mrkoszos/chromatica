package com.mrkoszos.chromatica.gui;

import com.mrkoszos.chromatica.Chromatica;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class ColorSelectorMenu {

  public static final String TITLE = "Select Color";

  private final Chromatica plugin;

  public ColorSelectorMenu(Chromatica plugin) {
    this.plugin = plugin;
  }

  public void open(Player player, ColorSelectionContext context, int gradientIndex) {

    plugin.getColorSelectorSession().set(player.getUniqueId(), context);

    Inventory inventory = plugin.getServer().createInventory(null, 27, TITLE);

    inventory.setItem(10, createColor(Material.RED_DYE, ChatColor.RED + "Red", "#FF5555"));

    inventory.setItem(11, createColor(Material.ORANGE_DYE, ChatColor.GOLD + "Orange", "#FFAA00"));

    inventory.setItem(12, createColor(Material.YELLOW_DYE, ChatColor.YELLOW + "Yellow", "#FFFF55"));

    inventory.setItem(13, createColor(Material.LIME_DYE, ChatColor.GREEN + "Lime", "#55FF55"));

    inventory.setItem(14, createColor(Material.CYAN_DYE, ChatColor.AQUA + "Cyan", "#55FFFF"));

    inventory.setItem(15, createColor(Material.BLUE_DYE, ChatColor.BLUE + "Blue", "#5555FF"));

    inventory.setItem(
        16, createColor(Material.PURPLE_DYE, ChatColor.LIGHT_PURPLE + "Purple", "#AA55FF"));

    inventory.setItem(
        22,
        createItem(
            Material.NETHER_STAR,
            ChatColor.LIGHT_PURPLE + "Custom Color",
            ChatColor.GRAY + "Enter a HEX color."));

    inventory.setItem(26, createItem(Material.BARRIER, ChatColor.RED + "Back"));

    /*
     * =========================
     *        BACKGROUND
     * =========================
     */

    GuiUtils.fillEmptySlots(inventory, plugin);

    player.openInventory(inventory);
  }

  private ItemStack createColor(Material material, String name, String hex) {

    return createItem(material, name, ChatColor.GRAY + hex);
  }

  private ItemStack createItem(Material material, String name, String... lore) {

    ItemStack item = new ItemStack(material);

    ItemMeta meta = item.getItemMeta();

    meta.setDisplayName(name);

    if (lore.length > 0) {
      meta.setLore(List.of(lore));
    }

    item.setItemMeta(meta);

    return item;
  }
}
