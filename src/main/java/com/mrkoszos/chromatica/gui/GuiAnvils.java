package com.mrkoszos.chromatica.gui;

import java.util.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;

/** Inventory identity, never title, guards the library callback as well as item movement. */
public final class GuiAnvils implements Listener {
  private final Map<Inventory, UUID> owners = new IdentityHashMap<>();
  private final Set<Inventory> submissions = Collections.newSetFromMap(new IdentityHashMap<>());

  public boolean canOpen(Player player) {
    var cursor = player.getItemOnCursor();
    if (cursor == null || cursor.getType() == org.bukkit.Material.AIR) return true;
    player.sendActionBar(GuiUtils.text("Put away your cursor item first.", net.kyori.adventure.text.format.NamedTextColor.RED));
    return false;
  }

  public void track(Player player, Inventory inventory) { owners.put(inventory, player.getUniqueId()); }

  public boolean consumeSubmission(Player player, int slot) {
    Inventory inventory = player.getOpenInventory().getTopInventory();
    return slot == 2 && player.getUniqueId().equals(owners.get(inventory)) && submissions.remove(inventory);
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onClick(InventoryClickEvent event) {
    Inventory inventory = event.getView().getTopInventory();
    if (!owners.containsKey(inventory)) return;
    submissions.remove(inventory);
    if (event.getRawSlot() == 2 && GuiUtils.isActionClick(event)
        && (event.getCursor() == null || event.getCursor().getType() == org.bukkit.Material.AIR)) submissions.add(inventory);
    event.setCancelled(true);
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void protectClick(InventoryClickEvent event) {
    if (owners.containsKey(event.getView().getTopInventory())) event.setCancelled(true);
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onDrag(InventoryDragEvent event) {
    if (owners.containsKey(event.getView().getTopInventory())) event.setCancelled(true);
  }

  @EventHandler
  public void onClose(InventoryCloseEvent event) {
    owners.remove(event.getInventory());
    submissions.remove(event.getInventory());
  }

  @EventHandler
  public void onQuit(org.bukkit.event.player.PlayerQuitEvent event) {
    UUID owner = event.getPlayer().getUniqueId();
    owners.entrySet().removeIf(entry -> {
      if (!entry.getValue().equals(owner)) return false;
      submissions.remove(entry.getKey());
      return true;
    });
  }

  public void clear() { owners.clear(); submissions.clear(); }
}
