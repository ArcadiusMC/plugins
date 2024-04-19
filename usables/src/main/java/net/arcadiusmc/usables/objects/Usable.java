package net.arcadiusmc.usables.objects;

import static net.arcadiusmc.usables.Usables.NO_FAILURE;
import static net.arcadiusmc.usables.Usables.formatString;

import com.google.common.base.Strings;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.text.TextWriter;
import net.arcadiusmc.usables.Action;
import net.arcadiusmc.usables.Condition;
import net.arcadiusmc.usables.Interaction;
import net.arcadiusmc.usables.list.ActionsList;
import net.arcadiusmc.usables.list.ComponentList;
import net.arcadiusmc.usables.list.ConditionsList;
import net.arcadiusmc.utils.io.Results;
import net.arcadiusmc.utils.io.TagOps;
import net.forthecrown.nbt.BinaryTag;
import net.forthecrown.nbt.CompoundTag;
import net.forthecrown.nbt.TypeIds;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.slf4j.Logger;

@Getter
public abstract class Usable implements ConditionHolder {

  public static final Logger LOGGER = Loggers.getLogger();

  public static final String KEY_CONDITIONS = "checks";
  public static final String KEY_ACTIONS = "actions";
  public static final String KEY_G_ERROR_OVERRIDE = "globalErrorOverride";
  public static final String KEY_ERROR_OVERRIDES = "errorOverrides";

  @Setter
  private boolean silent;

  private final ConditionsList conditions;
  private final ActionsList actions;

  @Setter
  private String globalErrorOverride = null;

  public Usable() {
    this.conditions = new ConditionsList();
    this.actions = new ActionsList();
  }

  @Override
  public int getFailureIndex(Interaction interaction) {
    ConditionsList conditions = getConditions();

    for (int i = 0; i < conditions.size(); i++) {
      Condition condition = conditions.get(i);

      if (condition.test(interaction)) {
        continue;
      }

      return i;
    }

    return NO_FAILURE;
  }

  @Override
  public boolean runConditions(Interaction interaction) {
    ConditionsList conditions = getConditions();
    int failedIndex = getFailureIndex(interaction);

    if (failedIndex == NO_FAILURE) {
      for (Condition condition : conditions) {
        condition.afterTests(interaction);
      }

      return true;
    }

    if (interaction.getBoolean("silent").orElse(false)) {
      return false;
    }

    Player player = interaction.getPlayer().orElse(null);

    if (player == null) {
      return false;
    }

    Component message = formatError(failedIndex, player, interaction);

    if (!Text.isEmpty(message)) {
      player.sendMessage(message);
    }

    return false;
  }

  public Component formatError(int failedIndex, Audience viewer, Interaction interaction) {
    Condition condition = conditions.get(failedIndex);
    String errorOverride;
    String errorAt = conditions.getError(failedIndex);

    if (Strings.isNullOrEmpty(errorAt)) {
      errorOverride = globalErrorOverride;
    } else {
      errorOverride = errorAt;
    }

    if (Strings.isNullOrEmpty(errorOverride)) {
      return condition.failMessage(interaction);
    }

    Map<String, Object> ctx2 = new HashMap<>(interaction.getContext());
    ctx2.put("original", condition.failMessage(interaction));
    return formatString(errorOverride, viewer, ctx2);
  }

  public void clear() {
    conditions.clear();
    actions.clear();
  }

  @Override
  public void fillContext(Map<String, Object> context) {
    context.put("silent", silent);
  }

  @Override
  public boolean interact(Interaction interaction) {
    if (!runConditions(interaction)) {
      return false;
    }

    runActions(interaction);
    return true;
  }

  public void runActions(Interaction interaction) {
    for (Action action : getActions()) {
      action.onUse(interaction);
    }
  }

  @Override
  public void save(CompoundTag tag) {
    tag.putBoolean("silent", silent);

    save(conditions, tag, KEY_CONDITIONS);
    save(actions, tag, KEY_ACTIONS);

    if (!Strings.isNullOrEmpty(globalErrorOverride)) {
      tag.putString(KEY_G_ERROR_OVERRIDE, globalErrorOverride);
    }
  }

  @Override
  public void load(CompoundTag tag) {
    this.silent = tag.getBoolean("silent");

    load(tag.get(KEY_ACTIONS), actions);
    load(tag.get(KEY_CONDITIONS), conditions);

    this.globalErrorOverride = tag.getString(KEY_G_ERROR_OVERRIDE);
  }

  public static void save(ComponentList<?> list, CompoundTag container, String key) {
    list.save(TagOps.OPS)
        .flatMap(binaryTag -> {
          if (binaryTag == null || binaryTag.getId() == TypeIds.END) {
            return Results.error("TAG_End found as save() result????");
          }
          return DataResult.success(binaryTag);
        })
        .mapError(s -> "Failed to save " + key + ": " + s)
        .resultOrPartial(LOGGER::error)
        .ifPresent(binaryTag -> container.put(key, binaryTag));
  }

  public static void load(BinaryTag tag, ComponentList<?> list) {
    if (tag == null) {
      list.clear();
      return;
    }

    list.load(new Dynamic<>(TagOps.OPS, tag));
  }

  @Override
  public void write(TextWriter writer) {
    writer.field("Silent", silent);
    writer.field("Conditions");
    conditions.write(writer, getCommandPrefix() + " tests");

    writer.field("Actions");
    actions.write(writer, getCommandPrefix() + " actions");

    if (!Strings.isNullOrEmpty(globalErrorOverride)) {
      writer.field("Global Error Override", globalErrorOverride);
    }
  }
}
