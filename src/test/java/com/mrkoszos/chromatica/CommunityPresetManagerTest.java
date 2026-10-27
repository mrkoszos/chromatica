package com.mrkoszos.chromatica;

import static org.junit.jupiter.api.Assertions.*;

import com.mrkoszos.chromatica.gui.PresetBrowserMenu;
import com.mrkoszos.chromatica.manager.CommunityPresetManager;
import com.mrkoszos.chromatica.model.*;
import java.nio.file.*;
import java.util.*;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CommunityPresetManagerTest {
  @TempDir Path folder;

  private CommunityPresetManager manager() {
    return new CommunityPresetManager(folder.toFile(), Logger.getAnonymousLogger());
  }

  private NameStyle style() {
    NameStyle style = new NameStyle();
    style.setType(StyleType.GRADIENT);
    style.setPrimaryColor("#123456");
    style.setSecondaryColor("#654321");
    style.setGradientColors(List.of("#112233", "#AABBCC", "#FF0000"));
    style.setPresetId("original-preset");
    style.setBold(true);
    style.setItalic(true);
    style.setUnderlined(true);
    style.setStrikethrough(true);
    return style;
  }

  @Test
  void fullStyleAndMetadataSurviveRestart() {
    UUID owner = UUID.randomUUID();
    CommunityPresetManager manager = manager();
    var result = manager.publish(owner, "Tester", "Sunset", style(), 10, true);
    assertEquals(CommunityPresetManager.Status.SUCCESS, result.status());
    var loaded = manager().get(result.preset().id());
    assertEquals(result.preset().id(), loaded.id());
    assertEquals(owner, loaded.creator());
    assertEquals("Tester", loaded.username());
    assertEquals("Sunset", loaded.name());
    assertTrue(loaded.createdAt() > 0);
    assertEquals(style(), loaded.style());
  }

  @Test
  void namesCanRepeatAndIdsRemainDistinct() {
    var manager = manager();
    UUID first = UUID.randomUUID(), second = UUID.randomUUID();
    var a = manager.publish(first, "A", "Sunset", style(), 10, true).preset();
    var b = manager.publish(second, "B", "Sunset", style(), 10, true).preset();
    var c = manager.publish(first, "A", "Sunset", style(), 10, true).preset();
    assertNotEquals(a.id(), b.id());
    assertNotEquals(a.id(), c.id());
    assertEquals(3, manager().getAll().size());
  }

  @Test
  void ownershipUsesUuidAndAdminCanDeleteOthers() {
    var manager = manager();
    UUID owner = UUID.randomUUID(), other = UUID.randomUUID();
    var preset = manager.publish(owner, "SameUsername", "Mine", style(), 10, true).preset();
    assertFalse(CommunityPresetManager.canDelete(preset, other, false));
    assertTrue(CommunityPresetManager.canDelete(preset, owner, false));
    assertTrue(CommunityPresetManager.canDelete(preset, other, true));
    assertEquals(
        CommunityPresetManager.Status.NOT_OWNER, manager.delete(preset.id(), other, false, true));
    assertNotNull(manager.get(preset.id()));
    assertEquals(
        CommunityPresetManager.Status.SUCCESS, manager.delete(preset.id(), other, true, true));
    assertNull(manager().get(preset.id()));
  }

  @Test
  void ownerDeletionFreesQuotaAndLoweredLimitKeepsExistingEntries() {
    var manager = manager();
    UUID owner = UUID.randomUUID();
    var preset = manager.publish(owner, "Tester", "One", style(), 1, true).preset();
    assertEquals(
        CommunityPresetManager.Status.LIMIT_REACHED,
        manager.publish(owner, "Tester", "Two", style(), 1, true).status());
    assertEquals(
        CommunityPresetManager.Status.LIMIT_REACHED,
        manager.publish(owner, "Tester", "Two", style(), 0, true).status());
    assertEquals(1, manager.count(owner));
    assertEquals(
        CommunityPresetManager.Status.SUCCESS, manager.delete(preset.id(), owner, false, true));
    assertEquals(
        CommunityPresetManager.Status.SUCCESS,
        manager.publish(owner, "Tester", "Two", style(), 1, true).status());
  }

  @Test
  void disabledActionsLeaveStoredDataUntouched() {
    var manager = manager();
    UUID owner = UUID.randomUUID();
    var preset = manager.publish(owner, "Tester", "One", style(), 10, true).preset();
    assertEquals(
        CommunityPresetManager.Status.DISABLED,
        manager.publish(owner, "Tester", "Two", style(), 10, false).status());
    assertEquals(
        CommunityPresetManager.Status.DISABLED, manager.delete(preset.id(), owner, true, false));
    assertEquals(1, manager().getAll().size());
  }

  @Test
  void storedStylesNeverLeakMutableCopies() {
    var manager = manager();
    NameStyle original = style(), expected = original.copy();
    var preset = manager.publish(UUID.randomUUID(), "Tester", "One", original, 10, true).preset();
    original.getGradientColors().clear();
    original.setItalic(false);
    NameStyle applied = preset.style();
    applied.getGradientColors().clear();
    applied.setBold(false);
    manager.getAll().getFirst().style().setPrimaryColor("#FFFFFF");
    assertEquals(expected, manager.get(preset.id()).style());
    assertEquals(expected, manager().get(preset.id()).style());
  }

  @Test
  void nameValidationPreservesUnicodeAndRemovesFormatting() {
    assertEquals("Café 日没", CommunityPresetManager.sanitizeName("  Cafe\u0301 日没  "));
    assertEquals("Sunset", CommunityPresetManager.sanitizeName("<red>§cSunset</red>"));
    assertEquals("Safe", CommunityPresetManager.sanitizeName("\u0000Sa\u202Efe"));
    assertNull(CommunityPresetManager.sanitizeName(" \t\n"));
    assertNull(CommunityPresetManager.sanitizeName("<click:run_command:'/op me'></click>"));
    assertNull(CommunityPresetManager.sanitizeName("a".repeat(33)));
    assertNotNull(CommunityPresetManager.sanitizeName("日".repeat(32)));
    var manager = manager();
    assertEquals(
        CommunityPresetManager.Status.INVALID_NAME,
        manager.publish(UUID.randomUUID(), "Tester", "  ", style(), 10, true).status());
    assertTrue(manager.getAll().isEmpty());
  }

  @Test
  void paginationHasTwentyOneSlotsAndClampsAfterRemoval() {
    assertEquals(21, PresetBrowserMenu.PRESET_SLOTS.length);
    assertEquals(21, Arrays.stream(PresetBrowserMenu.PRESET_SLOTS).distinct().count());
    assertEquals(1, PresetBrowserMenu.pageCount(0));
    assertEquals(1, PresetBrowserMenu.pageCount(21));
    assertEquals(2, PresetBrowserMenu.pageCount(22));
    assertEquals(2, PresetBrowserMenu.pageCount(42));
    assertEquals(3, PresetBrowserMenu.pageCount(43));
    assertEquals(0, PresetBrowserMenu.clampPage(2, 21));
    assertEquals(0, PresetBrowserMenu.clampPage(-1, 50));
    for (int slot : PresetBrowserMenu.PRESET_SLOTS) {
      assertTrue(slot / 9 >= 1 && slot / 9 <= 3);
      assertTrue(slot % 9 >= 1 && slot % 9 <= 7);
    }
  }

  @Test
  void unavailableStorageDoesNotPretendPublicationSucceeded() throws Exception {
    Files.writeString(folder.resolve("community-presets"), "blocks directory creation");
    var manager = manager();
    assertEquals(
        CommunityPresetManager.Status.STORAGE_ERROR,
        manager.publish(UUID.randomUUID(), "Tester", "One", style(), 10, true).status());
    assertTrue(manager.getAll().isEmpty());
  }

  @Test
  void failedDeletionKeepsEntryAndCorruptFilesAreNotOverwritten() throws Exception {
    var manager = manager();
    UUID owner = UUID.randomUUID();
    var preset = manager.publish(owner, "Tester", "One", style(), 10, true).preset();
    Path file = folder.resolve("community-presets").resolve(preset.id() + ".yml");
    Files.delete(file);
    Files.createDirectory(file);
    Files.writeString(file.resolve("block"), "block");
    assertEquals(
        CommunityPresetManager.Status.STORAGE_ERROR,
        manager.delete(preset.id(), owner, false, true));
    assertNotNull(manager.get(preset.id()));
    Path broken = folder.resolve("community-presets").resolve(UUID.randomUUID() + ".yml");
    Files.writeString(broken, "style: [broken");
    var restarted = manager();
    assertEquals(
        CommunityPresetManager.Status.SUCCESS,
        restarted.publish(owner, "Tester", "Valid", style(), 10, true).status());
    assertEquals("style: [broken", Files.readString(broken));
  }

  @Test
  void publicationOrderIsStableAcrossRestartAndPages() {
    var manager = manager();
    UUID owner = UUID.randomUUID();
    for (int i = 0; i < 24; i++) {
      assertEquals(
          CommunityPresetManager.Status.SUCCESS,
          manager.publish(owner, "Tester", "Preset " + i, new NameStyle(), 30, true).status());
    }
    var before = manager.getAll().stream().map(CommunityPresetManager.Preset::id).toList();
    assertEquals(
        before, manager().getAll().stream().map(CommunityPresetManager.Preset::id).toList());
    assertEquals(2, PresetBrowserMenu.pageCount(before.size()));
    assertEquals("Preset 21", manager.getAll().get(21).name());
  }
}
