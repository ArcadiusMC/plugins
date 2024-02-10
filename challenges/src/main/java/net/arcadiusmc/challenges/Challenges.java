package net.arcadiusmc.challenges;


import java.util.Random;
import java.util.function.Consumer;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.menu.Menu;
import net.arcadiusmc.menu.MenuBuilder;
import net.arcadiusmc.menu.MenuNode;
import net.arcadiusmc.menu.Menus;
import net.arcadiusmc.menu.Slot;
import net.arcadiusmc.registry.Holder;
import net.arcadiusmc.registry.Registry;
import net.arcadiusmc.sellshop.SellShop;
import net.arcadiusmc.sellshop.SellShopNodes;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.utils.inventory.ItemStacks;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Utility class for challenge-related functions
 */
public final class Challenges {
  private Challenges() {}

  public static final String METHOD_STREAK_INCREASE = "onStreakIncrease";

  public static final Random RANDOM = new Random();

  public static ChallengesPlugin getPlugin() {
    return JavaPlugin.getPlugin(ChallengesPlugin.class);
  }
  
  public static ChallengeManager getManager() {
    return getPlugin().getChallenges();
  }
  
  public static void trigger(String challengeName, Object input) {
    apply(challengeName, challenge -> challenge.trigger(input));
  }

  public static boolean isActive(Challenge challenge) {
    var manager = getManager();

    return manager
        .getChallengeRegistry()
        .getHolderByValue(challenge)
        .map(holder -> manager.getActiveChallenges().contains(holder))
        .orElse(false);
  }

  public static void apply(Challenge challenge, Consumer<Holder<Challenge>> consumer) {
    getManager()
        .getChallengeRegistry()
        .getHolderByValue(challenge)
        .ifPresent(holder -> {
          if (!isActive(holder.getValue())) {
            return;
          }

          consumer.accept(holder);
        });
  }

  public static void apply(String challengeName, Consumer<Challenge> consumer) {
    getManager()
        .getChallengeRegistry()
        .get(challengeName)
        .ifPresent(challenge -> {
          if (!isActive(challenge)) {
            return;
          }

          consumer.accept(challenge);
        });
  }

  static Menu createItemMenu(Registry<Challenge> challenges, SellShop shop) {
    MenuBuilder builder = Menus.builder(Menus.MAX_INV_SIZE - 9)
        .setTitle("Daily Item Challenges")
        .addBorder()

        // < Go back
        .add(Slot.ZERO, SellShopNodes.previousPage(shop))

        // Header
        .add(Slot.of(4), createMenuHeader())

        .add(Slot.of(4, 4),
            MenuNode.builder()
                .setItem((user, context) -> {
                  return ItemStacks.builder(Material.BOOK)
                      .setName("&eInfo")
                      .addLore("&7This challenge is reset daily. Complete it to build a streak.")
                      .addLore("&7The longer your streak, the greater the rewards!")
                      .addLore("")
                      .addLore("&7Rewards include:")
                      .addLore("&7Rhines, Gems, Guild EXP and mob Plushies")
                      .build();
                })
                .build()
        );

    for (var h : challenges.entries()) {
      if (!(h.getValue() instanceof ItemChallenge item)) {
        continue;
      }

      Loggers.getLogger().debug("Adding item challenge to shop menu: {}",
          h.getKey()
      );

      builder.add(
          item.getMenuSlot(),
          item.toInvOption()
      );
    }

    return builder.build();
  }

  public static MenuNode createMenuHeader() {
    return MenuNode.builder()
        .setItem((user, context) -> {
          var builder = ItemStacks.builder(Material.CLOCK)
              .setName("&bDaily Item Challenges")
              .setFlags(ItemFlag.HIDE_ATTRIBUTES);

          int streak = getManager()
              .getEntry(user.getUniqueId())
              .getStreak(StreakCategory.ITEMS)
              .get();

          builder.addLore(
              Text.format(
                  "Current streak: {0, number}",
                  NamedTextColor.GRAY,
                  streak
              )
          );

          return builder.build();
        })
        .build();
  }
}