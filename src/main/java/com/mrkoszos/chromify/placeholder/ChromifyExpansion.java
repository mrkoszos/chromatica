package com.mrkoszos.chromify.placeholder;

import com.mrkoszos.chromify.Chromify;
import com.mrkoszos.chromify.model.NameStyle;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ChromifyExpansion extends PlaceholderExpansion {

    private final Chromify plugin;

    public ChromifyExpansion(Chromify plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "chromify";
    }

    @Override
    public @NotNull String getAuthor() {
        return "mk";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(
            Player player,
            @NotNull String identifier
    ) {

        if (player == null) {
            return "";
        }

        NameStyle style = plugin
                .getNameStyleManager()
                .getStyle(player.getUniqueId());

        Component rendered = plugin
                .getNameRenderer()
                .render(player.getName(), style);

        /*
         * =========================
         *   %chromify_name%  (ALAP - MiniMessage)
         * =========================
         */

        if (identifier.equals("name")) {
            return MiniMessage.miniMessage()
                    .serialize(rendered);
        }

        /*
         * =========================
         *   %chromify_name_legacy%
         * =========================
         * Tartalék legacy §-kódos formátum,
         * ha egy plugin nem érti a MiniMessage-t.
         */

        if (identifier.equals("name_legacy")) {
            return LegacyComponentSerializer
                    .legacySection()
                    .serialize(rendered);
        }

        return null;
    }
}