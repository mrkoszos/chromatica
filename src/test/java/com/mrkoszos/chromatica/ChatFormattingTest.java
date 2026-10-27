package com.mrkoszos.chromatica;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.mrkoszos.chromatica.chat.ChromaticaChatListener;
import com.mrkoszos.chromatica.manager.NameStyleManager;
import com.mrkoszos.chromatica.model.NameStyle;
import com.mrkoszos.chromatica.renderer.NameRenderer;
import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ChatFormattingTest {
  @Test
  void disabledLeavesEventEntirelyUntouched() {
    Chromatica plugin = mock(Chromatica.class);
    AsyncChatEvent event = mock(AsyncChatEvent.class);
    new ChromaticaChatListener(plugin).onChat(event);
    verifyNoInteractions(event);
    verify(plugin, never()).getNameStyleManager();
  }

  @Test
  void enabledKeepsStyledNameAndMessageAndCanBeDisabledAgain() {
    Chromatica plugin = mock(Chromatica.class);
    AtomicBoolean enabled = new AtomicBoolean(true);
    when(plugin.isChatFormattingEnabled()).thenAnswer(invocation -> enabled.get());
    NameStyleManager manager = mock(NameStyleManager.class);
    NameRenderer renderer = new NameRenderer();
    when(plugin.getNameStyleManager()).thenReturn(manager);
    when(plugin.getNameRenderer()).thenReturn(renderer);
    Player player = mock(Player.class);
    UUID uuid = UUID.randomUUID();
    when(player.getUniqueId()).thenReturn(uuid);
    when(player.getName()).thenReturn("Tester");
    NameStyle style = new NameStyle();
    style.setPrimaryColor("#123456");
    style.setBold(true);
    when(manager.getSnapshot(uuid)).thenReturn(style);
    AsyncChatEvent event = mock(AsyncChatEvent.class);
    when(event.getPlayer()).thenReturn(player);
    ChromaticaChatListener listener = new ChromaticaChatListener(plugin);
    listener.onChat(event);
    ArgumentCaptor<ChatRenderer> captured = ArgumentCaptor.forClass(ChatRenderer.class);
    verify(event).renderer(captured.capture());
    Component message = Component.text("message");
    assertEquals(
        Component.empty()
            .append(renderer.render("Tester", style))
            .append(Component.text(": "))
            .append(message),
        captured.getValue().render(player, Component.text("display"), message, player));
    enabled.set(false);
    AsyncChatEvent disabledEvent = mock(AsyncChatEvent.class);
    listener.onChat(disabledEvent);
    verifyNoInteractions(disabledEvent);
  }
}
