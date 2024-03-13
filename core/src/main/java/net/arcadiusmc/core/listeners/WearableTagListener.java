package net.arcadiusmc.core.listeners;

import net.arcadiusmc.Loggers;
import net.arcadiusmc.utils.inventory.ItemStacks;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.Sound.Source;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.slf4j.Logger;

public class WearableTagListener implements Listener {

  private static final Logger LOGGER = Loggers.getLogger();
  static final String TAG = "wearable";

  static final Sound EQUIP_SOUND = Sound.sound()
      .type(Key.key("item.armor.equip_iron"))
      .source(Source.PLAYER)
      .build();

  static boolean isWearable(ItemStack item) {
    if (ItemStacks.isEmpty(item)) {
      return false;
    }

    Material type = item.getType();
    if (type.name().contains("HELMET")) {
      return false;
    }

    return ItemStacks.hasTagElement(item.getItemMeta(), TAG);
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onPlayerInteract(PlayerInteractEvent event) {
    if (event.getAction() == Action.PHYSICAL) {
      return;
    }

    ItemStack item = event.getItem();
    if (!isWearable(item)) {
      return;
    }

    Player player = event.getPlayer();
    PlayerInventory inventory = player.getInventory();
    ItemStack helmet = inventory.getHelmet();
    EquipmentSlot slot = event.getHand();

    assert slot != null;

    if (player.getGameMode() == GameMode.SPECTATOR) {
      return;
    }

    event.setUseItemInHand(Result.DENY);

    if (ItemStacks.notEmpty(helmet)) {
      if (helmet.getEnchantmentLevel(Enchantment.BINDING_CURSE) > 0
          && player.getGameMode() != GameMode.CREATIVE
      ) {
        return;
      }
    }

    inventory.setHelmet(item);
    inventory.setItem(slot, helmet);

    player.getWorld().playSound(EQUIP_SOUND, player);
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
  public void onInventoryClick(InventoryClickEvent event) {
    InventoryAction action = event.getAction();
    SlotType slotType = event.getSlotType();
    ClickType clickType = event.getClick();

    if (!(event.getView().getTopInventory() instanceof PlayerInventory inventory)) {
      return;
    }


  }
}
