package net.arcadiusmc.items.guns;

import com.destroystokyo.paper.ParticleBuilder;
import java.util.Random;
import lombok.Getter;
import lombok.Setter;
import net.forthecrown.nbt.BinaryTags;
import net.forthecrown.nbt.CompoundTag;
import net.forthecrown.nbt.TagTypes;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.joml.Quaterniond;
import org.joml.Vector3d;

@Getter @Setter
public class HitScanGun extends Gun {

  static final double HEADSHOT_MULTIPLIER = 2;

  static final Sound HEADSHOT_SOUND = Sound.sound()
      .type(org.bukkit.Sound.ENTITY_ARROW_HIT_PLAYER)
      .build();

  private double maxDistance = 25;
  private double raySize;
  private double particleDistance;
  private double maxSpreadDeg;
  private double knockbackStrength;

  private double movementInaccuracy = 1;
  private double baseInaccuracy = 1;

  private float blockDamageMultiplier = 0.25f;

  private int launchedPellets;

  private GunParticle particle;

  private double baseSpread(Random random) {
    if (maxSpreadDeg <= 0) {
      return 0d;
    }

    double half = maxSpreadDeg / 2;
    return random.nextDouble(-half, half);
  }

  @Override
  protected boolean onUse(Player player, Random random) {
    World world = player.getWorld();
    Location start = player.getEyeLocation();
    Vector direction = start.getDirection();
    Vector3d dirVec = new Vector3d(direction.getX(), direction.getY(), direction.getZ());

    boolean shifting = player.isSneaking();

    double movement = PlayerMoveSpeeds.SPEEDS.getMovement(player).length();
    movement *= shifting ? 0.25 : 1;

    double baseSpreadX = baseSpread(random);
    double baseSpreadY = baseSpread(random);
    double baseSpreadZ = baseSpread(random);

    double moveScalar = (movement * movementInaccuracy) + baseInaccuracy;

    for (int i = 0; i < launchedPellets; i++) {
      Vector3d offsetDir;

      if (i == 0 && (movement <= 0.08 || shifting)) {
        offsetDir = dirVec;
      } else {
        Quaterniond rotation = new Quaterniond();

        double spreadX = (baseSpreadX + baseSpread(random) / 2) * moveScalar;
        double spreadY = (baseSpreadY + baseSpread(random) / 2) * moveScalar;
        double spreadZ = (baseSpreadZ + baseSpread(random) / 2) * moveScalar;

        rotation.rotateLocalX(Math.toRadians(spreadX));
        rotation.rotateLocalY(Math.toRadians(spreadY));
        rotation.rotateLocalZ(Math.toRadians(spreadZ));

        offsetDir = new Vector3d(dirVec).rotate(rotation).normalize();
      }

      RayTraceResult result = world.rayTrace(
          start,
          new Vector(offsetDir.x, offsetDir.y, offsetDir.z),
          maxDistance,
          FluidCollisionMode.ALWAYS,
          true,
          raySize,
          entity -> {
            if (entity.equals(player)) {
              return false;
            }

            if (entity instanceof Player otherPlayer) {
              return otherPlayer.getGameMode() != GameMode.SPECTATOR;
            }

            return true;
          }
      );

      playEffects(player, start, offsetDir, result);

      if (result == null) {
        continue;
      }

      onHit(player, result, offsetDir);
    }

    return true;
  }

  public void playEffects(Player player, Location origin, Vector3d direction, RayTraceResult result) {
    if (particle == null || particle.isErroneous()) {
      return;
    }

    Vector3d end;
    Vector3d originVec = new Vector3d(origin.getX(), origin.getY(), origin.getZ());

    if (result == null) {
      end = new Vector3d(direction).mul(maxDistance);
      end.add(originVec);
    } else {
      var hit = result.getHitPosition();
      end = new Vector3d(hit.getX(), hit.getY(), hit.getZ());
    }

    Vector3d dif = new Vector3d();
    end.sub(originVec, dif);

    double particleDistance = this.particleDistance > 0 ? this.particleDistance : 0.25;

    double len = dif.length();
    double points = len / particleDistance;

    dif.div(points);
    Vector3d pos = new Vector3d();
    ParticleBuilder builder = particle.createBuilder().source(player);

    World world = origin.getWorld();

    for (int i = 0; i < points; i++) {
      dif.mul(i, pos);
      pos.add(originVec);

      builder.location(world, pos.x, pos.y, pos.z)
          .allPlayers()
          .spawn();
    }
  }

  protected double calculateDamageModifier(Entity entity, Vector hitPos) {
    BoundingBox box = entity.getBoundingBox();
    double minY = box.getMinY();

    double maxY = box.getMaxY() - minY;
    double localizedY = hitPos.getY() - minY;

    double hitRatio = localizedY / maxY;

    if (hitRatio >= 0.75) {
      return HEADSHOT_MULTIPLIER;
    }

    return hitRatio + 0.25;
  }

  protected void onHit(Player player, RayTraceResult result, Vector3d direction) {
    Entity entity = result.getHitEntity();
    Block block = result.getHitBlock();

    Color color;

    if (entity instanceof Damageable living) {
      color = Color.RED;

      if (entity instanceof Mob mob) {
        mob.setNoDamageTicks(0);
      }

      double modifier = calculateDamageModifier(living, result.getHitPosition());
      double damage = modifier * getBaseDamage();

      if (modifier >= HEADSHOT_MULTIPLIER) {
        player.playSound(HEADSHOT_SOUND);
      }

      living.damage(damage, player);

      Vector knockback = new Vector(direction.x, direction.y, direction.z);
      knockback.multiply(knockbackStrength);
      knockback.add(living.getVelocity());

      living.setVelocity(knockback);
    } else if (block != null) {
      color = Color.GRAY;
      float blockDamage = getBaseDamage() * blockDamageMultiplier;
      BlockDamage.BLOCK_DAMAGE.damage(block, blockDamage);
    } else {
      color = Color.GRAY;
    }

    Vector3d invDir = new Vector3d();
    direction.mul(-0.2, invDir);

    var hit = result.getHitPosition()
        .add(new Vector(invDir.x, invDir.y, invDir.y))
        .toLocation(player.getWorld());

    Particle.DUST.builder()
        .source(player)
        .count(6)
        .location(hit)
        .offset(0.1, 0.1, 0.1)
        .extra(0)
        .color(color)
        .allPlayers()
        .spawn();

  }

  @Override
  public void save(CompoundTag tag) {
    super.save(tag);

    tag.putDouble("max_distance", maxDistance);
    tag.putDouble("ray_size", raySize);
    tag.putDouble("particle_distance", particleDistance);
    tag.putDouble("max_spread_degrees", maxSpreadDeg);
    tag.putDouble("knockback_strength", knockbackStrength);
    tag.putDouble("base_inaccuracy", baseInaccuracy);
    tag.putDouble("movement_inaccuracy", movementInaccuracy);

    tag.putFloat("block_damage_modifier", blockDamageMultiplier);

    tag.putInt("launched_pellets", launchedPellets);

    if (particle != null) {
      CompoundTag particleTag = BinaryTags.compoundTag();
      particle.save(particleTag);

      tag.put("particle", particleTag);
    }
  }

  @Override
  public void load(CompoundTag tag) {
    super.load(tag);

    maxDistance = tag.getDouble("max_distance", 125);
    raySize = tag.getDouble("ray_size", 0.01);
    particleDistance = tag.getDouble("particle_distance", 0.25);
    maxSpreadDeg = tag.getDouble("max_spread_degrees", 6.5);
    knockbackStrength = tag.getDouble("knockback_strength", 1);
    movementInaccuracy = tag.getDouble("movement_inaccuracy", 1.5d);
    baseInaccuracy = tag.getDouble("base_inaccuracy", 1);

    blockDamageMultiplier = tag.getFloat("block_damage_modifier", 0.25f);

    launchedPellets = tag.getInt("launched_pellets", 1);

    if (tag.contains("particle", TagTypes.compoundType())) {
      CompoundTag particleTag = tag.getCompound("particle");

      if (particle == null) {
        particle = new GunParticle();
      }

      particle.load(particleTag);
    } else {
      particle = null;
    }
  }
}
