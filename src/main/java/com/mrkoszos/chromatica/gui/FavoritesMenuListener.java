package com.mrkoszos.chromatica.gui;

import com.mrkoszos.chromatica.Chromatica;
import com.mrkoszos.chromatica.model.NameStyle;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class FavoritesMenuListener implements Listener {

  private final Chromatica plugin;

  public FavoritesMenuListener(Chromatica plugin) {
    this.plugin = plugin;
  }

  @EventHandler
  public void onInventoryClick(InventoryClickEvent event) {

    if (!GuiUtils.isMenu(event.getView().getTopInventory(), "FavoritesMenu")) {
      return;
    }

    event.setCancelled(true);
    if (!GuiUtils.isActionClick(event) && !InlineConfirmation.isDrop(event)) return;

    if (!(event.getWhoClicked() instanceof Player player)) {
      return;
    }

    if (!GuiUtils.allowed(plugin, player)) return;

    if (event.getCurrentItem() == null) {
      return;
    }

    int slot = event.getRawSlot();

    /*
     * =========================
     *            BACK
     * =========================
     */

    if (slot == FavoritesMenu.BACK_SLOT) {
      if (InlineConfirmation.isDrop(event)) return;
      GuiUtils.navigate(plugin, player, () -> new ChromaticaMenu(plugin).open(player));
      return;
    }

    /*
     * =========================
     *       FAVORITE SLOTS
     * =========================
     */

    for (int i = 0; i < FavoritesMenu.FAVORITE_SLOTS.length; i++) {

      if (slot != FavoritesMenu.FAVORITE_SLOTS[i]) {
        continue;
      }

      if (InlineConfirmation.isDrop(event)) {
        NameStyle selected = plugin.getFavoriteManager().getFavorite(player.getUniqueId(), i);
        if (selected != null) InlineConfirmation.favorite(plugin, player, i, selected);
      } else if (event.isLeftClick()) handleFavoriteClick(player, i, false);
      return;
    }
  }

  private void handleFavoriteClick(Player player, int index, boolean rightClick) {

    NameStyle existing = plugin.getFavoriteManager().getFavorite(player.getUniqueId(), index);

    /*
     * =========================
     *      EMPTY SLOT -> SAVE
     * =========================
     */

    if (existing == null) {

      NameStyle current = plugin.getNameStyleManager().getStyle(player.getUniqueId());

      if (!plugin.getFavoriteManager().setFavorite(player.getUniqueId(), index, current)) {
        GuiSounds.play(plugin, player, GuiSounds.Event.ERROR);
        player.sendActionBar(
            Component.text("Could not save favorite. Please try again.", NamedTextColor.RED));
        return;
      }

      GuiSelection.remember(
          plugin, player.getUniqueId(), "favorite", String.valueOf(index), current);
      GuiSounds.play(plugin, player, GuiSounds.Event.CONFIRM);
      player.sendActionBar(
          Component.text(
              "Saved your current style to slot " + (index + 1) + ".", NamedTextColor.GREEN));

      new FavoritesMenu(plugin).refresh(player, player.getOpenInventory().getTopInventory());
      return;
    }

    /*
     * =========================
     *   RIGHT CLICK -> REMOVE
     * =========================
     */

    /*
     * =========================
     *   LEFT CLICK -> APPLY
     * =========================
     */

    if (!GuiUtils.persist(plugin, player, existing.copy())) return;
    GuiSelection.remember(
        plugin, player.getUniqueId(), "favorite", String.valueOf(index), existing);
    GuiSounds.play(plugin, player, GuiSounds.Event.CONFIRM);

    player.sendActionBar(Component.text("Favorite applied.", NamedTextColor.GREEN));

    new FavoritesMenu(plugin).refresh(player, player.getOpenInventory().getTopInventory());
  }
}
