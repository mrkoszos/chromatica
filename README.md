# Chromatica local development

Targets Paper/Minecraft 26.2 with Java 25. Plugin version is maintained in
`gradle.properties` and expanded into `plugin.yml` during the build.

In PowerShell, point `JAVA_HOME` to your JDK 25 installation (the path below
matches this development machine), then use the Gradle Wrapper:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-25.0.2'
.\gradlew.bat clean build
.\gradlew.bat runServer
```

`runServer` downloads Paper 26.2 and automatically builds and loads the shaded
Chromatica JAR. No manual copying is needed. Server files are stored in the
Git-ignored `run/` directory. On first launch, review the Minecraft EULA linked
in `run/eula.txt`; if you agree, set `eula=true` and launch again. Enter `stop`
in the server console to shut down cleanly.

The distributable plugin is `build/libs/Chromatica 26.2-1.3-all.jar`.

`src.zip` is a historical source snapshot from before the rebrand. It contains
Chromify names and packages, is retained as an archive, and is not a build input.

## Chromatica 1.3 release candidate

See RELEASE_1.3_VALIDATION.md for stabilization changes, validation results and the manual multiplayer checklist. Historical 1.2 validation notes are retained separately.

Player styles, five fixed Favorites slots, and Community Presets retain their existing YAML paths and fields. Invalid files are preserved rather than overwritten. Fix a quarantined player/Favorites file and reconnect to retry loading it. Community data can be reloaded with /chromatica reload; malformed catalogues retain their previous in-memory values.

Style manager callers receive detached drafts: persist edits using setStyle(uuid, draft) and check its boolean result. Async integrations use getSnapshot(uuid), which never reads disk. Player join preloads styles; disconnect unloads them.

The main menu credit uses the existing corner decoration (slot 0), with one gray lore line. Clicking sends only that player the metadata-derived version message. branding.show-credit: false leaves ordinary decoration. Background materials and patterns are retained.

## GUI redesign in 1.3

Color, Favorites and Formatting use 27 slots with the head preview at 4 and Barrier Back at 22. Color keeps seven colors at 10–16 and its existing lime-pane Custom HEX at 17. Favorites uses five fixed positions 11–15. Formatting uses Mace, Writable/Written Book, Brush, Shears and Stick at 11–15. Gradient uses `gui.gradient.layout: compact` (27 slots, seven stops at 10–16) or `expanded` (45 slots, twenty-one stops at 10–16, 19–25 and 28–34). Both modes use preview 4. Compact actions: Apply 21, Add Color 22, Back 23. Expanded actions: Apply 39, Add Color 40, Back 41. Materials remain Knowledge Book, Netherite Upgrade Smithing Template icon and Barrier respectively. Add Color appends white in place; click an existing stop to use the standard/HEX editor. Right-click removes a stop, retaining at least two. Unused stop slots stay empty even after background refresh. Saved gradients above seven stops automatically expand without truncation; new additions respect the visible seven/twenty-one capacity, including auto-expanded saved gradients. Add Color remains visible at capacity. Dye icons honor the seven standard HEX identities first; arbitrary HEX uses hue-weighted D65 OKLab distance against vanilla dye diffuse RGB values with stable tie breaking. Invalid HEX uses white dye without changing stored colors.

Presets uses 27 slots: player preview 4, Server 10 (Structure Block), Premium 11 (Prize Pottery Sherd), Seasonal 15 (Clock), Community 16 (static Globe Player Head, Minecraft-Heads 23202), Barrier Back 22. The exact supplied Base64 texture is embedded in GuiHeads without a network lookup or attribution lore. Server and enabled Community categories open their existing browsers. Premium and Seasonal currently show an unavailable actionbar message without opening an inventory; disabled Community does the same. No animation renderer, seasonal activation, integration, or data-format migration was added.

Favorites and Community Presets use the Drop action twice on the same entry within five seconds to delete. The warning says `Drop again to delete`, independent of keyboard bindings. Reset uses two ordinary left clicks. First interaction only arms the action; another interaction, closing, navigation or disconnect cancels it. Player `/chromatica reset` and `/chromatica clear [player]` require a repeated invocation for the same target within five seconds. Console target resets remain immediate.

Formatting slots 11–15 are Bold, Italic, Scramble, Strikethrough and Underline. `chromatica.format.scramble` defaults to `op`. Stored Scramble data is preserved; rendering suppresses it without permission, and applying a Scramble style is rejected without changing the current style. Scramble can always be switched off. Server presets may optionally set `scramble: true/false`; omission preserves existing formatting. Administrative application of Scramble requires an online target with permission. Legacy name placeholders end with a reset code to contain formatting.

Each formatting toggle has independent `gui.sounds.<bold|italic|scramble|strikethrough|underline>-<on|off>.sound`, `.volume` and `.pitch` settings. They use the existing global sound toggle and reload command. Existing configuration values remain in effect; fresh configuration uses alternating gray/light-gray background panes.

See [FINAL_BROWSER_VALIDATION_HU.md](FINAL_BROWSER_VALIDATION_HU.md) for the current Hungarian validation report and manual checklist. Earlier GUI and confirmation reports describe historical layouts.

## Preset browsers and GUI tooltips

Server and Community browsers use 45 slots and 21 presets per page: 10–16, 19–25, 28–34. Unused preset positions stay empty, including after background refresh. Slot 39 is Random Preset (Bundle) for Server and Publish Preset (Netherite Upgrade Smithing Template icon) for Community. Slot 40 is Paper (`Page current/total`): ordinary left click goes back one page, right click advances, with no wrapping. Slot 41 is Barrier Back to the Presets categories. Neither browser has a Close button. Refreshes keep the same inventory, cursor, browser type and UUID-based community targets; deletion clamps the last page.

Random uses ThreadLocalRandom and the complete eligible Server catalogue, including other pages. It respects `chromatica.use`, valid color data, and the effective Scramble permission after optional preset formatting. It uses the same application/persistence path as ordinary selection, preserving unrelated formatting and the committed style on failure. One action emits one result sound.

GuiTooltips uses Paper TOOLTIP_DISPLAY with selected hidden components for bundle contents, written-book metadata, equipment attributes and other automatic descriptions. Custom names, intentional lore, profile textures and glint remain visible. Smithing template text is hard-coded in 26.2 and cannot be suppressed with a component flag: these GUI icons use a neutral Paper carrier with the exact vanilla template ITEM_MODEL. The icon requires no resource pack; the physical carrier material is Paper. No player-owned items are modified. Background decorations retain their intentional tooltip suppression.

See FINAL_BROWSER_VALIDATION_HU.md for the latest validation results. GuiTooltipRuntimeProbe under src/test is an optional real-Paper probe; it is packaged separately for validation and is never part of the release JAR.

## Permission defaults

All declared restricted Chromatica permissions default to `op`, including Scramble and Community administration. `chromatica.use` remains `true` for ordinary players; regular formatting, Favorites and Community publishing continue to use that permission. Bukkit registers the nodes from plugin.yml; there is no separate runtime permission registrar. Actions continue to check hasPermission, so explicit permission-plugin denials apply to OPs too. No isOp bypass or wildcard grant is used. Premium and animations are currently unavailable and have no dedicated permission nodes; future nodes must be declared with `op` (or `true` for deliberately public features). PermissionPolicyTest validates every declared node and runtime permission name against this policy.

Expanded gradients support 21 stops across the editor, player-style YAML, Favorites, Community Presets and server preset loading/application. The shared NameStyleCodec validation rejects 22 stops without truncation. The original saved fields and legacy PRESET style normalization are unchanged. See FINAL_EXPANDED_GRADIENT_VALIDATION_HU.md for this layout/capacity update.
