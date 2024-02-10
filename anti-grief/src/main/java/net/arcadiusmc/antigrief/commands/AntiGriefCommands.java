package net.arcadiusmc.antigrief.commands;

import net.arcadiusmc.antigrief.JailCell;
import net.arcadiusmc.antigrief.Punishments;
import net.arcadiusmc.command.arguments.RegistryArguments;

public final class AntiGriefCommands {
  private AntiGriefCommands() {}

  public static final RegistryArguments<JailCell> JAIL_CELL_ARG = new RegistryArguments<>(
      Punishments.get().getCells(),
      "Jail Cell"
  );

  public static void createCommands() {
    PunishmentCommand.createCommands();
    new CommandSeparate();
    new CommandNotes();
    new CommandPunish();
    new CommandSmite();
    new CommandStaffChat();
  }
}