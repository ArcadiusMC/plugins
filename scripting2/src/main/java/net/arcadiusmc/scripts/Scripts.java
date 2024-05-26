package net.arcadiusmc.scripts;

import com.google.gson.JsonElement;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.DataResult;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import lombok.Getter;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.Worlds;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.UserLookup;
import net.arcadiusmc.user.UserLookup.LookupEntry;
import net.arcadiusmc.user.Users;
import net.arcadiusmc.utils.inventory.ItemStacks;
import net.arcadiusmc.utils.io.JsonUtils;
import net.arcadiusmc.utils.io.Results;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.Grenadier;
import net.forthecrown.grenadier.types.ArgumentTypes;
import net.forthecrown.grenadier.types.BlockArgument.Result;
import net.forthecrown.nbt.CompoundTag;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.SourceSection;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.io.IOAccess;
import org.joml.Vector2d;
import org.joml.Vector2f;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.slf4j.Logger;

public final class Scripts {
  private Scripts() {}

  private static final Logger LOGGER = Loggers.getLogger();

  public static final String JS_LANGUAGE_ID = "js";

  static final String FILE_SUFFIX = ".js";

  @Getter
  static ScriptService service;

  public static void setService(ScriptService service) {
    if (Scripts.service != null) {
      throw new IllegalStateException("Cannot change script service once changed");
    }

    Scripts.service = service;
  }

  public static DataResult<Source> fromScriptFileName(String fileName) {
    if (service == null) {
      return Results.error("Service not set");
    }

    String filePath;

    if (!fileName.endsWith(FILE_SUFFIX)) {
      filePath = fileName + FILE_SUFFIX;
    } else {
      filePath = fileName;
      fileName = fileName.substring(0, fileName.length() - FILE_SUFFIX.length());
    }

    Path directory = getService().getScriptsDirectory();
    Path file = directory.resolve(filePath);

    if (Files.notExists(file)) {
      return Results.error("Script file %s does not exist", file);
    }

    String content;

    try {
      content = Files.readString(file, StandardCharsets.UTF_8);
    } catch (IOException exc) {
      return Results.error("Error reading script file: %s: %s", file, exc.getMessage());
    }

    Source source = Source.newBuilder(JS_LANGUAGE_ID, content, fileName)
        .encoding(StandardCharsets.UTF_8)
        .buildLiteral();

    return Results.success(source);
  }

  public static Context.Builder createContext() {
    return Context.newBuilder(JS_LANGUAGE_ID)
        .allowHostAccess(
            HostAccess.newBuilder()
                .allowListAccess(true)
                .allowArrayAccess(true)
                .allowMapAccess(true)
                .allowPublicAccess(true)
                .build()
        )

        .allowCreateProcess(false)
        .allowCreateThread(false)
        .allowHostClassLoading(true)
        .allowHostClassLookup(s -> true)
        .useSystemExit(false)
        .option("engine.WarnInterpreterOnly", "false")

        .allowIO(
            IOAccess.newBuilder()
                .allowHostSocketAccess(false)
                .allowHostFileAccess(true)
                .build()
        );
  }

  public static String jsonStringify(Value value) {
    Context ctx = value.getContext();
    Value bindings = ctx.getBindings(JS_LANGUAGE_ID);
    return bindings.getMember("JSON").invokeMember("stringify", value).asString();
  }
  
  public static JsonElement json(Value value) {
    String jsonString = jsonStringify(value);
    return JsonUtils.gson.fromJson(jsonString, JsonElement.class);
  }

  public static Value jsonParse(String json, Context context) {
    Value bindings = context.getBindings(JS_LANGUAGE_ID);
    return bindings.getMember("JSON").invokeMember("parse", Value.asValue(json));
  }

  public static boolean isInstance(Value value, Class<?> type) {
    if (type == null) {
      return value.isNull();
    }

    if (type == String.class) {
      return value.isString();
    }
    if (type == Number.class) {
      return value.isNumber();
    }
    if (type == Boolean.class) {
      return value.isBoolean();
    }

    if (!value.isHostObject()) {
      return false;
    }
    return type.isInstance(value.asHostObject());
  }

  public static String join(Value[] values) {
    return join(values, 0);
  }

  public static String join(Value[] values, int start) {
    if (values == null || values.length == 0 || start >= values.length) {
      return "";
    }

    StringBuilder builder = new StringBuilder();
    for (Value value : values) {
      builder.append(value.asString());
    }
    return builder.toString();
  }

  public static Logger getLogger(Value... values) {
    if (values == null || values.length < 1) {
      return LOGGER;
    }

    Value value = values[0];
    SourceSection sourceLocation = value.getSourceLocation();

    if (sourceLocation == null) {
      return LOGGER;
    }

    Source source = sourceLocation.getSource();
    return Loggers.getLogger(source.getName());
  }

  public static Vector3f toVec3f(Value value) {
    if (isInstance(value, Vector3f.class)) {
      return value.asHostObject();
    }
    if (isInstance(value, Vector3d.class)) {
      Vector3d d = value.asHostObject();
      return new Vector3f((float) d.x, (float) d.y, (float) d.z);
    }

    if (value.hasMembers()) {
      float x = value.hasMember("x") ? value.getMember("x").asFloat() : 0f;
      float y = value.hasMember("y") ? value.getMember("y").asFloat() : 0f;
      float z = value.hasMember("z") ? value.getMember("z").asFloat() : 0f;
      return new Vector3f(x, y, z);
    }

    throw cantLoad("Vector3f", value);
  }

  public static Vector2f toVec2f(Value value) {
    if (isInstance(value, Vector2f.class)) {
      return value.asHostObject();
    }
    if (isInstance(value, Vector2d.class)) {
      Vector2d d = value.asHostObject();
      return new Vector2f((float) d.x, (float) d.y);
    }

    if (value.hasMembers()) {
      float x = value.hasMember("x") ? value.getMember("x").asFloat() : 0f;
      float y = value.hasMember("y") ? value.getMember("y").asFloat() : 0f;
      return new Vector2f(x, y);
    }

    throw cantLoad("Vector2f", value);
  }

  public static World toWorld(Value scriptValue) {
    World world;
    Object value = scriptValue.as(Object.class);

    if (value instanceof World w) {
      return w;
    } else if (value instanceof NamespacedKey namespacedKey) {
      world = Bukkit.getWorld(namespacedKey);
    } else if (value instanceof Key key) {
      world = Bukkit.getWorld(new NamespacedKey(key.namespace(), key.value()));
    } else if (value instanceof Entity entity) {
      return entity.getWorld();
    } else if (value instanceof Block block) {
      return block.getWorld();
    } else if (value instanceof BlockState state) {
      return state.getWorld();
    } else if (value instanceof CommandSource source) {
      return source.getWorld();
    } else if (value instanceof CharSequence sequence) {
      var v = sequence.toString();
      if (v.equalsIgnoreCase("overworld")) {
        return Worlds.overworld();
      }

      if (v.equalsIgnoreCase("nether")) {
        return Worlds.nether();
      }

      if (v.equalsIgnoreCase("end") || v.equalsIgnoreCase("the_end")) {
        return Worlds.end();
      }

      if (v.contains(":")) {
        NamespacedKey key = NamespacedKey.fromString(v);

        if (key == null) {
          throw typeError("Invalid namespaced key: '" + v + "'");
        }

        world = Bukkit.getWorld(key);
      } else {
        world = Bukkit.getWorld(v);
      }
    } else {
      throw cantLoad("World", value);
    }

    if (world == null) {
      throw typeError("Unknown world: '" + value + "'");
    }

    return world;
  }

  public static Component toText(Value[] arguments, int index, Audience viewer) {
    if (arguments.length <= index) {
      return null;
    }

    return toText(arguments[index], viewer);
  }

  public static Component toText(Value value, Audience viewer) {
    if (value == null || value.isNull()) {
      return Messages.NULL;
    }

    if (value.isHostObject()) {
      Object host = value.asHostObject();

      if (host instanceof Component text) {
        return text;
      }

      throw cantLoad("Component", host);
    }

    if (value.isString()) {
      return Text.valueOf(value.asString(), viewer);
    }

    String json = jsonStringify(value);
    return GsonComponentSerializer.gson().deserialize(json);
  }

  public static ItemStack toItemStack(Value[] arguments, int index) {
    if (arguments.length <= index) {
      return null;
    }

    return toItemStack(arguments[index]);
  }

  public static ItemStack toItemStack(Value value) {
    if (value.isString()) {
      String str = value.toString();

      if (str.startsWith("{")) {
        return ItemStacks.fromNbtString(str);
      } else {
        try {
          return Arguments.ITEMSTACK.parse(new StringReader(str));
        } catch (CommandSyntaxException exc) {
          throw typeError("Invalid item string: '%s': %s", str, exc.getRawMessage().getString());
        }
      }
    }

    if (value.isHostObject()) {
      Object host = value.asHostObject();

      if (host instanceof ItemStack item) {
        return item;
      }
      if (host instanceof CompoundTag tag) {
        return ItemStacks.load(tag);
      }
    }

    throw cantLoad("ItemStack", value);
  }

  public static CommandSource toSource(Value[] args, int index) {
    if (args.length <= index) {
      return null;
    }

    Value value = args[index];
    return toSource(value);
  }

  public static CommandSource toSource(Value value) {
    if (value == null || value.isNull()) {
      return null;
    }

    if (value.isString()) {
      String string = value.asString();

      if (string.equalsIgnoreCase("console") || string.equalsIgnoreCase("server")) {
        return Grenadier.createSource(Bukkit.getConsoleSender());
      }

      UserLookup lookup = Users.getService().getLookup();
      LookupEntry entry = lookup.query(string);

      if (entry == null) {
        throw typeError("Unknown player name: '" + string + "'");
      }

      User player = Users.get(entry);

      if (!player.isOnline()) {
        throw typeError("Player not online: " + player.getName());
      }

      return player.getCommandSource();
    }

    if (!value.isHostObject()) {
      throw cantLoad("CommandSource", value);
    }

    Object o = value.asHostObject();

    if (o instanceof UUID uuid) {
      o = Bukkit.getEntity(uuid);
    }

    if (o instanceof User user) {
      if (!user.isOnline()) {
        throw typeError("User " + user.getName() + " is not online");
      }

      return Grenadier.createSource(user.getPlayer());
    }

    if (o instanceof CommandSender sender) {
      return Grenadier.createSource(sender);
    }

    if (o instanceof CommandSource source) {
      return source;
    }

    throw cantLoad("CommandSource", o);
  }

  public static RuntimeException cantLoad(String typename, Object v) {
    return new RuntimeException("Don't know how to load " + typename + " from: " + v);
  }

  public static RuntimeException typeError(String format, Object... args) {
    return new RuntimeException(format.formatted(args));
  }

  public static void ensureParameterCount(Value[] args, int requiredLength) {
    if (args.length >= requiredLength) {
      return;
    }

    throw typeError("Expected " + requiredLength + " arguments, found " + args.length);
  }

  public static BlockData toBlockData(Value value) {
    if (value.isNull()) {
      return null;
    }

    if (value.isString()) {
      try {
        Result result = ArgumentTypes.block().parse(new StringReader(value.asString()));
        return result.getParsedState();
      } catch (CommandSyntaxException exc) {
        throw typeError(exc.getRawMessage().getString());
      }
    }

    if (value.isHostObject()) {
      Object obj = value.asHostObject();

      if (obj instanceof BlockData data) {
        return data;
      }
      if (obj instanceof Block block) {
        return block.getBlockData();
      }
      if (obj instanceof BlockState state) {
        return state.getBlockData();
      }
      if (obj instanceof Location location) {
        return location.getBlock().getBlockData();
      }
    }

    throw cantLoad("BlockData", value);
  }
}
