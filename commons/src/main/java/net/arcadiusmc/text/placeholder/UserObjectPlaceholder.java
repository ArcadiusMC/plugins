package net.arcadiusmc.text.placeholder;

import com.google.common.base.Strings;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.text.UnitFormat;
import net.arcadiusmc.user.TimeField;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.UserService;
import net.arcadiusmc.user.Users;
import net.arcadiusmc.user.currency.Currency;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class UserObjectPlaceholder implements ObjectPlaceholder<User> {

  @Override
  public Object lookup(
      @NotNull User user,
      @Nullable String fieldName,
      @NotNull PlaceholderContext render
  ) {
    if (Strings.isNullOrEmpty(fieldName)) {
      return user.displayName(render.viewer());
    }

    var location = user.getLocation();

    String x     = String.format("%.2f", location.getX());
    String y     = String.format("%.2f", location.getY());
    String z     = String.format("%.2f", location.getZ());
    String yaw   = String.format("%.2f", location.getYaw());
    String pitch = String.format("%.2f", location.getPitch());

    String bx = "" + location.getBlockX();
    String by = "" + location.getBlockY();
    String bz = "" + location.getBlockZ();

    UserService service = Users.getService();

    var currencyOpt = service.getCurrencies().get(fieldName);

    if (currencyOpt.isPresent()) {
      Currency currency = currencyOpt.get();
      int value = currency.get(user.getUniqueId());
      Component formatted = currency.format(value);

      return TextPlaceholder.simple(formatted);
    }

    return switch (fieldName) {
      case "x" -> location.x();
      case "y" -> location.y();
      case "z" -> location.z();

      case "bx" -> bx;
      case "by" -> by;
      case "bz" -> bz;

      case "pos" -> x + " " + y + " " + z;
      case "block" -> bx + " " + by + " " + bz;

      case "world" -> location.getWorld();
      case "location" -> location;

      case "yaw" -> yaw;
      case "pitch" -> pitch;

      case "uuid" -> user.getUniqueId();

      case "ip" -> {
        var ip = user.getIp();
        if (Strings.isNullOrEmpty(ip)) {
          yield null;
        }
        yield ip;
      }

      case "playtime" -> {
        int playtime = user.getPlayTime();
        yield UnitFormat.playTime(playtime);
      }

      case "votes" -> {
        int votes = user.getTotalVotes();
        yield UnitFormat.votes(votes);
      }

      case "property" -> (TextPlaceholder) (match, r1) -> {
        if (match.isEmpty()) {
          return null;
        }

        return service.getUserProperties().get(match)
            .map(property -> Text.valueOf(user.get(property), r1.viewer()))
            .orElse(null);
      };

      case "timestamp" -> (TextPlaceholder) (match, r1) -> {
        if (match.isEmpty()) {
          return null;
        }

        return TimeField.REGISTRY.get(match)
            .map(user::getTime)
            .map(Text::formatDate)
            .orElse(null);
      };

      default -> null;
    };
  }
}
