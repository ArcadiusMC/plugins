package net.arcadiusmc.core.commands;

import net.arcadiusmc.ArcadiusServer;
import net.arcadiusmc.command.BaseCommand;
import net.forthecrown.grenadier.GrenadierCommand;
import net.arcadiusmc.user.User;

public class CommandSettings extends BaseCommand {

  public CommandSettings() {
    super("settings");

    setAliases("options", "preferences");
    setDescription("Opens the settings book");
    simpleUsages();

    register();
  }

  /*
   * ----------------------------------------
   * 			Command description:
   * ----------------------------------------
   * Shows some basic info about a user.
   *
   * Valid usages of command:
   * - /settings
   * - /options
   *
   * Author: Wout
   */

  @Override
  public void createCommand(GrenadierCommand command) {
    command.executes(c -> {
      ArcadiusServer server = ArcadiusServer.server();
      User user = getUserSender(c);
      server.getGlobalSettingsBook().open(user, user);
      return 0;
    });
  }
}