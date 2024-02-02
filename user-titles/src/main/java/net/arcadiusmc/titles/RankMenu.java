package net.arcadiusmc.titles;

import static net.arcadiusmc.menu.Menus.MAX_INV_SIZE;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import net.arcadiusmc.menu.ClickContext;
import net.arcadiusmc.menu.MenuBuilder;
import net.arcadiusmc.menu.MenuNode;
import net.arcadiusmc.menu.Menus;
import net.arcadiusmc.menu.page.ListPage;
import net.arcadiusmc.menu.page.MenuPage;
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
    var titles = user.getComponent(UserTitles.class);
    var tier = titles.getTier();
    open(user, tier);
  }

  private Tier getNext(Tier tier) {
    Registry<Tier> reg = Tiers.REGISTRY;

    List<Tier> arr = new ArrayList<>(reg.values());
    arr.sort(Comparator.naturalOrder());

    int index = arr.indexOf(tier);

    if (index == -1 || arr.size() == 1) {
      return tier;
    }

    int nextIndex = (index + 1) % arr.size();
    return arr.get(nextIndex);
  }

  private void openNext(User user, Tier tier) {
    open(user, getNext(tier));
  }

  private void open(User user, Tier tier) {
    if (tier.getPage() == null) {
      tier.setPage(new RankPage(tier));
    }

    tier.getPage().getMenu().open(user, SET.createContext());
  }

  public static List<Title> getExtraRanks(User user, Tier tier) {
    var titles = user.getComponent(UserTitles.class);

    return tier.getRanks()
        .stream()
        .filter(rank -> {
          if (rank.getMenuSlot() != null) {
            return false;
          }

          boolean has = titles.hasTitle(rank);
          return has || !rank.isHidden();
        })
        .collect(Collectors.toList());
  }

  private class RankPage extends MenuPage {
    private final Tier tier;
    private final ExtraRankListPage listPage;

    public RankPage(Tier tier) {
      super(null);

      this.tier = tier;
      this.listPage = new ExtraRankListPage(this, tier);

      initMenu(
          Menus.builder(MAX_INV_SIZE).setTitle(tier.displayName()),
          true
      );
    }

    @Override
    protected void createMenu(MenuBuilder builder) {
      decorateMenu(builder);
      fillMenu(builder, tier);

      builder.add(8,
          MenuNode.builder()
              .setItem(
                  ItemStacks.builder(Material.PAPER)
                      .setName("&eNext page >")
                      .build()
              )

              .setRunnable((user, context, click) -> {
                click.shouldClose(false);
                click.shouldReloadMenu(false);

                openNext(user, tier);
              })

              .build()
      );

      builder.add(4, 5, Titles.DEFAULT.getMenuNode());
    }

    private void fillMenu(MenuBuilder builder, Tier tier) {
      tier.getRanks()
          .stream()
          .filter(rank -> rank.getMenuSlot() != null)
          .forEach(rank -> {
            builder.add(rank.getMenuSlot(), rank.getMenuNode());
          });

      for (int i = 1; i < 8; i++) {
        int finalI = i;

        builder.add(i, 4,
            MenuNode.builder()
                .setItem((user, context) -> {
                  var extra = getExtraRanks(user, tier);
                  int index = finalI - 1;

                  if (index >= extra.size()) {
                    return null;
                  }

                  return extra.get(index)
                      .getMenuNode()
                      .createItem(user, context);
                })

                .setRunnable((user, context, click) -> {
                  var extra = getExtraRanks(user, tier);
                  int index = finalI - 1;

                  if (index >= extra.size()) {
                    return;
                  }

                  extra.get(index)
                      .getMenuNode()
                      .onClick(user, context, click);
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

      for (MenuDecoration decoration : tier.getDecorations()) {
        builder.add(decoration.slot(), Menus.createBorderItem(decoration.material()));
      }
    }

    @Override
    public @Nullable ItemStack createItem(@NotNull User user, @NotNull Context context) {
      return tier.getNode().createItem(user, context);
    }
  }

  private static class ExtraRankListPage extends ListPage<Title> {
    private final Tier tier;

    public static final int MAX_DISPLAY_RANKS = 7;

    public ExtraRankListPage(MenuPage parent, Tier tier) {
      super(parent, PAGE);
      this.tier = tier;

      initMenu(
          Menus.builder(
              Menus.sizeFromRows(5),

              Messages.render("ranksmenu.extras.title")
                  .addValue("tier", tier.getDisplayItem())
                  .asComponent()
          ),
          true
      );
    }

    @Override
    protected List<Title> getList(User user, Context context) {
      return getExtraRanks(user, tier);
    }

    @Override
    protected ItemStack getItem(User user, Title entry, Context context) {
      return entry.getMenuNode().createItem(user, context);
    }

    @Override
    protected void onClick(User user, Title entry, Context context, ClickContext click)
        throws CommandSyntaxException
    {
      entry.getMenuNode().onClick(user, context, click);
    }

    @Override
    public @Nullable ItemStack createItem(@NotNull User user, @NotNull Context context) {
      List<Title> extra = getExtraRanks(user, tier);

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
      var extra = getExtraRanks(user, tier);

      if (extra.size() <= MAX_DISPLAY_RANKS) {
        return;
      }

      super.onClick(user, context, click);
    }
  }
}