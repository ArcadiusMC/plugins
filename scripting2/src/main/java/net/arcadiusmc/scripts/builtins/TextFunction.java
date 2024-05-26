package net.arcadiusmc.scripts.builtins;

import static net.kyori.adventure.text.Component.text;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.scripts.Scripts;
import net.arcadiusmc.text.Text;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent.Builder;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

public enum TextFunction implements ProxyExecutable {
  TEXT;

  @Override
  public Object execute(Value... arguments) {
    int argsLength = arguments.length;

    if (argsLength == 0) {
      return Component.empty();
    }

    if (argsLength == 1) {
      Value arg = arguments[0];

      if (arg.isString()) {
        return text(arg.asString());
      }

      return Text.valueOf(arg);
    }

    Value arg2 = arguments[1];

    if (argsLength == 2) {
      Value arg = arguments[0];

      if (Scripts.isInstance(arg2, Audience.class)) {
        return Text.valueOf(arg.toString(), arg2.as(Audience.class));
      }
    }

    String content = arguments[0].asString();
    TextColor color;

    if (Scripts.isInstance(arg2, TextColor.class)) {
      color = arg2.as(TextColor.class);
    } else if (arg2.isString()) {
      String str = arg2.asString();

      try {
        color = Arguments.COLOR.parse(new StringReader(str));
      } catch (CommandSyntaxException exc) {
        throw Scripts.typeError("Invalid color string: '%s': %s", str, exc.getRawMessage().getString());
      }
    } else if (arg2.isNumber()) {
      color = TextColor.color(arg2.asInt());
    } else if (arg2.isNull()) {
      color = null;
    } else {
      throw Scripts.cantLoad("text color", arg2);
    }

    Builder builder = Component.text()
        .content(content)
        .color(color);

    for (int i = 2; i < argsLength; i++) {
      Value decoArg = arguments[i];

      TextDecoration decoration;

      if (Scripts.isInstance(decoArg, TextDecoration.class)) {
        decoration = decoArg.as(TextDecoration.class);
        builder.decoration(decoration, true);
        continue;
      }

      if (!decoArg.isString()) {
        throw Scripts.cantLoad("text decoration", decoArg);
      }

      String name = decoArg.asString();
      boolean state;

      if (name.startsWith("!")) {
        state = false;
        name = name.substring(1);
      } else {
        state = true;
      }

      decoration = TextDecoration.NAMES.value(name.toLowerCase());

      if (decoration == null) {
        throw Scripts.typeError("Unknown decoration: %s", name);
      }

      builder.decoration(decoration, state);
    }

    return builder.build();
  }
}
