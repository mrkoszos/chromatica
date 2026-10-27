package com.mrkoszos.chromatica.manager;

import com.mrkoszos.chromatica.model.NameStyle;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.logging.Logger;
import org.bukkit.configuration.file.YamlConfiguration;

public class FavoriteManager {
  public static final int MAX_FAVORITES = 5;
  private final Map<UUID, NameStyle[]> favorites = new HashMap<>();
  private final Set<UUID> unreadable = new HashSet<>();
  private final File folder;
  private final Logger logger;

  public FavoriteManager(File dataFolder, Logger logger) {
    this.folder = new File(dataFolder, "favorites");
    this.logger = logger;
  }

  private NameStyle[] getArray(UUID uuid) {
    return favorites.computeIfAbsent(uuid, this::load);
  }

  private NameStyle[] load(UUID uuid) {
    NameStyle[] result = new NameStyle[MAX_FAVORITES];
    File file = new File(folder, uuid + ".yml");
    if (!file.exists()) return result;
    YamlConfiguration config = new YamlConfiguration();
    try {
      if (file.length() > 262144) throw new IOException("Favorites file exceeds 256 KiB");
      config.load(file);
    } catch (Exception exception) {
      unreadable.add(uuid);
      logger.warning("Cannot load favorites for " + uuid + ": " + exception.getMessage());
      return result;
    }
    for (int i = 0; i < MAX_FAVORITES; i++) {
      var section = config.getConfigurationSection("slots." + i);
      if (section != null) {
        try { result[i] = NameStyleCodec.read(section); }
        catch (IllegalArgumentException exception) {
          unreadable.add(uuid);
          logger.warning("Invalid favorite for " + uuid + ": " + exception.getMessage());
          return new NameStyle[MAX_FAVORITES];
        }
      }
    }
    return result;
  }

  public NameStyle[] getAll(UUID uuid) {
    return Arrays.stream(getArray(uuid))
        .map(style -> style == null ? null : style.copy())
        .toArray(NameStyle[]::new);
  }

  public NameStyle getFavorite(UUID uuid, int index) {
    if (index < 0 || index >= MAX_FAVORITES) return null;
    NameStyle style = getArray(uuid)[index];
    return style == null ? null : style.copy();
  }

  public boolean setFavorite(UUID uuid, int index, NameStyle style) {
    if (index < 0 || index >= MAX_FAVORITES || style == null) return false;
    try { return update(uuid, index, NameStyleCodec.validated(style)); }
    catch (IllegalArgumentException exception) { return false; }
  }

  public boolean removeFavorite(UUID uuid, int index) {
    if (index < 0 || index >= MAX_FAVORITES) return false;
    return update(uuid, index, null);
  }

  private boolean update(UUID uuid, int index, NameStyle style) {
    NameStyle[] next = getArray(uuid).clone();
    if (unreadable.contains(uuid)) return false;
    next[index] = style;
    YamlConfiguration config = new YamlConfiguration();
    config.set("schema-version", 1);
    for (int i = 0; i < MAX_FAVORITES; i++) {
      if (next[i] != null) NameStyleCodec.write(config.createSection("slots." + i), next[i]);
    }
    Path temporary = null;
    try {
      Files.createDirectories(folder.toPath());
      temporary = Files.createTempFile(folder.toPath(), uuid + "-", ".tmp");
      config.save(temporary.toFile());
      Path target = new File(folder, uuid + ".yml").toPath();
      try {
        Files.move(
            temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      } catch (AtomicMoveNotSupportedException exception) {
        Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
      }
      favorites.put(uuid, next);
      return true;
    } catch (IOException exception) {
      logger.warning("Failed to save favorites for " + uuid + ": " + exception.getMessage());
      return false;
    } finally {
      if (temporary != null)
        try {
          Files.deleteIfExists(temporary);
        } catch (IOException ignored) {
        }
    }
  }

  public boolean isEmpty(UUID uuid, int index) {
    return getFavorite(uuid, index) == null;
  }

  public void unload(UUID uuid) {
    favorites.remove(uuid);
    unreadable.remove(uuid);
  }
}
