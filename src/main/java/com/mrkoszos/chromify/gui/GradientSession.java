package com.mrkoszos.chromify.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GradientSession {

    private final Map<UUID, List<String>> sessions =
            new HashMap<>();

    public void start(
            UUID uuid,
            List<String> colors
    ) {
        sessions.put(
                uuid,
                new ArrayList<>(colors)
        );
    }

    public List<String> get(UUID uuid) {
        return sessions.get(uuid);
    }

    public void setColor(
            UUID uuid,
            int index,
            String color
    ) {
        List<String> colors =
                sessions.get(uuid);

        if (colors == null) {
            return;
        }

        while (colors.size() <= index) {
            colors.add("#FFFFFF");
        }

        colors.set(index, color);
    }

    public void remove(UUID uuid) {
        sessions.remove(uuid);
    }
}