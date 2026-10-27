package com.mrkoszos.chromatica.gui;

import com.mrkoszos.chromatica.Chromatica;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.InventoryClickEvent;

public class FormattingMenuListener implements Listener {
  private final Chromatica plugin;

  public FormattingMenuListener(Chromatica plugin) {
    this.plugin = plugin;
  }

  @EventHandler
  public void onInventoryClick(InventoryClickEvent event) {
    if (!GuiUtils.isMenu(event.getView().getTopInventory(), "FormattingMenu")) return;
    event.setCancelled(true);
    if (!GuiUtils.isActionClick(event) || !(event.getWhoClicked() instanceof Player player)) return;
    if (!GuiUtils.allowed(plugin, player)) return;
    int slot = event.getRawSlot();
    if (slot == FormattingMenu.BACK_SLOT) {
      GuiUtils.navigate(plugin, player, () -> new ChromaticaMenu(plugin).open(player));
      return;
    }
    var style = plugin.getNameStyleManager().getStyle(player.getUniqueId());
    boolean enabled;
    String name;
    switch (slot) {
      case 11 -> {
        enabled = !style.isBold();
        style.setBold(enabled);
        name = "BOLD";
      }
      case 12 -> {
        enabled = !style.isItalic();
        style.setItalic(enabled);
        name = "ITALIC";
      }
      case 13 -> {
        enabled = !style.isScramble();
        if (enabled && !player.hasPermission("chromatica.format.scramble")) {
          GuiSounds.play(plugin, player, GuiSounds.Event.ERROR);
          return;
        }
        style.setScramble(enabled);
        name = "SCRAMBLE";
      }
      case 14 -> {
        enabled = !style.isStrikethrough();
        style.setStrikethrough(enabled);
        name = "STRIKETHROUGH";
      }
      case 15 -> {
        enabled = !style.isUnderlined();
        style.setUnderlined(enabled);
        name = "UNDERLINE";
      }
      default -> {
        return;
      }
    }
    if (!GuiUtils.persist(plugin, player, style)) return;
    GuiSounds.play(plugin, player, GuiSounds.Event.valueOf(name + (enabled ? "_ON" : "_OFF")));
    new FormattingMenu(plugin).refresh(player, event.getView().getTopInventory());
  }
}
