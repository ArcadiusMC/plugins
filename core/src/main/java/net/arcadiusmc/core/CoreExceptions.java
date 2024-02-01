package net.arcadiusmc.core;

import static net.arcadiusmc.command.Exceptions.create;
import static net.arcadiusmc.command.Exceptions.format;
import static net.arcadiusmc.text.Messages.MESSAGE_LIST;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.arcadiusmc.text.loader.MessageRef;
import net.arcadiusmc.user.User;
import org.bukkit.Location;

public interface CoreExceptions {

  MessageRef CANNOT_IGNORE_SELF = MESSAGE_LIST.reference("blocking.self");

  MessageRef NO_RETURN = MESSAGE_LIST.reference("cmd.back.noReturn");

  MessageRef RETURN_FORBIDDEN = MESSAGE_LIST.reference("cmd.back.forbidden");

  MessageRef CANNOT_REMOVE_HELMET = MESSAGE_LIST.reference("cmd.hat.cannotRemove");

  MessageRef NO_NEARBY_PLAYERS = MESSAGE_LIST.reference("cmd.near.empty");

  MessageRef SERVER_EMPTY = MESSAGE_LIST.reference("cmd.list.emptyServer");

  MessageRef NO_REPLY_TARGETS = MESSAGE_LIST.reference("cmd.tell.noReplyTargets");

  /**
   * Exception stating that a given item is not repairable
   */
  CommandSyntaxException NOT_REPAIRABLE = create("Given item is not repairable");

  CommandSyntaxException NO_ATTR_MODS = create("No attribute modifiers to remove");

  MessageRef HOLD_COINS = MESSAGE_LIST.reference("coins.noneHeld");

  static CommandSyntaxException profilePrivate(User user) {
    return format("{0, user}'s profile is not public.", user);
  }

  static CommandSyntaxException notSign(Location l) {
    return format("{0, location, -c -w} is not a sign", l);
  }
}
