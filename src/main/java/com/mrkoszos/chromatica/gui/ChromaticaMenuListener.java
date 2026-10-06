package com.mrkoszos.chromatica.gui;

import com.mrkoszos.chromatica.Chromatica;
import com.mrkoszos.chromatica.model.NameStyle;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class ChromaticaMenuListener implements Listener {

    private final Chromatica plugin;

    public ChromaticaMenuListener(Chromatica plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(
            InventoryClickEvent event
    ) {

        if (!event.getView()
                .title()
                .equals(ChromaticaMenu.TITLE)) {
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

            case 19 -> {              // COLOR
                player.closeInventory();
                new ColorMenu(plugin).open(player);
            }

            case 20 -> {              // GRADIENT
                player.closeInventory();
                new GradientMenu(plugin).open(player);
            }

            case 22 -> {              // FAVORITES
                new FavoritesMenu(plugin).open(player);
            }

            case 24 -> {              // PRESETS
                player.closeInventory();
                new PresetMenu(plugin).open(player);
            }

            case 25 -> {              // FORMATTING
                player.closeInventory();
                new FormattingMenu(plugin).open(player);
            }

            case 31 -> {              // RESET
                plugin.getNameStyleManager().resetStyle(player.getUniqueId());

                player.sendActionBar(
                        Component.text(
                                "Your name style has been reset.",
                                NamedTextColor.YELLOW
                        )
                );

                player.closeInventory();
                new ChromaticaMenu(plugin).open(player);
            }

            default -> {
            }
        }
    }
}