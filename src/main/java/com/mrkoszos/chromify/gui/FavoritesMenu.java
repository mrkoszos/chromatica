package com.mrkoszos.chromify.gui;

import com.mrkoszos.chromify.Chromify;
import com.mrkoszos.chromify.model.NameStyle;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class FavoritesMenu {

    public static final String TITLE = "Favorites";

    /*
     * =========================
     *          BORDER
     * =========================
     * null = funkcionális/dinamikus item slot (preview, favorite, back).
     */
    private static final Material[] BORDER = {
            Material.LIME_STAINED_GLASS_PANE,
            Material.GREEN_STAINED_GLASS_PANE,
            Material.GREEN_STAINED_GLASS_PANE,
            Material.LIME_STAINED_GLASS_PANE,
            null, // 4  - Preview
            Material.LIME_STAINED_GLASS_PANE,
            Material.GREEN_STAINED_GLASS_PANE,
            Material.GREEN_STAINED_GLASS_PANE,
            Material.LIME_STAINED_GLASS_PANE,
            Material.GREEN_STAINED_GLASS_PANE,
            Material.LIME_STAINED_GLASS_PANE,
            null, // 11 - Favorite 1
            null, // 12 - Favorite 2
            null, // 13 - Favorite 3
            null, // 14 - Favorite 4
            null, // 15 - Favorite 5
            Material.LIME_STAINED_GLASS_PANE,
            Material.GREEN_STAINED_GLASS_PANE,
            Material.LIME_STAINED_GLASS_PANE,
            Material.GREEN_STAINED_GLASS_PANE,
            Material.GREEN_STAINED_GLASS_PANE,
            null, // 21 - Back
            Material.LIME_STAINED_GLASS_PANE,
            null, // 23 - Close (STRUCTURE_VOID)
            Material.GREEN_STAINED_GLASS_PANE,
            Material.GREEN_STAINED_GLASS_PANE,
            Material.LIME_STAINED_GLASS_PANE
    };

    private static final int PREVIEW_SLOT = 4;
    public static final int[] FAVORITE_SLOTS = {11, 12, 13, 14, 15};
    private static final int BACK_SLOT = 21;
    private static final int CLOSE_SLOT = 23;

    private final Chromify plugin;

    public FavoritesMenu(Chromify plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {

        NameStyle[] favorites = plugin.getFavoriteManager().getAll(player.getUniqueId());

        Inventory inventory = plugin.getServer().createInventory(null, 27, TITLE);

        /*
         * =========================
         *          BORDER
         * =========================
         */

        for (int i = 0; i < BORDER.length; i++) {

            if (BORDER[i] != null) {
                inventory.setItem(i, createItem(BORDER[i], " "));
            }
        }

        /*
         * =========================
         *          PREVIEW
         * =========================
         */

        NameStyle currentStyle = plugin.getNameStyleManager().getStyle(player.getUniqueId());

        Component preview = plugin.getNameRenderer().render(player.getName(), currentStyle);

        ItemStack previewItem = new ItemStack(Material.ENDER_EYE);

        ItemMeta previewMeta = previewItem.getItemMeta();

        previewMeta.displayName(preview.decoration(TextDecoration.ITALIC, false));

        previewMeta.lore(
                List.of(
                        Component.text("Your current name style.", NamedTextColor.GRAY)
                                .decoration(TextDecoration.ITALIC, false)
                )
        );

        previewItem.setItemMeta(previewMeta);

        inventory.setItem(PREVIEW_SLOT, previewItem);

        /*
         * =========================
         *       FAVORITE SLOTS
         * =========================
         */

        for (int i = 0; i < FAVORITE_SLOTS.length; i++) {

            inventory.setItem(
                    FAVORITE_SLOTS[i],
                    createFavoriteItem(player, favorites[i], i)
            );
        }

        /*
         * =========================
         *            BACK
         * =========================
         */

        inventory.setItem(
                BACK_SLOT,
                createItem(Material.ARROW, ChatColor.RED + "Back")
        );

        /*
         * =========================
         *           CLOSE
         * =========================
         */

        inventory.setItem(
                CLOSE_SLOT,
                createItem(
                        Material.STRUCTURE_VOID,
                        ChatColor.RED + "Close",
                        ChatColor.GRAY + "Closes this menu."
                )
        );

        player.openInventory(inventory);
    }

    /*
     * =========================
     *      FAVORITE ITEM
     * =========================
     */

    private ItemStack createFavoriteItem(Player player, NameStyle style, int index) {

        if (style == null) {

            return createItem(
                    Material.ITEM_FRAME,
                    ChatColor.GREEN + "Empty Slot",
                    ChatColor.GRAY + "Click to save your current",
                    ChatColor.GRAY + "name style here."
            );
        }

        ItemStack item = new ItemStack(Material.GLOW_ITEM_FRAME);

        ItemMeta meta = item.getItemMeta();

        Component preview = plugin.getNameRenderer().render(player.getName(), style);

        meta.displayName(preview.decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();

        lore.add(
                Component.text("Slot " + (index + 1), NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        );

        lore.add(Component.empty());

        lore.add(
                Component.text("Left click to apply.", NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false)
        );

        lore.add(
                Component.text("Right click to remove.", NamedTextColor.RED)
                        .decoration(TextDecoration.ITALIC, false)
        );

        meta.lore(lore);

        item.setItemMeta(meta);

        return item;
    }

    /*
     * =========================
     *         GENERIC ITEM
     * =========================
     */

    private ItemStack createItem(Material material, String name, String... lore) {

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