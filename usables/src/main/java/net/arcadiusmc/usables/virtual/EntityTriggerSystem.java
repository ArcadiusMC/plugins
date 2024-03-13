package net.arcadiusmc.usables.virtual;

import static net.arcadiusmc.usables.virtual.EntityTriggerSystem.LOGGER;
import static net.arcadiusmc.usables.virtual.EntityTriggerSystem.TRIGGER_KEY;
import static net.arcadiusmc.usables.virtual.EntityTriggerSystem.newTriggerMap;

import java.util.List;
import java.util.Optional;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.events.Events;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.slf4j.Logger;

public class EntityTriggerSystem implements TriggerSystem<EntityTrigger> {

  static final Logger LOGGER = Loggers.getLogger();

  static final NamespacedKey TRIGGER_KEY = new NamespacedKey("usables", "entity_triggers");

  EntityTriggerListener listener;

  static TriggerMap<EntityAction> newTriggerMap() {
    return new TriggerMap<>(EntityAction.CODEC);
  }

  @Override
  public void initializeSystem(VirtualUsableManager manager) {
    listener = new EntityTriggerListener(manager);
    Events.register(listener);
  }

  @Override
  public void onTriggerLoaded(VirtualUsable usable, EntityTrigger trigger) {

  }

  @Override
  public void onTriggerAdd(VirtualUsable usable, EntityTrigger trigger) {
    updateEntityAttachment(usable, trigger, true);
  }

  @Override
  public void onTriggerRemove(VirtualUsable usable, EntityTrigger trigger) {
    updateEntityAttachment(usable, trigger, false);
  }

  private void updateEntityAttachment(VirtualUsable usable, EntityTrigger trigger, boolean add) {
    Optional<Entity> entityOpt = trigger.getEntity();

    if (entityOpt.isEmpty()) {
      LOGGER.warn("Couldn't find entity to update attachment trigger, usable={}, ref={}",
          usable.getName(), trigger.getReference()
      );
      return;
    }

    Entity entity = entityOpt.get();

    TriggerMap<EntityAction> map = listener.loadMap(entity);

    if (add) {
      map.add(trigger.getAction(), usable.getName());
    } else {
      map.remove(trigger.getAction(), usable.getName());
    }

    listener.saveMap(map, entity);
  }
}

class EntityTriggerListener implements Listener {

  private final VirtualUsableManager manager;

  public EntityTriggerListener(VirtualUsableManager manager) {
    this.manager = manager;
  }

  TriggerMap<EntityAction> loadMap(Entity entity) {
    var map = newTriggerMap();

    map.loadFromContainer(entity.getPersistentDataContainer(), TRIGGER_KEY)
        .mapError(s -> "Failed to load triggers inside entity " + entity + ": " + s)
        .resultOrPartial(LOGGER::error);

    return map;
  }

  void saveMap(TriggerMap<EntityAction> map, Entity entity) {
    map.saveToContainer(entity.getPersistentDataContainer(), TRIGGER_KEY)
        .mapError(s -> "Failed to save triggers inside entity " + entity + ": " + s)
        .resultOrPartial(LOGGER::error);
  }

  @EventHandler(ignoreCancelled = true)
  public void onEntityDeath(EntityDeathEvent event) {
    LivingEntity entity = event.getEntity();
    Player killer = entity.getKiller();

    if (killer == null) {
      return;
    }

    TriggerMap<EntityAction> map = loadMap(entity);
    List<String> refs = map.get(EntityAction.ON_ENTITY_KILL);

    if (refs.isEmpty()) {
      return;
    }

    Triggers.runReferences(refs, manager, killer, event, null, null);
  }

  @EventHandler(ignoreCancelled = true)
  public void onEntityInteract(PlayerInteractEntityEvent event) {
    Entity entity = event.getRightClicked();
    Player player = event.getPlayer();

    TriggerMap<EntityAction> map = loadMap(entity);
    List<String> refs = map.get(EntityAction.ON_ENTITY_INTERACT);

    if (refs.isEmpty()) {
      return;
    }

    Triggers.runReferences(refs, manager, player, event, null, null);
  }


  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
    if (!(event.getRightClicked() instanceof ArmorStand)) {
      return;
    }
    onEntityInteract(event);
  }
}