package net.arcadiusmc.scripts.builtins;

import net.arcadiusmc.scripts.Scripts;
import net.forthecrown.grenadier.CommandSource;
import net.kyori.adventure.text.Component;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

public enum SendMessageFunction implements ProxyExecutable {
  MESSAGE (false),
  ACTIONBAR (true);

  private final boolean actionbar;

  SendMessageFunction(boolean actionbar) {
    this.actionbar = actionbar;
  }

  @Override
  public Object execute(Value... arguments) {
    Scripts.ensureParameterCount(arguments, 2);

    CommandSource source = Scripts.toSource(arguments[0]);

    for (int i = 1; i < arguments.length; i++) {
      Component text = Scripts.toText(arguments[i], source);

      if (actionbar) {
        source.sendActionBar(text);
      } else {
        source.sendMessage(text);
      }
    }

    return null;
  }
}
