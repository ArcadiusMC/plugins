package net.arcadiusmc.entity.dungeons;

import static net.arcadiusmc.entity.dungeons.GuardianBeam.DEFAULT_MAX_DIST_SQ;
import static net.arcadiusmc.entity.dungeons.GuardianBeam.HEAD_DRAW_INTERVAL_SECONDS;
import static net.arcadiusmc.entity.dungeons.GuardianBeam.TRAIL_DRAW_INTERVAL_SECONDS;
import static net.arcadiusmc.entity.dungeons.ShulkerGuardian.BASE_DAMAGE;
import static net.arcadiusmc.entity.dungeons.ShulkerGuardian.FIRE_SPEED;

import com.badlogic.ashley.core.Entity;
import java.util.UUID;
import net.arcadiusmc.entity.Entities;
import net.arcadiusmc.entity.EntityTemplate;
import net.arcadiusmc.entity.system.Transform;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.joml.Vector3d;

public class GuardianBeamTemplate implements EntityTemplate {

  @Override
  public Entity summon(Location location) {
    Entity entity = Entities.create(UUID.randomUUID(), location);
    Transform transform = entity.getComponent(Transform.class);

    GuardianBeamData beamData = new GuardianBeamData();
    beamData.damage = BASE_DAMAGE;
    beamData.maxTravelDistanceSq = DEFAULT_MAX_DIST_SQ;
    beamData.sinceHeadDraw = HEAD_DRAW_INTERVAL_SECONDS;
    beamData.sinceTrailDraw = TRAIL_DRAW_INTERVAL_SECONDS;
    beamData.collideWithBlocks = true;

    entity.add(beamData);

    Vector direction = location.getDirection();
    Vector3d vel = new Vector3d();
    vel.x = direction.getX();
    vel.y = direction.getY();
    vel.z = direction.getZ();

    vel.mul(FIRE_SPEED);

    transform.setVelocity(vel);

    return entity;
  }
}
