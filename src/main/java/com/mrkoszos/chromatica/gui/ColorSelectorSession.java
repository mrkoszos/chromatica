package com.mrkoszos.chromatica.gui;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ColorSelectorSession {

  private final Map<UUID, ColorSelectionContext> sessions = new HashMap<>();

  public void set(UUID uuid, ColorSelectionContext context) {
    sessions.put(uuid, context);
  }

  public ColorSelectionContext get(UUID uuid) {
    return sessions.get(uuid);
  }

  public void remove(UUID uuid) {
    sessions.remove(uuid);
  }
}
