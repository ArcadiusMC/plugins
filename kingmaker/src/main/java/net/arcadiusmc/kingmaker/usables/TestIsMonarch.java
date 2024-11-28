package net.arcadiusmc.kingmaker.usables;

import java.util.Objects;
import java.util.UUID;
import net.arcadiusmc.kingmaker.Kingmaker;
import net.arcadiusmc.kingmaker.KingmakerPlugin;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.usables.Condition;
import net.arcadiusmc.usables.Interaction;
import net.arcadiusmc.usables.ObjectType;
import net.arcadiusmc.usables.SimpleType;
import net.arcadiusmc.usables.UsableComponent;
import net.kyori.adventure.text.Component;
import org.bukkit.plugin.java.JavaPlugin;

public class TestIsMonarch implements Condition {

  public static final SimpleType<TestIsMonarch> TYPE = new SimpleType<>(TestIsMonarch::new);

  @Override
  public boolean test(Interaction interaction) {
    return interaction.getPlayer()
        .map(player -> {
          Kingmaker kingmaker = JavaPlugin.getPlugin(KingmakerPlugin.class).getKingmaker();
          UUID monarchId = kingmaker.getMonarchId();

          if (monarchId == null) {
            return false;
          }

          return Objects.equals(monarchId, player.getUniqueId());
        })
        .orElse(false);
  }

  @Override
  public Component failMessage(Interaction interaction) {
    return Messages.renderText("kingmaker.errors.usableError");
  }

  @Override
  public ObjectType<? extends UsableComponent> getType() {
    return TYPE;
  }
}
