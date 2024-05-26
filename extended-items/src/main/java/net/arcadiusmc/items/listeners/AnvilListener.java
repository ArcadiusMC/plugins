package net.arcadiusmc.items.listeners;

import java.util.Map;
import net.arcadiusmc.items.ItemPlugin;
import net.arcadiusmc.utils.inventory.ItemStacks;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

public class AnvilListener implements Listener {

  private final ItemPlugin plugin;

  public AnvilListener(ItemPlugin plugin) {
    this.plugin = plugin;
  }

  @EventHandler(ignoreCancelled = true)
  public void onPrepareAnvil(PrepareAnvilEvent event) {
    if (!plugin.getItemsConfig().allowOpEnchants()) {
      return;
    }

    // Empty result slot means it's not prepared
    if (ItemStacks.isEmpty(event.getResult())) {
      return;
    }

    ItemStack first = event.getInventory().getFirstItem();
    ItemStack second = event.getInventory().getSecondItem();

    if (ItemStacks.isEmpty(second)) {
      return;
    }

    ItemMeta firstMeta = first.getItemMeta();
    ItemMeta secondMeta = second.getItemMeta();

    // Neither has enchants, we don't care
    if (!hasEnchants(firstMeta) && !hasEnchants(secondMeta)) {
      return;
    }

    ItemStack result = event.getResult();
    ItemMeta resultMeta = result.getItemMeta();
    Map<Enchantment, Integer> resultEnchants = getEnchantments(resultMeta);

    for (var e : resultEnchants.entrySet()) {
      Enchantment enchant = e.getKey();

      int firstLevel = getLevel(firstMeta, enchant);
      int secondLevel = getLevel(secondMeta, enchant);
      int maxLevel = enchant.getMaxLevel();

      // If either level is above max,
      // use the bigger level
      if (firstLevel <= maxLevel && secondLevel <= maxLevel) {
        continue;
      }

      int level = Math.max(firstLevel, secondLevel);

      // Same level, increment
      if (firstLevel == secondLevel) {
        level++;
      }

      addEnchant(resultMeta, enchant, level);
    }

    result.setItemMeta(resultMeta);
    event.setResult(result);
  }

  Map<Enchantment, Integer> getEnchantments(ItemMeta meta) {
    if (meta instanceof EnchantmentStorageMeta storageMeta) {
      return storageMeta.getStoredEnchants();
    }

    return meta.getEnchants();
  }

  boolean hasEnchants(ItemMeta meta) {
    if (meta instanceof EnchantmentStorageMeta storageMeta) {
      return storageMeta.hasStoredEnchants();
    }
    return meta.hasEnchants();
  }

  int getLevel(ItemMeta meta, Enchantment enchantment) {
    if (meta instanceof EnchantmentStorageMeta storage) {
      return storage.getStoredEnchantLevel(enchantment);
    } else {
      return meta.getEnchantLevel(enchantment);
    }
  }

  void addEnchant(ItemMeta meta, Enchantment enchantment, int level) {
    if (meta instanceof EnchantmentStorageMeta storage) {
      storage.addStoredEnchant(enchantment, level, true);
    } else {
      meta.addEnchant(enchantment, level, true);
    }
  }
}