package com.mrkoszos.chromatica.gui;

import com.mrkoszos.chromatica.Chromatica;
import com.mrkoszos.chromatica.model.StyleType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.InventoryClickEvent;

public class ColorMenuListener implements Listener {
  private final Chromatica plugin;

  public ColorMenuListener(Chromatica plugin) {
    this.plugin = plugin;
  }

  @EventHandler
  public void onInventoryClick(InventoryClickEvent event) {
    if (!GuiUtils.isMenu(event.getView().getTopInventory(), "ColorMenu")) return;
    event.setCancelled(true);
    if (!GuiUtils.isActionClick(event) || !(event.getWhoClicked() instanceof Player player)) return;
    int slot = event.getRawSlot();
    if (!GuiUtils.allowed(plugin, player)) return;
    String hex = GuiSelection.paletteHex(slot);
    if (hex != null) {
      applyColor(player, hex);
      new ColorMenu(plugin).open(player); // Copies contents into the existing Color menu.
    } else if (slot == ColorMenu.HEX_SLOT) {
      GuiUtils.navigate(
          plugin,
          player,
          () -> new HexColorMenu(plugin).open(player, value -> applyColor(player, value)));
    } else if (slot == ColorMenu.BACK_SLOT) {
      GuiUtils.navigate(plugin, player, () -> new ChromaticaMenu(plugin).open(player));
    }
  }

  private void applyColor(Player player, String hex) {
    var style = plugin.getNameStyleManager().getStyle(player.getUniqueId());
    style.setType(StyleType.SOLID);
    style.setPrimaryColor(hex);
    style.setPresetId(null);
    if (!GuiUtils.persist(plugin, player, style)) return;
    GuiSelection.clear(plugin, player.getUniqueId());
    GuiSounds.play(plugin, player, GuiSounds.Event.CONFIRM);
    player.sendActionBar(Component.text("Your name color has been changed.", NamedTextColor.GREEN));
  }
}
