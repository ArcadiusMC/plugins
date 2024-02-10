package net.arcadiusmc.antigrief.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.List;
import net.arcadiusmc.antigrief.GExceptions;
import net.arcadiusmc.antigrief.GriefPermissions;
import net.arcadiusmc.antigrief.PunishEntry;
import net.arcadiusmc.antigrief.Punishments;
import net.arcadiusmc.antigrief.StaffNote;
import net.arcadiusmc.command.Commands;
import net.arcadiusmc.command.FtcCommand;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.command.help.UsageFactory;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.GrenadierCommand;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.text.TextWriters;
import net.kyori.adventure.text.Component;

public class CommandNotes extends FtcCommand {

  public CommandNotes() {
    super("Notes");

    setPermission(GriefPermissions.PUNISH_NOTES);
    setDescription("Shows all admin notes of a player");

    register();
  }

  /*
   * ----------------------------------------
   * 			Command description:
   * ----------------------------------------
   *
   * Valid usages of command:
   * /Notes
   *
   * Permissions used:
   *
   * Main Author:
   */

  @Override
  public void populateUsages(UsageFactory factory) {
    factory = factory.withPrefix("<user>");

    factory.usage("")
        .addInfo("Views a <user>'s staff notes");

    factory.usage("add <text>")
        .addInfo("Adds a staff note to a <user>");

    factory.usage("remove <index>")
        .addInfo("Removes a staff note from a <user>");
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command
        .then(argument("user", Arguments.USER)
            .executes(c -> {
              PunishEntry entry = entry(c);

              if (entry.getNotes().isEmpty()) {
                throw GExceptions.noNotes(entry);
              }

              List<StaffNote> notes = entry.getNotes();
              var writer = TextWriters.newWriter();

              StaffNote.writeNotes(notes, writer, entry.getUser());

              c.getSource().sendMessage(writer);
              return 0;
            })

            .then(literal("add")
                .then(argument("str", StringArgumentType.greedyString())
                    .executes(c -> {
                      PunishEntry entry = entry(c);

                      String msg = c.getArgument("str", String.class);
                      StaffNote note = StaffNote.of(msg, c.getSource());

                      entry.getNotes().add(0, note);

                      c.getSource().sendMessage(
                          Text.format("Added note '{0}' to {1, user}",
                              msg, entry.getUser()
                          )
                      );
                      return 0;
                    })
                )
            )

            .then(literal("remove")
                .then(argument("index", IntegerArgumentType.integer(1))
                    .executes(c -> {
                      PunishEntry entry = entry(c);
                      var notes = entry.getNotes();

                      if (notes.isEmpty()) {
                        throw GExceptions.noNotes(entry);
                      }

                      int index = c.getArgument("index", Integer.class);
                      Commands.ensureIndexValid(index, notes.size());

                      notes.remove(index - 1);

                      c.getSource().sendSuccess(
                          Component.text("Removed note from " + entry.getUser().getName())
                      );
                      return 0;
                    })
                )
            )
        );
  }

  private PunishEntry entry(CommandContext<CommandSource> c) throws CommandSyntaxException {
    return Punishments.get().getEntry(Arguments.getUser(c, "user").getUniqueId());
  }
}