package com.mrkoszos.chromify.renderer;

import com.mrkoszos.chromify.model.NameStyle;
import com.mrkoszos.chromify.model.StyleType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.List;

public class NameRenderer {

    private final GradientRenderer gradientRenderer;

    public NameRenderer() {
        this.gradientRenderer = new GradientRenderer();
    }

    public Component render(String name, NameStyle style) {

        Component component;

        if (style.getType() == StyleType.GRADIENT) {

            component = gradientRenderer.render(
                    name,
                    style.getGradientColors()
            );

        } else {

            component = renderSolid(name, style);
        }

        component = component.decoration(
                TextDecoration.BOLD,
                style.isBold()
        );

        component = component.decoration(
                TextDecoration.ITALIC,
                style.isItalic()
        );

        component = component.decoration(
                TextDecoration.UNDERLINED,
                style.isUnderlined()
        );

        component = component.decoration(
                TextDecoration.STRIKETHROUGH,
                style.isStrikethrough()
        );

        return component;
    }

    public Component renderGradientPreview(
            String name,
            List<String> colors,
            NameStyle style
    ) {
        if (colors == null || colors.size() < 2) {
            return Component.text(name);
        }

        Component component = gradientRenderer.render(
                name,
                colors
        );

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
            component = component.decorate(
                    TextDecoration.STRIKETHROUGH
            );
        }

        return component;
    }

    private Component renderSolid(
            String name,
            NameStyle style
    ) {

        TextColor color = TextColor.fromHexString(
                style.getPrimaryColor()
        );

        if (color == null) {
            color = TextColor.color(255, 255, 255);
        }

        return Component.text(name)
                .color(color);
    }
}