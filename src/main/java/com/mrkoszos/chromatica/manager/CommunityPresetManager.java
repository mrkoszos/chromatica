package com.mrkoszos.chromatica.manager;

import com.mrkoszos.chromatica.model.NameStyle;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.text.Normalizer;
import java.util.*;
import java.util.logging.Logger;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Server-local presets. Each UUID owns a separate file; favorites and server presets are untouched.
 */
public final class CommunityPresetManager {
  public static final int MAX_NAME_LENGTH = 32;

  public record Preset(
      UUID id, String name, UUID creator, String username, long createdAt, NameStyle style) {
    public Preset {
      style = style.copy();
    }

    @Override
    public NameStyle style() {
      return style.copy();
    }
  }

  public enum Status {
    SUCCESS,
    DISABLED,
    INVALID_NAME,
    LIMIT_REACHED,
    NOT_FOUND,
    NOT_OWNER,
    STORAGE_ERROR
  }

  public record Result(Status status, Preset preset) {}

  private final Path folder;
  private final Logger logger;
  private final Map<UUID, Preset> presets = new LinkedHashMap<>();
  private boolean available = true;

  public CommunityPresetManager(File dataFolder, Logger logger) {
    folder = dataFolder.toPath().resolve("community-presets");
    this.logger = logger;
    load(false);
  }

  public synchronized boolean reload() { return load(true); }

  private boolean load(boolean reload) {
    try {
      Files.createDirectories(folder);
      List<Preset> loaded = new ArrayList<>();
      try (var paths = Files.list(folder)) {
        for (Path file :
            paths.filter(path -> path.getFileName().toString().endsWith(".yml")).toList()) {
          try {
            if (Files.size(file) > 262144) throw new IOException("Preset file exceeds 256 KiB");
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.load(file.toFile());
            if (yaml.getInt("schema-version", 1) != 1)
              throw new IOException("Unsupported schema version");
            UUID id = UUID.fromString(yaml.getString("id", ""));
            if (!file.getFileName().toString().equals(id + ".yml"))
              throw new IOException("ID does not match filename");
            var style = yaml.getConfigurationSection("style");
            String name = sanitizeName(yaml.getString("name", ""));
            if (style == null || name == null) throw new IOException("Invalid preset data");
            loaded.add(
                new Preset(
                    id,
                    name,
                    UUID.fromString(yaml.getString("creator-uuid", "")),
                    yaml.getString("creator-name", "Unknown"),
                    yaml.getLong("created-at"),
                    NameStyleCodec.read(style)));
          } catch (Exception exception) {
            if (reload) throw new IOException("Invalid community preset " + file.getFileName(), exception);
            logger.warning(
                "Cannot load community preset "
                    + file.getFileName()
                    + ": "
                    + exception.getMessage());
          }
        }
      }
      loaded.sort(
          Comparator.comparingLong(Preset::createdAt)
              .thenComparing(preset -> preset.id().toString()));
      presets.clear();
      loaded.forEach(preset -> presets.put(preset.id(), preset));
      available = true;
      return true;
    } catch (IOException exception) {
      if (!reload) available = false;
      logger.warning("Community preset reload failed; keeping previous data: " + exception.getMessage());
      return false;
    }
  }

  /**
   * Keep literal Unicode text, remove formatting/control sequences, and never parse MiniMessage.
   */
  public static String sanitizeName(String input) {
    if (input == null) return null;
    String text =
        Normalizer.normalize(input, Normalizer.Form.NFC)
            .replaceAll("(?i)[§&][0-9a-fk-orx]", "")
            .replaceAll("<[^>]*>", "");
    StringBuilder clean = new StringBuilder();
    text.codePoints()
        .filter(cp -> !Character.isISOControl(cp) && Character.getType(cp) != Character.FORMAT)
        .forEach(clean::appendCodePoint);
    text = clean.toString().strip();
    return text.isBlank() || text.codePointCount(0, text.length()) > MAX_NAME_LENGTH ? null : text;
  }

  public synchronized List<Preset> getAll() {
    return presets.values().stream()
        .sorted(
            Comparator.comparingLong(Preset::createdAt)
                .thenComparing(preset -> preset.id().toString()))
        .toList();
  }

  public synchronized Preset get(UUID id) {
    return presets.get(id);
  }

  public synchronized long count(UUID creator) {
    return presets.values().stream().filter(preset -> preset.creator().equals(creator)).count();
  }

  public static boolean canDelete(Preset preset, UUID actor, boolean admin) {
    return preset != null && (admin || preset.creator().equals(actor));
  }

  public synchronized Result publish(
      UUID creator, String username, String name, NameStyle style, int limit, boolean enabled) {
    if (!enabled) return new Result(Status.DISABLED, null);
    if (!available) return new Result(Status.STORAGE_ERROR, null);
    String clean = sanitizeName(name);
    if (clean == null) return new Result(Status.INVALID_NAME, null);
    if (count(creator) >= Math.max(0, limit)) return new Result(Status.LIMIT_REACHED, null);
    try { style = NameStyleCodec.validated(style); }
    catch (IllegalArgumentException exception) {
      logger.warning("Invalid community style: " + exception.getMessage());
      return new Result(Status.STORAGE_ERROR, null);
    }
    UUID id;
    do {
      id = UUID.randomUUID();
    } while (Files.exists(folder.resolve(id + ".yml")));
    long newest = presets.values().stream().mapToLong(Preset::createdAt).max().orElse(0);
    Preset preset =
        new Preset(
            id, clean, creator, username, Math.max(System.currentTimeMillis(), newest + 1), style);
    YamlConfiguration yaml = new YamlConfiguration();
    yaml.set("schema-version", 1);
    yaml.set("id", id.toString());
    yaml.set("name", clean);
    yaml.set("creator-uuid", creator.toString());
    yaml.set("creator-name", username);
    yaml.set("created-at", preset.createdAt());
    NameStyleCodec.write(yaml.createSection("style"), preset.style());
    Path temporary = null;
    try {
      temporary = Files.createTempFile(folder, "publish-", ".tmp");
      yaml.save(temporary.toFile());
      Path target = folder.resolve(id + ".yml");
      try {
        Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
      } catch (AtomicMoveNotSupportedException exception) {
        Files.move(temporary, target);
      }
      presets.put(id, preset);
      return new Result(Status.SUCCESS, preset);
    } catch (IOException exception) {
      logger.warning("Cannot publish community preset: " + exception.getMessage());
      return new Result(Status.STORAGE_ERROR, null);
    } finally {
      if (temporary != null)
        try {
          Files.deleteIfExists(temporary);
        } catch (IOException ignored) {
        }
    }
  }

  public synchronized Status delete(UUID id, UUID actor, boolean admin, boolean enabled) {
    if (!enabled) return Status.DISABLED;
    Preset preset = presets.get(id);
    if (preset == null) return Status.NOT_FOUND;
    if (!canDelete(preset, actor, admin)) return Status.NOT_OWNER;
    try {
      Files.deleteIfExists(folder.resolve(id + ".yml"));
      presets.remove(id);
      return Status.SUCCESS;
    } catch (IOException exception) {
      logger.warning("Cannot delete community preset " + id + ": " + exception.getMessage());
      return Status.STORAGE_ERROR;
    }
  }
}
