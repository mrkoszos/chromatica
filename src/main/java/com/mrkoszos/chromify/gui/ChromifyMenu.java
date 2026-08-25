package com.mrkoszos.chromify.gui;

import com.mrkoszos.chromify.Chromify;
import com.mrkoszos.chromify.model.NameStyle;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class ChromifyMenu {

  public static final Component TITLE = gradientTitle("Chromify");

  private static Component gradientTitle(String text) {
    TextColor start = TextColor.fromHexString("#002239");
    TextColor end = TextColor.fromHexString("#2F00FF");

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

    return result.decoration(TextDecoration.BOLD, true);
  }

  /*
   * =========================
   *          BORDER
   * =========================
   * null = funkcionális item slot.
   */
  private static final Material[] BORDER = {
    Material.BLACK_STAINED_GLASS_PANE,
    Material.GRAY_STAINED_GLASS_PANE,
    Material.GRAY_STAINED_GLASS_PANE,
    Material.BLACK_STAINED_GLASS_PANE,
    null, // 4 - Preview
    Material.BLACK_STAINED_GLASS_PANE,
    Material.GRAY_STAINED_GLASS_PANE,
    Material.GRAY_STAINED_GLASS_PANE,
    Material.BLACK_STAINED_GLASS_PANE,
    Material.GRAY_STAINED_GLASS_PANE,
    null, // 10 - Color
    null, // 11 - Gradient
    Material.GRAY_STAINED_GLASS_PANE,
    Material.BLACK_STAINED_GLASS_PANE,
    Material.GRAY_STAINED_GLASS_PANE,
    null, // 15 - Presets
    null, // 16 - Formatting
    Material.GRAY_STAINED_GLASS_PANE,
    Material.BLACK_STAINED_GLASS_PANE,
    Material.GRAY_STAINED_GLASS_PANE,
    Material.GRAY_STAINED_GLASS_PANE,
    Material.BLACK_STAINED_GLASS_PANE,
    null, // 22 - Reset
    Material.BLACK_STAINED_GLASS_PANE,
    Material.GRAY_STAINED_GLASS_PANE,
    Material.GRAY_STAINED_GLASS_PANE,
    Material.BLACK_STAINED_GLASS_PANE
  };

  private static final int PREVIEW_SLOT = 4;
  private static final int COLOR_SLOT = 10;
  private static final int GRADIENT_SLOT = 11;
  private static final int PRESETS_SLOT = 15;
  private static final int FORMATTING_SLOT = 16;
  private static final int RESET_SLOT = 22;

  private final Chromify plugin;

  public ChromifyMenu(Chromify plugin) {
    this.plugin = plugin;
  }

  public void open(Player player) {

    NameStyle style = plugin.getNameStyleManager().getStyle(player.getUniqueId());

    Inventory inventory = plugin.getServer().createInventory(null, 27, TITLE);

    /*
     * =========================
     *          BORDER
     * =========================
     */

    for (int i = 0; i < BORDER.length; i++) {

      if (BORDER[i] != null) {

        inventory.setItem(i, createItem(BORDER[i], Component.text(" ")));
      }
    }

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
