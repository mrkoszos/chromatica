package com.mrkoszos.chromatica.gui;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.mrkoszos.chromatica.Chromatica;
import com.mrkoszos.chromatica.manager.NameStyleManager;
import com.mrkoszos.chromatica.model.NameStyle;
import java.util.*;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.*;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ReleaseSafetyTest {
  record Fixture(
      Chromatica plugin,
      Player player,
      Inventory inventory,
      InventoryView view,
      InventoryClickEvent event) {}

  Fixture fixture(String name, int slot) {
    Chromatica plugin = mock(Chromatica.class);
    Server server = mock(Server.class);
    when(plugin.getServer()).thenReturn(server);
    Inventory inventory = mock(Inventory.class);
    when(inventory.getSize()).thenReturn(54);
    when(server.createInventory(any(InventoryHolder.class), eq(54), any(Component.class)))
        .thenAnswer(
            call -> {
              when(inventory.getHolder()).thenReturn(call.getArgument(0));
              return inventory;
            });
    GuiUtils.inventory(plugin, 54, Component.text("Unrelated title"), name);
    Player player = mock(Player.class);
    when(player.getUniqueId()).thenReturn(UUID.randomUUID());
    InventoryView view = mock(InventoryView.class);
    when(view.getTopInventory()).thenReturn(inventory);
    when(player.getOpenInventory()).thenReturn(view);
    InventoryClickEvent event = mock(InventoryClickEvent.class);
    when(event.getView()).thenReturn(view);
    when(event.getWhoClicked()).thenReturn(player);
    when(event.getRawSlot()).thenReturn(slot);
    when(event.getClick()).thenReturn(ClickType.LEFT);
    when(event.getCurrentItem()).thenReturn(mock(ItemStack.class));
    return new Fixture(plugin, player, inventory, view, event);
  }

  @Test
  void revokedPermissionBlocksEveryChestAction() {
    String[] menus = {
      "ChromaticaMenu",
      "ColorMenu",
      "ColorSelectorMenu",
      "GradientMenu",
      "FormattingMenu",
      "FavoritesMenu",
      "PresetMenu",
      "ServerPresetMenu",
      "CommunityPresetMenu"
    };
    int[] slots = {31, 10, 10, 9, 11, 11, 11, 10, 40};
    for (int i = 0; i < menus.length; i++) {
      Fixture f = fixture(menus[i], slots[i]);
      List<Consumer<InventoryClickEvent>> listeners =
          List.of(
              new ChromaticaMenuListener(f.plugin)::onInventoryClick,
              new ColorMenuListener(f.plugin)::onInventoryClick,
              new ColorSelectorListener(f.plugin)::onInventoryClick,
              new GradientMenuListener(f.plugin)::onInventoryClick,
              new FormattingMenuListener(f.plugin)::onInventoryClick,
              new FavoritesMenuListener(f.plugin)::onInventoryClick,
              new PresetMenuListener(f.plugin)::onInventoryClick,
              new PresetBrowserListener(f.plugin)::onClick,
              new PresetBrowserListener(f.plugin)::onClick);
      listeners.get(i).accept(f.event);
      verify(f.event).setCancelled(true);
      verify(f.plugin, never()).getNameStyleManager();
      verify(f.plugin, never()).getFavoriteManager();
      verify(f.plugin, never()).getCommunityPresetManager();
    }
  }

  @Test
  void failedSaveNeverConfirmsOrChangesSelection() {
    Fixture f = fixture("ColorMenu", 10);
    when(f.player.hasPermission("chromatica.use")).thenReturn(true);
    NameStyleManager styles = mock(NameStyleManager.class);
    when(f.plugin.getNameStyleManager()).thenReturn(styles);
    when(styles.getStyle(f.player.getUniqueId())).thenReturn(new NameStyle());
    GuiSounds sounds = mock(GuiSounds.class);
    when(f.plugin.getGuiSounds()).thenReturn(sounds);
    try (var menus = mockConstruction(ColorMenu.class)) {
      new ColorMenuListener(f.plugin).onInventoryClick(f.event);
      verify(styles).setStyle(eq(f.player.getUniqueId()), any());
      verify(sounds).play(f.player, GuiSounds.Event.ERROR);
      verify(sounds, never()).play(f.player, GuiSounds.Event.CONFIRM);
    }
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  @Test
  void creditUsesExistingDecorationAndOneVisibleGrayLoreLine() {
    Fixture f = fixture("ChromaticaMenu", 0);
    GuiBranding branding = new GuiBranding();
    when(f.plugin.getGuiBranding()).thenReturn(branding);
    ItemStack pane = mock(ItemStack.class);
    ItemMeta meta = mock(ItemMeta.class);
    when(f.inventory.getItem(ChromaticaMenu.CREDIT_SLOT)).thenReturn(pane);
    when(pane.getItemMeta()).thenReturn(meta);
    new ChromaticaMenu(f.plugin).applyCredit(f.inventory);
    verify(meta).setHideTooltip(false);
    ArgumentCaptor<List<Component>> lore = ArgumentCaptor.forClass(List.class);
    verify(meta).lore(lore.capture());
    assertEquals(
        List.of(GuiUtils.text("Powered by Chromatica", NamedTextColor.GRAY)), lore.getValue());
    assertEquals(
        TextDecoration.State.FALSE, lore.getValue().getFirst().decoration(TextDecoration.ITALIC));
    verify(meta, never()).displayName(any());
    verify(pane, never()).setType(any());
    verify(f.inventory).setItem(ChromaticaMenu.CREDIT_SLOT, pane);
  }

  @Test
  void disabledCreditLeavesDecorationUntouchedAndCannotSendCredit() {
    Fixture f = fixture("ChromaticaMenu", 0);
    when(f.player.hasPermission("chromatica.use")).thenReturn(true);
    GuiBranding branding = new GuiBranding();
    YamlConfiguration config = new YamlConfiguration();
    config.set("branding.show-credit", false);
    branding.reload(config);
    when(f.plugin.getGuiBranding()).thenReturn(branding);
    new ChromaticaMenu(f.plugin).applyCredit(f.inventory);
    verify(f.inventory, never()).getItem(anyInt());
    verify(f.inventory, never()).setItem(anyInt(), any());
    new ChromaticaMenuListener(f.plugin).onInventoryClick(f.event);
    verify(f.player, never()).sendMessage(any(Component.class));
    verify(f.plugin, never()).getNameStyleManager();
  }

  @Test
  void creditClickIsPrivateAndUsesMetadataVersion() {
    Fixture f = fixture("ChromaticaMenu", 0);
    when(f.player.hasPermission("chromatica.use")).thenReturn(true);
    io.papermc.paper.plugin.configuration.PluginMeta meta =
        mock(io.papermc.paper.plugin.configuration.PluginMeta.class);
    when(f.plugin.getPluginMeta()).thenReturn(meta);
    when(meta.getVersion()).thenReturn("1.3");
    new ChromaticaMenuListener(f.plugin).onInventoryClick(f.event);
    verify(f.player).sendMessage(GuiUtils.text("Chromatica 1.3 by mk", NamedTextColor.GRAY));
    verify(f.plugin, never()).getNameStyleManager();
    verify(f.plugin.getServer(), never()).broadcast(any(Component.class));
  }

  @Test
  void anvilRejectsAlternativeClicksAndProtectsCursorAndDrag() {
    Fixture f = fixture("unused", 2);
    GuiAnvils guard = new GuiAnvils();
    guard.track(f.player, f.inventory);
    for (ClickType click : ClickType.values()) {
      when(f.event.getClick()).thenReturn(click);
      guard.onClick(f.event);
      assertEquals(
          click == ClickType.LEFT || click == ClickType.RIGHT,
          guard.consumeSubmission(f.player, 2));
      assertFalse(guard.consumeSubmission(f.player, 2));
    }
    when(f.event.getClick()).thenReturn(ClickType.LEFT);
    ItemStack cursor = mock(ItemStack.class);
    when(cursor.getType()).thenReturn(Material.DIAMOND);
    when(f.event.getCursor()).thenReturn(cursor);
    guard.onClick(f.event);
    assertFalse(guard.consumeSubmission(f.player, 2));
    verify(f.event, never()).setCursor(any());
    InventoryDragEvent drag = mock(InventoryDragEvent.class);
    when(drag.getView()).thenReturn(f.view);
    guard.onDrag(drag);
    verify(drag).setCancelled(true);
    InventoryCloseEvent close = mock(InventoryCloseEvent.class);
    when(close.getInventory()).thenReturn(f.inventory);
    guard.onClose(close);
    guard.onClick(f.event);
    assertFalse(guard.consumeSubmission(f.player, 2));
  }

  @Test
  void nearestDyeUsesDistanceForCustomHexAndStableFallback() {
    assertEquals(Material.RED_DYE, DyeColors.nearest("#b12e26"));
    assertEquals(Material.BLUE_DYE, DyeColors.nearest("#3c44ab"));
    assertEquals(Material.GREEN_DYE, DyeColors.nearest("#5e7c17"));
    assertEquals(Material.BLACK_DYE, DyeColors.nearest("#000000"));
    assertEquals(Material.WHITE_DYE, DyeColors.nearest("invalid"));
    assertEquals(DyeColors.nearest("#123456"), DyeColors.nearest("#123456"));
  }

  @Test
  void revokedAdministrativePermissionsBlockCommands() {
    Chromatica plugin = mock(Chromatica.class);
    org.bukkit.command.CommandSender sender = mock(org.bukkit.command.CommandSender.class);
    var executor = new com.mrkoszos.chromatica.command.ChromaticaCommand(plugin);
    for (String action : List.of("reload", "reset", "set"))
      assertTrue(
          executor.onCommand(
              sender,
              mock(org.bukkit.command.Command.class),
              "chromatica",
              new String[] {action, "Tester", "saved"}));
    verify(plugin, never()).getNameStyleManager();
    verify(plugin, never()).getPresetManager();
    verify(plugin, never()).getCommunityPresetManager();
  }

  @Test
  void resetRequestNeverMutatesOrShowsSuccessBeforeConfirmation() {
    Fixture f = fixture("ChromaticaMenu", 31);
    when(f.player.hasPermission("chromatica.use")).thenReturn(true);
    NameStyleManager manager = mock(NameStyleManager.class);
    when(f.plugin.getNameStyleManager()).thenReturn(manager);
    GuiSounds sounds = mock(GuiSounds.class);
    when(f.plugin.getGuiSounds()).thenReturn(sounds);
    var actor = f.player.getUniqueId();
    try (var menus = mockConstruction(ChromaticaMenu.class);
        var confirmation = mockStatic(InlineConfirmation.class)) {
      new ChromaticaMenuListener(f.plugin).onInventoryClick(f.event);
      confirmation.verify(
          () ->
              InlineConfirmation.reset(
                  eq(f.plugin), eq(f.player), eq(actor), eq("chromatica.use"), eq(false)));
      verifyNoInteractions(manager);
      assertTrue(menus.constructed().isEmpty());
      verify(sounds, never()).play(f.player, GuiSounds.Event.CONFIRM);
    }
  }

  @Test
  void anvilCannotOpenOverAnOwnedCursorItem() {
    Player player = mock(Player.class);
    GuiAnvils guard = new GuiAnvils();
    assertTrue(guard.canOpen(player));
    ItemStack cursor = mock(ItemStack.class);
    when(cursor.getType()).thenReturn(Material.DIAMOND);
    when(player.getItemOnCursor()).thenReturn(cursor);
    assertFalse(guard.canOpen(player));
    verify(player, never()).setItemOnCursor(any());
  }
}
