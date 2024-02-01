package net.arcadiusmc.core.commands.admin;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import java.util.Collection;
import java.util.List;
import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.command.arguments.ExpandedEntityArgument;
import net.arcadiusmc.command.help.UsageFactory;
import net.arcadiusmc.core.CorePermissions;
import net.arcadiusmc.user.UserTeleport;
import net.arcadiusmc.user.Users;
import net.forthecrown.grenadier.GrenadierCommand;
import net.forthecrown.grenadier.types.ArgumentTypes;
import net.forthecrown.grenadier.types.EntitySelector;
import net.forthecrown.grenadier.types.options.ArgumentOption;
import net.forthecrown.grenadier.types.options.Options;
import net.forthecrown.grenadier.types.options.OptionsArgument;
import net.forthecrown.grenadier.types.options.ParsedOptions;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class CommandTeleportExact extends BaseCommand {

  public static final ArgumentOption<World> WORLD
      = Options.argument(ArgumentTypes.world(), "world");

  public static final ArgumentOption<Double> CORD_X
      = Options.argument(DoubleArgumentType.doubleArg(), "x");

  public static final ArgumentOption<Double> CORD_Y
      = Options.argument(DoubleArgumentType.doubleArg(), "y");

  public static final ArgumentOption<Double> CORD_Z
      = Options.argument(DoubleArgumentType.doubleArg(), "z");

  public static final ArgumentOption<EntitySelector> TARGETS
      = Options.argument(new ExpandedEntityArgument(true, false), "targets");

  public static final ArgumentOption<Float> YAW
      = Options.argument(FloatArgumentType.floatArg(-180, 180))
      .setDefaultValue(0F)
      .setLabel("yaw")
      .build();


  public static final ArgumentOption<Float> PITCH
      = Options.argument(FloatArgumentType.floatArg(-90, 90))
      .setDefaultValue(0F)
      .setLabel("pitch")
      .build();

  public static final OptionsArgument ARGS = OptionsArgument.builder()
      .addRequired(WORLD)

      .requireAllOf(CORD_X, CORD_Y, CORD_Z)

      .addOptional(YAW)
      .addOptional(PITCH)

      .addOptional(TARGETS)

      .build();

  public CommandTeleportExact() {
    super("tp_exact");

    setPermission(CorePermissions.CMD_TELEPORT);
    setDescription("Command for more precise teleportation");

    register();
  }

  @Override
  public void populateUsages(UsageFactory factory) {
    factory.usage(
        "world=<world> x=<cord> y=<cord> z=<cord> "
            + "[yaw=<value>] [pitch=<value>] [targets=<target selector>]"
        )
        .addInfo("Teleports you to the location specified in the parameters");
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command
        .then(argument("args", ARGS)
            .executes(c -> {
              var source = c.getSource();
              var args = c.getArgument("args", ParsedOptions.class);

              Location loc = new Location(
                  args.getValue(WORLD),
                  args.getValue(CORD_X),
                  args.getValue(CORD_Y),
                  args.getValue(CORD_Z),
                  args.getValue(YAW),
                  args.getValue(PITCH)
              );

              Collection<Entity> targets;

              if (args.has(TARGETS)) {
                targets = args.getValue(TARGETS).findEntities(source);
              } else {
                targets = List.of(source.asEntity());
              }

              for (Entity target : targets) {
                if (target instanceof Player player) {
                  var user = Users.get(player);

                  if (!user.checkTeleporting()) {
                    return 0;
                  }

                  user.createTeleport(() -> loc, UserTeleport.Type.TELEPORT)
                      .setDelay(null)
                      .setSilent(true)
                      .start();

                } else {
                  target.teleport(loc);
                }
              }

              return 0;
            })
        );
  }
}