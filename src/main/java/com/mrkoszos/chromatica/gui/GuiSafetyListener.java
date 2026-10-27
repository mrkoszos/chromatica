package com.mrkoszos.chromatica.gui;

import com.mrkoszos.chromatica.Chromatica;
import java.util.UUID;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public final class GuiSafetyListener implements Listener {
  private final Chromatica plugin;

  public GuiSafetyListener(Chromatica plugin) {
    this.plugin = plugin;
  }

  @EventHandler
  public void onDrag(InventoryDragEvent event) {
    if (event.getView().getTopInventory().getHolder() instanceof GuiUtils.MenuHolder
        && event.getRawSlots().stream()
            .anyMatch(slot -> slot < event.getView().getTopInventory().getSize())) {
      event.setCancelled(true);
    }
  }

  @EventHandler
  public void onClose(InventoryCloseEvent event) {
    if (!(event.getInventory().getHolder() instanceof GuiUtils.MenuHolder)) return;
    if (!plugin.isEnabled()) return;
    UUID uuid = event.getPlayer().getUniqueId();
    plugin
        .getServer()
        .getScheduler()
        .runTask(
            plugin,
            () -> {
              var player = plugin.getServer().getPlayer(uuid);
              if (player == null) {
                clear(uuid);
                return;
              }
              var top = player.getOpenInventory().getTopInventory();
              if (!GuiUtils.isMenu(top, "GradientMenu")
                  && !GuiUtils.isMenu(top, "ColorSelectorMenu")
                  && !plugin.getHexPlayers().contains(uuid)) clear(uuid);
            });
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    plugin.getNameStyleManager().getStyle(event.getPlayer().getUniqueId());
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    UUID uuid = event.getPlayer().getUniqueId();
    clear(uuid);
    GuiSelection.clear(plugin, uuid);
    plugin.getHexPlayers().remove(uuid);
    plugin.getFavoriteManager().unload(uuid);
    plugin.getNameStyleManager().unload(uuid);
  }

  private void clear(UUID uuid) {
    plugin.getGradientSession().remove(uuid);
    plugin.getColorSelectorSession().remove(uuid);
    plugin.getGradientColorSelection().remove(uuid);
  }
}
