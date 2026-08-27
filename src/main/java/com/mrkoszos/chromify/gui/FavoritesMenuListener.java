package com.mrkoszos.chromify.gui;

import com.mrkoszos.chromify.Chromify;
import com.mrkoszos.chromify.model.NameStyle;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class FavoritesMenuListener implements Listener {

    private static final int BACK_SLOT = 21;
    private static final int CLOSE_SLOT = 23;

    private final Chromify plugin;

    public FavoritesMenuListener(Chromify plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {

        if (!event.getView().getTitle().equals(FavoritesMenu.TITLE)) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (event.getCurrentItem() == null) {
            return;
        }

        int slot = event.getRawSlot();

        /*
         * =========================
         *            BACK
         * =========================
         */

        if (slot == BACK_SLOT) {
            player.closeInventory();
            new ChromifyMenu(plugin).open(player);
            return;
        }

        /*
         * =========================
         *           CLOSE
         * =========================
         */

        if (slot == CLOSE_SLOT) {
            player.closeInventory();
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

            handleFavoriteClick(player, i, event.isRightClick());
            return;
        }
    }

    private void handleFavoriteClick(Player player, int index, boolean rightClick) {

        NameStyle existing = plugin.getFavoriteManager()
                .getFavorite(player.getUniqueId(), index);

        /*
         * =========================
         *      EMPTY SLOT -> SAVE
         * =========================
         */

        if (existing == null) {

            NameStyle current = plugin.getNameStyleManager()
                    .getStyle(player.getUniqueId());

            plugin.getFavoriteManager().setFavorite(player.getUniqueId(), index, current);

            player.sendActionBar(
                    Component.text(
                            "Saved your current style to slot " + (index + 1) + ".",
                            NamedTextColor.GREEN
                    )
            );

            new FavoritesMenu(plugin).open(player);
            return;
        }

        /*
         * =========================
         *   RIGHT CLICK -> REMOVE
         * =========================
         */

        if (rightClick) {

            plugin.getFavoriteManager().removeFavorite(player.getUniqueId(), index);

            player.sendActionBar(
                    Component.text(
                            "Favorite slot " + (index + 1) + " cleared.",
                            NamedTextColor.YELLOW
                    )
            );

            new FavoritesMenu(plugin).open(player);
            return;
        }

        /*
         * =========================
         *   LEFT CLICK -> APPLY
         * =========================
         */

        plugin.getNameStyleManager().setStyle(player.getUniqueId(), existing.copy());

        player.sendActionBar(
                Component.text("Favorite applied.", NamedTextColor.GREEN)
        );

        player.closeInventory();
    }
}