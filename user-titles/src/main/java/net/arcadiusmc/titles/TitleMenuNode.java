package net.arcadiusmc.titles;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import java.util.Map.Entry;
import net.arcadiusmc.command.Exceptions;
import net.arcadiusmc.menu.ClickContext;
import net.arcadiusmc.menu.MenuNode;
import net.arcadiusmc.registry.Holder;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.TextJoiner;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.currency.Currency;
import net.arcadiusmc.utils.context.Context;
import net.arcadiusmc.utils.inventory.ItemStacks;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TitleMenuNode implements MenuNode {

  private final Holder<Title> holder;

  public TitleMenuNode(Holder<Title> holder) {
    this.holder = holder;
  }

  @Override
  public void onClick(User user, Context context, ClickContext click)
      throws CommandSyntaxException
  {
    onClick(holder, user, context, click);
  }

  public static void onClick(Holder<Title> holder, User user, Context context, ClickContext click)
      throws CommandSyntaxException
  {
    UserTitles titles = UserTitles.load(user);
    Title title = holder.getValue();

    boolean has = titles.hasTitle(holder);
    boolean active = titles.getActive().equals(holder);

    if (title.isHidden() && !has) {
      return;
    }

    if (!has) {
      if (attemptPurchase(holder, user, click, titles)) {
        click.shouldReloadMenu(true);
        return;
      }

      throw Messages.render("ranksmenu.error.titleNotOwned")
          .exception(user);
    }

    if (active) {
      throw Messages.render("ranksmenu.error.titleAlreadySet")
          .exception(user);
    }

    titles.setTitle(holder);
    click.shouldReloadMenu(true);

    user.sendMessage(
        Messages.render("ranksmenu.set")
            .addValue("title", title.asComponent())
            .create(user)
    );
  }

  private static boolean attemptPurchase(
      Holder<Title> holder,
      User user,
      ClickContext click,
      UserTitles titles
  ) throws CommandSyntaxException {
    Title title = holder.getValue();
    Tier tier = title.getTier();

    if (title.getPrice() == null || title.getPrice().isEmpty()) {
      return false;
    }

    if (!titles.hasTier(tier)) {
      throw Messages.render("ranksmenu.error.purchaseNoTier")
          .addValue("tier", tier.displayName())
          .exception(user);
    }

    ObjectSet<Entry<Currency, Integer>> entrySet = title.getPrice().entrySet();

    // Validate affordable
    for (Entry<Currency, Integer> entry : entrySet) {
      int existing = entry.getKey().get(user.getUniqueId());

      if (existing < entry.getValue()) {
        throw Exceptions.cannotAfford(user, entry.getValue(), entry.getKey());
      }
    }

    if (!click.getClickType().isShiftClick()) {
      throw Messages.render("ranksmenu.error.confirmPurchase")
          .addValue("title", title.asComponent())
          .exception(user);
    }

    TextJoiner spentJoiner = TextJoiner.onComma();

    for (Entry<Currency, Integer> entry : entrySet) {
      var currency = entry.getKey();
      var amount = entry.getValue();

      currency.remove(user.getUniqueId(), amount);
      spentJoiner.add(currency.format(amount));
    }

    titles.addTitle(holder);
    titles.setTitle(holder);

    user.sendMessage(
        Messages.render("ranksmenu.boughtTitle")
            .addValue("title", title.asComponent())
            .addValue("price", spentJoiner.asComponent())
            .create(user)
    );

    return true;
  }

  @Override
  public @Nullable ItemStack createItem(@NotNull User user, @NotNull Context context) {
    return createItem(holder, user, context);
  }

  public static ItemStack createItem(Holder<Title> holder, User user, Context context) {
    UserTitles titles = UserTitles.load(user);
    Title title = holder.getValue();

    boolean has = titles.hasTitle(holder);
    boolean active = titles.getActive().equals(holder);

    // If hidden, and the user doesn't have it, don't display
    if (title.isHidden() && !has) {
      return null;
    }

    var builder = ItemStacks.builder(
        has ? Material.GLOBE_BANNER_PATTERN : Material.PAPER
    );

    builder.setName(title.getTruncatedPrefix())
        .addFlags(
            ItemFlag.HIDE_ITEM_SPECIFICS,
            ItemFlag.HIDE_ATTRIBUTES,
            ItemFlag.HIDE_DYE
        );

    title.getDescription().forEach(component -> {
      builder.addLore(component.colorIfAbsent(NamedTextColor.GRAY));
    });

    if (!title.getDescription().isEmpty()) {
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

    if (title.getPrice() == null || title.getPrice().isEmpty()) {
      return builder.build();
    }

    builder.addLoreRaw(Component.empty());
    builder.addLore(Messages.renderText("ranksmenu.prices.header", user));

    title.getPrice().forEach((currency, integer) -> {
      builder.addLore(
          Messages.render("ranksmenu.prices.format")
              .addValue("amount", currency.format(integer))
              .create(user)
      );
    });

    if (!titles.hasTier(title.getTier())) {
      builder.addLore(
          Messages.render("ranksmenu.error.purchaseNoTier")
              .addValue("tier", title.getTier().displayName())
              .create(user)
      );
    }

    return builder.build();
  }
}
