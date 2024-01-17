package net.arcadiusmc.utils.collision;

import static net.arcadiusmc.utils.PluginUtil.getCallingPlugin;

import net.arcadiusmc.utils.PluginUtil;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public final class CollisionSystems {
  private CollisionSystems() {}

  public static <T> PlayerCollisionSystem<T> createSystem(CollisionListener<Player, T> listener) {
    var plugin = PluginUtil.getCallingPlugin();
    return new PlayerCollisionSystem<>(new WorldChunkMap<>(), plugin, listener);
  }

  public static <T> PlayerCollisionSystem<T> createSystem(
      CollisionLookup<T> map,
      CollisionListener<Player, T> listener
  ) {
    var plugin = PluginUtil.getCallingPlugin();
    return new PlayerCollisionSystem<>(map, plugin, listener);
  }

  public static <T> EntityCollisionSystem<T> createEntitySystem(CollisionListener<Entity, T> listener) {
    var plugin = PluginUtil.getCallingPlugin();
    return new EntityCollisionSystem<>(new WorldChunkMap<>(), plugin, listener);
  }

  public static <T> EntityCollisionSystem<T> createEntitySystem(
      CollisionLookup<T> map,
      CollisionListener<Entity, T> listener
  ) {
    var plugin = PluginUtil.getCallingPlugin();
    return new EntityCollisionSystem<>(map, plugin, listener);
  }
}
