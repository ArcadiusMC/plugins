package net.arcadiusmc.command.settings;

import lombok.Getter;
import lombok.Setter;
import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.user.User;
import net.forthecrown.grenadier.GrenadierCommand;
import org.bukkit.permissions.Permission;

@Getter @Setter
public class SettingCommand extends BaseCommand {

  private final Setting setting;
  private final Permission othersPermission;

  public SettingCommand(
      String name,
      Setting setting,
      Permission permission,
      Permission othersPermission,
      String... aliases
  ) {
    super(name);

    setPermission(permission);
    setAliases(aliases);
    setDescription("Toggles your " + setting.getDisplayName());

    this.setting = setting;
    this.othersPermission = othersPermission;
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command
        .executes(context -> {
          var user = getUserSender(context);
          setting.toggleState(user);
          return 0;
        })

        .then(argument("user", Arguments.USER)
            .requires(source -> source.hasPermission(othersPermission))

            .executes(c -> {
              User user = Arguments.getUser(c, "user");
              setting.toggleOther(c.getSource(), user);
              return 0;
            })
        );
  }
}