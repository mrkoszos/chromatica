package com.mrkoszos.chromify.gui;

import com.mrkoszos.chromify.Chromify;
import com.mrkoszos.chromify.model.NameStyle;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class MenuListener implements Listener {

    private final Chromify plugin;

    public MenuListener(Chromify plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(
            InventoryClickEvent event
    ) {

        if (!event.getView()
                .title()
                .equals(ChromifyMenu.TITLE)) {
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

        Material clickedMaterial =
                event.getCurrentItem().getType();

        NameStyle style =
                plugin.getNameStyleManager()
                        .getStyle(
                                player.getUniqueId()
                        );

        switch (event.getRawSlot()) {

            case 10 -> {              // volt: 11 — COLOR
                player.closeInventory();
                new ColorMenu(plugin).open(player);
            }

            case 11 -> {              // volt: 12 — GRADIENT
                player.closeInventory();
                new GradientMenu(plugin).open(player);
            }

            case 15 -> {              // volt: 14 — PRESETS
                player.closeInventory();
                new PresetMenu(plugin).open(player);
            }

            case 16 -> {              // volt: 15 — FORMATTING
                player.closeInventory();
                new FormattingMenu(plugin).open(player);
            }

            case 22 -> {              // volt: 13 — RESET
                plugin.getNameStyleManager().resetStyle(player.getUniqueId());

                player.sendActionBar(
                        Component.text(
                                "Your name style has been reset.",
                                NamedTextColor.YELLOW
                        )
                );

                player.closeInventory();
                new ChromifyMenu(plugin).open(player);
            }

            default -> {
            }
        }
    }
}