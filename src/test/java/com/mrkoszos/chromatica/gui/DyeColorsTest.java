package com.mrkoszos.chromatica.gui;

import static org.junit.jupiter.api.Assertions.*;
import com.mrkoszos.chromatica.manager.NameStyleCodec;
import com.mrkoszos.chromatica.model.*;
import java.util.*;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class DyeColorsTest {
  @Test
  void everyStandardPaletteColorRetainsItsIdentity() {
    Material[] dyes = {Material.RED_DYE, Material.ORANGE_DYE, Material.YELLOW_DYE,
        Material.LIME_DYE, Material.CYAN_DYE, Material.BLUE_DYE, Material.PURPLE_DYE};
    for (int i = 0; i < dyes.length; i++) {
      assertEquals(dyes[i], DyeColors.nearest(GuiSelection.paletteHex(10 + i)));
      assertEquals(dyes[i], DyeColors.nearest(GuiSelection.paletteHex(10 + i).toLowerCase(Locale.ROOT)));
    }
  }

  @Test
  void exactVanillaReferenceColorsAndCustomChannelsAreStable() {
    String[] hex = {"#F9FFFE", "#F9801D", "#C74EBD", "#3AB3DA", "#FED83D", "#80C71F",
        "#F38BAA", "#474F52", "#9D9D97", "#169C9C", "#8932B8", "#3C44AA", "#835432",
        "#5E7C16", "#B02E26", "#1D1D21"};
    Material[] dyes = {Material.WHITE_DYE, Material.ORANGE_DYE, Material.MAGENTA_DYE,
        Material.LIGHT_BLUE_DYE, Material.YELLOW_DYE, Material.LIME_DYE, Material.PINK_DYE,
        Material.GRAY_DYE, Material.LIGHT_GRAY_DYE, Material.CYAN_DYE, Material.PURPLE_DYE,
        Material.BLUE_DYE, Material.BROWN_DYE, Material.GREEN_DYE, Material.RED_DYE, Material.BLACK_DYE};
    for (int i = 0; i < hex.length; i++) assertEquals(dyes[i], DyeColors.nearest(hex[i]));
    assertEquals(Material.RED_DYE, DyeColors.nearest("#FF0000"));
    assertEquals(Material.BLUE_DYE, DyeColors.nearest("#0000FF"));
    assertEquals(Material.LIME_DYE, DyeColors.nearest("#00FF00"));
    for (String invalid : Arrays.asList(null, "", "red", "#12345", "#GG0000", "123456"))
      assertEquals(Material.WHITE_DYE, DyeColors.nearest(invalid));
  }

  @Test
  void yamlRoundTripKeepsColorsOrderAndIconsWithoutChangingHex() throws Exception {
    var style = new NameStyle();
    style.setType(StyleType.GRADIENT);
    var colors = List.of("#FF5555", "#ffaa00", "#FFFF55", "#123456");
    style.setGradientColors(colors);
    var yaml = new YamlConfiguration();
    NameStyleCodec.write(yaml, style);
    var loadedYaml = new YamlConfiguration();
    loadedYaml.loadFromString(yaml.saveToString());
    var loaded = NameStyleCodec.read(loadedYaml);
    assertEquals(colors, loaded.getGradientColors());
    for (int i = 0; i < colors.size(); i++)
      assertEquals(DyeColors.nearest(colors.get(i)), DyeColors.nearest(loaded.getGradientColors().get(i)));
  }
}
