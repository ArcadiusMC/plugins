package net.arcadiusmc.scripts.builtin;

import static org.mozilla.javascript.ScriptableObject.putConstProperty;

import net.arcadiusmc.ArcadiusServer;
import org.bukkit.Bukkit;
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;

public class PluginScriptRuntime {

  static final Function EXEC_CONSOLE = new ConsoleCommandFunction(false, false);
  static final Function EXEC_CONSOLE_SILENT = new ConsoleCommandFunction(true, false);
  static final Function EXEC_AS = new ConsoleCommandFunction(false, true);
  static final Function EXEC_AS_SILENT = new ConsoleCommandFunction(true, true);

  static final Function GIVE_ITEM = new GiveItemFunction();
  static final Function LOAD_ITEM = new LoadItemFunction();

  static final Function RENDER_PLACEHOLDERS = new RenderPlaceholdersFunction();
  static final Function SEND_MESSAGE = new SendMessageFunction(false);
  static final Function SEND_ACTION_BAR = new SendMessageFunction(true);
  static final Function PLAY_SOUND = new PlaySoundFunction();

  static final Callable TIME_MILLIS = (cx, scope, thisObj, args) -> System.currentTimeMillis();
  static final Callable TIME_SECONDS = (cx, scope, thisObj, args) -> {
    double timeMillis = System.currentTimeMillis();
    return timeMillis / 1000.0d;
  };

  static final Function TEXT = new TextFunction();

  public static void initStandardObjects(Scriptable object) {
    putConstProperty(object, "command", EXEC_CONSOLE);
    putConstProperty(object, "silentCommand", EXEC_CONSOLE_SILENT);
    putConstProperty(object, "runAs", EXEC_AS);
    putConstProperty(object, "silentRunAs", EXEC_AS_SILENT);

    putConstProperty(object, "giveItem", GIVE_ITEM);
    putConstProperty(object, "loadItem", LOAD_ITEM);

    putConstProperty(object, "currentTimeMillis", TIME_MILLIS);
    putConstProperty(object, "currentTimeSeconds", TIME_SECONDS);

    putConstProperty(object, "sendMessage", SEND_MESSAGE);
    putConstProperty(object, "sendActionBar", SEND_ACTION_BAR);
    putConstProperty(object, "renderPlaceholders", RENDER_PLACEHOLDERS);
    putConstProperty(object, "playSound", PLAY_SOUND);
    putConstProperty(object, "text", TEXT);

    putConstProperty(object, "arcServer", ArcadiusServer.server());
    putConstProperty(object, "server", Bukkit.getServer());

    NativeVectors.init(object);

    JsCommandStorage storage = new JsCommandStorage();
    storage.setParentScope(object);
    putConstProperty(object, "serverStorage", storage);
  }
}
