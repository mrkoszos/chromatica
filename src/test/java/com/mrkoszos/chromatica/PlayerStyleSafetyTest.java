package com.mrkoszos.chromatica;

import static org.junit.jupiter.api.Assertions.*;
import com.mrkoszos.chromatica.manager.*;
import com.mrkoszos.chromatica.model.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PlayerStyleSafetyTest {
  @TempDir Path folder;
  NameStyleManager manager() { return new NameStyleManager(folder.toFile(), Logger.getAnonymousLogger()); }
  NameStyle style(String hex) { NameStyle s = new NameStyle(); s.setPrimaryColor(hex); return s; }

  @Test void detachedDraftsAndSnapshotsSurviveRestart() {
    UUID id = UUID.randomUUID(); NameStyleManager m = manager(); NameStyle source = style("#123456");
    source.setBold(true); source.setGradientColors(List.of("#112233", "#445566"));
    assertTrue(m.setStyle(id, source)); NameStyle expected = source.copy();
    source.setBold(false); m.getStyle(id).setPrimaryColor("#000000");
    m.getSnapshot(id).getGradientColors().clear();
    assertEquals(expected, m.getSnapshot(id)); assertEquals(expected, manager().getStyle(id));
    m.unload(id); assertFalse(m.hasStyle(id)); assertEquals(expected, m.getStyle(id));
  }

  @Test void snapshotNeverLoadsEvenWhenDiskFileExists() throws Exception {
    UUID id = UUID.randomUUID(); NameStyleManager writer = manager();
    assertTrue(writer.setStyle(id, style("#123456")));
    NameStyleManager reader = manager();
    try (var executor = Executors.newSingleThreadExecutor()) {
      assertEquals("#FFFFFF", executor.submit(() -> reader.getSnapshot(id).getPrimaryColor()).get(2, TimeUnit.SECONDS));
    }
    assertFalse(reader.hasStyle(id)); assertEquals("#123456", reader.getStyle(id).getPrimaryColor());
  }

  @Test void failedReplacementKeepsCacheAndCanRecover() throws Exception {
    UUID id = UUID.randomUUID(); NameStyleManager m = manager();
    assertTrue(m.setStyle(id, style("#123456")));
    Path target = folder.resolve("playerdata/" + id + ".yml");
    Files.delete(target); Files.createDirectory(target); Files.writeString(target.resolve("block"), "x");
    assertFalse(m.setStyle(id, style("#654321")));
    assertEquals("#123456", m.getSnapshot(id).getPrimaryColor());
    assertEquals("x", Files.readString(target.resolve("block")));
    Files.delete(target.resolve("block")); Files.delete(target);
    assertTrue(m.setStyle(id, style("#654321")));
    assertEquals("#654321", manager().getStyle(id).getPrimaryColor());
    try (var files = Files.list(target.getParent())) { assertEquals(1, files.count()); }
  }

  @Test void failedResetRetainsStyleUntilDeletionSucceeds() throws Exception {
    UUID id = UUID.randomUUID(); NameStyleManager m = manager();
    assertTrue(m.setStyle(id, style("#123456")));
    Path target = folder.resolve("playerdata/" + id + ".yml");
    Files.delete(target); Files.createDirectory(target); Files.writeString(target.resolve("block"), "x");
    assertFalse(m.resetStyle(id)); assertEquals("#123456", m.getSnapshot(id).getPrimaryColor());
    Files.delete(target.resolve("block")); Files.delete(target);
    assertTrue(m.resetStyle(id)); assertEquals("#FFFFFF", m.getSnapshot(id).getPrimaryColor());
    assertEquals("#FFFFFF", manager().getStyle(id).getPrimaryColor());
  }

  @Test void corruptStyleIsPreservedAndRepairRecoversAfterUnload() throws Exception {
    UUID id = UUID.randomUUID(); Path target = Files.createDirectories(folder.resolve("playerdata")).resolve(id + ".yml");
    Files.writeString(target, "primary-color: not-a-color"); NameStyleManager m = manager();
    assertEquals("#FFFFFF", m.getStyle(id).getPrimaryColor());
    assertFalse(m.setStyle(id, style("#123456")));
    assertEquals("primary-color: not-a-color", Files.readString(target));
    Files.writeString(target, "primary-color: '#123456'"); m.unload(id);
    assertEquals("#123456", m.getStyle(id).getPrimaryColor());
    assertTrue(m.setStyle(id, style("#654321")));
  }

  @Test void concurrentReadersOnlySeeCompleteCommittedStyles() throws Exception {
    UUID id = UUID.randomUUID(); NameStyleManager m = manager();
    NameStyle a = style("#123456"); a.setBold(true); NameStyle b = style("#654321");
    assertTrue(m.setStyle(id, a));
    try (var executor = Executors.newFixedThreadPool(3)) {
      var reads = new ArrayList<Future<?>>();
      for (int thread = 0; thread < 2; thread++) reads.add(executor.submit(() -> {
        for (int i = 0; i < 10000; i++) {
          NameStyle snapshot = m.getSnapshot(id);
          assertEquals(snapshot.getPrimaryColor().equals("#123456"), snapshot.isBold());
          snapshot.setPrimaryColor("#000000");
        }
      }));
      for (int i = 0; i < 20; i++) assertTrue(m.setStyle(id, i % 2 == 0 ? b : a));
      for (Future<?> read : reads) read.get(5, TimeUnit.SECONDS);
    }
  }

  @Test void malformedFieldsAndGradientCountsAreRejected() {
    for (String yaml : List.of("type: UNKNOWN", "type: GRADIENT", "primary-color: '#xyzxyz'",
        "bold: yesplease", "gradient-colors: [123, '#FFFFFF']")) {
      YamlConfiguration config = new YamlConfiguration();
      assertThrows(IllegalArgumentException.class, () -> { config.loadFromString(yaml); NameStyleCodec.read(config); });
    }
    NameStyle oversized = style("#FFFFFF"); oversized.setType(StyleType.GRADIENT);
    oversized.setGradientColors(Collections.nCopies(22, "#FFFFFF"));
    assertThrows(IllegalArgumentException.class, () -> NameStyleCodec.validated(oversized));
    YamlConfiguration legacy = new YamlConfiguration(); legacy.set("type", "PRESET");
    legacy.set("gradient-colors", List.of("#112233", "#445566"));
    assertEquals(StyleType.GRADIENT, NameStyleCodec.read(legacy).getType());
  }

  @Test void communityReloadIsTransactionalAndImportsValidExternalEdits() throws Exception {
    CommunityPresetManager m = new CommunityPresetManager(folder.toFile(), Logger.getAnonymousLogger());
    UUID owner = UUID.randomUUID(); var preset = m.publish(owner, "Tester", "Before", style("#123456"), 10, true).preset();
    Path file = folder.resolve("community-presets/" + preset.id() + ".yml");
    String original = Files.readString(file); Files.writeString(file, "name: [broken");
    assertFalse(m.reload()); assertEquals("Before", m.get(preset.id()).name());
    Files.writeString(file, original.replace("Before", "After"));
    assertTrue(m.reload()); assertEquals("After", m.get(preset.id()).name());
    Files.delete(file); assertTrue(m.reload()); assertTrue(m.getAll().isEmpty());
  }

  @Test void oversizedCommunityFilesAreRejectedWithoutDiscardingCatalogue() throws Exception {
    CommunityPresetManager m = new CommunityPresetManager(folder.toFile(), Logger.getAnonymousLogger());
    var p = m.publish(UUID.randomUUID(), "Tester", "Saved", style("#123456"), 10, true).preset();
    Files.writeString(folder.resolve("community-presets/" + p.id() + ".yml"), "x".repeat(262145));
    assertFalse(m.reload()); assertEquals("Saved", m.get(p.id()).name());
  }

  @Test void malformedFavoriteFieldsDoNotCrashOrOverwriteFile() throws Exception {
    UUID owner = UUID.randomUUID(); Path file = Files.createDirectories(folder.resolve("favorites")).resolve(owner + ".yml");
    String invalid = "slots:\n  0:\n    type: GRADIENT\n    gradient-colors: ['#000000']\n";
    Files.writeString(file, invalid);
    FavoriteManager m = new FavoriteManager(folder.toFile(), Logger.getAnonymousLogger());
    assertNull(m.getFavorite(owner, 0)); assertFalse(m.setFavorite(owner, 0, style("#123456")));
    assertEquals(invalid, Files.readString(file));
  }

  @Test void malformedServerPresetReloadPreservesValidCatalogue() throws Exception {
    Chromatica plugin = org.mockito.Mockito.mock(Chromatica.class);
    org.mockito.Mockito.when(plugin.getDataFolder()).thenReturn(folder.toFile());
    org.mockito.Mockito.when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());
    Path file = folder.resolve("presets.yml");
    Files.writeString(file, "presets:\n  saved:\n    colors: ['#123456']\n");
    PresetManager m = new PresetManager(plugin); assertNotNull(m.get("saved"));
    Files.writeString(file, "presets:\n  bad:\n    colors: ['not-hex']\n");
    assertFalse(m.reload()); assertNotNull(m.get("saved")); assertNull(m.get("bad"));
    Files.writeString(file, "presets: [broken"); assertFalse(m.reload()); assertNotNull(m.get("saved"));
    Files.writeString(file, "presets:\n  replacement:\n    colors: ['#654321']\n");
    assertTrue(m.reload()); assertNull(m.get("saved")); assertNotNull(m.get("replacement"));
  }
}
