package com.mrkoszos.chromatica.gui;

import com.mrkoszos.chromatica.Chromatica;
import org.bukkit.inventory.Inventory;

public final class GuiUtils {

    private GuiUtils() {
    }

    /**
     * Feltölti az inventory üres slotjait a plugin egységes háttér-rendszerével
     * (lásd BackgroundManager / BackgroundPattern). A már elhelyezett
     * (funkcionális) itemeket nem érinti, ezért ezt minden menüben a
     * funkcionális itemek elhelyezése UTÁN kell hívni.
     */
    public static void fillEmptySlots(Inventory inventory, Chromatica plugin) {
        plugin.getBackgroundManager().applyBackground(inventory);
    }

    /**
     * Csak a keretet (legfelső/legalsó sor + bal/jobb szélső oszlop)
     * tölti fel háttér-mintázattal; minden belső cellát üresen hagy. Lásd
     * {@link com.mrkoszos.chromatica.manager.BackgroundManager#applyBorderOnly(Inventory)}.
     */
    public static void fillBorderRowsOnly(Inventory inventory, Chromatica plugin) {
        plugin.getBackgroundManager().applyBorderOnly(inventory);
    }
}