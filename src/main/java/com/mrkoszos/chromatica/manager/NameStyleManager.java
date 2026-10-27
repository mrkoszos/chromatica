package com.mrkoszos.chromatica.manager;

import com.mrkoszos.chromatica.Chromatica;
import com.mrkoszos.chromatica.model.NameStyle;
import java.io.File;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import org.bukkit.configuration.file.YamlConfiguration;

/** Published values are never mutated or exposed. Async readers never perform disk I/O. */
public class NameStyleManager {
  private final Path folder;
  private final Logger logger;
  private final Map<UUID, NameStyle> styles = new ConcurrentHashMap<>();
  private final Set<UUID> unreadable = new HashSet<>();

  public NameStyleManager(Chromatica plugin) { this(plugin.getDataFolder(), plugin.getLogger()); }
  public NameStyleManager(File dataFolder, Logger logger) {
    this.folder = dataFolder.toPath().resolve("playerdata");
    this.logger = logger;
  }

  /** Main-thread load/edit entry point. Callers own the returned draft. */
  public synchronized NameStyle getStyle(UUID uuid) {
    return styles.computeIfAbsent(uuid, this::loadStyle).copy();
  }

  /** Lock-free detached snapshot, with no disk I/O even for uncached players. */
  public NameStyle getSnapshot(UUID uuid) {
    NameStyle style = styles.get(uuid);
    return style == null ? new NameStyle() : style.copy();
  }

  /** Publish only after file replacement succeeds. Failure retains the previous value. */
  public synchronized boolean setStyle(UUID uuid, NameStyle style) {
    getStyle(uuid);
    if (unreadable.contains(uuid)) return false;
    Path temporary = null;
    try {
      NameStyle next = NameStyleCodec.validated(style);
      Files.createDirectories(folder);
      temporary = Files.createTempFile(folder, uuid + "-", ".tmp");
      YamlConfiguration yaml = new YamlConfiguration();
      NameStyleCodec.write(yaml, next);
      yaml.save(temporary.toFile());
      try {
        Files.move(temporary, file(uuid), StandardCopyOption.ATOMIC_MOVE,
            StandardCopyOption.REPLACE_EXISTING);
      } catch (AtomicMoveNotSupportedException exception) {
        Files.move(temporary, file(uuid), StandardCopyOption.REPLACE_EXISTING);
      }
      styles.put(uuid, next);
      return true;
    } catch (Exception exception) {
      logger.warning("Cannot save player style for " + uuid + ": " + exception.getMessage());
      return false;
    } finally {
      if (temporary != null) try { Files.deleteIfExists(temporary); }
      catch (java.io.IOException exception) { logger.warning("Cannot remove style temporary file: " + exception.getMessage()); }
    }
  }

  /** Rewrite the committed snapshot; persist edited drafts with setStyle instead. */
  public synchronized boolean saveStyle(UUID uuid) { return setStyle(uuid, getStyle(uuid)); }

  public synchronized boolean resetStyle(UUID uuid) {
    try {
      Files.deleteIfExists(file(uuid));
      styles.put(uuid, new NameStyle());
      unreadable.remove(uuid);
      return true;
    } catch (java.io.IOException exception) {
      logger.warning("Cannot reset player style for " + uuid + ": " + exception.getMessage());
      return false;
    }
  }

  public boolean hasStyle(UUID uuid) { return styles.containsKey(uuid); }
  public synchronized void unload(UUID uuid) { styles.remove(uuid); unreadable.remove(uuid); }

  private NameStyle loadStyle(UUID uuid) {
    if (!Files.exists(file(uuid))) return new NameStyle();
    try {
      if (Files.size(file(uuid)) > 262144) throw new java.io.IOException("Style file exceeds 256 KiB");
      YamlConfiguration yaml = new YamlConfiguration();
      yaml.load(file(uuid).toFile());
      return NameStyleCodec.read(yaml);
    } catch (Exception exception) {
      unreadable.add(uuid);
      logger.warning("Cannot load player style for " + uuid + "; preserving file: " + exception.getMessage());
      return new NameStyle();
    }
  }
  private Path file(UUID uuid) { return folder.resolve(uuid + ".yml"); }
}
