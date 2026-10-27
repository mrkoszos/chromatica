package com.mrkoszos.chromatica;

import com.mrkoszos.chromatica.chat.ChromaticaChatListener;
import com.mrkoszos.chromatica.command.ChromaticaCommand;
import com.mrkoszos.chromatica.gui.*;
import com.mrkoszos.chromatica.manager.BackgroundManager;
import com.mrkoszos.chromatica.manager.FavoriteManager;
import com.mrkoszos.chromatica.manager.NameStyleManager;
import com.mrkoszos.chromatica.manager.PresetManager;
import com.mrkoszos.chromatica.placeholder.ChromaticaExpansion;
import com.mrkoszos.chromatica.renderer.NameRenderer;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.plugin.java.JavaPlugin;

public final class Chromatica extends JavaPlugin {

  private GuiSounds guiSounds;
  private GuiBranding guiBranding;
  private final GuiAnvils guiAnvils = new GuiAnvils();

  public GuiAnvils getGuiAnvils() {
    return guiAnvils;
  }

  public GuiSounds getGuiSounds() {
    return guiSounds;
  }

  public GuiBranding getGuiBranding() {
    return guiBranding;
  }

  private boolean expandedGradient;

  public boolean isExpandedGradient() {
    return expandedGradient;
  }

  private volatile boolean chatFormattingEnabled;
  private volatile boolean communityPresetsEnabled = true;
  private volatile int communityPresetLimit = 10;
  private com.mrkoszos.chromatica.manager.CommunityPresetManager communityPresetManager;

  public boolean isCommunityPresetsEnabled() {
    return communityPresetsEnabled;
  }

  public int getCommunityPresetLimit() {
    return communityPresetLimit;
  }

  public com.mrkoszos.chromatica.manager.CommunityPresetManager getCommunityPresetManager() {
    return communityPresetManager;
  }

  public boolean isChatFormattingEnabled() {
    return chatFormattingEnabled;
  }

  private boolean configReloadSuccessful = true;
  private boolean configLoaded;

  public boolean wasConfigReloadSuccessful() {
    return configReloadSuccessful;
  }

  @Override
  public void reloadConfig() {
    java.io.File file = new java.io.File(getDataFolder(), "config.yml");
    if (file.exists()) {
      try {
        org.bukkit.configuration.file.YamlConfiguration check =
            new org.bukkit.configuration.file.YamlConfiguration();
        check.load(file);
      } catch (Exception exception) {
        configReloadSuccessful = false;
        getLogger()
            .warning(
                "Cannot reload config.yml; keeping previous settings: " + exception.getMessage());
        if (!configLoaded) {
          super.reloadConfig();
          configLoaded = true;
          applyRuntimeSettings();
        }
        return;
      }
    }
    configReloadSuccessful = true;
    super.reloadConfig();
    configLoaded = true;
    applyRuntimeSettings();
  }

  private void applyRuntimeSettings() {
    if (guiSounds != null) guiSounds.reload(getConfig(), getLogger());
    if (guiBranding != null) guiBranding.reload(getConfig());
    String layout = getConfig().getString("gui.gradient.layout", "compact");
    expandedGradient = GradientMenu.expandedLayout(layout, getLogger());
    chatFormattingEnabled = getConfig().getBoolean("chat-formatting-enabled", false);
    communityPresetsEnabled = getConfig().getBoolean("community-presets.enabled", true);
    communityPresetLimit = Math.max(0, getConfig().getInt("community-presets.max-per-player", 10));
  }

  private NameStyleManager nameStyleManager;
  private NameRenderer nameRenderer;

  private final ColorSelectorSession colorSelectorSession = new ColorSelectorSession();

  private final GradientSession gradientSession = new GradientSession();

  private final Map<UUID, Integer> gradientColorSelection = new HashMap<>();

  private PresetManager presetManager;

  private FavoriteManager favoriteManager;

  private BackgroundManager backgroundManager;
  private final java.util.Set<UUID> hexPlayers = new java.util.HashSet<>();

  public java.util.Set<UUID> getHexPlayers() {
    return hexPlayers;
  }

  @Override
  public void onEnable() {

    this.guiSounds = new GuiSounds();
    this.guiBranding = new GuiBranding();
    this.backgroundManager = new BackgroundManager(this);
    this.nameStyleManager = new NameStyleManager(this);
    getServer()
        .getOnlinePlayers()
        .forEach(player -> nameStyleManager.getStyle(player.getUniqueId()));
    nameRenderer = new NameRenderer();
    this.presetManager = new PresetManager(this);
    this.favoriteManager = new FavoriteManager(getDataFolder(), getLogger());
    this.communityPresetManager =
        new com.mrkoszos.chromatica.manager.CommunityPresetManager(getDataFolder(), getLogger());

    getCommand("chromatica").setExecutor(new ChromaticaCommand(this));

    getServer().getPluginManager().registerEvents(new ChromaticaMenuListener(this), this);

    getServer().getPluginManager().registerEvents(new ChromaticaChatListener(this), this);

    getServer().getPluginManager().registerEvents(new ColorMenuListener(this), this);

    getServer().getPluginManager().registerEvents(new ColorSelectorListener(this), this);

    getServer().getPluginManager().registerEvents(new GradientMenuListener(this), this);

    getServer().getPluginManager().registerEvents(new FormattingMenuListener(this), this);

    if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {

      new ChromaticaExpansion(this).register();
    }

    getServer().getPluginManager().registerEvents(new FavoritesMenuListener(this), this);

    getServer().getPluginManager().registerEvents(new PresetMenuListener(this), this);
    getServer().getPluginManager().registerEvents(new PresetBrowserListener(this), this);

    getServer().getPluginManager().registerEvents(new GuiSafetyListener(this), this);
    getServer().getPluginManager().registerEvents(new InlineConfirmation(this), this);
    getServer().getPluginManager().registerEvents(guiAnvils, this);
    getLogger().info("Chromatica has been enabled!");
  }

  @Override
  public void onDisable() {
    InlineConfirmation.clear(this);
    guiAnvils.clear();
    GuiHeads.clear(this);
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
