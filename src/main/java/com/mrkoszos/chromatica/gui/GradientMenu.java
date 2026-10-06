package com.mrkoszos.chromatica.gui;

import com.mrkoszos.chromatica.Chromatica;
import com.mrkoszos.chromatica.model.NameStyle;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class GradientMenu {

    public static final String TITLE = "Gradient";

    private static final int[] GRADIENT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25
    };

    private static final int PREVIEW_SLOT = 4;
    private static final int ADD_SLOT = 39;
    private static final int APPLY_SLOT = 40;
    private static final int BACK_SLOT = 41;

    private final Chromatica plugin;

    public GradientMenu(Chromatica plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {

        NameStyle style = plugin
                .getNameStyleManager()
                .getStyle(player.getUniqueId());

        /*
         * =========================
         *      START SESSION
         * =========================
         */

        if (plugin.getGradientSession()
                .get(player.getUniqueId()) == null) {

            List<String> existing =
                    style.getGradientColors();

            List<String> colors;

            if (existing == null || existing.size() < 2) {

                colors = new ArrayList<>();
                colors.add("#FF0000");
                colors.add("#0000FF");

            } else {

                colors = new ArrayList<>(existing);
            }

            plugin.getGradientSession().start(
                    player.getUniqueId(),
                    colors
            );
        }

        List<String> colors =
                plugin.getGradientSession()
                        .get(player.getUniqueId());

        /*
         * =========================
         *         INVENTORY
         * =========================
         */

        Inventory inventory = plugin.getServer()
                .createInventory(
                        null,
                        45,
                        TITLE
                );

        /*
         * =========================
         *          PREVIEW
         * =========================
         */

        Component preview =
                plugin.getNameRenderer()
                        .renderGradientPreview(
                                player.getName(),
                                colors,
                                style
                        );

        ItemStack previewItem =
                new ItemStack(Material.ENDER_EYE);

        ItemMeta previewMeta =
                previewItem.getItemMeta();

        previewMeta.displayName(
                preview.decoration(
                        TextDecoration.ITALIC,
                        false
                )
        );

        previewMeta.lore(
                List.of(
                        Component.text(
                                "Current gradient preview.",
                                NamedTextColor.GRAY
                        ).decoration(
                                TextDecoration.ITALIC,
                                false
                        )
                )
        );

        previewItem.setItemMeta(previewMeta);

        inventory.setItem(
                PREVIEW_SLOT,
                previewItem
        );

        /*
         * =========================
         *       COLOR SLOTS
         * =========================
         */

        for (int i = 0; i < colors.size(); i++) {

            if (i >= GRADIENT_SLOTS.length) {
                break;
            }

            String hex = colors.get(i);

            inventory.setItem(
                    GRADIENT_SLOTS[i],
                    createColorItem(
                            getColorMaterial(hex),
                            "Color " + (i + 1),
                            hex
                    )
            );
        }

        /*
         * =========================
         *        ADD COLOR
         * =========================
         */

        inventory.setItem(
                ADD_SLOT,
                createItem(
                        Material.LIME_DYE,
                        ChatColor.GREEN + "Apply",
                        ChatColor.GRAY +
                                "Apply this gradient."
                )
        );

        /*
         * =========================
         *           APPLY
         * =========================
         */

        inventory.setItem(
                APPLY_SLOT,
                createItem(
                        Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE,
                        ChatColor.GREEN + "Add Color",
                        ChatColor.GRAY +
                                "Add another gradient color."
                )
        );

        /*
         * =========================
         *            BACK
         * =========================
         */

        inventory.setItem(
                BACK_SLOT,
                createItem(
                        Material.BARRIER,
                        ChatColor.RED + "Back",
                        ChatColor.GRAY +
                                "Discard changes."
                )
        );

        /*
         * =========================
         *        BACKGROUND
         * =========================
         */

        GuiUtils.fillBorderRowsOnly(inventory, plugin);

        player.openInventory(inventory);
    }

    /*
     * =========================
     *       COLOR MATERIAL
     * =========================
     */

    private Material getColorMaterial(String hex) {

        if (hex == null) {
            return Material.WHITE_DYE;
        }

        String color = hex.toUpperCase();

        return switch (color) {

            case "#FF0000", "#FF5555", "#AA0000" -> Material.RED_DYE;
            case "#FFAA00", "#FFAA55" -> Material.ORANGE_DYE;
            case "#FFFF00", "#FFFF55" -> Material.YELLOW_DYE;
            case "#55FF55", "#00FF00" -> Material.LIME_DYE;
            case "#55FFFF", "#00FFFF" -> Material.CYAN_DYE;
            case "#5555FF", "#0000FF" -> Material.BLUE_DYE;
            case "#AA55FF", "#AA00AA", "#FF55FF" -> Material.PURPLE_DYE;
            case "#FFFFFF" -> Material.WHITE_DYE;
            case "#000000" -> Material.BLACK_DYE;
            default -> Material.WHITE_DYE;
        };
    }

    /*
     * =========================
     *       COLOR ITEM
     * =========================
     */

    private ItemStack createColorItem(
            Material material,
            String name,
            String hex
    ) {

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        Component coloredName = Component.text(name);
        TextColor textColor = TextColor.fromHexString(hex);

        if (textColor != null) {
            coloredName = coloredName.color(textColor);
        }

        coloredName = coloredName.decoration(
                TextDecoration.ITALIC,
                false
        );

        meta.displayName(coloredName);

        Component hexComponent = Component.text(hex);

        if (textColor != null) {
            hexComponent = hexComponent.color(textColor);
        }

        hexComponent = hexComponent.decoration(
                TextDecoration.ITALIC,
                false
        );

        meta.lore(
                List.of(
                        hexComponent,
                        Component.empty(),
                        Component.text(
                                "Left click to change.",
                                NamedTextColor.YELLOW
                        ).decoration(
                                TextDecoration.ITALIC,
                                false
                        ),
                        Component.text(
                                "Right click to remove.",
                                NamedTextColor.RED
                        ).decoration(
                                TextDecoration.ITALIC,
                                false
                        )
                )
        );

        item.setItemMeta(meta);

        return item;
    }

    /*
     * =========================
     *         GENERIC ITEM
     * =========================
     */

    private ItemStack createItem(
            Material material,
            String name,
            String... lore
    ) {

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(name);

        if (lore.length > 0) {
            meta.setLore(List.of(lore));
        }

        item.setItemMeta(meta);

        return item;
    }
}