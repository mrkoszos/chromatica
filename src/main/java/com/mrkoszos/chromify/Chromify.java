package com.mrkoszos.chromify;

import com.mrkoszos.chromify.command.ChromifyCommand;
import com.mrkoszos.chromify.gui.*;
import com.mrkoszos.chromify.manager.FavoriteManager;
import com.mrkoszos.chromify.manager.NameStyleManager;
import com.mrkoszos.chromify.manager.PresetManager;
import com.mrkoszos.chromify.placeholder.ChromifyExpansion;
import com.mrkoszos.chromify.renderer.NameRenderer;
import com.mrkoszos.chromify.chat.ChromifyChatListener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Chromify extends JavaPlugin {

    private NameStyleManager nameStyleManager;
    private NameRenderer nameRenderer;

    private final ColorSelectorSession colorSelectorSession =
            new ColorSelectorSession();

    private final GradientSession gradientSession =
            new GradientSession();

    private final Map<UUID, Integer> gradientColorSelection =
            new HashMap<>();

    private PresetManager presetManager;

    private FavoriteManager favoriteManager;

    @Override
    public void onEnable() {

        this.nameStyleManager = new NameStyleManager(this);
        nameRenderer = new NameRenderer();
        this.presetManager = new PresetManager(this);
        this.favoriteManager = new FavoriteManager();

        getCommand("chromify").setExecutor(
                new ChromifyCommand(this)
        );

        getServer().getPluginManager().registerEvents(
                new MenuListener(this),
                this
        );

        getServer().getPluginManager().registerEvents(
                new ChromifyChatListener(this),
                this
        );

        getServer().getPluginManager().registerEvents(
                new ColorMenuListener(this),
                this
        );

        getServer().getPluginManager().registerEvents(
                new ColorSelectorListener(this),
                this
        );

        getServer().getPluginManager().registerEvents(
                new GradientMenuListener(this),
                this
        );

        getServer().getPluginManager().registerEvents(
                new FormattingMenuListener(this),
                this
        );

        if (getServer().getPluginManager()
                .getPlugin("PlaceholderAPI") != null) {

            new ChromifyExpansion(this).register();
        }

        getServer().getPluginManager().registerEvents(
                new FavoritesMenuListener(this),
                this
        );

        getServer().getPluginManager().registerEvents(new PresetMenuListener(this), this);

        getLogger().info("Chromify has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Chromify has been disabled!");
    }

    public NameStyleManager getNameStyleManager() {
        return nameStyleManager;
    }

    public NameRenderer getNameRenderer() {
        return nameRenderer;
    }

    public ColorSelectorSession getColorSelectorSession() {
        return colorSelectorSession;
    }

    public GradientSession getGradientSession() {
        return gradientSession;
    }

    public Map<UUID, Integer> getGradientColorSelection() {
        return gradientColorSelection;
    }

    public PresetManager getPresetManager() {
        return presetManager;
    }

    public FavoriteManager getFavoriteManager() {
        return favoriteManager;
    }
}