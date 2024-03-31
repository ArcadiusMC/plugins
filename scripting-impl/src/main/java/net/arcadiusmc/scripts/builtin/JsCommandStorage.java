package net.arcadiusmc.scripts.builtin;

import static net.forthecrown.nbt.TypeIds.BYTE;
import static net.forthecrown.nbt.TypeIds.BYTE_ARRAY;
import static net.forthecrown.nbt.TypeIds.COMPOUND;
import static net.forthecrown.nbt.TypeIds.DOUBLE;
import static net.forthecrown.nbt.TypeIds.END;
import static net.forthecrown.nbt.TypeIds.FLOAT;
import static net.forthecrown.nbt.TypeIds.INT;
import static net.forthecrown.nbt.TypeIds.LIST;
import static net.forthecrown.nbt.TypeIds.LONG;
import static net.forthecrown.nbt.TypeIds.LONG_ARRAY;
import static net.forthecrown.nbt.TypeIds.SHORT;
import static net.forthecrown.nbt.TypeIds.STRING;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.io.StringReader;
import java.util.Map.Entry;
import lombok.Getter;
import lombok.Setter;
import net.arcadiusmc.utils.VanillaAccess;
import net.arcadiusmc.utils.io.TagOps;
import net.forthecrown.nbt.BinaryTag;
import net.forthecrown.nbt.CollectionTag;
import net.forthecrown.nbt.CompoundTag;
import org.bukkit.NamespacedKey;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeJSON;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

public class JsCommandStorage implements Scriptable {

  @Getter @Setter
  Scriptable parentScope;

  @Override
  public String getClassName() {
    return "ServerCommandStorage";
  }

  NamespacedKey getKey(String name) {
    int colonIndex = name.indexOf(':');
    if (colonIndex == -1) {
      name = "arcadius-scripts:" + name;
    }

    return NamespacedKey.fromString(name);
  }

  @Override
  public Object get(String name, Scriptable start) {
    NamespacedKey key = getKey(name);
    if (key == null) {
      return NOT_FOUND;
    }

    CompoundTag tag = VanillaAccess.getStoredData(key);
    Context cx = Context.getCurrentContext();

    if (tag == null || tag.isEmpty()) {
      return cx.newObject(parentScope);
    }

    return toScriptable(tag, cx, parentScope);
  }

  @Override
  public Object get(int index, Scriptable start) {
    return NOT_FOUND;
  }

  @Override
  public boolean has(String name, Scriptable start) {
    NamespacedKey key = getKey(name);
    if (key == null) {
      return false;
    }

    return VanillaAccess.getStoredData(key).size() > 1;
  }

  @Override
  public boolean has(int index, Scriptable start) {
    return false;
  }

  @Override
  public void put(String name, Scriptable start, Object value) {
    NamespacedKey key = getKey(name);
    if (key == null) {
      throw ScriptRuntime.typeError("Invalid resource key: " + name);
    }

    Context ctx = Context.getCurrentContext();
    Object jsonStr = NativeJSON.stringify(ctx, parentScope, value, null, null);
    JsonElement elem = JsonParser.parseReader(new StringReader(String.valueOf(jsonStr)));

    CompoundTag tag = JsonOps.INSTANCE.convertTo(TagOps.OPS, elem)
        .asCompound();

    VanillaAccess.setStoredData(key, tag);
  }

  @Override
  public void put(int index, Scriptable start, Object value) {

  }

  @Override
  public void delete(String name) {

  }

  @Override
  public void delete(int index) {

  }

  @Override
  public Scriptable getPrototype() {
    return null;
  }

  @Override
  public void setPrototype(Scriptable prototype) {

  }

  @Override
  public Object[] getIds() {
    return new Object[0];
  }

  @Override
  public Object getDefaultValue(Class<?> hint) {
    if (String.class == hint) {
      return getClassName();
    }
    return null;
  }

  @Override
  public boolean hasInstance(Scriptable instance) {
    return false;
  }

  Object toScriptable(BinaryTag tag, Context ctx, Scriptable parentScope) {
    return switch (tag.getId()) {
      case END -> null;
      case BYTE, SHORT, INT, LONG, FLOAT, DOUBLE -> tag.asNumber().doubleValue();
      case STRING -> tag.toString();

      case BYTE_ARRAY, LONG_ARRAY, LIST -> {
        CollectionTag cTag = (CollectionTag) tag;
        int length = cTag.size();
        Scriptable jsArray = ctx.newArray(parentScope, length);

        for (int i = 0; i < length; i++) {
          Object jsValue = toScriptable(cTag.getTag(i), ctx, jsArray);
          ScriptableObject.putProperty(jsArray, i, jsValue);
        }

        yield jsArray;
      }

      case COMPOUND -> {
        CompoundTag cTag = tag.asCompound();
        Scriptable jsObject = ctx.newObject(parentScope);

        for (Entry<String, BinaryTag> entry : cTag.entrySet()) {
          String key = entry.getKey();
          Object jsValue = toScriptable(entry.getValue(), ctx, jsObject);
          ScriptableObject.putProperty(jsObject, key, jsValue);
        }

        yield jsObject;
      }

      default -> throw new IllegalStateException("Unexpected value: " + tag.getId());
    };
  }
}
