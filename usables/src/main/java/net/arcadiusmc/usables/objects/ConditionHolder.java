package net.arcadiusmc.usables.objects;

import java.util.function.Predicate;
import net.arcadiusmc.usables.ComponentList;
import net.arcadiusmc.usables.Condition;
import net.arcadiusmc.usables.Interaction;
import net.arcadiusmc.usables.Usables;

public interface ConditionHolder extends Predicate<Interaction>, UsableObject {

  ComponentList<Condition> getConditions();

  default Iterable<Condition> getEffectiveConditions() {
    return getConditions();
  }

  boolean isSilent();

  void setSilent(boolean silent);

  @Override
  default boolean test(Interaction interaction) {
    return Usables.test(getEffectiveConditions(), interaction);
  }

  default boolean runConditions(Interaction interaction) {
    return Usables.runConditions(getEffectiveConditions(), interaction);
  }
}
