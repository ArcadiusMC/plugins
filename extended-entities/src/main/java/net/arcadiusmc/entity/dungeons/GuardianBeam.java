package net.arcadiusmc.entity.dungeons;

import static net.arcadiusmc.entity.dungeons.ShulkerGuardian.PARTICLE_DISTANCE;
import static net.arcadiusmc.entity.dungeons.ShulkerGuardian.TARGET_SEARCH_RADIUS;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.destroystokyo.paper.ParticleBuilder;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.entity.persistence.PersistentTypes;
import net.arcadiusmc.entity.system.Transform;
import net.arcadiusmc.utils.VanillaAccess;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.joml.Vector3d;
import org.slf4j.Logger;

public class GuardianBeam extends IteratingSystem {

  private static final Logger LOGGER = Loggers.getLogger();

  static final double PARRY_DAMAGE_MULTIPLIER = 1.75;
  static final double PARRY_SPEED_MULTIPLIER = 1.75;
  static final double HIT_RANGE = 0.5;
  static final double HEAD_DRAW_INTERVAL_SECONDS = 0.25;
  static final double TRAIL_DRAW_INTERVAL_SECONDS = 0.5;
  static final double MIN_DISTANCE = 1.5;
  static final double MIN_DISTANCE_SQ = Math.pow(MIN_DISTANCE, 2);
  static final double DEFAULT_MAX_DIST_SQ = Math.pow(30, 2);

  static final float PARTICLE_SIZE = 3f;
  static final Color PARTICLE_COLOR = Color.RED;

  private final BoundingBox box = new BoundingBox();
  private final BoundingBox searchRange = new BoundingBox();
  private final Random random = new Random();

  public GuardianBeam() {
    super(Family.all(GuardianBeamData.class, Transform.class).get());

    PersistentTypes.registerComponent("guardian_beam", GuardianBeamData.class);
  }

  @Override
  protected void processEntity(Entity entity, float deltaTime) {
    Transform transform = entity.getComponent(Transform.class);
    GuardianBeamData data = entity.getComponent(GuardianBeamData.class);

    if (data.origin == null) {
      data.origin = new Vector3d(transform.getPosition());
    }

    data.sinceTrailDraw += deltaTime;
    data.sinceHeadDraw += deltaTime;

    if (data.sinceTrailDraw >= TRAIL_DRAW_INTERVAL_SECONDS) {
      drawTrail(transform, data);
    }
    if (data.sinceHeadDraw >= HEAD_DRAW_INTERVAL_SECONDS) {
      drawHead(transform);
    }

    transform.getPosition().add(transform.getVelocity());

    double lengthSq = data.origin.distanceSquared(transform.getPosition());
    if (lengthSq <= MIN_DISTANCE_SQ) {
      return;
    }

    Vector3d pos = transform.getPosition();
    World world = transform.getWorld();

    box.resize(
        pos.x - HIT_RANGE,
        pos.y - HIT_RANGE,
        pos.z - HIT_RANGE,

        pos.x + HIT_RANGE,
        pos.y + HIT_RANGE,
        pos.z + HIT_RANGE
    );

    Collection<org.bukkit.entity.Entity> entities = world.getNearbyEntities(box);

    if (!entities.isEmpty()) {
      for (org.bukkit.entity.Entity entity1 : entities) {
        if (entity1 instanceof Player player && player.isBlocking()) {
          reflect(transform, data, player);
          return;
        }

        detonate(transform, data, entity);
        return;
      }
    }

    if (data.collideWithBlocks && overlapsCollidableBlocks(world)) {
      detonate(transform, data, entity);
      return;
    }

    double distSq = transform.getPosition().distanceSquared(data.origin);

    if (data.maxTravelDistanceSq == 0) {
      data.maxTravelDistanceSq = DEFAULT_MAX_DIST_SQ;
    }

    if (distSq >= data.maxTravelDistanceSq) {
      detonate(transform, data, entity);
    }
  }

  boolean overlapsCollidableBlocks(World world) {
    int minX = (int) box.getMinX();
    int minY = (int) box.getMinY();
    int minZ = (int) box.getMinZ();

    int maxX = (int) box.getMaxX();
    int maxY = (int) box.getMaxY();
    int maxZ = (int) box.getMaxZ();

    for (int x = minX; x <= maxX; x++) {
      for (int y = minY; y <= maxY; y++) {
        for (int z = minZ; z <= maxZ; z++) {
          Block block = world.getBlockAt(x, y, z);

          if (!block.isCollidable()) {
            continue;
          }

          BoundingBox shape = block.getBoundingBox();

          if (shape.overlaps(box)) {
            return true;
          }
        }
      }
    }

    return false;
  }

  void detonate(Transform transform, GuardianBeamData data, Entity entity) {
    World world = transform.getWorld();
    Vector3d pos = transform.getPosition();

    double explosionRadius = data.damage;

    world.createExplosion(pos.x, pos.y, pos.z, (float) explosionRadius, true, true);

    getEngine().removeEntity(entity);
  }

  void reflect(Transform transform, GuardianBeamData data, Player player) {
    Vector3d pos = transform.getPosition();
    World world = transform.getWorld();

    searchRange.resize(
        pos.x - TARGET_SEARCH_RADIUS,
        pos.y - TARGET_SEARCH_RADIUS,
        pos.z - TARGET_SEARCH_RADIUS,

        pos.x + TARGET_SEARCH_RADIUS,
        pos.y + TARGET_SEARCH_RADIUS,
        pos.z + TARGET_SEARCH_RADIUS
    );

    List<org.bukkit.entity.Entity> nearby
        = world.getNearbyEntities(searchRange, e -> e instanceof Mob)
        .stream()
        .toList();

    Vector3d newVelocity;
    double maxDistSq;

    if (nearby.isEmpty()) {
      newVelocity = new Vector3d();
      Vector dir = player.getLocation().getDirection();

      newVelocity.x = dir.getX();
      newVelocity.y = dir.getY();
      newVelocity.z = dir.getZ();

      maxDistSq = DEFAULT_MAX_DIST_SQ;
    } else {
      org.bukkit.entity.Entity random = nearby.get(this.random.nextInt(nearby.size()));

      Vector3d entityPos = new Vector3d();
      VanillaAccess.getPosition(entityPos, random);
      entityPos.add(0, random.getHeight() / 2, 0);

      Vector3d dif = new Vector3d();
      pos.sub(entityPos, dif);
      dif.normalize();

      newVelocity = dif;
      maxDistSq = pos.distanceSquared(entityPos);
    }

    double currentLength = transform.getVelocity().length();
    newVelocity.mul(currentLength * PARRY_SPEED_MULTIPLIER);

    data.damage *= PARRY_DAMAGE_MULTIPLIER;
    data.maxTravelDistanceSq = maxDistSq;

    if (data.origin == null) {
      data.origin = new Vector3d(pos);
    } else {
      data.origin.set(pos);
    }

    transform.setVelocity(newVelocity);
  }

  void drawTrail(Transform transform, GuardianBeamData data) {
    Vector3d trailEnd = data.lastTrailDraw;
    World world = transform.getWorld();

    if (trailEnd == null) {
      trailEnd = new Vector3d(transform.getPosition());
      data.lastTrailDraw = trailEnd;

      return;
    }

    Vector3d dif = new Vector3d();
    transform.getPosition().sub(trailEnd, dif);

    double len = dif.length();
    dif.normalize().mul(PARTICLE_DISTANCE);

    for (double d = 0; d < len; d += PARTICLE_DISTANCE) {
      trailEnd.add(dif);

      new ParticleBuilder(Particle.DUST_COLOR_TRANSITION)
          .colorTransition(PARTICLE_COLOR, Color.fromRGB(43, 30, 30), PARTICLE_SIZE)
          .location(world, trailEnd.x, trailEnd.y, trailEnd.z)
          .count(2)
          .allPlayers()
          .spawn();
    }
  }

  void drawHead(Transform transform) {
    World world = transform.getWorld();
    Vector3d pos = transform.getPosition();

    Particle.FLAME.builder()
        .location(world, pos.x, pos.y, pos.z)
        .allPlayers()
        .count(5)
        .offset(HIT_RANGE, HIT_RANGE, HIT_RANGE)
        .extra(0)
        .spawn();

    Particle.LARGE_SMOKE.builder()
        .location(world, pos.x, pos.y, pos.z)
        .allPlayers()
        .count(5)
        .offset(HIT_RANGE, HIT_RANGE, HIT_RANGE)
        .extra(0)
        .spawn();
  }
}

class GuardianBeamData implements Component {
  Vector3d origin;
  Vector3d lastTrailDraw;

  double damage = 0;
  double maxTravelDistanceSq = 0;

  double sinceTrailDraw = 0;
  double sinceHeadDraw = 0;

  boolean collideWithBlocks = false;
}