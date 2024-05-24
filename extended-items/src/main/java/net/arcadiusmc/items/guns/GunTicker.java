package net.arcadiusmc.items.guns;

import java.util.Set;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.utils.inventory.ItemStacks;
import net.arcadiusmc.utils.inventory.ItemStacks.NonEmptyItemIterator;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import org.spongepowered.math.GenericMath;

public final class GunTicker extends TickingObject {
  private GunTicker() {}

  private static final int OFFHAND_SLOT = 40;

  private static final Set<EquipmentSlot> TICKED_SLOTS = Set.of(
      EquipmentSlot.HAND,
      EquipmentSlot.OFF_HAND
  );

  public static final GunTicker TICKER = new GunTicker();

  @Override
  public void tick() {
    for (Player player : Bukkit.getOnlinePlayers()) {
      updateItems(player);

      PlayerInventory inventory = player.getInventory();

      for (EquipmentSlot tickedSlot : TICKED_SLOTS) {
        ItemStack item = inventory.getItem(tickedSlot);
        Gun gun = GunTypes.getGun(item);

        if (gun == null) {
          continue;
        }

        gun.tick(player);

        Component actionText = gun.hotbarText();
        Component itemName = Text.itemDisplayName(item);
        Component text = Component.textOfChildren(itemName, Component.text(": "), actionText);
        player.sendActionBar(text);

        item.editMeta(Damageable.class, meta -> {
          setDurability(gun, meta, item.getType());
        });

        gun.getItem().update();
      }
    }
  }

  void updateItems(Player player) {
    PlayerInventory inventory = player.getInventory();
    NonEmptyItemIterator it = ItemStacks.nonEmptyIterator(inventory);
    int selectedIndex = inventory.getHeldItemSlot();

    while (it.hasNext()) {
      int index = it.nextIndex();
      ItemStack n = it.next();

      Gun gun = GunTypes.getGun(n);

      if (gun == null) {
        continue;
      }

      n.editMeta(Damageable.class, damageable -> {
        setDurability(gun, damageable, n.getType());
      });

      if (index == selectedIndex || index == OFFHAND_SLOT) {
        continue;
      }

      // Reset reloads if not in hand or offhand
      if (gun.getReloadTicks() > 0) {
        gun.setReloadTicks(gun.getReloadDelay());
        gun.getItem().update();
      }
    }
  }

  void setDurability(Gun gun, Damageable meta, Material material) {
    if (gun.hasUnlimitedAmmo() || gun.getMaxAmmo() < 1 || gun.getClipSize() < 1) {
      return;
    }

    int max = gun.getClipSize();
    int ammo = gun.getRemainingClipAmmo();

    double ratio = 1.0D - (double) ammo / max;

    int maxDamage = material.getMaxDurability();
    int damage = GenericMath.clamp((int) (maxDamage * ratio), 1, maxDamage - 1);

    meta.setDamage(damage);
  }
}
