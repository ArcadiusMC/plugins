package net.arcadiusmc.ui.event;

public interface EventListener {

  void onEvent(Event event);

  interface Typed<T extends Event> extends EventListener {

    void onEventFired(T event);

    @Override
    default void onEvent(Event event) {
      onEventFired((T) event);
    }
  }
}
