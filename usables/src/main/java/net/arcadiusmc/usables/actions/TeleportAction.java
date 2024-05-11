package net.arcadiusmc.usables.actions;

import com.google.common.base.Strings;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.floats.FloatList;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import lombok.Getter;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.Worlds;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.usables.Action;
import net.arcadiusmc.usables.Interaction;
import net.arcadiusmc.usables.ObjectType;
import net.arcadiusmc.usables.UsableComponent;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.UserTeleport.Type;
import net.arcadiusmc.user.Users;
import net.arcadiusmc.utils.io.ExtraCodecs;
import net.arcadiusmc.utils.io.Results;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.types.ArgumentTypes;
import net.forthecrown.grenadier.types.options.ArgumentOption;
import net.forthecrown.grenadier.types.options.Options;
import net.forthecrown.grenadier.types.options.OptionsArgument;
import net.forthecrown.grenadier.types.options.ParsedOptions;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

@Getter
public class TeleportAction implements Action {

  private static final Logger LOGGER = Loggers.getLogger();

  static final TeleportActionType TYPE = new TeleportActionType();

  private final String worldName;
  private final double x;
  private final double y;
  private final double z;
  private final Float yaw;
  private final Float pitch;

  public TeleportAction(
      String worldName,
      double x,
      double y,
      double z,
      Float yaw,
      Float pitch
  ) {
    this.worldName = worldName;
    this.x = x;
    this.y = y;
    this.z = z;
    this.yaw = yaw;
    this.pitch = pitch;
  }

  DataResult<Location> getLocation(Player player) {
    World world;

    if (Strings.isNullOrEmpty(worldName)) {
      world = player.getWorld();
    } else {
      world = Bukkit.getWorld(worldName);

      if (world == null) {
        return Results.error("Unknown world '%s'", worldName);
      }
    }

    Location location = new Location(world, x, y, z);

    if (yaw == null) {
      location.setYaw(player.getYaw());
    } else {
      location.setYaw(yaw);
    }

    if (pitch == null) {
      location.setPitch(player.getPitch());
    } else {
      location.setPitch(pitch);
    }

    return DataResult.success(location);
  }

  @Override
  public void onUse(Interaction interaction) {
    Optional<Player> playerOpt = interaction.getPlayer();

    if (playerOpt.isEmpty()) {
      return;
    }

    Player player = playerOpt.get();
    Optional<Location> result = getLocation(player)
        .mapError(s -> "Failed to get location: " + s)
        .resultOrPartial(Loggers.getLogger()::error);

    if (result.isEmpty()) {
      return;
    }

    Location destination = result.get();

    if (!destination.isWorldLoaded()) {
      destination.setWorld(player.getWorld());
    }

    User user = Users.get(player);
    user.createTeleport(() -> destination, Type.TELEPORT)
        .setSilent(interaction.getBoolean("silent").orElse(false))
        .setDelay(null)
        .setSetReturn(false)
        .start();
  }

  @Override
  public ObjectType<? extends UsableComponent> getType() {
    return TYPE;
  }

  @Override
  public @Nullable Component displayInfo() {
    World world = Optional.ofNullable(worldName).map(Bukkit::getWorld)
        .orElseGet(Worlds::overworld);

    Location location = new Location(world, x, y, z);

    return Text.clickableLocation(location, true);
  }
}

class TeleportActionType implements ObjectType<TeleportAction> {

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

  private static final ArgumentOption<Double> X
      = Options.argument(DoubleArgumentType.doubleArg(), "x");

  private static final ArgumentOption<Double> Y
      = Options.argument(DoubleArgumentType.doubleArg(), "y");

  private static final ArgumentOption<Double> Z
      = Options.argument(DoubleArgumentType.doubleArg(), "z");


  private static final OptionsArgument ARGS = OptionsArgument.builder()
      .addOptional(YAW)
      .addOptional(PITCH)
      .addOptional(WORLD)
      .addRequired(X)
      .addRequired(Y)
      .addRequired(Z)
      .build();

  static final Codec<TeleportAction> CODEC = RecordCodecBuilder.create(instance -> {
    return instance
        .group(
            Codec.STRING.optionalFieldOf("world")
                .forGetter(o -> Optional.ofNullable(o.getWorldName())),

            Codec.DOUBLE.fieldOf("x").forGetter(TeleportAction::getX),
            Codec.DOUBLE.fieldOf("y").forGetter(TeleportAction::getY),
            Codec.DOUBLE.fieldOf("z").forGetter(TeleportAction::getZ),

            Codec.FLOAT.optionalFieldOf("yaw")
                .forGetter(o -> Optional.ofNullable(o.getYaw())),

            Codec.FLOAT.optionalFieldOf("pitch")
                .forGetter(o -> Optional.ofNullable(o.getPitch()))
        )
        .apply(instance, (world, x, y, z, yaw, pitch) -> {
          return new TeleportAction(
              world.orElse(null),
              x,
              y,
              z,
              yaw.orElse(null),
              pitch.orElse(null)
          );
        });
  });

  static final Codec<Location> OLD_CODEC = RecordCodecBuilder.create(instance -> {
    return instance
        .group(
            ExtraCodecs.WORLD_CODEC.optionalFieldOf("world")
                    .forGetter(l -> Optional.ofNullable(l.getWorld())),

            Codec.DOUBLE.listOf().fieldOf("pos")
                .forGetter(l -> DoubleList.of(l.x(), l.y(), l.z())),

            Codec.FLOAT.listOf().optionalFieldOf("rot", FloatList.of(0, 0))
                .forGetter(l -> FloatList.of(l.getYaw(), l.getPitch()))
        )
        .apply(instance, (world, pos, rot) -> {
          return new Location(
              world.orElse(null),
              pos.get(0),
              pos.get(1),
              pos.get(2),
              rot.get(0),
              rot.get(1)
          );
        });
  });

  @Override
  public TeleportAction parse(StringReader reader, CommandSource source)
      throws CommandSyntaxException
  {
    double x;
    double y;
    double z;
    World world;
    Float yaw;
    Float pitch;

    if (reader.canRead()) {
      ParsedOptions options = ARGS.parse(reader);

      x = options.getValue(X);
      y = options.getValue(Y);
      z = options.getValue(Z);

      world = options.getValueOptional(WORLD).orElseGet(source::getWorld);

      yaw = options.getValue(YAW);
      pitch = options.getValue(PITCH);
    } else {
      Location loc = source.getLocation();

      x = loc.x();
      y = loc.y();
      z = loc.z();
      world = loc.getWorld();
      yaw = loc.getYaw();
      pitch = loc.getPitch();
    }

    return new TeleportAction(world.getName(), x, y, z, yaw, pitch);
  }

  @Override
  public CompletableFuture<Suggestions> getSuggestions(
      CommandContext<CommandSource> context,
      SuggestionsBuilder builder
  ) {
    return ARGS.listSuggestions(context, builder);
  }

  private <S> Dynamic<S> fix(Dynamic<S> dynamic) {
    return OLD_CODEC.parse(dynamic)
        .flatMap(location -> ExtraCodecs.LOCATION_CODEC.encodeStart(dynamic.getOps(), location))
        .map(s -> new Dynamic<>(dynamic.getOps(), s))
        .result()
        .orElse(dynamic);
  }

  @Override
  public <S> DataResult<TeleportAction> load(Dynamic<S> dynamic) {
    return CODEC.parse(fix(dynamic));
  }

  @Override
  public <S> DataResult<S> save(@NotNull TeleportAction value, @NotNull DynamicOps<S> ops) {
    return CODEC.encodeStart(ops, value);
  }
}
