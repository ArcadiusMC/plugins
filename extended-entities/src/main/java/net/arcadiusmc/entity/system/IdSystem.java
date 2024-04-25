package net.arcadiusmc.entity.system;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntityListener;
import com.badlogic.ashley.core.EntitySystem;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Map;
import java.util.UUID;

public class IdSystem extends EntitySystem {

  private final Map<UUID, Entity> byId = new Object2ObjectOpenHashMap<>();

  private final IdRegisteringListener listener = new IdRegisteringListener();

  public Entity getEntity(UUID uuid) {
    return byId.get(uuid);
  }

  @Override
  public void addedToEngine(Engine engine) {
    engine.addEntityListener(listener);
  }

  @Override
  public void removedFromEngine(Engine engine) {
    engine.removeEntityListener(listener);
  }

  class IdRegisteringListener implements EntityListener {

    @Override
    public void entityAdded(Entity entity) {
      EntityId id = entity.getComponent(EntityId.class);
      if (id == null) {
        return;
      }

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
