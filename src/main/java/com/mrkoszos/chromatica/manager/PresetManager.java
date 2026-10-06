package com.mrkoszos.chromatica.manager;

import com.mrkoszos.chromatica.Chromatica;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class PresetManager {

    private static final Material DEFAULT_MATERIAL = Material.NETHER_STAR;

    /*
     * =========================
     *     PRESET DEFINITION
     * =========================
     * colors.size() == 1  -> solid szín preset
     * colors.size() >= 2  -> gradient preset
     */
    public record PresetDefinition(String id, String display, List<String> colors, Material material) {
    }

    private final Chromatica plugin;
    private final Map<String, PresetDefinition> presets = new LinkedHashMap<>();

    public PresetManager(Chromatica plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {

        presets.clear();

        File file = new File(plugin.getDataFolder(), "presets.yml");

        if (!file.exists()) {
            plugin.saveResource("presets.yml", false);
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection section = yaml.getConfigurationSection("presets");

        if (section == null) {
            return;
        }

        for (String id : section.getKeys(false)) {

            String display = section.getString(id + ".display", id);

            List<String> colors = section.getStringList(id + ".colors");

            if (colors.isEmpty()) {
                continue;
            }

            Material material = parseMaterial(id, section.getString(id + ".material"));

            presets.put(id, new PresetDefinition(id, display, new ArrayList<>(colors), material));
        }
    }

    private Material parseMaterial(String presetId, String raw) {

        if (raw == null || raw.isBlank()) {
            return DEFAULT_MATERIAL;
        }

        Material material = Material.matchMaterial(raw.trim());

        if (material == null) {

            plugin.getLogger().log(
                    Level.WARNING,
                    "Unknown material '" + raw + "' for preset '" + presetId
                            + "', falling back to " + DEFAULT_MATERIAL + "."
            );

            return DEFAULT_MATERIAL;
        }

        return material;
    }

    public Map<String, PresetDefinition> getAll() {
        return presets;
    }

    public PresetDefinition get(String id) {
        return presets.get(id);
    }
}

