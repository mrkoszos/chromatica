package com.mrkoszos.chromify.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class GuiUtils {

    private GuiUtils() {
    }

    public static void fillEmptySlots(Inventory inventory) {

        ItemStack pane =
                new ItemStack(
                        Material.BLACK_STAINED_GLASS_PANE
                );

        ItemMeta meta =
                pane.getItemMeta();

        meta.displayName(
                Component.empty()
        );

        pane.setItemMeta(meta);

        for (int slot = 0;
             slot < inventory.getSize();
             slot++) {

            if (inventory.getItem(slot) == null) {
                inventory.setItem(
                        slot,
                        pane
                );
            }
        }
    }
}