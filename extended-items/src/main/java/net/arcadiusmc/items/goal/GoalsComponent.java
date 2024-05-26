package net.arcadiusmc.items.goal;

import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import net.arcadiusmc.items.CallbackComponent;
import net.arcadiusmc.items.ItemComponent;
import net.arcadiusmc.items.Level;
import net.arcadiusmc.items.goal.ItemGoalsImpl.LevelGoalImpl;
import net.forthecrown.nbt.BinaryTags;
import net.forthecrown.nbt.CompoundTag;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.EquipmentSlot;

public class GoalsComponent extends ItemComponent implements CallbackComponent {

  private final Object2FloatMap<String> progress = new Object2FloatOpenHashMap<>();
  private final ItemGoalsImpl goals;

  public GoalsComponent(ItemGoalsImpl goals) {
    this.goals = goals;
  }

  public float getProgress(GoalKey key) {
    return progress.getFloat(key.toString());
  }

  public boolean hasGoalsOfType(String type) {
    int level = Level.getLevel(item);

    if (level < 1) {
      return false;
    }

    int index = level - 1;
    if (index >= goals.levelGoals.length) {
      return false;
    }

    LevelGoalImpl levelGoals = goals.levelGoals[index];

    if (levelGoals == null) {
      return false;
    }

    for (Goal goal : levelGoals.goals) {
      if (goal.trigger().getType().equals(type)) {
        return true;
      }
    }

    return false;
  }

  public void triggerGoal(GoalKey trigger, float value, Player player) {
    String key = trigger.toString();
    float progress = this.progress.getFloat(key) + value;

    Goal goal = findGoal(trigger);

    if (goal == null) {
      return;
    }

    this.progress.put(key, progress);

    if (!(progress >= goal.goal())) {
      return;
    }

    Level level = item.getComponent(Level.class).orElse(null);

    if (level == null) {
      return;
    }

    level.levelUp(player);
    this.progress.clear();
  }

  private Goal findGoal(GoalKey trigger) {
    int level = Level.getLevel(item);

    if (level < 1) {
      return null;
    }

    int index = level - 1;

    if (index >= goals.levelGoals.length) {
      return null;
    }

    LevelGoalImpl levelGoal = goals.levelGoals[index];

    if (levelGoal == null) {
      return null;
    }

    for (Goal goal : levelGoal.goals) {
      if (goal == null) {
        continue;
      }

      if (goal.trigger().equals(trigger)) {
        return goal;
      }
    }

    return null;
  }

  @Override
  public void onMineBlock(BlockBreakEvent event, EquipmentSlot slot) {
    if (slot != EquipmentSlot.HAND || !hasGoalsOfType(GoalKey.TYPE_BLOCK_BREAK)) {
      return;
    }

    GoalKey key = GoalKey.valueOf(GoalKey.TYPE_BLOCK_BREAK, event.getBlock().getType());
    triggerGoal(key, 1f, event.getPlayer());
  }

  @Override
  public void onAttack(Player player, EntityDamageByEntityEvent event, EquipmentSlot slot) {
    if (slot != EquipmentSlot.HAND || !hasGoalsOfType(GoalKey.TYPE_ENTITY_DAMAGE)) {
      return;
    }

    GoalKey key = GoalKey.valueOf(GoalKey.TYPE_ENTITY_DAMAGE, event.getEntityType());
    triggerGoal(key, (float) event.getFinalDamage(), player);
  }

  @Override
  public void onKill(Player player, EntityDeathEvent event, EquipmentSlot slot) {
    if (slot != EquipmentSlot.HAND || !hasGoalsOfType(GoalKey.TYPE_ENTITY_KILL)) {
      return;
    }

    GoalKey key = GoalKey.valueOf(GoalKey.TYPE_ENTITY_KILL, event.getEntityType());
    triggerGoal(key, 1f, player);
  }

  @Override
  public void save(CompoundTag tag) {
    CompoundTag goalTag = BinaryTags.compoundTag();
    progress.forEach(goalTag::putFloat);
    tag.put("goals", goalTag);
  }

  @Override
  public void load(CompoundTag tag) {
    tag.getCompound("goals").forEach((s, binaryTag) -> {
      if (!binaryTag.isNumber()) {
        return;
      }

      progress.put(s, binaryTag.asNumber().floatValue());
    });
  }
}
