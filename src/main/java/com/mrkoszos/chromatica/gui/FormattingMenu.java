package com.mrkoszos.chromatica.gui;

import com.mrkoszos.chromatica.Chromatica;
import com.mrkoszos.chromatica.model.NameStyle;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class FormattingMenu {

  public static final String TITLE = "Formatting";

  private final Chromatica plugin;

  public FormattingMenu(Chromatica plugin) {
    this.plugin = plugin;
  }

  public void open(Player player) {

    NameStyle style = plugin.getNameStyleManager().getStyle(player.getUniqueId());

    Inventory inventory = plugin.getServer().createInventory(null, 27, TITLE);

    /*
     * =========================
     *          PREVIEW
     * =========================
     */

    Component preview = plugin.getNameRenderer().render(player.getName(), style);

    ItemStack previewItem = new ItemStack(Material.ENDER_EYE);

    ItemMeta previewMeta = previewItem.getItemMeta();

    previewMeta.displayName(preview);

    previewMeta.lore(
        List.of(
            Component.text("Current name preview.")
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false)));

    previewItem.setItemMeta(previewMeta);

    inventory.setItem(4, previewItem);

    /*
     * =========================
     *           BOLD
     * =========================
     */

    inventory.setItem(10, createToggleItem(Material.GOLD_INGOT, "Bold", style.isBold()));

    /*
     * =========================
     *          ITALIC
     * =========================
     */

    inventory.setItem(12, createToggleItem(Material.WRITABLE_BOOK, "Italic", style.isItalic()));

    /*
     * =========================
     *         UNDERLINE
     * =========================
     */

    inventory.setItem(14, createToggleItem(Material.PAPER, "Underline", style.isUnderlined()));

    /*
     * =========================
     *       STRIKETHROUGH
     * =========================
     */

    inventory.setItem(
        16, createToggleItem(Material.IRON_SWORD, "Strikethrough", style.isStrikethrough()));

    /*
     * =========================
     *            BACK
     * =========================
     */

    inventory.setItem(
        22,
        createItem(
            Material.BARRIER, ChatColor.RED + "Back", ChatColor.GRAY + "Return to the main menu."));

    /*
     * =========================
     *        BACKGROUND
     * =========================
     */

    GuiUtils.fillEmptySlots(inventory, plugin);

    player.openInventory(inventory);
  }

  private ItemStack createToggleItem(Material material, String name, boolean enabled) {

    ItemStack item = new ItemStack(material);

    ItemMeta meta = item.getItemMeta();

    Component displayName = Component.text(name).decoration(TextDecoration.ITALIC, false);

    if (enabled) {

      displayName = displayName.decoration(TextDecoration.BOLD, true);
    }

    meta.displayName(displayName);

    if (enabled) {

      meta.lore(
          List.of(
              Component.text("Enabled", NamedTextColor.GREEN)
                  .decoration(TextDecoration.ITALIC, false),
              Component.empty(),
              Component.text("Click to disable.", NamedTextColor.YELLOW)
                  .decoration(TextDecoration.ITALIC, false)));

    } else {

      meta.lore(
          List.of(
              Component.text("Disabled", NamedTextColor.RED)
                  .decoration(TextDecoration.ITALIC, false),
              Component.empty(),
              Component.text("Click to enable.", NamedTextColor.YELLOW)
                  .decoration(TextDecoration.ITALIC, false)));
    }

    item.setItemMeta(meta);

    return item;
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
