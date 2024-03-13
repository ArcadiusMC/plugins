package net.arcadiusmc.items.wreath;

import net.arcadiusmc.items.CallbackComponent;
import net.arcadiusmc.items.ItemComponent;
import net.arcadiusmc.items.Level;
import net.forthecrown.nbt.CompoundTag;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

class PostDamageEffect extends ItemComponent implements CallbackComponent {

  private final PotionEffectType type;
  private final int potionDuration;
  private final int startingLevel;
  private final long useIntervalMillis;
  private final String nbtKey;

  private long lastApplied;

  public PostDamageEffect(
      PotionEffectType type,
      int duration,
      int startingItemLevel,
      long useIntervalMillis
  ) {
    this.type = type;
    this.potionDuration = duration;
    this.startingLevel = startingItemLevel;
    this.useIntervalMillis = useIntervalMillis;

    this.nbtKey = "last_applied_post_dmg_" + type.key().value();
  }

  @Override
  public void onHolderDamaged(Player player, EntityDamageEvent event, EquipmentSlot slot) {
    if (slot != EquipmentSlot.HEAD) {
      return;
    }

    if (!Level.levelMatches(item, startingLevel)) {
      return;
    }

    long currentTime = System.currentTimeMillis();
    if (lastApplied > 0) {
      long timeDif = currentTime - lastApplied;

      if (timeDif < useIntervalMillis) {
        return;
      }

      lastApplied = currentTime;
    }

    PotionEffect effect = new PotionEffect(type, potionDuration, 0);
    player.addPotionEffect(effect);
  }

  @Override
  public void save(CompoundTag tag) {
    if (lastApplied <= 0) {
      return;
    }

    tag.putLong(nbtKey, lastApplied);
  }

  @Override
  public void load(CompoundTag tag) {
    lastApplied = tag.getLong(nbtKey, 0L);
  }
}
