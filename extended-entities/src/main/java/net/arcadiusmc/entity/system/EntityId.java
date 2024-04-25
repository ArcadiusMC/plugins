package net.arcadiusmc.entity.system;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.Getter;

@Getter
public class EntityId implements Component {
  final UUID id;

  public EntityId(UUID id) {
    Objects.requireNonNull(id, "Null Id");
    this.id = id;
  }

  public static void apply(Entity entity, Consumer<UUID> o) {
    EntityId component = entity.getComponent(EntityId.class);

    if (component == null) {
      return;
    }

    o.accept(component.id);
  }
}
