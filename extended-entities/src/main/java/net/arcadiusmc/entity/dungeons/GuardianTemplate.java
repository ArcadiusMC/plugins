package net.arcadiusmc.entity.dungeons;

import com.badlogic.ashley.core.Entity;
import net.arcadiusmc.entity.Entities;
import net.arcadiusmc.entity.EntityTemplate;
import net.arcadiusmc.entity.system.Handle;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Shulker;

public class GuardianTemplate implements EntityTemplate {

  @Override
  public Entity summon(Location location) {
    World world = location.getWorld();

    Shulker shulker = world.spawn(location, Shulker.class, s -> {
      s.customName(Component.text("Shulker Guardian", NamedTextColor.RED, TextDecoration.BOLD));
      s.setCustomNameVisible(true);
      s.setColor(DyeColor.RED);
      s.setAI(false);
    });

    Entity entity = Entities.create();

    ShulkerGuardianData data = new ShulkerGuardianData();
    data.state = GuardianState.NONE;

    Handle handle = new Handle();
    handle.setMinecraftId(shulker.getUniqueId());
    handle.setEntity(shulker);

    entity.add(data);
    entity.add(handle);

    return entity;
  }
}
