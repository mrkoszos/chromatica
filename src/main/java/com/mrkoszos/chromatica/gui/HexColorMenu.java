package com.mrkoszos.chromatica.gui;

import com.mrkoszos.chromatica.Chromatica;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;
import java.util.function.Consumer;

public class HexColorMenu {

    private final Chromatica plugin;

    public HexColorMenu(Chromatica plugin) {
        this.plugin = plugin;
    }

    public void open(
            Player player,
            Consumer<String> onColor
    ) {

        new AnvilGUI.Builder()
                .plugin(plugin)
                .title("Custom Color")

                .itemLeft(createInputItem())

                .text("#FFFFFF")

                .onClick((slot, snapshot) -> {

                    if (slot != AnvilGUI.Slot.OUTPUT) {
                        return Collections.emptyList();
                    }

                    String input = snapshot
                            .getText()
                            .trim();

                    if (!isValidHex(input)) {

                        return Collections.singletonList(
                                AnvilGUI.ResponseAction
                                        .replaceInputText(
                                                "#FFFFFF"
                                        )
                        );
                    }

                    onColor.accept(input);

                    return Collections.singletonList(
                            AnvilGUI.ResponseAction.close()
                    );
                })

                .open(player);
    }

    private ItemStack createInputItem() {

        ItemStack item =
                new ItemStack(Material.ENDER_EYE);

        ItemMeta meta =
                item.getItemMeta();

        meta.displayName(
                Component.text("Enter HEX color")
        );

        item.setItemMeta(meta);

        return item;
    }

    private boolean isValidHex(String input) {

        return input.matches(
                "^#[0-9a-fA-F]{6}$"
        );
    }
}