package com.mrkoszos.chromatica.renderer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

import java.util.List;

public class GradientRenderer {

    public Component render(String text, List<String> colors) {

        if (text == null || text.isEmpty()) {
            return Component.empty();
        }

        if (colors == null || colors.size() < 2) {
            return Component.text(text);
        }

        List<TextColor> parsedColors = colors.stream()
                .map(TextColor::fromHexString)
                .toList();

        if (parsedColors.stream().anyMatch(color -> color == null)) {
            return Component.text(text);
        }

        Component result = Component.empty();

        int length = text.length();

        for (int i = 0; i < length; i++) {

            double position;

            if (length == 1) {
                position = 0;
            } else {
                position = (double) i / (length - 1);
            }

            TextColor color = getColorAtPosition(
                    parsedColors,
                    position
            );

            result = result.append(
                    Component.text(String.valueOf(text.charAt(i)))
                            .color(color)
            );
        }

        return result;
    }

    private TextColor getColorAtPosition(
            List<TextColor> colors,
            double position
    ) {

        if (position <= 0) {
            return colors.getFirst();
        }

        if (position >= 1) {
            return colors.getLast();
        }

        double scaledPosition = position * (colors.size() - 1);

        int index = (int) Math.floor(scaledPosition);

        double localPosition = scaledPosition - index;

        TextColor first = colors.get(index);
        TextColor second = colors.get(index + 1);

        return interpolate(first, second, localPosition);
    }

    private TextColor interpolate(
            TextColor first,
            TextColor second,
            double amount
    ) {

        int red = interpolate(
                first.red(),
                second.red(),
                amount
        );

        int green = interpolate(
                first.green(),
                second.green(),
                amount
        );

        int blue = interpolate(
                first.blue(),
                second.blue(),
                amount
        );

        return TextColor.color(red, green, blue);
    }

    private int interpolate(
            int first,
            int second,
            double amount
    ) {
        return (int) Math.round(
                first + (second - first) * amount
        );
    }
}