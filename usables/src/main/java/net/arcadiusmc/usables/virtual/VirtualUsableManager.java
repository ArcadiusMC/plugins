package net.arcadiusmc.usables.virtual;

import com.google.common.base.Preconditions;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import lombok.Getter;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.registry.Registries;
import net.arcadiusmc.registry.Registry;
import net.arcadiusmc.usables.ObjectType;
import net.arcadiusmc.utils.io.SerializationHelper;
import net.forthecrown.nbt.BinaryTag;
import net.forthecrown.nbt.BinaryTags;
import net.forthecrown.nbt.CompoundTag;
import org.slf4j.Logger;

public class VirtualUsableManager {

  private static final Logger LOGGER = Loggers.getLogger();

  @Getter
  private final Registry<ObjectType<? extends Trigger>> triggerTypes = Registries.newFreezable();

  private final Map<String, VirtualUsable> usableMap = new Object2ObjectOpenHashMap<>();

  private final Map<Class, TriggerSystem> triggerSystems = new Object2ObjectOpenHashMap<>();
  private boolean systemsLocked;

  private final Path file;

  public VirtualUsableManager(Path file) {
    this.file = file;
  }

  public void initialize() {
    Triggers.registerAll(triggerTypes);
    Triggers.manager = this;

    addSystem(BlockTrigger.class, new BlockTriggerSystem());
    addSystem(EntityTrigger.class, new EntityTriggerSystem());
    addSystem(RegionTrigger.class, new RegionTriggerSystem());
  }

  public void lockSystems() {
    systemsLocked = true;
  }

  void ensureSystemsUnlocked() {
    Preconditions.checkState(!systemsLocked, "System adding/removing has been locked");
  }

  public <T extends Trigger> void addSystem(Class<T> type, TriggerSystem<T> system) {
    Objects.requireNonNull(system, "Null system");
    ensureSystemsUnlocked();

    if (triggerSystems.containsKey(type)) {
      throw new IllegalStateException("System under class " + type + " already registered");
    }

    triggerSystems.put(type, system);
    system.initializeSystem(this);
  }

  @SuppressWarnings("unchecked")
  public TriggerSystem<Trigger> getSystem(Trigger trigger) {
    return triggerSystems.get(trigger.getClass());
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  public <T extends Trigger> TriggerSystem<T> getSystem(Class<T> type) {
    return triggerSystems.get(type);
  }

  public VirtualUsable getUsable(String name) {
    return usableMap.get(name);
  }

  void internalAdd(VirtualUsable usable) {
    Objects.requireNonNull(usable, "Null usable");

    if (usableMap.containsKey(usable.getName())) {
      throw new IllegalStateException(
          "Usable with name '" + usable.getName() + "' is already registered"
      );
    }

    usableMap.put(usable.getName(), usable);
    usable.manager = this;
  }

  public void add(VirtualUsable usable) {
    internalAdd(usable);

    if (!systemsLocked) {
      return;
    }

    for (Trigger trigger : usable.getTriggers()) {
      TriggerSystem<Trigger> system = getSystem(trigger);
      system.onTriggerLoaded(usable, trigger);
    }
  }

  VirtualUsable internalRemove(String name) {
    Objects.requireNonNull(name, "Null name");
    VirtualUsable removed = usableMap.get(name);

    if (removed == null) {
      return null;
    }

    usableMap.remove(name);
    removed.manager = null;

    return removed;
  }

  public void remove(String name) {
    VirtualUsable removed = internalRemove(name);

    if (removed == null || !systemsLocked) {
      return;
    }

    for (Trigger trigger : removed.getTriggers()) {
      TriggerSystem<Trigger> system = getSystem(trigger);
      system.onTriggerRemove(removed, trigger);
    }
  }

  public boolean containsUsable(String name) {
    return usableMap.containsKey(name);
  }

  public Set<String> getNames() {
    return Collections.unmodifiableSet(usableMap.keySet());
  }

  public void save() {
    SerializationHelper.writeTagFile(file, tag -> {
      for (Entry<String, VirtualUsable> entry : usableMap.entrySet()) {
        CompoundTag usableTag = BinaryTags.compoundTag();
        entry.getValue().save(usableTag);
        tag.put(entry.getKey(), usableTag);
      }
    });
  }

  public void load() {
    usableMap.clear();

    SerializationHelper.readTagFile(file, tag -> {
      for (Entry<String, BinaryTag> entry : tag.entrySet()) {
        String key = entry.getKey();
        BinaryTag uTag = entry.getValue();

        if (!Registries.isValidKey(key)) {
          LOGGER.error("Invalid virtual usable name '{}'", key);
          continue;
        }

        if (!uTag.isCompound()) {
          LOGGER.error("Usable {} had non-compound tag in data", key);
          return;
        }

        VirtualUsable usable = new VirtualUsable(key);
        usable.load(uTag.asCompound());

        add(usable);
      }
    });
  }
}
