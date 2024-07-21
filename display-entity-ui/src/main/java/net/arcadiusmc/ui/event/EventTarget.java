package net.arcadiusmc.ui.event;

import java.util.List;

public interface EventTarget extends Listenable {

  EventListeners getListeners();

  void dispatchEvent(Event event);

  @Override
  default void addEventListener(String eventType, EventListener listener) {
    getListeners().addEventListener(eventType, listener);
  }

  @Override
  default boolean removeEventListener(String eventType, EventListener listener) {
    return getListeners().removeEventListener(eventType, listener);
  }

  @Override
  default void clearEventListeners() {
    getListeners().clearEventListeners();
  }

  @Override
  default void clearEventListeners(String eventType) {
    getListeners().clearEventListeners(eventType);
  }

  @Override
  default List<EventListener> getListeners(String eventType) {
    return getListeners().getListeners(eventType);
  }
}
