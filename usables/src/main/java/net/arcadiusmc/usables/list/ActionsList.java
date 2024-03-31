package net.arcadiusmc.usables.list;

import net.arcadiusmc.registry.Registry;
import net.arcadiusmc.usables.Action;
import net.arcadiusmc.usables.ObjectType;
import net.arcadiusmc.usables.UsablesPlugin;

public class ActionsList extends ComponentsArray<Action> {

  public ActionsList() {
    super(Action.class);
  }

  @Override
  @SuppressWarnings({"unchecked", "rawtypes"})
  public Registry<ObjectType<Action>> getRegistry() {
    return (Registry) UsablesPlugin.get().getActions();
  }
}
