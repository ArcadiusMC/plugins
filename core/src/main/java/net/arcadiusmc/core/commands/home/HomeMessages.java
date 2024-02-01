package net.arcadiusmc.core.commands.home;

import static net.arcadiusmc.text.Messages.MESSAGE_LIST;

import com.mojang.brigadier.ImmutableStringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.arcadiusmc.text.loader.MessageRef;

public interface HomeMessages {

  MessageRef SET_FORBIDDEN = makeRef("error.set");
  MessageRef FORBIDDEN = makeRef("error.forbidden");
  MessageRef LIMIT_REACHED = makeRef("error.limit");
  MessageRef NO_DEFAULT = makeRef("error.noDefault");
  MessageRef UNKNOWN = makeRef("error.unknown");

  MessageRef DELETED_SELF = makeRef("deleted.regular.self");
  MessageRef DELETED_OTHER = makeRef("deleted.regular.other");

  MessageRef DELETED_DEF_SELF = makeRef("deleted.default.self");
  MessageRef DELETED_DEF_OTHER = makeRef("deleted.default.other");

  MessageRef TELEPORT_REG = makeRef("teleport.regular");
  MessageRef TELEPORT_DEF = makeRef("teleport.default");

  MessageRef SET = makeRef("set");
  MessageRef SET_DEFAULT = makeRef("set.default");

  MessageRef LIST_HEADER_SELF_UNLIMITED = makeRef("list.self.unlimited");
  MessageRef LIST_HEADER_SELF_LIMITED = makeRef("list.self.limited");
  MessageRef LIST_HEADER_OTHER_UNLIMITED = makeRef("list.other.unlimited");
  MessageRef LIST_HEADER_OTHER_LIMITED = makeRef("list.other.limited");

  private static MessageRef makeRef(String suffix) {
    return MESSAGE_LIST.reference("homes." + suffix);
  }

  static CommandSyntaxException unknownHome(ImmutableStringReader reader, String name) {
    return UNKNOWN.get()
        .addValue("name", name)
        .exceptionWithContext(reader);
  }
}
