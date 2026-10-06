package com.mrkoszos.chromatica.manager;

import com.mrkoszos.chromatica.Chromatica;
import com.mrkoszos.chromatica.model.BackgroundPattern;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.logging.Level;

/**
 * =========================
 *     BACKGROUND MANAGER
 * =========================
 * Egyetlen, minden menü által megosztott háttér-rendszer.
 * <p>
 * - A minta (BackgroundPattern) hard-code-olt (lásd a model csomagban).
 * - A ténylegesen megjelenő item (material + név) a config.yml-ből jön,
 *   primary/secondary bontásban.
 */
public class BackgroundManager {

    private static final Material DEFAULT_PRIMARY_MATERIAL = Material.BLACK_STAINED_GLASS_PANE;
    private static final Material DEFAULT_SECONDARY_MATERIAL = Material.GRAY_STAINED_GLASS_PANE;
    private static final BackgroundPattern DEFAULT_PATTERN = BackgroundPattern.CLASSIC;

    private static final int ROW_WIDTH = 9;

    private final Chromatica plugin;

    private BackgroundPattern pattern;
    private ItemStack primaryTemplate;
    private ItemStack secondaryTemplate;

    public BackgroundManager(Chromatica plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {

        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        FileConfiguration config = plugin.getConfig();

        this.pattern = parsePattern(config.getString("gui.background.pattern"));

        this.primaryTemplate = buildTemplate(
                config.getString("gui.background.primary.material"),
                config.getString("gui.background.primary.name", " "),
                DEFAULT_PRIMARY_MATERIAL
        );

        this.secondaryTemplate = buildTemplate(
                config.getString("gui.background.secondary.material"),
                config.getString("gui.background.secondary.name", " "),
                DEFAULT_SECONDARY_MATERIAL
        );
    }

    /**
     * Az inventory MÉG ÜRES (null) slotjait tölti fel a konfigurált
     * primary/secondary itemekkel, a kiválasztott minta szerint.
     * A már elhelyezett (funkcionális) itemeket nem írja felül, ezért
     * ezt minden menüben a funkcionális itemek elhelyezése UTÁN kell hívni.
     */
    public void applyBackground(Inventory inventory) {

        int size = inventory.getSize();

        for (int slot = 0; slot < size; slot++) {

            if (inventory.getItem(slot) != null) {
                continue;
            }

            int row = slot / ROW_WIDTH;
            int col = slot % ROW_WIDTH;
            int rows = size / ROW_WIDTH;

            boolean primary = pattern.isPrimary(row, col, rows);

            inventory.setItem(slot, (primary ? primaryTemplate : secondaryTemplate).clone());
        }
    }

    /**
     * Ugyanaz, mint {@link #applyBackground(Inventory)}, de csak a keret
     * (legfelső sor, legalsó sor, illetve a bal/jobb szélső oszlop)
     * kap háttér-itemet; minden belső cellát (a keretet leszámítva)
     * érintetlenül, teljesen üresen hagy - függetlenül attól, hogy ott
     * funkcionális item van-e vagy sem. Olyan menükhöz való, ahol a belső
     * terület kizárólag funkcionális tartalomnak (pl. gradient színeknek)
     * van fenntartva, és nem szabad, hogy háttér-mintázat látszódjon rajta.
     */
    public void applyBorderOnly(Inventory inventory) {

        int size = inventory.getSize();
        int rows = size / ROW_WIDTH;

        for (int slot = 0; slot < size; slot++) {

            int row = slot / ROW_WIDTH;
            int col = slot % ROW_WIDTH;

            boolean isBorder = row == 0 || row == rows - 1
                    || col == 0 || col == ROW_WIDTH - 1;

            if (!isBorder) {
                continue;
            }

            if (inventory.getItem(slot) != null) {
                continue;
            }

            boolean primary = pattern.isPrimary(row, col, rows);

            inventory.setItem(slot, (primary ? primaryTemplate : secondaryTemplate).clone());
        }
    }

    private BackgroundPattern parsePattern(String raw) {

        if (raw == null || raw.isBlank()) {
            return DEFAULT_PATTERN;
        }

        try {
            return BackgroundPattern.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {

            plugin.getLogger().log(
                    Level.WARNING,
                    "Unknown gui.background.pattern '" + raw
                            + "', falling back to " + DEFAULT_PATTERN + "."
            );

            return DEFAULT_PATTERN;
        }
    }

    private ItemStack buildTemplate(String rawMaterial, String name, Material fallback) {

        Material material = fallback;

        if (rawMaterial != null && !rawMaterial.isBlank()) {

            Material parsed = Material.matchMaterial(rawMaterial.trim());

            if (parsed == null) {

                plugin.getLogger().log(
                        Level.WARNING,
                        "Unknown material '" + rawMaterial
                                + "' for gui background, falling back to " + fallback + "."
                );

            } else {
                material = parsed;
            }
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(name == null ? " " : name);
            meta.setHideTooltip(true);
            item.setItemMeta(meta);
        }

        return item;
    }

    public BackgroundPattern getPattern() {
        return pattern;
    }
}