package com.mrkoszos.chromatica.gui;

import com.mrkoszos.chromatica.Chromatica;
import com.mrkoszos.chromatica.manager.CommunityPresetManager;
import com.mrkoszos.chromatica.model.NameStyle;
import java.util.*;
import java.util.function.BooleanSupplier;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;

/** Single-use, five-second confirmations bound to the original view and immutable target. */
public final class InlineConfirmation implements Listener {
  private static final Map<Chromatica, Map<UUID, Pending>> STATES = new WeakHashMap<>();
  private final Chromatica plugin;

  public InlineConfirmation(Chromatica plugin) {
    this.plugin = plugin;
  }

  static final class Pending {
    final String key;
    final Inventory inventory;
    final int slot;
    final boolean drop;
    final long deadline = System.nanoTime() + 5_000_000_000L;
    final ItemStack original, warning;
    final BooleanSupplier action, valid;
    final Runnable refresh;
    boolean consumed;

    Pending(
        String key,
        Inventory inventory,
        int slot,
        boolean drop,
        ItemStack original,
        ItemStack warning,
        BooleanSupplier valid,
        BooleanSupplier action,
        Runnable refresh) {
      this.key = key;
      this.inventory = inventory;
      this.slot = slot;
      this.drop = drop;
      this.original = original;
      this.warning = warning;
      this.valid = valid;
      this.action = action;
      this.refresh = refresh;
    }
  }

  private static Map<UUID, Pending> states(Chromatica plugin) {
    return STATES.computeIfAbsent(plugin, ignored -> new HashMap<>());
  }

  public static void cancel(Player player) {
    for (var state : STATES.values()) restore(player, state.remove(player.getUniqueId()));
  }

  public static void clear(Chromatica plugin) {
    var pending = STATES.remove(plugin);
    if (pending == null) return;
    pending.forEach(
        (id, state) -> {
          Player player = plugin.getServer().getPlayer(id);
          if (player != null) restore(player, state);
        });
  }

  private static void restore(Player player, Pending pending) {
    if (pending != null
        && pending.slot >= 0
        && player.getOpenInventory().getTopInventory() == pending.inventory
        && Objects.equals(pending.inventory.getItem(pending.slot), pending.warning))
      pending.inventory.setItem(pending.slot, pending.original);
  }

  static void request(
      Chromatica plugin,
      Player player,
      String key,
      int slot,
      boolean drop,
      BooleanSupplier valid,
      BooleanSupplier action,
      Runnable refresh) {
    Inventory inventory = player.getOpenInventory().getTopInventory();
    Map<UUID, Pending> states = states(plugin);
    Pending pending = states.get(player.getUniqueId());
    if (pending != null
        && pending.inventory == inventory
        && pending.key.equals(key)
        && pending.slot == slot
        && System.nanoTime() < pending.deadline) {
      if (pending.consumed) return;
      pending.consumed = true;
      Pending claimed = pending;
      plugin
          .getServer()
          .getScheduler()
          .runTask(
              plugin,
              () -> {
                if (states.get(player.getUniqueId()) != claimed) return;
                if (!player.isOnline()
                    || player.getOpenInventory().getTopInventory() != inventory
                    || System.nanoTime() >= claimed.deadline) {
                  cancel(player);
                  return;
                }
                states.remove(player.getUniqueId());
                restore(player, claimed);
                boolean success = claimed.valid.getAsBoolean() && claimed.action.getAsBoolean();
                GuiSounds.play(
                    plugin, player, success ? GuiSounds.Event.CONFIRM : GuiSounds.Event.ERROR);
                if (!success)
                  player.sendActionBar(
                      GuiUtils.text(
                          "Action failed. Check permissions or select the item again.",
                          NamedTextColor.RED));
                claimed.refresh.run();
              });
      return;
    }
    cancel(player);
    if (!valid.getAsBoolean()) {
      GuiSounds.play(plugin, player, GuiSounds.Event.ERROR);
      return;
    }
    ItemStack original = slot < 0 ? null : inventory.getItem(slot);
    if (slot >= 0 && original == null) return;
    original = original == null ? null : original.clone();
    ItemStack warning = original == null ? null : original.clone();
    if (warning != null) {
      var meta = warning.getItemMeta();
      meta.displayName(
          GuiUtils.text(
              drop ? "Drop again to delete" : "Click again to reset", NamedTextColor.RED));
      warning.setItemMeta(meta);
      inventory.setItem(slot, warning);
    } else
      player.sendMessage(
          GuiUtils.text(
              "Reset target "
                  + key.substring("reset:".length())
                  + ": repeat the same reset/clear command within 5 seconds to confirm.",
              NamedTextColor.RED));
    Pending armed =
        new Pending(key, inventory, slot, drop, original, warning, valid, action, refresh);
    states.put(player.getUniqueId(), armed);
    GuiSounds.play(plugin, player, GuiSounds.Event.SELECT);
    plugin
        .getServer()
        .getScheduler()
        .runTaskLater(
            plugin,
            () -> {
              if (states.get(player.getUniqueId()) != armed) return;
              states.remove(player.getUniqueId());
              restore(player, armed);
            },
            100L);
  }

  public static void reset(
      Chromatica plugin, Player player, UUID target, String permission, boolean command) {
    request(
        plugin,
        player,
        "reset:" + target,
        command ? -1 : 31,
        false,
        () -> player.hasPermission(permission),
        () -> {
          if (!plugin.getNameStyleManager().resetStyle(target)) return false;
          GuiSelection.clear(plugin, target);
          player.sendActionBar(GuiUtils.text("Name style has been reset.", NamedTextColor.YELLOW));
          return true;
        },
        () -> {
          if (!command) new ChromaticaMenu(plugin).open(player);
        });
  }

  public static void favorite(Chromatica plugin, Player player, int index, NameStyle selected) {
    NameStyle snapshot = selected.copy();
    request(
        plugin,
        player,
        "favorite:" + index,
        FavoritesMenu.FAVORITE_SLOTS[index],
        true,
        () ->
            player.hasPermission("chromatica.use")
                && snapshot.equals(
                    plugin.getFavoriteManager().getFavorite(player.getUniqueId(), index)),
        () -> plugin.getFavoriteManager().removeFavorite(player.getUniqueId(), index),
        () ->
            new FavoritesMenu(plugin).refresh(player, player.getOpenInventory().getTopInventory()));
  }

  public static void community(Chromatica plugin, Player player, UUID id, int slot, int page) {
    request(
        plugin,
        player,
        "community:" + id,
        slot,
        true,
        () -> {
          var preset = plugin.getCommunityPresetManager().get(id);
          var inventory = player.getOpenInventory().getTopInventory();
          if (!(inventory.getHolder() instanceof GuiUtils.MenuHolder holder)) return false;
          int index = -1;
          for (int i = 0; i < PresetBrowserMenu.PRESET_SLOTS.length; i++)
            if (PresetBrowserMenu.PRESET_SLOTS[i] == slot) index = i;
          return player.hasPermission("chromatica.use")
              && plugin.isCommunityPresetsEnabled()
              && index >= 0
              && index < holder.entries.size()
              && id.toString().equals(holder.entries.get(index))
              && preset != null
              && CommunityPresetManager.canDelete(
                  preset, player.getUniqueId(), player.hasPermission("chromatica.community.admin"));
        },
        () ->
            plugin
                    .getCommunityPresetManager()
                    .delete(
                        id,
                        player.getUniqueId(),
                        player.hasPermission("chromatica.community.admin"),
                        plugin.isCommunityPresetsEnabled())
                == CommunityPresetManager.Status.SUCCESS,
        () ->
            new PresetBrowserMenu(plugin, true)
                .refresh(player, player.getOpenInventory().getTopInventory(), page));
  }

  public static boolean isDrop(InventoryClickEvent event) {
    return event.getRawSlot() >= 0
        && event.getRawSlot() < event.getView().getTopInventory().getSize()
        && (event.getClick() == ClickType.DROP || event.getClick() == ClickType.CONTROL_DROP);
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onClick(InventoryClickEvent event) {
    if (!(event.getWhoClicked() instanceof Player player)) return;
    Pending pending = states(plugin).get(player.getUniqueId());
    if (pending != null
        && (pending.inventory != event.getView().getTopInventory()
            || pending.slot != event.getRawSlot()
            || !(pending.drop ? isDrop(event) : event.getClick() == ClickType.LEFT)))
      cancel(player);
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onDrag(InventoryDragEvent event) {
    if (event.getWhoClicked() instanceof Player player) cancel(player);
    if (event.getView().getTopInventory().getHolder() instanceof GuiUtils.MenuHolder)
      event.setCancelled(true);
  }

  @EventHandler
  public void onClose(InventoryCloseEvent event) {
    if (event.getPlayer() instanceof Player player) cancel(player);
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    cancel(event.getPlayer());
  }

  @EventHandler
  public void onCommand(PlayerCommandPreprocessEvent event) {
    String[] words = event.getMessage().trim().toLowerCase(Locale.ROOT).split("\\s+");
    String label = words[0].replaceFirst("^/", "").replaceFirst("^chromatica:", "");
    if (words.length >= 2
        && Set.of("chromatica", "chroma", "ch", "namecolor", "nc").contains(label)
        && Set.of("reset", "clear").contains(words[1])) return;
    cancel(event.getPlayer());
  }
}
