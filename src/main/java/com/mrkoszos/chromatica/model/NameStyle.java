package com.mrkoszos.chromatica.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class NameStyle {

    private StyleType type;

    private String primaryColor;
    private String secondaryColor;

    private List<String> gradientColors;

    private String presetId;

    private boolean bold;
    private boolean italic;
    private boolean underlined;
    private boolean strikethrough;

    public NameStyle() {
        this.type = StyleType.SOLID;
        this.primaryColor = "#FFFFFF";
        this.gradientColors = new ArrayList<>();
    }

    public StyleType getType() {
        return type;
    }

    public void setType(StyleType type) {
        this.type = type;
    }

    public String getPrimaryColor() {
        return primaryColor;
    }

    public void setPrimaryColor(String primaryColor) {
        this.primaryColor = primaryColor;
    }

    public String getSecondaryColor() {
        return secondaryColor;
    }

    public void setSecondaryColor(String secondaryColor) {
        this.secondaryColor = secondaryColor;
    }

    public List<String> getGradientColors() {
        return gradientColors;
    }

    public void setGradientColors(List<String> gradientColors) {
        this.gradientColors = new ArrayList<>(gradientColors);
    }

    public void addGradientColor(String color) {
        this.gradientColors.add(color);
    }

    public String getPresetId() {
        return presetId;
    }

    public void setPresetId(String presetId) {
        this.presetId = presetId;
    }

    public boolean isBold() {
        return bold;
    }

    public void setBold(boolean bold) {
        this.bold = bold;
    }

    public boolean isItalic() {
        return italic;
    }

    public void setItalic(boolean italic) {
        this.italic = italic;
    }

    public boolean isUnderlined() {
        return underlined;
    }

    public void setUnderlined(boolean underlined) {
        this.underlined = underlined;
    }

    public boolean isStrikethrough() {
        return strikethrough;
    }

    public void setStrikethrough(boolean strikethrough) {
        this.strikethrough = strikethrough;
    }

    public NameStyle copy() {
        NameStyle copy = new NameStyle();

        copy.type = this.type;
        copy.primaryColor = this.primaryColor;
        copy.secondaryColor = this.secondaryColor;
        copy.gradientColors = new ArrayList<>(this.gradientColors);
        copy.presetId = this.presetId;
        copy.bold = this.bold;
        copy.italic = this.italic;
        copy.underlined = this.underlined;
        copy.strikethrough = this.strikethrough;

        return copy;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof NameStyle other)) {
            return false;
        }

        return bold == other.bold
                && italic == other.italic
                && underlined == other.underlined
                && strikethrough == other.strikethrough
                && type == other.type
                && Objects.equals(primaryColor, other.primaryColor)
                && Objects.equals(secondaryColor, other.secondaryColor)
                && Objects.equals(gradientColors, other.gradientColors)
                && Objects.equals(presetId, other.presetId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                type,
                primaryColor,
                secondaryColor,
                gradientColors,
                presetId,
                bold,
                italic,
                underlined,
                strikethrough
        );
    }
}