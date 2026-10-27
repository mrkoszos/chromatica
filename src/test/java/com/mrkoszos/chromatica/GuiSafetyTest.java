package com.mrkoszos.chromatica;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.mrkoszos.chromatica.gui.*;
import java.util.Set;
import net.kyori.adventure.text.Component;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import org.junit.jupiter.api.Test;

class GuiSafetyTest {
  private Inventory menu(Chromatica plugin, String name) {
    Server server = mock(Server.class);
    when(plugin.getServer()).thenReturn(server);
    Inventory inventory = mock(Inventory.class);
    when(inventory.getSize()).thenReturn(27);
    when(server.createInventory(any(InventoryHolder.class), eq(27), any(Component.class)))
        .thenAnswer(
            invocation -> {
              when(inventory.getHolder()).thenReturn(invocation.getArgument(0));
              return inventory;
            });
    return GuiUtils.inventory(plugin, 27, Component.text("title"), name);
  }

  @Test
  void unsafeClicksAndBottomInventoryClicksCannotTriggerMenuActions() {
    Chromatica plugin = mock(Chromatica.class);
    Inventory top = menu(plugin, "ChromaticaMenu");
    InventoryView view = mock(InventoryView.class);
    when(view.getTopInventory()).thenReturn(top);
    ChromaticaMenuListener listener = new ChromaticaMenuListener(plugin);
    for (ClickType click : ClickType.values()) {
      if (click == ClickType.LEFT || click == ClickType.RIGHT) continue;
      InventoryClickEvent event = mock(InventoryClickEvent.class);
      when(event.getView()).thenReturn(view);
      when(event.getClick()).thenReturn(click);
      when(event.getRawSlot()).thenReturn(19);
      listener.onInventoryClick(event);
      verify(event).setCancelled(true);
      verify(event, never()).getWhoClicked();
    }
    InventoryClickEvent bottom = mock(InventoryClickEvent.class);
    when(bottom.getView()).thenReturn(view);
    when(bottom.getClick()).thenReturn(ClickType.LEFT);
    when(bottom.getRawSlot()).thenReturn(35);
    listener.onInventoryClick(bottom);
    verify(bottom).setCancelled(true);
    verify(bottom, never()).getWhoClicked();
    verify(plugin, never()).getNameStyleManager();
  }

  @Test
  void topInventoryDragIsBlockedAndUnrelatedInventoryIsUntouched() {
    Chromatica plugin = mock(Chromatica.class);
    Inventory top = menu(plugin, "FavoritesMenu");
    InventoryView view = mock(InventoryView.class);
    when(view.getTopInventory()).thenReturn(top);
    InventoryDragEvent drag = mock(InventoryDragEvent.class);
    when(drag.getView()).thenReturn(view);
    when(drag.getRawSlots()).thenReturn(Set.of(11, 35));
    new GuiSafetyListener(plugin).onDrag(drag);
    verify(drag).setCancelled(true);
    when(view.getTopInventory()).thenReturn(mock(Inventory.class));
    InventoryDragEvent normal = mock(InventoryDragEvent.class);
    when(normal.getView()).thenReturn(view);
    new GuiSafetyListener(plugin).onDrag(normal);
    verify(normal, never()).setCancelled(anyBoolean());
    InventoryClickEvent normalClick = mock(InventoryClickEvent.class);
    when(normalClick.getView()).thenReturn(view);
    new ChromaticaMenuListener(plugin).onInventoryClick(normalClick);
    verify(normalClick, never()).setCancelled(anyBoolean());
  }

  @Test
  void openingSameMenuUpdatesExistingInventoryWithoutOpeningAgain() {
    Chromatica plugin = mock(Chromatica.class);
    Inventory current = menu(plugin, "FormattingMenu");
    Inventory target = mock(Inventory.class);
    InventoryHolder holder = current.getHolder();
    when(target.getHolder()).thenReturn(holder);
    ItemStack[] contents = new ItemStack[27];
    when(target.getContents()).thenReturn(contents);
    int currentSize = current.getSize();
    when(target.getSize()).thenReturn(currentSize);
    Player player = mock(Player.class);
    when(player.hasPermission("chromatica.use")).thenReturn(true);
    when(player.isOnline()).thenReturn(true);
    InventoryView view = mock(InventoryView.class);
    when(player.getOpenInventory()).thenReturn(view);
    when(view.getTopInventory()).thenReturn(current);
    GuiUtils.open(player, target);
    verify(current).setContents(contents);
    verify(player, never()).openInventory(any(Inventory.class));
  }

  @Test
  void containerTitlesHaveNoExplicitStyleAndIdentityDoesNotDependOnTitle() {
    Component[] titles = {
      ChromaticaMenu.TITLE,
      ColorMenu.TITLE,
      ColorSelectorMenu.TITLE,
      HexColorMenu.TITLE,
      GradientMenu.TITLE,
      FormattingMenu.TITLE,
      PresetMenu.TITLE,
      PresetBrowserMenu.SERVER_TITLE,
      PresetBrowserMenu.COMMUNITY_TITLE,
      PublishPresetMenu.TITLE,
      FavoritesMenu.TITLE
    };
    for (Component title : titles) {
      org.junit.jupiter.api.Assertions.assertEquals(
          net.kyori.adventure.text.format.Style.empty(), title.style());
    }
    Chromatica plugin = mock(Chromatica.class);
    Inventory inventory =
        menu(plugin, "FavoritesMenu"); // Visible title is "title", not "Favorites".
    org.junit.jupiter.api.Assertions.assertTrue(GuiUtils.isMenu(inventory, "FavoritesMenu"));
    org.junit.jupiter.api.Assertions.assertFalse(GuiUtils.isMenu(inventory, "GradientMenu"));
  }

  private InventoryClickEvent navigationClick(
      Chromatica plugin, String name, int slot, Player player) {
    when(player.isOnline()).thenReturn(true);
    Inventory top = menu(plugin, name);
    InventoryView view = mock(InventoryView.class);
    when(view.getTopInventory()).thenReturn(top);
    org.bukkit.scheduler.BukkitScheduler scheduler =
        mock(org.bukkit.scheduler.BukkitScheduler.class);
    when(plugin.getServer().getScheduler()).thenReturn(scheduler);
    when(scheduler.runTask(eq(plugin), any(Runnable.class)))
        .thenAnswer(
            invocation -> {
              ((Runnable) invocation.getArgument(1)).run();
              return mock(org.bukkit.scheduler.BukkitTask.class);
            });
    InventoryClickEvent event = mock(InventoryClickEvent.class);
    when(event.getView()).thenReturn(view);
    when(event.getClick()).thenReturn(ClickType.LEFT);
    when(event.getRawSlot()).thenReturn(slot);
    when(event.getWhoClicked()).thenReturn(player);
    ItemStack item = mock(ItemStack.class);
    when(item.getType()).thenReturn(org.bukkit.Material.BARRIER);
    when(event.getCurrentItem()).thenReturn(item);
    return event;
  }

  @Test
  void selectorBackReturnsToItsActualParent() {
    for (ColorSelectionContext context : ColorSelectionContext.values()) {
      Chromatica plugin = mock(Chromatica.class);
      Player player = mock(Player.class);
      when(player.hasPermission("chromatica.use")).thenReturn(true);
      java.util.UUID uuid = java.util.UUID.randomUUID();
      when(player.getUniqueId()).thenReturn(uuid);
      ColorSelectorSession session = new ColorSelectorSession();
      session.set(uuid, context);
      when(plugin.getColorSelectorSession()).thenReturn(session);
      java.util.Map<java.util.UUID, Integer> indices = new java.util.HashMap<>();
      indices.put(uuid, 0);
      when(plugin.getGradientColorSelection()).thenReturn(indices);
      InventoryClickEvent event =
          navigationClick(plugin, "ColorSelectorMenu", ColorSelectorMenu.BACK_SLOT, player);
      try (var colors = mockConstruction(ColorMenu.class);
          var gradients = mockConstruction(GradientMenu.class)) {
        new ColorSelectorListener(plugin).onInventoryClick(event);
        if (context == ColorSelectionContext.GRADIENT) {
          verify(gradients.constructed().get(0)).open(player);
          org.junit.jupiter.api.Assertions.assertTrue(colors.constructed().isEmpty());
        } else {
          verify(colors.constructed().get(0)).open(player);
          org.junit.jupiter.api.Assertions.assertTrue(gradients.constructed().isEmpty());
        }
      }
      verify(player, never()).closeInventory();
      org.junit.jupiter.api.Assertions.assertNull(session.get(uuid));
      org.junit.jupiter.api.Assertions.assertFalse(indices.containsKey(uuid));
    }
  }

  @Test
  void selectorCloseOnlyClosesAndFavoritesSlotsHaveCorrectBehavior() {
    Chromatica plugin = mock(Chromatica.class);
    Player player = mock(Player.class);
    when(player.hasPermission("chromatica.use")).thenReturn(true);
    InventoryClickEvent selectorClose =
        navigationClick(plugin, "ColorSelectorMenu", ColorSelectorMenu.CLOSE_SLOT, player);
    try (var colors = mockConstruction(ColorMenu.class);
        var gradients = mockConstruction(GradientMenu.class)) {
      new ColorSelectorListener(plugin).onInventoryClick(selectorClose);
      verify(player).closeInventory();
      org.junit.jupiter.api.Assertions.assertTrue(colors.constructed().isEmpty());
      org.junit.jupiter.api.Assertions.assertTrue(gradients.constructed().isEmpty());
    }
    clearInvocations(player);
    InventoryClickEvent favoritesClose = navigationClick(plugin, "FavoritesMenu", 21, player);
    try (var main = mockConstruction(ChromaticaMenu.class)) {
      new FavoritesMenuListener(plugin).onInventoryClick(favoritesClose);
      verify(player, never()).closeInventory();
      org.junit.jupiter.api.Assertions.assertTrue(main.constructed().isEmpty());
      clearInvocations(player);
      InventoryClickEvent favoritesBack =
          navigationClick(plugin, "FavoritesMenu", FavoritesMenu.BACK_SLOT, player);
      new FavoritesMenuListener(plugin).onInventoryClick(favoritesBack);
      verify(main.constructed().get(0)).open(player);
      verify(player, never()).closeInventory();
    }
  }
}
