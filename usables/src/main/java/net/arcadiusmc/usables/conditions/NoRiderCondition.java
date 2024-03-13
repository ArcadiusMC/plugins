package net.arcadiusmc.usables.conditions;

import net.arcadiusmc.usables.Condition;
import net.arcadiusmc.usables.Interaction;
import net.arcadiusmc.usables.SimpleType;
import net.arcadiusmc.usables.UsableComponent;
import net.arcadiusmc.usables.ObjectType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class NoRiderCondition implements Condition {

  public static final ObjectType<NoRiderCondition> TYPE = new SimpleType<>(NoRiderCondition::new);

  @Override
  public boolean test(Interaction interaction) {
    return interaction.getPlayer().map(player -> player.getPassengers().isEmpty()).orElse(true);
  }

  @Override
  public Component failMessage(Interaction interaction) {
    return Component.text("Cannot have anyone riding you", NamedTextColor.GRAY);
  }

  @Override
  public ObjectType<? extends UsableComponent> getType() {
    return TYPE;
  }
}
