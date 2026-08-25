package com.mrkoszos.chromify.command;

import com.mrkoszos.chromify.Chromify;
import com.mrkoszos.chromify.gui.ChromifyMenu;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ChromifyCommand implements CommandExecutor {

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

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        new ChromifyMenu(plugin).open(player);

        return true;
    }
}