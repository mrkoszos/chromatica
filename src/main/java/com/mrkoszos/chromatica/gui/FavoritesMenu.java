package com.mrkoszos.chromatica.gui;

import com.mrkoszos.chromatica.Chromatica;
import com.mrkoszos.chromatica.model.NameStyle;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class FavoritesMenu {

  public static final net.kyori.adventure.text.Component TITLE =
      net.kyori.adventure.text.Component.text("Favorites");

  private static final int PREVIEW_SLOT = 4;
  public static final int[] FAVORITE_SLOTS = {11, 12, 13, 14, 15};
  public static final int BACK_SLOT = 22;

  private final Chromatica plugin;

  public FavoritesMenu(Chromatica plugin) {
    this.plugin = plugin;
  }

  public void open(Player player) {
    Inventory inventory = GuiUtils.inventory(plugin, 27, TITLE, "FavoritesMenu");
    refresh(player, inventory);
    GuiUtils.open(player, inventory);
  }

  public void refresh(Player player, Inventory inventory) {
    inventory.clear();

    NameStyle[] favorites = plugin.getFavoriteManager().getAll(player.getUniqueId());

    /*
     * =========================
     *          PREVIEW
     * =========================
     */

    NameStyle currentStyle = plugin.getNameStyleManager().getStyle(player.getUniqueId());

    Component preview =
        plugin
            .getNameRenderer()
            .render(
                player.getName(), currentStyle, player.hasPermission("chromatica.format.scramble"));

    ItemStack previewItem = GuiHeads.playerHead(player, Material.ENDER_EYE);

    ItemMeta previewMeta = previewItem.getItemMeta();

    previewMeta.displayName(preview);

    previewItem.setItemMeta(previewMeta);

    inventory.setItem(PREVIEW_SLOT, previewItem);

    /*
     * =========================
     *       FAVORITE SLOTS
     * =========================
     */

    var candidates = new java.util.LinkedHashMap<String, NameStyle>();
    for (int i = 0; i < favorites.length; i++)
      if (favorites[i] != null) candidates.put(String.valueOf(i), favorites[i]);
    String selected =
        GuiSelection.selected(
            plugin, player.getUniqueId(), "favorite", currentStyle, candidates, false);
    for (int i = 0; i < FAVORITE_SLOTS.length; i++) {

      inventory.setItem(
          FAVORITE_SLOTS[i],
          GuiUtils.selected(
              createFavoriteItem(player, favorites[i], i), String.valueOf(i).equals(selected)));
    }

    /*
     * =========================
     *            BACK
     * =========================
     */

    inventory.setItem(
        BACK_SLOT,
        GuiUtils.item(
            Material.BARRIER,
            GuiUtils.text("Back", net.kyori.adventure.text.format.NamedTextColor.RED)));

    /*
     * =========================
     *           CLOSE
     * =========================
     */

    /*
     * =========================
     *        BACKGROUND
     * =========================
     */

    GuiUtils.fillEmptySlots(inventory, plugin);
  }

  /*
   * =========================
   *      FAVORITE ITEM
   * =========================
   */

  private ItemStack createFavoriteItem(Player player, NameStyle style, int index) {

    if (style == null) {

      return GuiUtils.item(
          Material.ITEM_FRAME,
          GuiUtils.text(
              "Favorite " + (index + 1) + " (Empty)",
              net.kyori.adventure.text.format.NamedTextColor.GREEN),
          GuiUtils.text("Save current style", net.kyori.adventure.text.format.NamedTextColor.GRAY));
    }

    ItemStack item = new ItemStack(Material.GLOW_ITEM_FRAME);

    ItemMeta meta = item.getItemMeta();

    Component preview =
        plugin
            .getNameRenderer()
            .render(player.getName(), style, player.hasPermission("chromatica.format.scramble"));

    meta.displayName(preview);

    List<Component> lore = new ArrayList<>();

    lore.add(
        Component.text("Favorite " + (index + 1), NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false));

    lore.add(
        Component.text("Drop: remove", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));

    meta.lore(lore);

    item.setItemMeta(meta);

    return item;
  }

  /*
   * =========================
   *         GENERIC ITEM
   * =========================
   */

}
