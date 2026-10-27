package com.mrkoszos.chromatica.gui;

import com.mrkoszos.chromatica.Chromatica;
import com.mrkoszos.chromatica.manager.CommunityPresetManager;
import com.mrkoszos.chromatica.model.NameStyle;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/** Anvil state belongs to this flow; no separate UUID session map is retained. */
public final class PublishPresetMenu {
  public static final Component TITLE = Component.text("Publish Preset");
  private final Chromatica plugin;

  public PublishPresetMenu(Chromatica plugin) {
    this.plugin = plugin;
  }

  public void open(Player player, NameStyle snapshot, int page) {
    if (!player.isOnline()
        || !plugin.isCommunityPresetsEnabled()
        || !player.hasPermission("chromatica.use")) return;
    if (!plugin.getGuiAnvils().canOpen(player)) return;
    boolean[] accepted = {false};
    AnvilGUI anvil = new AnvilGUI.Builder()
        .plugin(plugin)
        .jsonTitle(
            net.kyori.adventure.text.serializer.gson.GsonComponentSerializer.gson()
                .serialize(TITLE))
        .itemLeft(
            GuiUtils.item(
                Material.PAPER,
                GuiUtils.text("Preset name", NamedTextColor.YELLOW),
                GuiUtils.text("Use 1–32 characters.", NamedTextColor.GRAY)))
        .text("Preset name")
        .onClose(
            state -> {
              if (!plugin.isEnabled() || accepted[0]) return;
              plugin.getServer().getScheduler().runTask(plugin, () -> {
                GuiSounds.play(plugin, player, GuiSounds.Event.NAVIGATE);
                returnToMenu(player, page);
              });
            })
        .onClick(
            (slot, state) -> {
              if (accepted[0] || !plugin.getGuiAnvils().consumeSubmission(player, slot) || !GuiUtils.allowed(plugin, player) || !plugin.isCommunityPresetsEnabled()) return List.of();
              if (slot != AnvilGUI.Slot.OUTPUT) return List.of();
              String name = CommunityPresetManager.sanitizeName(state.getText());
              if (name == null) {
                GuiSounds.play(plugin, player, GuiSounds.Event.INVALID_NAME);
                player.sendActionBar(
                    Component.text(
                        message(CommunityPresetManager.Status.INVALID_NAME), NamedTextColor.RED));
                return List.of();
              }
              accepted[0] = true;
              plugin
                  .getServer()
                  .getScheduler()
                  .runTask(
                      plugin,
                      () -> {
                        if (!player.isOnline()) return;
                        if (!player.hasPermission("chromatica.use")) {
                          GuiSounds.play(plugin, player, GuiSounds.Event.ERROR);
                          player.sendActionBar(
                              Component.text(
                                  "You no longer have permission to publish.", NamedTextColor.RED));
                          return;
                        }
                        var result =
                            plugin
                                .getCommunityPresetManager()
                                .publish(
                                    player.getUniqueId(),
                                    player.getName(),
                                    name,
                                    snapshot,
                                    plugin.getCommunityPresetLimit(),
                                    plugin.isCommunityPresetsEnabled());
                        GuiSounds.play(plugin, player, switch (result.status()) {
                          case SUCCESS -> GuiSounds.Event.PUBLISH;
                          case LIMIT_REACHED -> GuiSounds.Event.LIMIT;
                          case INVALID_NAME -> GuiSounds.Event.INVALID_NAME;
                          default -> GuiSounds.Event.ERROR;
                        });
                        player.sendActionBar(
                            Component.text(
                                result.status() == CommunityPresetManager.Status.SUCCESS
                                    ? "Community preset '" + name + "' published."
                                    : message(result.status()),
                                result.status() == CommunityPresetManager.Status.SUCCESS
                                    ? NamedTextColor.GREEN
                                    : NamedTextColor.RED));
                        returnToMenu(
                            player,
                            result.status() == CommunityPresetManager.Status.SUCCESS
                                ? PresetBrowserMenu.pageCount(
                                        plugin.getCommunityPresetManager().getAll().size())
                                    - 1
                                : page);
                      });
              return List.of(AnvilGUI.ResponseAction.close());
            })
        .open(player);
    plugin.getGuiAnvils().track(player, anvil.getInventory());
  }

  private void returnToMenu(Player player, int page) {
    if (!player.isOnline()
        || player.getOpenInventory().getTopInventory().getHolder() instanceof GuiUtils.MenuHolder)
      return;
    if (player.getOpenInventory().getTopInventory().getType() != org.bukkit.event.inventory.InventoryType.CRAFTING) return;
    new PresetBrowserMenu(plugin, true).open(player, page);
  }

  public static String message(CommunityPresetManager.Status status) {
    return switch (status) {
      case SUCCESS -> "Done.";
      case DISABLED -> "Community Presets are disabled.";
      case INVALID_NAME -> "Use a nonblank preset name of at most 32 characters.";
      case LIMIT_REACHED -> "You have reached the Community Preset publication limit.";
      case NOT_FOUND -> "This preset is no longer available.";
      case NOT_OWNER -> "You can only delete your own presets.";
      case STORAGE_ERROR -> "Could not save the change. Please contact an administrator.";
    };
  }
}
