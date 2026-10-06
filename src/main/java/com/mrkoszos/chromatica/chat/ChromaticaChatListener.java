package com.mrkoszos.chromatica.chat;

import com.mrkoszos.chromatica.Chromatica;
import com.mrkoszos.chromatica.model.NameStyle;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class ChromaticaChatListener implements Listener {

    private final Chromatica plugin;

    public ChromaticaChatListener(Chromatica plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onChat(AsyncChatEvent event) {

        Player player = event.getPlayer();

        NameStyle style = plugin
                .getNameStyleManager()
                .getStyle(player.getUniqueId());

        Component name = plugin
                .getNameRenderer()
                .render(player.getName(), style);

        Component message = event.message();

        event.renderer((source, sourceDisplayName, chatMessage, viewer) ->
                name
                        .append(Component.text(": "))
                        .append(chatMessage)
        );
    }
}