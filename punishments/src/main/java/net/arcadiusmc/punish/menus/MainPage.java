package net.arcadiusmc.punish.menus;

import static net.arcadiusmc.punish.menus.AdminUi.HEADER;

import net.arcadiusmc.menu.MenuBuilder;
import net.arcadiusmc.menu.MenuNode;
import net.arcadiusmc.menu.Menus;
import net.arcadiusmc.menu.Slot;
import net.arcadiusmc.menu.page.MenuPage;
import net.arcadiusmc.user.User;
import net.arcadiusmc.utils.context.Context;
import net.arcadiusmc.utils.inventory.ItemStacks;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class MainPage extends MenuPage {

  static final Slot PUNISH_USER_SLOT = Slot.of(4, 2);
  static final Slot NOTES_SLOT = Slot.of(4, 1);
  static final Slot PAST_PUNISHMENTS_SLOT = Slot.of(2, 1);
  static final Slot CURRENT_PUNISHMENTS_SLOT = Slot.of(6, 1);

  public MainPage() {
    initMenu(
        Menus.builder().setTitle("Admin GUI").setSize(27),
        false
    );
  }

  @Override
  protected MenuNode createHeader() {
    return HEADER;
  }

  @Override
  protected void createMenu(MenuBuilder builder) {
    var punishPage = new PunishPage(this);
    var noteView = new NotesPage(this);
    var pastView = new PunishmentListPage(this, false);
    var currentView = new PunishmentListPage(this, true);

    builder.add(PUNISH_USER_SLOT, punishPage);
    builder.add(NOTES_SLOT, noteView);

    builder.add(PAST_PUNISHMENTS_SLOT, pastView);
    builder.add(CURRENT_PUNISHMENTS_SLOT, currentView);
  }

  @Override
  public @Nullable ItemStack createItem(@NotNull User user, @NotNull Context context) {
    return ItemStacks.builder(Material.PAPER)
        .setName("&e< Main Page")
        .build();
  }
}