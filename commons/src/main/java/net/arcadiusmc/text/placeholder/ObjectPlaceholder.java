package net.arcadiusmc.text.placeholder;

import static net.kyori.adventure.text.Component.text;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import net.arcadiusmc.Worlds;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.PeriodFormat;
import net.arcadiusmc.text.RomanNumeral;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.text.format.TextFormatTypes;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.Users;
import net.arcadiusmc.utils.Audiences;
import net.arcadiusmc.utils.inventory.ItemStacks;
import net.arcadiusmc.utils.math.Vectors;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.nbt.paper.PaperNbt;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.math.vector.Vector2d;
import org.spongepowered.math.vector.Vector2f;
import org.spongepowered.math.vector.Vector2i;
import org.spongepowered.math.vector.Vectord;
import org.spongepowered.math.vector.Vectorf;
import org.spongepowered.math.vector.Vectori;

public interface ObjectPlaceholder<T> {

  ObjectPlaceholder<Number> NUMBER = (value, fieldName, ctx) -> {
    return switch (fieldName) {
      case "floor" -> value.longValue();
      case "roman" -> text(RomanNumeral.arabicToRoman(value.longValue()));

      case "signed" -> {
        double doubleValue = value.doubleValue();

        if (doubleValue < 0) {
          yield Text.formatNumber(value);
        }

        yield Component.textOfChildren(text("+"), Text.formatNumber(value));
      }

      default -> Text.formatNumber(value);
    };
  };

  ObjectPlaceholder<ItemStack> ITEM = (value, fieldName, ctx) -> {
    return switch (fieldName) {
      case "amount" -> value.getAmount();
      case "nameAndAmount" -> Text.itemAndAmount(value);
      case "nbt" -> PaperNbt.asComponent(ItemStacks.save(value));
      default -> Text.itemDisplayName(value);
    };
  };

  ObjectPlaceholder<World> WORLD = (value, fieldName, ctx) -> {
    return switch (fieldName) {
      case "minY"           -> value.getMinHeight();
      case "maxY"           -> value.getMaxHeight();

      case "raw"            -> value.getName();

      case "directory"      -> value.getWorldFolder().toPath().toString();

      case "gametime"       -> value.getGameTime();
      case "fulltime"       -> value.getFullTime();
      case "time"           -> value.getTime();
      case "day",   "days"  -> value.getFullTime() / 1000 / 24;
      case "year", "years"  -> value.getFullTime() / 1000 / 24 / 365;

      default               -> text(Text.formatWorldName(value));
    };
  };

  ObjectPlaceholder<Location> LOCATION = (value, fieldName, ctx) -> {
    var viewer = ctx.viewer();
    var player = Audiences.getPlayer(viewer);
    boolean clickable = player != null && player.hasPermission("arcadius.commands.tpexact");

    return switch (fieldName) {
      case "x" -> value.x();
      case "y" -> value.y();
      case "z" -> value.z();

      case "bx" -> Text.formatNumber(Math.floor(value.x()));
      case "by" -> Text.formatNumber(Math.floor(value.y()));
      case "bz" -> Text.formatNumber(Math.floor(value.z()));

      case "chunkX" -> Vectors.toChunk(value.getBlockX());
      case "chunkZ" -> Vectors.toChunk(value.getBlockZ());

      case "yaw" -> Text.formatNumber(Math.floor(value.getYaw()));
      case "pitch" -> Text.formatNumber(Math.floor(value.getPitch()));

      case "world" -> value.getWorld();

      case "worldIfNotDefault" -> {
        boolean defaultWorld = Objects.equals(Worlds.overworld(), value.getWorld());
        yield Messages.location(value, !defaultWorld, clickable);
      }

      default -> {
        boolean sameWorld = player != null && Objects.equals(value.getWorld(), player.getWorld());
        yield Messages.location(value, !sameWorld, clickable);
      }
    };
  };

  ObjectPlaceholder<User> USER = new UserObjectPlaceholder();

  ObjectPlaceholder<Player> PLAYER = (value, fieldName, ctx) -> {
    User user = Users.get(value);
    return USER.lookup(user, fieldName, ctx);
  };

  ObjectPlaceholder<CommandSource> COMMAND_SOURCE = (value, fieldName, ctx) -> {
    if (value.isPlayer()) {
      return PLAYER.lookup(value.asPlayerOrNull(), fieldName, ctx);
    }

    return switch (fieldName) {
      case "world" -> value.getWorld();
      case "location" -> value.getLocation();
      default -> value.displayName();
    };
  };

  ObjectPlaceholder<Boolean> BOOLEAN = (value, fieldName, ctx) -> {
    return switch (fieldName) {
      case "onoff" -> text(value ? "on" : "off");
      case "OnOff" -> text(value ? "On" : "Off");
      case "state" -> text(value ? "Enabled" : "Disabled");
      case "yesno" -> text(value ? "yes" : "no");
      case "YesNo" -> text(value ? "Yes" : "No");
      default -> text(value);
    };
  };

  ObjectPlaceholder<Server> SERVER = (value, fieldName, ctx) -> {
    return switch (fieldName) {
      case "version" -> text(value.getMinecraftVersion());
      case "bukkitVersion" -> text(value.getVersion());
      case "maxPlayers" -> text(value.getMaxPlayers());

      case "onlinePlayers" -> {
        Set<User> onlineUsers = new HashSet<>(Users.getOnline());
        User playerViewer = Audiences.getUser(ctx.viewer());

        if (playerViewer != null) {
          onlineUsers.removeIf(user -> !playerViewer.canSee(user));
        }

        yield onlineUsers.size();
      }

      default -> Messages.MESSAGE_LIST.renderText("server.name", ctx.viewer());
    };
  };

  ObjectPlaceholder<Duration> DURATION = (value, fieldName, ctx) -> {
    return switch (fieldName) {
      case "abs" -> value.abs();

      case "seconds"  -> value.getSeconds();
      case "minutes"  -> value.getSeconds() / 60;
      case "hours"    -> value.getSeconds() / 60 / 60;
      case "days"     -> value.getSeconds() / 60 / 60 / 24;

      case "singleUnit" -> PeriodFormat.of(value).retainBiggest().asComponent();
      case "short" -> PeriodFormat.of(value).withShortNames().asComponent();

      case "singleUnitShort", "singleShortUnit", "shortSingleUnit" -> {
        yield PeriodFormat.of(value).withShortNames().retainBiggest().asComponent();
      }

      default -> PeriodFormat.of(value).asComponent();
    };
  };

  ObjectPlaceholder<ZonedDateTime> ZONED_DATE_TIME = (value, fieldName, ctx) -> {
    return switch (fieldName) {
      case "until" -> {
        ZonedDateTime now = ZonedDateTime.now();
        yield Duration.between(now, value);
      }

      case "year" -> value.getYear();
      case "dayOfYear" -> value.getDayOfYear();
      case "date" -> value.getDayOfMonth();
      case "weekDay" -> value.getDayOfWeek().getValue();
      case "monthNum" -> value.getMonthValue();

      default -> Text.formatDate(value.toInstant());
    };
  };

  ObjectPlaceholder<Instant> INSTANT = (value, fieldName, ctx) -> {
    if (fieldName.isEmpty()) {
      return Text.formatDate(value);
    }

    ZonedDateTime dateTime = ZonedDateTime.ofInstant(value, ZoneId.systemDefault());
    return ZONED_DATE_TIME.lookup(dateTime, fieldName, ctx);
  };

  @SuppressWarnings("rawtypes")
  ObjectPlaceholder<Enum> ENUM = (value, fieldName, ctx) -> {
    return switch (fieldName) {
      case "ordinal" -> value.ordinal();
      case "raw" -> value.name();
      case "formatted" -> Text.prettyEnumName(value);
      default -> value.name().toLowerCase();
    };
  };

  ObjectPlaceholder<Vectori> VECTOR_I = (value, fieldName, ctx) -> {
    return switch (fieldName) {
      case "x" -> value.toArray()[0];
      case "y" -> value.toArray()[1];
      case "z" -> value.toArray()[2];
      case "w" -> value.toArray()[3];

      case "xy" -> {
        var arr = value.toArray();
        yield Vector2i.from(arr[0], arr[1]);
      }

      case "xz" -> {
        var arr = value.toArray();
        yield Vector2i.from(arr[0], arr[2]);
      }

      case "length" -> value.length();
      case "negate" -> value.negate();

      case "volume" -> {
        var arr = value.toArray();
        double result = 1;

        for (var v : arr) {
          result *= v;
        }

        yield result;
      }

      default -> TextFormatTypes.VECTOR.resolve(value, "", ctx.viewer());
    };
  };

  ObjectPlaceholder<Vectorf> VECTOR_F = (value, fieldName, ctx) -> {
    return switch (fieldName) {
      case "x" -> value.toArray()[0];
      case "y" -> value.toArray()[1];
      case "z" -> value.toArray()[2];
      case "w" -> value.toArray()[3];

      case "xy" -> {
        var arr = value.toArray();
        yield Vector2f.from(arr[0], arr[1]);
      }

      case "xz" -> {
        var arr = value.toArray();
        yield Vector2f.from(arr[0], arr[2]);
      }

      case "length" -> value.length();
      case "negate" -> value.negate();

      case "volume" -> {
        var arr = value.toArray();
        double result = 1;

        for (var v : arr) {
          result *= v;
        }

        yield result;
      }

      default -> TextFormatTypes.VECTOR.resolve(value, "", ctx.viewer());
    };
  };

  ObjectPlaceholder<Vectord> VECTOR_D = (value, fieldName, ctx) -> {
    return switch (fieldName) {
      case "x" -> value.toArray()[0];
      case "y" -> value.toArray()[1];
      case "z" -> value.toArray()[2];
      case "w" -> value.toArray()[3];

      case "xy" -> {
        var arr = value.toArray();
        yield Vector2d.from(arr[0], arr[1]);
      }

      case "xz" -> {
        var arr = value.toArray();
        yield Vector2d.from(arr[0], arr[2]);
      }

      case "length" -> value.length();
      case "negate" -> value.negate();

      case "volume" -> {
        var arr = value.toArray();
        double result = 1;

        for (var v : arr) {
          result *= v;
        }

        yield result;
      }

      default -> TextFormatTypes.VECTOR.resolve(value, "", ctx.viewer());
    };
  };

  Object lookup(@NotNull T value, @NotNull String fieldName, @NotNull PlaceholderContext ctx);
}
