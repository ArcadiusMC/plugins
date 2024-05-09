package net.arcadiusmc.entity.dungeons;

import static net.arcadiusmc.entity.dungeons.GuardianBeam.HEAD_DRAW_INTERVAL_SECONDS;
import static net.arcadiusmc.entity.dungeons.GuardianBeam.TRAIL_DRAW_INTERVAL_SECONDS;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.destroystokyo.paper.ParticleBuilder;
import java.util.List;
import java.util.Random;
import net.arcadiusmc.entity.Entities;
import net.arcadiusmc.entity.system.Handle;
import net.arcadiusmc.entity.system.Transform;
import net.arcadiusmc.events.Events;
import net.arcadiusmc.utils.VanillaAccess;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Shulker;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.util.BoundingBox;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class ShulkerGuardian extends IteratingSystem {

  static final float DYING_TIME_SECONDS = 2.5f;
  static final float DRAW_INTERVAL_SECONDS = 0.25f;
  static final float AIMING_TIME_SECONDS = 0.25f;
  static final float AIM_INTERVAL_SECONDS = 1f;
  static final float AIM_VARIANCE_SECONDS = 0.5f;
  static final float BASE_DAMAGE = 3.5f;
  static final double FAST_FIRE_DISTANCE = 10d;
  static final double FAST_FIRE_SPEED = 2.5;
  static final double FIRE_SPEED = 2;
  static final double PARTICLE_DISTANCE = 0.2d;
  static final double TARGET_SEARCH_RADIUS = 45;
  static final double FAST_FIRE_DISTANCE_SQ = FAST_FIRE_DISTANCE * FAST_FIRE_DISTANCE;

  private final Random random;
  private final BukkitListener listener;

  public ShulkerGuardian() {
    super(Family.all(ShulkerGuardianData.class, Handle.class, Transform.class).get());

    this.random = new Random();
    this.listener = new BukkitListener();
  }

  @Override
  public void addedToEngine(Engine engine) {
    super.addedToEngine(engine);
    Events.register(listener);
  }

  @Override
  public void removedFromEngine(Engine engine) {
    super.removedFromEngine(engine);
    Events.unregister(listener);
  }

  @Override
  protected void processEntity(Entity entity, float deltaTime) {
    ShulkerGuardianData data = entity.getComponent(ShulkerGuardianData.class);
    Handle handle = entity.getComponent(Handle.class);
    Transform transform = entity.getComponent(Transform.class);

    Shulker shulker = handle.getAs(Shulker.class);

    if (shulker == null) {
      return;
    }
    if (data.state == null) {
      data.state = GuardianState.NONE;
    }

    data.stateTime += deltaTime;
    data.sinceDraw += deltaTime;

    Vector3d beamOrigin = new Vector3d(transform.getPosition());
    beamOrigin.add(0, shulker.getHeight() / 2, 0);
    World world = transform.getWorld();

    switch (data.state) {
      case AIMING -> {
        if (data.sinceDraw >= DRAW_INTERVAL_SECONDS) {
          data.sinceDraw = 0;
          drawBeam(data, beamOrigin, data.target, world);
        }

        if (data.stateTime < AIMING_TIME_SECONDS) {
          return;
        }

        data.switchState(GuardianState.FIRING);
        data.sinceDraw = DRAW_INTERVAL_SECONDS;

        Entity beam = Entities.create();

        // Set up beam
        GuardianBeamData beamData = new GuardianBeamData();
        beamData.origin = beamOrigin;
        beamData.damage = BASE_DAMAGE;
        beamData.sinceHeadDraw = HEAD_DRAW_INTERVAL_SECONDS;
        beamData.sinceTrailDraw = TRAIL_DRAW_INTERVAL_SECONDS;

        beam.add(beamData);

        // Compute velocity
        Vector3d dif = new Vector3d();
        data.target.sub(beamOrigin, dif);

        double lengthSq = dif.lengthSquared();
        beamData.maxTravelDistanceSq = lengthSq;

        dif.normalize();

        if (lengthSq >= FAST_FIRE_DISTANCE_SQ) {
          dif.mul(FIRE_SPEED);
          data.firingTime = (float) FAST_FIRE_SPEED;
        } else {
          dif.mul(FAST_FIRE_SPEED);
          data.firingTime = (float) FIRE_SPEED;
        }

        Transform beamTransform = beam.getComponent(Transform.class);
        beamTransform.setPosition(beamOrigin);
        beamTransform.setVelocity(dif);
        beamTransform.setWorld(transform.getWorld());

        getEngine().addEntity(beam);
      }

      case FIRING -> {
        if (data.stateTime < data.firingTime) {
          return;
        }

        data.switchState(GuardianState.NONE);
        shulker.setPeek(0);
      }

      case NONE -> {
        float nextAimTick = AIM_INTERVAL_SECONDS + (random.nextFloat() * AIM_VARIANCE_SECONDS);

        if (data.stateTime < nextAimTick) {
          return;
        }

        Vector3d target = data.target;
        findTarget(target, shulker, transform.getWorld());

        // If failed to find target
        if (target.x == 0 && target.y == 0 && target.z == 0) {
          data.stateTime = 0;
          return;
        }

        data.switchState(GuardianState.AIMING);
        data.sinceDraw = DRAW_INTERVAL_SECONDS;
        shulker.setPeek(1);
      }

      case TRAITOR -> {
        if (data.betrayalTarget == null) {
          data.switchState(GuardianState.DYING);
          return;
        }

        if (data.sinceDraw >= DRAW_INTERVAL_SECONDS) {
          data.sinceDraw = 0;
          drawBeam(data, beamOrigin, data.betrayalTarget, world);
        }
      }

      case DYING -> {
        if (data.stateTime >= DYING_TIME_SECONDS) {
          data.switchState(GuardianState.DEAD);
        }

        double randX = random.nextDouble(-1, 1);
        double randY = random.nextDouble(-1, 1);
        double randZ = random.nextDouble(-1, 1);

        Vector3d dir = new Vector3d(randX, randY, randZ);
        dir.normalize();
        dir.mul(random.nextDouble(1, 3));

        Vector3d target = new Vector3d(beamOrigin);
        target.add(dir);

        drawBeam(data, beamOrigin, target, world);
      }

      case DEAD -> {
        getEngine().removeEntity(entity);
        shulker.remove();
      }
    }
  }

  void findTarget(Vector3d out, Shulker shulker, World world) {
    BoundingBox searchRange = shulker.getBoundingBox().expand(TARGET_SEARCH_RADIUS);

    List<Player> nearby = world.getNearbyEntities(searchRange)
        .stream()
        .filter(entity -> entity instanceof Player)
        .map(entity -> (Player) entity)
        .filter(player -> {
          return player.getGameMode() != GameMode.SPECTATOR
              && player.getGameMode() != GameMode.CREATIVE;
        })
        .toList();

    if (nearby.isEmpty()) {
      out.x = 0;
      out.y = 0;
      out.z = 0;

      return;
    }

    Player target = nearby.get(random.nextInt(nearby.size()));

    Vector3d vel = new Vector3d();

    VanillaAccess.getPosition(out, target);
    VanillaAccess.getVelocity(vel, target);

    out.add(vel);
    out.add(0, target.getHeight() / 2, 0);
  }

  void drawBeam(ShulkerGuardianData data, Vector3dc start, Vector3dc target, World world) {
    ParticleBuilder spawn = new ParticleBuilder(Particle.DUST)
        .count(1)
        .color(data.state.color, data.state.size);

    Vector3d dif = new Vector3d();
    target.sub(start, dif);

    double length = dif.length();
    dif.normalize();
    dif.mul(PARTICLE_DISTANCE);

    Vector3d pos = new Vector3d(start);

    for (double i = 0; i < length; i += PARTICLE_DISTANCE) {
      pos.add(dif);

      spawn.location(world, pos.x, pos.y, pos.z)
          .allPlayers()
          .spawn();
    }
  }
}

class BukkitListener implements Listener {

  @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
  public void onEntityDeath(EntityDeathEvent event) {
    Entity entity = Entities.fromBukkit(event.getEntity());
    if (entity == null) {
      return;
    }

    ShulkerGuardianData data = entity.getComponent(ShulkerGuardianData.class);
    if (data == null) {
      return;
    }

    event.setCancelled(true);

    if (data.betrayalTarget == null) {
      data.switchState(GuardianState.DYING);
    } else {
      data.switchState(GuardianState.TRAITOR);
    }
  }

  @EventHandler(ignoreCancelled = true)
  public void onEntityDamage(EntityDamageEvent event) {
    DamageSource source = event.getDamageSource();
    DamageType type = source.getDamageType();

    if (!type.getKey().equals(DamageType.EXPLOSION.getKey())) {
      return;
    }

    Entity entity = Entities.fromBukkit(event.getEntity());
    if (entity == null || entity.getComponent(ShulkerGuardianData.class) == null) {
      return;
    }

    event.setCancelled(true);
  }
}

enum GuardianState {

  NONE(null, 0f),
  AIMING(Color.WHITE, 1f),
  FIRING(Color.RED, 3f),
  TRAITOR(Color.PURPLE, 4f),
  DYING (Color.GRAY, 1f),
  DEAD (null, 0F);

  final Color color;
  final float size;

  GuardianState(Color color, float size) {
    this.color = color;
    this.size = size;
  }
}

class ShulkerGuardianData implements Component {
  GuardianState state = GuardianState.NONE;

  final Vector3d target = new Vector3d();
  Vector3d betrayalTarget;

  float stateTime;
  float sinceDraw;
  float firingTime;

  void switchState(GuardianState state) {
    this.stateTime = 0;
    this.sinceDraw = 0;
    this.state = state;
  }
}
