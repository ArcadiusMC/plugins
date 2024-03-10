package net.arcadiusmc.waypoints;

import static net.arcadiusmc.text.Text.format;
import static net.kyori.adventure.text.Component.text;

import net.arcadiusmc.user.User;
import net.arcadiusmc.waypoints.type.WaypointType;
import net.arcadiusmc.waypoints.type.WaypointTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.spongepowered.math.vector.Vector3i;

public interface WMessages {


  Component HOME_WAYPOINT_SET = text(
      """
      Home waypoint set.
      Use /invite <player> to invite others\s
      Use /home to come to this waypoint when near another waypoint.
      """.trim(),

      NamedTextColor.YELLOW
  );

  static Component senderInvited(User target) {
    return format("Invited &e{0, user}&r.",
        NamedTextColor.GOLD, target
    );
  }

  static Component targetInvited(User sender) {
    return format("&e{0, user}&r has invited you to their region.",
        NamedTextColor.GOLD, sender
    );
  }

  static Component invitedTotal(int count) {
    return format("Invited a total of &e{0, number}&r people.",
        NamedTextColor.GOLD, count
    );
  }

  static Component createdWaypoint(Vector3i pos, WaypointType type) {
    String typeStr;

    if (type == WaypointTypes.REGION_POLE) {
      typeStr = "Region pole";
    } else if (type == WaypointTypes.ADMIN) {
      typeStr = "Admin waypoint";
    } else {
      typeStr = type.getDisplayName() + " Waypoint";
    }

    return format("Created &e{0}&r at x&6{1}&r y&6{2}&r z&6{3}&r.",
        NamedTextColor.GRAY,

        typeStr,
        pos.x(), pos.y(), pos.z()
    );
  }

}