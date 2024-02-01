package net.arcadiusmc.core.commands.admin;

import com.mojang.brigadier.context.CommandContext;
import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.command.help.UsageFactory;
import net.arcadiusmc.core.CorePermissions;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.loader.MessageRef;
import net.arcadiusmc.text.loader.MessageRender;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.UserTeleport.Type;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.GrenadierCommand;
import net.forthecrown.grenadier.types.ArgumentTypes;
import net.kyori.adventure.text.Component;
import org.bukkit.World;

public class CommandWorld extends BaseCommand {

  static final MessageRef SELF = Messages.MESSAGE_LIST.reference("admincmd.world.self");
  static final MessageRef OTHER = Messages.MESSAGE_LIST.reference("admincmd.world.other");

  public CommandWorld() {
    super("world");

    setPermission(CorePermissions.CMD_TELEPORT);
    setDescription("Teleports you or another player into a world");

    register();
  }

  @Override
  public void populateUsages(UsageFactory factory) {
    factory.usage("<world>", "Teleports you into a <world>");
    factory.usage("<world> <user>", "Teleports a <user> into a <world>");
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command
        .then(argument("world", ArgumentTypes.world())
            .executes(c -> {
              User user = getUserSender(c);
              return moveToWorld(c, user);
            })

            .then(argument("user", Arguments.ONLINE_USER)
                .executes(c -> {
                  User user = Arguments.getUser(c, "user");
                  return moveToWorld(c, user);
                })
            )
        );
  }

  private int moveToWorld(CommandContext<CommandSource> c, User user) {
    CommandSource source = c.getSource();
    World world = c.getArgument("world", World.class);

    user.createTeleport(() -> world.getSpawnLocation().toCenterLocation(), Type.OTHER)
        .setDelay(null)
        .setSilent(user.hasPermission(CorePermissions.CMD_TELEPORT))
        .start();

    boolean self = source.textName().equals(user.getName());
    MessageRender message = self ? SELF.get() : OTHER.get();
    Component rendered = message
        .addValue("player", user)
        .addValue("world", world)
        .create(source);

    if (self) {
      source.sendMessage(rendered);
    } else {
      source.sendSuccess(rendered);
    }

    return 0;
  }
}