package com.mrkoszos.chromatica.gui;

import com.mrkoszos.chromatica.Chromatica;
import com.mrkoszos.chromatica.model.StyleType;
import com.mrkoszos.chromatica.manager.PresetManager.PresetDefinition;
import com.mrkoszos.chromatica.manager.NameStyleCodec;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

public final class PresetBrowserListener implements Listener {
  private final Chromatica plugin;

  public PresetBrowserListener(Chromatica plugin) {
    this.plugin = plugin;
  }

  @EventHandler
  public void onClick(InventoryClickEvent event) {
    Inventory inventory = event.getView().getTopInventory();
    boolean community = GuiUtils.isMenu(inventory, "CommunityPresetMenu");
    if (!community && !GuiUtils.isMenu(inventory, "ServerPresetMenu")) return;
    event.setCancelled(true);
    if ((!GuiUtils.isActionClick(event) && !InlineConfirmation.isDrop(event))
        || !(event.getWhoClicked() instanceof Player player)) return;
    int slot = event.getRawSlot();
    if (InlineConfirmation.isDrop(event)
        && (!community || java.util.Arrays.stream(PresetBrowserMenu.PRESET_SLOTS)
            .noneMatch(value -> value == slot))) return;
    if (!player.hasPermission("chromatica.use")) {
      GuiSounds.play(plugin, player, GuiSounds.Event.ERROR);
      return;
    }
    if (community && !plugin.isCommunityPresetsEnabled()) {
      GuiSounds.play(plugin, player, GuiSounds.Event.ERROR);
      player.sendActionBar(Component.text("Community Presets are disabled.", NamedTextColor.RED));
      plugin.getServer().getScheduler().runTask(plugin, () -> new PresetMenu(plugin).open(player));
      return;
    }
    var holder = (GuiUtils.MenuHolder) inventory.getHolder();
    PresetBrowserMenu menu = new PresetBrowserMenu(plugin, community);
    if (slot == PresetBrowserMenu.BACK_SLOT) {
      navigate(player, () -> new PresetMenu(plugin).open(player));
      return;
    }
    if (slot == PresetBrowserMenu.PAGE_SLOT) {
      int before = holder.page;
      menu.refresh(player, inventory, holder.page + (event.getClick() == org.bukkit.event.inventory.ClickType.RIGHT ? 1 : -1));
      if (holder.page != before) GuiSounds.play(plugin, player, GuiSounds.Event.NAVIGATE);
      return;
    }
    if (!community && slot == PresetBrowserMenu.RANDOM_SLOT) {
      if (event.getClick() != org.bukkit.event.inventory.ClickType.LEFT) return;
      List<PresetDefinition> eligible = plugin.getPresetManager().getAll().values().stream()
          .filter(preset -> eligible(player, preset)).toList();
      if (eligible.isEmpty()) {
        GuiSounds.play(plugin, player, GuiSounds.Event.ERROR);
        player.sendActionBar(Component.text("No available presets.", NamedTextColor.RED));
        return;
      }
      applyServer(player, eligible.get(ThreadLocalRandom.current().nextInt(eligible.size())),
          menu, inventory, holder.page);
      return;
    }
    if (community && slot == PresetBrowserMenu.PUBLISH_SLOT) {
      if (plugin.getCommunityPresetManager().count(player.getUniqueId())
          >= plugin.getCommunityPresetLimit()) {
        GuiSounds.play(plugin, player, GuiSounds.Event.LIMIT);
        player.sendActionBar(
            Component.text(
                "You have reached the Community Preset limit ("
                    + plugin.getCommunityPresetLimit()
                    + ").",
                NamedTextColor.RED));
        return;
      }
      var snapshot = plugin.getNameStyleManager().getStyle(player.getUniqueId()).copy();
      int page = holder.page;
      navigate(player, () -> new PublishPresetMenu(plugin).open(player, snapshot, page));
      return;
    }
    for (int i = 0; i < PresetBrowserMenu.PRESET_SLOTS.length; i++) {
      if (slot != PresetBrowserMenu.PRESET_SLOTS[i] || i >= holder.entries.size()) continue;
      String id = holder.entries.get(i);
      if (community) {
        var preset = plugin.getCommunityPresetManager().get(UUID.fromString(id));
        if (preset == null) {
          GuiSounds.play(plugin, player, GuiSounds.Event.ERROR);
          player.sendActionBar(
              Component.text("This preset is no longer available.", NamedTextColor.RED));
          menu.refresh(player, inventory, holder.page);
          return;
        }
        if (InlineConfirmation.isDrop(event)) {
          InlineConfirmation.community(plugin, player, preset.id(), slot, holder.page);
          return;
        } else if (event.isLeftClick()) {
          if (!GuiUtils.persist(plugin, player, preset.style())) return;
          GuiSelection.remember(plugin, player.getUniqueId(), "community", id, preset.style());
          GuiSounds.play(plugin, player, GuiSounds.Event.CONFIRM);
          player.sendActionBar(
              Component.text(
                  "Community preset '" + preset.name() + "' applied.", NamedTextColor.GREEN));
        }
        menu.refresh(player, inventory, holder.page);
      } else if (event.isLeftClick()) {
        var preset = plugin.getPresetManager().get(id);
        if (preset == null) {
          GuiSounds.play(plugin, player, GuiSounds.Event.ERROR);
          menu.refresh(player, inventory, holder.page);
          return;
        }
        applyServer(player, preset, menu, inventory, holder.page);
      }
      return;
    }
  }

  /** Match normal preset application, including inherited formatting permissions. */
  private boolean eligible(Player player, PresetDefinition preset) {
    if (preset == null || preset.colors() == null || preset.colors().isEmpty()
        || preset.colors().size() > NameStyleCodec.MAX_STOPS
        || preset.colors().stream().anyMatch(color -> !NameStyleCodec.isHex(color))) return false;
    var style = plugin.getNameStyleManager().getStyle(player.getUniqueId());
    preset.applyFormatting(style);
    return !style.isScramble() || player.hasPermission("chromatica.format.scramble");
  }

  private void applyServer(Player player, PresetDefinition preset, PresetBrowserMenu menu,
      Inventory inventory, int page) {
    // Resolve again against the current catalogue; never apply a stale random candidate.
    preset = plugin.getPresetManager().get(preset.id());
    if (!eligible(player, preset)) {
      GuiSounds.play(plugin, player, GuiSounds.Event.ERROR);
      player.sendActionBar(Component.text("This preset is unavailable.", NamedTextColor.RED));
      return;
    }
    var style = plugin.getNameStyleManager().getStyle(player.getUniqueId());
    if (preset.colors().size() == 1) {
      style.setPrimaryColor(preset.colors().getFirst());
      style.setType(StyleType.SOLID);
    } else {
      style.setGradientColors(preset.colors());
      style.setType(StyleType.GRADIENT);
    }
    style.setPresetId(preset.id());
    preset.applyFormatting(style);
    if (!GuiUtils.persist(plugin, player, style)) return;
    GuiSelection.remember(plugin, player.getUniqueId(), "server", preset.id(), style);
    GuiSounds.play(plugin, player, GuiSounds.Event.CONFIRM);
    player.sendActionBar(Component.text(
        "Preset '" + preset.display() + "' has been applied.", NamedTextColor.GREEN));
    menu.refresh(player, inventory, page);
  }

  private void navigate(Player player, Runnable action) {
    GuiUtils.navigate(plugin, player, action);
  }
}
