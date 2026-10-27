package com.mrkoszos.chromatica;

import static org.junit.jupiter.api.Assertions.*;

import com.mrkoszos.chromatica.gui.GradientSession;
import com.mrkoszos.chromatica.manager.*;
import com.mrkoszos.chromatica.model.*;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FavoritePersistenceTest {
  @TempDir Path folder;

  private FavoriteManager manager() {
    return new FavoriteManager(folder.toFile(), Logger.getAnonymousLogger());
  }

  private NameStyle style() {
    NameStyle style = new NameStyle();
    style.setType(StyleType.GRADIENT);
    style.setPrimaryColor("#123456");
    style.setSecondaryColor("#654321");
    style.setGradientColors(List.of("#FF0000", "#00FF00", "#0000FF"));
    style.setPresetId("test-preset");
    style.setBold(true);
    style.setItalic(true);
    style.setUnderlined(true);
    style.setStrikethrough(true);
    return style;
  }

  @Test
  void allFieldsSurviveRestartAndRemovalKeepsFixedSlots() {
    UUID uuid = UUID.randomUUID();
    FavoriteManager first = manager();
    NameStyle original = style();
    for (int i = 0; i < 5; i++) assertTrue(first.setFavorite(uuid, i, original));
    FavoriteManager restarted = manager();
    for (int i = 0; i < 5; i++) assertEquals(original, restarted.getFavorite(uuid, i));
    assertTrue(restarted.removeFavorite(uuid, 1));
    FavoriteManager again = manager();
    assertNull(again.getFavorite(uuid, 1));
    assertEquals(original, again.getFavorite(uuid, 2));
    assertEquals(5, again.getAll(uuid).length);
    assertFalse(again.setFavorite(uuid, 5, original));
    assertFalse(again.removeFavorite(uuid, -1));
  }

  @Test
  void copiesAreIndependentOnSaveAndRead() {
    UUID uuid = UUID.randomUUID();
    FavoriteManager manager = manager();
    NameStyle original = style(), expected = original.copy();
    assertTrue(manager.setFavorite(uuid, 0, original));
    original.getGradientColors().clear();
    original.setBold(false);
    manager.getFavorite(uuid, 0).getGradientColors().clear();
    manager.getAll(uuid)[0].setItalic(false);
    assertEquals(expected, manager.getFavorite(uuid, 0));
    assertEquals(expected, manager().getFavorite(uuid, 0));
  }

  @Test
  void failedWriteDoesNotChangeStoredFavorite() throws Exception {
    UUID uuid = UUID.randomUUID();
    FavoriteManager manager = manager();
    java.nio.file.Files.writeString(folder.resolve("favorites"), "block directory creation");
    assertFalse(manager.setFavorite(uuid, 0, style()));
    assertNull(manager.getFavorite(uuid, 0));
  }

  @Test
  void gradientEditingDoesNotMutateOriginalOrGrowOnInvalidIndex() {
    UUID uuid = UUID.randomUUID();
    GradientSession session = new GradientSession();
    List<String> original = new ArrayList<>(List.of("#FF0000", "#0000FF"));
    session.start(uuid, original);
    session.setColor(uuid, 1, "#00FF00");
    assertEquals("#0000FF", original.get(1));
    session.setColor(uuid, 14, "#FFFFFF");
    session.setColor(uuid, -1, "#FFFFFF");
    assertEquals(List.of("#FF0000", "#00FF00"), session.get(uuid));
    session.remove(uuid);
    assertNull(session.get(uuid));
  }

  @Test
  void unreadableFileIsPreservedInsteadOfOverwritten() throws Exception {
    UUID uuid = UUID.randomUUID();
    Path directory = java.nio.file.Files.createDirectories(folder.resolve("favorites"));
    Path file = directory.resolve(uuid + ".yml");
    String invalid = "slots: [broken";
    java.nio.file.Files.writeString(file, invalid);
    FavoriteManager manager = manager();
    assertNull(manager.getFavorite(uuid, 0));
    assertFalse(manager.setFavorite(uuid, 0, style()));
    assertEquals(invalid, java.nio.file.Files.readString(file));
  }
}
