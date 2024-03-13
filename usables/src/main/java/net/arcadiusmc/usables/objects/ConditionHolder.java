package net.arcadiusmc.usables.objects;

import static net.arcadiusmc.usables.Usables.NO_FAILURE;

import java.util.function.Predicate;
import net.arcadiusmc.usables.ComponentList;
import net.arcadiusmc.usables.Condition;
import net.arcadiusmc.usables.Interaction;

public interface ConditionHolder extends Predicate<Interaction>, UsableObject {

  ComponentList<Condition> getConditions();

  String[] getErrorOverrides();

  String getGlobalErrorOverride();

  boolean isSilent();

  void setSilent(boolean silent);

  int getFailureIndex(Interaction interaction);

  @Override
  default boolean test(Interaction interaction) {
    return getFailureIndex(interaction) == NO_FAILURE;
  }

  boolean runConditions(Interaction interaction);
}
