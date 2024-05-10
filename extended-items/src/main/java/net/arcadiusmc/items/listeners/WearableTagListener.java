package net.arcadiusmc.items.listeners;

import net.arcadiusmc.Loggers;
import net.arcadiusmc.items.ItemPlugin;
import net.arcadiusmc.items.ItemsConfig;
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
import org.bukkit.inventory.meta.ItemMeta;
import org.slf4j.Logger;

public class WearableTagListener implements Listener {

  private static final Logger LOGGER = Loggers.getLogger();
  static final String TAG = "wearable";

  static final Sound EQUIP_SOUND = Sound.sound()
      .type(Key.key("item.armor.equip_iron"))
      .source(Source.PLAYER)
      .build();

  private final ItemPlugin plugin;

  public WearableTagListener(ItemPlugin plugin) {
    this.plugin = plugin;
  }

  boolean isWearable(ItemStack item) {
    if (ItemStacks.isEmpty(item)) {
      return false;
    }

    Material type = item.getType();
    if (type.name().contains("HELMET")) {
      return false;
    }

    ItemsConfig config = plugin.getItemsConfig();
    if (!config.allowWearable()) {
      return false;
    }

    ItemMeta meta = item.getItemMeta();

    if (config.wearableIds().length > 0 && meta.hasCustomModelData()) {
      int modelData = meta.getCustomModelData();

      for (int i : config.wearableIds()) {
        if (i != modelData) {
          continue;
        }

        return true;
      }
    }

    return ItemStacks.hasTagElement(meta, TAG);
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
