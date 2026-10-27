package com.mrkoszos.chromatica;

import static org.junit.jupiter.api.Assertions.*;

import com.mrkoszos.chromatica.manager.*;
import com.mrkoszos.chromatica.model.*;
import com.mrkoszos.chromatica.renderer.NameRenderer;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ScrambleTest {
  @TempDir Path folder;

  @Test
  void codecKeepsScrambleAndOldDataDefaultsToFalse() {
    var style = new NameStyle();
    style.setScramble(true);
    var yaml = new YamlConfiguration();
    NameStyleCodec.write(yaml, style);
    assertEquals(style, NameStyleCodec.read(yaml));
    assertTrue(style.copy().isScramble());
    var plain = style.copy();
    plain.setScramble(false);
    assertNotEquals(style, plain);
    yaml.set("scramble", null);
    assertFalse(NameStyleCodec.read(yaml).isScramble());
    yaml.set("scramble", "true");
    assertThrows(IllegalArgumentException.class, () -> NameStyleCodec.read(yaml));
  }

  @Test
  void playerFavoriteAndCommunityPersistenceKeepScramble() {
    UUID id = UUID.randomUUID();
    var style = new NameStyle();
    style.setScramble(true);
    style.setBold(true);
    style.setType(StyleType.GRADIENT);
    style.setGradientColors(List.of("#112233", "#AABBCC"));
    var styles = new NameStyleManager(folder.toFile(), Logger.getAnonymousLogger());
    assertTrue(styles.setStyle(id, style));
    styles.unload(id);
    assertEquals(style, styles.getStyle(id));
    var favorites = new FavoriteManager(folder.toFile(), Logger.getAnonymousLogger());
    assertTrue(favorites.setFavorite(id, 4, style));
    favorites.unload(id);
    assertEquals(style, favorites.getFavorite(id, 4));
    assertNull(favorites.getFavorite(id, 0));
    var presets = new CommunityPresetManager(folder.toFile(), Logger.getAnonymousLogger());
    var result = presets.publish(id, "Tester", "Scramble", style, 10, true);
    assertEquals(CommunityPresetManager.Status.SUCCESS, result.status());
    assertTrue(presets.reload());
    assertEquals(style, presets.get(result.preset().id()).style());
  }

  @Test
  void renderingAndPermissionFilteringNeverMutateStoredStyle() {
    var renderer = new NameRenderer();
    var style = new NameStyle();
    style.setScramble(true);
    style.setItalic(true);
    Component allowed = renderer.render("Tester", style, true);
    assertEquals(TextDecoration.State.TRUE, allowed.decoration(TextDecoration.OBFUSCATED));
    assertEquals(
        TextDecoration.State.FALSE,
        renderer.render("Tester", style, false).decoration(TextDecoration.OBFUSCATED));
    assertTrue(style.isScramble());
    assertTrue(LegacyComponentSerializer.legacySection().serialize(allowed).contains("§k"));
    assertEquals(
        TextDecoration.State.TRUE,
        renderer
            .renderGradientPreview("Tester", List.of("#112233", "#445566"), style, true)
            .decoration(TextDecoration.OBFUSCATED));
    assertEquals(
        TextDecoration.State.FALSE,
        renderer
            .renderGradientPreview("Tester", List.of("#112233", "#445566"), style, false)
            .decoration(TextDecoration.OBFUSCATED));
  }

  @Test
  void chatMessageAndSeparatorDoNotInheritNameDecorations() {
    var renderer = new NameRenderer();
    var style = new NameStyle();
    style.setScramble(true);
    Component chat =
        Component.empty()
            .append(renderer.render("Tester", style, true))
            .append(Component.text(": "))
            .append(Component.text("message"));
    assertEquals(TextDecoration.State.NOT_SET, chat.decoration(TextDecoration.OBFUSCATED));
    assertEquals(
        TextDecoration.State.NOT_SET,
        chat.children().getLast().decoration(TextDecoration.OBFUSCATED));
  }

  @Test
  void serverPresetScrambleIsOptionalAndDoesNotEraseLegacyFormatting() {
    var style = new NameStyle();
    style.setScramble(true);
    new PresetManager.PresetDefinition(
            "legacy", "Legacy", List.of("#FFFFFF"), org.bukkit.Material.PAPER)
        .applyFormatting(style);
    assertTrue(style.isScramble());
    new PresetManager.PresetDefinition(
            "plain", "Plain", List.of("#FFFFFF"), org.bukkit.Material.PAPER, false)
        .applyFormatting(style);
    assertFalse(style.isScramble());
    new PresetManager.PresetDefinition(
            "scramble", "Scramble", List.of("#FFFFFF"), org.bukkit.Material.PAPER, true)
        .applyFormatting(style);
    assertTrue(style.isScramble());
  }
}
