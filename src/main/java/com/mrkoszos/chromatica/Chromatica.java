package com.mrkoszos.chromatica;

import com.mrkoszos.chromatica.command.ChromaticaCommand;
import com.mrkoszos.chromatica.gui.*;
import com.mrkoszos.chromatica.manager.BackgroundManager;
import com.mrkoszos.chromatica.manager.FavoriteManager;
import com.mrkoszos.chromatica.manager.NameStyleManager;
import com.mrkoszos.chromatica.manager.PresetManager;
import com.mrkoszos.chromatica.placeholder.ChromaticaExpansion;
import com.mrkoszos.chromatica.renderer.NameRenderer;
import com.mrkoszos.chromatica.chat.ChromaticaChatListener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Chromatica extends JavaPlugin {

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

    private BackgroundManager backgroundManager;

    @Override
    public void onEnable() {

        this.backgroundManager = new BackgroundManager(this);
        this.nameStyleManager = new NameStyleManager(this);
        nameRenderer = new NameRenderer();
        this.presetManager = new PresetManager(this);
        this.favoriteManager = new FavoriteManager();

        getCommand("chromatica").setExecutor(
                new ChromaticaCommand(this)
        );

        getServer().getPluginManager().registerEvents(
                new ChromaticaMenuListener(this),
                this
        );

        getServer().getPluginManager().registerEvents(
                new ChromaticaChatListener(this),
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

            new ChromaticaExpansion(this).register();
        }

        getServer().getPluginManager().registerEvents(
                new FavoritesMenuListener(this),
                this
        );

        getServer().getPluginManager().registerEvents(new PresetMenuListener(this), this);

        getLogger().info("Chromatica has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Chromatica has been disabled!");
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

    public BackgroundManager getBackgroundManager() {
        return backgroundManager;
    }
}