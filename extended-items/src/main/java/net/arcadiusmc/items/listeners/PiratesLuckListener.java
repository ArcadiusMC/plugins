package net.arcadiusmc.items.listeners;

import java.util.Map;
import java.util.Random;
import net.arcadiusmc.items.ArcadiusEnchantments;
import net.arcadiusmc.items.ItemTypes;
import net.arcadiusmc.items.goal.GoalKey;
import net.arcadiusmc.items.goal.GoalsComponent;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.Users;
import net.arcadiusmc.utils.inventory.ItemStacks;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class PiratesLuckListener implements Listener {

  static final Drop[] DROPS = {
      new Drop(1, 0.06f, 0.02f),
      new Drop(10, 0.03f, 0.01f),
      new Drop(100, 0.01f, 0.005f),
  };

  static final Random random = new Random();

  @EventHandler(ignoreCancelled = true)
  public void onBlockBreak(BlockBreakEvent event) {
    if (!ArcadiusEnchantments.ENABLED) {
      return;
    }

    Player player = event.getPlayer();
    ItemStack held = player.getInventory().getItemInMainHand();

    if (ItemStacks.isEmpty(held)) {
      return;
    }

    ItemMeta meta = held.getItemMeta();
    Map<Enchantment, Integer> enchantMap = meta.getEnchants();

    if (!enchantMap.containsKey(ArcadiusEnchantments.PIRATES_LUCK)) {
      return;
    }

    int level = enchantMap.get(ArcadiusEnchantments.PIRATES_LUCK);
    int reward = run(event, level);

    if (reward < 1) {
      return;
    }

    ItemTypes.getItem(held)
        .flatMap(item -> item.getComponent(GoalsComponent.class))
        .ifPresent(goals -> {
          Material material = event.getBlock().getType();
          goals.triggerGoal(GoalKey.blockBreak(material), 1f, player);
        });
  }

  public static int run(BlockBreakEvent event, int level) {
    Player player = event.getPlayer();

    for (Drop drop : DROPS) {
      float dropChance = drop.baseDropChance + ((level - 1) * drop.levelIncrease);
      float rand = random.nextFloat();

      if (rand > dropChance) {
        continue;
      }

      int reward = drop.dropAmount;
      User user = Users.get(player);

      user.addBalance(reward);

      user.sendActionBar(
          Messages.render("itemsPlugin.spade.foundMoney")
              .addValue("amount", Messages.currency(reward))
              .create(user)
      );

      return reward;
    }

    return 0;
  }

  private record Drop(int dropAmount, float baseDropChance, float levelIncrease) {

  }
}
