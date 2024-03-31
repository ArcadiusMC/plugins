package net.arcadiusmc.usables.objects;

import java.util.Map;
import java.util.Objects;
import lombok.Getter;
import net.arcadiusmc.usables.Condition.TransientCondition;
import net.arcadiusmc.usables.Interaction;
import net.arcadiusmc.usables.list.ConditionsList;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

@Getter
public abstract class CommandUsable extends Usable {

  private final String name;

  private TransientCondition additional;

  public CommandUsable(String name) {
    super();

    Objects.requireNonNull(name, "Null name");
    this.name = name;
  }

  @Override
  public void fillContext(Map<String, Object> context) {
    super.fillContext(context);
    context.put("name", name);
  }

  @Override
  public Component name() {
    return Component.text(getName());
  }

  @Override
  public boolean interact(Interaction interaction) {
    if (!runConditions(interaction)) {
      return false;
    }

    runActions(interaction);

    var playerOpt = interaction.getPlayer();
    if (playerOpt.isEmpty()) {
      return false;
    }

    onInteract(playerOpt.get(), interaction.getBoolean("adminInteraction").orElse(false));
    return true;
  }

  protected abstract void onInteract(Player player, boolean adminInteraction);

  @Override
  public ConditionsList getConditions() {
    ConditionsList list = super.getConditions();

    if (additional != null) {
      int index = list.indexOf(additional);

      if (index == -1) {
        list.addFirst(additional);
      } else if (index != 0) {
        list.remove(index);
        list.addFirst(additional);
      }

      return list;
    }

    TransientCondition created = additionalCondition();
    if (created == null) {
      return list;
    }

    additional = created;
    list.addFirst(additional);

    return list;
  }

  protected TransientCondition additionalCondition() {
    return null;
  }
}
