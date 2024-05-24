package net.arcadiusmc.items.guns;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.UUID;
import net.arcadiusmc.utils.VanillaAccess;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.joml.Vector3d;

public class PlayerMoveSpeeds extends TickingObject {
  private PlayerMoveSpeeds() {}

  public static final PlayerMoveSpeeds SPEEDS = new PlayerMoveSpeeds();

  private final Map<UUID, MovementEntry> entryMap = new HashMap<>();

  public void addPlayer(Player player) {
    entryMap.computeIfAbsent(player.getUniqueId(), uuid -> {
      MovementEntry entry = new MovementEntry();
      Location loc = player.getLocation();
      entry.set(loc);
      return entry;
    });
  }

  public void removePlayer(Player player) {
    entryMap.remove(player.getUniqueId());
  }

  public Vector3d getMovement(Player player) {
    MovementEntry entry = entryMap.get(player.getUniqueId());

    if (entry == null) {
      return new Vector3d();
    }

    return new Vector3d(entry.distance);
  }

  @Override
  public void tick() {
    for (Entry<UUID, MovementEntry> entry : entryMap.entrySet()) {
      Player player = Bukkit.getPlayer(entry.getKey());

      if (player == null) {
        continue;
      }

      MovementEntry e = entry.getValue();
      Location loc = player.getLocation();

      if (!Objects.equals(e.lastWorld, loc.getWorld())) {
        e.set(loc);
        continue;
      }

      e.lastPosition.set(e.position);
      VanillaAccess.getPosition(e.position, player);

      e.position.sub(e.lastPosition, e.distance);
    }
  }

  private class MovementEntry {

    private final Vector3d lastPosition = new Vector3d();
    private final Vector3d position = new Vector3d();

    private final Vector3d distance = new Vector3d();

    private World lastWorld;

    public void set(Location loc) {
      lastWorld = loc.getWorld();

      lastPosition.x = loc.getX();
      lastPosition.y = loc.getY();
      lastPosition.z = loc.getZ();

      position.x = loc.getX();
      position.y = loc.getY();
      position.z = loc.getZ();

      distance.x = 0;
      distance.y = 0;
      distance.z = 0;
    }
  }
}
