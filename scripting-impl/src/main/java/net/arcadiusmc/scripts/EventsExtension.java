package net.arcadiusmc.scripts;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;

public class EventsExtension {

  private final RhinoScript script;
  private final Plugin plugin;
  private final List<ExecutorWrapper> wrappers = new ObjectArrayList<>();

  public EventsExtension(Plugin plugin, RhinoScript script) {
    this.plugin = plugin;
    this.script = script;
  }

  /* ---------------------------- REGISTRATION ---------------------------- */

  public <E extends Event> void register(Class<E> eventClass, Consumer<E> mirror) {
    register(eventClass, mirror, true);
  }

  public <E extends Event> void register(
      Class<E> eventClass,
      Consumer<E> mirror,
      String priority
  ) {
    register(eventClass, mirror, true, priority);
  }

  public <E extends Event> void register(
      Class<E> eventClass,
      Consumer<E> mirror,
      boolean ignoreCancelled
  ) {
    register(eventClass, mirror, ignoreCancelled, "normal");
  }

  public <E extends Event> void register(
      Class<E> eventClass,
      Consumer<E> mirror,
      boolean ignoreCancelled,
      String priorityName
  ) {
    Objects.requireNonNull(eventClass, "Null event class");
    Objects.requireNonNull(mirror, "Null function");
    Objects.requireNonNull(priorityName, "Null priority");

    ExecutorWrapper<E> wrapper = new ExecutorWrapper<>(eventClass, mirror, ignoreCancelled);

    EventPriority priority = switch (priorityName.toLowerCase()) {
      case "lowest" -> EventPriority.LOWEST;
      case "low" -> EventPriority.LOW;
      case "high" -> EventPriority.HIGH;
      case "highest" -> EventPriority.HIGHEST;
      case "monitor" -> EventPriority.MONITOR;
      default -> EventPriority.NORMAL;
    };

    PluginManager manager = Bukkit.getPluginManager();
    manager.registerEvent(eventClass, wrapper, priority, wrapper, plugin, ignoreCancelled);

    wrappers.add(wrapper);
  }

  /* --------------------------- UNREGISTRATION --------------------------- */

  public void unregisterAll() {
    wrappers.forEach(HandlerList::unregisterAll);
    wrappers.clear();
  }

  public void unregisterFrom(Class<? extends Event> eventClass) {
    Objects.requireNonNull(eventClass);

    wrappers.removeIf(wrapper -> {
      if (wrapper.type == eventClass) {
        HandlerList.unregisterAll(wrapper);
        return true;
      }

      return false;
    });
  }

  void onScriptClose() {
    unregisterAll();
  }

  @Getter
  @RequiredArgsConstructor
  class ExecutorWrapper<E extends Event> implements EventExecutor, Listener {

    private final Class<E> type;
    private final Consumer<E> mirror;
    private final boolean ignoreCancelled;

    @Override
    public void execute(@NotNull Listener listener, @NotNull Event event) {
      if (!type.isInstance(event)) {
        return;
      }

      if (event instanceof Cancellable c && c.isCancelled() && ignoreCancelled) {
        return;
      }

      mirror.accept((E) event);
    }
  }
}