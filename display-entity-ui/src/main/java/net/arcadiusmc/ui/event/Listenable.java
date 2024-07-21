package net.arcadiusmc.ui.event;

import java.util.List;

public interface Listenable {

  void addEventListener(String eventType, EventListener listener);

  boolean removeEventListener(String eventType, EventListener listener);

  void clearEventListeners();

  void clearEventListeners(String eventType);

  List<EventListener> getListeners(String eventType);
}
