package net.arcadiusmc.items.tools;

import static net.kyori.adventure.text.Component.text;

import lombok.Getter;
import lombok.Setter;
import net.arcadiusmc.items.ArcadiusEnchantments;
import net.arcadiusmc.items.CallbackComponent;
import net.arcadiusmc.items.ExtendedItem;
import net.arcadiusmc.items.ItemComponent;
import net.arcadiusmc.items.goal.GoalKey;
import net.arcadiusmc.items.goal.GoalsComponent;
import net.arcadiusmc.items.listeners.PiratesLuckListener;
import net.arcadiusmc.items.lore.LoreElement;
import net.arcadiusmc.text.RomanNumeral;
import net.arcadiusmc.text.TextWriter;
import net.forthecrown.nbt.CompoundTag;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.EquipmentSlot;

@Getter @Setter
public class PiratesLuck extends ItemComponent implements CallbackComponent, LoreElement {

  int level = 1;

  @Override
  public void onMineBlock(BlockBreakEvent event, EquipmentSlot slot) {
    if (ArcadiusEnchantments.ENABLED) {
      return;
    }

    int reward = PiratesLuckListener.run(event, level);

    if (reward < 1) {
      return;
    }

    GoalsComponent goals = item.getComponent(GoalsComponent.class).orElse(null);

    if (goals == null) {
      return;
    }

    goals.triggerGoal(GoalKey.valueOf(SpadeItem.GOAL_TYPE, reward + "x"), 1, event.getPlayer());
  }

  @Override
  public void save(CompoundTag tag) {
    tag.putInt("pirates_luck_level", level);
  }

  @Override
  public void load(CompoundTag tag) {
    level = tag.getInt("pirates_luck_level");
  }

  @Override
  public void writeLore(ExtendedItem item, TextWriter writer) {
    if (ArcadiusEnchantments.ENABLED) {
      return;
    }

    writer.line(
        text("Pirate's Luck " + RomanNumeral.arabicToRoman(level), NamedTextColor.GRAY)
    );
  }
}
