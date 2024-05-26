package net.arcadiusmc.items.goal;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;

@Getter
public class GoalKey {

  private static final Map<String, GoalKey> keys = new HashMap<>();

  public static final String TYPE_BLOCK_BREAK = "block/break";
  public static final String TYPE_BLOCK_PLACE = "block/place";
  public static final String TYPE_ENTITY_KILL = "entity/kill";
  public static final String TYPE_ENTITY_DAMAGE = "entity/damage";

  private final String key;
  private final String type;
  private final String predicate;

  private GoalKey(String key, String type, String predicate) {
    this.key = key;
    this.type = type;
    this.predicate = predicate;
  }

  public static GoalKey valueOf(String type, Keyed keyed) {
    return valueOf(type, keyed == null ? "any" : keyed.key().asString());
  }

  public static GoalKey valueOf(String type, String predicate) {
    String key = toString(type, predicate);
    return keys.computeIfAbsent(key, s -> new GoalKey(s, type, predicate));
  }

  public static GoalKey entityKill(EntityType type) {
    return valueOf(TYPE_ENTITY_KILL, type);
  }

  public static GoalKey blockBreak(Material material) {
    return valueOf(TYPE_BLOCK_BREAK, material);
  }

  @Override
  public String toString() {
    return key;
  }

  private static String toString(String type, String predicate) {
    return type + "::" + predicate;
  }
}
