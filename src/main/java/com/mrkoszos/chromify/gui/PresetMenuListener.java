package com.mrkoszos.chromify.gui;

import com.mrkoszos.chromify.Chromify;
import com.mrkoszos.chromify.manager.PresetManager;
import com.mrkoszos.chromify.model.NameStyle;
import com.mrkoszos.chromify.model.StyleType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;

public class PresetMenuListener implements Listener {

    private final Chromify plugin;

    public PresetMenuListener(Chromify plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {

        if (!event.getView().getTitle().equals(PresetMenu.TITLE)) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (event.getCurrentItem() == null) {
            return;
        }

        int slot = event.getRawSlot();

        /*
         * =========================
         *            BACK
         * =========================
         */

        if (slot == 40) {
            player.closeInventory();
            new ChromifyMenu(plugin).open(player);
            return;
        }

        /*
         * =========================
         *       PRESET SLOTS
         * =========================
         */

        for (int i = 0; i < PresetMenu.PRESET_SLOTS.length; i++) {

            if (slot != PresetMenu.PRESET_SLOTS[i]) {
                continue;
            }

            List<PresetManager.PresetDefinition> presets =
                    new ArrayList<>(plugin.getPresetManager().getAll().values());

            if (i >= presets.size()) {
                return;
            }

            applyPreset(player, presets.get(i));
            return;
        }
    }

    private void applyPreset(Player player, PresetManager.PresetDefinition preset) {

        NameStyle style = plugin.getNameStyleManager()
                .getStyle(player.getUniqueId());

        List<String> colors = preset.colors();

        if (colors.size() == 1) {

            style.setPrimaryColor(colors.get(0));
            style.setType(StyleType.SOLID);

        } else {

            style.setGradientColors(new ArrayList<>(colors));
            style.setType(StyleType.GRADIENT);
        }

        style.setPresetId(preset.id());

        plugin.getNameStyleManager()
                .saveStyle(player.getUniqueId());   // ÚJ SOR

        player.sendActionBar(
                Component.text(
                        "Preset '" + preset.display() + "' has been applied.",
                        NamedTextColor.GREEN
                )
        );

        player.closeInventory();

        new ChromifyMenu(plugin).open(player);
    }
}
