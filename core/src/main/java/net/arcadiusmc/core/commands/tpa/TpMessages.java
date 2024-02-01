package net.arcadiusmc.core.commands.tpa;

import static net.arcadiusmc.core.commands.tpa.TpExceptions.makeRef;
import static net.arcadiusmc.text.Messages.MESSAGE_LIST;
import static net.arcadiusmc.text.Messages.crossButton;
import static net.arcadiusmc.text.Messages.tickButton;
import static net.arcadiusmc.text.Text.format;

import net.arcadiusmc.text.loader.MessageRef;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.UserTeleport;
import net.arcadiusmc.utils.Time;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public interface TpMessages {

  /**
   * Message used by {@link UserTeleport} to tell users that the delayed teleport was cancelled,
   * most likely because they moved
   */
  MessageRef TELEPORT_CANCELLED = MESSAGE_LIST.reference("teleporting.cancelled");

  /**
   * Message shown to a user when the {@link UserTeleport#getDestination()} supplier throws an
   * error
   */
  MessageRef TELEPORT_ERROR = MESSAGE_LIST.reference("teleporting.destinationError");

  MessageRef TELEPORT_START = MESSAGE_LIST.reference("teleporting.start");

  MessageRef TELEPORT_FINISH = MESSAGE_LIST.reference("teleporting.complete");

  /**
   * Message stating the viewer is already teleporting
   */
  MessageRef ALREADY_TELEPORTING = makeRef("error.teleporting");

  /**
   * Message stating the viewer denied all incoming TPA requests
   */
  MessageRef TPA_DENIED_ALL = makeRef("denyAll");

  MessageRef REQUEST_NORMAL_SENDER = makeRef("request.normal.sender");
  MessageRef REQUEST_NORMAL_TARGET = makeRef("request.normal.target");
  MessageRef REQUEST_HERE_SENDER = makeRef("request.here.sender");
  MessageRef REQUEST_HERE_TARGET = makeRef("request.here.target");

  MessageRef ACCEPTED_TARGET = makeRef("accepted.target");
  MessageRef ACCEPTED_SENDER = makeRef("accepted.sender");

  MessageRef DENIED_SENDER = makeRef("denied.sender");
  MessageRef DENIED_TARGET = makeRef("denied.target");

  MessageRef CANCELLED_SENDER = makeRef("cancelled.sender");
  MessageRef CANCELLED_TARGET = makeRef("cancelled.target");

  static Component makeRequestMessage(MessageRef format, TeleportRequest request, Audience viewer) {
    User sender = request.getSender();
    User target = request.getTarget();

    return format.get()
        .addValue("sender", request.getSender())
        .addValue("target", request.getTarget())
        .addValue("here", request.isTpaHere())
        .addValue("accept", tpaAcceptButton(sender))
        .addValue("deny", tpaDenyButton(sender))
        .addValue("cancel", tpaCancelButton(target))
        .create(viewer);
  }

  static Component userTeleportMessage(UserTeleport teleport, MessageRef ref) {
    return ref.get()
        .addValue("type", teleport.getType())
        .addValue("action", teleport.getType().getAction())
        .addValue("delay", teleport.getDelay())
        .create(teleport.getUser());
  }

  /**
   * Creates a tpa cancel button
   *
   * @param target The target of the tpa request
   * @return The formatted button component
   */
  static Component tpaCancelButton(User target) {
    return crossButton("/tpacancel %s", target.getName());
  }

  /**
   * Creates a tpa accept button
   *
   * @param sender The sender of the tpa request
   * @return The formatted button component
   */
  static Component tpaAcceptButton(User sender) {
    return tickButton("/tpaccept %s", sender.getName());
  }

  /**
   * Creates a tpa deny button
   *
   * @param sender The sender of the tpa request
   * @return The formatted button component
   */
  static Component tpaDenyButton(User sender) {
    return crossButton("/tpdeny %s", sender.getName());
  }

  /**
   * Creates a message stating the viewer can teleport again in x amount of time
   *
   * @param nextTpTime The next allowed timestamp the user can teleport at
   * @return The formatted component
   */
  static Component canTeleportIn(long nextTpTime) {
    return format("You can teleport again in &6{0, time}",
        NamedTextColor.GRAY,
        Time.timeUntil(nextTpTime)
    );
  }

}
