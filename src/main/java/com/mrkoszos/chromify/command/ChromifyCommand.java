package com.mrkoszos.chromify.command;

import com.mrkoszos.chromify.Chromify;
import com.mrkoszos.chromify.gui.ChromifyMenu;
import com.mrkoszos.chromify.gui.ColorMenu;
import com.mrkoszos.chromify.gui.FavoritesMenu;
import com.mrkoszos.chromify.gui.FormattingMenu;
import com.mrkoszos.chromify.gui.GradientMenu;
import com.mrkoszos.chromify.gui.PresetMenu;
import com.mrkoszos.chromify.manager.PresetManager;
import com.mrkoszos.chromify.model.NameStyle;
import com.mrkoszos.chromify.model.StyleType;
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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ChromifyCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of(
            "color", "gradient", "formatting", "presets", "favorites",
            "reload", "reset", "set", "clear"
    );

    private final Chromify plugin;

    public ChromifyCommand(Chromify plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {

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
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (!player.hasPermission("chromify.use")) {
            player.sendMessage(
                    Component.text("You don't have permission to do that.", NamedTextColor.RED)
            );
            return true;
        }

        switch (menu) {
            case "color" -> new ColorMenu(plugin).open(player);
            case "gradient" -> new GradientMenu(plugin).open(player);
            case "formatting" -> new FormattingMenu(plugin).open(player);
            case "presets" -> new PresetMenu(plugin).open(player);
            case "favorites" -> new FavoritesMenu(plugin).open(player);
            default -> new ChromifyMenu(plugin).open(player);
        }

        return true;
    }

    /*
     * =========================
     *          RELOAD
     * =========================
     */

    private boolean handleReload(CommandSender sender) {

        if (!sender.hasPermission("chromify.reload")) {
            sender.sendMessage(
                    Component.text("You don't have permission to do that.", NamedTextColor.RED)
            );
            return true;
        }

        plugin.getPresetManager().reload();

        sender.sendMessage(
                Component.text("Chromify presets reloaded.", NamedTextColor.GREEN)
        );

        return true;
    }

    /*
     * =========================
     *           RESET
     * =========================
     */

    private boolean handleReset(CommandSender sender, String[] args) {

        if (!sender.hasPermission("chromify.reset")) {
            sender.sendMessage(
                    Component.text("You don't have permission to do that.", NamedTextColor.RED)
            );
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(
                    Component.text("Usage: /chromify reset <player>", NamedTextColor.RED)
            );
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);

        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(
                    Component.text("Player not found.", NamedTextColor.RED)
            );
            return true;
        }

        plugin.getNameStyleManager().resetStyle(target.getUniqueId());

        sender.sendMessage(
                Component.text(
                        "Reset " + target.getName() + "'s name style.",
                        NamedTextColor.GREEN
                )
        );

        return true;
    }

    /*
     * =========================
     *            SET
     * =========================
     */

    private boolean handleSet(CommandSender sender, String[] args) {

        if (!sender.hasPermission("chromify.set")) {
            sender.sendMessage(
                    Component.text("You don't have permission to do that.", NamedTextColor.RED)
            );
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(
                    Component.text("Usage: /chromify set <player> <preset>", NamedTextColor.RED)
            );
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);

        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(
                    Component.text("Player not found.", NamedTextColor.RED)
            );
            return true;
        }

        PresetManager.PresetDefinition preset = plugin.getPresetManager().get(args[2]);

        if (preset == null) {
            sender.sendMessage(
                    Component.text("Unknown preset: " + args[2], NamedTextColor.RED)
            );
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

        sender.sendMessage(
                Component.text(
                        "Set " + target.getName() + "'s style to preset '" + preset.display() + "'.",
                        NamedTextColor.GREEN
                )
        );

        return true;
    }

    /*
     * =========================
     *      TAB COMPLETION
     * =========================
     */

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args
    ) {

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
