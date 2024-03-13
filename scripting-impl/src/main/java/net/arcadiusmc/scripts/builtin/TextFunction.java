package net.arcadiusmc.scripts.builtin;

import static net.kyori.adventure.text.Component.text;
import static org.mozilla.javascript.Context.javaToJS;
import static org.mozilla.javascript.Context.jsToJava;

import net.arcadiusmc.scripts.ScriptUtils;
import net.arcadiusmc.text.Text;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent.Builder;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;

public class TextFunction extends BaseFunction {

  @Override
  public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
    int argl = args.length;

    if (argl == 0) {
      return Component.empty();
    }

    Object[] uargs = new Object[argl];
    for (int i = 0; i < argl; i++) {
      uargs[i] = jsToJava(args[i], Object.class);
    }

    if (argl == 1) {
      Object arg = uargs[0];

      if (arg instanceof CharSequence str) {
        return text(str.toString());
      }

      return Text.valueOf(arg);
    }

    Object arg2 = uargs[1];

    if (argl == 2) {
      Object arg1 = uargs[0];

      if (arg2 instanceof Audience audience) {
        return Text.valueOf(arg1, audience);
      }
    }

    String content = String.valueOf(uargs[0]);
    TextColor color;

    if (arg2 instanceof TextColor c) {
      color = c;
    } else if (arg2 instanceof String str) {
      color = NamedTextColor.NAMES.value(str.toLowerCase());
      if (color == null) {
        throw ScriptRuntime.typeError("Unknown color '" + str + "'");
      }
    } else if (arg2 instanceof Number number) {
      color = TextColor.color(number.intValue());
    } else if (arg2 == null || arg2 == Context.getUndefinedValue()) {
      color = null;
    } else {
      throw ScriptUtils.cantLoad("text color", arg2);
    }

    Builder builder = Component.text()
        .content(content)
        .color(color);

    for (int i = 2; i < uargs.length; i++) {
      Object decoArg = uargs[i];

      TextDecoration decoration;
      boolean state;

      if (decoArg instanceof TextDecoration deco) {
        decoration = deco;
        state = true;
      } else if (decoArg instanceof CharSequence seq) {
        String name = seq.toString();

        if (name.startsWith("!")) {
          state = false;
          name = name.substring(1);
        } else {
          state = true;
        }

        decoration = TextDecoration.NAMES.value(name.toLowerCase());
      } else {
        throw ScriptUtils.cantLoad("text decoration", decoArg);
      }

      builder.decoration(decoration, state);
    }

    return builder.build();
  }

  @Override
  public Scriptable construct(Context cx, Scriptable scope, Object[] args) {
    return (Scriptable) javaToJS(call(cx, scope, scope, args), scope, cx);
  }
}
