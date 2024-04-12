 package net.arcadiusmc.scripts.builtin;

 import net.arcadiusmc.scripts.NonCtorFunction;
 import net.arcadiusmc.scripts.RhinoScript;
 import net.arcadiusmc.scripts.Script;
 import net.arcadiusmc.scripts.ScriptUtils;
 import net.forthecrown.grenadier.CommandSource;
 import net.forthecrown.grenadier.Grenadier;
 import org.bukkit.Bukkit;
 import org.mozilla.javascript.Context;
 import org.mozilla.javascript.ScriptRuntime;
 import org.mozilla.javascript.Scriptable;
 import org.mozilla.javascript.Undefined;

 public class ConsoleCommandFunction extends NonCtorFunction {

  private final boolean silent;
  private final boolean provideSender;

  public ConsoleCommandFunction(boolean silent, boolean provideSender) {
    this.silent = silent;
    this.provideSender = provideSender;
  }

  @Override
  public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
    String command;
    CommandSource source;

    if (provideSender) {
      if (args.length < 2) {
        throw ScriptRuntime.typeError("Expected at least 2 arguments: sender and command");
      }

      command = ScriptUtils.concatArgs(args, 1);
      source = ScriptUtils.toSource(args, 0);

      assert source != null;
    } else {
      if (args.length < 1) {
        throw ScriptRuntime.typeError("Expected at least 1 argument: command");
      }

      command = ScriptUtils.concatArgs(args, 0);
      source = Grenadier.createSource(Bukkit.getConsoleSender());
    }

     if (silent) {
      source = source.silent();
    }

    try {
      return Grenadier.dispatch(source, command);
    } catch (Throwable t) {
      Script script = RhinoScript.fromScope(scope);

      if (script != null) {
        script.getLogger().error("Error executing command '{}'", command, t);
      }

      return Undefined.instance;
    }
  }
}
