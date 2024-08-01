package net.arcadiusmc.items.listeners;

import java.util.Collection;
import java.util.Random;
import net.arcadiusmc.items.ArcadiusEnchantments;
import net.arcadiusmc.items.ItemPlugin;
import net.arcadiusmc.items.ItemsConfig;
import net.arcadiusmc.items.NonNatural;
import net.arcadiusmc.utils.inventory.ItemStacks;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

public class DoubleDropListener implements Listener {

  static final Random RANDOM = new Random();

  private final ItemPlugin plugin;
  private final NonNatural tracker;

  public DoubleDropListener(ItemPlugin plugin) {
    this.plugin = plugin;
    this.tracker = plugin.getNonNatural();
  }

  @EventHandler(ignoreCancelled = true)
  public void onBlockBreak(BlockBreakEvent event) {
    Block block = event.getBlock();

    if (!tracker.isNatural(block.getWorld(), block.getX(), block.getY(), block.getZ())) {
      return;
    }

    processDoubleDrops(event);
    tracker.setNonNatural(block.getWorld(), block.getX(), block.getY(), block.getZ());
  }

  void processDoubleDrops(BlockBreakEvent event) {
    ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
    if (ItemStacks.isEmpty(item)) {
      return;
    }

    int level = getDoubleDropEnchantLevel(item);
    float rate = getRate(level);

    if (rate <= 0) {
      return;
    }

    if (RANDOM.nextFloat() > rate) {
      return;
    }

    event.setDropItems(false);

    Block block = event.getBlock();
    Player player = event.getPlayer();
    World world = block.getWorld();

    Location l = block.getLocation();

    Collection<ItemStack> drops = block.getDrops(item, player);

    for (ItemStack drop : drops) {
      world.dropItemNaturally(l, drop.clone());
      world.dropItemNaturally(l, drop.clone());
    }
  }

  @EventHandler(ignoreCancelled = true)
  public void onBlockPlace(BlockPlaceEvent event) {
    Block b = event.getBlock();
    tracker.setNonNatural(b.getWorld(), b.getX(), b.getY(), b.getZ());
  }

  float getRate(float level) {
    if (level < 1) {
      return 0.0f;
    }

    ItemsConfig cfg = plugin.getItemsConfig();
    float base = cfg.doubleDropBase();
    float increase = cfg.doubleDropIncrease();
    float max = cfg.doubleDropMax();

    float uncapped = base + (increase * (level - 1));
    return Math.clamp(uncapped, base, max);
  }

  int getDoubleDropEnchantLevel(ItemStack item) {
    int lvl = item.getEnchantmentLevel(ArcadiusEnchantments.IMPERIAL_DUPING);

    if (lvl > 0) {
      return lvl;
    }

    return item.getEnchantmentLevel(ArcadiusEnchantments.CUTTING_MASTERY);
  }
}
