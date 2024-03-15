package net.arcadiusmc.factions.usables;

import net.arcadiusmc.registry.Registry;
import net.arcadiusmc.usables.Action;
import net.arcadiusmc.usables.Condition;
import net.arcadiusmc.usables.ObjectType;
import net.arcadiusmc.usables.UsablesPlugin;

public class FactionUsables {

  public static void registerAll() {
    UsablesPlugin plugin = UsablesPlugin.get();

    Registry<ObjectType<? extends Condition>> conditionRegistry = plugin.getConditions();
    Registry<ObjectType<? extends Action>> actionRegistry = plugin.getActions();

    conditionRegistry.register("is_faction_member", FactionMemberType.INSTANCE);
  }
}
