package com.mrkoszos.chromatica.command;

import com.mrkoszos.chromatica.Chromatica;
import com.mrkoszos.chromatica.gui.ChromaticaMenu;
import com.mrkoszos.chromatica.gui.ColorMenu;
import com.mrkoszos.chromatica.gui.FavoritesMenu;
import com.mrkoszos.chromatica.gui.FormattingMenu;
import com.mrkoszos.chromatica.gui.GradientMenu;
import com.mrkoszos.chromatica.gui.GuiSelection;
import com.mrkoszos.chromatica.gui.GuiSounds;
import com.mrkoszos.chromatica.gui.PresetMenu;
import com.mrkoszos.chromatica.manager.PresetManager;
import com.mrkoszos.chromatica.model.NameStyle;
import com.mrkoszos.chromatica.model.StyleType;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ChromaticaCommand implements CommandExecutor, TabCompleter {

  private static final List<String> SUBCOMMANDS =
      List.of(
          "color",
          "gradient",
          "formatting",
          "presets",
          "favorites",
          "reload",
          "reset",
          "set",
          "clear");

  private final Chromatica plugin;

  public ChromaticaCommand(Chromatica plugin) {
    this.plugin = plugin;
  }

  @Override
  public boolean onCommand(
      @NotNull CommandSender sender,
      @NotNull Command command,
      @NotNull String label,
      @NotNull String[] args) {

    if (args.length == 0) {
      return openMenu(sender, "main");
    }

    switch (args[0].toLowerCase()) {
      case "color", "colors" -> {
        return openMenu(sender, "color");
      }

      case "gradient", "gradients" -> {
        return openMenu(sender, "gradient");
      }

      case "formatting", "format" -> {
        return openMenu(sender, "formatting");
      }

      case "presets", "preset" -> {
        return openMenu(sender, "presets");
      }

      case "favorites", "favourites", "favs" -> {
        return openMenu(sender, "favorites");
      }

      case "reload" -> {
        return handleReload(sender);
      }

      case "reset", "clear" -> {
        return handleReset(sender, args);
      }

      case "set" -> {
        return handleSet(sender, args);
      }

      default -> {
        return openMenu(sender, "main");
      }
    }
  }

  /*
   * =========================
   *          MENUS
   * =========================
   */

  private boolean openMenu(CommandSender sender, String menu) {

    if (!(sender instanceof Player player)) {
      message(sender, "Only players can use this command.");
      return true;
    }

    if (!player.hasPermission("chromatica.use")) {
      message(player, Component.text("You don't have permission to do that.", NamedTextColor.RED));
      return true;
    }

    String identity =
        switch (menu) {
          case "color" -> "ColorMenu";
          case "gradient" -> "GradientMenu";
          case "formatting" -> "FormattingMenu";
          case "presets" -> "PresetMenu";
          case "favorites" -> "FavoritesMenu";
          default -> "ChromaticaMenu";
        };
    if (!com.mrkoszos.chromatica.gui.GuiUtils.isMenu(
        player.getOpenInventory().getTopInventory(), identity))
      GuiSounds.play(plugin, player, GuiSounds.Event.NAVIGATE);
    switch (menu) {
      case "color" -> new ColorMenu(plugin).open(player);
      case "gradient" -> new GradientMenu(plugin).open(player);
      case "formatting" -> new FormattingMenu(plugin).open(player);
      case "presets" -> new PresetMenu(plugin).open(player);
      case "favorites" -> new FavoritesMenu(plugin).open(player);
      default -> new ChromaticaMenu(plugin).open(player);
    }

    return true;
  }

  /*
   * =========================
   *          RELOAD
   * =========================
   */

  private boolean handleReload(CommandSender sender) {

    if (!sender.hasPermission("chromatica.reload")) {
      message(sender, Component.text("You don't have permission to do that.", NamedTextColor.RED));
      return true;
    }

    boolean presetsReloaded = plugin.getPresetManager().reload();
    plugin.getBackgroundManager().reload();
    if (!plugin.getCommunityPresetManager().reload()
        || !presetsReloaded
        || !plugin.wasConfigReloadSuccessful()) {
      message(
          sender,
          Component.text(
              "Some settings or presets could not reload; previous invalid sections retained. Check"
                  + " logs.",
              NamedTextColor.RED));
      return true;
    }

    message(sender, Component.text("Presets & config reloaded.", NamedTextColor.GREEN));

    return true;
  }

  /*
   * =========================
   *           RESET
   * =========================
   */

  private boolean handleReset(CommandSender sender, String[] args) {

    if (!sender.hasPermission("chromatica.reset")) {
      message(sender, Component.text("You don't have permission to do that.", NamedTextColor.RED));
      return true;
    }

    if (args.length < 2 && sender instanceof Player player) {
      com.mrkoszos.chromatica.gui.InlineConfirmation.reset(
          plugin, player, player.getUniqueId(), "chromatica.reset", true);
      return true;
    }
    if (args.length < 2) {
      message(sender, Component.text("Usage: /chromatica reset <player>", NamedTextColor.RED));
      return true;
    }

    OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);

    if (!target.hasPlayedBefore() && !target.isOnline()) {
      message(sender, Component.text("Player not found.", NamedTextColor.RED));
      return true;
    }

    if (sender instanceof Player player) {
      com.mrkoszos.chromatica.gui.InlineConfirmation.reset(
          plugin, player, target.getUniqueId(), "chromatica.reset", true);
      return true;
    }

    if (!plugin.getNameStyleManager().resetStyle(target.getUniqueId())) {
      message(
          sender,
          Component.text("Could not reset player style. Check server logs.", NamedTextColor.RED));
      return true;
    }
    GuiSelection.clear(plugin, target.getUniqueId());

    message(
        sender,
        Component.text("Reset " + target.getName() + "'s name style.", NamedTextColor.GREEN));

    return true;
  }

  /*
   * =========================
   *            SET
   * =========================
   */

  private boolean handleSet(CommandSender sender, String[] args) {

    if (!sender.hasPermission("chromatica.set")) {
      message(sender, Component.text("You don't have permission to do that.", NamedTextColor.RED));
      return true;
    }

    if (args.length < 3) {
      message(
          sender, Component.text("Usage: /chromatica set <player> <preset>", NamedTextColor.RED));
      return true;
    }

    OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);

    if (!target.hasPlayedBefore() && !target.isOnline()) {
      message(sender, Component.text("Player not found.", NamedTextColor.RED));
      return true;
    }

    PresetManager.PresetDefinition preset = plugin.getPresetManager().get(args[2]);

    if (preset == null) {
      message(sender, Component.text("Unknown preset: " + args[2], NamedTextColor.RED));
      return true;
    }

    NameStyle style = plugin.getNameStyleManager().getStyle(target.getUniqueId());

    if (preset.colors().size() == 1) {
      style.setPrimaryColor(preset.colors().get(0));
      style.setType(StyleType.SOLID);
    } else {
      style.setGradientColors(new ArrayList<>(preset.colors()));
      style.setType(StyleType.GRADIENT);
    }

    style.setPresetId(preset.id());
    preset.applyFormatting(style);
    Player affected = target.getPlayer();
    if (style.isScramble()
        && (affected == null || !affected.hasPermission("chromatica.format.scramble"))) {
      message(
          sender,
          Component.text(
              "The target must be online and have chromatica.format.scramble to apply this style.",
              NamedTextColor.RED));
      return true;
    }
    if (!plugin.getNameStyleManager().setStyle(target.getUniqueId(), style)) {
      message(
          sender,
          Component.text("Could not save player style. Check server logs.", NamedTextColor.RED));
      return true;
    }
    GuiSelection.remember(plugin, target.getUniqueId(), "server", preset.id(), style);

    message(
        sender,
        Component.text(
            "Set " + target.getName() + "'s style to preset '" + preset.display() + "'.",
            NamedTextColor.GREEN));

    return true;
  }

  /*
   * =========================
   *      TAB COMPLETION
   * =========================
   */

  private void message(CommandSender sender, String text) {
    message(sender, Component.text(text));
  }

  private void message(CommandSender sender, Component text) {
    sender.sendMessage(
        plugin.getGuiBranding() == null ? text : plugin.getGuiBranding().message(text));
    if (sender instanceof Player player && NamedTextColor.RED.equals(text.color()))
      GuiSounds.play(plugin, player, GuiSounds.Event.ERROR);
  }

  @Override
  public @Nullable List<String> onTabComplete(
      @NotNull CommandSender sender,
      @NotNull Command command,
      @NotNull String alias,
      @NotNull String[] args) {

    if (args.length == 1) {
      return SUBCOMMANDS.stream()
          .filter(s -> s.startsWith(args[0].toLowerCase()))
          .collect(Collectors.toList());
    }

    if (args.length == 2
        && (args[0].equalsIgnoreCase("reset")
            || args[0].equalsIgnoreCase("clear")
            || args[0].equalsIgnoreCase("set"))) {

      return Bukkit.getOnlinePlayers().stream()
          .map(Player::getName)
          .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
          .collect(Collectors.toList());
    }

    if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
      return new ArrayList<>(plugin.getPresetManager().getAll().keySet());
    }

    return List.of();
  }
}
