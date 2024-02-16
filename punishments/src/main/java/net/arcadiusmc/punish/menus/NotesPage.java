package net.arcadiusmc.punish.menus;

import static net.arcadiusmc.punish.menus.AdminUi.ENTRY;
import static net.arcadiusmc.punish.menus.AdminUi.HEADER;
import static net.arcadiusmc.punish.menus.AdminUi.PAGE;

import java.util.List;
import net.arcadiusmc.menu.MenuNode;
import net.arcadiusmc.menu.page.ListPage;
import net.arcadiusmc.menu.page.MenuPage;
import net.arcadiusmc.punish.GMessages;
import net.arcadiusmc.punish.Note;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.TextWriters;
import net.arcadiusmc.user.User;
import net.arcadiusmc.utils.context.Context;
import net.arcadiusmc.utils.inventory.ItemStacks;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class NotesPage extends ListPage<Note> {

  public NotesPage(MenuPage parent) {
    super(parent, PAGE);
  }

  @Override
  public @Nullable ItemStack createItem(@NotNull User user, @NotNull Context context) {
    var builder = ItemStacks.builder(Material.BOOK)
        .setName("Staff notes");

    if (getList(user, context).isEmpty()) {
      builder.addLore(Messages.renderText("staffNotes.menu.noNotes", user));
    } else {
      builder.addLore(Messages.renderText("staffNotes.menu.lore", user));
    }

    return builder.build();
  }

  @Override
  protected List<Note> getList(User user, Context context) {
    var entry = context.getOrThrow(ENTRY);
    return entry.getNotes();
  }

  @Override
  protected ItemStack getItem(User user, Note note, Context context) {
    var builder = ItemStacks.builder(Material.MAP)
        .setNameRaw(GMessages.noteMetadata(note, user));

    String[] words = note.message().split(" ");
    var writer = TextWriters.buffered();

    int lineLength = 0;

    for (String s : words) {
      lineLength += s.length();

      writer.write(s + " ");

      // Try to limit the amount of character
      // on one line to just 20
      // if more than 20 characters on current
      // line, move to next line
      if (lineLength >= 20) {
        writer.newLine();
        lineLength = 0;
      }
    }

    writer.newLine();
    builder.setLore(writer.getBuffer());

    return builder.build();
  }

  @Override
  protected MenuNode createHeader() {
    return HEADER;
  }
}