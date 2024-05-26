package net.arcadiusmc.scripts.builtins;

import java.util.Set;
import net.arcadiusmc.scripts.Scripts;
import net.arcadiusmc.text.placeholder.PlaceholderContext;
import net.arcadiusmc.text.placeholder.PlaceholderRenderer;
import net.arcadiusmc.text.placeholder.Placeholders;
import net.arcadiusmc.text.placeholder.TextPlaceholder;
import net.forthecrown.grenadier.CommandSource;
import net.kyori.adventure.text.Component;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.jetbrains.annotations.Nullable;

public enum RenderPlaceholdersFunction implements ProxyExecutable {
  RENDER;

  @Override
  public Object execute(Value... arguments) {
    if (arguments.length > 4) {
      throw Scripts.typeError("Too many arguments! (Max: 4)");
    }
    if (arguments.length == 0) {
      return null;
    }

    CommandSource source = Scripts.toSource(arguments[1]);
    Component baseText = Scripts.toText(arguments[2], source);
    PlaceholderRenderer renderer = toRenderer(arguments, 3);

    Value placeholderTable = getPlaceholderTable(arguments, 2);

    if (placeholderTable != null) {
      Set<String> keys = placeholderTable.getMemberKeys();

      for (String key : keys) {
        Value value = placeholderTable.getMember(key);
        TextPlaceholder placeholder = new WrappedPlaceholder(value);
        renderer.add(key, placeholder);
      }
    }

    return renderer.render(baseText, source);
  }

  Value getPlaceholderTable(Value[] args, int index) {
    if (args.length <= index) {
      return null;
    }

    Value arg = args[index];

    if (!arg.hasMembers()) {
      return null;
    }

    return arg;
  }

  private PlaceholderRenderer toRenderer(Value[] args, int index) {
    if (args.length <= index) {
      return Placeholders.newRenderer().useDefaults();
    }

    Value value = args[index];

    if (value.isHostObject()) {
      Object host = value.asHostObject();

      if (host instanceof PlaceholderRenderer renderer) {
        return renderer;
      }
      if (host instanceof PlaceholderContext context) {
        return context.renderer();
      }
    }

    return Placeholders.newRenderer().useDefaults();
  }

  record WrappedPlaceholder(Value value) implements TextPlaceholder {

    @Override
    public @Nullable Component render(String match, PlaceholderContext render) {
      if (value.canExecute()) {
        Value result = value.execute(match, render);
        return Scripts.toText(result, render.viewer());
      }

      return Scripts.toText(value, render.viewer());
    }
  }
}
