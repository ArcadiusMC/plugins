package net.arcadiusmc.markets.gui;

import static net.kyori.adventure.text.Component.text;

import com.google.common.base.Strings;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import net.arcadiusmc.markets.Market;
import net.arcadiusmc.markets.MarketsConfig;
import net.arcadiusmc.markets.MarketsConfig.TaxBracket;
import net.arcadiusmc.markets.ValueModifierList;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.text.TextJoiner;
import net.arcadiusmc.text.loader.MessageRender;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.Users;
import net.arcadiusmc.utils.BookBuilder;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;

public final class ShopEditBook {
  private ShopEditBook() {}

  static final String INFO = "info";
  static final String MEMBERS = "members";
  static final String BANNED = "bannedCustomers";
  static final String TAXES = "taxes";
  static final String RENT = "rent";

  static final String[] PAGES = {INFO, MEMBERS, BANNED, TAXES, RENT};

  static Component showClaimSize(Market market, User user) {
    return Messages.render("markets.gui.buttons.showBounds")
        .create(user)
        .clickEvent(ClickEvent.runCommand("/market-bounds " + market.getRegionName().hashCode()));
  }

  public static Book createBook(Market market, User user) {
    if (market.getOwnerId() == null) {
      throw new IllegalArgumentException("Unowned market");
    }

    BookBuilder builder = new BookBuilder()
        .setTitle("ShopEditBook")
        .author(text(Bukkit.getName()));

    for (String page : PAGES) {
      String titleKey = "markets.gui.title." + page;
      String contentKey = "markets.gui.content." + page;
      String footerKey = "markets.gui.footer." + page;

      if (Messages.MESSAGE_LIST.hasMessage(titleKey)) {
        builder.addCentered(Messages.renderText(titleKey, user));
      }

      MessageRender render = Messages.render(contentKey);
      configureMessage(render, market, user, page);

      builder.addText(render.create(user));

      if (Messages.MESSAGE_LIST.hasMessage(footerKey)) {
        builder.justifyRight(Messages.renderText(footerKey, user));
      }

      builder.newPage();
    }

    return builder.build();
  }

  static void configureMessage(MessageRender render, Market market, User user, String pageName) {
    Market merged = market.getMerged();
    Set<UUID> mergedMembers;
    Set<UUID> mergedBanned;

    if (merged != null && merged.getOwnerId() != null) {
      User mergedOwner = Users.get(merged.getOwnerId());
      render.addValue("merged", mergedOwner.displayName(user));

      mergedMembers = merged.getMembers();
      mergedBanned = merged.getBannedCustomers();
    } else {
      render.addValue("merged", Messages.renderText("markets.gui.merged.none", user));

      mergedMembers = null;
      mergedBanned = null;
    }

    render.addValue("showClaimSize", showClaimSize(market, user));

    Set<UUID> members = market.getMembers();

    int mergedMemberSize = mergedMembers == null
        ? 0
        : mergedMembers.size();

    Component membersList = playerList(members, "/marketuntrust %s", user);
    Component mergedList = playerList(mergedMembers, null, user);

    render.addValue("members.count", members.size());
    render.addValue("members.mergedCount", mergedMemberSize);
    render.addValue("members.totalCount", members.size() + mergedMemberSize);
    render.addValue("members.list", membersList);
    render.addValue("members.mergedList", mergedList);
    render.addValue("members.add",
        Messages.renderText("markets.gui.add", user)
            .hoverEvent(Messages.renderText("markets.gui.add.hover.member", user))
            .clickEvent(ClickEvent.suggestCommand("/shop-trust "))
    );

    switch (pageName) {
      default -> {
        // no-op
      }

      case MEMBERS -> {
        boolean editState = market.memberEditingAllowed();

        Component editYes = Messages.BUTTON_ACCEPT_TICK.renderText(user)
            .clickEvent(ClickEvent.runCommand("/market-editing true"))
            .color(editState ? NamedTextColor.GREEN : NamedTextColor.BLACK);

        Component editNo = Messages.BUTTON_DENY_CROSS.renderText(user)
            .clickEvent(ClickEvent.runCommand("/market-editing false"))
            .color(editState ? NamedTextColor.BLACK : NamedTextColor.RED);

        render.addValue("edit.yes", editYes);
        render.addValue("edit.no", editNo);
      }

      case BANNED -> {
        Set<UUID> banned = market.getBannedCustomers();

        Component bannedList = playerList(banned, "/unbancustomer %s", user);
        Component mergedBannedList = playerList(mergedBanned, null, user);

        int mergedBannedCount = mergedBanned == null
            ? 0
            : mergedBanned.size();

        render.addValue("banned.count", banned.size());
        render.addValue("banned.mergedCount", mergedBannedCount);
        render.addValue("banned.totalCount", mergedBannedCount + banned.size());
        render.addValue("banned.list", bannedList);
        render.addValue("banned.mergedList", mergedBannedList);
        render.addValue("banned.add",
            Messages.renderText("markets.gui.add", user)
                .hoverEvent(Messages.renderText("markets.gui.add.hover.banned", user))
                .clickEvent(ClickEvent.suggestCommand("/shop-ban "))
        );
      }

      case RENT -> {
        int baseRent = market.getBaseRent();
        int rent = market.getRent();

        ValueModifierList modifierList = market.getRentModifiers();

        Instant nextPayment;
        Instant lastPayment = market.getLastRentTime();
        if (lastPayment == null) {
          lastPayment = Instant.now();
        }

        MarketsConfig config = market.getManager().getPlugin().getPluginConfig();
        nextPayment = lastPayment.plus(config.rentInterval());

        render.addValue("rent", rent);
        render.addValue("rent.base", baseRent);
        render.addValue("rent.modifierCount", modifierList.getModifiers().size());
        render.addValue("rent.modifierList", modifierList(modifierList, user));
        render.addValue("rent.nextPaymentDate", Text.formatDate(nextPayment));
      }

      case TAXES -> {
        TaxBracket bracket = market.getTaxBracket();

        Instant lastReset = market.getLastTaxReset();
        Instant nextReset;

        if (lastReset == null) {
          lastReset = Instant.now();
        }

        MarketsConfig config = market.getManager().getPlugin().getPluginConfig();
        nextReset = lastReset.plus(config.taxResetInterval());

        render.addValue("taxes.nextReset", Text.formatDate(nextReset));

        if (bracket == null) {
          render.addValue("taxes.rate", 0);
          render.addValue("taxes.baseRate", 0);
          render.addValue("taxes.bracket", Messages.renderText("markets.gui.noTaxBracket", user));
          render.addValue("taxes.modifierCount", 0);
          render.addValue("taxes.modifierList", Component.empty());
        } else {
          ValueModifierList list = market.getTaxModifiers();

          float baseRate = bracket.rate();
          float rate = list.apply(baseRate);

          render.addValue("taxes.rate", rate);
          render.addValue("taxes.baseRate", baseRate);
          render.addValue("taxes.bracket", bracket.earningsRange().toString());
          render.addValue("taxes.modifierCount", list.getModifiers().size());
          render.addValue("taxes.modifierList", modifierList(list, user));
        }

      }
    }
  }

  static Component modifierList(ValueModifierList list, User viewer) {
    return TextJoiner.onNewLine()
        .add(
            list.getModifiers()
                .stream()
                .map(modifier -> modifier.displayText(viewer))
        )
        .asComponent();
  }

  static Component playerList(Set<UUID> uuids, String cmdFormat, User viewer) {
    if (uuids == null) {
      return Messages.renderText("markets.gui.merged.not", viewer);
    }

    boolean removable = !Strings.isNullOrEmpty(cmdFormat);

    String messageKey = removable
        ? "markets.gui.listFormat.removable"
        : "markets.gui.listFormat.unRemovable";

    return TextJoiner.onNewLine()
        .add(
            uuids.stream()
                .map(Users::get)
                .map(user1 -> {
                  MessageRender render = Messages.render(messageKey)
                      .addValue("player", user1);

                  if (removable) {
                    Component cross = Messages.BUTTON_DENY_CROSS.renderText(null)
                        .clickEvent(ClickEvent.runCommand(cmdFormat.formatted(user1.getName())));

                    render.addValue("button", cross);
                  }

                  return render.create(viewer);
                })
        )
        .asComponent();
  }
}
