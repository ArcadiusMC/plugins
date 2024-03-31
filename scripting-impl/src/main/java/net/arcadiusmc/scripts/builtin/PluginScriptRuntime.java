package net.arcadiusmc.scripts.builtin;

import static org.mozilla.javascript.ScriptableObject.putConstProperty;

import net.arcadiusmc.ArcadiusServer;
import net.arcadiusmc.scripts.ScriptUtils;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.Grenadier;
import org.bukkit.Bukkit;
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;

public class PluginScriptRuntime {

  static final Callable EXEC_CONSOLE = (cx, scope, thisObj, args) -> {
    return runString(Grenadier.createSource(Bukkit.getConsoleSender()), args,0);
  };

  static final Callable EXEC_AS = (cx, scope, thisObj, args) -> {
    if (args.length == 0) {
      return null;
    }

    CommandSource source = ScriptUtils.toSource(args, 0);

    if (source == null) {
      source = Grenadier.createSource(Bukkit.getConsoleSender());
    }

    return runString(source, args, 1);
  };

  static final Callable GIVE_ITEM = new GiveItemFunction();

  static final Callable RENDER_PLACEHOLDERS = new RenderPlaceholdersFunction();
  static final Callable SEND_MESSAGE = new SendMessageFunction(false);
  static final Callable SEND_ACTION_BAR = new SendMessageFunction(true);
  static final Callable PLAY_SOUND = new PlaySoundFunction();

  static final Callable TIME_MILLIS = (cx, scope, thisObj, args) -> System.currentTimeMillis();
  static final Callable TIME_SECONDS = (cx, scope, thisObj, args) -> {
    double timeMillis = System.currentTimeMillis();
    return timeMillis / 1000.0d;
  };

  static final Function TEXT = new TextFunction();

  public static void initStandardObjects(Scriptable object) {
    putConstProperty(object, "command", EXEC_CONSOLE);
    putConstProperty(object, "runAs", EXEC_AS);

    putConstProperty(object, "giveItem", GIVE_ITEM);

    putConstProperty(object, "currentTimeMillis", TIME_MILLIS);
    putConstProperty(object, "currentTimeSeconds", TIME_SECONDS);

    putConstProperty(object, "sendMessage", SEND_MESSAGE);
    putConstProperty(object, "sendActionBar", SEND_ACTION_BAR);
    putConstProperty(object, "renderPlaceholders", RENDER_PLACEHOLDERS);
    putConstProperty(object, "playSound", PLAY_SOUND);
    putConstProperty(object, "text", TEXT);

    putConstProperty(
        object,
        "arcServer",
        Context.javaToJS(ArcadiusServer.server(), object)
    );

    putConstProperty(
        object,
        "server",
        Context.javaToJS(Bukkit.getServer(), object)
    );

    NativeVectors.init(object);

    JsCommandStorage storage = new JsCommandStorage();
    storage.setParentScope(object);
    putConstProperty(object, "serverStorage", storage);
  }

  static Object runString(CommandSource sender, Object[] args, int argsStart) {
    String command = ScriptUtils.concatArgs(args, argsStart);

    try {
      return Grenadier.dispatch(sender, command);
    } catch (Throwable t) {
      t.printStackTrace();
      return null;
    }
  }
}
