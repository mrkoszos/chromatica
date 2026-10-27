package com.mrkoszos.chromatica.gui;

import com.mrkoszos.chromatica.Chromatica;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

/** Entry point for server-defined and player-published presets. */
public class PresetMenu {
  public static final Component TITLE = Component.text("Presets");
  public static final int SERVER_SLOT = 10;
  public static final int PREMIUM_SLOT = 11;
  public static final int SEASONAL_SLOT = 15;
  public static final int COMMUNITY_SLOT = 16;
  public static final int BACK_SLOT = 22;
  private final Chromatica plugin;

  public PresetMenu(Chromatica plugin) {
    this.plugin = plugin;
  }

  public void open(Player player) {
    Inventory inventory = GuiUtils.inventory(plugin, 27, TITLE, "PresetMenu");
    inventory.setItem(
        SERVER_SLOT,
        GuiUtils.item(
            Material.STRUCTURE_BLOCK,
            GuiUtils.text("Server Presets", NamedTextColor.YELLOW)));
    inventory.setItem(PREMIUM_SLOT, GuiUtils.item(Material.PRIZE_POTTERY_SHERD,
        GuiUtils.text("Premium Presets", NamedTextColor.YELLOW)));
    inventory.setItem(SEASONAL_SLOT, GuiUtils.item(Material.CLOCK,
        GuiUtils.text("Seasonal Presets", NamedTextColor.YELLOW)));
    inventory.setItem(COMMUNITY_SLOT, GuiHeads.globeHead(plugin));
    var style = plugin.getNameStyleManager().getStyle(player.getUniqueId());
    var head = GuiHeads.playerHead(player, Material.ENDER_EYE);
    var meta = head.getItemMeta();
    meta.displayName(plugin.getNameRenderer().render(player.getName(), style,
        player.hasPermission("chromatica.format.scramble")));
    head.setItemMeta(meta);
    inventory.setItem(4, head);
    inventory.setItem(
        BACK_SLOT, GuiUtils.item(Material.BARRIER, GuiUtils.text("Back", NamedTextColor.RED)));
    GuiUtils.fillEmptySlots(inventory, plugin);
    GuiUtils.open(player, inventory);
  }
}
