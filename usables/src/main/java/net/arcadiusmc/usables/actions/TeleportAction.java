package net.arcadiusmc.usables.actions;

import com.mojang.brigadier.arguments.FloatArgumentType;
import java.util.Optional;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.usables.Action;
import net.arcadiusmc.usables.BuiltType;
import net.arcadiusmc.usables.Interaction;
import net.arcadiusmc.usables.ObjectType;
import net.arcadiusmc.usables.UsableComponent;
import net.arcadiusmc.usables.objects.Warp;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.UserTeleport.Type;
import net.arcadiusmc.user.Users;
import net.arcadiusmc.utils.Locations;
import net.arcadiusmc.utils.io.TagUtil;
import net.forthecrown.grenadier.types.ArgumentTypes;
import net.forthecrown.grenadier.types.ParsedPosition;
import net.forthecrown.grenadier.types.options.ArgumentOption;
import net.forthecrown.grenadier.types.options.Options;
import net.forthecrown.grenadier.types.options.OptionsArgument;
import net.forthecrown.grenadier.types.options.ParsedOptions;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.util.TriState;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class TeleportAction implements Action {

  private static final ArgumentOption<Float> YAW
      = Options.argument(FloatArgumentType.floatArg(-180, 180))
      .setLabel("yaw")
      .build();

  private static final ArgumentOption<Float> PITCH
      = Options.argument(FloatArgumentType.floatArg(-90, 90))
      .setLabel("pitch")
      .build();

  private static final ArgumentOption<World> WORLD
      = Options.argument(ArgumentTypes.world())
      .setLabel("world")
      .build();

  private static final ArgumentOption<ParsedPosition> POS_ARG
      = Options.argument(ArgumentTypes.position())
      .setLabel("pos")
      .setDefaultValue(ParsedPosition.IDENTITY)
      .build();

  private static final OptionsArgument ARGS = OptionsArgument.builder()
      .addOptional(YAW)
      .addOptional(PITCH)
      .addOptional(WORLD)
      .addOptional(POS_ARG)
      .build();

  public static final ObjectType<TeleportAction> TYPE = BuiltType.<TeleportAction>builder()
      .requiresInput(TriState.FALSE)

      .tagLoader(binaryTag -> new TeleportAction(TagUtil.readLocation(binaryTag)))
      .tagSaver(action -> TagUtil.writeLocation(action.location))

      .parser((reader, source) -> {
        Location loc;

        if (reader.canRead()) {
          ParsedOptions options = ARGS.parse(reader).checkAccess(source);

          if (options.has(POS_ARG)) {
            ParsedPosition pos = options.getValue(POS_ARG);
            loc = pos.apply(source);
          } else {
            loc = source.getLocation();
          }

          if (options.has(YAW)) {
            loc.setYaw(options.getValue(YAW));
          }

          if (options.has(PITCH)) {
            loc.setPitch(options.getValue(PITCH));
          }

          if (options.has(WORLD)) {
            loc.setWorld(options.getValue(WORLD));
          } else {
            loc.setWorld(null);
          }
        } else {
          loc = source.getLocation();
        }

        return new TeleportAction(loc);
      })
      .suggester(ARGS::listSuggestions)
      .applicableTo(object -> object.as(Warp.class).isEmpty())
      .build();

  private final Location location;

  public TeleportAction(Location location) {
    this.location = location;
  }

  @Override
  public void onUse(Interaction interaction) {
    Optional<Player> playerOpt = interaction.getPlayer();

    if (playerOpt.isEmpty()) {
      return;
    }

    Player player = playerOpt.get();
    Location destination = Locations.clone(location);

    if (!destination.isWorldLoaded()) {
      destination.setWorld(player.getWorld());
    }

    User user = Users.get(player);
    user.createTeleport(() -> destination, Type.TELEPORT)
        .setSilent(interaction.getBoolean("silent").orElse(false))
        .setDelay(null)
        .start();
  }

  @Override
  public ObjectType<? extends UsableComponent> getType() {
    return TYPE;
  }

  @Override
  public @Nullable Component displayInfo() {
    return Text.clickableLocation(location, true);
  }
}
