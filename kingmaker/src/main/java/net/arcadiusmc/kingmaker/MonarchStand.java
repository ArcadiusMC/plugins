package net.arcadiusmc.kingmaker;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.google.common.base.Strings;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.utils.EntityRef;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.slf4j.Logger;

public record MonarchStand(
    EntityRef ref,
    boolean changeHead,
    boolean changeName
) {

  private static final Logger LOGGER = Loggers.getLogger();

  static final Codec<MonarchStand> CODEC = RecordCodecBuilder.create(instance -> {
    return instance
        .group(
            EntityRef.CODEC.fieldOf("entity_ref").forGetter(MonarchStand::ref),
            Codec.BOOL.optionalFieldOf("change_head", true).forGetter(MonarchStand::changeHead),
            Codec.BOOL.optionalFieldOf("change_name", true).forGetter(MonarchStand::changeName)
        )
        .apply(instance, MonarchStand::new);
  });

  void update(PlayerProfile profile) {
    Entity entity = ref.get();

    if (entity == null) {
      LOGGER.error("Failed to find monarch stand {}", ref);
      return;
    }

    if (!(entity instanceof ArmorStand stand)) {
      LOGGER.warn("Entity referenced by '{}' is not armor stand", ref);
      return;
    }

    EntityEquipment equipment = stand.getEquipment();

    if (changeHead) {
      if (profile == null) {
        equipment.setHelmet(null);
      } else {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);

        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setPlayerProfile(profile);
        item.setItemMeta(meta);

        equipment.setHelmet(item);
      }
    }

    if (changeName) {
      if (profile == null) {
        stand.customName(null);
      } else {
        String name = profile.getName();

        if (Strings.isNullOrEmpty(name)) {
          LOGGER.warn("Profile {} has null name, cannot change monarch stand {}", profile, ref);
        } else {
          stand.customName(Component.text(name));
        }
      }
    }
  }
}
