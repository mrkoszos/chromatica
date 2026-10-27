package com.mrkoszos.chromatica.gui;

import com.mrkoszos.chromatica.Chromatica;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;

/** UUID-based, bounded profile cache. Never waits for skin resolution. */
public final class GuiHeads {
  private GuiHeads() {}
  private static final Map<Chromatica, Map<UUID, CompletableFuture<? extends PlayerProfile>>> CACHES =
      new WeakHashMap<>();

  public static void clear(Chromatica plugin) { CACHES.remove(plugin); }

  public static final String GLOBE_TEXTURE =
      "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDM0NjdhNTMxOTc4ZDBiOGZkMjRmNTYyODVjNzI3MzRkODRmNWVjODhlMGI0N2M0OTMyMzM2Mjk3OWIzMjNhZiJ9fX0=";

  /** Static property; never resolves a skin online. */
  public static ItemStack globeHead(Chromatica plugin) {
    ItemStack item = new ItemStack(Material.PLAYER_HEAD);
    SkullMeta meta = (SkullMeta) item.getItemMeta();
    var profile = plugin.getServer().createProfile(UUID.nameUUIDFromBytes(
        GLOBE_TEXTURE.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    profile.setProperty(new com.destroystokyo.paper.profile.ProfileProperty("textures", GLOBE_TEXTURE));
    meta.setPlayerProfile(profile);
    meta.displayName(GuiUtils.text("Community Presets",
        net.kyori.adventure.text.format.NamedTextColor.YELLOW));
    item.setItemMeta(meta);
    return item;
  }

  public static ItemStack playerHead(Player player, Material fallback) {
    try {
      ItemStack item = new ItemStack(Material.PLAYER_HEAD);
      SkullMeta meta = (SkullMeta) item.getItemMeta();
      // Online profile already contains the authenticated player's skin; no lookup.
      meta.setPlayerProfile(player.getPlayerProfile());
      item.setItemMeta(meta);
      return item;
    } catch (RuntimeException exception) {
      return new ItemStack(fallback);
    }
  }

  public static void creatorHead(Chromatica plugin, UUID creator, Inventory inventory, int slot) {
    ItemStack original = inventory.getItem(slot);
    var holder = (GuiUtils.MenuHolder) inventory.getHolder();
    var entries = holder.entries;
    Player online = plugin.getServer().getPlayer(creator);
    if (online != null) {
      ItemStack head = playerHead(online, Material.PLAYER_HEAD);
      var meta = head.getItemMeta();
      meta.displayName(original.getItemMeta().displayName());
      meta.lore(original.getItemMeta().lore());
      if (original.getItemMeta().hasEnchantmentGlintOverride())
        meta.setEnchantmentGlintOverride(original.getItemMeta().getEnchantmentGlintOverride());
      head.setItemMeta(meta);
      inventory.setItem(slot, head);
      return;
    }
    try {
      Map<UUID, CompletableFuture<? extends PlayerProfile>> cache =
          CACHES.computeIfAbsent(plugin, ignored -> new LinkedHashMap<>());
      var future = cache.get(creator);
      if (future == null) {
        if (cache.size() >= 256) cache.remove(cache.keySet().iterator().next());
        future = plugin.getServer().createPlayerProfile(creator).update();
        cache.put(creator, future);
      }
      future.thenAccept(profile -> {
        try {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
          // A page refresh/replacement must not receive a stale creator's head.
          Inventory displayed = holder.inventory == null ? inventory : holder.inventory;
          if (!plugin.isEnabled() || profile.getTextures().getSkin() == null
              || displayed.getViewers().isEmpty()
              || !(displayed.getHolder() instanceof GuiUtils.MenuHolder current)
              || current.entries != entries
              || !original.equals(displayed.getItem(slot))) return;
          try {
            var meta = (SkullMeta) original.getItemMeta();
            meta.setOwnerProfile(profile);
            original.setItemMeta(meta);
            displayed.setItem(slot, original);
          } catch (RuntimeException ignored) {
            // The untextured player head is a stable fallback.
          }
        });
        } catch (org.bukkit.plugin.IllegalPluginAccessException ignored) {
          // Plugin disabled between completion and scheduling.
        }
      }).exceptionally(ignored -> null);
    } catch (RuntimeException ignored) {
      // Missing profiles never prevent preset browsing.
    }
  }
}
