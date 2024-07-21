package net.arcadiusmc.ui.event;

import java.util.List;

public record EventExecutionTask(Event event, List<EventListener> listeners) implements Runnable {

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
