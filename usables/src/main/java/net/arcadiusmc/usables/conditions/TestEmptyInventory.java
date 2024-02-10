package net.arcadiusmc.usables.conditions;

import net.arcadiusmc.usables.Condition;
import net.arcadiusmc.usables.Interaction;
import net.arcadiusmc.usables.SimpleType;
import net.arcadiusmc.usables.UsableComponent;
import net.arcadiusmc.usables.ObjectType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class TestEmptyInventory implements Condition {

  public static final ObjectType<TestEmptyInventory> TYPE
      = new SimpleType<>(TestEmptyInventory::new);

  @Override
  public boolean test(Interaction interaction) {
    return interaction.player().getInventory().isEmpty();
  }

  @Override
  public Component failMessage(Interaction interaction) {
    return Component.text("Your inventory must be empty", NamedTextColor.GRAY);
  }

  @Override
  public ObjectType<? extends UsableComponent> getType() {
    return TYPE;
  }
}
