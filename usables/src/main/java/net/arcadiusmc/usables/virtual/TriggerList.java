package net.arcadiusmc.usables.virtual;

import com.mojang.serialization.Dynamic;
import java.util.function.BiConsumer;
import net.arcadiusmc.registry.Registry;
import net.arcadiusmc.usables.ObjectType;
import net.arcadiusmc.usables.list.ComponentsArray;

public class TriggerList extends ComponentsArray<Trigger> {

  static final Trigger[] EMPTY_ARRAY = new Trigger[0];

  final VirtualUsable usable;
  boolean suppressUpdates = false;

  public TriggerList(VirtualUsable usable) {
    super(Trigger.class);
    this.usable = usable;
  }

  @Override
  @SuppressWarnings({"unchecked", "rawtypes"})
  public Registry<ObjectType<Trigger>> getRegistry() {
    return (Registry) usable.getManager().getTriggerTypes();
  }

  void runSystem(Trigger trigger, BiConsumer<TriggerSystem<Trigger>, Trigger> consumer) {
    if (suppressUpdates || usable.getManager() == null) {
      return;
    }

    VirtualUsableManager manager = usable.getManager();
    TriggerSystem<Trigger> system = manager.getSystem(trigger);

    if (system != null) {
      consumer.accept(system, trigger);
    }
  }

  @Override
  public void add(Trigger value, int index) {
    super.add(value, index);
    runSystem(value, (system, trigger) -> system.onTriggerAdd(usable, trigger));
  }

  @Override
  public Trigger remove(int index) {
    Trigger value = super.remove(index);
    runSystem(value, (system, trigger) -> system.onTriggerRemove(usable, trigger));
    return value;
  }

  @Override
  public void set(int index, Trigger value) {
    Trigger existing = get(index);
    runSystem(existing, (system, trigger) -> system.onTriggerRemove(usable, trigger));

    super.set(index, value);
    runSystem(value, (system, trigger) -> system.onTriggerAdd(usable, trigger));
  }

  @Override
  public void clear() {
    for (Trigger value : this) {
      runSystem(value, (system, trigger) -> system.onTriggerRemove(usable, trigger));
    }

    super.clear();
  }

  @Override
  public <S> void load(Dynamic<S> dynamic) {
    suppressUpdates = true;

    try {
      super.load(dynamic);
    } finally {
      suppressUpdates = false;
    }
  }
}
