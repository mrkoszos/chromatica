package com.mrkoszos.chromatica.gui;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.mrkoszos.chromatica.Chromatica;
import com.mrkoszos.chromatica.manager.*;
import com.mrkoszos.chromatica.manager.PresetManager.PresetDefinition;
import com.mrkoszos.chromatica.model.*;
import com.mrkoszos.chromatica.renderer.NameRenderer;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.*;

class PresetBrowserRedesignTest {
  @TempDir Path folder;
  Chromatica plugin;
  Player player;
  Inventory inventory;
  InventoryView view;
  Server server;
  NameStyleManager styles;
  CommunityPresetManager community;
  Map<String, PresetDefinition> presets;
  Map<Integer, ItemStack> slots;
  GuiSounds sounds;
  List<Runnable> queued, expiry;
  MockedConstruction<ItemStack> items;
  MockedStatic<GuiTooltips> tooltips;
  MockedStatic<GuiHeads> heads;

  @BeforeEach void setup() {
    tooltips = mockStatic(GuiTooltips.class);
    tooltips.when(() -> GuiTooltips.clean(any())).thenAnswer(c -> c.getArgument(0));
    heads = mockStatic(GuiHeads.class);
    items = mockConstruction(ItemStack.class, (item, context) -> {
      when(item.getType()).thenReturn((Material) context.arguments().getFirst());
      SkullMeta meta = mock(SkullMeta.class);
      when(item.getItemMeta()).thenReturn(meta);
      doAnswer(c -> { when(meta.displayName()).thenReturn(c.getArgument(0)); return null; })
          .when(meta).displayName(any(Component.class));
      doAnswer(c -> { when(meta.lore()).thenReturn(c.getArgument(0)); return null; })
          .when(meta).lore(anyList());
      when(item.clone()).thenReturn(item);
    });
    plugin = mock(Chromatica.class);
    server = mock(Server.class);
    when(plugin.getServer()).thenReturn(server);
    when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());
    when(plugin.getConfig()).thenReturn(new YamlConfiguration());
    when(plugin.isCommunityPresetsEnabled()).thenReturn(true);
    when(plugin.getCommunityPresetLimit()).thenReturn(100);
    player = mock(Player.class);
    when(player.getUniqueId()).thenReturn(UUID.randomUUID());
    when(player.getName()).thenReturn("Tester");
    when(player.isOnline()).thenReturn(true);
    when(player.hasPermission("chromatica.use")).thenReturn(true);
    styles = new NameStyleManager(folder.toFile(), Logger.getAnonymousLogger());
    when(plugin.getNameStyleManager()).thenReturn(styles);
    when(plugin.getNameRenderer()).thenReturn(new NameRenderer());
    community = new CommunityPresetManager(folder.toFile(), Logger.getAnonymousLogger());
    when(plugin.getCommunityPresetManager()).thenReturn(community);
    presets = new LinkedHashMap<>();
    var manager = mock(PresetManager.class);
    when(plugin.getPresetManager()).thenReturn(manager);
    when(manager.getAll()).thenReturn(presets);
    when(manager.get(anyString())).thenAnswer(c -> presets.get(c.getArgument(0)));
    sounds = mock(GuiSounds.class);
    when(plugin.getGuiSounds()).thenReturn(sounds);
    var background = new BackgroundManager(plugin);
    when(plugin.getBackgroundManager()).thenReturn(background);
    inventory = mock(Inventory.class);
    slots = new HashMap<>();
    when(inventory.getSize()).thenReturn(45);
    when(inventory.getItem(anyInt())).thenAnswer(c -> slots.get(c.getArgument(0)));
    doAnswer(c -> { slots.put(c.getArgument(0), c.getArgument(1)); return null; })
        .when(inventory).setItem(anyInt(), nullable(ItemStack.class));
    doAnswer(c -> { slots.clear(); return null; }).when(inventory).clear();
    when(server.createInventory(any(InventoryHolder.class), anyInt(), any(Component.class)))
        .thenAnswer(c -> {
          when(inventory.getHolder()).thenReturn(c.getArgument(0));
          when(inventory.getSize()).thenReturn(c.getArgument(1));
          return inventory;
        });
    view = mock(InventoryView.class);
    when(view.getTopInventory()).thenReturn(inventory);
    when(player.getOpenInventory()).thenReturn(view);
    queued = new ArrayList<>(); expiry = new ArrayList<>();
    var scheduler = mock(BukkitScheduler.class);
    when(server.getScheduler()).thenReturn(scheduler);
    when(scheduler.runTask(eq(plugin), any(Runnable.class)))
        .thenAnswer(c -> { queued.add(c.getArgument(1)); return null; });
    when(scheduler.runTaskLater(eq(plugin), any(Runnable.class), eq(100L)))
        .thenAnswer(c -> { expiry.add(c.getArgument(1)); return null; });
  }

  @AfterEach void cleanup() {
    InlineConfirmation.clear(plugin);
    heads.close(); items.close(); tooltips.close();
  }

  void open(boolean isCommunity, int page) {
    GuiUtils.inventory(plugin, 45, Component.text("Unrelated title"),
        isCommunity ? "CommunityPresetMenu" : "ServerPresetMenu");
    new PresetBrowserMenu(plugin, isCommunity).open(player, page);
  }
  GuiUtils.MenuHolder holder() { return (GuiUtils.MenuHolder) inventory.getHolder(); }
  InventoryClickEvent click(int slot, ClickType type) {
    var e = mock(InventoryClickEvent.class);
    when(e.getView()).thenReturn(view);
    when(e.getRawSlot()).thenReturn(slot);
    when(e.getWhoClicked()).thenReturn(player);
    when(e.getClick()).thenReturn(type);
    when(e.isLeftClick()).thenReturn(type == ClickType.LEFT);
    when(e.isRightClick()).thenReturn(type == ClickType.RIGHT);
    when(e.getCurrentItem()).thenReturn(slots.get(slot));
    new InlineConfirmation(plugin).onClick(e);
    new PresetBrowserListener(plugin).onClick(e);
    verify(e).setCancelled(true);
    verify(e, never()).setCursor(any());
    return e;
  }
  void drain() { var work = List.copyOf(queued); queued.clear(); work.forEach(Runnable::run); }
  void servers(int size) {
    for (int i = 0; i < size; i++) {
      String id = "server" + i;
      presets.put(id, new PresetDefinition(id, id,
          List.of(String.format("#%06X", 0x100000 + i)), Material.NETHER_STAR));
    }
  }
  void communities(int size) {
    for (int i = 0; i < size; i++)
      assertEquals(CommunityPresetManager.Status.SUCCESS,
          community.publish(player.getUniqueId(), "Tester", "Preset " + i,
              new NameStyle(), 100, true).status());
  }
  void material(int slot, Material material) { assertEquals(material, slots.get(slot).getType()); }

  @Test void bothBrowsersHaveExact45SlotLayoutAndTrulyEmptyUnusedPositions() {
    assertArrayEquals(new int[] {10,11,12,13,14,15,16,19,20,21,22,23,24,25,28,29,30,31,32,33,34},
        PresetBrowserMenu.PRESET_SLOTS);
    servers(4); communities(4);
    for (boolean isCommunity : new boolean[] {false, true}) {
      open(isCommunity, 0);
      assertEquals(45, inventory.getSize());
      material(39, isCommunity ? Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE : Material.BUNDLE);
      material(40, Material.PAPER); material(41, Material.BARRIER);
      assertEquals(4, holder().entries.size());
      for (int i = 0; i < 4; i++) assertNotNull(slots.get(PresetBrowserMenu.PRESET_SLOTS[i]));
      plugin.getBackgroundManager().applyBackground(inventory);
      for (int i = 4; i < 21; i++) assertNull(slots.get(PresetBrowserMenu.PRESET_SLOTS[i]));
      for (int slot : new int[] {0,9,17,18,26,27,35,36,38,42,44})
        assertTrue(slots.get(slot).getType().name().contains("STAINED_GLASS_PANE"));
      assertTrue(slots.keySet().stream().allMatch(slot -> slot < 45));
    }
    UUID creator = player.getUniqueId();
    heads.verify(() -> GuiHeads.creatorHead(eq(plugin), eq(creator), eq(inventory), anyInt()), times(4));
  }

  @Test void leftPreviousRightNextNeverWrapAndPreserveInventoryAndSelection() {
    servers(43); open(false, 0);
    click(40, ClickType.LEFT); assertEquals(0, holder().page);
    verifyNoInteractions(sounds);
    click(40, ClickType.RIGHT); assertEquals(1, holder().page);
    assertEquals("server21", holder().entries.getFirst());
    assertEquals(GuiUtils.text("Page 2/3", NamedTextColor.GRAY), slots.get(40).getItemMeta().displayName());
    click(40, ClickType.RIGHT); assertEquals(2, holder().page);
    click(40, ClickType.RIGHT); assertEquals(2, holder().page);
    click(40, ClickType.LEFT); assertEquals(1, holder().page);
    verify(sounds, times(3)).play(player, GuiSounds.Event.NAVIGATE);
    verifyNoMoreInteractions(sounds);
    verify(player, never()).openInventory(any(Inventory.class));
    verify(player, never()).setItemOnCursor(any());
    assertEquals(new NameStyle(), styles.getStyle(player.getUniqueId()));
  }

  @Test void randomCanSelectEveryEligibleEntryAcrossAllPagesWithOneSoundAndGlint() {
    servers(23); open(false, 0);
    var draft = styles.getStyle(player.getUniqueId()); draft.setBold(true); draft.setItalic(true);
    assertTrue(styles.setStyle(player.getUniqueId(), draft));
    try (var randomness = mockStatic(ThreadLocalRandom.class)) {
      var random = mock(ThreadLocalRandom.class);
      randomness.when(ThreadLocalRandom::current).thenReturn(random);
      for (int i = 0; i < 23; i++) {
        when(random.nextInt(23)).thenReturn(i);
        click(39, ClickType.LEFT);
        var applied = styles.getStyle(player.getUniqueId());
        assertEquals("server" + i, applied.getPresetId());
        assertTrue(applied.isBold()); assertTrue(applied.isItalic());
      }
      verify(random, times(23)).nextInt(23);
    }
    assertEquals(23, presets.size());
    verify(sounds, times(23)).play(player, GuiSounds.Event.CONFIRM);
    verifyNoMoreInteractions(sounds);
    click(40, ClickType.RIGHT);
    verify(slots.get(11).getItemMeta()).setEnchantmentGlintOverride(true);
    assertEquals(1, holder().page);
  }

  @Test void randomFiltersInvalidAndScrambleInaccessiblePresetsAndHonorsRevocation() {
    servers(1);
    presets.put("locked", new PresetDefinition("locked", "Locked", List.of("#112233"), Material.BOOK, true));
    presets.put("bad", new PresetDefinition("bad", "Bad", List.of("invalid"), Material.BOOK));
    // Invalid catalogue entries cannot normally load; test the click path without rendering them.
    GuiUtils.inventory(plugin, 45, Component.text("Server Presets"), "ServerPresetMenu");
    try (var menus = mockConstruction(PresetBrowserMenu.class);
        var randomness = mockStatic(ThreadLocalRandom.class)) {
      var random = mock(ThreadLocalRandom.class);
      randomness.when(ThreadLocalRandom::current).thenReturn(random);
      when(random.nextInt(1)).thenReturn(0);
      click(39, ClickType.LEFT);
      assertEquals("server0", styles.getStyle(player.getUniqueId()).getPresetId());
      verify(random).nextInt(1);
      when(player.hasPermission("chromatica.use")).thenReturn(false);
      click(39, ClickType.LEFT);
      verifyNoMoreInteractions(random);
      verify(sounds).play(player, GuiSounds.Event.ERROR);
      verify(sounds).play(player, GuiSounds.Event.CONFIRM);
    }
  }

  @Test void randomEmptyOrInheritedScrambleWithoutPermissionHasNoEligiblePreset() {
    open(false, 0);
    click(39, ClickType.LEFT);
    var draft = styles.getStyle(player.getUniqueId()); draft.setScramble(true);
    assertTrue(styles.setStyle(player.getUniqueId(), draft));
    servers(1);
    click(39, ClickType.LEFT);
    assertEquals(draft, styles.getStyle(player.getUniqueId()));
    verify(sounds, times(2)).play(player, GuiSounds.Event.ERROR);
    verifyNoMoreInteractions(sounds);
  }

  @Test void failedRandomPersistenceKeepsCommittedStyleSelectionAndCatalogue() throws Exception {
    servers(1); open(false, 0);
    var original = styles.getStyle(player.getUniqueId());
    Files.writeString(folder.resolve("playerdata"), "blocked directory");
    click(39, ClickType.LEFT);
    assertEquals(original, styles.getStyle(player.getUniqueId()));
    assertEquals(1, presets.size());
    verify(sounds).play(player, GuiSounds.Event.ERROR);
    verifyNoMoreInteractions(sounds);
  }

  @Test void unsafeAndBottomInventoryClicksNeverApplyRandomOrNavigateOrPublish() {
    servers(1); communities(1);
    for (boolean isCommunity : new boolean[] {false, true}) {
      open(isCommunity, 0);
      for (int slot : new int[] {10,39,40,41,45,50,-999})
        for (ClickType type : ClickType.values()) {
          if (type == ClickType.LEFT || type == ClickType.RIGHT) continue;
          if (slot == 10 && isCommunity && (type == ClickType.DROP || type == ClickType.CONTROL_DROP)) continue;
          click(slot, type);
        }
    }
    assertEquals(new NameStyle(), styles.getStyle(player.getUniqueId()));
    assertEquals(1, community.getAll().size());
    assertTrue(queued.isEmpty());
    verifyNoInteractions(sounds);
    click(39, ClickType.RIGHT); // Community Publish allows ordinary right click as before.
    assertEquals(1, queued.size());
  }

  @Test void publishUsesExistingSnapshotQuotaAndRevalidatesPermissionBeforeOpening() {
    open(true, 0);
    when(plugin.getCommunityPresetLimit()).thenReturn(0);
    click(39, ClickType.LEFT);
    assertTrue(queued.isEmpty());
    verify(sounds).play(player, GuiSounds.Event.LIMIT);
    when(plugin.getCommunityPresetLimit()).thenReturn(2);
    click(39, ClickType.LEFT);
    when(player.hasPermission("chromatica.use")).thenReturn(false);
    try (var publication = mockConstruction(PublishPresetMenu.class)) {
      drain(); assertTrue(publication.constructed().isEmpty());
    }
    when(player.hasPermission("chromatica.use")).thenReturn(true);
    click(39, ClickType.LEFT);
    try (var publication = mockConstruction(PublishPresetMenu.class)) {
      drain();
      verify(publication.constructed().getFirst()).open(eq(player), eq(new NameStyle()), eq(0));
    }
  }

  @Test void deletingOnlyPresetOnLastPageClampsToRealPreviousPageByUuid() {
    communities(22); open(true, 1);
    UUID target = UUID.fromString(holder().entries.getFirst());
    click(10, ClickType.DROP);
    assertNotNull(community.get(target));
    assertEquals(GuiUtils.text("Drop again to delete", NamedTextColor.RED), slots.get(10).getItemMeta().displayName());
    click(10, ClickType.CONTROL_DROP);
    drain();
    assertNull(community.get(target));
    assertEquals(21, community.getAll().size());
    assertEquals(0, holder().page); assertEquals(21, holder().entries.size());
    material(39, Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
    assertEquals(GuiUtils.text("Page 1/1", NamedTextColor.GRAY), slots.get(40).getItemMeta().displayName());
  }

  @Test void pageNavigationCancelsArmedDeletionAndBackOpensParentWithoutClosing() {
    communities(22); open(true, 0);
    UUID target = UUID.fromString(holder().entries.getFirst());
    click(10, ClickType.DROP);
    click(40, ClickType.RIGHT);
    drain();
    assertNotNull(community.get(target));
    try (var categories = mockConstruction(PresetMenu.class)) {
      click(41, ClickType.LEFT); drain();
      verify(categories.constructed().getFirst()).open(player);
    }
    verify(player, never()).closeInventory();
  }

  @Test void ordinaryRightClickOnServerRandomDoesNothing() {
    servers(1); open(false, 0);
    click(39, ClickType.RIGHT);
    assertEquals(new NameStyle(), styles.getStyle(player.getUniqueId()));
    verifyNoInteractions(sounds);
  }

  @Test void serverAndCommunityApplyFull21StopGradientsWithoutTruncation() {
    List<String> colors = new ArrayList<>();
    for (int i = 0; i < 21; i++) colors.add(String.format("#%06X", 0x111100 + i));
    presets.put("full", new PresetDefinition("full", "Full", colors, Material.NETHER_STAR));
    open(false, 0);
    click(10, ClickType.LEFT);
    assertEquals(colors, styles.getStyle(player.getUniqueId()).getGradientColors());
    var snapshot = styles.getStyle(player.getUniqueId());
    var saved = community.publish(player.getUniqueId(), "Tester", "Full", snapshot, 10, true);
    assertEquals(CommunityPresetManager.Status.SUCCESS, saved.status());
    assertTrue(styles.resetStyle(player.getUniqueId()));
    open(true, 0);
    click(10, ClickType.LEFT);
    assertEquals(snapshot, styles.getStyle(player.getUniqueId()));
    assertEquals(colors, community.get(saved.preset().id()).style().getGradientColors());
  }

  @Test void normalAndRandomApplySameFormattingAndCanDisableStoredScramble() {
    var original = styles.getStyle(player.getUniqueId());
    original.setBold(true); original.setUnderlined(true); original.setScramble(true);
    assertTrue(styles.setStyle(player.getUniqueId(), original));
    presets.put("safe", new PresetDefinition("safe", "Safe",
        List.of("#112233", "#AABBCC"), Material.NETHER_STAR, false));
    open(false, 0);
    click(10, ClickType.LEFT);
    var normal = styles.getStyle(player.getUniqueId());
    assertTrue(normal.isBold()); assertTrue(normal.isUnderlined()); assertFalse(normal.isScramble());
    assertTrue(styles.setStyle(player.getUniqueId(), original));
    click(39, ClickType.LEFT);
    assertEquals(normal, styles.getStyle(player.getUniqueId()));
    verify(sounds, times(2)).play(player, GuiSounds.Event.CONFIRM);
    verifyNoMoreInteractions(sounds);
  }

  @Test void staleRandomCandidateAndDropOnDisabledNavigationNeverApplyOrOpen() {
    servers(1);
    open(false, 0);
    when(plugin.getPresetManager().get("server0")).thenReturn(null);
    click(39, ClickType.LEFT);
    assertEquals(new NameStyle(), styles.getStyle(player.getUniqueId()));
    verify(sounds).play(player, GuiSounds.Event.ERROR);
    open(true, 0);
    when(plugin.isCommunityPresetsEnabled()).thenReturn(false);
    click(41, ClickType.DROP);
    click(40, ClickType.CONTROL_DROP);
    assertTrue(queued.isEmpty());
    verifyNoMoreInteractions(sounds);
  }
}
