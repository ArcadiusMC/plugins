package net.arcadiusmc.sellshop;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import net.arcadiusmc.menu.MenuNode;
import net.arcadiusmc.menu.Slot;
import net.arcadiusmc.sellshop.data.ItemSellData;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.user.UserProperty;
import net.arcadiusmc.utils.inventory.DefaultItemBuilder;
import net.arcadiusmc.utils.inventory.ItemStacks;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

public final class SellShopNodes {
  private SellShopNodes() {}

  /* ----------------------------- CONSTANTS ------------------------------ */

  private static final ItemStack PREVIOUS_PAGE_ITEM = ItemStacks.builder(Material.PAPER)
      .setName("&e< Previous Page")
      .addLore("&7Return to the main menu")
      .build();

  /**
   * Node to toggle selling items with names
   */
  public static final MenuNode SELLING_NAMED
      = toggleProperty("Named Items", SellProperties.SELLING_NAMED);

  /**
   * Node to toggle selling items with lore
   */
  public static final MenuNode SELLING_LORE
      = toggleProperty("Items With Lore", SellProperties.SELLING_LORE);

  public static final MenuNode SELL_PER_1 = sellAmountOption(SellAmount.PER_1);
  public static final MenuNode SELL_PER_16 = sellAmountOption(SellAmount.PER_16);
  public static final MenuNode SELL_PER_64 = sellAmountOption(SellAmount.PER_64);
  public static final MenuNode SELL_PER_ALL = sellAmountOption(SellAmount.ALL);

  /**
   * Slot 2 node map of sell amount options
   */
  public static final Map<Slot, MenuNode> SELL_AMOUNT_NODES = ImmutableMap.<Slot, MenuNode>builder()
      .put(Slot.of(8, 1), SELL_PER_1)
      .put(Slot.of(8, 2), SELL_PER_16)
      .put(Slot.of(8, 3), SELL_PER_64)
      .put(Slot.of(8, 4), SELL_PER_ALL)
      .build();

  /**
   * Node to toggle selling compacted items
   */
  public static final MenuNode COMPACT_TOGGLE = MenuNode.builder()
      .setItem((user, context) -> {
        boolean compacted = user.get(SellProperties.COMPACTED);

        DefaultItemBuilder builder = ItemStacks.builder(compacted ? Material.IRON_BLOCK : Material.IRON_INGOT)
            .setName(Messages.renderText("sellshop.compact.name", user))
            .addLore(Messages.renderText("sellshop.compact.lore", user));

        if (compacted) {
          builder.addLore(Messages.renderText("sellshop.compact.state.on", user));
        } else {
          builder.addLore(Messages.renderText("sellshop.compact.state.off", user));
        }

        return builder.build();
      })
      .setRunnable((user, context) -> {
        user.flip(SellProperties.COMPACTED);
        context.shouldReloadMenu(true);
      })
      .build();

  /* ----------------------------- STATICS ------------------------------ */

  private static MenuNode toggleProperty(String name, UserProperty<Boolean> property) {
    return MenuNode.builder()
        .setItem(user -> {
          var builder = ItemStacks.builder(Material.BLACK_STAINED_GLASS_PANE)
              .setName("&e" + (user.get(property) ? "Selling " : "Ignoring ") + name)
              .addLore("&7Click to switch");

          if (user.get(property)) {
            builder
                .addEnchant(Enchantment.BINDING_CURSE, 1)
                .setFlags(ItemFlag.HIDE_ENCHANTS);
          }

          return builder.build();
        })

        .setRunnable((user, context) -> {
          user.flip(property);
          context.shouldReloadMenu(true);
        })

        .build();
  }

  private static MenuNode sellAmountOption(SellAmount amount) {
    return MenuNode.builder()
        .setItem(user -> {
          var builder = ItemStacks.builder(Material.BLACK_STAINED_GLASS_PANE)
              .setAmount(amount.getItemAmount())
              .setName(amount.getSellPerText())
              .addLore("&7Set the amount of items you")
              .addLore("&7will sell per click");

          if (user.get(SellProperties.SELL_AMOUNT) == amount) {
            builder.addEnchant(Enchantment.BINDING_CURSE, 1)
                .setFlags(ItemFlag.HIDE_ENCHANTS);
          }

          return builder.build();
        })

        .setRunnable((user, context) -> {
          var current = user.get(SellProperties.SELL_AMOUNT);

          if (current == amount) {
            return;
          }

          user.set(SellProperties.SELL_AMOUNT, amount);
          context.shouldReloadMenu(true);
        })

        .build();
  }

  /**
   * Creates a menu node to sell the given data's items
   *
   * @param data The data to create the node for
   * @return The created node
   */

  public static MenuNode sellNode(ItemSellData data, boolean autoSellAllowed) {
    SellableItemNode node = new SellableItemNode(data, autoSellAllowed);
    return MenuNode.builder().setItem(node).setRunnable(node).setPlaySound(false).build();
  }
}