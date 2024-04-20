package net.arcadiusmc.command.settings;

import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.command.help.UsageFactory;
import net.arcadiusmc.user.User;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.GrenadierCommand;

public class SettingCommand extends BaseCommand {

  private final Setting setting;

  public SettingCommand(String name, Setting setting) {
    super(name);
    this.setting = setting;
  }

  @Override
  public void populateUsages(UsageFactory factory) {
    factory.usage("").addInfo(getDescription());

    factory.usage("<player>")
        .setPermission(getAdminPermission())
        .addInfo("Toggles this setting for another player");
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command
        .executes(c -> {
          var user = getUserSender(c);
          setting.toggleState(user);
          return 0;
        })

        .then(argument("player", Arguments.USER)
            .requires(source -> source.hasPermission(getAdminPermission()))
            .executes(c -> {
              User target = Arguments.getUser(c, "player");
              CommandSource source = c.getSource();

              setting.toggleOther(source, target);
              return 0;
            })
        );
  }
}
