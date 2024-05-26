package net.arcadiusmc.scripts.builtins;

import net.arcadiusmc.scripts.Scripts;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.Grenadier;
import org.bukkit.Bukkit;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.slf4j.Logger;

public enum ExecCommandFunction implements ProxyExecutable {
  CONSOLE (false, false),
  RUN_AS (false, true),
  CONSOLE_SILENT (true, false),
  RUN_AS_SILENT (true, true);

  private final boolean silent;
  private final boolean senderExpected;

  ExecCommandFunction(boolean silent, boolean senderExpected) {
    this.silent = silent;
    this.senderExpected = senderExpected;
  }

  @Override
  public Object execute(Value... args) {
    String command;
    CommandSource source;

    if (senderExpected) {
      if (args.length < 2) {
        throw Scripts.typeError("Expected at least 2 arguments: sender and command");
      }

      command = Scripts.join(args, 1);
      source = Scripts.toSource(args, 0);

      assert source != null;
    } else {
      if (args.length < 1) {
        return null;
      }

      command = Scripts.join(args);
      source = Grenadier.createSource(Bukkit.getConsoleSender());
    }

    if (silent) {
      source = source.silent();
    }

    try {
      return Grenadier.dispatch(source, command);
    } catch (Throwable t) {
      Logger logger = Scripts.getLogger(args);
      logger.error("Command invocation error: ", t);
      return null;
    }
  }
}
