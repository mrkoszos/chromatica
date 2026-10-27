package com.mrkoszos.chromatica.gui;

import static org.mockito.Mockito.*;
import com.mrkoszos.chromatica.Chromatica;
import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Server;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.*;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.Test;

class GuiHeadsTest {
  @Test void completedSkinTargetsDisplayedInventoryAfterSameMenuCopy() throws Exception {
    skinTransfer(false);
  }

  @Test void completedSkinCannotUpdateClosedInventory() throws Exception {
    skinTransfer(true);
  }

  private void skinTransfer(boolean closed) throws Exception {
    Chromatica plugin = mock(Chromatica.class); Server server = mock(Server.class);
    when(plugin.getServer()).thenReturn(server); when(plugin.isEnabled()).thenReturn(true);
    Inventory temporary = mock(Inventory.class), displayed = mock(Inventory.class);
    when(server.createInventory(any(InventoryHolder.class), eq(45), any(net.kyori.adventure.text.Component.class)))
        .thenAnswer(call -> { when(temporary.getHolder()).thenReturn(call.getArgument(0)); return temporary; });
    GuiUtils.inventory(plugin, 45, net.kyori.adventure.text.Component.text("title"), "CommunityPresetMenu");
    var target = (GuiUtils.MenuHolder) temporary.getHolder(); target.entries = List.of("preset");
    var current = mock(GuiUtils.MenuHolder.class); current.entries = List.of("old");
    // Give current a real menu identity through a separate inventory factory call.
    when(server.createInventory(any(InventoryHolder.class), eq(45), any(net.kyori.adventure.text.Component.class)))
        .thenAnswer(call -> { when(displayed.getHolder()).thenReturn(call.getArgument(0)); return displayed; });
    GuiUtils.inventory(plugin, 45, net.kyori.adventure.text.Component.text("title"), "CommunityPresetMenu");
    org.bukkit.entity.Player viewer = mock(org.bukkit.entity.Player.class); when(viewer.isOnline()).thenReturn(true);
    var view = mock(org.bukkit.inventory.InventoryView.class); when(view.getTopInventory()).thenReturn(displayed);
    when(viewer.getOpenInventory()).thenReturn(view);
    when(displayed.getViewers()).thenReturn(closed ? List.of() : List.of(viewer));
    ItemStack original = mock(ItemStack.class); SkullMeta meta = mock(SkullMeta.class);
    when(original.getItemMeta()).thenReturn(meta);
    when(temporary.getItem(10)).thenReturn(original); when(displayed.getItem(10)).thenReturn(original);
    when(temporary.getContents()).thenReturn(new ItemStack[45]);
    UUID creator = UUID.randomUUID(); PlayerProfile request = mock(PlayerProfile.class);
    when(server.createPlayerProfile(creator)).thenReturn(request);
    CompletableFuture<PlayerProfile> pending = new CompletableFuture<>(); doReturn(pending).when(request).update();
    BukkitScheduler scheduler = mock(BukkitScheduler.class); when(server.getScheduler()).thenReturn(scheduler);
    when(scheduler.runTask(eq(plugin), any(Runnable.class))).thenAnswer(call -> { ((Runnable) call.getArgument(1)).run(); return null; });
    GuiHeads.creatorHead(plugin, creator, temporary, 10);
    GuiUtils.open(viewer, temporary);
    PlayerProfile resolved = mock(PlayerProfile.class); PlayerTextures textures = mock(PlayerTextures.class);
    when(resolved.getTextures()).thenReturn(textures);
    when(textures.getSkin()).thenReturn(URI.create("https://textures.minecraft.net/texture/test").toURL());
    pending.complete(resolved);
    verify(temporary, never()).setItem(anyInt(), any());
    if (closed) { verify(displayed, never()).setItem(anyInt(), any()); verify(meta, never()).setOwnerProfile(any()); }
    else { verify(displayed).setItem(10, original); verify(meta).setOwnerProfile(resolved); }
  }

  @Test void unresolvedProfilesDoNotBlockAndFailuresLeaveFallbackUntouched() {
    Chromatica plugin = mock(Chromatica.class);
    Server server = mock(Server.class);
    when(plugin.getServer()).thenReturn(server);
    UUID creator = UUID.randomUUID();
    PlayerProfile requested = mock(PlayerProfile.class);
    when(server.createPlayerProfile(creator)).thenReturn(requested);
    CompletableFuture<PlayerProfile> pending = new CompletableFuture<>();
    doReturn(pending).when(requested).update();
    Inventory inventory = mock(Inventory.class);
    GuiUtils.MenuHolder holder = mock(GuiUtils.MenuHolder.class);
    holder.entries = List.of("preset");
    when(inventory.getHolder()).thenReturn(holder);
    ItemStack fallback = mock(ItemStack.class);
    when(inventory.getItem(10)).thenReturn(fallback);
    GuiHeads.creatorHead(plugin, creator, inventory, 10);
    GuiHeads.creatorHead(plugin, creator, inventory, 10);
    verify(requested, times(1)).update();
    verify(inventory, never()).setItem(anyInt(), any());
    pending.completeExceptionally(new IllegalStateException("unavailable"));
    verify(inventory, never()).setItem(anyInt(), any());
    verify(server, never()).getScheduler();
  }

  @Test void lateSkinDoesNotOverwriteARefreshedPage() throws Exception {
    Chromatica plugin = mock(Chromatica.class);
    Server server = mock(Server.class);
    when(plugin.getServer()).thenReturn(server);
    when(plugin.isEnabled()).thenReturn(true);
    UUID creator = UUID.randomUUID();
    PlayerProfile requested = mock(PlayerProfile.class);
    when(server.createPlayerProfile(creator)).thenReturn(requested);
    CompletableFuture<PlayerProfile> pending = new CompletableFuture<>();
    doReturn(pending).when(requested).update();
    Inventory inventory = mock(Inventory.class);
    GuiUtils.MenuHolder holder = mock(GuiUtils.MenuHolder.class);
    holder.entries = List.of("old");
    when(inventory.getHolder()).thenReturn(holder);
    ItemStack fallback = mock(ItemStack.class);
    when(inventory.getItem(10)).thenReturn(fallback);
    BukkitScheduler scheduler = mock(BukkitScheduler.class);
    when(server.getScheduler()).thenReturn(scheduler);
    when(scheduler.runTask(eq(plugin), any(Runnable.class))).thenAnswer(call -> {
      ((Runnable) call.getArgument(1)).run(); return null;
    });
    GuiHeads.creatorHead(plugin, creator, inventory, 10);
    holder.entries = List.of("new");
    PlayerProfile resolved = mock(PlayerProfile.class);
    PlayerTextures textures = mock(PlayerTextures.class);
    when(resolved.getTextures()).thenReturn(textures);
    when(textures.getSkin()).thenReturn(URI.create("https://textures.minecraft.net/texture/test").toURL());
    pending.complete(resolved);
    verify(inventory, never()).setItem(anyInt(), any());
  }
}
