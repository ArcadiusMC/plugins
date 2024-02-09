package net.arcadiusmc.usables.commands;

import net.arcadiusmc.usables.ComponentList;
import net.arcadiusmc.usables.UsableComponent;
import net.arcadiusmc.usables.objects.UsableObject;

public interface ListHolder<T extends UsableComponent> {

  ComponentList<T> getList();

  void postEdit();

  UsableObject object();
}
