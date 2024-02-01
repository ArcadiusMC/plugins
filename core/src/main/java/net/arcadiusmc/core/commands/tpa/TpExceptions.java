package net.arcadiusmc.core.commands.tpa;

import static net.arcadiusmc.text.Messages.MESSAGE_LIST;

import net.arcadiusmc.text.loader.MessageRef;

public interface TpExceptions {

  MessageRef NO_INCOMING = makeRef("error.noIncoming");
  MessageRef NO_INCOMING_FROM = makeRef("error.noIncoming.from");

  MessageRef NO_OUTGOING = makeRef("error.noOutgoing");
  MessageRef NO_OUTGOING_TO = makeRef("error.noOutgoing.to");

  MessageRef NOT_TELEPORTING = makeRef("error.notTeleporting");

  MessageRef SELF = makeRef("error.self");

  MessageRef DISABLED_SENDER = makeRef("error.disabled.sender");
  MessageRef DISABLED_TARGET = makeRef("error.disabled.target");

  MessageRef ALREADY_SENT = makeRef("error.alreadySent");

  MessageRef FORBIDDEN_NORMAL = makeRef("error.forbidden.normal");
  MessageRef FORBIDDEN_HERE = makeRef("error.forbidden.here");

  MessageRef COOLDOWN_SENDER = makeRef("error.cooldown.sender");
  MessageRef COOLDOWN_TARGET = makeRef("error.cooldown.target");

  static MessageRef makeRef(String suffix) {
    return MESSAGE_LIST.reference("cmd.tpa." + suffix);
  }
}
