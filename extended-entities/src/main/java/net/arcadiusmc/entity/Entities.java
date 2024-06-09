package net.arcadiusmc.entity;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.Getter;
import net.arcadiusmc.entity.system.EntityId;
import net.arcadiusmc.entity.system.HandleSystem;
import net.arcadiusmc.entity.system.Transform;
import org.bukkit.Location;

public class Entities {

  public static float timeScale = 1.0f;

  @Getter
  static Engine engine;

  public static Entity create() {
    return create(UUID.randomUUID(), null);
  }

  public static Entity create(UUID uuid, Location location) {
    Entity entity = new Entity();

    entity.add(new EntityId(uuid));
    entity.add(new Transform(location));

    return entity;
  }

  public static <T extends Component> T apply(Entity entity, Class<T> type, Consumer<T> consumer) {
    if (entity == null) {
      return null;
    }

    T value = entity.getComponent(type);

    if (value == null) {
      return null;
    }

    consumer.accept(value);
    return value;
  }

  public static Entity fromBukkit(org.bukkit.entity.Entity entity) {
    if (engine == null) {
      return null;
    }

    HandleSystem system = engine.getSystem(HandleSystem.class);
    if (system == null) {
      return null;
    }

    return system.getEntityByMinecraftId(entity.getUniqueId());
  }
}
