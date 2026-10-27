package com.mrkoszos.chromatica.gui;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.mrkoszos.chromatica.Chromatica;
import com.mrkoszos.chromatica.manager.*;
import com.mrkoszos.chromatica.model.NameStyle;
import java.nio.file.Path;
import java.util.*;
import net.kyori.adventure.text.Component;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

class CommunityPresetGuiTest {
  @TempDir Path folder;

  private record Fixture(
      Chromatica plugin,
      Player player,
      Inventory inventory,
      InventoryClickEvent event,
      GuiUtils.MenuHolder holder) {}

  private Fixture fixture() {
    Chromatica plugin = mock(Chromatica.class);
    Server server = mock(Server.class);
    when(plugin.getServer()).thenReturn(server);
    Inventory inventory = mock(Inventory.class);
    when(inventory.getSize()).thenReturn(45);
    when(server.createInventory(any(InventoryHolder.class), eq(45), any(Component.class)))
        .thenAnswer(
            invocation -> {
              when(inventory.getHolder()).thenReturn(invocation.getArgument(0));
              return inventory;
            });
    GuiUtils.inventory(
        plugin, 45, Component.text("Unrelated visible title"), "CommunityPresetMenu");
    Player player = mock(Player.class);
    when(player.getUniqueId()).thenReturn(UUID.randomUUID());
    when(player.hasPermission("chromatica.use")).thenReturn(true);
    when(player.isOnline()).thenReturn(true);
    InventoryView view = mock(InventoryView.class);
    when(view.getTopInventory()).thenReturn(inventory);
    InventoryClickEvent event = mock(InventoryClickEvent.class);
    when(event.getView()).thenReturn(view);
    when(event.getWhoClicked()).thenReturn(player);
    when(event.getRawSlot()).thenReturn(PresetBrowserMenu.PRESET_SLOTS[0]);
    when(event.getClick()).thenReturn(ClickType.LEFT);
    when(event.isLeftClick()).thenReturn(true);
    BukkitScheduler scheduler = mock(BukkitScheduler.class);
    when(server.getScheduler()).thenReturn(scheduler);
    when(scheduler.runTask(eq(plugin), any(Runnable.class)))
        .thenAnswer(
            invocation -> {
              ((Runnable) invocation.getArgument(1)).run();
              return null;
            });
    return new Fixture(
        plugin, player, inventory, event, (GuiUtils.MenuHolder) inventory.getHolder());
  }

  @Test
  void disabledCommunityGuiCannotApplyOrPublish() {
    Fixture f = fixture();
    NameStyleManager styles = mock(NameStyleManager.class);
    when(styles.setStyle(any(), any())).thenReturn(true);
    when(f.plugin.getNameStyleManager()).thenReturn(styles);
    try (var categories = mockConstruction(PresetMenu.class)) {
      new PresetBrowserListener(f.plugin).onClick(f.event);
      verify(f.event).setCancelled(true);
      verifyNoInteractions(styles);
      verify(f.plugin, never()).getCommunityPresetManager();
      verify(categories.constructed().getFirst()).open(f.player);
    }
  }

  @Test
  void applyingUsesSavedIdAndIndependentStyleWithoutReopening() {
    Fixture f = fixture();
    when(f.plugin.isCommunityPresetsEnabled()).thenReturn(true);
    CommunityPresetManager manager =
        new CommunityPresetManager(folder.toFile(), java.util.logging.Logger.getAnonymousLogger());
    GuiSounds sounds = mock(GuiSounds.class);
    when(f.plugin.getGuiSounds()).thenReturn(sounds);
    NameStyle original = new NameStyle();
    original.setGradientColors(List.of("#112233", "#AABBCC"));
    original.setItalic(true);
    var preset =
        manager.publish(f.player.getUniqueId(), "Tester", "Sunset", original, 10, true).preset();
    when(f.plugin.getCommunityPresetManager()).thenReturn(manager);
    f.holder.entries = List.of(preset.id().toString());
    NameStyleManager styles = mock(NameStyleManager.class);
    when(styles.setStyle(any(), any())).thenReturn(true);
    when(f.plugin.getNameStyleManager()).thenReturn(styles);
    try (var browsers = mockConstruction(PresetBrowserMenu.class)) {
      new PresetBrowserListener(f.plugin).onClick(f.event);
      ArgumentCaptor<NameStyle> applied = ArgumentCaptor.forClass(NameStyle.class);
      verify(styles).setStyle(eq(f.player.getUniqueId()), applied.capture());
      assertEquals(original, applied.getValue());
      applied.getValue().getGradientColors().clear();
      assertEquals(original, manager.get(preset.id()).style());
      verify(browsers.constructed().getFirst()).refresh(f.player, f.inventory, 0);
      verify(f.player, never()).openInventory(any(Inventory.class));
      verify(sounds).play(f.player, GuiSounds.Event.CONFIRM);
      verifyNoMoreInteractions(sounds);
    }
  }

  @Test
  void deletionByAnotherViewerDoesNotShiftClickTarget() {
    Fixture f = fixture();
    when(f.plugin.isCommunityPresetsEnabled()).thenReturn(true);
    CommunityPresetManager manager =
        new CommunityPresetManager(folder.toFile(), java.util.logging.Logger.getAnonymousLogger());
    var first =
        manager
            .publish(f.player.getUniqueId(), "Tester", "First", new NameStyle(), 10, true)
            .preset();
    manager.publish(f.player.getUniqueId(), "Tester", "Second", new NameStyle(), 10, true);
    f.holder.entries = List.of(first.id().toString());
    manager.delete(first.id(), f.player.getUniqueId(), false, true);
    when(f.plugin.getCommunityPresetManager()).thenReturn(manager);
    NameStyleManager styles = mock(NameStyleManager.class);
    when(styles.setStyle(any(), any())).thenReturn(true);
    when(f.plugin.getNameStyleManager()).thenReturn(styles);
    try (var browsers = mockConstruction(PresetBrowserMenu.class)) {
      new PresetBrowserListener(f.plugin).onClick(f.event);
      verifyNoInteractions(styles);
      verify(browsers.constructed().getFirst()).refresh(f.player, f.inventory, 0);
    }
  }

  @Test
  void sameMenuOpenCopiesPageIdentityAlongWithContents() {
    Fixture f = fixture();
    f.holder.page = 0;
    Inventory target = mock(Inventory.class);
    GuiUtils.MenuHolder other =
        (GuiUtils.MenuHolder)
            GuiUtils.inventory(f.plugin, 45, Component.text("Other title"), "CommunityPresetMenu")
                .getHolder();
    // Use distinct holders even though the mocked server reuses its Inventory object.
    GuiUtils.MenuHolder current = f.holder;
    current.page = 0;
    current.entries = List.of("first");
    other.page = 2;
    other.entries = List.of("last");
    when(f.inventory.getHolder()).thenReturn(current);
    when(target.getHolder()).thenReturn(other);
    when(target.getContents()).thenReturn(new ItemStack[45]);
    when(target.getSize()).thenReturn(45);
    InventoryView view = mock(InventoryView.class);
    when(view.getTopInventory()).thenReturn(f.inventory);
    when(f.player.getOpenInventory()).thenReturn(view);
    GuiUtils.open(f.player, target);
    assertEquals(2, current.page);
    assertEquals(List.of("last"), current.entries);
    verify(f.player, never()).openInventory(any(Inventory.class));
  }

  @Test
  void backReturnsToCategoriesEvenIfCommunityIsDisabled() {
    Fixture f = fixture();
    when(f.event.getRawSlot()).thenReturn(PresetBrowserMenu.BACK_SLOT);
    try (var categories = mockConstruction(PresetMenu.class)) {
      new PresetBrowserListener(f.plugin).onClick(f.event);
      verify(f.player, never()).closeInventory();
      verify(categories.constructed().getFirst()).open(f.player);
      verify(f.plugin, never()).getCommunityPresetManager();
    }
  }
}
