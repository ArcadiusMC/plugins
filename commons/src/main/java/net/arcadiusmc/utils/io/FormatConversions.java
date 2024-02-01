package net.arcadiusmc.utils.io;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.time.temporal.TemporalAccessor;
import java.util.Collection;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.tomlj.TomlArray;
import org.tomlj.TomlTable;

public final class FormatConversions {
  private FormatConversions() {}

  public static JsonElement ymlToJson(Object obj) {
    if (obj instanceof CharSequence str) {
      return new JsonPrimitive(str.toString());
    }
    if (obj instanceof Character ch) {
      return new JsonPrimitive(ch);
    }
    if (obj instanceof Number number) {
      return new JsonPrimitive(number);
    }
    if (obj instanceof Boolean bool) {
      return new JsonPrimitive(bool);
    }
    if (obj == null) {
      return JsonNull.INSTANCE;
    }

    if (obj instanceof ConfigurationSection section) {
      JsonObject result = new JsonObject();

      section.getValues(false).forEach((string, o) -> {
        result.add(string, ymlToJson(o));
      });

      return result;
    }

    if (obj instanceof Map<?,?> map) {
      JsonObject result = new JsonObject();

      map.forEach((keyObj, valueObj) -> {
        String key = String.valueOf(keyObj);
        JsonElement value = ymlToJson(valueObj);
        result.add(key, value);
      });

      return result;
    }

    if (obj instanceof Collection<?> c) {
      JsonArray arr = new JsonArray();
      for (Object o : c) {
        arr.add(ymlToJson(o));
      }
      return arr;
    }

    String typeName = obj.getClass().getName();
    throw new IllegalStateException(
        "Don't know how to convert YML to JSON (" + typeName + "): " + obj
    );
  }

  public static JsonObject tomlToJson(TomlTable table) {
    JsonObject obj = new JsonObject();

    for (var e: table.entrySet()) {
      obj.add(e.getKey(), _tomlToJson(e.getValue()));
    }

    return obj;
  }

  public static JsonArray tomlToJson(TomlArray array) {
    JsonArray jArray = new JsonArray();

    for (int i = 0; i < array.size(); i++) {
      jArray.add(_tomlToJson(array.get(i)));
    }

    return jArray;
  }

  private static JsonElement _tomlToJson(Object o) {
    // Primitives
    if (o instanceof Boolean b) {
      return new JsonPrimitive(b);
    } else if (o instanceof String str) {
      return new JsonPrimitive(str);
    } else if (o instanceof Number number) {
      return new JsonPrimitive(number);
    } else if (o instanceof Character c) {
      return new JsonPrimitive(c);
    }

    // Containers
    if (o instanceof TomlTable table) {
      return tomlToJson(table);
    } else if (o instanceof TomlArray arr) {
      return tomlToJson(arr);
    }

    // Date/Time objects, they all implement TemporalAccessor
    if (o instanceof TemporalAccessor) {
      return new JsonPrimitive(o.toString());
    }

    throw new IllegalStateException("Unexpected class type: " + o);
  }
}