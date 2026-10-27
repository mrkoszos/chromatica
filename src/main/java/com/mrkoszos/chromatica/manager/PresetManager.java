package com.mrkoszos.chromatica.manager;

import com.mrkoszos.chromatica.Chromatica;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public class PresetManager {

  private static final Material DEFAULT_MATERIAL = Material.NETHER_STAR;

  /*
   * =========================
   *     PRESET DEFINITION
   * =========================
   * colors.size() == 1  -> solid szín preset
   * colors.size() >= 2  -> gradient preset
   */
  public record PresetDefinition(
      String id, String display, List<String> colors, Material material, Boolean scramble) {
    public PresetDefinition(String id, String display, List<String> colors, Material material) {
      this(id, display, colors, material, null);
    }

    /** Absent in legacy color-only presets: preserve the player's formatting. */
    public void applyFormatting(com.mrkoszos.chromatica.model.NameStyle style) {
      if (scramble != null) style.setScramble(scramble);
    }
  }

  private final Chromatica plugin;
  private final Map<String, PresetDefinition> presets = new LinkedHashMap<>();

  public PresetManager(Chromatica plugin) {
    this.plugin = plugin;
    reload();
  }

  public boolean reload() {

    Map<String, PresetDefinition> loaded = new LinkedHashMap<>();

    File file = new File(plugin.getDataFolder(), "presets.yml");

    if (!file.exists()) {
      plugin.saveResource("presets.yml", false);
    }

    YamlConfiguration yaml = new YamlConfiguration();
    try {
      yaml.load(file);
    } catch (Exception exception) {
      plugin
          .getLogger()
          .warning(
              "Cannot reload server presets; keeping previous data: " + exception.getMessage());
      return false;
    }

    ConfigurationSection section = yaml.getConfigurationSection("presets");

    if (section == null) {
      plugin.getLogger().warning("Missing presets section; keeping previous server presets.");
      return false;
    }

    for (String id : section.getKeys(false)) {

      String display = section.getString(id + ".display", id);

      List<String> colors = section.getStringList(id + ".colors");

      Object rawColors = section.get(id + ".colors");
      if (!(rawColors instanceof List<?> raw)
          || raw.size() != colors.size()
          || colors.isEmpty()
          || colors.size() > NameStyleCodec.MAX_STOPS
          || colors.stream().anyMatch(color -> !NameStyleCodec.isHex(color))) {
        plugin.getLogger().warning("Invalid server preset " + id + "; keeping previous catalogue.");
        return false;
      }

      Material material = parseMaterial(id, section.getString(id + ".material"));

      Object scramble = section.get(id + ".scramble");
      if (scramble != null && !(scramble instanceof Boolean)) {
        plugin
            .getLogger()
            .warning(
                "Invalid scramble flag in server preset " + id + "; keeping previous catalogue.");
        return false;
      }
      loaded.put(
          id, new PresetDefinition(id, display, List.copyOf(colors), material, (Boolean) scramble));
    }
    presets.clear();
    presets.putAll(loaded);
    return true;
  }

  private Material parseMaterial(String presetId, String raw) {

    if (raw == null || raw.isBlank()) {
      return DEFAULT_MATERIAL;
    }

    Material material = Material.matchMaterial(raw.trim());

    if (material == null || !material.isItem() || material.isAir()) {

      plugin
          .getLogger()
          .log(
              Level.WARNING,
              "Unknown material '"
                  + raw
                  + "' for preset '"
                  + presetId
                  + "', falling back to "
                  + DEFAULT_MATERIAL
                  + ".");

      return DEFAULT_MATERIAL;
    }

    return material;
  }

  public Map<String, PresetDefinition> getAll() {
    return java.util.Collections.unmodifiableMap(presets);
  }

  public PresetDefinition get(String id) {
    return presets.get(id);
  }
}
