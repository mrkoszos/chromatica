package com.mrkoszos.chromatica.gui;

import com.mrkoszos.chromatica.Chromatica;
import com.mrkoszos.chromatica.manager.CommunityPresetManager;
import com.mrkoszos.chromatica.model.NameStyle;
import java.util.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;

/** A shared 3-by-7 grid keeps both preset categories visually consistent. */
public final class PresetBrowserMenu {
  public static final Component SERVER_TITLE = Component.text("Server Presets");
  public static final Component COMMUNITY_TITLE = Component.text("Community Presets");
  public static final int[] PRESET_SLOTS = {
    10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34
  };
  public static final int PAGE_SIZE = 21;
  public static final int RANDOM_SLOT = 39, PUBLISH_SLOT = 39,
      PAGE_SLOT = 40, BACK_SLOT = 41;
  private final Chromatica plugin;
  private final boolean community;

  public PresetBrowserMenu(Chromatica plugin, boolean community) {
    this.plugin = plugin;
    this.community = community;
  }

  public String identity() {
    return community ? "CommunityPresetMenu" : "ServerPresetMenu";
  }

  public static int pageCount(int size) {
    return Math.max(1, (size + PAGE_SIZE - 1) / PAGE_SIZE);
  }

  public static int clampPage(int page, int size) {
    return Math.max(0, Math.min(page, pageCount(size) - 1));
  }

  public void open(Player player, int page) {
    if (!player.isOnline() || !player.hasPermission("chromatica.use")) return;
    if (community && !plugin.isCommunityPresetsEnabled()) {
      new PresetMenu(plugin).open(player);
      return;
    }
    Inventory current = player.getOpenInventory().getTopInventory();
    Inventory inventory =
        GuiUtils.isMenu(current, identity()) && current.getSize() == 45
            ? current
            : GuiUtils.inventory(
                plugin, 45, community ? COMMUNITY_TITLE : SERVER_TITLE, identity());
    refresh(player, inventory, page);
    if (inventory != current) GuiUtils.open(player, inventory);
  }

  public void refresh(Player player, Inventory inventory, int requestedPage) {
    InlineConfirmation.cancel(player);
    inventory.clear();
    GuiUtils.MenuHolder holder = (GuiUtils.MenuHolder) inventory.getHolder();
    List<String> ids =
        community
            ? plugin.getCommunityPresetManager().getAll().stream()
                .map(preset -> preset.id().toString())
                .toList()
            : new ArrayList<>(plugin.getPresetManager().getAll().keySet());
    var active = plugin.getNameStyleManager().getStyle(player.getUniqueId());
    Map<String, NameStyle> candidates = new LinkedHashMap<>();
    for (String id : ids) {
      candidates.put(
          id,
          community
              ? plugin.getCommunityPresetManager().get(UUID.fromString(id)).style()
              : GuiSelection.colors(plugin.getPresetManager().get(id).colors()));
    }
    String selected =
        GuiSelection.selected(
            plugin,
            player.getUniqueId(),
            community ? "community" : "server",
            active,
            candidates,
            !community);
    int page = clampPage(requestedPage, ids.size());
    int offset = page * PAGE_SIZE;
    List<String> visible =
        ids.subList(Math.min(offset, ids.size()), Math.min(offset + PAGE_SIZE, ids.size()));
    holder.page = page;
    holder.entries = List.copyOf(visible);
    for (int i = 0; i < visible.size(); i++) {
      String id = visible.get(i);
      if (community) {
        var preset = plugin.getCommunityPresetManager().get(UUID.fromString(id));
        List<Component> lore = new ArrayList<>();
        lore.add(GuiUtils.text("by " + preset.username(), NamedTextColor.GRAY));
        if (CommunityPresetManager.canDelete(
            preset, player.getUniqueId(), player.hasPermission("chromatica.community.admin"))) {
          lore.add(GuiUtils.text("Drop: delete", NamedTextColor.RED));
        }
        inventory.setItem(
            PRESET_SLOTS[i],
            previewItem(
                Material.PLAYER_HEAD,
                preset.name(),
                permittedPreview(player, preset.style()),
                lore));
        GuiUtils.mark(inventory, PRESET_SLOTS[i], id.equals(selected));
        GuiHeads.creatorHead(plugin, preset.creator(), inventory, PRESET_SLOTS[i]);
      } else {
        var preset = plugin.getPresetManager().get(id);
        List<Component> lore = new ArrayList<>();
        for (String hex : preset.colors()) {
          var color = net.kyori.adventure.text.format.TextColor.fromHexString(hex);
          lore.add(
              Component.text(hex, color == null ? NamedTextColor.WHITE : color)
                  .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
        }
        inventory.setItem(
            PRESET_SLOTS[i],
            previewItem(
                preset.material(),
                serverPreview(preset.display(), preset.colors())
                    .decoration(
                        net.kyori.adventure.text.format.TextDecoration.OBFUSCATED,
                        Boolean.TRUE.equals(preset.scramble())
                            && player.hasPermission("chromatica.format.scramble")),
                lore));
        GuiUtils.mark(inventory, PRESET_SLOTS[i], id.equals(selected));
      }
    }
    if (community)
      inventory.setItem(
          PUBLISH_SLOT,
          GuiUtils.item(
              Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE,
              GuiUtils.text("Publish Preset", NamedTextColor.GREEN)));
    else inventory.setItem(RANDOM_SLOT,
        GuiUtils.item(Material.BUNDLE, GuiUtils.text("Random Preset", NamedTextColor.YELLOW)));
    inventory.setItem(
        PAGE_SLOT,
        GuiUtils.item(
            Material.PAPER,
            GuiUtils.text(
                "Page " + (page + 1) + "/" + pageCount(ids.size()), NamedTextColor.GRAY),
            GuiUtils.text("Left-click: previous", NamedTextColor.GRAY),
            GuiUtils.text("Right-click: next", NamedTextColor.GRAY)));
    inventory.setItem(
        BACK_SLOT, GuiUtils.item(Material.BARRIER, GuiUtils.text("Back", NamedTextColor.RED)));
    GuiUtils.fillEmptySlots(inventory, plugin);
  }

  private NameStyle permittedPreview(Player player, NameStyle style) {
    NameStyle copy = style.copy();
    copy.setScramble(style.isScramble() && player.hasPermission("chromatica.format.scramble"));
    return copy;
  }

  private ItemStack previewItem(
      Material material, String text, NameStyle style, List<Component> lore) {
    return previewItem(material, plugin.getNameRenderer().render(text, style), lore);
  }

  private ItemStack previewItem(Material material, Component preview, List<Component> lore) {
    ItemStack item = new ItemStack(material);
    var meta = item.getItemMeta();
    // Preserve intentional formatting in the saved-style preview, including italic.
    meta.displayName(preview);
    meta.lore(lore);
    item.setItemMeta(meta);
    return item;
  }

  /** Preserve the existing server-preset preview interpolation and invalid-color fallback. */
  private Component serverPreview(String text, List<String> colors) {
    var stops =
        colors.stream()
            .map(
                hex -> {
                  var color = net.kyori.adventure.text.format.TextColor.fromHexString(hex);
                  return color == null ? NamedTextColor.WHITE : color;
                })
            .toList();
    if (stops.size() == 1)
      return Component.text(text, stops.getFirst())
          .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false);
    Component result =
        Component.empty().decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false);
    for (int i = 0; i < text.length(); i++) {
      double scaled =
          (text.length() == 1 ? 0 : (double) i / (text.length() - 1)) * (stops.size() - 1);
      int segment = Math.min((int) scaled, stops.size() - 2);
      double progress = scaled - segment;
      var start = stops.get(segment);
      var finish = stops.get(segment + 1);
      var color =
          net.kyori.adventure.text.format.TextColor.color(
              (int) (start.red() + (finish.red() - start.red()) * progress),
              (int) (start.green() + (finish.green() - start.green()) * progress),
              (int) (start.blue() + (finish.blue() - start.blue()) * progress));
      result = result.append(Component.text(String.valueOf(text.charAt(i)), color));
    }
    return result;
  }
}
