package com.mrkoszos.chromatica.gui;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.mrkoszos.chromatica.Chromatica;
import com.mrkoszos.chromatica.manager.*;
import com.mrkoszos.chromatica.model.*;
import com.mrkoszos.chromatica.renderer.NameRenderer;
import java.util.*;
import java.util.logging.Logger;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.SkullMeta;
import org.junit.jupiter.api.*;
import org.mockito.MockedConstruction;

class RedesignTest {
  Chromatica plugin;
  Player player;
  Inventory inventory;
  InventoryView view;
  NameStyleManager styles;
  NameStyle style;
  Map<Integer, ItemStack> slots;
  MockedConstruction<ItemStack> items;
  org.mockito.MockedStatic<GuiTooltips> tooltips;

  @BeforeEach
  void setup() {
    tooltips = mockStatic(GuiTooltips.class);
    tooltips.when(() -> GuiTooltips.clean(any())).thenAnswer(c -> c.getArgument(0));
    items =
        mockConstruction(
            ItemStack.class,
            (item, context) -> {
              when(item.getType()).thenReturn((Material) context.arguments().getFirst());
              when(item.getItemMeta()).thenReturn(mock(SkullMeta.class));
              when(item.clone()).thenReturn(item);
            });
    plugin = mock(Chromatica.class);
    player = mock(Player.class);
    when(player.getUniqueId()).thenReturn(UUID.randomUUID());
    when(player.getName()).thenReturn("Tester");
    when(player.isOnline()).thenReturn(true);
    when(player.hasPermission("chromatica.use")).thenReturn(true);
    styles = mock(NameStyleManager.class);
    style = new NameStyle();
    when(plugin.getNameStyleManager()).thenReturn(styles);
    when(styles.getStyle(player.getUniqueId())).thenAnswer(c -> style.copy());
    when(styles.setStyle(any(), any())).thenReturn(true);
    when(plugin.getNameRenderer()).thenReturn(new NameRenderer());
    when(plugin.getGradientSession()).thenReturn(new GradientSession());
    inventory = mock(Inventory.class);
    when(inventory.getSize()).thenReturn(27);
    slots = new HashMap<>();
    when(inventory.getItem(anyInt())).thenAnswer(c -> slots.get(c.getArgument(0)));
    doAnswer(
            c -> {
              slots.put(c.getArgument(0), c.getArgument(1));
              return null;
            })
        .when(inventory)
        .setItem(anyInt(), nullable(ItemStack.class));
    doAnswer(
            c -> {
              slots.clear();
              return null;
            })
        .when(inventory)
        .clear();
    Server server = mock(Server.class);
    when(plugin.getServer()).thenReturn(server);
    when(server.createInventory(any(InventoryHolder.class), anyInt(), any(Component.class)))
        .thenAnswer(
            c -> {
              when(inventory.getHolder()).thenReturn(c.getArgument(0));
              when(inventory.getSize()).thenReturn(c.getArgument(1));
              return inventory;
            });
    view = mock(InventoryView.class);
    when(view.getTopInventory()).thenReturn(inventory);
    when(player.getOpenInventory()).thenReturn(view);
    YamlConfiguration config = new YamlConfiguration();
    when(plugin.getConfig()).thenReturn(config);
    when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());
    var background = new BackgroundManager(plugin);
    when(plugin.getBackgroundManager()).thenReturn(background);
  }

  @AfterEach
  void cleanup() {
    items.close();
    tooltips.close();
    InlineConfirmation.cancel(player);
  }

  void holder(String name) {
    GuiUtils.inventory(plugin, inventory.getSize(), Component.text(name), name);
  }

  InventoryClickEvent click(int slot, ClickType type) {
    var e = mock(InventoryClickEvent.class);
    when(e.getView()).thenReturn(view);
    when(e.getWhoClicked()).thenReturn(player);
    when(e.getRawSlot()).thenReturn(slot);
    when(e.getClick()).thenReturn(type);
    when(e.isLeftClick()).thenReturn(type == ClickType.LEFT);
    when(e.isRightClick()).thenReturn(type == ClickType.RIGHT);
    when(e.getCurrentItem()).thenReturn(slots.get(slot));
    return e;
  }

  void pane(int slot, Material material) {
    assertNotNull(slots.get(slot), "slot " + slot);
    assertEquals(material, slots.get(slot).getType(), "slot " + slot);
  }

  @Test
  void colorLayoutAndPaletteOrderAndHexAction() {
    new ColorMenu(plugin).open(player);
    pane(4, Material.PLAYER_HEAD);
    pane(17, Material.LIME_STAINED_GLASS_PANE);
    pane(22, Material.BARRIER);
    Material[] palette = {
      Material.RED_DYE,
      Material.ORANGE_DYE,
      Material.YELLOW_DYE,
      Material.LIME_DYE,
      Material.CYAN_DYE,
      Material.BLUE_DYE,
      Material.PURPLE_DYE
    };
    for (int i = 0; i < 7; i++) pane(10 + i, palette[i]);
    assertEquals(27, slots.size());
    verify(slots.get(17).getItemMeta(), never()).setEnchantmentGlintOverride(anyBoolean());
    pane(0, Material.GRAY_STAINED_GLASS_PANE);
    pane(1, Material.LIGHT_GRAY_STAINED_GLASS_PANE);
    pane(18, Material.GRAY_STAINED_GLASS_PANE);
    pane(19, Material.LIGHT_GRAY_STAINED_GLASS_PANE);
  }

  @Test
  void compactGradientKeepsUnusedSlotsTrulyEmpty() {
    plugin
        .getGradientSession()
        .start(player.getUniqueId(), List.of("#FF0000", "#00FF00", "#0000FF", "#FFFFFF"));
    new GradientMenu(plugin).open(player);
    assertEquals(27, inventory.getSize());
    pane(4, Material.PLAYER_HEAD);
    pane(21, Material.KNOWLEDGE_BOOK);
    pane(23, Material.BARRIER);
    for (int i = 10; i <= 13; i++) assertNotNull(slots.get(i));
    for (int i = 14; i <= 16; i++) assertNull(slots.get(i));
    pane(19, Material.LIGHT_GRAY_STAINED_GLASS_PANE);
    pane(22, Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
    plugin.getBackgroundManager().applyBackground(inventory);
    for (int i = 14; i <= 16; i++) assertNull(slots.get(i));
  }

  @Test
  void compactGradientHasSevenVisibleStops() {
    plugin.getGradientSession().start(player.getUniqueId(), Collections.nCopies(7, "#123456"));
    new GradientMenu(plugin).open(player);
    assertEquals(7, GradientMenu.capacity(inventory));
    for (int i = 10; i <= 16; i++) assertNotNull(slots.get(i));
  }

  @Test
  void expandedGradientHasTwentyOneEditableStopsWithoutOverlap() {
    when(plugin.isExpandedGradient()).thenReturn(true);
    plugin.getGradientSession().start(player.getUniqueId(), Collections.nCopies(21, "#123456"));
    new GradientMenu(plugin).open(player);
    assertEquals(45, inventory.getSize());
    assertEquals(21, GradientMenu.capacity(inventory));
    for (int slot : GradientMenu.GRADIENT_SLOTS) assertNotNull(slots.get(slot));
    pane(39, Material.KNOWLEDGE_BOOK);
    pane(41, Material.BARRIER);
  }

  @Test
  void expandedUnusedStopsAreEmpty() {
    when(plugin.isExpandedGradient()).thenReturn(true);
    new GradientMenu(plugin).open(player);
    for (int i = 2; i < 21; i++) assertNull(slots.get(GradientMenu.GRADIENT_SLOTS[i]));
    plugin.getBackgroundManager().applyBackground(inventory);
    for (int i = 2; i < 21; i++) assertNull(slots.get(GradientMenu.GRADIENT_SLOTS[i]));
  }

  @Test
  void expandedStopCountsUseExactSequentialRowsAndControls() {
    when(plugin.isExpandedGradient()).thenReturn(true);
    int[] expected = {10,11,12,13,14,15,16,19,20,21,22,23,24,25,28,29,30,31,32,33,34};
    assertArrayEquals(expected, GradientMenu.GRADIENT_SLOTS);
    assertEquals(NameStyleCodec.MAX_STOPS, expected.length);
    for (int count : new int[] {7, 8, 14, 15, 20, 21}) {
      List<String> colors = new ArrayList<>();
      for (int i = 0; i < count; i++) colors.add(String.format("#%06X", 0x123400 + i));
      plugin.getGradientSession().start(player.getUniqueId(), colors);
      new GradientMenu(plugin).open(player);
      assertEquals(45, inventory.getSize());
      pane(4, Material.PLAYER_HEAD);
      pane(39, Material.KNOWLEDGE_BOOK);
      pane(40, Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
      pane(41, Material.BARRIER);
      for (int i = 0; i < count; i++) {
        pane(expected[i], DyeColors.nearest(colors.get(i)));
        String hex = colors.get(i);
        verify(slots.get(expected[i]).getItemMeta()).lore(argThat(lore ->
            lore.contains(GuiUtils.text(hex,
                net.kyori.adventure.text.format.NamedTextColor.GRAY))));
      }
      plugin.getBackgroundManager().applyBackground(inventory);
      for (int i = count; i < 21; i++) assertNull(slots.get(expected[i]));
      pane(35, Material.LIGHT_GRAY_STAINED_GLASS_PANE);
      assertEquals(colors, plugin.getGradientSession().get(player.getUniqueId()));
    }
  }

  @Test
  void savedGradientAboveSevenAutoExpandsWithoutTruncating() {
    style.setType(StyleType.GRADIENT);
    style.setGradientColors(Collections.nCopies(11, "#123456"));
    new GradientMenu(plugin).open(player);
    assertEquals(45, inventory.getSize());
    assertEquals(11, plugin.getGradientSession().get(player.getUniqueId()).size());
    assertEquals(11, style.getGradientColors().size());
  }

  @Test
  void allSavedExpandedSizesAutoExpandWithoutChangingData() {
    for (int count = 8; count <= 21; count++) {
      plugin.getGradientSession().remove(player.getUniqueId());
      style.setType(StyleType.GRADIENT);
      style.setGradientColors(Collections.nCopies(count, "#123456"));
      new GradientMenu(plugin).open(player);
      assertEquals(45, inventory.getSize());
      assertEquals(style.getGradientColors(), plugin.getGradientSession().get(player.getUniqueId()));
      assertEquals(count, style.getGradientColors().size());
    }
  }

  @Test
  void expandedAdd21Reject22AndThirdRowEditRemoveApplyBackUseCorrectSlots() {
    when(plugin.isExpandedGradient()).thenReturn(true);
    var selected = new HashMap<UUID, Integer>();
    when(plugin.getGradientColorSelection()).thenReturn(selected);
    plugin.getGradientSession().start(player.getUniqueId(), Collections.nCopies(20, "#123456"));
    new GradientMenu(plugin).open(player);
    immediateScheduler();
    var listener = new GradientMenuListener(plugin);
    listener.onInventoryClick(click(40, ClickType.LEFT));
    var colors = plugin.getGradientSession().get(player.getUniqueId());
    assertEquals(21, colors.size());
    assertEquals("#FFFFFF", colors.getLast());
    listener.onInventoryClick(click(40, ClickType.LEFT));
    assertEquals(21, colors.size());
    pane(40, Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
    try (var selectors = mockConstruction(ColorSelectorMenu.class)) {
      for (int i = 14; i < 21; i++) {
        listener.onInventoryClick(click(GradientMenu.GRADIENT_SLOTS[i], ClickType.LEFT));
        assertEquals(i, selected.get(player.getUniqueId()));
        verify(selectors.constructed().get(i - 14)).open(player, ColorSelectionContext.GRADIENT, i);
        plugin.getGradientSession().setColor(player.getUniqueId(), i, "#FF5555");
      }
    }
    new GradientMenu(plugin).refresh(player, inventory);
    for (int slot = 28; slot <= 34; slot++) pane(slot, Material.RED_DYE);
    verify(slots.get(4).getItemMeta()).displayName(any(Component.class));
    listener.onInventoryClick(click(39, ClickType.LEFT));
    var applied = org.mockito.ArgumentCaptor.forClass(NameStyle.class);
    verify(styles).setStyle(eq(player.getUniqueId()), applied.capture());
    assertEquals(21, applied.getValue().getGradientColors().size());
    assertEquals(colors, applied.getValue().getGradientColors());
    for (int slot = 34; slot >= 28; slot--)
      listener.onInventoryClick(click(slot, ClickType.RIGHT));
    assertEquals(14, colors.size());
    plugin.getBackgroundManager().applyBackground(inventory);
    for (int slot = 28; slot <= 34; slot++) assertNull(slots.get(slot));
    try (var parent = mockConstruction(ChromaticaMenu.class)) {
      listener.onInventoryClick(click(41, ClickType.LEFT));
      verify(parent.constructed().getFirst()).open(player);
      assertNull(plugin.getGradientSession().get(player.getUniqueId()));
    }
    verify(player, never()).setItemOnCursor(any());
  }

  @Test
  void autoExpandedSavedGradientCanGrowTo21WhileCompactInventoryStillCapsAtSeven() {
    style.setType(StyleType.GRADIENT);
    style.setGradientColors(Collections.nCopies(8, "#123456"));
    new GradientMenu(plugin).open(player);
    assertEquals(45, inventory.getSize());
    var listener = new GradientMenuListener(plugin);
    for (int i = 0; i < 20; i++) listener.onInventoryClick(click(40, ClickType.LEFT));
    assertEquals(21, plugin.getGradientSession().get(player.getUniqueId()).size());
    assertEquals(8, style.getGradientColors().size());
    pane(40, Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
  }

  @Test
  void expandedOldCompactActionSlotsAreStopsAndUnsafeClicksCannotAct() {
    when(plugin.isExpandedGradient()).thenReturn(true);
    plugin.getGradientSession().start(player.getUniqueId(), Collections.nCopies(21, "#123456"));
    new GradientMenu(plugin).open(player);
    var listener = new GradientMenuListener(plugin);
    // Slot 23 is stop 12 in expanded mode, not Back.
    listener.onInventoryClick(click(23, ClickType.RIGHT));
    assertEquals(20, plugin.getGradientSession().get(player.getUniqueId()).size());
    for (int slot : new int[] {28,34,39,40,41}) {
      for (ClickType type : ClickType.values()) {
        if (type == ClickType.LEFT || type == ClickType.RIGHT) continue;
        var event = click(slot, type);
        listener.onInventoryClick(event);
        verify(event).setCancelled(true);
        verify(event, never()).setCursor(any());
      }
    }
    verify(styles, never()).setStyle(any(), any());
    assertEquals(20, plugin.getGradientSession().get(player.getUniqueId()).size());
  }

  @Test
  void addButtonAppendsInPlaceAndEmptyStopsDoNothing() {
    new GradientMenu(plugin).open(player);
    var scheduler = mock(org.bukkit.scheduler.BukkitScheduler.class);
    when(plugin.getServer().getScheduler()).thenReturn(scheduler);
    new GradientMenuListener(plugin).onInventoryClick(click(15, ClickType.LEFT));
    assertEquals(2, plugin.getGradientSession().get(player.getUniqueId()).size());
    var event = click(22, ClickType.LEFT);
    new GradientMenuListener(plugin).onInventoryClick(event);
    assertEquals(3, plugin.getGradientSession().get(player.getUniqueId()).size());
    verifyNoInteractions(scheduler);
    verify(event).setCancelled(true);
  }

  @Test
  void gradientCannotApplyInvalidDataOrRemoveBelowTwo() {
    new GradientMenu(plugin).open(player);
    new GradientMenuListener(plugin).onInventoryClick(click(10, ClickType.RIGHT));
    assertEquals(2, plugin.getGradientSession().get(player.getUniqueId()).size());
    plugin.getGradientSession().get(player.getUniqueId()).set(0, "invalid");
    new GradientMenuListener(plugin).onInventoryClick(click(21, ClickType.LEFT));
    verify(styles, never()).setStyle(any(), any());
  }

  @Test
  void favoritesHaveExactlyFiveFixedSlots() {
    var favorites = mock(FavoriteManager.class);
    when(plugin.getFavoriteManager()).thenReturn(favorites);
    when(favorites.getAll(player.getUniqueId()))
        .thenReturn(new NameStyle[] {null, new NameStyle(), null, null, null});
    new FavoritesMenu(plugin).open(player);
    assertArrayEquals(new int[] {11, 12, 13, 14, 15}, FavoritesMenu.FAVORITE_SLOTS);
    pane(4, Material.PLAYER_HEAD);
    pane(11, Material.ITEM_FRAME);
    pane(12, Material.GLOW_ITEM_FRAME);
    pane(22, Material.BARRIER);
    pane(21, Material.GRAY_STAINED_GLASS_PANE);
  }

  @Test
  void formattingIconsAndItalicDynamicState() {
    holder("FormattingMenu");
    new FormattingMenu(plugin).refresh(player, inventory);
    pane(11, Material.MACE);
    pane(12, Material.WRITABLE_BOOK);
    pane(13, Material.BRUSH);
    pane(14, Material.SHEARS);
    pane(15, Material.STICK);
    pane(22, Material.BARRIER);
    style.setItalic(true);
    new FormattingMenu(plugin).refresh(player, inventory);
    pane(12, Material.WRITTEN_BOOK);
    verify(slots.get(12).getItemMeta()).setEnchantmentGlintOverride(true);
  }

  @Test
  void formattingTogglesIndependentlyPreserveGradientAndPlayOneSpecificSound() {
    style.setType(StyleType.GRADIENT);
    style.setGradientColors(List.of("#112233", "#445566"));
    style.setItalic(true);
    holder("FormattingMenu");
    new FormattingMenu(plugin).refresh(player, inventory);
    var sounds = mock(GuiSounds.class);
    when(plugin.getGuiSounds()).thenReturn(sounds);
    new FormattingMenuListener(plugin).onInventoryClick(click(11, ClickType.LEFT));
    var captured = org.mockito.ArgumentCaptor.forClass(NameStyle.class);
    verify(styles).setStyle(eq(player.getUniqueId()), captured.capture());
    assertTrue(captured.getValue().isBold());
    assertTrue(captured.getValue().isItalic());
    assertEquals(style.getGradientColors(), captured.getValue().getGradientColors());
    verify(sounds).play(player, GuiSounds.Event.BOLD_ON);
    verifyNoMoreInteractions(sounds);
    verify(player, never()).openInventory(any(Inventory.class));
  }

  @Test
  void allFormattingOnOffSoundsAreSpecific() {
    when(player.hasPermission("chromatica.format.scramble")).thenReturn(true);
    holder("FormattingMenu");
    var sounds = mock(GuiSounds.class);
    when(plugin.getGuiSounds()).thenReturn(sounds);
    String[] names = {"BOLD", "ITALIC", "SCRAMBLE", "STRIKETHROUGH", "UNDERLINE"};
    for (int i = 0; i < 5; i++) {
      style = new NameStyle();
      new FormattingMenu(plugin).refresh(player, inventory);
      new FormattingMenuListener(plugin).onInventoryClick(click(11 + i, ClickType.LEFT));
      verify(sounds).play(player, GuiSounds.Event.valueOf(names[i] + "_ON"));
      style.setBold(true);
      style.setItalic(true);
      style.setScramble(true);
      style.setStrikethrough(true);
      style.setUnderlined(true);
      new FormattingMenuListener(plugin).onInventoryClick(click(11 + i, ClickType.LEFT));
      verify(sounds).play(player, GuiSounds.Event.valueOf(names[i] + "_OFF"));
    }
    verifyNoMoreInteractions(sounds);
  }

  @Test
  void unauthorizedScrambleToggleAndPresetApplicationAreRejected() {
    holder("FormattingMenu");
    new FormattingMenu(plugin).refresh(player, inventory);
    new FormattingMenuListener(plugin).onInventoryClick(click(13, ClickType.LEFT));
    var saved = new NameStyle();
    saved.setScramble(true);
    assertFalse(GuiUtils.persist(plugin, player, saved));
    verify(styles, never()).setStyle(any(), any());
    assertTrue(saved.isScramble());
  }

  @Test
  void storedScrambleCanBeDisabledAfterPermissionRevocation() {
    style.setScramble(true);
    holder("FormattingMenu");
    new FormattingMenu(plugin).refresh(player, inventory);
    new FormattingMenuListener(plugin).onInventoryClick(click(13, ClickType.LEFT));
    var capture = org.mockito.ArgumentCaptor.forClass(NameStyle.class);
    verify(styles).setStyle(any(), capture.capture());
    assertFalse(capture.getValue().isScramble());
  }

  @Test
  void unsafeClicksCannotToggleOrExtract() {
    holder("FormattingMenu");
    for (ClickType type : ClickType.values()) {
      if (type == ClickType.LEFT || type == ClickType.RIGHT) continue;
      var event = click(13, type);
      new FormattingMenuListener(plugin).onInventoryClick(event);
      verify(event).setCancelled(true);
      verify(event, never()).setCursor(any());
    }
    verify(styles, never()).setStyle(any(), any());
  }

  @Test
  void soundDefaultsExistInPaperApiAndReloadSupportsEveryToggle() {
    for (GuiSounds.Event event : GuiSounds.Event.values())
      assertDoesNotThrow(() -> org.bukkit.Sound.class.getField(event.sound));
    var sounds = new GuiSounds();
    var config = new YamlConfiguration();
    config.set("gui.sounds.scramble-off.volume", .12);
    config.set("gui.sounds.scramble-off.pitch", 1.9);
    sounds.reload(config, Logger.getAnonymousLogger(), n -> Key.key("minecraft:ui.button.click"));
    sounds.play(player, GuiSounds.Event.SCRAMBLE_OFF);
    var capture = org.mockito.ArgumentCaptor.forClass(net.kyori.adventure.sound.Sound.class);
    verify(player).playSound(capture.capture());
    assertEquals(.12f, capture.getValue().volume());
    assertEquals(1.9f, capture.getValue().pitch());
  }

  @Test
  void gradientLayoutConfigValidatesAndFallsBack() {
    var logger = mock(Logger.class);
    assertFalse(GradientMenu.expandedLayout(null, logger));
    assertFalse(GradientMenu.expandedLayout("compact", logger));
    assertTrue(GradientMenu.expandedLayout("EXPANDED", logger));
    assertFalse(GradientMenu.expandedLayout("invalid", logger));
    verify(logger).warning(anyString());
  }

  @Test
  void customHexButtonUsesExistingAnvilFlow() {
    new ColorMenu(plugin).open(player);
    var scheduler = mock(org.bukkit.scheduler.BukkitScheduler.class);
    when(plugin.getServer().getScheduler()).thenReturn(scheduler);
    when(scheduler.runTask(eq(plugin), any(Runnable.class)))
        .thenAnswer(
            c -> {
              ((Runnable) c.getArgument(1)).run();
              return null;
            });
    try (var anvils = mockConstruction(HexColorMenu.class)) {
      new ColorMenuListener(plugin).onInventoryClick(click(17, ClickType.LEFT));
      verify(anvils.constructed().getFirst())
          .open(eq(player), any(java.util.function.Consumer.class));
    }
    verify(player, never()).setItemOnCursor(any());
  }
  @Test
  void configuredLimitsAndOrderedEditingPreserveSavedStops() {
    new GradientMenu(plugin).open(player);
    var listener = new GradientMenuListener(plugin);
    for (int i = 0; i < 10; i++) listener.onInventoryClick(click(22, ClickType.LEFT));
    var colors = plugin.getGradientSession().get(player.getUniqueId());
    assertEquals(7, colors.size());
    pane(22, Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
    plugin.getGradientSession().setColor(player.getUniqueId(), 1, "#123456");
    listener.onInventoryClick(click(10, ClickType.RIGHT));
    assertEquals("#123456", colors.getFirst());
    when(plugin.isExpandedGradient()).thenReturn(true);
    new GradientMenu(plugin).open(player);
    for (int i = 0; i < 20; i++) listener.onInventoryClick(click(40, ClickType.LEFT));
    assertEquals(21, colors.size());
    when(plugin.isExpandedGradient()).thenReturn(false);
    new GradientMenu(plugin).open(player);
    listener.onInventoryClick(click(40, ClickType.LEFT));
    assertEquals(21, colors.size());
    assertEquals("#123456", colors.getFirst());
    pane(40, Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
  }

  @Test
  void standardStopIconsUseActualOrderedColorsAfterRefresh() {
    var colors = List.of("#FF5555", "#FFAA00", "#FFFF55");
    plugin.getGradientSession().start(player.getUniqueId(), colors);
    var menu = new GradientMenu(plugin);
    menu.open(player);
    for (int pass = 0; pass < 2; pass++) {
      pane(10, Material.RED_DYE);
      pane(11, Material.ORANGE_DYE);
      pane(12, Material.YELLOW_DYE);
      assertEquals(colors, plugin.getGradientSession().get(player.getUniqueId()));
      menu.refresh(player, inventory);
    }
  }

  @Test
  void addingRefreshesPreviewInPlaceAndReloadCannotHideNewStops() {
    new GradientMenu(plugin).open(player);
    clearInvocations(player);
    var before = slots.get(4);
    var listener = new GradientMenuListener(plugin);
    listener.onInventoryClick(click(22, ClickType.LEFT));
    assertNotSame(before, slots.get(4));
    verify(slots.get(4).getItemMeta()).displayName(any(Component.class));
    verify(player, never()).openInventory(any(Inventory.class));
    verify(player, never()).setItemOnCursor(any());
    for (int i = 0; i < 4; i++) listener.onInventoryClick(click(22, ClickType.LEFT));
    when(plugin.isExpandedGradient()).thenReturn(true);
    listener.onInventoryClick(click(22, ClickType.LEFT));
    assertEquals(7, plugin.getGradientSession().get(player.getUniqueId()).size());
  }

  @Test
  void presetsHaveExactCategoriesPreviewAndStaticGlobeTexture() {
    var profile = mock(com.destroystokyo.paper.profile.PlayerProfile.class);
    when(plugin.getServer().createProfile(any(UUID.class))).thenReturn(profile);
    new PresetMenu(plugin).open(player);
    assertEquals(27, inventory.getSize());
    pane(4, Material.PLAYER_HEAD);
    pane(10, Material.STRUCTURE_BLOCK);
    pane(11, Material.PRIZE_POTTERY_SHERD);
    pane(15, Material.CLOCK);
    pane(16, Material.PLAYER_HEAD);
    pane(22, Material.BARRIER);
    var property = org.mockito.ArgumentCaptor.forClass(
        com.destroystokyo.paper.profile.ProfileProperty.class);
    verify(profile).setProperty(property.capture());
    assertEquals("textures", property.getValue().getName());
    assertEquals("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDM0NjdhNTMxOTc4ZDBiOGZkMjRmNTYyODVjNzI3MzRkODRmNWVjODhlMGI0N2M0OTMyMzM2Mjk3OWIzMjNhZiJ9fX0=",
        property.getValue().getValue());
    verify((SkullMeta) slots.get(16).getItemMeta()).setPlayerProfile(profile);
    verify(slots.get(16).getItemMeta()).displayName(
        GuiUtils.text("Community Presets", net.kyori.adventure.text.format.NamedTextColor.YELLOW));
    verify(slots.get(16).getItemMeta(), never()).lore(anyList());
    verify(profile, never()).update();
    pane(9, Material.LIGHT_GRAY_STAINED_GLASS_PANE);
  }

  @Test
  void unavailableCategoriesStayInCurrentInventoryAndRejectUnsafeClicks() {
    holder("PresetMenu");
    var listener = new PresetMenuListener(plugin);
    for (int slot : new int[] {11, 15, 16}) {
      listener.onInventoryClick(click(slot, ClickType.LEFT));
    }
    verify(player, times(3)).sendActionBar(any(Component.class));
    for (ClickType type : ClickType.values()) {
      if (type == ClickType.LEFT || type == ClickType.RIGHT) continue;
      var event = click(11, type);
      listener.onInventoryClick(event);
      verify(event).setCancelled(true);
    }
    verify(player, times(3)).sendActionBar(any(Component.class));
    verify(player, never()).openInventory(any(Inventory.class));
    verify(plugin.getServer(), never()).getScheduler();
    when(player.hasPermission("chromatica.use")).thenReturn(false);
    listener.onInventoryClick(click(11, ClickType.LEFT));
    verify(player, times(3)).sendActionBar(any(Component.class));
  }

  void immediateScheduler() {
    var scheduler = mock(org.bukkit.scheduler.BukkitScheduler.class);
    when(plugin.getServer().getScheduler()).thenReturn(scheduler);
    when(scheduler.runTask(eq(plugin), any(Runnable.class))).thenAnswer(c -> {
      ((Runnable) c.getArgument(1)).run();
      return null;
    });
  }

  @Test
  void relocatedBackButtonsNavigateToParentAndGradientEndsOnlyDraft() {
    immediateScheduler();
    try (var parent = mockConstruction(ChromaticaMenu.class)) {
      holder("ColorMenu");
      new ColorMenuListener(plugin).onInventoryClick(click(22, ClickType.LEFT));
      holder("FavoritesMenu");
      inventory.setItem(22, GuiUtils.item(Material.BARRIER, Component.text("Back")));
      new FavoritesMenuListener(plugin).onInventoryClick(click(22, ClickType.LEFT));
      holder("FormattingMenu");
      new FormattingMenuListener(plugin).onInventoryClick(click(22, ClickType.LEFT));
      holder("PresetMenu");
      new PresetMenuListener(plugin).onInventoryClick(click(22, ClickType.LEFT));
      new GradientMenu(plugin).open(player);
      new GradientMenuListener(plugin).onInventoryClick(click(23, ClickType.LEFT));
      assertNull(plugin.getGradientSession().get(player.getUniqueId()));
      assertEquals(5, parent.constructed().size());
      for (var menu : parent.constructed()) verify(menu).open(player);
      verify(styles, never()).setStyle(any(), any());
    }
  }

  @Test
  void serverAndCommunityCategoriesOpenExistingBrowsers() {
    immediateScheduler();
    holder("PresetMenu");
    when(plugin.isCommunityPresetsEnabled()).thenReturn(true);
    try (var browsers = mockConstruction(PresetBrowserMenu.class, (menu, context) -> {
      assertEquals(context.getCount() == 2, context.arguments().get(1));
    })) {
      var listener = new PresetMenuListener(plugin);
      listener.onInventoryClick(click(10, ClickType.LEFT));
      listener.onInventoryClick(click(16, ClickType.LEFT));
      assertEquals(2, browsers.constructed().size());
      for (var menu : browsers.constructed()) verify(menu).open(player, 0);
    }
  }

  @Test
  void gradientApplyPersistsOnlyValidDraftAndRetainsSessionOnFailure() {
    new GradientMenu(plugin).open(player);
    var colors = new ArrayList<>(plugin.getGradientSession().get(player.getUniqueId()));
    when(styles.setStyle(any(), any())).thenReturn(false);
    var sounds = mock(GuiSounds.class);
    when(plugin.getGuiSounds()).thenReturn(sounds);
    var listener = new GradientMenuListener(plugin);
    listener.onInventoryClick(click(21, ClickType.LEFT));
    assertEquals(colors, plugin.getGradientSession().get(player.getUniqueId()));
    assertEquals(StyleType.SOLID, style.getType());
    verify(sounds).play(player, GuiSounds.Event.ERROR);
    verify(sounds, never()).play(player, GuiSounds.Event.CONFIRM);
    when(styles.setStyle(any(), any())).thenReturn(true);
    listener.onInventoryClick(click(21, ClickType.LEFT));
    var captured = org.mockito.ArgumentCaptor.forClass(NameStyle.class);
    verify(styles, times(2)).setStyle(eq(player.getUniqueId()), captured.capture());
    assertEquals(colors, captured.getValue().getGradientColors());
    assertEquals(StyleType.GRADIENT, captured.getValue().getType());
    verify(sounds).play(player, GuiSounds.Event.CONFIRM);
    verify(player, never()).setItemOnCursor(any());
  }

}
