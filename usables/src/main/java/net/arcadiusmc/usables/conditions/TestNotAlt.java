package net.arcadiusmc.usables.conditions;

import net.arcadiusmc.usables.Condition;
import net.arcadiusmc.usables.Interaction;
import net.arcadiusmc.usables.SimpleType;
import net.arcadiusmc.usables.UsableComponent;
import net.arcadiusmc.usables.ObjectType;
import net.arcadiusmc.user.UserService;
import net.arcadiusmc.user.Users;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class TestNotAlt implements Condition {

  public static final ObjectType<TestNotAlt> TYPE = new SimpleType<>(TestNotAlt::new);

  @Override
  public boolean test(Interaction interaction) {
    return interaction.getPlayerId()
        .map(playerId -> {
          UserService service = Users.getService();
          return !service.isAltAccount(playerId);
        })
        .orElse(true);
  }

  @Override
  public Component failMessage(Interaction interaction) {
    return Component.text("Alt accounts may not use this", NamedTextColor.GRAY);
  }

  @Override
  public ObjectType<? extends UsableComponent> getType() {
    return TYPE;
  }
}
