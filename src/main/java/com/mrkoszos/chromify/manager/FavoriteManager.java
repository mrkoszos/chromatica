package com.mrkoszos.chromify.manager;

import com.mrkoszos.chromify.model.NameStyle;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FavoriteManager {

    public static final int MAX_FAVORITES = 5;

    /*
     * Fix méretű tömb slotonként, nem lista - így egy adott index
     * (pl. slot 2) mindig ugyanazt a kedvencet jelenti, még akkor is,
     * ha egy másik slotot közben törölnek.
     */
    private final Map<UUID, NameStyle[]> favorites = new HashMap<>();

    private NameStyle[] getArray(UUID uuid) {
        return favorites.computeIfAbsent(uuid, ignored -> new NameStyle[MAX_FAVORITES]);
    }

    public NameStyle[] getAll(UUID uuid) {
        return getArray(uuid);
    }

    public NameStyle getFavorite(UUID uuid, int index) {

        if (index < 0 || index >= MAX_FAVORITES) {
            return null;
        }

        return getArray(uuid)[index];
    }

    public boolean setFavorite(UUID uuid, int index, NameStyle style) {

        if (index < 0 || index >= MAX_FAVORITES) {
            return false;
        }

        getArray(uuid)[index] = style.copy();

        return true;
    }

    public void removeFavorite(UUID uuid, int index) {

        if (index < 0 || index >= MAX_FAVORITES) {
            return;
        }

        getArray(uuid)[index] = null;
    }

    public boolean isEmpty(UUID uuid, int index) {
        return getFavorite(uuid, index) == null;
    }
}
