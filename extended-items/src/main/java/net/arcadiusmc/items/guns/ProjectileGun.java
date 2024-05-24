package net.arcadiusmc.items.guns;

import com.google.common.base.Strings;
import java.util.Random;
import lombok.Getter;
import lombok.Setter;
import net.arcadiusmc.Loggers;
import net.forthecrown.nbt.CompoundTag;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Explosive;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.persistence.PersistentDataType;
import org.slf4j.Logger;

@Getter @Setter
public class ProjectileGun extends Gun {

  public static final NamespacedKey YIELD_KEY = new NamespacedKey("oct2023", "yield");

  private static final Logger LOGGER = Loggers.getLogger();

  private EntityType projectileType;
  private boolean firebomb;

  @Override
  protected boolean onUse(Player player, Random random) {
    if (projectileType == null) {
      return false;
    }

    World world = player.getWorld();
    Location spawn = player.getEyeLocation();

    Entity ent = world.spawnEntity(spawn, projectileType);

    ent.setVelocity(spawn.getDirection());
    ent.setRotation(spawn.getYaw(), spawn.getPitch());

    if (ent instanceof Explosive explosive) {
      explosive.setYield(getBaseDamage());
      explosive.setIsIncendiary(firebomb);
    }

    if (ent instanceof Projectile projectile) {
      projectile.setShooter(player);
    }

    var pdc = ent.getPersistentDataContainer();
    pdc.set(YIELD_KEY, PersistentDataType.FLOAT, getBaseDamage());

    return true;
  }

  @Override
  public void save(CompoundTag tag) {
    super.save(tag);

    if (projectileType != null) {
      tag.putString("projectile", projectileType.key().asString());
    }

    tag.putBoolean("incendiary", firebomb);
  }

  @Override
  public void load(CompoundTag tag) {
    super.load(tag);

    this.firebomb = tag.getBoolean("incendiary", false);

    String typeName = tag.getString("projectile", null);
    if (Strings.isNullOrEmpty(typeName)) {
      projectileType = null;
      // Being logged too much cuz of upgrades menu
      //LOGGER.error("No 'projectile' set for gun");
    } else {
      NamespacedKey key = NamespacedKey.fromString(typeName);
      setProjectileType(key);
    }
  }

  private void setProjectileType(NamespacedKey type) {
    if (type == null) {
      projectileType = null;
      return;
    }

    projectileType = Registry.ENTITY_TYPE.get(type);

    if (projectileType == null) {
      return;
    }

    setProjectileType(projectileType);
  }

  public void setProjectileType(EntityType type) {
    if (type == null) {
      this.projectileType = null;
      return;
    }

    Class<? extends Entity> entityClass = type.getEntityClass();

    if (entityClass == null) {
      LOGGER.error("Entity type '{}' can not be summoned???", type);
      projectileType = null;
      return;
    }

    if (!Projectile.class.isAssignableFrom(entityClass)) {
      projectileType = null;
      LOGGER.error("Entity type '{}' is not a projectile", type);
    }

    this.projectileType = type;
  }
}
