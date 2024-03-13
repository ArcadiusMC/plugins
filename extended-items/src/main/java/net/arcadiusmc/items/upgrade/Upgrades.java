package net.arcadiusmc.items.upgrade;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import net.arcadiusmc.items.ExtendedItem;
import net.arcadiusmc.items.ItemComponent;
import net.arcadiusmc.items.Level;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class Upgrades extends ItemComponent {

  private final LevelUpgrade[] upgrades;

  private Upgrades(LevelUpgrade[] upgrades) {
    this.upgrades = upgrades;
  }

  public static UpgradeBuilder builder() {
    return new UpgradeBuilder();
  }

  @Override
  public void onUpdate(ItemMeta meta, ItemStack stack) {
    int level = Level.getLevel(item);
    if (level == -1) {
      return;
    }

    for (LevelUpgrade upgrade : upgrades) {
      if (upgrade.level != level) {
        continue;
      }

      upgrade.apply(item, meta, stack);
    }
  }

  public record LevelUpgrade(int level, ItemUpgrade[] upgrades) {

    void apply(ExtendedItem item, ItemMeta meta, ItemStack stack) {
      for (ItemUpgrade upgrade : upgrades) {
        upgrade.apply(item, meta, stack);
      }
    }
  }

  public static class UpgradeBuilder {
    LevelUpgrade[] cachedResult;
    List<LevelUpgrade> upgrades = new ArrayList<>();

    int currentLevel = -1;
    List<ItemUpgrade> currentUpgrades;

    private void maybePushLevel() {
      if (currentLevel == -1 || currentUpgrades == null || currentUpgrades.isEmpty()) {
        return;
      }

      ItemUpgrade[] arr = currentUpgrades.toArray(ItemUpgrade[]::new);
      LevelUpgrade levelUpgrade = new LevelUpgrade(currentLevel, arr);
      upgrades.add(levelUpgrade);

      currentLevel = -1;
      currentUpgrades = null;

      cachedResult = null;
    }

    public UpgradeBuilder level(int level) {
      maybePushLevel();

      currentLevel = level;
      currentUpgrades = new ArrayList<>(10);

      return this;
    }

    public UpgradeBuilder upgrade(ItemUpgrade upgrade) {
      Objects.requireNonNull(upgrade, "Null upgrade");

      if (currentLevel == -1) {
        throw new IllegalStateException("No level set");
      }

      currentUpgrades.add(upgrade);
      cachedResult = null;

      return this;
    }

    public List<LevelUpgrade> getUpgrades() {
      return Collections.unmodifiableList(upgrades);
    }

    public Upgrades build() {
      if (cachedResult != null) {
        return new Upgrades(cachedResult);
      }

      maybePushLevel();
      LevelUpgrade[] upgradeArray = upgrades.toArray(LevelUpgrade[]::new);
      return new Upgrades(upgradeArray);
    }
  }
}
