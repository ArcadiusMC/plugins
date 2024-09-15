package net.arcadiusmc.items.goal;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;

public record Goal(GoalKey trigger, Component displayName, float goal) {

  public static final Goal DONATOR = new Goal(
      GoalKey.DONATOR,
      Component.text("Donate to the server"),
      1
  );

  public static Goal entitiesKilled(EntityType type, int goal) {
    return new Goal(GoalKey.entityKill(type), Component.translatable(type), goal);
  }

  public static Goal blocksBroken(Material type, int goal) {
    return new Goal(GoalKey.blockBreak(type), Component.translatable(type), goal);
  }
}
