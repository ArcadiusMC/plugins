package net.arcadiusmc.items.goal;

import java.util.function.Consumer;
import net.arcadiusmc.items.ItemComponent;
import net.arcadiusmc.items.goal.ItemGoalsImpl.BuilderImpl;
import net.arcadiusmc.items.lore.LoreElement;

public interface ItemGoals {

  static Builder builder() {
    return new BuilderImpl();
  }

  ItemComponent createComponent();

  LoreElement createGoalText();

  interface Builder {

    default Builder level(int level, Consumer<LevelGoal> consumer) {
      LevelGoal goal = level(level);

      if (consumer != null) {
        consumer.accept(goal);
      }

      return this;
    }

    LevelGoal level(int level);

    Builder prefixedWith(LoreElement element);

    ItemGoals build();
  }

  interface LevelGoal {

    LevelGoal add(Goal goal);
  }
}
