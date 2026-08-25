package com.mrkoszos.chromify.gui;

import com.mrkoszos.chromify.Chromify;
import com.mrkoszos.chromify.model.NameStyle;
import com.mrkoszos.chromify.model.StyleType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;

public class GradientMenuListener implements Listener {

    private final Chromify plugin;

    private final int[] colorSlots = {
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    public GradientMenuListener(Chromify plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(
            InventoryClickEvent event
    ) {

        if (!event.getView().getTitle()
                .equals(GradientMenu.TITLE)) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked()
                instanceof Player player)) {
            return;
        }

        if (event.getCurrentItem() == null) {
            return;
        }

        int slot = event.getRawSlot();

        /*
         * =========================
         *       COLOR SLOTS
         * =========================
         */

        for (int i = 0; i < colorSlots.length; i++) {

            if (slot == colorSlots[i]) {

                if (event.isRightClick()) {

                    removeColor(player, i);

                } else if (event.isLeftClick()) {

                    openColorSelector(
                            player,
                            i
                    );
                }

                return;
            }
        }

        /*
         * =========================
         *        ADD COLOR
         * =========================
         */

        if (slot == 40) {
            addColor(player);
            return;
        }

        /*
         * =========================
         *           APPLY
         * =========================
         */

        if (slot == 39) {
            applyGradient(player);
            return;
        }

        /*
         * =========================
         *            BACK
         * =========================
         */

        if (slot == 41) {
            plugin.getGradientSession().remove(player.getUniqueId());
            player.closeInventory();
            new ChromifyMenu(plugin).open(player);
        }
    }

    private void openColorSelector(
            Player player,
            int index
    ) {

        plugin.getGradientColorSelection()
                .put(
                        player.getUniqueId(),
                        index
                );

        new ColorSelectorMenu(plugin)
                .open(
                        player,
                        ColorSelectionContext.GRADIENT,
                        index
                );
    }

    private void addColor(Player player) {

        List<String> colors =
                plugin.getGradientSession()
                        .get(
                                player.getUniqueId()
                        );

        if (colors == null) {
            return;
        }

        if (colors.size() >= 14) {

            player.sendActionBar(
                    Component.text(
                            "You cannot have more than 14 colors.",
                            NamedTextColor.RED
                    )
            );

            return;
        }

        colors.add("#FFFFFF");

        new GradientMenu(plugin)
                .open(player);
    }

    private void removeColor(
            Player player,
            int index
    ) {

        List<String> colors =
                plugin.getGradientSession()
                        .get(
                                player.getUniqueId()
                        );

        if (colors == null) {
            return;
        }

        /*
         * Legalább két színnek maradnia kell.
         */
        if (colors.size() <= 2) {

            player.sendActionBar(
                    Component.text(
                            "A gradient needs at least two colors.",
                            NamedTextColor.RED
                    )
            );

            return;
        }

        if (index >= colors.size()) {
            return;
        }

        colors.remove(index);

        player.sendActionBar(
                Component.text(
                        "Color " + (index + 1) + " has been removed.",
                        NamedTextColor.YELLOW
                )
        );

        new GradientMenu(plugin)
                .open(player);
    }

    private void applyGradient(Player player) {

        NameStyle style =
                plugin.getNameStyleManager()
                        .getStyle(
                                player.getUniqueId()
                        );

        List<String> colors =
                plugin.getGradientSession()
                        .get(
                                player.getUniqueId()
                        );

        if (colors == null || colors.size() < 2) {

            player.sendActionBar(
                    Component.text(
                            "A gradient needs at least two colors.",
                            NamedTextColor.RED
                    )
            );

            return;
        }

        style.setGradientColors(
                new ArrayList<>(colors)
        );

        style.setType(
                StyleType.GRADIENT
        );

        plugin.getNameStyleManager()
                .saveStyle(player.getUniqueId());

        player.sendActionBar(
                Component.text(
                        "Your gradient has been applied.",
                        NamedTextColor.GREEN
                )
        );

        player.closeInventory();

        new ChromifyMenu(plugin)
                .open(player);
    }
}