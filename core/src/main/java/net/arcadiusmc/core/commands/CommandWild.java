package net.arcadiusmc.core.commands;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.List;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.core.Wild;
import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.user.User;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.GrenadierCommand;
import net.forthecrown.grenadier.types.ArgumentTypes;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class CommandWild extends BaseCommand {

  static final Sound SOUND = Sound.sound()
      .type(org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT)
      .volume(1)
      .pitch(1)
      .build();

  private final Wild wild;

  public CommandWild(Wild wild) {
    super("wild");

    this.wild = wild;

    setDescription("Allows you to teleport to random locations");
    setAliases("rtp", "randomtp", "randomteleport");
    simpleUsages();

    register();
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command
        .executes(c -> {
          Player player = c.getSource().asPlayer();
          Location location = wild.getWildLocation(player);
          teleportToWild(player, location);
          return 0;
        })

        .then(argument("players", Arguments.ONLINE_USERS)
            .executes(c -> {
              teleportOther(c.getSource(), Arguments.getUsers(c, "players"), null);
              return SINGLE_SUCCESS;
            })

            .then(argument("world", ArgumentTypes.world())
                .executes(c -> {
                  teleportOther(
                      c.getSource(),
                      Arguments.getUsers(c, "players"),
                      c.getArgument("world", World.class)
                  );

                  return SINGLE_SUCCESS;
                })
            )
        );
  }

  private void teleportOther(CommandSource source, List<User> users, World world)
      throws CommandSyntaxException
  {
    int teleported = 0;

    for (User user : users) {
      World wildWorld;

      if (world != null) {
        wildWorld = world;
      } else {
        wildWorld = wild.getFlagWorld(user.getLocation());
      }

      Location wildLocation = wild.findWild(wildWorld);

      if (wildLocation == null) {
        continue;
      }

      teleportToWild(user.getPlayer(), wildLocation);
      teleported++;
    }

    if (teleported < 1) {
      throw Wild.FAILED.exception();
    }

    source.sendSuccess(
        Messages.render("cmd.wild.many")
            .addValue("count", teleported)
            .create(source)
    );
  }

  private void teleportToWild(Player player, Location location) {
    player.teleport(location);

    if (wild.fallingWild()) {
      player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 600, 0));
    }

    player.playSound(SOUND);
  }
}
