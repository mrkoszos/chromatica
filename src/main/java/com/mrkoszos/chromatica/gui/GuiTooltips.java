package com.mrkoszos.chromatica.gui;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/** Presentation only: retain names, lore, profiles, models and selection glint. */
public final class GuiTooltips {
  private GuiTooltips() {}

  public static ItemStack clean(ItemStack item) {
    if (item.getType().name().endsWith("_SMITHING_TEMPLATE")) {
      // SmithingTemplateItem.appendHoverText ignores TOOLTIP_DISPLAY in 26.2.
      // A neutral carrier with the vanilla model keeps the exact icon without that text.
      Key model = item.getData(DataComponentTypes.ITEM_MODEL);
      if (model == null) model = item.getType().getKey();
      item.setType(Material.PAPER);
      item.setData(DataComponentTypes.ITEM_MODEL, model);
    }
    var current = item.getData(DataComponentTypes.TOOLTIP_DISPLAY);
    var display = TooltipDisplay.tooltipDisplay().hideTooltip(false);
    if (current != null) display.hiddenComponents(current.hiddenComponents());
    display.addHiddenComponents(
        DataComponentTypes.ATTRIBUTE_MODIFIERS, DataComponentTypes.ENCHANTMENTS,
        DataComponentTypes.STORED_ENCHANTMENTS, DataComponentTypes.UNBREAKABLE,
        DataComponentTypes.CAN_BREAK, DataComponentTypes.CAN_PLACE_ON,
        DataComponentTypes.BUNDLE_CONTENTS, DataComponentTypes.WRITTEN_BOOK_CONTENT,
        DataComponentTypes.POTION_CONTENTS, DataComponentTypes.TRIM,
        DataComponentTypes.FIREWORKS, DataComponentTypes.FIREWORK_EXPLOSION);
    item.setData(DataComponentTypes.TOOLTIP_DISPLAY, display.build());
    return item;
  }
}
