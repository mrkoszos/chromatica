package com.mrkoszos.chromify.gui;

import com.mrkoszos.chromify.Chromify;
import com.mrkoszos.chromify.manager.PresetManager;
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

public class PresetMenu {

    public static final String TITLE = "Presets";

    /*
     * Ugyanaz a keret-minta, mint a GradientMenu-ben:
     * ugyanazok a nyitott slotok (19-25, 28-34), csak itt
     * preset itemek kerülnek beléjük egy Apply gomb helyett.
     */
    private static final Material[] BORDER = new Material[54];

    static {
        Material B = Material.BLACK_STAINED_GLASS_PANE;
        Material G = Material.GRAY_STAINED_GLASS_PANE;

        Material[] pattern = {
                B, G, G, B, B, B, G, G, B,               // row 1 (0-8)
                G, B, B, B, null, B, B, B, G,             // row 2 (9-17): 13 - info item
                B, null, null, null, null, null, null, null, B, // row 3 (18-26): 19-25 preset slotok
                B, null, null, null, null, null, null, null, B, // row 4 (27-35): 28-34 preset slotok
                G, B, B, B, null, B, B, B, G,             // row 5 (36-44): 40 - back
                B, G, G, B, B, B, G, G, B                 // row 6 (45-53)
        };

        System.arraycopy(pattern, 0, BORDER, 0, 54);
    }

    public static final int[] PRESET_SLOTS = {
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    private static final int INFO_SLOT = 13;
    private static final int BACK_SLOT = 40;

    private final Chromify plugin;

    public PresetMenu(Chromify plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {

        Inventory inventory = plugin.getServer()
                .createInventory(null, 54, TITLE);

        /*
         * =========================
         *          BORDER
         * =========================
         */

        for (int i = 0; i < 54; i++) {

            if (BORDER[i] != null) {

                inventory.setItem(i, createItem(BORDER[i], " "));
            }
        }

        /*
         * =========================
         *           INFO
         * =========================
         */

        inventory.setItem(
                INFO_SLOT,
                createItem(
                        Material.NETHER_STAR,
                        ChatColor.GOLD + "Presets",
                        ChatColor.GRAY + "Choose a predefined style.",
                        ChatColor.GRAY + "Click one to apply it instantly."
                )
        );

        /*
         * =========================
         *          PRESETS
         * =========================
         */

        List<PresetManager.PresetDefinition> presets =
                new ArrayList<>(plugin.getPresetManager().getAll().values());

        for (int i = 0; i < presets.size(); i++) {

            if (i >= PRESET_SLOTS.length) {
                break;
            }

            PresetManager.PresetDefinition preset = presets.get(i);

            inventory.setItem(
                    PRESET_SLOTS[i],
                    createPresetItem(preset)
            );
        }

        /*
         * =========================
         *            BACK
         * =========================
         */

        inventory.setItem(
                BACK_SLOT,
                createItem(
                        Material.BARRIER,
                        ChatColor.RED + "Back"
                )
        );

        player.openInventory(inventory);
    }

    /*
     * =========================
     *      PRESET ITEM
     * =========================
     */

    private ItemStack createPresetItem(PresetManager.PresetDefinition preset) {

        ItemStack item = new ItemStack(preset.material());

        ItemMeta meta = item.getItemMeta();

        meta.displayName(
                createGradientText(preset.display(), preset.colors())
                        .decoration(TextDecoration.ITALIC, false)
        );

        List<Component> lore = new ArrayList<>();

        for (String hex : preset.colors()) {

            TextColor textColor = TextColor.fromHexString(hex);

            Component hexLine = Component.text(hex);

            if (textColor != null) {
                hexLine = hexLine.color(textColor);
            }

            lore.add(hexLine.decoration(TextDecoration.ITALIC, false));
        }

        lore.add(Component.empty());

        lore.add(
                Component.text("Click to apply.", NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false)
        );

        meta.lore(lore);

        item.setItemMeta(meta);

        return item;
    }

    /*
     * =========================
     *       GRADIENT TEXT
     * =========================
     * Egy szín esetén sima színezés, több szín esetén
     * a szöveg egyenletesen oszlik el a színek között.
     */

    private Component createGradientText(String text, List<String> hexColors) {

        if (hexColors.size() == 1) {

            TextColor color = TextColor.fromHexString(hexColors.get(0));

            Component component = Component.text(text);

            if (color != null) {
                component = component.color(color);
            }

            return component.decoration(TextDecoration.ITALIC, false);
        }

        List<TextColor> stops = new ArrayList<>();

        for (String hex : hexColors) {

            TextColor color = TextColor.fromHexString(hex);

            stops.add(color != null ? color : NamedTextColor.WHITE);
        }

        Component result = Component.empty();

        for (int i = 0; i < text.length(); i++) {

            double progress = text.length() == 1
                    ? 0
                    : (double) i / (text.length() - 1);

            double scaled = progress * (stops.size() - 1);

            int segment = Math.min((int) scaled, stops.size() - 2);

            double segmentProgress = scaled - segment;

            TextColor start = stops.get(segment);
            TextColor end = stops.get(segment + 1);

            int red = (int) (start.red() + (end.red() - start.red()) * segmentProgress);
            int green = (int) (start.green() + (end.green() - start.green()) * segmentProgress);
            int blue = (int) (start.blue() + (end.blue() - start.blue()) * segmentProgress);

            TextColor color = TextColor.color(red, green, blue);

            result = result.append(
                    Component.text(String.valueOf(text.charAt(i)))
                            .color(color)
                            .decoration(TextDecoration.ITALIC, false)
            );
        }

        return result;
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
