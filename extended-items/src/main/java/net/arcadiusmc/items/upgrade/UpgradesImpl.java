package net.arcadiusmc.items.upgrade;

import it.unimi.dsi.fastutil.objects.ObjectArrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.arcadiusmc.items.ExtendedItem;
import net.arcadiusmc.items.ItemComponent;
import net.arcadiusmc.items.Level;
import net.arcadiusmc.items.lore.LoreElement;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.TextWriter;
import net.forthecrown.nbt.CompoundTag;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

class UpgradesImpl implements ItemUpgrades {

  private final LoreElement statusPrefix;
  private final LoreElement previewPrefix;
  private final LevelUpgradesImpl[] upgrades;

  private final StatusElement statusElement;
  private final PreviewElement previewElement;

  UpgradesImpl(LevelUpgradesBuilder[] array, LoreElement statusPrefix, LoreElement previewPrefix) {
    this.previewPrefix = previewPrefix;
    this.statusPrefix = statusPrefix;
    this.upgrades = new LevelUpgradesImpl[array.length];

    for (int i = 0; i < array.length; i++) {
      LevelUpgradesBuilder builder = array[i];

      if (builder == null) {
        continue;
      }

      upgrades[i] = builder.build();
    }

    this.previewElement = new PreviewElement();
    this.statusElement = new StatusElement();
  }

  @Override
  public ItemComponent createComponent() {
    return new UpgradeComponent();
  }

  @Override
  public LoreElement createPreviewElement() {
    return previewElement;
  }

  @Override
  public LoreElement createStatusElement() {
    return statusElement;
  }

  private TextWriter createPrefixed(TextWriter writer) {
    return writer.withPrefix(Messages.renderText("itemsPlugin.upgrades.listPrefix"));
  }

  class UpgradeComponent extends ItemComponent {

    private int lastAppliedUpgrade;

    @Override
    public void onUpdate(ItemMeta meta, ItemStack stack) {
      int level = Level.getLevel(item);
      if (level == -1) {
        return;
      }

      if (level == lastAppliedUpgrade) {
        return;
      }

      int index = level - 1;
      if (index >= upgrades.length) {
        return;
      }

      LevelUpgradesImpl levelUpgrades = upgrades[index];

      if (levelUpgrades == null) {
        return;
      }

      for (UpgradeImpl upgrade : levelUpgrades.upgrades) {
        upgrade.getFunction().apply(item, meta, stack);
      }

      lastAppliedUpgrade = level;
    }

    @Override
    public void save(CompoundTag tag) {
      tag.putInt("last_applied_upgrade_level", lastAppliedUpgrade);
    }

    @Override
    public void load(CompoundTag tag) {
      lastAppliedUpgrade = tag.getInt("last_applied_upgrade_level");
    }
  }

  class PreviewElement implements LoreElement {

    @Override
    public void writeLore(ExtendedItem item, TextWriter writer) {
      int level = Level.getLevel(item);

      if (level < 0) {
        return;
      }

      // Level is basically just levelIndex + 1, so just using the level as
      // the index returns the next level
      int next = level;

      if (upgrades.length <= next) {
        return;
      }

      LevelUpgradesImpl levelUpgrades = upgrades[next];

      if (levelUpgrades == null || levelUpgrades.upgrades.length < 1) {
        return;
      }

      if (previewPrefix != null) {
        previewPrefix.writeLore(item, writer);
      }

      Component prefix = Messages.renderText("itemsPlugin.upgrades.nextRank");
      writer.line(prefix);

      TextWriter prefixed = createPrefixed(writer);

      for (UpgradeImpl upgrade : levelUpgrades.upgrades) {
        upgrade.writePreview(item, prefixed);
      }
    }
  }

  class StatusElement implements LoreElement {

    @Override
    public void writeLore(ExtendedItem item, TextWriter writer) {
      int level = Level.getLevel(item);

      if (level < 0) {
        return;
      }

      List<UpgradeImpl> effectiveUpgrades = new ArrayList<>();
      int iterations = Math.min(upgrades.length, level);

      for (int i = 0; i < iterations; i++) {
        LevelUpgradesImpl levelUpgrades = upgrades[i];

        if (levelUpgrades == null) {
          continue;
        }

        for (UpgradeImpl upgrade : levelUpgrades.upgrades) {
          if (upgrade == null) {
            continue;
          }

          if (!upgrade.statusPersists() && levelUpgrades.level != level) {
            continue;
          }

          effectiveUpgrades.add(upgrade);
        }
      }

      if (effectiveUpgrades.isEmpty()) {
        return;
      }

      if (statusPrefix != null) {
        statusPrefix.writeLore(item, writer);
      }

      for (UpgradeImpl effectiveUpgrade : effectiveUpgrades) {
        effectiveUpgrade.writeStatus(item, writer);
      }
    }
  }

  static class BuilderImpl implements Builder {

    private LevelUpgradesBuilder[] builders = new LevelUpgradesBuilder[0];
    private LoreElement statusPrefix;
    private LoreElement previewPrefix;

    @Override
    public LevelUpgrades level(int level) {
      builders = ObjectArrays.ensureCapacity(builders, level);

      int index = level - 1;
      LevelUpgradesBuilder result = builders[index];

      if (result == null) {
        result = new LevelUpgradesBuilder(level);
        builders[index] = result;
      }

      return result;
    }

    @Override
    public Builder statusPrefix(LoreElement element) {
      this.statusPrefix = element;
      return this;
    }

    @Override
    public Builder previewPrefix(LoreElement element) {
      this.previewPrefix = element;
      return this;
    }

    @Override
    public ItemUpgrades build() {
      return new UpgradesImpl(builders, statusPrefix, previewPrefix);
    }
  }

  @Getter
  static class LevelUpgradesImpl {
    private final int level;
    private final UpgradeImpl[] upgrades;

    public LevelUpgradesImpl(int level, List<UpgradeImpl> upgrades) {
      this.level = level;
      this.upgrades = upgrades.toArray(UpgradeImpl[]::new);
    }
  }

  static class LevelUpgradesBuilder implements LevelUpgrades {

    @Getter
    private final int level;

    private final List<UpgradeImpl> upgrades = new ArrayList<>();

    public LevelUpgradesBuilder(int level) {
      this.level = level;
    }

    @Override
    public LevelUpgrades upgrade(UpgradeFunction function) {
      return upgrade(function, null);
    }

    @Override
    public LevelUpgrades upgrade(UpgradeFunction function, Consumer<UpgradeBuilder> consumer) {
      Objects.requireNonNull(function, "Null function");

      if (consumer == null) {
        upgrades.add(new UpgradeImpl(function, null, null, null));
        return this;
      }

      UpgradeBuilderImpl builder = new UpgradeBuilderImpl(function);
      consumer.accept(builder);

      upgrades.add(builder.build());
      return this;
    }

    public LevelUpgradesImpl build() {
      return new LevelUpgradesImpl(level, upgrades);
    }
  }

  @Getter
  static class UpgradeBuilderImpl implements UpgradeBuilder {

    private final UpgradeFunction function;

    private LoreElement previewText;
    private LoreElement statusText;

    private Boolean statusPersists;

    public UpgradeBuilderImpl(UpgradeFunction function) {
      this.function = function;
    }

    @Override
    public UpgradeBuilder previewText(LoreElement element) {
      this.previewText = element;
      return this;
    }

    @Override
    public UpgradeBuilder statusText(LoreElement element) {
      this.statusText = element;
      return this;
    }

    @Override
    public UpgradeBuilder statusPersists(Boolean persists) {
      this.statusPersists = persists;
      return this;
    }

    @Override
    public UpgradeImpl build() {
      return new UpgradeImpl(function, previewText, statusText, statusPersists);
    }
  }

  @Getter
  @RequiredArgsConstructor
  static class UpgradeImpl implements Upgrade {

    private final UpgradeFunction function;

    private final LoreElement previewText;
    private final LoreElement statusText;

    private final Boolean statusPersists;

    @Override
    public boolean statusPersists() {
      if (statusPersists != null) {
        return statusPersists;
      }

      return function.statusPersists();
    }

    @Override
    public void writePreview(ExtendedItem item, TextWriter writer) {
      if (previewText != null) {
        previewText.writeLore(item, writer);
        return;
      }

      function.writePreview(item, writer);
    }

    @Override
    public void writeStatus(ExtendedItem item, TextWriter writer) {
      if (statusText != null) {
        statusText.writeLore(item, writer);
        return;
      }

      function.writeStatus(item, writer);
    }
  }
}
