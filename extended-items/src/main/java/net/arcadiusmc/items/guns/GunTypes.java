package net.arcadiusmc.items.guns;

import java.util.Optional;
import java.util.function.Consumer;
import net.arcadiusmc.items.ExtendedItem;
import net.arcadiusmc.items.ItemTypes;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

public interface GunTypes {

  GunType<HitScanGun> PISTOL = createType(type -> {
    type.setFactory(HitScanGun::new);
    type.setMaterial(Material.STONE_AXE);
    type.setName(Component.text("Pistol", NamedTextColor.AQUA));
    type.setTextureId(10090003);

    type.setConsumer(gun -> {
      gun.setMaxAmmo(100);
      gun.setAmmoUse(1);
      gun.setRemainingAmmo(26);
      gun.setClipSize(13);
      gun.setRemainingClipAmmo(13);
      gun.setFiringDelay(3);
      gun.setReloadDelay(44);
      gun.setBaseDamage(7);

      GunParticle particle = new GunParticle();
      particle.setParticle(Particle.DUST);
      particle.setOptions(new DustOptions(Color.fromARGB(0xFFAA0000), 0.75f));

      gun.setParticle(particle);

      gun.getSounds().add(
          Sound.sound()
              .type(Key.key("arcadiusmc:guns.pistol"))
              .build()
      );
    });
  });

  GunType<HitScanGun> SHOTGUN = createType(type -> {
    type.setMaterial(Material.IRON_AXE);
    type.setFactory(HitScanGun::new);
    type.setName(Component.text("Twelve Gauges of fun", NamedTextColor.AQUA));
    type.setTextureId(10090004);

    type.setConsumer(gun -> {
      gun.setMaxAmmo(32);
      gun.setClipSize(8);
      gun.setRemainingAmmo(16);
      gun.setRemainingClipAmmo(8);
      gun.setBaseDamage(13);
      gun.setFiringDelay(1);
      gun.setReloadDelay(10);
      gun.setReloadAmount(1);
      gun.setLaunchedPellets(8);
      gun.setScreenKickPitch(5);
      gun.setMovementInaccuracy(2.5);
      gun.setMaxSpreadDeg(12.5);

      GunParticle particle = new GunParticle();
      particle.setParticle(Particle.DUST);
      particle.setOptions(new DustOptions(Color.fromARGB(0xFFAA0000), 0.75f));

      gun.setParticle(particle);

      gun.getSounds().add(
          Sound.sound()
              .type(Key.key("arcadiusmc:guns.shotgun"))
              .build()
      );
    });
  });

  GunType<HitScanGun> ASSAULT_RIFLE = createType(type -> {
    type.setMaterial(Material.GOLDEN_AXE);
    type.setFactory(HitScanGun::new);
    type.setTextureId(10090005);
    type.setName(Component.text("Assault Rifle", NamedTextColor.YELLOW));

    type.setConsumer(gun -> {
      gun.setMaxAmmo(200);
      gun.setClipSize(30);
      gun.setRemainingAmmo(50);
      gun.setRemainingClipAmmo(25);
      gun.setFiringDelay(0);
      gun.setReloadDelay(48);
      gun.setBaseDamage(10);

      GunParticle particle = new GunParticle();
      particle.setParticle(Particle.DUST);
      particle.setOptions(new DustOptions(Color.fromARGB(0xFFAA0000), 0.75f));

      gun.setParticle(particle);

      gun.getSounds().add(
          Sound.sound()
              .type(Key.key("arcadiusmc:guns.assault_rifle"))
              .build()
      );
    });
  });

  GunType<ProjectileGun> ROCKET_LAUNCHER = createType(type -> {
    type.setMaterial(Material.DIAMOND_AXE);
    type.setName(Component.text("Rocket Launcher", NamedTextColor.YELLOW));
    type.setTextureId(10090006);
    type.setFactory(ProjectileGun::new);

    type.setConsumer(gun -> {
      gun.setMaxAmmo(10);
      gun.setClipSize(1);
      gun.setRemainingAmmo(5);
      gun.setRemainingClipAmmo(1);
      gun.setBaseDamage(4);
      gun.setProjectileType(EntityType.FIREBALL);

      gun.getSounds().add(
          Sound.sound()
              .type(Key.key("arcadiusmc:guns.rocket_launcher"))
              .build()
      );
    });
  });

  private static <T extends Gun> GunType<T> createType(Consumer<GunType<T>> consumer) {
    GunType<T> type = new GunType<>();
    consumer.accept(type);
    return type;
  }

  static void applyItem(ItemStack itemStack, Consumer<Gun> gunConsumer) {
    Gun gun = getGun(itemStack);

    if (gun == null) {
      return;
    }

    gunConsumer.accept(gun);
  }

  static Gun getGun(ItemStack itemStack) {
    Optional<ExtendedItem> opt = ItemTypes.getItem(itemStack);

    if (opt.isEmpty()) {
      return null;
    }

    ExtendedItem eItem = opt.get();
    Optional<Gun> gunOpt = eItem.getComponent(Gun.class);

    return gunOpt.orElse(null);
  }
}
