package com.mrkoszos.chromatica.gui;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.mrkoszos.chromatica.Chromatica;
import com.mrkoszos.chromatica.manager.*;
import com.mrkoszos.chromatica.model.NameStyle;
import java.util.*;
import java.util.function.BooleanSupplier;
import net.kyori.adventure.text.Component;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.*;

/** The previous confirmation-inventory regressions migrated to the inline lifecycle. */
class ConfirmationMenuTest {
  Chromatica plugin;
  Player player;
  Inventory inventory;
  InventoryView view;
  InlineConfirmation listener;
  List<Runnable> tasks, expiry;
  BooleanSupplier action, valid;
  Runnable refresh;
  GuiSounds sounds;
  Map<Integer, ItemStack> slots;

  @BeforeEach
  void setup() {
    plugin = mock(Chromatica.class);
    player = mock(Player.class);
    when(player.getUniqueId()).thenReturn(UUID.randomUUID());
    when(player.isOnline()).thenReturn(true);
    when(player.hasPermission("chromatica.use")).thenReturn(true);
    Server server = mock(Server.class);
    when(plugin.getServer()).thenReturn(server);
    inventory = mock(Inventory.class);
    when(inventory.getSize()).thenReturn(27);
    when(server.createInventory(any(InventoryHolder.class), eq(27), any(Component.class)))
        .thenAnswer(
            c -> {
              when(inventory.getHolder()).thenReturn(c.getArgument(0));
              return inventory;
            });
    GuiUtils.inventory(plugin, 27, Component.text("Unrelated"), "FavoritesMenu");
    view = mock(InventoryView.class);
    when(view.getTopInventory()).thenReturn(inventory);
    when(player.getOpenInventory()).thenReturn(view);
    slots = new HashMap<>();
    when(inventory.getItem(anyInt())).thenAnswer(c -> slots.get(c.getArgument(0)));
    doAnswer(
            c -> {
              slots.put(c.getArgument(0), c.getArgument(1));
              return null;
            })
        .when(inventory)
        .setItem(anyInt(), nullable(ItemStack.class));
    ItemStack original = item(), snapshot = item(), warning = item();
    when(original.clone()).thenReturn(snapshot);
    when(snapshot.clone()).thenReturn(warning);
    slots.put(11, original);
    slots.put(31, original);
    tasks = new ArrayList<>();
    expiry = new ArrayList<>();
    BukkitScheduler scheduler = mock(BukkitScheduler.class);
    when(server.getScheduler()).thenReturn(scheduler);
    when(scheduler.runTask(eq(plugin), any(Runnable.class)))
        .thenAnswer(
            c -> {
              tasks.add(c.getArgument(1));
              return null;
            });
    when(scheduler.runTaskLater(eq(plugin), any(Runnable.class), eq(100L)))
        .thenAnswer(
            c -> {
              expiry.add(c.getArgument(1));
              return null;
            });
    action = mock(BooleanSupplier.class);
    valid = mock(BooleanSupplier.class);
    refresh = mock(Runnable.class);
    when(valid.getAsBoolean()).thenReturn(true);
    when(action.getAsBoolean()).thenReturn(true);
    sounds = mock(GuiSounds.class);
    when(plugin.getGuiSounds()).thenReturn(sounds);
    listener = new InlineConfirmation(plugin);
  }

  ItemStack item() {
    ItemStack i = mock(ItemStack.class);
    when(i.getItemMeta()).thenReturn(mock(ItemMeta.class));
    when(i.clone()).thenReturn(i);
    return i;
  }

  void request() {
    InlineConfirmation.request(plugin, player, "original", 11, true, valid, action, refresh);
  }

  void drain() {
    var copy = List.copyOf(tasks);
    tasks.clear();
    copy.forEach(Runnable::run);
  }

  InventoryClickEvent click(int slot, ClickType type) {
    var e = mock(InventoryClickEvent.class);
    when(e.getView()).thenReturn(view);
    when(e.getWhoClicked()).thenReturn(player);
    when(e.getRawSlot()).thenReturn(slot);
    when(e.getClick()).thenReturn(type);
    when(e.isLeftClick()).thenReturn(type == ClickType.LEFT);
    when(e.getCurrentItem()).thenReturn(slots.get(slot));
    listener.onClick(e);
    return e;
  }

  @Test
  void firstQOnlyArmsAndClonesAppearance() {
    ItemStack original = slots.get(11);
    request();
    assertNotSame(original, slots.get(11));
    verify(slots.get(11).getItemMeta())
        .displayName(
            GuiUtils.text(
                "Drop again to delete", net.kyori.adventure.text.format.NamedTextColor.RED));
    verifyNoInteractions(action, refresh);
    verify(player, never()).openInventory(any(Inventory.class));
  }

  @Test
  void secondQIsDeferredAndSingleUse() {
    request();
    request();
    request();
    verifyNoInteractions(action);
    drain();
    verify(action).getAsBoolean();
    verify(refresh).run();
  }

  @Test
  void cancellationNeverExecutes() {
    request();
    InlineConfirmation.cancel(player);
    drain();
    verifyNoInteractions(action, refresh);
  }

  @Test
  void escapeInvalidatesQueuedConfirmation() {
    request();
    request();
    var e = mock(InventoryCloseEvent.class);
    when(e.getPlayer()).thenReturn(player);
    listener.onClose(e);
    drain();
    verifyNoInteractions(action, refresh);
  }

  @Test
  void replacingInventoryInvalidatesQueuedAction() {
    request();
    request();
    when(view.getTopInventory()).thenReturn(mock(Inventory.class));
    drain();
    verifyNoInteractions(action, refresh);
  }

  @Test
  void disconnectInvalidatesAction() {
    request();
    request();
    var e = mock(PlayerQuitEvent.class);
    when(e.getPlayer()).thenReturn(player);
    listener.onQuit(e);
    drain();
    verifyNoInteractions(action, refresh);
  }

  @Test
  void offlinePlayerCannotConfirm() {
    request();
    request();
    when(player.isOnline()).thenReturn(false);
    drain();
    verifyNoInteractions(action, refresh);
  }

  @Test
  void unrelatedAndUnsafeClicksCancel() {
    for (ClickType type : ClickType.values()) {
      if (type == ClickType.DROP || type == ClickType.CONTROL_DROP) continue;
      request();
      click(11, type);
      request();
      drain();
      verifyNoInteractions(action);
      InlineConfirmation.cancel(player);
    }
    request();
    click(27, ClickType.DROP);
    request();
    drain();
    verifyNoInteractions(action);
    verify(player, never()).setItemOnCursor(any());
  }

  @Test
  void allDraggingCancelsAndIsBlocked() {
    request();
    var e = mock(InventoryDragEvent.class);
    when(e.getView()).thenReturn(view);
    when(e.getWhoClicked()).thenReturn(player);
    listener.onDrag(e);
    verify(e).setCancelled(true);
    request();
    drain();
    verifyNoInteractions(action);
  }

  @Test
  void playerIdentityCannotClaimAnotherPlayersAction() {
    request();
    Player other = mock(Player.class);
    when(other.getUniqueId()).thenReturn(UUID.randomUUID());
    when(other.getOpenInventory()).thenReturn(view);
    InlineConfirmation.request(plugin, other, "original", 11, true, valid, action, refresh);
    drain();
    verifyNoInteractions(action);
  }

  @Test
  void expiryRestoresOriginalWithoutExecuting() {
    ItemStack original = slots.get(11).clone();
    request();
    expiry.getFirst().run();
    assertSame(original, slots.get(11));
    request();
    drain();
    verifyNoInteractions(action);
  }

  @Test
  void staleExpiryCannotOverwriteNewState() {
    request();
    Runnable stale = expiry.getFirst();
    InlineConfirmation.cancel(player);
    request();
    ItemStack warning = slots.get(11);
    stale.run();
    assertSame(warning, slots.get(11));
    request();
    drain();
    verify(action).getAsBoolean();
  }

  @Test
  void expiryCannotOverwriteChangedSlot() {
    request();
    ItemStack replacement = item();
    slots.put(11, replacement);
    expiry.getFirst().run();
    assertSame(replacement, slots.get(11));
  }

  @Test
  void failedOperationHasOnlyErrorActionSound() {
    when(action.getAsBoolean()).thenReturn(false);
    request();
    clearInvocations(sounds);
    request();
    drain();
    verify(sounds).play(player, GuiSounds.Event.ERROR);
    verifyNoMoreInteractions(sounds);
  }

  @Test
  void permissionIsRevalidatedAfterArming() {
    request();
    when(valid.getAsBoolean()).thenReturn(false);
    request();
    drain();
    verifyNoInteractions(action);
    verify(sounds).play(player, GuiSounds.Event.ERROR);
  }

  @Test
  void favoriteCancelLeavesDataUntouched() {
    var manager = mock(FavoriteManager.class);
    when(plugin.getFavoriteManager()).thenReturn(manager);
    var style = new NameStyle();
    when(manager.getFavorite(player.getUniqueId(), 0)).thenReturn(style);
    InlineConfirmation.favorite(plugin, player, 0, style);
    InlineConfirmation.cancel(player);
    drain();
    verify(manager, never()).removeFavorite(any(), anyInt());
  }

  @Test
  void favoriteUsesFixedSlotAndRejectsChangedOrMissingStyle() {
    var manager = mock(FavoriteManager.class);
    when(plugin.getFavoriteManager()).thenReturn(manager);
    var style = new NameStyle();
    when(manager.getFavorite(player.getUniqueId(), 0)).thenReturn(style.copy());
    try (var menus = mockConstruction(FavoritesMenu.class)) {
      InlineConfirmation.favorite(plugin, player, 0, style);
      var changed = style.copy();
      changed.setBold(true);
      when(manager.getFavorite(player.getUniqueId(), 0)).thenReturn(changed);
      InlineConfirmation.favorite(plugin, player, 0, changed);
      drain();
      verify(manager, never()).removeFavorite(any(), anyInt());
      when(manager.getFavorite(player.getUniqueId(), 0)).thenReturn(style.copy());
      InlineConfirmation.favorite(plugin, player, 0, style);
      when(manager.getFavorite(player.getUniqueId(), 0)).thenReturn(null);
      InlineConfirmation.favorite(plugin, player, 0, style);
      drain();
      verify(manager, never()).removeFavorite(any(), anyInt());
    }
  }

  @Test
  void favoriteSuccessfulDeletionOnlyRemovesOriginalSlot() {
    var manager = mock(FavoriteManager.class);
    when(plugin.getFavoriteManager()).thenReturn(manager);
    var style = new NameStyle();
    when(manager.getFavorite(player.getUniqueId(), 0)).thenReturn(style);
    when(manager.removeFavorite(player.getUniqueId(), 0)).thenReturn(true);
    try (var menus = mockConstruction(FavoritesMenu.class)) {
      InlineConfirmation.favorite(plugin, player, 0, style);
      InlineConfirmation.favorite(plugin, player, 0, style);
      drain();
      verify(manager).removeFavorite(player.getUniqueId(), 0);
      verify(menus.constructed().getFirst()).refresh(player, inventory);
    }
  }

  UUID community(boolean owner) {
    GuiUtils.inventory(plugin, 27, Component.text("Community"), "CommunityPresetMenu");
    UUID id = UUID.randomUUID();
    var holder = (GuiUtils.MenuHolder) inventory.getHolder();
    holder.entries = List.of(id.toString());
    holder.page = 2;
    var manager = mock(CommunityPresetManager.class);
    when(plugin.getCommunityPresetManager()).thenReturn(manager);
    when(plugin.isCommunityPresetsEnabled()).thenReturn(true);
    var preset =
        new CommunityPresetManager.Preset(
            id,
            "Saved",
            owner ? player.getUniqueId() : UUID.randomUUID(),
            "Tester",
            1L,
            new NameStyle());
    when(manager.get(id)).thenReturn(preset);
    slots.put(10, slots.get(11));
    return id;
  }

  @Test
  void communityCancelPreservesPageAndData() {
    UUID id = community(true);
    InlineConfirmation.community(plugin, player, id, 10, 2);
    InlineConfirmation.cancel(player);
    verify(plugin.getCommunityPresetManager(), never())
        .delete(any(), any(), anyBoolean(), anyBoolean());
    assertEquals(2, ((GuiUtils.MenuHolder) inventory.getHolder()).page);
  }

  @Test
  void communityUsesUuidAndCurrentAdminPermissionAndReturnsToPage() {
    UUID id = community(false);
    when(player.hasPermission("chromatica.community.admin")).thenReturn(true);
    try (var menus = mockConstruction(PresetBrowserMenu.class)) {
      InlineConfirmation.community(plugin, player, id, 10, 2);
      when(player.hasPermission("chromatica.community.admin")).thenReturn(false);
      InlineConfirmation.community(plugin, player, id, 10, 2);
      drain();
      verify(plugin.getCommunityPresetManager(), never())
          .delete(any(), any(), anyBoolean(), anyBoolean());
      when(player.hasPermission("chromatica.community.admin")).thenReturn(true);
      when(plugin.getCommunityPresetManager().delete(id, player.getUniqueId(), true, true))
          .thenReturn(CommunityPresetManager.Status.SUCCESS);
      InlineConfirmation.community(plugin, player, id, 10, 2);
      InlineConfirmation.community(plugin, player, id, 10, 2);
      drain();
      verify(plugin.getCommunityPresetManager()).delete(id, player.getUniqueId(), true, true);
      verify(menus.constructed().getLast()).refresh(player, inventory, 2);
    }
  }

  @Test
  void missingUuidAndChangedVisibleSlotCannotDeleteAnotherPreset() {
    UUID id = community(true);
    try (var menus = mockConstruction(PresetBrowserMenu.class)) {
      InlineConfirmation.community(plugin, player, id, 10, 2);
      ((GuiUtils.MenuHolder) inventory.getHolder()).entries = List.of(UUID.randomUUID().toString());
      InlineConfirmation.community(plugin, player, id, 10, 2);
      drain();
      verify(plugin.getCommunityPresetManager(), never())
          .delete(any(), any(), anyBoolean(), anyBoolean());
      ((GuiUtils.MenuHolder) inventory.getHolder()).entries = List.of(id.toString());
      InlineConfirmation.community(plugin, player, id, 10, 2);
      when(plugin.getCommunityPresetManager().get(id)).thenReturn(null);
      InlineConfirmation.community(plugin, player, id, 10, 2);
      drain();
      verify(plugin.getCommunityPresetManager(), never())
          .delete(any(), any(), anyBoolean(), anyBoolean());
    }
  }

  @Test
  void favoriteDropRequestsInlineConfirmationAndCancelsActualDrop() {
    var manager = mock(FavoriteManager.class);
    when(plugin.getFavoriteManager()).thenReturn(manager);
    var style = new NameStyle();
    when(manager.getFavorite(player.getUniqueId(), 0)).thenReturn(style);
    for (ClickType type : List.of(ClickType.DROP, ClickType.CONTROL_DROP)) {
      InlineConfirmation.cancel(player);
      var e = click(11, type);
      try (var capture = mockStatic(InlineConfirmation.class)) {
        capture.when(() -> InlineConfirmation.isDrop(e)).thenReturn(true);
        capture
            .when(() -> InlineConfirmation.favorite(plugin, player, 0, style))
            .thenAnswer(c -> null);
        new FavoritesMenuListener(plugin).onInventoryClick(e);
        capture.verify(() -> InlineConfirmation.favorite(plugin, player, 0, style));
        verify(e).setCancelled(true);
      }
    }
    verify(manager, never()).removeFavorite(any(), anyInt());
  }

  @Test
  void resetRequiresTwoOrdinaryLeftClicksAndNeverRightOrDoubleClick() {
    GuiUtils.inventory(plugin, 27, Component.text("Main"), "ChromaticaMenu");
    when(inventory.getSize()).thenReturn(45);
    var manager = mock(NameStyleManager.class);
    when(plugin.getNameStyleManager()).thenReturn(manager);
    when(manager.resetStyle(player.getUniqueId())).thenReturn(true);
    var menu = new ChromaticaMenuListener(plugin);
    try (var menus = mockConstruction(ChromaticaMenu.class)) {
      menu.onInventoryClick(click(31, ClickType.RIGHT));
      menu.onInventoryClick(click(31, ClickType.DOUBLE_CLICK));
      verifyNoInteractions(manager);
      menu.onInventoryClick(click(31, ClickType.LEFT));
      verifyNoInteractions(manager);
      menu.onInventoryClick(click(31, ClickType.LEFT));
      menu.onInventoryClick(click(31, ClickType.LEFT));
      drain();
      verify(manager).resetStyle(player.getUniqueId());
    }
  }

  @Test
  void resetCancelLeavesStyleUntouched() {
    var manager = mock(NameStyleManager.class);
    when(plugin.getNameStyleManager()).thenReturn(manager);
    InlineConfirmation.reset(plugin, player, player.getUniqueId(), "chromatica.use", false);
    InlineConfirmation.cancel(player);
    drain();
    verifyNoInteractions(manager);
  }

  @Test
  void resetChecksPermissionAgainAndReportsStorageFailure() {
    var manager = mock(NameStyleManager.class);
    when(plugin.getNameStyleManager()).thenReturn(manager);
    InlineConfirmation.reset(plugin, player, player.getUniqueId(), "chromatica.use", true);
    when(player.hasPermission("chromatica.use")).thenReturn(false);
    InlineConfirmation.reset(plugin, player, player.getUniqueId(), "chromatica.use", true);
    drain();
    verifyNoInteractions(manager);
    when(player.hasPermission("chromatica.use")).thenReturn(true);
    InlineConfirmation.reset(plugin, player, player.getUniqueId(), "chromatica.use", true);
    InlineConfirmation.reset(plugin, player, player.getUniqueId(), "chromatica.use", true);
    drain();
    verify(manager).resetStyle(player.getUniqueId());
    verify(sounds, never()).play(player, GuiSounds.Event.CONFIRM);
  }

  @Test
  void playerResetAndClearCommandsRequireRepeatedInvocation() {
    when(player.hasPermission("chromatica.reset")).thenReturn(true);
    var manager = mock(NameStyleManager.class);
    when(plugin.getNameStyleManager()).thenReturn(manager);
    var executor = new com.mrkoszos.chromatica.command.ChromaticaCommand(plugin);
    executor.onCommand(
        player, mock(org.bukkit.command.Command.class), "chromatica", new String[] {"reset"});
    verifyNoInteractions(manager);
    executor.onCommand(
        player, mock(org.bukkit.command.Command.class), "chromatica", new String[] {"clear"});
    drain();
    verify(manager).resetStyle(player.getUniqueId());
  }

  @Test
  void communityDropListenerUsesImmutableUuidAndPage() {
    UUID id = community(true);
    var event = click(10, ClickType.CONTROL_DROP);
    try (var capture = mockStatic(InlineConfirmation.class)) {
      capture.when(() -> InlineConfirmation.isDrop(event)).thenReturn(true);
      new PresetBrowserListener(plugin).onClick(event);
      capture.verify(() -> InlineConfirmation.community(plugin, player, id, 10, 2));
      verify(event).setCancelled(true);
      verify(plugin.getCommunityPresetManager(), never())
          .delete(any(), any(), anyBoolean(), anyBoolean());
    }
  }

  @Test
  void unrelatedCommandCancelsPendingAction() {
    request();
    var event = mock(org.bukkit.event.player.PlayerCommandPreprocessEvent.class);
    when(event.getPlayer()).thenReturn(player);
    when(event.getMessage()).thenReturn("/help");
    listener.onCommand(event);
    request();
    drain();
    verifyNoInteractions(action);
  }

  @Test
  void targetedCommandCapturesUuidAndConsoleRemainsImmediate() {
    var target = mock(org.bukkit.OfflinePlayer.class);
    UUID targetId = UUID.randomUUID();
    when(target.hasPlayedBefore()).thenReturn(true);
    when(target.getUniqueId()).thenReturn(targetId);
    when(player.hasPermission("chromatica.reset")).thenReturn(true);
    var manager = mock(NameStyleManager.class);
    when(plugin.getNameStyleManager()).thenReturn(manager);
    when(manager.resetStyle(targetId)).thenReturn(true);
    var executor = new com.mrkoszos.chromatica.command.ChromaticaCommand(plugin);
    try (var bukkit = mockStatic(org.bukkit.Bukkit.class)) {
      bukkit.when(() -> org.bukkit.Bukkit.getOfflinePlayer("Target")).thenReturn(target);
      executor.onCommand(
          player,
          mock(org.bukkit.command.Command.class),
          "chromatica",
          new String[] {"reset", "Target"});
      verifyNoInteractions(manager);
      executor.onCommand(
          player,
          mock(org.bukkit.command.Command.class),
          "chromatica",
          new String[] {"clear", "Target"});
      drain();
      verify(manager).resetStyle(targetId);
      clearInvocations(manager);
      var console = mock(org.bukkit.command.CommandSender.class);
      when(console.hasPermission("chromatica.reset")).thenReturn(true);
      executor.onCommand(
          console,
          mock(org.bukkit.command.Command.class),
          "chromatica",
          new String[] {"reset", "Target"});
      verify(manager).resetStyle(targetId);
    }
  }

  @Test
  void resetToDifferentTargetRearmsInsteadOfConfirmingOriginal() {
    when(player.hasPermission("chromatica.reset")).thenReturn(true);
    var manager = mock(NameStyleManager.class);
    when(plugin.getNameStyleManager()).thenReturn(manager);
    InlineConfirmation.reset(plugin, player, UUID.randomUUID(), "chromatica.reset", true);
    InlineConfirmation.reset(plugin, player, UUID.randomUUID(), "chromatica.reset", true);
    drain();
    verifyNoInteractions(manager);
  }
}
