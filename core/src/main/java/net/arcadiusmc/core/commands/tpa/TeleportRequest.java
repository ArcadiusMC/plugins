package net.arcadiusmc.core.commands.tpa;

import static net.arcadiusmc.core.commands.tpa.TpMessages.ACCEPTED_SENDER;
import static net.arcadiusmc.core.commands.tpa.TpMessages.ACCEPTED_TARGET;
import static net.arcadiusmc.core.commands.tpa.TpMessages.CANCELLED_SENDER;
import static net.arcadiusmc.core.commands.tpa.TpMessages.CANCELLED_TARGET;
import static net.arcadiusmc.core.commands.tpa.TpMessages.DENIED_SENDER;
import static net.arcadiusmc.core.commands.tpa.TpMessages.DENIED_TARGET;
import static net.arcadiusmc.core.commands.tpa.TpMessages.REQUEST_HERE_SENDER;
import static net.arcadiusmc.core.commands.tpa.TpMessages.REQUEST_HERE_TARGET;
import static net.arcadiusmc.core.commands.tpa.TpMessages.REQUEST_NORMAL_SENDER;
import static net.arcadiusmc.core.commands.tpa.TpMessages.REQUEST_NORMAL_TARGET;
import static net.arcadiusmc.core.commands.tpa.TpMessages.makeRequestMessage;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.time.Duration;
import java.util.UUID;
import lombok.Getter;
import net.arcadiusmc.command.request.PlayerRequest;
import net.arcadiusmc.command.request.RequestTable;
import net.arcadiusmc.core.CoreConfig;
import net.arcadiusmc.core.CorePlugin;
import net.arcadiusmc.text.loader.MessageRef;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.UserTeleport;
import org.bukkit.Sound;

public class TeleportRequest extends PlayerRequest {

  /**
   * Determines whether the sender or target will be teleporting.
   * <p>
   * If <code>tpaHere</code> == true, the target of this request will be teleporting, otherwise the
   * sender will be teleporting
   */
  @Getter
  private final boolean tpaHere;

  boolean accepted;

  public TeleportRequest(UUID senderId, UUID targetId, boolean tpaHere) {
    super(senderId, targetId);
    this.tpaHere = tpaHere;
  }

  /**
   * Executes a teleport request.
   *
   * @param sender  The sender of the request
   * @param target  The recipient of the request
   * @param tpaHere True, if sender wants the target to teleport to them, false if it's the other
   *                way round
   */
  public static void run(User sender, User target, boolean tpaHere) throws CommandSyntaxException {
    TeleportRequest request = new TeleportRequest(sender.getUniqueId(), target.getUniqueId(), tpaHere);
    RequestTable<TeleportRequest> table = TeleportRequests.getTable();
    table.sendRequest(request);
  }

  @Override
  public void onBegin() {
    var sender = getSender();
    var target = getTarget();

    MessageRef senderFormat = tpaHere ? REQUEST_HERE_SENDER : REQUEST_NORMAL_SENDER;
    MessageRef targetFormat = tpaHere ? REQUEST_HERE_TARGET : REQUEST_NORMAL_TARGET;

    sender.sendMessage(makeRequestMessage(senderFormat, this, sender));
    target.sendMessage(makeRequestMessage(targetFormat, this, target));

    sender.playSound(Sound.UI_TOAST_OUT, 2, 1.5f);
    target.playSound(Sound.UI_TOAST_IN, 2, 1.3f);
  }

  @Override
  protected Duration getExpiryDuration() {
    CoreConfig config = CorePlugin.plugin().getCoreConfig();
    return config.tpaExpireTime();
  }

  /**
   * Accepts the TPA request, tells both uses the request was accepted and starts the
   * {@link UserTeleport} to teleport either the sender or target to the other user.
   */
  public void accept() throws CommandSyntaxException {
    accepted = true;

    super.accept();

    User sender = getSender();
    User target = getTarget();

    sender.sendMessage(makeRequestMessage(ACCEPTED_SENDER, this, sender));
    target.sendMessage(makeRequestMessage(ACCEPTED_TARGET, this, target));

    // If tpaHere, target is teleporting,
    // otherwise it's the opposite
    User teleporting = tpaHere ? target : sender;
    User notTeleporting = tpaHere ? sender : target;

    teleporting.createTeleport(notTeleporting::getLocation, UserTeleport.Type.TPA)
        .start();

    stop();
  }

  /**
   * Tells the users the request was denied and calls {@link #stop()} to stop this request
   */
  public void deny() {
    var sender = getSender();
    var target = getTarget();

    sender.sendMessage(makeRequestMessage(DENIED_SENDER, this, sender));
    target.sendMessage(makeRequestMessage(DENIED_TARGET, this, target));

    stop();
  }

  /**
   * Tells the users this request was cancelled and calls {@link #stop()} to stop this request
   */
  public void cancel() {
    var sender = getSender();
    var target = getTarget();

    sender.sendMessage(makeRequestMessage(CANCELLED_SENDER, this, sender));
    target.sendMessage(makeRequestMessage(CANCELLED_TARGET, this, target));

    stop();
  }
}
