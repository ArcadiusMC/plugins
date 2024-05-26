package net.arcadiusmc.items.upgrade;

import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.arcadiusmc.items.ExtendedItem;
import net.arcadiusmc.items.ItemComponent;
import net.arcadiusmc.items.lore.LoreElement;
import net.arcadiusmc.items.upgrade.UpgradesImpl.BuilderImpl;
import net.arcadiusmc.text.TextWriter;

public interface ItemUpgrades {

  static Builder builder() {
    return new BuilderImpl();
  }

  ItemComponent createComponent();

  LoreElement createPreviewElement();

  LoreElement createStatusElement();

  interface Upgrade {
    UpgradeFunction getFunction();

    @Nullable
    LoreElement getPreviewText();

    @Nullable
    LoreElement getStatusText();

    boolean statusPersists();

    void writePreview(ExtendedItem item, TextWriter writer);

    void writeStatus(ExtendedItem item, TextWriter writer);
  }

  interface UpgradeBuilder {

    UpgradeFunction getFunction();

    UpgradeBuilder previewText(LoreElement element);

    UpgradeBuilder statusText(LoreElement element);

    UpgradeBuilder statusPersists(Boolean persists);

    Upgrade build();
  }

  interface Builder {

    LevelUpgrades level(int level);

    default Builder level(int level, Consumer<LevelUpgrades> factory) {
      LevelUpgrades upgrades = level(level);
      factory.accept(upgrades);
      return this;
    }

    Builder statusPrefix(LoreElement element);

    Builder previewPrefix(LoreElement element);

    ItemUpgrades build();
  }

  interface LevelUpgrades {

    int getLevel();

    LevelUpgrades upgrade(UpgradeFunction function);

    LevelUpgrades upgrade(UpgradeFunction function, Consumer<UpgradeBuilder> consumer);
  }
}
