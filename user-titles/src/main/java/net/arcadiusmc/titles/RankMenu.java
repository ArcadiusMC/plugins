package net.arcadiusmc.titles;

import static net.arcadiusmc.menu.Menus.MAX_INV_SIZE;

import com.google.common.base.Strings;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import net.arcadiusmc.menu.ClickContext;
import net.arcadiusmc.menu.CommonItems;
import net.arcadiusmc.menu.MenuBuilder;
import net.arcadiusmc.menu.MenuNode;
import net.arcadiusmc.menu.Menus;
import net.arcadiusmc.menu.page.ListPage;
import net.arcadiusmc.menu.page.MenuPage;
import net.arcadiusmc.registry.Holder;
import net.arcadiusmc.registry.Registry;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.titles.Tier.MenuDecoration;
import net.arcadiusmc.user.User;
import net.arcadiusmc.utils.context.Context;
import net.arcadiusmc.utils.context.ContextOption;
import net.arcadiusmc.utils.context.ContextSet;
import net.arcadiusmc.utils.inventory.ItemStacks;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class RankMenu {

  // Context required to represent the 'extra ranks' menu's page
  private static final ContextSet SET = ContextSet.create();
  private static final ContextOption<Integer> PAGE = SET.newOption(0);

  @Getter
  private static final RankMenu instance = new RankMenu();

  private RankMenu() {

  }

  public void open(User user) {
    var titles = UserTitles.load(user);
    var tier = titles.getTier();
    open(user, tier);
  }

  private Holder<Tier> getNext(Holder<Tier> tier, User user, int dir) {
    Registry<Tier> reg = Tiers.REGISTRY;
    List<Holder<Tier>> arr = new ArrayList<>(reg.entries());

    arr.removeIf(holder -> {
      Tier t = holder.getValue();
      if (!t.isHidden()) {
        return false;
      }
      String permission = UserTitles.getTierPermission(holder);

      if (Strings.isNullOrEmpty(permission)) {
        return false;
      }

      return !user.hasPermission(permission);
    });

    arr.sort(Tiers.BY_PRIORITY);

    int index = arr.indexOf(tier);

    if (index == -1 || arr.size() == 1) {
      return tier;
    }

    int nextIndex;

    if (dir < 0) {
      int idx = index + dir;

      if (idx < 0) {
        nextIndex = arr.size() - idx - 1;
      } else {
        nextIndex = idx;
      }
    } else {
      nextIndex = (index + dir) % arr.size();
    }

    return arr.get(nextIndex);
  }

  private void openNext(User user, Holder<Tier> tier, int dir) {
    open(user, getNext(tier, user, dir));
  }

  private void open(User user, Holder<Tier> holder) {
    Tier tier = holder.getValue();

    if (tier.getPage() == null) {
      tier.setPage(new RankPage(holder));
    }

    tier.getPage().getMenu().open(user, SET.createContext());
  }

  public static List<Holder<Title>> getExtraRanks(User user, Tier tier) {
    UserTitles titles = UserTitles.load(user);

    return tier.getRanks()
        .stream()
        .filter(holder -> {
          Title title = holder.getValue();

          if (title.getMenuSlot() != null) {
            return false;
          }

          boolean has = titles.hasTitle(holder);
          return has || !title.isHidden();
        })
        .collect(Collectors.toList());
  }

  private class RankPage extends MenuPage {
    private final Holder<Tier> tier;
    private final ExtraRankListPage listPage;

    public RankPage(Holder<Tier> tier) {
      super(null);

      this.tier = tier;
      this.listPage = new ExtraRankListPage(this, tier);

      initMenu(
          Menus.builder(MAX_INV_SIZE).setTitle(tier.getValue().displayName()),
          true
      );
    }

    @Override
    protected void createMenu(MenuBuilder builder) {
      decorateMenu(builder);
      fillMenu(builder, tier.getValue());

      builder.add(8,
          MenuNode.builder()
              .setItem(CommonItems.nextPage())

              .setRunnable((user, context, click) -> {
                click.shouldClose(false);
                click.shouldReloadMenu(false);

                openNext(user, tier, 1);
              })

              .build()
      );

      builder.add(0,
          MenuNode.builder()
              .setItem(CommonItems.previousPage())

              .setRunnable((user, context, click) -> {
                click.shouldClose(false);
                click.shouldReloadMenu(false);

                openNext(user, tier, -1);
              })

              .build()
      );

      builder.add(4, 5, new TitleMenuNode(Titles.DEFAULT_HOLDER));
    }

    private void fillMenu(MenuBuilder builder, Tier tier) {
      tier.getRanks()
          .stream()
          .filter(rank -> rank.getValue().getMenuSlot() != null)
          .filter(rank -> rank != Titles.DEFAULT_HOLDER)
          .forEach(rank -> {
            builder.add(rank.getValue().getMenuSlot(), new TitleMenuNode(rank));
          });

      for (int i = 1; i < 8; i++) {
        int finalI = i;
        final int index = finalI - 1;

        builder.add(i, 4,
            MenuNode.builder()
                .setItem((user, context) -> {
                  List<Holder<Title>> extra = getExtraRanks(user, tier);

                  if (index >= extra.size()) {
                    return null;
                  }

                  Holder<Title> holder = extra.get(index);
                  return TitleMenuNode.createItem(holder, user, context);
                })

                .setRunnable((user, context, click) -> {
                  List<Holder<Title>> extra = getExtraRanks(user, tier);

                  if (index >= extra.size()) {
                    return;
                  }

                  Holder<Title> holder = extra.get(index);
                  TitleMenuNode.onClick(holder, user, context, click);
                })

                .build()
        );
      }

      builder.add(8, 3, listPage);
    }

    private void decorateMenu(MenuBuilder builder) {
      builder.addBorder();

      for (int i = 1; i < 8; i++) {
        builder.add(i, 3, Menus.defaultBorderItem());
      }

      for (MenuDecoration decoration : tier.getValue().getDecorations()) {
        builder.add(decoration.slot(), Menus.createBorderItem(decoration.material()));
      }
    }

    @Override
    public @Nullable ItemStack createItem(@NotNull User user, @NotNull Context context) {
      return tier.getValue().getNode().createItem(user, context);
    }
  }

  private static class ExtraRankListPage extends ListPage<Holder<Title>> {
    private final Holder<Tier> tier;

    public static final int MAX_DISPLAY_RANKS = 7;

    public ExtraRankListPage(MenuPage parent, Holder<Tier> tier) {
      super(parent, PAGE);
      this.tier = tier;

      initMenu(
          Menus.builder(
              Menus.sizeFromRows(5),

              Messages.render("ranksmenu.extras.title")
                  .addValue("tier", tier.getValue().getDisplayItem())
                  .asComponent()
          ),
          true
      );
    }

    @Override
    protected List<Holder<Title>> getList(User user, Context context) {
      return getExtraRanks(user, tier.getValue());
    }

    @Override
    protected ItemStack getItem(User user, Holder<Title> entry, Context context) {
      return TitleMenuNode.createItem(entry, user, context);
    }

    @Override
    protected void onClick(User user, Holder<Title> entry, Context context, ClickContext click)
        throws CommandSyntaxException
    {
      TitleMenuNode.onClick(entry, user, context, click);
    }

    @Override
    public @Nullable ItemStack createItem(@NotNull User user, @NotNull Context context) {
      List<Holder<Title>> extra = getExtraRanks(user, tier.getValue());

      if (extra.size() <= MAX_DISPLAY_RANKS) {
        return null;
      }

      return ItemStacks.builder(Material.PAPER)
          .setName(Messages.renderText("ranksmenu.extras.button.name", user))
          .addLore(
              Messages.render("ranksmenu.extras.button.lore")
                  .addValue("extras", extra.size())
                  .create(user)
          )
          .build();
    }

    @Override
    public void onClick(User user, Context context, ClickContext click)
        throws CommandSyntaxException
    {
      var extra = getExtraRanks(user, tier.getValue());

      if (extra.size() <= MAX_DISPLAY_RANKS) {
        return;
      }

      super.onClick(user, context, click);
    }
  }
}