package com.mrkoszos.chromatica.gui;

import com.mrkoszos.chromatica.Chromatica;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.WrittenBookContent;
import java.util.*;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Optional real-Paper smoke probe, packaged separately from test classes in an ignored directory.
 * Never included in the release JAR. Reflection is limited to inspecting vanilla tooltip output
 * in this test; production uses only Paper API.
 */
public final class GuiTooltipRuntimeProbe extends JavaPlugin {
  @Override public void onEnable() {
    try {
      var name = GuiUtils.text("Runtime GUI name");
      var lore = GuiUtils.text("Intentional lore");
      for (Material material : List.of(Material.BUNDLE, Material.WRITTEN_BOOK,
          Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE, Material.MACE,
          Material.PRIZE_POTTERY_SHERD)) {
        ItemStack item = GuiUtils.item(material, name, lore);
        if (material == Material.WRITTEN_BOOK) {
          item.setData(DataComponentTypes.WRITTEN_BOOK_CONTENT,
              WrittenBookContent.writtenBookContent("Vanilla title", "Vanilla author").build());
          GuiTooltips.clean(item);
        }
        GuiUtils.selected(item, true);
        check(Boolean.TRUE.equals(item.getData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE)),
            "effective glint before refresh " + material);
        GuiTooltips.clean(item); // Must remain safe and preserve metadata on repeated refreshes.
        check(name.equals(item.getItemMeta().displayName()), "custom name " + material);
        check(List.of(lore).equals(item.getItemMeta().lore()), "custom lore " + material);
        check(Boolean.TRUE.equals(item.getData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE)),
            "effective glint " + material);
        var display = item.getData(DataComponentTypes.TOOLTIP_DISPLAY);
        check(display != null && !display.hideTooltip(), "visible tooltip " + material);
        check(display.hiddenComponents().contains(DataComponentTypes.BUNDLE_CONTENTS), "bundle policy");
        check(display.hiddenComponents().contains(DataComponentTypes.WRITTEN_BOOK_CONTENT), "book policy");
        check(!display.hiddenComponents().contains(DataComponentTypes.CUSTOM_NAME), "visible name policy");
        check(!display.hiddenComponents().contains(DataComponentTypes.LORE), "visible lore policy");
        if (material == Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE) {
          check(item.getType() == Material.PAPER, "neutral smithing carrier");
          check(Key.key("minecraft:netherite_upgrade_smithing_template")
              .equals(item.getData(DataComponentTypes.ITEM_MODEL)), "vanilla template model");
        } else check(item.getType() == material, "unchanged material " + material);
        List<String> lines = tooltipLines(item);
        check(lines.equals(List.of("Runtime GUI name", "Intentional lore")),
            "only intentional tooltip text for " + material + ": " + lines);
        if (material == Material.BUNDLE) {
          Object stack = nmsCopy(item);
          check(((Optional<?>) stack.getClass().getMethod("getTooltipImage").invoke(stack)).isEmpty(),
              "bundle contents image hidden");
        }
      }
      Chromatica chromatica = (Chromatica) getServer().getPluginManager().getPlugin("Chromatica");
      ItemStack globe = GuiHeads.globeHead(chromatica);
      var profile = ((SkullMeta) globe.getItemMeta()).getPlayerProfile();
      GuiUtils.selected(globe, true);
      GuiTooltips.clean(globe);
      check(profile.equals(((SkullMeta) globe.getItemMeta()).getPlayerProfile()), "head profile preserved");
      check(((SkullMeta) globe.getItemMeta()).getPlayerProfile().getProperties().stream()
          .anyMatch(p -> p.getName().equals("textures") && p.getValue().equals(GuiHeads.GLOBE_TEXTURE)),
          "exact globe texture preserved");
      check(Boolean.TRUE.equals(globe.getItemMeta().getEnchantmentGlintOverride()), "head glint preserved");
      getLogger().info("GUI_TOOLTIP_RUNTIME_PROBE_SUCCESS: vanilla tooltip text, models, names, lore, glint and globe profile verified.");
    } catch (Exception exception) {
      throw new IllegalStateException("GUI_TOOLTIP_RUNTIME_PROBE_FAILED", exception);
    }
  }

  private static void check(boolean value, String label) {
    if (!value) throw new IllegalStateException(label);
  }

  private static List<String> tooltipLines(ItemStack item) throws Exception {
    Object stack = nmsCopy(item);
    Class<?> context = Class.forName("net.minecraft.world.item.Item$TooltipContext");
    Class<?> flag = Class.forName("net.minecraft.world.item.TooltipFlag");
    List<?> lines = (List<?>) stack.getClass().getMethod("getTooltipLines", context,
        Class.forName("net.minecraft.world.entity.player.Player"), flag)
        .invoke(stack, context.getField("EMPTY").get(null), null, flag.getField("NORMAL").get(null));
    var getString = Class.forName("net.minecraft.network.chat.Component").getMethod("getString");
    List<String> result = new ArrayList<>();
    for (Object line : lines) result.add((String) getString.invoke(line));
    return result;
  }

  private static Object nmsCopy(ItemStack item) throws Exception {
    return Class.forName("org.bukkit.craftbukkit.inventory.CraftItemStack")
        .getMethod("asNMSCopy", ItemStack.class).invoke(null, item);
  }
}
