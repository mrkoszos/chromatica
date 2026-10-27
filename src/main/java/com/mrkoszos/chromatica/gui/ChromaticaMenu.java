package com.mrkoszos.chromatica.gui;

import com.mrkoszos.chromatica.Chromatica;
import com.mrkoszos.chromatica.model.NameStyle;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ChromaticaMenu {

  public static final Component TITLE = Component.text("Chromatica");

  public static final int CREDIT_SLOT = 0;
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
    Inventory inventory =
        GuiUtils.inventory(plugin, 45, GuiBranding.title(plugin), "ChromaticaMenu");
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

    inventory.setItem(COLOR_SLOT, GuiUtils.item(Material.MAGENTA_DYE, colorName));

    /*
     * =========================
     *         GRADIENT
     * =========================
     */

    Component gradientName = createGradientText("Gradient", "#FCFEA9", "#33775A");

    inventory.setItem(GRADIENT_SLOT, GuiUtils.item(Material.EXPERIENCE_BOTTLE, gradientName));

    /*
     * =========================
     *          PRESETS
     * =========================
     */

    Component presetsName =
        Component.text("Presets")
            .color(TextColor.fromHexString("#FFD83D"))
            .decoration(TextDecoration.ITALIC, false);

    inventory.setItem(PRESETS_SLOT, GuiUtils.item(Material.PRIZE_POTTERY_SHERD, presetsName));

    /*
     * =========================
     *          FAVORITES
     * =========================
     */

    Component favoritesName =
        Component.text("Favorites")
            .color(TextColor.fromHexString("#55FF55"))
            .decoration(TextDecoration.ITALIC, false);

    inventory.setItem(FAVORITES_SLOT, GuiUtils.item(Material.GLOW_ITEM_FRAME, favoritesName));

    /*
     * =========================
     *        FORMATTING
     * =========================
     */

    Component formattingName =
        Component.text("Formatting")
            .color(TextColor.fromHexString("#9B7BFF"))
            .decoration(TextDecoration.ITALIC, false);

    inventory.setItem(FORMATTING_SLOT, GuiUtils.item(Material.WRITABLE_BOOK, formattingName));

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
        Component.text("Restore default style.")
            .color(TextColor.fromHexString("#AAAAAA"))
            .decoration(TextDecoration.ITALIC, false);

    inventory.setItem(RESET_SLOT, GuiUtils.item(Material.MILK_BUCKET, resetName, resetLore));

    /*
     * =========================
     *        BACKGROUND
     * =========================
     */

    GuiUtils.mark(
        inventory, COLOR_SLOT, style.getType() == com.mrkoszos.chromatica.model.StyleType.SOLID);
    GuiUtils.mark(
        inventory,
        GRADIENT_SLOT,
        style.getType() == com.mrkoszos.chromatica.model.StyleType.GRADIENT);
    inventory.setItem(44, GuiUtils.closeItem());
    GuiUtils.fillEmptySlots(inventory, plugin);
    applyCredit(inventory);
  }

  /*
   * =========================
   *       GRADIENT TEXT
   * =========================
   */

  void applyCredit(Inventory inventory) {
    if (plugin.getGuiBranding() != null && !plugin.getGuiBranding().showCredit()) return;
    ItemStack pane = inventory.getItem(CREDIT_SLOT);
    if (pane == null) return;
    ItemMeta meta = pane.getItemMeta();
    meta.setHideTooltip(false);
    meta.lore(
        List.of(
            GuiUtils.text(
                "Powered by Chromatica", net.kyori.adventure.text.format.NamedTextColor.GRAY)));
    pane.setItemMeta(meta);
    inventory.setItem(CREDIT_SLOT, pane);
  }

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

}
