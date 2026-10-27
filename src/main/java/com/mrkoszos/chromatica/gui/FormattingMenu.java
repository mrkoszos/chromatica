package com.mrkoszos.chromatica.gui;

import com.mrkoszos.chromatica.Chromatica;
import com.mrkoszos.chromatica.model.NameStyle;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class FormattingMenu {

  public static final net.kyori.adventure.text.Component TITLE =
      net.kyori.adventure.text.Component.text("Formatting");

  public static final int BACK_SLOT = 22;
  private final Chromatica plugin;

  public FormattingMenu(Chromatica plugin) {
    this.plugin = plugin;
  }

  public void open(Player player) {
    Inventory inventory = GuiUtils.inventory(plugin, 27, TITLE, "FormattingMenu");
    refresh(player, inventory);
    GuiUtils.open(player, inventory);
  }

  public void refresh(Player player, Inventory inventory) {
    inventory.clear();

    NameStyle style = plugin.getNameStyleManager().getStyle(player.getUniqueId());

    /*
     * =========================
     *          PREVIEW
     * =========================
     */

    Component preview =
        plugin
            .getNameRenderer()
            .render(player.getName(), style, player.hasPermission("chromatica.format.scramble"));

    ItemStack previewItem = GuiHeads.playerHead(player, Material.ENDER_EYE);

    ItemMeta previewMeta = previewItem.getItemMeta();

    previewMeta.displayName(preview);

    previewItem.setItemMeta(previewMeta);

    inventory.setItem(4, previewItem);

    /*
     * =========================
     *           BOLD
     * =========================
     */

    inventory.setItem(11, createToggleItem(Material.MACE, "Bold", style.isBold()));

    /*
     * =========================
     *          ITALIC
     * =========================
     */

    inventory.setItem(
        12,
        createToggleItem(
            style.isItalic() ? Material.WRITTEN_BOOK : Material.WRITABLE_BOOK,
            "Italic",
            style.isItalic()));

    /*
     * =========================
     *         UNDERLINE
     * =========================
     */

    inventory.setItem(15, createToggleItem(Material.STICK, "Underline", style.isUnderlined()));

    /*
     * =========================
     *       STRIKETHROUGH
     * =========================
     */

    inventory.setItem(
        14, createToggleItem(Material.SHEARS, "Strikethrough", style.isStrikethrough()));

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
     *        BACKGROUND
     * =========================
     */

    inventory.setItem(
        13,
        createToggleItem(
            Material.BRUSH,
            "Scramble",
            style.isScramble() && player.hasPermission("chromatica.format.scramble")));
    GuiUtils.fillEmptySlots(inventory, plugin);
  }

  private ItemStack createToggleItem(Material material, String name, boolean enabled) {

    ItemStack item = new ItemStack(material);

    ItemMeta meta = item.getItemMeta();

    Component displayName =
        Component.text(name, NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false);

    if (enabled) {

      displayName = displayName.decoration(TextDecoration.BOLD, true);
    }

    meta.displayName(displayName);

    meta.setEnchantmentGlintOverride(enabled);
    item.setItemMeta(meta);

    return item;
  }
}
