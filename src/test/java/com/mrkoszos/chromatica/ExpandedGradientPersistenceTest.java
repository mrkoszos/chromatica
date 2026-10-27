package com.mrkoszos.chromatica;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.mrkoszos.chromatica.gui.GradientSession;
import com.mrkoszos.chromatica.manager.*;
import com.mrkoszos.chromatica.model.*;
import com.mrkoszos.chromatica.renderer.NameRenderer;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Logger;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ExpandedGradientPersistenceTest {
  @TempDir Path folder;
  final Logger logger = Logger.getAnonymousLogger();

  NameStyle gradient(int count) {
    NameStyle style = new NameStyle();
    style.setType(StyleType.GRADIENT);
    List<String> colors = new ArrayList<>();
    for (int i = 0; i < count; i++) colors.add(String.format("#%06X", 0x111100 + i));
    style.setGradientColors(colors);
    style.setBold(true);
    style.setItalic(true);
    return style;
  }

  @Test void playerFavoritesAndCommunityPreserve21StopsAcrossRestart() {
    UUID owner = UUID.randomUUID();
    NameStyle expected = gradient(21);
    var players = new NameStyleManager(folder.toFile(), logger);
    assertTrue(players.setStyle(owner, expected));
    assertEquals(expected, new NameStyleManager(folder.toFile(), logger).getStyle(owner));
    var favorites = new FavoriteManager(folder.toFile(), logger);
    assertTrue(favorites.setFavorite(owner, 4, expected));
    assertEquals(expected, new FavoriteManager(folder.toFile(), logger).getFavorite(owner, 4));
    var community = new CommunityPresetManager(folder.toFile(), logger);
    var published = community.publish(owner, "Tester", "Full Gradient", expected, 10, true);
    assertEquals(CommunityPresetManager.Status.SUCCESS, published.status());
    var reloaded = new CommunityPresetManager(folder.toFile(), logger).get(published.preset().id());
    assertEquals(expected, reloaded.style());
    assertEquals(21, reloaded.style().getGradientColors().size());
    assertEquals(21, expected.getGradientColors().size());
  }

  @Test void codecRetainsOlderFormatsAndRejects22WithoutTruncation() {
    for (int count : new int[] {7,8,14,15,20,21}) {
      var yaml = new YamlConfiguration();
      NameStyleCodec.write(yaml, gradient(count));
      assertEquals(gradient(count), NameStyleCodec.read(yaml));
      // Legacy PRESET styles and omitted optional flags remain supported.
      yaml.set("type", "PRESET"); yaml.set("scramble", null);
      assertEquals(gradient(count), NameStyleCodec.read(yaml));
    }
    var oversized = gradient(22);
    assertThrows(IllegalArgumentException.class, () -> NameStyleCodec.validated(oversized));
    assertEquals(22, oversized.getGradientColors().size());
  }

  @Test void serverPresetLoaderAccepts21AndKeepsPreviousCatalogueOn22() throws Exception {
    var plugin = mock(Chromatica.class);
    when(plugin.getDataFolder()).thenReturn(folder.toFile());
    when(plugin.getLogger()).thenReturn(logger);
    var yaml = new YamlConfiguration();
    yaml.set("presets.full.display", "Full");
    yaml.set("presets.full.colors", gradient(21).getGradientColors());
    yaml.save(folder.resolve("presets.yml").toFile());
    var presets = new PresetManager(plugin);
    assertEquals(gradient(21).getGradientColors(), presets.get("full").colors());
    yaml.set("presets.full.colors", gradient(22).getGradientColors());
    yaml.save(folder.resolve("presets.yml").toFile());
    assertFalse(presets.reload());
    assertEquals(21, presets.get("full").colors().size());
  }

  @Test void previewAndSessionUseAll21StopsIncludingFinalRow() {
    var style = gradient(21);
    var session = new GradientSession();
    UUID owner = UUID.randomUUID();
    session.start(owner, style.getGradientColors());
    session.setColor(owner, 20, "#FF5555");
    assertNotEquals(style.getGradientColors().getLast(), session.get(owner).getLast());
    var preview = new NameRenderer().renderGradientPreview("X".repeat(21),
        session.get(owner), style, true);
    assertEquals(21, preview.children().size());
    for (int i = 0; i < 21; i++)
      assertEquals(TextColor.fromHexString(session.get(owner).get(i)), preview.children().get(i).color());
    assertTrue(preview.hasDecoration(net.kyori.adventure.text.format.TextDecoration.BOLD));
  }
}
