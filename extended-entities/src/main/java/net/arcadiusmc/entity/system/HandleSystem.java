package net.arcadiusmc.entity.system;

import static net.arcadiusmc.entity.system.Transform.FLAG_POS_CHANGED;
import static net.arcadiusmc.entity.system.Transform.FLAG_ROT_CHANGED;
import static net.arcadiusmc.entity.system.Transform.FLAG_VEL_CHANGED;
import static net.arcadiusmc.entity.system.Transform.FLAG_WORLD_CHANGED;
import static net.arcadiusmc.entity.system.Transform.NONE_CHANGED;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntityListener;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import net.arcadiusmc.entity.Entities;
import net.arcadiusmc.entity.util.UuidDataType;
import net.arcadiusmc.events.Events;
import net.arcadiusmc.utils.VanillaAccess;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.util.Vector;
import org.joml.Vector3d;

public class HandleSystem extends IteratingSystem {

  static final NamespacedKey ID_KEY = new NamespacedKey("arcadiusmc", "ecs_entity_id");

  private SystemListener systemListener;
  private BukkitListener bukkitListener;

  private final Map<UUID, Entity> byMinecraftId = new Object2ObjectOpenHashMap<>();

  private Location currentlyProcessing;

  public HandleSystem() {
    super(Family.all(Handle.class, Transform.class).get());
  }

  public Entity getEntityByMinecraftId(UUID uuid) {
    Objects.requireNonNull(uuid, "Null ID");
    return byMinecraftId.get(uuid);
  }

  @Override
  public void addedToEngine(Engine engine) {
    super.addedToEngine(engine);

    IdSystem idSystem = engine.getSystem(IdSystem.class);

    systemListener = new SystemListener(this);
    bukkitListener = new BukkitListener(this, idSystem);

    engine.addEntityListener(systemListener);

    Events.register(bukkitListener);
  }

  @Override
  public void removedFromEngine(Engine engine) {
    super.removedFromEngine(engine);
    engine.removeEntityListener(systemListener);

    Events.unregister(bukkitListener);
  }

  @Override
  protected void processEntity(Entity entity, float deltaTime) {
    Handle handle = entity.getComponent(Handle.class);
    Transform transform = entity.getComponent(Transform.class);

    org.bukkit.entity.Entity bukkit = handle.getEntity();

    if (bukkit == null) {
      return;
    }

    int flags = transform.flags;
    transform.flags = NONE_CHANGED;
    boolean teleportEntity = false;

    if (currentlyProcessing == null) {
      currentlyProcessing = bukkit.getLocation();
    } else {
      bukkit.getLocation(currentlyProcessing);
    }

    if ((flags & FLAG_WORLD_CHANGED) == FLAG_WORLD_CHANGED) {
      currentlyProcessing.setWorld(transform.world);
      teleportEntity = true;
    } else {
      transform.world = currentlyProcessing.getWorld();
    }

    if ((flags & FLAG_ROT_CHANGED) == FLAG_ROT_CHANGED) {
      currentlyProcessing.setPitch(transform.pitch);
      currentlyProcessing.setYaw(transform.yaw);
      teleportEntity = true;
    } else {
      transform.yaw = currentlyProcessing.getYaw();
      transform.pitch = currentlyProcessing.getPitch();
    }

    if ((flags & FLAG_POS_CHANGED) == FLAG_POS_CHANGED) {
      Vector3d pos = transform.getPosition();
      currentlyProcessing.setX(pos.x());
      currentlyProcessing.setY(pos.y());
      currentlyProcessing.setZ(pos.z());
      teleportEntity = true;
    } else {
      VanillaAccess.getPosition(transform.position, bukkit);
    }

    if (teleportEntity) {
      bukkit.teleport(currentlyProcessing);
    }

    if ((flags & FLAG_VEL_CHANGED) == FLAG_VEL_CHANGED) {
      Vector3d vel = transform.getVelocity();
      Vector vector = new Vector(vel.x(), vel.y(), vel.z());
      bukkit.setVelocity(vector);
    } else {
      VanillaAccess.getVelocity(transform.velocity, bukkit);
    }
  }

  static void ensureEntityHasTag(org.bukkit.entity.Entity bukkit, UUID entityId) {
    PersistentDataContainer pdc = bukkit.getPersistentDataContainer();
    pdc.set(ID_KEY, UuidDataType.INSTANCE, entityId);
  }

  @RequiredArgsConstructor
  class BukkitListener implements Listener {

    final HandleSystem handleSystem;
    final IdSystem idSystem;

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityAddToWorld(EntityAddToWorldEvent event) {
      apply(event, false);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityRemoveFromWorld(EntityRemoveFromWorldEvent event) {
      apply(event, true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {
      apply(event, true);
    }

    private void apply(EntityEvent event, boolean remove) {
      org.bukkit.entity.Entity minecraft = event.getEntity();
      PersistentDataContainer pdc = minecraft.getPersistentDataContainer();

      org.bukkit.entity.Entity setAs = remove ? null : minecraft;
      UUID id = pdc.get(ID_KEY, UuidDataType.INSTANCE);

      if (id != null && idSystem != null) {
        Entity entity = idSystem.getEntity(id);

        Entities.apply(entity, Handle.class, handle -> {
          handle.setEntity(setAs);

          if (!remove) {
            handleSystem.byMinecraftId.put(minecraft.getUniqueId(), entity);
          }
        });
      }

      Entity entity;

      if (remove) {
        entity = handleSystem.byMinecraftId.remove(minecraft.getUniqueId());
      } else {
        entity = handleSystem.byMinecraftId.get(minecraft.getUniqueId());
      }

      if (entity == null) {
        return;
      }

      Entities.apply(entity, Handle.class, handle -> handle.setEntity(setAs));
    }
  }

  @RequiredArgsConstructor
  class SystemListener implements EntityListener {

    final HandleSystem handleSystem;

    @Override
    public void entityAdded(Entity entity) {
      Handle handle = entity.getComponent(Handle.class);
      if (handle == null || handle.getMinecraftId() == null) {
        return;
      }

      UUID minecraftId = handle.getMinecraftId();
      org.bukkit.entity.Entity bukkit = Bukkit.getEntity(minecraftId);

      if (bukkit != null) {
        EntityId.apply(entity, id -> ensureEntityHasTag(bukkit, id));
        handle.setEntity(bukkit);
      }

      handleSystem.byMinecraftId.put(minecraftId, entity);
    }

    @Override
    public void entityRemoved(Entity entity) {
      Handle handle = entity.getComponent(Handle.class);
      if (handle == null || handle.getMinecraftId() == null) {
        return;
      }

      UUID minecraftId = handle.getMinecraftId();
      handleSystem.byMinecraftId.remove(minecraftId);

      if (handle.getEntity() != null) {
        org.bukkit.entity.Entity e = handle.getEntity();
        e.getPersistentDataContainer().remove(ID_KEY);
      }

      handle.setEntity(null);
    }
  }
}