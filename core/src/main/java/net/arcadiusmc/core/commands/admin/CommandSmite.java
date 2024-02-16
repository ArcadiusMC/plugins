package net.arcadiusmc.core.commands.admin;


import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.command.help.UsageFactory;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.user.User;
import net.forthecrown.grenadier.GrenadierCommand;

public class CommandSmite extends BaseCommand {

  public CommandSmite() {
    super("smite");
    setDescription("Smites a user lol. This command will deal damage");
    register();
  }

  /*
   * ----------------------------------------
   * 			Command description:
   * ----------------------------------------
   * Smites a player with lightning
   *
   * Valid usages of command:
   * /smite <player>
   *
   * Permissions used:
   * ftc.admin
   *
   * Main Author: Julie
   */

  @Override
  public void populateUsages(UsageFactory factory) {
    factory.usage("<user>", "Smites a <user>");
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command
        .then(argument("user", Arguments.ONLINE_USER)
            .executes(c -> {
              User user = Arguments.getUser(c, "user");

              user.getWorld().strikeLightning(user.getLocation());

              c.getSource().sendMessage(
                  Text.format("Smiting {0, user}.", user)
              );
              return 0;
            })
        );
  }
}