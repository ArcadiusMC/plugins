package net.arcadiusmc.core.commands.tpa;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import lombok.Getter;
import net.arcadiusmc.command.request.RequestTable;
import net.arcadiusmc.command.request.RequestValidator;
import net.arcadiusmc.text.loader.MessageRef;
import net.arcadiusmc.user.Properties;
import net.arcadiusmc.user.User;
import net.arcadiusmc.utils.Audiences;
import net.arcadiusmc.utils.WgUtils;
import net.kyori.adventure.audience.Audience;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class TeleportRequests {

  @Getter
  private static final RequestTable<TeleportRequest> table;

  static {
    table = new RequestTable<>();
    table.setValidator(new TpaValidator());
  }

  public static void add(TeleportRequest request) {
    table.add(request);
  }

  public static void remove(TeleportRequest request) {
    table.remove(request);
  }

  public static TeleportRequest getOutgoing(User sender, User target) {
    return table.getOutgoing(sender, target);
  }

  public static TeleportRequest getIncoming(User target, User sender) {
    return table.getIncoming(target, sender);
  }

  public static boolean clearIncoming(User target) {
    return table.clearIncoming(target);
  }

  public static TeleportRequest latestIncoming(User target) {
    return table.latestIncoming(target);
  }

  public static TeleportRequest latestOutgoing(User sender) {
    return table.latestOutgoing(sender);
  }


  static class TpaValidator implements RequestValidator<TeleportRequest> {

    static CommandSyntaxException makeError(
        MessageRef ref,
        User sender,
        User target,
        Audience viewer
    ) {
      return ref.get()
          .addValue("sender", sender)
          .addValue("target", target)
          .exception(viewer);
    }

    @Override
    public void validate(TeleportRequest request, Audience viewer) throws CommandSyntaxException {
      User sender = request.getSender();
      User target = request.getTarget();

      boolean tpaHere = request.isTpaHere();
      boolean isSender = Audiences.equals(viewer, sender);

      if (sender.equals(target)) {
        throw TpExceptions.SELF.exception(viewer);
      }

      if (!sender.get(Properties.TPA)) {
        throw makeError(
            isSender ? TpExceptions.DISABLED_SENDER : TpExceptions.DISABLED_TARGET,
            sender, target,
            viewer
        );
      }

      if (!target.get(Properties.TPA)) {
        throw makeError(
            isSender ? TpExceptions.DISABLED_TARGET : TpExceptions.DISABLED_SENDER,
            sender, target,
            viewer
        );
      }

      if (!request.accepted) {
        TeleportRequest outgoing = table.getOutgoing(sender, target);
        TeleportRequest incoming = table.getIncoming(target, sender);

        if (outgoing != null || incoming != null) {
          throw makeError(TpExceptions.ALREADY_SENT, sender, target, viewer);
        }
      }

      // Player standing at the teleport destination
      Player destinationPlayer = tpaHere ? sender.getPlayer() : target.getPlayer();
      Location destinationLocation = destinationPlayer.getLocation();

      Player teleported = tpaHere ? target.getPlayer() : sender.getPlayer();

      if (!WgUtils.testFlag(destinationLocation, WgUtils.PLAYER_TELEPORTING, teleported)) {
        MessageRef noMessage = (isSender != tpaHere)
            ? TpExceptions.FORBIDDEN_HERE
            : TpExceptions.FORBIDDEN_NORMAL;

        throw makeError(noMessage, sender, target, viewer);
      }

      MessageRef cooldownError;

      if (tpaHere) {
        if (!target.canTeleport()) {
          cooldownError = isSender ? TpExceptions.COOLDOWN_TARGET : TpExceptions.COOLDOWN_SENDER;
          throw makeError(cooldownError, sender, target, viewer);
        }
      } else if (!sender.canTeleport()) {
        cooldownError = isSender ? TpExceptions.COOLDOWN_SENDER : TpExceptions.COOLDOWN_TARGET;
        throw makeError(cooldownError, sender, target, viewer);
      }
    }
  }
}
