package net.arcadiusmc.scripts.builtin;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.arcadiusmc.scripts.NonCtorFunction;
import net.arcadiusmc.scripts.ScriptUtils;
import net.arcadiusmc.utils.inventory.ItemStacks;
import org.bukkit.inventory.ItemStack;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeJSON;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;

public class LoadItemFunction extends NonCtorFunction {

  @Override
  public Object call(
      Context cx,
      Scriptable scope,
      Scriptable thisObj,
      Object[] args
  ) {
    if (args.length != 1) {
      throw ScriptRuntime.typeError("Expected a single item argument");
    }

    Object arg = args[0];
    if (arg instanceof Scriptable scriptable) {
      if (scriptable instanceof NativeJavaObject njo) {
        if (njo.unwrap() instanceof ItemStack item) {
          return item;
        }

        throw ScriptRuntime.typeError("Cannot cast " + njo.unwrap() + " to an item stack");
      }

      String json = String.valueOf(NativeJSON.stringify(cx, scope, arg, null, null));
      JsonElement el = JsonParser.parseString(json);

      return tryLoadItem(el, arg);
    }

    if (arg instanceof CharSequence seq) {
      String str = seq.toString();
      JsonElement el;

      try {
        el = JsonParser.parseString(str);
      } catch (JsonParseException exc) {
        throw ScriptRuntime.typeError("Failed to parse '" + str + "' to JSON");
      }

      return tryLoadItem(el, arg);
    }

    throw ScriptUtils.cantLoad("ItemStack", arg);
  }

  private ItemStack tryLoadItem(JsonElement el, Object arg) {
    return ItemStacks.NMS_CODEC.parse(JsonOps.INSTANCE, el)
        .mapError(s -> "Failed to load item stack from " + arg + ": " + s)
        .getOrThrow(ScriptRuntime::typeError);
  }
}
