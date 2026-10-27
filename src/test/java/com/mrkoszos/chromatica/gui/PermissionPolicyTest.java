package com.mrkoszos.chromatica.gui;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.mrkoszos.chromatica.Chromatica;
import com.mrkoszos.chromatica.command.ChromaticaCommand;
import com.mrkoszos.chromatica.manager.NameStyleManager;
import com.mrkoszos.chromatica.model.NameStyle;
import java.io.StringReader;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.bukkit.permissions.*;
import org.bukkit.plugin.*;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;

/** Exercise Bukkit's real default/attachment resolution, not a mocked hasPermission result. */
class PermissionPolicyTest {
  MockedStatic<Bukkit> bukkit;
  Map<String, Permission> permissions;
  Chromatica plugin;

  @BeforeEach void setup() throws Exception {
    var server = mock(Server.class);
    var manager = mock(PluginManager.class);
    when(server.getPluginManager()).thenReturn(manager);
    bukkit = mockStatic(Bukkit.class);
    bukkit.when(Bukkit::getServer).thenReturn(server);
    bukkit.when(Bukkit::getPluginManager).thenReturn(manager);
    permissions = new LinkedHashMap<>();
    when(manager.getPermission(anyString())).thenAnswer(c -> permissions.get(c.getArgument(0)));
    when(manager.getDefaultPermissions(anyBoolean())).thenAnswer(c -> {
      Set<Permission> defaults = new LinkedHashSet<>();
      for (var permission : permissions.values())
        if (permission.getDefault().getValue(c.getArgument(0))) defaults.add(permission);
      return defaults;
    });
    // Bukkit registers these descriptor permissions automatically; no custom registrar exists.
    var descriptor = new PluginDescriptionFile(
        new StringReader(Files.readString(Path.of("src/main/resources/plugin.yml"))));
    for (var permission : descriptor.getPermissions()) permissions.put(permission.getName(), permission);
    plugin = mock(Chromatica.class);
    when(plugin.isEnabled()).thenReturn(true);
  }

  @AfterEach void cleanup() { bukkit.close(); }

  PermissibleBase subject(boolean op) {
    var operator = mock(ServerOperator.class);
    when(operator.isOp()).thenReturn(op);
    return new PermissibleBase(operator);
  }

  @Test void allDeclaredAndFutureNodesFollowPolicyAndEveryUsedNodeIsDeclared() throws Exception {
    assertFalse(permissions.isEmpty());
    assertEquals(PermissionDefault.TRUE, permissions.get("chromatica.use").getDefault());
    for (var permission : permissions.values()) {
      assertTrue(permission.getDefault() == PermissionDefault.OP
          || permission.getDefault() == PermissionDefault.TRUE, permission.getName());
      for (String child : permission.getChildren().keySet())
        assertTrue(permissions.containsKey(child), "Undeclared child " + child);
    }
    Pattern node = Pattern.compile("\"(chromatica\\.[a-z0-9_.]+)\"");
    try (var paths = Files.walk(Path.of("src/main/java"))) {
      for (Path file : paths.filter(p -> p.toString().endsWith(".java")).toList()) {
        String source = Files.readString(file);
        assertFalse(source.contains(".isOp("), "Do not bypass permissions in " + file);
        var matcher = node.matcher(source);
        while (matcher.find())
          assertTrue(permissions.containsKey(matcher.group(1)),
              "Undeclared runtime node " + matcher.group(1) + " in " + file);
      }
    }
  }

  @Test void operatorsHaveEveryDeclaredPermissionByDefault() {
    var op = subject(true);
    for (String node : permissions.keySet()) assertTrue(op.hasPermission(node), node);
  }

  @Test void nonOperatorsKeepPublicAccessAndRestrictedNodesRequireGrants() {
    var player = subject(false);
    for (var permission : permissions.values())
      assertEquals(permission.getDefault() == PermissionDefault.TRUE,
          player.hasPermission(permission.getName()), permission.getName());
  }

  @Test void explicitDenialsOverrideOpAndInheritedAdminDefaults() {
    var op = subject(true);
    for (String node : permissions.keySet()) {
      var denial = op.addAttachment(plugin, node, false);
      assertFalse(op.hasPermission(node), node);
      op.removeAttachment(denial);
      assertTrue(op.hasPermission(node), "Default restored: " + node);
    }
  }

  @Test void explicitGrantsAndRevocationUseNormalBukkitResolutionForNonOperators() {
    var player = subject(false);
    for (var permission : permissions.values()) {
      if (permission.getDefault() == PermissionDefault.TRUE) continue;
      var grant = player.addAttachment(plugin, permission.getName(), true);
      assertTrue(player.hasPermission(permission.getName()), permission.getName());
      player.removeAttachment(grant);
      assertFalse(player.hasPermission(permission.getName()), permission.getName());
    }
  }

  @Test void opDenialsAreRecheckedWhenSavingAndExecutingAdministrativeCommands() {
    var op = subject(true);
    var player = mock(Player.class);
    when(player.getUniqueId()).thenReturn(UUID.randomUUID());
    when(player.hasPermission(anyString())).thenAnswer(c -> op.hasPermission((String) c.getArgument(0)));
    var styles = mock(NameStyleManager.class);
    when(plugin.getNameStyleManager()).thenReturn(styles);
    when(styles.setStyle(any(), any())).thenReturn(true);
    var style = new NameStyle(); style.setScramble(true);
    assertTrue(GuiUtils.persist(plugin, player, style));
    // Access was available earlier; the new attachment must affect this action immediately.
    op.addAttachment(plugin, "chromatica.format.scramble", false);
    assertFalse(GuiUtils.persist(plugin, player, style));
    style.setScramble(false);
    op.addAttachment(plugin, "chromatica.use", false);
    assertFalse(GuiUtils.persist(plugin, player, style));
    verify(styles, times(1)).setStyle(any(), any());
    op.addAttachment(plugin, "chromatica.reload", false);
    new ChromaticaCommand(plugin).onCommand(player, mock(Command.class), "chromatica",
        new String[] {"reload"});
    verify(plugin, never()).getPresetManager();
    verify(player, never()).isOp();
  }
}
