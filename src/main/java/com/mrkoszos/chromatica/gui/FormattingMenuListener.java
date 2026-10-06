package com.mrkoszos.chromatica.gui;

import com.mrkoszos.chromatica.Chromatica;
import com.mrkoszos.chromatica.model.NameStyle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class FormattingMenuListener implements Listener {

    private final Chromatica plugin;

    public FormattingMenuListener(Chromatica plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(
            InventoryClickEvent event
    ) {

        /*
         * Csak a Formatting GUI érdekel.
         */

        if (!event.getView()
                .getTitle()
                .equals(FormattingMenu.TITLE)) {
            return;
        }

        /*
         * Semmilyen itemet nem lehet
         * kivenni vagy áthelyezni.
         */

        event.setCancelled(true);

        /*
         * Csak játékos használhatja.
         */

        if (!(event.getWhoClicked()
                instanceof Player player)) {
            return;
        }

        /*
         * Shift-click, hotbar swap stb.
         * szintén legyen tiltva.
         */

        if (event.isShiftClick()) {
            return;
        }

        if (event.getCurrentItem() == null) {
            return;
        }

        /*
         * A játékos jelenlegi style-ja.
         */

        NameStyle style =
                plugin.getNameStyleManager()
                        .getStyle(
                                player.getUniqueId()
                        );

        switch (event.getRawSlot()) {

            /*
             * =========================
             *           BOLD
             * =========================
             */

            case 10 -> {

                style.setBold(
                        !style.isBold()
                );

                plugin.getNameStyleManager()
                        .saveStyle(player.getUniqueId());

                new FormattingMenu(plugin)
                        .open(player);
            }

            /*
             * =========================
             *          ITALIC
             * =========================
             */

            case 12 -> {

                style.setItalic(
                        !style.isItalic()
                );

                plugin.getNameStyleManager()
                        .saveStyle(player.getUniqueId());

                new FormattingMenu(plugin)
                        .open(player);
            }

            /*
             * =========================
             *         UNDERLINE
             * =========================
             */

            case 14 -> {

                style.setUnderlined(
                        !style.isUnderlined()
                );

                plugin.getNameStyleManager()
                        .saveStyle(player.getUniqueId());

                new FormattingMenu(plugin)
                        .open(player);
            }

            /*
             * =========================
             *       STRIKETHROUGH
             * =========================
             */

            case 16 -> {

                style.setStrikethrough(
                        !style.isStrikethrough()
                );

                plugin.getNameStyleManager()
                        .saveStyle(player.getUniqueId());

                new FormattingMenu(plugin)
                        .open(player);
            }

            /*
             * =========================
             *            BACK
             * =========================
             */

            case 22 -> {

                player.closeInventory();

                new ChromaticaMenu(plugin)
                        .open(player);
            }

            default -> {
                // Semmit nem csinálunk.
            }
        }
    }
}