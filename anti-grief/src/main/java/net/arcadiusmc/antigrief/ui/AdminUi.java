package net.arcadiusmc.antigrief.ui;

import net.arcadiusmc.antigrief.PunishEntry;
import net.arcadiusmc.antigrief.Punishments;
import net.arcadiusmc.menu.MenuNode;
import net.arcadiusmc.text.TextWriters;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.Users;
import net.arcadiusmc.user.name.UserNameFactory;
import net.arcadiusmc.utils.context.Context;
import net.arcadiusmc.utils.context.ContextOption;
import net.arcadiusmc.utils.context.ContextSet;
import net.arcadiusmc.utils.inventory.ItemStacks;

public final class AdminUi {
  private AdminUi() {}

  static final ContextSet SET = ContextSet.create();
  static final ContextOption<Integer> PAGE = SET.newOption(0);
  static final ContextOption<PunishEntry> ENTRY = SET.newOption();
  static final ContextOption<PunishBuilder> PUNISHMENT = SET.newOption();
  static final ContextOption<Integer> TIME_MULTIPLIER = SET.newOption(1);

  static final MenuNode HEADER = MenuNode.builder()
      .setItem((user, context) -> {
        PunishEntry entry = context.getOrThrow(ENTRY);
        User target = entry.getUser();

        var builder = ItemStacks.headBuilder()
            .setProfile(target.getProfile())
            .setName(target.displayName(user));

        var writer = TextWriters.buffered();

        UserNameFactory factory = Users.getService().getNameFactory();
        factory.applyProfileStyle(writer);
        factory.writeProfileDisplay(writer, target, user);

        builder.addLore(writer.getBuffer());
        return builder.build();
      })

      .build();

  private static final MainPage MAIN_PAGE = new MainPage();

  public static void open(User viewer, User target) {
    Context ctx = SET.createContext();

    PunishEntry entry = Punishments.entry(target);
    assert entry != null;
    ctx.set(ENTRY, entry);

    MAIN_PAGE.getMenu().open(viewer, ctx);
  }
}