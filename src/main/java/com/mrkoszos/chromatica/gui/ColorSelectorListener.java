package com.mrkoszos.chromatica.gui;

import com.mrkoszos.chromatica.Chromatica;
import com.mrkoszos.chromatica.model.StyleType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class ColorSelectorListener implements Listener {

  private final Chromatica plugin;

  public ColorSelectorListener(Chromatica plugin) {
    this.plugin = plugin;
  }

  @EventHandler
  public void onInventoryClick(InventoryClickEvent event) {

    if (!GuiUtils.isMenu(event.getView().getTopInventory(), "ColorSelectorMenu")) {
      return;
    }

    event.setCancelled(true);
    if (!GuiUtils.isActionClick(event)) return;
    if (event.getRawSlot() == ColorSelectorMenu.CLOSE_SLOT) {
      plugin
          .getServer()
          .getScheduler()
          .runTask(plugin, () -> event.getWhoClicked().closeInventory());
      return;
    }

    if (!(event.getWhoClicked() instanceof Player player)) {
      return;
    }

    if (!GuiUtils.allowed(plugin, player)) return;

    if (event.getCurrentItem() == null) {
      return;
    }

    String hex = GuiSelection.paletteHex(event.getRawSlot());

    if (hex != null) {

      applyColor(player, hex, false);

      return;
    }

    if (event.getRawSlot() == ColorSelectorMenu.HEX_SLOT) {
      GuiSounds.play(plugin, player, GuiSounds.Event.NAVIGATE);

      plugin
          .getServer()
          .getScheduler()
          .runTask(
              plugin,
              () ->
                  new HexColorMenu(plugin).open(player, hexColor -> applyColor(player, hexColor, true)));

      return;
    }

    if (event.getRawSlot() == ColorSelectorMenu.BACK_SLOT) {
      GuiSounds.play(plugin, player, GuiSounds.Event.NAVIGATE);
      ColorSelectionContext context = plugin.getColorSelectorSession().get(player.getUniqueId());
      plugin.getColorSelectorSession().remove(player.getUniqueId());
      plugin.getGradientColorSelection().remove(player.getUniqueId());
      plugin
          .getServer()
          .getScheduler()
          .runTask(
              plugin,
              () -> {
                if (context == ColorSelectionContext.GRADIENT)
                  new GradientMenu(plugin).open(player);
                else new ColorMenu(plugin).open(player);
              });
    }
  }

  private void applyColor(Player player, String hex, boolean custom) {
    if (!GuiUtils.allowed(plugin, player)) return;

    ColorSelectionContext context = plugin.getColorSelectorSession().get(player.getUniqueId());

    if (context == null) {
      return;
    }

    switch (context) {
      case SOLID -> {
        var style = plugin.getNameStyleManager().getStyle(player.getUniqueId());
        style.setType(StyleType.SOLID);
        style.setPresetId(null);
        style.setPrimaryColor(hex);
        if (!GuiUtils.persist(plugin, player, style)) return;
        GuiSelection.clear(plugin, player.getUniqueId());
        GuiSounds.play(plugin, player, GuiSounds.Event.CONFIRM);
        player.sendActionBar(
            Component.text("Your name color has been changed.", NamedTextColor.GREEN));
        plugin.getServer().getScheduler().runTask(plugin, () -> new ColorMenu(plugin).open(player));
      }
      case GRADIENT -> {
        Integer index = plugin.getGradientColorSelection().get(player.getUniqueId());

        if (index == null) {
          return;
        }

        var colors = plugin.getGradientSession().get(player.getUniqueId());
        if (colors == null || index < 0 || index >= colors.size()) {
          GuiSounds.play(plugin, player, GuiSounds.Event.ERROR);
          return;
        }
        plugin.getGradientSession().setColor(player.getUniqueId(), index, hex);
        GuiSounds.play(plugin, player, custom ? GuiSounds.Event.CONFIRM : GuiSounds.Event.SELECT);

        plugin.getGradientColorSelection().remove(player.getUniqueId());

        plugin
            .getServer()
            .getScheduler()
            .runTask(plugin, () -> new GradientMenu(plugin).open(player));
      }
    }

    plugin.getColorSelectorSession().remove(player.getUniqueId());
  }
}
