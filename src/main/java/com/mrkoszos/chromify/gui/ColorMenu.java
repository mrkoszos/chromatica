package com.mrkoszos.chromify.gui;

import com.mrkoszos.chromify.Chromify;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class ColorMenu {

  public static final String TITLE = "Color";

  private final Chromify plugin;

  public ColorMenu(Chromify plugin) {
    this.plugin = plugin;
  }

  public void open(Player player) {

    Inventory inventory = plugin.getServer().createInventory(null, 27, TITLE);

    inventory.setItem(10, createItem(Material.RED_DYE, ChatColor.RED + "Red"));

    inventory.setItem(11, createItem(Material.ORANGE_DYE, ChatColor.GOLD + "Orange"));

    inventory.setItem(12, createItem(Material.YELLOW_DYE, ChatColor.YELLOW + "Yellow"));

    inventory.setItem(13, createItem(Material.LIME_DYE, ChatColor.GREEN + "Lime"));

    inventory.setItem(14, createItem(Material.CYAN_DYE, ChatColor.AQUA + "Cyan"));

    inventory.setItem(15, createItem(Material.BLUE_DYE, ChatColor.BLUE + "Blue"));

    inventory.setItem(16, createItem(Material.PURPLE_DYE, ChatColor.LIGHT_PURPLE + "Purple"));

    inventory.setItem(
        22,
        createItem(
            Material.NETHER_STAR,
            ChatColor.LIGHT_PURPLE + "Custom Color",
            ChatColor.GRAY + "Choose a custom HEX color."));

    inventory.setItem(26, createItem(Material.BARRIER, ChatColor.RED + "Back"));

    /*
     * =========================
     *        BACKGROUND
     * =========================
     */

    GuiUtils.fillEmptySlots(inventory);

    player.openInventory(inventory);
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
