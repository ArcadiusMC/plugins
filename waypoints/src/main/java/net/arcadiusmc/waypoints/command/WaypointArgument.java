package net.arcadiusmc.waypoints.command;

import com.google.common.base.Strings;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import net.arcadiusmc.command.ArcSuggestions;
import net.arcadiusmc.command.arguments.ParseResult;
import net.arcadiusmc.user.UserLookup.LookupEntry;
import net.arcadiusmc.user.Users;
import net.arcadiusmc.waypoints.WExceptions;
import net.arcadiusmc.waypoints.WPermissions;
import net.arcadiusmc.waypoints.Waypoint;
import net.arcadiusmc.waypoints.WaypointExtension;
import net.arcadiusmc.waypoints.WaypointManager;
import net.arcadiusmc.waypoints.WaypointProperties;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.Completions;
import net.forthecrown.grenadier.Readers;

public class WaypointArgument implements ArgumentType<ParseResult<Waypoint>> {

  private static final int ALIAS_SUGGESTION_INPUT_REQ = 3;

  public static final String FLAG_CURRENT = "-current";
  public static final String FLAG_NEAREST = "-nearest";

  @Override
  public ParseResult<Waypoint> parse(StringReader reader) throws CommandSyntaxException {
    int start = reader.getCursor();
    var name = reader.readUnquotedString();

    // By flags
    if (name.equalsIgnoreCase(FLAG_CURRENT)) {
      return WaypointResults.CURRENT;
    } else if (name.equalsIgnoreCase(FLAG_NEAREST)) {
      return WaypointResults.NEAREST;
    }

    // By waypoint name
    WaypointManager manager = WaypointManager.getInstance();
    Waypoint waypoint = manager.get(name);
    if (waypoint != null) {
      return new WaypointResults.DirectResult(waypoint);
    }

    // By username
    LookupEntry lookup = Users.getService().getLookup().query(name);
    if (lookup != null) {
      return new WaypointResults.UserResult(lookup);
    }

    // By guild name
    var extensions = manager.getExtensions();

    for (var e: extensions) {
      try {
        var extReader = Readers.copy(reader, start);
        ParseResult<Waypoint> result = e.parse(extReader);

        if (result != null) {
          return result;
        }
      } catch (CommandSyntaxException exc) {
        continue;
      }
    }

    throw WExceptions.unknownRegion(reader, start);
  }

  @Override
  public <S> CompletableFuture<Suggestions> listSuggestions(
      CommandContext<S> context,
      SuggestionsBuilder builder
  ) {
    CommandSource source = (CommandSource) context.getSource();

    if (source.hasPermission(WPermissions.WAYPOINTS_FLAGS)) {
      Completions.suggest(builder, FLAG_NEAREST, FLAG_CURRENT);
    }

    // Suggest players
    ArcSuggestions.suggestPlayerNames(source, builder, false);

    WaypointManager manager = WaypointManager.getInstance();

    Collection<WaypointExtension> extensions = manager.getExtensions();
    extensions.forEach(extension -> extension.addSuggestions(builder, source));

    // Suggest names and only include aliases if the input is longer
    // than 3 characters
    String remaining = builder.getRemainingLowerCase();
    boolean admin = source.hasPermission(WPermissions.WAYPOINTS_ADMIN);

    for (Waypoint waypoint : manager.getWaypoints()) {
      String name = waypoint.get(WaypointProperties.NAME);

      if (Strings.isNullOrEmpty(name)) {
        continue;
      }
      if (!Completions.matches(remaining, name)) {
        continue;
      }
      if (waypoint.get(WaypointProperties.DISABLED) && !admin) {
        continue;
      }

      builder.suggest(name);
    }

    return builder.buildFuture();
  }
}