package net.arcadiusmc.factions.usables;

import net.arcadiusmc.registry.Registry;
import net.arcadiusmc.usables.Action;
import net.arcadiusmc.usables.Condition;
import net.arcadiusmc.usables.ObjectType;
import net.arcadiusmc.usables.UsablesPlugin;

public class FactionUsables {

  public static void registerAll() {
    UsablesPlugin plugin = UsablesPlugin.get();

    Registry<ObjectType<? extends Condition>> conditions = plugin.getConditions();
    Registry<ObjectType<? extends Action>> actions = plugin.getActions();

    conditions.register("is_faction_member", FactionMemberType.INSTANCE);
    conditions.register("in_any_faction", AnyFactionMemberTest.TYPE);
  }
}
