package net.arcadiusmc.punish.commands;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import net.arcadiusmc.command.Commands;
import net.arcadiusmc.command.Exceptions;
import net.arcadiusmc.punish.GPermissions;
import net.arcadiusmc.punish.Note;
import net.arcadiusmc.punish.PunishEntry;
import net.arcadiusmc.punish.PunishPlugin;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.TextWriter;
import net.arcadiusmc.text.TextWriters;
import net.arcadiusmc.user.User;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.annotations.Argument;
import net.forthecrown.grenadier.annotations.CommandFile;

@CommandFile("commands/notes.gcn")
class CommandNotes {
  static final String PERMISSION = GPermissions.STAFF_NOTES.getName();

  private final PunishPlugin plugin;

  public CommandNotes(PunishPlugin plugin) {
    this.plugin = plugin;
  }

  Optional<List<Note>> optionalNotes(User target) {
    return plugin.getPunishManager()
        .getOptionalEntry(target.getUniqueId())
        .map(PunishEntry::getNotes)
        .filter(notes -> !notes.isEmpty());
  }

  void listNotes(CommandSource source, @Argument("user") User target)
      throws CommandSyntaxException
  {
    Optional<List<Note>> opt = optionalNotes(target);

    if (opt.isEmpty()) {
      throw Exceptions.NOTHING_TO_LIST.exception(source);
    }

    List<Note> list = opt.get();

    TextWriter writer = TextWriters.newWriter();
    writer.viewer(source);

    Note.writeNotes(list, writer, target);

    source.sendMessage(writer.asComponent());
  }

  void addNote(
      CommandSource source,
      @Argument("user") User target,
      @Argument("text") String text
  ) throws CommandSyntaxException {
    PunishEntry entry = plugin.getPunishManager().getEntry(target.getUniqueId());
    List<Note> notes = entry.getNotes();

    Note note = new Note(source.textName(), text, Instant.now());
    notes.add(0, note);

    source.sendSuccess(
        Messages.render("staffNotes.cmd.added")
            .addValue("player", target)
            .addValue("note", note.display(source))
            .create(source)
    );
  }

  void removeNote(
      CommandSource source,
      @Argument("user") User target,
      @Argument("index") int index
  ) throws CommandSyntaxException {
    List<Note> list = optionalNotes(target).orElse(List.of());
    Commands.ensureIndexValid(index, list.size());

    Note note = list.remove(index);

    source.sendSuccess(
        Messages.render("staffNotes.cmd.removed")
            .addValue("player", target)
            .addValue("note", note.display(source))
            .create(source)
    );
  }
}
