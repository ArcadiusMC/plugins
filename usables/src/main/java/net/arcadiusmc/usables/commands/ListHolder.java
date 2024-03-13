package net.arcadiusmc.usables.commands;

import net.arcadiusmc.usables.ComponentList;
import net.arcadiusmc.usables.UsableComponent;
import net.arcadiusmc.usables.objects.UsableObject;
import net.forthecrown.grenadier.types.IntRangeArgument.IntRange;

public interface ListHolder<T extends UsableComponent> {

  ComponentList<T> getList();

  void postEdit();

  UsableObject object();

  default void onRemove(IntRange removeRange) {

  }

  default void onClear() {

  }
}
