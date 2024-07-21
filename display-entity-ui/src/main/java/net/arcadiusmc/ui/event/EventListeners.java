package net.arcadiusmc.ui.event;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class EventListeners implements Listenable {

  private final Map<String, ObjectList<EventListener>> listeners = new Object2ObjectOpenHashMap<>();

  @Override
  public void clearEventListeners(String eventType) {
    Objects.requireNonNull(eventType, "Null event type");
    listeners.remove(eventType);
  }

  @Override
  public void clearEventListeners() {
    listeners.clear();
  }

  @Override
  public boolean removeEventListener(String eventType, EventListener listener) {
    Objects.requireNonNull(eventType, "Null event type");
    Objects.requireNonNull(listener, "Null event listener");

    List<EventListener> list = listeners.get(eventType);

    if (list == null || list.isEmpty()) {
      return false;
    }

    return list.remove(listener);
  }

  @Override
  public void addEventListener(String eventType, EventListener listener) {
    Objects.requireNonNull(eventType, "Null event type");
    Objects.requireNonNull(listener, "Null event listener");

    List<EventListener> list = listeners.computeIfAbsent(eventType, s -> new ObjectArrayList<>());
    list.add(listener);
  }

  @Override
  public List<EventListener> getListeners(String eventType) {
    return listeners.getOrDefault(eventType, ObjectLists.emptyList());
  }
}
