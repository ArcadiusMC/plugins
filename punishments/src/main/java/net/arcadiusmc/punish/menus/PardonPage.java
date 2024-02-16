package net.arcadiusmc.punish.menus;

import static net.arcadiusmc.menu.Menus.DEFAULT_INV_SIZE;
import static net.arcadiusmc.punish.menus.AdminUi.ENTRY;

import net.arcadiusmc.menu.CommonItems;
import net.arcadiusmc.menu.MenuBuilder;
import net.arcadiusmc.menu.MenuNode;
import net.arcadiusmc.menu.Menus;
import net.arcadiusmc.menu.Slot;
import net.arcadiusmc.menu.page.MenuPage;
import net.arcadiusmc.punish.PunishEntry;
import net.arcadiusmc.punish.Punishment;
import net.arcadiusmc.punish.commands.PunishCommands;
import net.arcadiusmc.user.User;
import net.arcadiusmc.utils.context.Context;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class PardonPage extends MenuPage {

  private final ItemStack CONFIRM_ITEM = CommonItems.greenTick()
      .setName("Confirm pardon")
      .build();

  private final ItemStack DENY_ITEM = CommonItems.redCross()
      .setName("Deny pardon")
      .build();

  private final static Slot CONFIRM_SLOT = Slot.of(3, 1);
  private final static Slot DENY_SLOT = Slot.of(5, 1);

  private final Punishment punishment;

  public PardonPage(MenuPage parent, Punishment punishment) {
    super(parent);
    this.punishment = punishment;

    initMenu(
        Menus.builder(DEFAULT_INV_SIZE, Component.text("Pardon punishment?")),
        true
    );
  }

  @Override
  protected void createMenu(MenuBuilder builder) {
    builder.add(HEADER_SLOT.add(0, 2),
        MenuNode.builder()
            // This needs to be a function because when this
            // class is created, this createMenu function will
            // be called before the `punishment` field is set
            .setItem(user -> PunishmentListPage.createItem(user, punishment, true))
            .build()
    );

    builder.add(CONFIRM_SLOT, option(true));
    builder.add(DENY_SLOT, option(false));
  }

  private MenuNode option(boolean pardon) {
    return MenuNode.builder()
        .setItem((user, context) -> pardon ? CONFIRM_ITEM : DENY_ITEM)

        .setRunnable((user, context, click) -> {
          if (!pardon) {
            getParent().onClick(user, context, click);
            return;
          }

          PunishEntry entry = context.getOrThrow(ENTRY);

          PunishCommands.ensureCanPardon(
              user.getCommandSource(),
              entry.getUser(),
              punishment.getType()
          );

          entry.pardon(punishment.getType(), user.getCommandSource());

          AdminUi.open(user, entry.getUser());
        })

        .build();
  }

  @Override
  protected MenuNode createHeader() {
    return AdminUi.HEADER;
  }

  @Override
  public @Nullable ItemStack createItem(@NotNull User user, @NotNull Context context) {
    return CommonItems.goBack();
  }
}