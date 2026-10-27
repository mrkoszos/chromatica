package com.mrkoszos.chromatica.gui;

import com.mrkoszos.chromatica.Chromatica;
import com.mrkoszos.chromatica.model.NameStyle;
import com.mrkoszos.chromatica.model.StyleType;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class GradientMenuListener implements Listener {

  private final Chromatica plugin;

  private final int[] colorSlots = GradientMenu.GRADIENT_SLOTS;

  public GradientMenuListener(Chromatica plugin) {
    this.plugin = plugin;
  }

  @EventHandler
  public void onInventoryClick(InventoryClickEvent event) {

    if (!GuiUtils.isMenu(event.getView().getTopInventory(), "GradientMenu")) {
      return;
    }

    event.setCancelled(true);
    if (!GuiUtils.isActionClick(event)) return;

    if (!(event.getWhoClicked() instanceof Player player)) {
      return;
    }

    if (!GuiUtils.allowed(plugin, player)) return;

    int slot = event.getRawSlot();

    /*
     * =========================
     *       COLOR SLOTS
     * =========================
     */

    for (int i = 0; i < GradientMenu.capacity(event.getView().getTopInventory()); i++) {

      if (slot == colorSlots[i]) {
        var colors = plugin.getGradientSession().get(player.getUniqueId());
        if (colors == null) return;
        if (i >= colors.size()) return;

        if (event.isRightClick()) {

          removeColor(player, i);

        } else if (event.isLeftClick()) {

          openColorSelector(player, i);
        }

        return;
      }
    }

    /*
     * =========================
     *        ADD COLOR
     * =========================
     */

    /*
     * =========================
     *           APPLY
     * =========================
     */

    if (slot == GradientMenu.addSlot(event.getView().getTopInventory())) {
      if (event.isLeftClick()) addColor(player);
      return;
    }

    if (slot == GradientMenu.applySlot(event.getView().getTopInventory())) {
      applyGradient(player);
      return;
    }

    /*
     * =========================
     *            BACK
     * =========================
     */

    if (slot == GradientMenu.backSlot(event.getView().getTopInventory())) {
      plugin.getGradientSession().remove(player.getUniqueId());
      GuiUtils.navigate(plugin, player, () -> new ChromaticaMenu(plugin).open(player));
    }
  }

  private void openColorSelector(Player player, int index) {

    GuiSounds.play(plugin, player, GuiSounds.Event.NAVIGATE);
    plugin.getGradientColorSelection().put(player.getUniqueId(), index);

    plugin
        .getServer()
        .getScheduler()
        .runTask(
            plugin,
            () ->
                new ColorSelectorMenu(plugin).open(player, ColorSelectionContext.GRADIENT, index));
  }

  private void addColor(Player player) {

    List<String> colors = plugin.getGradientSession().get(player.getUniqueId());

    if (colors == null) {
      return;
    }

    if (colors.size() >= GradientMenu.capacity(player.getOpenInventory().getTopInventory())) {

      GuiSounds.play(plugin, player, GuiSounds.Event.ERROR);
      player.sendActionBar(Component.text("This gradient layout is full.", NamedTextColor.RED));

      return;
    }

    colors.add("#FFFFFF");
    GuiSounds.play(plugin, player, GuiSounds.Event.SELECT);

    new GradientMenu(plugin).refresh(player, player.getOpenInventory().getTopInventory());
  }

  private void removeColor(Player player, int index) {

    List<String> colors = plugin.getGradientSession().get(player.getUniqueId());

    if (colors == null) {
      return;
    }

    /*
     * Legalább két színnek maradnia kell.
     */
    if (colors.size() <= 2) {

      GuiSounds.play(plugin, player, GuiSounds.Event.ERROR);
      player.sendActionBar(
          Component.text("A gradient needs at least two colors.", NamedTextColor.RED));

      return;
    }

    if (index >= colors.size()) {
      return;
    }

    colors.remove(index);
    GuiSounds.play(plugin, player, GuiSounds.Event.DELETE);

    player.sendActionBar(
        Component.text("Color " + (index + 1) + " has been removed.", NamedTextColor.YELLOW));

    new GradientMenu(plugin).refresh(player, player.getOpenInventory().getTopInventory());
  }

  private void applyGradient(Player player) {

    NameStyle style = plugin.getNameStyleManager().getStyle(player.getUniqueId());

    List<String> colors = plugin.getGradientSession().get(player.getUniqueId());

    if (colors == null
        || colors.size() < 2
        || colors.size() > com.mrkoszos.chromatica.manager.NameStyleCodec.MAX_STOPS
        || colors.stream()
            .anyMatch(color -> !com.mrkoszos.chromatica.manager.NameStyleCodec.isHex(color))) {

      GuiSounds.play(plugin, player, GuiSounds.Event.ERROR);
      player.sendActionBar(
          Component.text("A gradient needs at least two colors.", NamedTextColor.RED));

      return;
    }

    style.setGradientColors(new ArrayList<>(colors));

    style.setType(StyleType.GRADIENT);
    style.setPresetId(null);
    if (!GuiUtils.persist(plugin, player, style)) return;
    GuiSelection.clear(plugin, player.getUniqueId());

    GuiSounds.play(plugin, player, GuiSounds.Event.CONFIRM);
    player.sendActionBar(Component.text("Your gradient has been applied.", NamedTextColor.GREEN));

    new GradientMenu(plugin).refresh(player, player.getOpenInventory().getTopInventory());
  }
}
