package com.mrkoszos.chromatica.gui;

import com.mrkoszos.chromatica.Chromatica;
import com.mrkoszos.chromatica.model.NameStyle;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class ChromaticaMenu {

  public static final Component TITLE =
      Component.text("ℹ Chromatica")
          .decoration(TextDecoration.BOLD, false)
          .decoration(TextDecoration.ITALIC, false);

  private static final int PREVIEW_SLOT = 13;
  private static final int COLOR_SLOT = 19;
  private static final int GRADIENT_SLOT = 20;
  private static final int PRESETS_SLOT = 24;
  private static final int FORMATTING_SLOT = 25;
  private static final int RESET_SLOT = 31;
  private static final int FAVORITES_SLOT = 22;

  private final Chromatica plugin;

  public ChromaticaMenu(Chromatica plugin) {
    this.plugin = plugin;
  }

  public void open(Player player) {

    NameStyle style = plugin.getNameStyleManager().getStyle(player.getUniqueId());

    Inventory inventory = plugin.getServer().createInventory(null, 45, TITLE);

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
            Component.text("Your current name style.")
                .color(TextColor.fromHexString("#AAAAAA"))
                .decoration(TextDecoration.ITALIC, false)));

    previewItem.setItemMeta(previewMeta);

    inventory.setItem(PREVIEW_SLOT, previewItem);

    /*
     * =========================
     *           COLOR
     * =========================
     */

    Component colorName =
        Component.text("Color")
            .color(TextColor.fromHexString("#FF00AA"))
            .decoration(TextDecoration.ITALIC, false);

    Component colorLore =
        Component.text("Choose a custom name color.")
            .color(TextColor.fromHexString("#AAAAAA"))
            .decoration(TextDecoration.ITALIC, false);

    inventory.setItem(COLOR_SLOT, createItem(Material.MAGENTA_DYE, colorName, colorLore));

    /*
     * =========================
     *         GRADIENT
     * =========================
     */

    Component gradientName = createGradientText("Gradient", "#FCFEA9", "#33775A");

    Component gradientLore =
        Component.text("Create a multi-color gradient.")
            .color(TextColor.fromHexString("#AAAAAA"))
            .decoration(TextDecoration.ITALIC, false);

    inventory.setItem(
        GRADIENT_SLOT, createItem(Material.EXPERIENCE_BOTTLE, gradientName, gradientLore));

    /*
     * =========================
     *          PRESETS
     * =========================
     */

    Component presetsName =
        Component.text("Presets")
            .color(TextColor.fromHexString("#FFD83D"))
            .decoration(TextDecoration.ITALIC, false);

    Component presetsLore =
        Component.text("Choose from predefined styles.")
            .color(TextColor.fromHexString("#AAAAAA"))
            .decoration(TextDecoration.ITALIC, false);

    inventory.setItem(
        PRESETS_SLOT, createItem(Material.PRIZE_POTTERY_SHERD, presetsName, presetsLore));

    /*
     * =========================
     *          FAVORITES
     * =========================
     */

    Component favoritesName =
        Component.text("Favorites")
            .color(TextColor.fromHexString("#55FF55"))
            .decoration(TextDecoration.ITALIC, false);

    Component favoritesLore =
        Component.text("Save and quickly apply your favorite styles.")
            .color(TextColor.fromHexString("#AAAAAA"))
            .decoration(TextDecoration.ITALIC, false);

    inventory.setItem(
            FAVORITES_SLOT,
            createItem(Material.GLOW_ITEM_FRAME, favoritesName, favoritesLore));

    /*
     * =========================
     *        FORMATTING
     * =========================
     */

    Component formattingName =
        Component.text("Formatting")
            .color(TextColor.fromHexString("#9B7BFF"))
            .decoration(TextDecoration.ITALIC, false);

    Component formattingLore1 =
        Component.text("Bold, italic, underline")
            .color(TextColor.fromHexString("#AAAAAA"))
            .decoration(TextDecoration.ITALIC, false);

    Component formattingLore2 =
        Component.text("and strikethrough.")
            .color(TextColor.fromHexString("#AAAAAA"))
            .decoration(TextDecoration.ITALIC, false);

    inventory.setItem(
        FORMATTING_SLOT,
        createItem(Material.WRITABLE_BOOK, formattingName, formattingLore1, formattingLore2));

    /*
     * =========================
     *           RESET
     * =========================
     */

    Component resetName =
        Component.text("Reset")
            .color(TextColor.fromHexString("#FF5555"))
            .decoration(TextDecoration.ITALIC, false);

    Component resetLore =
        Component.text("Reset your name style.")
            .color(TextColor.fromHexString("#AAAAAA"))
            .decoration(TextDecoration.ITALIC, false);

    inventory.setItem(RESET_SLOT, createItem(Material.MILK_BUCKET, resetName, resetLore));

    /*
     * =========================
     *        BACKGROUND
     * =========================
     */

    GuiUtils.fillEmptySlots(inventory, plugin);

    player.openInventory(inventory);
  }

  /*
   * =========================
   *       GRADIENT TEXT
   * =========================
   */

  private Component createGradientText(String text, String startHex, String endHex) {

    TextColor start = TextColor.fromHexString(startHex);

    TextColor end = TextColor.fromHexString(endHex);

    if (start == null || end == null) {
      return Component.text(text).decoration(TextDecoration.ITALIC, false);
    }

    if (text.length() == 1) {
      return Component.text(text).color(start).decoration(TextDecoration.ITALIC, false);
    }

    Component result = Component.empty();

    for (int i = 0; i < text.length(); i++) {

      double progress = (double) i / (text.length() - 1);

      int red = (int) (start.red() + (end.red() - start.red()) * progress);

      int green = (int) (start.green() + (end.green() - start.green()) * progress);

      int blue = (int) (start.blue() + (end.blue() - start.blue()) * progress);

      TextColor color = TextColor.color(red, green, blue);

      result =
          result.append(
              Component.text(String.valueOf(text.charAt(i)))
                  .color(color)
                  .decoration(TextDecoration.ITALIC, false));
    }

    return result;
  }

  /*
   * =========================
   *      COMPONENT ITEM
   * =========================
   */

  private ItemStack createItem(Material material, Component name, Component... lore) {

    ItemStack item = new ItemStack(material);

    ItemMeta meta = item.getItemMeta();

    meta.displayName(name.decoration(TextDecoration.ITALIC, false));

    if (lore.length > 0) {
      meta.lore(List.of(lore));
    }

    item.setItemMeta(meta);

    return item;
  }
}
