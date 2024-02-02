package net.arcadiusmc.titles;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import java.util.Map.Entry;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import net.arcadiusmc.command.Exceptions;
import net.arcadiusmc.menu.ClickContext;
import net.arcadiusmc.menu.MenuNode;
import net.arcadiusmc.menu.Slot;
import net.arcadiusmc.registry.Registries;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.TextJoiner;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.currency.Currency;
import net.arcadiusmc.user.currency.CurrencyMap;
import net.arcadiusmc.user.currency.CurrencyMaps;
import net.arcadiusmc.utils.inventory.ItemStacks;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.intellij.lang.annotations.Pattern;
import org.jetbrains.annotations.NotNull;

@Getter
public class Title implements ComponentLike, ReloadableElement {

  /** The rank's tier */
  private final Tier tier;

  /** The rank's prefix without the trailing space */
  private final Component truncatedPrefix;

  /** The registry key of the opposite gender variant of this rank */
  @Pattern(Registries.VALID_KEY_REGEX)
  private final String genderEquivalentKey;

  /** This rank's menu slot, may be null */
  private final Slot menuSlot;

  /** Description text */
  private final ImmutableList<Component> description;

  /**
   * If true, it means this rank comes free with the tier, otherwise, this
   * rank will have to be earned in some other way
   */
  private final boolean defaultTitle;

  /**
   * If true, means this rank will not be displayed until a user has been given
   * this rank
   */
  private final boolean hidden;

  /**
   * Determines if this rank can be reloaded, aka, if the user ranks are
   * reloaded, then this rank will be unregistered
   */
  private final boolean reloadable;

  @Setter
  private CurrencyMap<Integer> price = CurrencyMaps.emptyMap();

  /** This rank's menu node, lazily initialized */
  private MenuNode menuNode;

  public Title(
      Tier tier,
      Component truncatedPrefix,
      String genderEquivalentKey,
      Slot menuSlot,
      ImmutableList<Component> description,
      boolean defaultTitle,
      boolean hidden,
      boolean reloadable
  ) {
    this.tier = tier;
    this.truncatedPrefix = truncatedPrefix;
    this.genderEquivalentKey = genderEquivalentKey;
    this.menuSlot = menuSlot;
    this.description = description;
    this.defaultTitle = defaultTitle;
    this.hidden = hidden;
    this.reloadable = reloadable;
  }

  public Component getPrefix() {
    return getTruncatedPrefix().append(Component.space());
  }

  @Override
  public @NotNull Component asComponent() {
    return getTruncatedPrefix().hoverEvent(
        TextJoiner.onNewLine()
            .setColor(NamedTextColor.GRAY)
            .add(description)
            .asComponent()
    );
  }

  public Title getGenderEquivalent() {
    return Strings.isNullOrEmpty(genderEquivalentKey)
        ? null
        : Titles.REGISTRY.orNull(getGenderEquivalentKey());
  }

  public MenuNode getMenuNode() {
    if (menuNode != null) {
      return menuNode;
    }

    return menuNode = MenuNode.builder()
        .setItem((user, context) -> {
          UserTitles titles = user.getComponent(UserTitles.class);

          boolean has = titles.hasTitle(this);
          boolean active = titles.getTitle() == this;

          // If hidden, and the user doesn't have it, don't display
          if (hidden && !has) {
            return null;
          }

          var builder = ItemStacks.builder(
              has ? Material.GLOBE_BANNER_PATTERN : Material.PAPER
          );

          builder.setName(getTruncatedPrefix())
              .addFlags(
                  ItemFlag.HIDE_ITEM_SPECIFICS,
                  ItemFlag.HIDE_ATTRIBUTES,
                  ItemFlag.HIDE_DYE
              );

          description.forEach(component -> {
            builder.addLoreRaw(
                Component.text()
                    .decoration(TextDecoration.ITALIC, false)
                    .color(NamedTextColor.GRAY)
                    .append(component)
                    .build()
            );
          });

          if (!description.isEmpty()) {
            builder.addLoreRaw(Component.empty());
          }

          if (active) {
            builder.addEnchant(Enchantment.BINDING_CURSE, 1)
                .addFlags(ItemFlag.HIDE_ENCHANTS)
                .addLore(Messages.renderText("ranksmenu.activeTitle", user));

            return builder.build();
          }

          if (has) {
            builder.addLore("&7Click to set as your rank");
            return builder.build();
          }

          if (price == null || price.isEmpty()) {
            return builder.build();
          }

          builder.addLoreRaw(Component.empty());
          builder.addLore(Messages.renderText("ranksmenu.prices.header", user));

          price.forEach((currency, integer) -> {
            builder.addLore(
                Messages.render("ranksmenu.prices.format")
                    .addValue("amount", currency.format(integer))
                    .create(user)
            );
          });

          if (!titles.hasTier(tier)) {
            builder.addLore(
                Messages.render("ranksmenu.error.purchaseNoTier")
                    .addValue("tier", tier.displayName())
                    .create(user)
            );
          }

          return builder.build();
        })

        .setRunnable((user, context, click) -> {
          UserTitles titles = user.getComponent(UserTitles.class);

          boolean has = titles.hasTitle(this);
          boolean active = titles.getTitle() == this;

          if (hidden && !has) {
            return;
          }

          if (!has) {
            if (attemptPurchase(user, click, titles)) {
              click.shouldReloadMenu(true);
              return;
            }

            throw Messages.render("ranksmenu.titleNotOwned")
                .exception(user);
          }

          if (active) {
            throw Messages.render("ranksmenu.error.titleAlreadySet")
                .exception(user);
          }

          titles.setTitle(this);
          click.shouldReloadMenu(true);

          user.sendMessage(
              Messages.render("ranksmenu.set")
                  .addValue("title", asComponent())
                  .create(user)
          );
        })

        .build();
  }

  private boolean attemptPurchase(User user, ClickContext click, UserTitles titles)
      throws CommandSyntaxException
  {
    if (price == null || price.isEmpty()) {
      return false;
    }

    if (!titles.hasTier(tier)) {
      throw Messages.render("ranksmenu.error.purchaseNoTier")
          .addValue("tier", tier.displayName())
          .exception(user);
    }

    ObjectSet<Entry<Currency, Integer>> entrySet = price.entrySet();

    // Validate affordable
    for (Entry<Currency, Integer> entry : entrySet) {
      int existing = entry.getKey().get(user.getUniqueId());

      if (existing < entry.getValue()) {
        throw Exceptions.cannotAfford(user, entry.getValue(), entry.getKey());
      }
    }

    if (!click.getClickType().isShiftClick()) {
      throw Messages.render("ranksmenu.error.confirmPurchase")
          .addValue("title", asComponent())
          .exception(user);
    }

    TextJoiner spentJoiner = TextJoiner.onComma();

    for (Entry<Currency, Integer> entry : entrySet) {
      var currency = entry.getKey();
      var amount = entry.getValue();

      currency.remove(user.getUniqueId(), amount);
      spentJoiner.add(currency.format(amount));
    }

    titles.addTitle(this);
    titles.setTitle(this);

    user.sendMessage(
        Messages.render("ranksmenu.boughtTitle")
            .addValue("title", asComponent())
            .addValue("price", spentJoiner.asComponent())
            .create(user)
    );

    return true;
  }

  /* -------------------------- OBJECT OVERRIDES -------------------------- */

  @Override
  public int hashCode() {
    return Objects.hash(tier, getTruncatedPrefix());
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Title rank)) {
      return false;
    }

    return Objects.equals(rank.getTier(), getTier())
        && Objects.equals(rank.getPrefix(), getPrefix());
  }
}