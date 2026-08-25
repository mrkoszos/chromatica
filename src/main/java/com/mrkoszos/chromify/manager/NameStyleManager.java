package com.mrkoszos.chromify.manager;

import com.mrkoszos.chromify.Chromify;
import com.mrkoszos.chromify.model.NameStyle;
import com.mrkoszos.chromify.model.StyleType;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NameStyleManager {

    private final Chromify plugin;
    private final File folder;

    private final Map<UUID, NameStyle> styles = new HashMap<>();

    public NameStyleManager(Chromify plugin) {

        this.plugin = plugin;

        this.folder = new File(
                plugin.getDataFolder(),
                "playerdata"
        );

        if (!folder.exists()) {
            folder.mkdirs();
        }
    }

    public NameStyle getStyle(UUID uuid) {

        return styles.computeIfAbsent(
                uuid,
                this::loadStyle
        );
    }

    public void setStyle(UUID uuid, NameStyle style) {

        styles.put(uuid, style);

        saveStyle(uuid);
    }

    public void resetStyle(UUID uuid) {

        styles.remove(uuid);

        File file = getFile(uuid);

        if (file.exists()) {
            file.delete();
        }
    }

    public boolean hasStyle(UUID uuid) {
        return styles.containsKey(uuid);
    }

    /*
     * =========================
     *          SAVE
     * =========================
     * A GUI-listenerek ezt hívják meg
     * minden módosítás után.
     */

    public void saveStyle(UUID uuid) {

        NameStyle style = styles.get(uuid);

        if (style == null) {
            return;
        }

        File file = getFile(uuid);

        YamlConfiguration config = new YamlConfiguration();

        config.set("type", style.getType().name());
        config.set("primary-color", style.getPrimaryColor());
        config.set("secondary-color", style.getSecondaryColor());
        config.set("gradient-colors", style.getGradientColors());
        config.set("preset-id", style.getPresetId());
        config.set("bold", style.isBold());
        config.set("italic", style.isItalic());
        config.set("underlined", style.isUnderlined());
        config.set("strikethrough", style.isStrikethrough());

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning(
                    "Failed to save style for " + uuid + ": " + e.getMessage()
            );
        }
    }

    /*
     * =========================
     *          LOAD
     * =========================
     */

    private NameStyle loadStyle(UUID uuid) {

        File file = getFile(uuid);

        if (!file.exists()) {
            return new NameStyle();
        }

        YamlConfiguration config =
                YamlConfiguration.loadConfiguration(file);

        NameStyle style = new NameStyle();

        String typeName = config.getString("type", "SOLID");

        try {
            style.setType(StyleType.valueOf(typeName));
        } catch (IllegalArgumentException e) {
            style.setType(StyleType.SOLID);
        }

        style.setPrimaryColor(
                config.getString("primary-color", "#FFFFFF")
        );

        style.setSecondaryColor(
                config.getString("secondary-color", null)
        );

        style.setGradientColors(
                config.getStringList("gradient-colors")
        );

        style.setPresetId(
                config.getString("preset-id", null)
        );

        style.setBold(config.getBoolean("bold", false));
        style.setItalic(config.getBoolean("italic", false));
        style.setUnderlined(config.getBoolean("underlined", false));
        style.setStrikethrough(config.getBoolean("strikethrough", false));

        return style;
    }

    private File getFile(UUID uuid) {
        return new File(folder, uuid.toString() + ".yml");
    }
}