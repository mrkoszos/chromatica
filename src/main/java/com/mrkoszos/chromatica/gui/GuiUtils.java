package com.mrkoszos.chromatica.gui;

import com.mrkoszos.chromatica.Chromatica;
import java.util.Arrays;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.*;

public final class GuiUtils {
  private GuiUtils() {}

  public static boolean allowed(Chromatica plugin, org.bukkit.entity.Player player) {
    if (player.hasPermission("chromatica.use")) return true;
    GuiSounds.play(plugin, player, GuiSounds.Event.ERROR);
    player.sendActionBar(text("You don't have permission to do that.", NamedTextColor.RED));
    return false;
  }

  public static boolean persist(
      Chromatica plugin,
      org.bukkit.entity.Player player,
      com.mrkoszos.chromatica.model.NameStyle style) {
    if (!allowed(plugin, player)) return false;
    if (style.isScramble() && !player.hasPermission("chromatica.format.scramble")) {
      GuiSounds.play(plugin, player, GuiSounds.Event.ERROR);
      player.sendActionBar(
          text("This style requires chromatica.format.scramble.", NamedTextColor.RED));
      return false;
    }
    if (plugin.getNameStyleManager().setStyle(player.getUniqueId(), style)) return true;
    storageError(plugin, player);
    return false;
  }

  public static void storageError(Chromatica plugin, org.bukkit.entity.Player player) {
    GuiSounds.play(plugin, player, GuiSounds.Event.ERROR);
    player.sendActionBar(
        text("Could not save your style. Please contact an administrator.", NamedTextColor.RED));
  }

  public static Component text(String text) {
    return text(text, NamedTextColor.WHITE);
  }

  public static Component text(String text, NamedTextColor color) {
    return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
  }

  public static ItemStack item(Material material, Component name, Component... lore) {
    ItemStack item = new ItemStack(material);
    var meta = item.getItemMeta();
    meta.displayName(name.decoration(TextDecoration.ITALIC, false));
    meta.lore(
        Arrays.stream(lore).map(line -> line.decoration(TextDecoration.ITALIC, false)).toList());
    item.setItemMeta(meta);
    return GuiTooltips.clean(item);
  }

  public static ItemStack selected(ItemStack item, boolean selected) {
    var meta = item.getItemMeta();
    meta.setEnchantmentGlintOverride(selected);
    item.setItemMeta(meta);
    return item;
  }

  public static void mark(Inventory inventory, int slot, boolean selected) {
    var item = inventory.getItem(slot);
    if (item != null) inventory.setItem(slot, selected(item, selected));
  }

  public static void navigate(Chromatica plugin, org.bukkit.entity.Player player, Runnable action) {
    GuiSounds.play(plugin, player, GuiSounds.Event.NAVIGATE);
    plugin
        .getServer()
        .getScheduler()
        .runTask(
            plugin,
            () -> {
              if (player.isOnline() && allowed(plugin, player)) action.run();
            });
  }

  public static Inventory inventory(Chromatica plugin, int size, Component title, String menu) {
    MenuHolder holder = new MenuHolder(menu);
    holder.title = title;
    holder.inventory = plugin.getServer().createInventory(holder, size, title);
    return holder.inventory;
  }

  public static void open(org.bukkit.entity.Player player, Inventory inventory) {
    if (!player.isOnline()) return;
    cleanTooltips(inventory);
    Inventory current = player.getOpenInventory().getTopInventory();
    if (inventory.getHolder() instanceof MenuHolder target
        && isMenu(current, target.menu)
        && current.getSize() == inventory.getSize()) {
      InlineConfirmation.cancel(player);
      current.setContents(inventory.getContents());
      MenuHolder currentHolder = (MenuHolder) current.getHolder();
      if (!java.util.Objects.equals(currentHolder.title, target.title)) {
        player
            .getOpenInventory()
            .setTitle(
                net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                    .serialize(target.title));
        currentHolder.title = target.title;
      }
      currentHolder.page = target.page;
      currentHolder.entries = target.entries;
      target.inventory = current;
    } else {
      player.openInventory(inventory);
    }
  }

  public static boolean isMenu(Inventory inventory, String menu) {
    return inventory.getHolder() instanceof MenuHolder holder && holder.menu.equals(menu);
  }

  public static boolean isActionClick(InventoryClickEvent event) {
    return event.getRawSlot() >= 0
        && event.getRawSlot() < event.getView().getTopInventory().getSize()
        && (event.getClick() == ClickType.LEFT || event.getClick() == ClickType.RIGHT);
  }

  public static void fillEmptySlots(Inventory inventory, Chromatica plugin) {
    plugin.getBackgroundManager().applyBackground(inventory);
    cleanTooltips(inventory);
  }

  public static void fillBorderRowsOnly(Inventory inventory, Chromatica plugin) {
    plugin.getBackgroundManager().applyBorderOnly(inventory);
    cleanTooltips(inventory);
  }

  private static void cleanTooltips(Inventory inventory) {
    for (int slot = 0; slot < inventory.getSize(); slot++) {
      ItemStack item = inventory.getItem(slot);
      // Background panes intentionally have no tooltip at all.
      if (item != null && !item.getItemMeta().isHideTooltip())
        inventory.setItem(slot, GuiTooltips.clean(item));
    }
  }

  public static ItemStack closeItem() {
    return item(Material.STRUCTURE_VOID, text("Close", NamedTextColor.RED));
  }

  public static final class MenuHolder implements InventoryHolder {
    final String menu;
    Inventory inventory;
    private Component title;
    int page;
    java.util.List<String> entries = java.util.List.of();

    private MenuHolder(String menu) {
      this.menu = menu;
    }

    @Override
    public Inventory getInventory() {
      return inventory;
    }
  }
}
