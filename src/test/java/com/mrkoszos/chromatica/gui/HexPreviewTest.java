package com.mrkoszos.chromatica.gui;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.mrkoszos.chromatica.Chromatica;
import com.mrkoszos.chromatica.manager.NameStyleManager;
import com.mrkoszos.chromatica.model.*;
import com.mrkoszos.chromatica.renderer.NameRenderer;
import java.util.*;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

class HexPreviewTest {
  @Test void solidAndGradientPreviewUseDraftWithoutMutatingSavedStyle() {
    Chromatica plugin = mock(Chromatica.class);
    Player player = mock(Player.class);
    UUID uuid = UUID.randomUUID();
    when(player.getUniqueId()).thenReturn(uuid);
    when(player.getName()).thenReturn("mrkoszos");
    NameStyleManager manager = mock(NameStyleManager.class);
    NameStyle saved = new NameStyle();
    saved.setItalic(true);
    saved.setType(StyleType.GRADIENT);
    saved.setGradientColors(List.of("#000000", "#FFFFFF"));
    NameStyle before = saved.copy();
    when(plugin.getNameStyleManager()).thenReturn(manager);
    when(manager.getStyle(uuid)).thenReturn(saved);
    NameRenderer renderer = new NameRenderer();
    when(plugin.getNameRenderer()).thenReturn(renderer);
    ColorSelectorSession context = new ColorSelectorSession();
    when(plugin.getColorSelectorSession()).thenReturn(context);
    HexColorMenu menu = new HexColorMenu(plugin);
    NameStyle expected = saved.copy();
    expected.setType(StyleType.SOLID);
    expected.setPrimaryColor("#123456");
    assertEquals(renderer.render("mrkoszos", expected), menu.preview(player, " #123456 "));
    GradientSession draft = new GradientSession();
    draft.start(uuid, List.of("#AA0000", "#00AA00", "#0000AA"));
    when(plugin.getGradientSession()).thenReturn(draft);
    when(plugin.getGradientColorSelection()).thenReturn(Map.of(uuid, 1));
    context.set(uuid, ColorSelectionContext.GRADIENT);
    expected.setType(StyleType.GRADIENT);
    expected.setGradientColors(List.of("#AA0000", "#123456", "#0000AA"));
    assertEquals(renderer.render("mrkoszos", expected), menu.preview(player, "#123456"));
    assertEquals(List.of("#AA0000", "#00AA00", "#0000AA"), draft.get(uuid));
    assertEquals(before, saved);
  }

  @Test void invalidInputNeverRendersFakePlayerPreview() {
    Chromatica plugin = mock(Chromatica.class);
    HexColorMenu menu = new HexColorMenu(plugin);
    Player player = mock(Player.class);
    for (String input : Arrays.asList(null, "", "#", "#12345", "#GGGGGG", "red")) {
      Component result = menu.preview(player, input);
      assertEquals(GuiUtils.text("Invalid color", net.kyori.adventure.text.format.NamedTextColor.RED), result);
    }
    verifyNoInteractions(plugin);
  }
}
