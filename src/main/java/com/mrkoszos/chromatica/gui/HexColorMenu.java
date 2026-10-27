package com.mrkoszos.chromatica.gui;

import com.mrkoszos.chromatica.Chromatica;
import java.util.Collections;
import java.util.function.Consumer;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class HexColorMenu {

  public static final net.kyori.adventure.text.Component TITLE =
      net.kyori.adventure.text.Component.text("Custom Color");

  private final Chromatica plugin;

  public HexColorMenu(Chromatica plugin) {
    this.plugin = plugin;
  }

  public void open(Player player, Consumer<String> onColor) {

    if (!GuiUtils.allowed(plugin, player)) return;
    if (!plugin.getGuiAnvils().canOpen(player)) return;
    boolean gradientEntry =
        plugin.getColorSelectorSession().get(player.getUniqueId())
            == ColorSelectionContext.GRADIENT;
    boolean[] accepted = {false};
    plugin.getHexPlayers().add(player.getUniqueId());
    org.bukkit.scheduler.BukkitTask[] previewTask = {null};
    AnvilGUI anvil =
        new AnvilGUI.Builder()
            .plugin(plugin)
            .jsonTitle(
                net.kyori.adventure.text.serializer.gson.GsonComponentSerializer.gson()
                    .serialize(TITLE))
            .onClose(
                snapshot -> {
                  if (previewTask[0] != null) previewTask[0].cancel();
                  plugin.getHexPlayers().remove(player.getUniqueId());
                  if (!plugin.isEnabled()) return;
                  plugin
                      .getServer()
                      .getScheduler()
                      .runTask(
                          plugin,
                          () -> {
                            if (!player.isOnline()
                                || !GuiUtils.allowed(plugin, player)
                                || (gradientEntry && accepted[0])) return;
                            var top = player.getOpenInventory().getTopInventory();
                            if (top.getHolder() instanceof GuiUtils.MenuHolder) {
                              if (!GuiUtils.isMenu(top, "ColorSelectorMenu")) {
                                plugin.getColorSelectorSession().remove(player.getUniqueId());
                                plugin.getGradientColorSelection().remove(player.getUniqueId());
                                if (!GuiUtils.isMenu(top, "GradientMenu")) {
                                  plugin.getGradientSession().remove(player.getUniqueId());
                                }
                              }
                              return;
                            }
                            if (top.getType() != org.bukkit.event.inventory.InventoryType.CRAFTING)
                              return;
                            ColorSelectionContext context =
                                plugin.getColorSelectorSession().get(player.getUniqueId());
                            if (context != null && !accepted[0]) {
                              Integer index =
                                  plugin.getGradientColorSelection().get(player.getUniqueId());
                              GuiSounds.play(plugin, player, GuiSounds.Event.NAVIGATE);
                              new ColorSelectorMenu(plugin)
                                  .open(player, context, index == null ? -1 : index);
                            } else {
                              if (!accepted[0])
                                GuiSounds.play(plugin, player, GuiSounds.Event.NAVIGATE);
                              new ColorMenu(plugin).open(player);
                            }
                          });
                })
            .itemLeft(
                GuiUtils.item(
                    Material.ENDER_EYE,
                    GuiUtils.text(
                        "#FFFFFF", net.kyori.adventure.text.format.NamedTextColor.YELLOW)))
            .itemOutput(createInputItem(player, "#FFFFFF"))
            .text("#FFFFFF")
            .onClick(
                (slot, snapshot) -> {
                  if (!plugin.getGuiAnvils().consumeSubmission(player, slot)
                      || !GuiUtils.allowed(plugin, player)) return Collections.emptyList();
                  if (accepted[0] || slot != AnvilGUI.Slot.OUTPUT) {
                    return Collections.emptyList();
                  }

                  String text =
                      player.getOpenInventory() instanceof org.bukkit.inventory.view.AnvilView view
                          ? view.getRenameText()
                          : snapshot.getText();
                  String input = text == null ? "#FFFFFF" : text.trim();

                  if (!isValidHex(input)) {
                    GuiSounds.play(plugin, player, GuiSounds.Event.ERROR);

                    return Collections.emptyList();
                  }

                  accepted[0] = true;
                  plugin
                      .getServer()
                      .getScheduler()
                      .runTask(
                          plugin,
                          () -> {
                            if (player.isOnline() && GuiUtils.allowed(plugin, player))
                              onColor.accept(input);
                          });

                  return Collections.singletonList(AnvilGUI.ResponseAction.close());
                })
            .open(player);
    plugin.getGuiAnvils().track(player, anvil.getInventory());
    String[] previous = {null};
    previewTask[0] =
        plugin
            .getServer()
            .getScheduler()
            .runTaskTimer(
                plugin,
                () -> {
                  if (!player.isOnline()
                      || !player
                          .getOpenInventory()
                          .getTopInventory()
                          .equals(anvil.getInventory())) {
                    previewTask[0].cancel();
                    return;
                  }
                  if (!(player.getOpenInventory()
                      instanceof org.bukkit.inventory.view.AnvilView view)) return;
                  String input = view.getRenameText();
                  if (input == null) input = "#FFFFFF";
                  if (java.util.Objects.equals(previous[0], input)) return;
                  previous[0] = input;
                  ItemStack output = anvil.getInventory().getItem(AnvilGUI.Slot.OUTPUT);
                  if (output == null) return;
                  ItemMeta meta = output.getItemMeta();
                  // Only lore changes: retain rename text, cursor, title, and the open container.
                  meta.lore(java.util.List.of(preview(player, input)));
                  output.setItemMeta(meta);
                  anvil.getInventory().setItem(AnvilGUI.Slot.OUTPUT, output);
                },
                1L,
                1L);
  }

  ItemStack createInputItem(Player player, String input) {

    ItemStack item = new ItemStack(Material.ENDER_EYE);

    ItemMeta meta = item.getItemMeta();

    meta.displayName(GuiUtils.text(input, net.kyori.adventure.text.format.NamedTextColor.YELLOW));

    meta.lore(java.util.List.of(preview(player, input)));
    item.setItemMeta(meta);

    return item;
  }

  net.kyori.adventure.text.Component preview(Player player, String input) {
    if (input == null || !isValidHex(input.trim()))
      return GuiUtils.text("Invalid color", net.kyori.adventure.text.format.NamedTextColor.RED);
    String hex = input.trim();
    var style = plugin.getNameStyleManager().getStyle(player.getUniqueId()).copy();
    if (plugin.getColorSelectorSession().get(player.getUniqueId())
        == ColorSelectionContext.GRADIENT) {
      var existing = plugin.getGradientSession().get(player.getUniqueId());
      Integer index = plugin.getGradientColorSelection().get(player.getUniqueId());
      if (existing == null || index == null || index < 0 || index >= existing.size())
        return GuiUtils.text("Invalid color", net.kyori.adventure.text.format.NamedTextColor.RED);
      var colors = new java.util.ArrayList<>(existing);
      colors.set(index, hex);
      if (colors.size() < 2
          || colors.stream().anyMatch(color -> color == null || !isValidHex(color)))
        return GuiUtils.text("Invalid color", net.kyori.adventure.text.format.NamedTextColor.RED);
      style.setType(com.mrkoszos.chromatica.model.StyleType.GRADIENT);
      style.setGradientColors(colors);
    } else {
      style.setType(com.mrkoszos.chromatica.model.StyleType.SOLID);
      style.setPrimaryColor(hex);
    }
    return plugin
        .getNameRenderer()
        .render(player.getName(), style, player.hasPermission("chromatica.format.scramble"));
  }

  private boolean isValidHex(String input) {

    return input.matches("^#[0-9a-fA-F]{6}$");
  }
}
