package com.mrkoszos.chromatica.gui;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.mrkoszos.chromatica.Chromatica;
import com.mrkoszos.chromatica.model.*;
import java.util.*;
import java.util.logging.Logger;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class GuiPolishTest {
  @Test
  void formattingFlagsGlowIndependentlyAndRefreshRemovesOldGlintSilently() {
    Chromatica plugin = mock(Chromatica.class);
    Player player = mock(Player.class);
    when(player.getName()).thenReturn("Tester");
    when(player.getUniqueId()).thenReturn(UUID.randomUUID());
    var styles = mock(com.mrkoszos.chromatica.manager.NameStyleManager.class);
    when(plugin.getNameStyleManager()).thenReturn(styles);
    NameStyle style = new NameStyle();
    style.setBold(true);
    style.setUnderlined(true);
    when(styles.getStyle(player.getUniqueId())).thenReturn(style);
    when(plugin.getNameRenderer()).thenReturn(new com.mrkoszos.chromatica.renderer.NameRenderer());
    when(plugin.getBackgroundManager())
        .thenReturn(mock(com.mrkoszos.chromatica.manager.BackgroundManager.class));
    GuiSounds sounds = mock(GuiSounds.class);
    when(plugin.getGuiSounds()).thenReturn(sounds);
    var inventory = mock(org.bukkit.inventory.Inventory.class);
    Map<Integer, ItemStack> slots = new HashMap<>();
    doAnswer(
            call -> {
              slots.put(call.getArgument(0), call.getArgument(1));
              return null;
            })
        .when(inventory)
        .setItem(anyInt(), any(ItemStack.class));
    try (var tooltips = mockStatic(GuiTooltips.class);
        var items =
        mockConstruction(
            ItemStack.class,
            (item, context) -> {
              when(item.getItemMeta()).thenReturn(mock(SkullMeta.class));
            })) {
      tooltips.when(() -> GuiTooltips.clean(any())).thenAnswer(c -> c.getArgument(0));
      FormattingMenu menu = new FormattingMenu(plugin);
      menu.refresh(player, inventory);
      verify(slots.get(11).getItemMeta()).setEnchantmentGlintOverride(true);
      verify(slots.get(12).getItemMeta()).setEnchantmentGlintOverride(false);
      verify(slots.get(15).getItemMeta()).setEnchantmentGlintOverride(true);
      verify(slots.get(14).getItemMeta()).setEnchantmentGlintOverride(false);
      style.setBold(false);
      style.setItalic(true);
      menu.refresh(player, inventory);
      verify(slots.get(11).getItemMeta()).setEnchantmentGlintOverride(false);
      verify(slots.get(12).getItemMeta()).setEnchantmentGlintOverride(true);
      verify(slots.get(15).getItemMeta()).setEnchantmentGlintOverride(true);
    }
    verifyNoInteractions(sounds);
    verify(player, never()).openInventory(any(org.bukkit.inventory.Inventory.class));
  }

  @Test
  void glintChangesOnlyMetadataFlagAndCanBeRemovedFromHeads() {
    ItemStack head = mock(ItemStack.class);
    SkullMeta meta = mock(SkullMeta.class);
    when(head.getItemMeta()).thenReturn(meta);
    assertSame(head, GuiUtils.selected(head, true));
    assertSame(head, GuiUtils.selected(head, false));
    verify(meta).setEnchantmentGlintOverride(true);
    verify(meta).setEnchantmentGlintOverride(false);
    verify(meta, never()).setOwnerProfile(any());
    verify(meta, never()).displayName(any());
    verify(meta, never()).lore(any());
    verify(meta, never()).addEnchant(any(), anyInt(), anyBoolean());
  }

  @Test
  void duplicatePresetsAndFavoritesChooseOnlyTheClickedIdentity() {
    Chromatica plugin = mock(Chromatica.class);
    UUID player = UUID.randomUUID();
    NameStyle style = GuiSelection.colors(List.of("#aabbcc", "#112233"));
    Map<String, NameStyle> candidates = new LinkedHashMap<>();
    candidates.put("first", style.copy());
    candidates.put("second", style.copy());
    assertEquals(
        "first", GuiSelection.selected(plugin, player, "community", style, candidates, false));
    GuiSelection.remember(plugin, player, "community", "second", style);
    assertEquals(
        "second", GuiSelection.selected(plugin, player, "community", style, candidates, false));
    assertNull(GuiSelection.selected(plugin, player, "server", style, candidates, true));
    NameStyle changed = style.copy();
    changed.setBold(true);
    assertNull(GuiSelection.selected(plugin, player, "community", changed, candidates, false));
    GuiSelection.remember(plugin, player, "favorite", "second", style);
    assertEquals(
        "second", GuiSelection.selected(plugin, player, "favorite", style, candidates, false));
    assertEquals(
        "second", GuiSelection.selected(plugin, player, "community", style, candidates, false));
    candidates.remove("second");
    assertNull(GuiSelection.selected(plugin, player, "favorite", style, candidates, false));
    GuiSelection.clear(plugin, player);
  }

  @Test
  void comparisonsUseActiveColorsAndFormattingRatherThanStaleUnusedFields() {
    NameStyle a = new NameStyle();
    a.setPrimaryColor("#AABBCC");
    a.setPresetId("old");
    a.setGradientColors(List.of("#111111", "#222222"));
    NameStyle b = new NameStyle();
    b.setPrimaryColor("#aabbcc");
    assertTrue(GuiSelection.sameStyle(a, b));
    assertTrue(GuiSelection.solid(a, "#aabbcc"));
    b.setItalic(true);
    assertFalse(GuiSelection.sameStyle(a, b));
    assertTrue(GuiSelection.sameColors(a, b));
    b.setType(StyleType.GRADIENT);
    assertFalse(GuiSelection.sameColors(a, b));
    assertFalse(GuiSelection.solid(b, "#aabbcc"));
  }

  @Test
  void serverPresetIdentityWinsOverIdenticalEarlierPalette() {
    NameStyle active = GuiSelection.colors(List.of("#123456"));
    active.setPresetId("second");
    Map<String, NameStyle> candidates = new LinkedHashMap<>();
    candidates.put("first", active.copy());
    candidates.put("second", active.copy());
    assertEquals(
        "second",
        GuiSelection.selected(
            mock(Chromatica.class), UUID.randomUUID(), "server", active, candidates, true));
    active.setPrimaryColor("#FFFFFF");
    assertNull(
        GuiSelection.selected(
            mock(Chromatica.class), UUID.randomUUID(), "server", active, candidates, true));
  }

  @Test
  void soundReloadAppliesCustomValuesAndGlobalDisable() {
    GuiSounds sounds = new GuiSounds();
    YamlConfiguration config = new YamlConfiguration();
    config.set("gui.sounds.confirm.sound", "minecraft:ui.button.click");
    config.set("gui.sounds.confirm.volume", .2);
    config.set("gui.sounds.confirm.pitch", 1.8);
    sounds.reload(
        config, Logger.getAnonymousLogger(), ignored -> Key.key("minecraft:ui.button.click"));
    Player player = mock(Player.class);
    sounds.play(player, GuiSounds.Event.CONFIRM);
    ArgumentCaptor<Sound> played = ArgumentCaptor.forClass(Sound.class);
    verify(player).playSound(played.capture());
    assertEquals(.2f, played.getValue().volume());
    assertEquals(1.8f, played.getValue().pitch());
    config.set("gui.sounds.enabled", false);
    sounds.reload(
        config, Logger.getAnonymousLogger(), ignored -> Key.key("minecraft:ui.button.click"));
    clearInvocations(player);
    sounds.play(player, GuiSounds.Event.SELECT);
    sounds.play(player, GuiSounds.Event.ERROR);
    verifyNoInteractions(player);
  }

  @Test
  void invalidSoundAndNumericConfigWarnAndFallbackWithoutCrashing() {
    GuiSounds sounds = new GuiSounds();
    YamlConfiguration config = new YamlConfiguration();
    config.set("gui.sounds.select.sound", "NOT_A_SOUND");
    config.set("gui.sounds.select.volume", "loud");
    config.set("gui.sounds.select.pitch", Double.NaN);
    Logger logger = mock(Logger.class);
    sounds.reload(
        config,
        logger,
        name -> {
          if (name.equals("NOT_A_SOUND")) throw new IllegalArgumentException();
          return Key.key("minecraft:ui.button.click");
        });
    Player player = mock(Player.class);
    sounds.play(player, GuiSounds.Event.SELECT);
    ArgumentCaptor<Sound> played = ArgumentCaptor.forClass(Sound.class);
    verify(player).playSound(played.capture());
    assertEquals(.3f, played.getValue().volume());
    assertEquals(1.2f, played.getValue().pitch());
    verify(logger, times(3)).warning(anyString());
  }

  @Test
  void brandingIsLiteralHasDefaultTitleStylingAndReloadableCredit() {
    GuiBranding branding = new GuiBranding();
    YamlConfiguration config = new YamlConfiguration();
    config.set("branding.display-name", "Server <red>Styles");
    branding.reload(config);
    assertEquals(Component.text("Server <red>Styles"), branding.title());
    assertNull(branding.title().color());
    assertTrue(branding.showCredit());
    assertEquals(
        "[Server <red>Styles] Done",
        PlainTextComponentSerializer.plainText()
            .serialize(branding.message(Component.text("Done"))));
    config.set("branding.show-credit", false);
    config.set("branding.display-name", "   ");
    branding.reload(config);
    assertEquals(Component.text("Chromatica"), branding.title());
    assertFalse(branding.showCredit());
  }
}
