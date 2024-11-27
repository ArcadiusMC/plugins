package net.arcadiusmc.entity.system;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntityListener;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.signals.Listener;
import com.badlogic.ashley.signals.Signal;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.entity.persistence.PersistentTypes;
import net.arcadiusmc.utils.io.ExtraCodecs;
import org.slf4j.Logger;

public class IdSystem extends EntitySystem {

  private static final Logger LOGGER = Loggers.getLogger();

  private final Map<UUID, Entity> byId = new Object2ObjectOpenHashMap<>();
  private final Map<Entity, UUID> byEntity = new Object2ObjectOpenHashMap<>();

  private final IdRegisteringListener listener = new IdRegisteringListener();
  private final ComponentListener componentListener = new ComponentListener();

  public IdSystem() {
    PersistentTypes.registerComponent("id", EntityId.class,
        ExtraCodecs.UUID_CODEC.xmap(EntityId::new, EntityId::getId)
    );
  }

  public Entity getEntity(UUID uuid) {
    return byId.get(uuid);
  }

  public UUID getId(Entity entity) {
    return byEntity.get(entity);
  }

  @Override
  public void addedToEngine(Engine engine) {
    engine.addEntityListener(listener);
  }

  @Override
  public void removedFromEngine(Engine engine) {
    engine.removeEntityListener(listener);
  }

  class ComponentListener implements Listener<Entity> {

    @Override
    public void receive(Signal<Entity> signal, Entity object) {
      EntityId id = object.getComponent(EntityId.class);

      if (id == null) {
        UUID existing = byEntity.remove(object);

        if (existing != null) {
          byId.remove(existing);
        }

        return;
      }

      UUID uuid = id.getId();
      Entity found = byId.get(uuid);

      if (found == null) {
        byEntity.put(object, uuid);
        byId.put(uuid, object);
      } else if (!Objects.equals(found, object)) {
        LOGGER.warn("Found entities with conflicting IDs, id={}", uuid);
      }
    }
  }

  class IdRegisteringListener implements EntityListener {

    @Override
    public void entityAdded(Entity entity) {
      EntityId id = entity.getComponent(EntityId.class);
      if (id == null) {
        return;

      }

      Entity existing = byId.get(id.id);

      if (existing != null && !Objects.equals(existing, entity)) {
        LOGGER.warn("Entity add with duplicate ID, id={}", id.id);
      }

      entity.componentAdded.add(componentListener);
      entity.componentRemoved.add(componentListener);

      byId.put(id.id, entity);
    }

    @Override
    public void entityRemoved(Entity entity) {
      EntityId id = entity.getComponent(EntityId.class);
      if (id == null) {
        return;
      }

      byId.remove(id.id);
    }
  }
}
