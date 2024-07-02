package net.arcadiusmc.ui.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;

public class EventListeners {

  private final Map<String, List<EventListener>> listeners = new HashMap<>();
  private final Executor executor;

  public EventListeners(Executor executor) {
    this.executor = executor;
  }

  public void clearListeners(String eventType) {
    Objects.requireNonNull(eventType, "Null event type");
    listeners.remove(eventType);
  }

  public void clearListeners() {
    listeners.clear();
  }

  public boolean removeEventListener(String eventType, EventListener listener) {
    Objects.requireNonNull(eventType, "Null event type");
    Objects.requireNonNull(listener, "Null event listener");

    List<EventListener> list = listeners.get(eventType);

    if (list == null || list.isEmpty()) {
      return false;
    }

    return list.remove(listener);
  }

  public void addEventListener(String eventType, EventListener listener) {
    Objects.requireNonNull(eventType, "Null event type");
    Objects.requireNonNull(listener, "Null event listener");

    List<EventListener> list = listeners.computeIfAbsent(eventType, s -> new ArrayList<>());
    list.add(listener);
  }

  public void fireEvent(Event event) {
    List<EventListener> listeners = this.listeners.get(event.getEventType());

    if (listeners == null || listeners.isEmpty()) {
      return;
    }

    executor.execute(new EventExecutionTask(event, listeners));
  }

  record EventExecutionTask(Event event, List<EventListener> listeners) implements Runnable {

    @Override
    public void run() {
      for (int i = 0; i < listeners.size(); i++) {
        EventListener listener = listeners.get(i);

        listener.onEvent(event);

        if (event.propagationStopped()) {
          return;
        }
      }
    }
  }
}
