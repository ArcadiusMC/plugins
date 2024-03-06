package net.arcadiusmc.waypoints;

import java.time.Duration;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.math.vector.Vector2i;
import org.spongepowered.math.vector.Vector3i;

@ConfigSerializable
public class WaypointConfig {

  public Vector3i playerWaypointSize = Vector3i.from(5);
  public Vector3i adminWaypointSize = Vector3i.from(5);
  public String spawnWaypoint = "Hazelguard";

  public boolean hulkSmashPoles = true;

  public Duration waypointDeletionDelay = Duration.ofDays(7);
  public Duration validInviteTime = Duration.ofMinutes(10);

  public int maxNameLength = 20;

  public int discoveryRange = 50;

  public Vector2i wildernessWaypointGrid = Vector2i.from(500);

  public String[] autoGenWorlds = { "world" };

  public String[] bannedNames = {};

  Duration autoSaveInterval = Duration.ofMinutes(30);
}