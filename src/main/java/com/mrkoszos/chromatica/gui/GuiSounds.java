package com.mrkoszos.chromatica.gui;

import com.mrkoszos.chromatica.Chromatica;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

/** One semantic sound per completed interaction; rendering never plays sounds. */
public final class GuiSounds {
  public enum Event {
    BOLD_ON("bold-on", "ITEM_MACE_SMASH_GROUND_HEAVY", 0.18f, 1.2f),
    BOLD_OFF("bold-off", "ITEM_MACE_SMASH_AIR", 0.2f, 1.3f),
    ITALIC_ON("italic-on", "BLOCK_ENCHANTMENT_TABLE_USE", 0.3f, 1.2f),
    ITALIC_OFF("italic-off", "ITEM_BOOK_PAGE_TURN", 0.25f, 1f),
    SCRAMBLE_ON("scramble-on", "ITEM_BRUSH_BRUSHING_GENERIC", 0.3f, 1.4f),
    SCRAMBLE_OFF("scramble-off", "ITEM_BRUSH_BRUSHING_GENERIC", 0.25f, 0.8f),
    STRIKETHROUGH_ON("strikethrough-on", "ENTITY_SHEEP_SHEAR", 0.3f, 1.2f),
    STRIKETHROUGH_OFF("strikethrough-off", "ITEM_SHEARS_SNIP", 0.2f, 0.8f),
    UNDERLINE_ON("underline-on", "BLOCK_BAMBOO_PLACE", 0.25f, 1.2f),
    UNDERLINE_OFF("underline-off", "BLOCK_BAMBOO_WOOD_BUTTON_CLICK_OFF", 0.2f, 0.8f),
    SELECT("select", "UI_BUTTON_CLICK", .3f, 1.2f),
    NAVIGATE("navigate", "UI_BUTTON_CLICK", .25f, 1f),
    CONFIRM("confirm", "ENTITY_EXPERIENCE_ORB_PICKUP", .4f, 1.4f),
    PUBLISH("publish", "ENTITY_PLAYER_LEVELUP", .3f, 1.5f),
    DELETE("delete", "BLOCK_NOTE_BLOCK_BASS", .3f, .8f),
    ADMIN_DELETE("admin-delete", "BLOCK_NOTE_BLOCK_BASS", .3f, 1f),
    LIMIT("limit", "BLOCK_NOTE_BLOCK_BASS", .3f, .65f),
    INVALID_NAME("invalid-name", "BLOCK_NOTE_BLOCK_BASS", .3f, .6f),
    ERROR("error", "BLOCK_NOTE_BLOCK_BASS", .3f, .5f);
    final String path, sound;
    final float volume, pitch;

    Event(String path, String sound, float volume, float pitch) {
      this.path = path;
      this.sound = sound;
      this.volume = volume;
      this.pitch = pitch;
    }
  }

  private boolean enabled = true;
  private final Map<Event, Sound> sounds = new EnumMap<>(Event.class);

  public void reload(ConfigurationSection config, Logger logger) {
    reload(config, logger, GuiSounds::resolve);
  }

  void reload(ConfigurationSection config, Logger logger, Function<String, Key> resolver) {
    enabled = config.getBoolean("gui.sounds.enabled", true);
    sounds.clear();
    for (Event event : Event.values()) {
      String base = "gui.sounds." + event.path;
      String raw = config.getString(base + ".sound", event.sound);
      Key key;
      try {
        key = resolver.apply(raw);
        if (key == null) throw new IllegalArgumentException("Unknown sound");
      } catch (RuntimeException exception) {
        logger.warning("Invalid " + base + ".sound '" + raw + "'; using " + event.sound + ".");
        key = resolver.apply(event.sound);
      }
      float volume = number(config, logger, base + ".volume", event.volume, 0, 1);
      float pitch = number(config, logger, base + ".pitch", event.pitch, .5f, 2);
      sounds.put(event, Sound.sound(key, Sound.Source.MASTER, volume, pitch));
    }
  }

  private static float number(
      ConfigurationSection config,
      Logger logger,
      String path,
      float fallback,
      float min,
      float max) {
    Object raw = config.get(path);
    if (raw == null) return fallback;
    if (raw instanceof Number number
        && Double.isFinite(number.doubleValue())
        && number.doubleValue() >= min
        && number.doubleValue() <= max) return number.floatValue();
    logger.warning(
        "Invalid "
            + path
            + " '"
            + raw
            + "'; expected "
            + min
            + "–"
            + max
            + "; using "
            + fallback
            + ".");
    return fallback;
  }

  private static Key resolve(String raw) {
    String name = raw.trim();
    NamespacedKey key;
    if (name.contains(":") || name.contains(".")) {
      key = NamespacedKey.fromString(name.toLowerCase(Locale.ROOT));
    } else {
      // Bukkit symbolic names flatten dots to underscores, preserving key underscores.
      key =
          Registry.SOUND_EVENT
              .keyStream()
              .filter(
                  candidate ->
                      candidate.getNamespace().equals("minecraft")
                          && candidate.getKey().replace('.', '_').equalsIgnoreCase(name))
              .findFirst()
              .orElse(null);
    }
    return key == null || Registry.SOUND_EVENT.get(key) == null ? null : Key.key(key.toString());
  }

  public void play(Player player, Event event) {
    if (enabled && sounds.containsKey(event)) player.playSound(sounds.get(event));
  }

  public static void play(Chromatica plugin, Player player, Event event) {
    if (plugin.getGuiSounds() != null) plugin.getGuiSounds().play(player, event);
  }
}
