package net.arcadiusmc.core;

import static net.arcadiusmc.text.Text.nonItalic;

import it.unimi.dsi.fastutil.objects.ObjectIntPair;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.arcadiusmc.events.CoinCreationEvent;
import net.arcadiusmc.events.CoinDepositEvent;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.user.User;
import net.arcadiusmc.utils.inventory.ItemArrayList;
import net.arcadiusmc.utils.inventory.ItemList;
import net.arcadiusmc.utils.inventory.ItemStacks;
import net.forthecrown.nbt.CompoundTag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Class for server items, such as Royal Swords, Crowns and home of the great makeItem method
 */
public final class Coins {
  private Coins() {}

  public static final int TEXTURE_ID = 10070001;
  public static final Material COIN_MATERIAL = Material.SUNFLOWER;

  public static final String NBT_TAG = "coin_worth";

  public static final Style NON_ITALIC_DARK_GRAY = Style.style(NamedTextColor.DARK_GRAY)
      .decoration(TextDecoration.ITALIC, false);

  /**
   * Make some coins
   *
   * @param amount     The amount the coin(s) will be worth
   * @param itemAmount The amount of seperate coins to make
   * @return The created coin(s)
   */
  public static ItemStack makeCoins(int amount, int itemAmount, User user) {
    var builder = ItemStacks.builder(COIN_MATERIAL, itemAmount)
        .setModelData(TEXTURE_ID)
        .setNameRaw(Messages.currencyUnit(false).style(nonItalic(NamedTextColor.GOLD)));

    for (Component component : CoreMessages.coinLore(amount)) {
      builder.addLoreRaw(component.applyFallbackStyle(NON_ITALIC_DARK_GRAY));
    }

    CoinCreationEvent event = new CoinCreationEvent(builder, user, amount);
    event.callEvent();

    return builder.build();
  }

  public static boolean isCoin(ItemStack item) {
    if (ItemStacks.isEmpty(item) || item.getType() != COIN_MATERIAL) {
      return false;
    }

    CompoundTag unhandledTags = ItemStacks.getUnhandledTags(item.getItemMeta());
    return unhandledTags.getNumberOptional(NBT_TAG).isPresent();
  }

  public static int deposit(User user, Iterator<ItemStack> it, int maxCoins) {
    var allCoins  = collectCoins(it);
    ItemList deposited = new ItemArrayList();

    int trueMax = maxCoins == -1 ? Integer.MAX_VALUE : maxCoins;
    int coins   = 0;
    int earned  = 0;

    for (ObjectIntPair<ItemStack> coinValuePair : allCoins) {
      int singletonValue = coinValuePair.rightInt();
      ItemStack coin = coinValuePair.left();

      int coinAmount = coin.getAmount();
      int nCoins = coins + coinAmount;

      if (nCoins > trueMax) {
        // 'over' is the amount of items that depositing this coin would put us over the
        // max deposit limit, it thus also becomes the remaining amount that will be left
        // after depositing the item
        int over = nCoins - trueMax;
        int itemShrink = coinAmount - over;

        ItemStack cloned = coin.clone();
        cloned.setAmount(itemShrink);
        deposited.add(cloned);

        coin.subtract(itemShrink);

        coins = trueMax;
        earned += singletonValue * itemShrink;
      } else {
        earned += coinAmount * singletonValue;
        coins += coinAmount;

        ItemStack cloned = coin.clone();
        coin.setAmount(0);

        deposited.add(cloned);
      }
    }

    if (earned < 1) {
      return 0;
    }

    CoinDepositEvent event = new CoinDepositEvent(user, deposited, coins, earned);
    event.callEvent();

    if (event.isCancelled() || event.getEarned() < 1) {
      return 0;
    }

    earned = event.getEarned();
    coins = event.getDepositedCoins();

    user.addBalance(earned);
    user.sendMessage(CoreMessages.deposit(user, coins, earned));
    user.playSound(Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);

    return earned;
  }

  private static List<ObjectIntPair<ItemStack>> collectCoins(Iterator<ItemStack> it) {
    List<ObjectIntPair<ItemStack>> coins = new ArrayList<>();

    while (it.hasNext()) {
      var n = it.next();

      if (ItemStacks.isEmpty(n)) {
        continue;
      }

      ItemMeta meta = n.getItemMeta();
      CompoundTag tags = ItemStacks.getUnhandledTags(meta);

      var opt = tags.getNumberOptional(NBT_TAG);

      if (opt.isEmpty()) {
        continue;
      }

      coins.add(ObjectIntPair.of(n, opt.get().intValue()));
    }

    return coins;
  }
}