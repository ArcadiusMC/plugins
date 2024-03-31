package net.arcadiusmc.usables.objects;

import static net.arcadiusmc.usables.Usables.NO_FAILURE;

import java.util.function.Predicate;
import net.arcadiusmc.usables.Interaction;
import net.arcadiusmc.usables.list.ConditionsList;

public interface ConditionHolder extends Predicate<Interaction>, UsableObject {

  ConditionsList getConditions();

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
