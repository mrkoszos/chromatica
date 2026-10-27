package com.mrkoszos.chromatica.renderer;

import com.mrkoszos.chromatica.model.NameStyle;
import com.mrkoszos.chromatica.model.StyleType;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class NameRenderer {

  private final GradientRenderer gradientRenderer;

  public NameRenderer() {
    this.gradientRenderer = new GradientRenderer();
  }

  public Component render(String name, NameStyle style, boolean scrambleAllowed) {
    NameStyle effective = style.copy();
    effective.setScramble(style.isScramble() && scrambleAllowed);
    return render(name, effective);
  }

  public Component renderGradientPreview(
      String name, List<String> colors, NameStyle style, boolean allowed) {
    NameStyle draft = style.copy();
    draft.setType(StyleType.GRADIENT);
    draft.setGradientColors(colors);
    return render(name, draft, allowed);
  }

  public Component render(String name, NameStyle style) {

    Component component;

    if (style.getType() == StyleType.GRADIENT) {

      component = gradientRenderer.render(name, style.getGradientColors());

    } else {

      component = renderSolid(name, style);
    }

    component = component.decoration(TextDecoration.BOLD, style.isBold());

    component = component.decoration(TextDecoration.ITALIC, style.isItalic());

    component = component.decoration(TextDecoration.UNDERLINED, style.isUnderlined());

    component = component.decoration(TextDecoration.STRIKETHROUGH, style.isStrikethrough());

    return component.decoration(TextDecoration.OBFUSCATED, style.isScramble());
  }

  public Component renderGradientPreview(String name, List<String> colors, NameStyle style) {
    if (colors == null || colors.size() < 2) {
      return Component.text(name);
    }

    Component component = gradientRenderer.render(name, colors);

    if (style.isBold()) {
      component = component.decorate(TextDecoration.BOLD);
    }

    if (style.isItalic()) {
      component = component.decorate(TextDecoration.ITALIC);
    }

    if (style.isUnderlined()) {
      component = component.decorate(TextDecoration.UNDERLINED);
    }

    if (style.isStrikethrough()) {
      component = component.decorate(TextDecoration.STRIKETHROUGH);
    }

    return component.decoration(TextDecoration.OBFUSCATED, style.isScramble());
  }

  private Component renderSolid(String name, NameStyle style) {

    TextColor color = TextColor.fromHexString(style.getPrimaryColor());

    if (color == null) {
      color = TextColor.color(255, 255, 255);
    }

    return Component.text(name).color(color);
  }
}
