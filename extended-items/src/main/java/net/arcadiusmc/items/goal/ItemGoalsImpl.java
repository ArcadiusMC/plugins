package net.arcadiusmc.items.goal;

import it.unimi.dsi.fastutil.objects.ObjectArrays;
import java.util.ArrayList;
import java.util.List;
import net.arcadiusmc.items.ExtendedItem;
import net.arcadiusmc.items.ItemComponent;
import net.arcadiusmc.items.Level;
import net.arcadiusmc.items.lore.LoreElement;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.TextWriter;
import net.kyori.adventure.text.Component;

class ItemGoalsImpl implements ItemGoals {

  final LevelGoalImpl[] levelGoals;
  final LoreElement prefix;

  public ItemGoalsImpl(LevelGoalImpl[] levelGoals, LoreElement prefix) {
    this.levelGoals = levelGoals;
    this.prefix = prefix;
  }

  @Override
  public ItemComponent createComponent() {
    return new GoalsComponent(this);
  }

  @Override
  public LoreElement createGoalText() {
    return new GoalLore();
  }

  class GoalLore implements LoreElement {

    @Override
    public void writeLore(ExtendedItem item, TextWriter writer) {
      Level levelComponent = item.getComponent(Level.class).orElse(null);
      GoalsComponent goals = item.getComponent(GoalsComponent.class).orElse(null);

      if (levelComponent == null || goals == null) {
        return;
      }

      int level = levelComponent.getLevel();
      int index = level - 1;

      if (index >= levelGoals.length) {
        return;
      }

      LevelGoalImpl levelGoal = levelGoals[index];

      if (levelGoal == null) {
        return;
      }

      Goal[] goalArray = levelGoal.goals;

      if (goalArray.length < 1) {
        return;
      }

      if (prefix != null) {
        prefix.writeLore(item, writer);
      }

      writer.line(Messages.renderText("itemsPlugin.goals.header"));

      for (Goal goal : goalArray) {
        float progress = goals.getProgress(goal.trigger());

        Component line = Messages.render("itemsPlugin.goals.line")
            .addValue("entry", goal.displayName())
            .addValue("progress", progress)
            .addValue("goal", goal.goal())
            .asComponent();

        writer.line(line);
      }
    }
  }

  static class BuilderImpl implements Builder {

    LevelBuilder[] goals = new LevelBuilder[0];
    LoreElement prefix;

    @Override
    public LevelGoal level(int level) {
      goals = ObjectArrays.ensureCapacity(goals, level);
      int index = level - 1;

      LevelBuilder goal = goals[index];

      if (goal == null) {
        return goals[index] = new LevelBuilder(level);
      }

      return goal;
    }

    @Override
    public Builder prefixedWith(LoreElement element) {
      this.prefix = element;
      return this;
    }

    @Override
    public ItemGoals build() {
      LevelGoalImpl[] built = new LevelGoalImpl[goals.length];
      for (int i = 0; i < goals.length; i++) {
        LevelBuilder g = goals[i];

        if (g == null) {
          continue;
        }

        built[i] = g.build();
      }

      return new ItemGoalsImpl(built, prefix);
    }
  }

  static class LevelBuilder implements LevelGoal {

    private final int level;
    private final List<Goal> goals = new ArrayList<>();

    public LevelBuilder(int level) {
      this.level = level;
    }

    @Override
    public LevelGoal add(Goal goal) {
      goals.add(goal);
      return this;
    }

    public LevelGoalImpl build() {
      return new LevelGoalImpl(level, goals.toArray(Goal[]::new));
    }
  }

  static class LevelGoalImpl {
    final int level;
    final Goal[] goals;

    public LevelGoalImpl(int level, Goal[] goals) {
      this.level = level;
      this.goals = goals;
    }
  }
}
