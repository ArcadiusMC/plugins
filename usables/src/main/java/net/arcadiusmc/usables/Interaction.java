package net.arcadiusmc.usables;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.Getter;
import net.arcadiusmc.usables.objects.UsableObject;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.Users;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

@Getter
public class Interaction {

  private final Map<String, Object> context = new Object2ObjectOpenHashMap<>();
  private final UsableObject object;

  public Interaction(UsableObject object) {
    this.object = object;
    this.object.fillContext(context);
  }

  public static Interaction create(Player player, UsableObject object) {
    return create(player, object, player.hasPermission(UPermissions.ADMIN_INTERACTION));
  }

  public static Interaction create(Player player, UsableObject object, boolean adminInteraction) {
    Interaction interaction = new Interaction(object);
    interaction.context.put("adminInteraction", adminInteraction);
    interaction.context.put("player", player);
    return interaction;
  }

  public static Interaction create(UsableObject object) {
    return new Interaction(object);
  }

  public Optional<User> getUser() {
    return getPlayer().map(Users::get);
  }

  public Optional<Player> getPlayer() {
    return getValue("player", Player.class);
  }

  public Optional<UUID> getPlayerId() {
    return getPlayer().map(Entity::getUniqueId);
  }

  public Optional<Object> getValue(String name) {
    return Optional.ofNullable(context.get(name));
  }

  public <T> Optional<T> getValue(String name, Class<T> type) {
    return getValue(name).filter(type::isInstance).map(o -> (T) o);
  }

  public Optional<Boolean> getBoolean(String key) {
    return getValue(key).map(o -> {
      if (o instanceof Boolean bo) {
        return bo;
      }

      return Boolean.parseBoolean(String.valueOf(o));
    });
  }
}
