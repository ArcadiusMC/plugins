package net.arcadiusmc.scripts;

import net.arcadiusmc.ArcadiusServer;
import net.arcadiusmc.scripts.builtins.ExecCommandFunction;
import net.arcadiusmc.scripts.builtins.ItemFunction;
import net.arcadiusmc.scripts.builtins.LerpFunction;
import net.arcadiusmc.scripts.builtins.PlaySoundFunction;
import net.arcadiusmc.scripts.builtins.RenderPlaceholdersFunction;
import net.arcadiusmc.scripts.builtins.ScriptCommandStorage;
import net.arcadiusmc.scripts.builtins.SendMessageFunction;
import net.arcadiusmc.scripts.builtins.TextFunction;
import net.arcadiusmc.scripts.builtins.TimeFunction;
import net.arcadiusmc.scripts.builtins.VectorFunctions;
import net.arcadiusmc.scripts.modules.ParticlesObject;
import net.arcadiusmc.scripts.modules.ScoreboardObject;
import net.arcadiusmc.scripts.modules.WorldsObject;
import org.bukkit.Bukkit;
import org.graalvm.polyglot.Value;

public final class ScriptRuntime {
  private ScriptRuntime() {}

  public static final ScriptCommandStorage STORAGE = new ScriptCommandStorage();

  public static final ParticlesObject PARTICLES = new ParticlesObject();
  public static final WorldsObject WORLDS = new WorldsObject();
  public static final ScoreboardObject SCORES = new ScoreboardObject();

  public static void initiateScope(Value scope) {
    scope.putMember("command", ExecCommandFunction.CONSOLE);
    scope.putMember("runAs", ExecCommandFunction.RUN_AS);
    scope.putMember("silentRunAs", ExecCommandFunction.RUN_AS_SILENT);
    scope.putMember("silentCommand", ExecCommandFunction.CONSOLE_SILENT);

    scope.putMember("text", TextFunction.TEXT);
    scope.putMember("sendMessage", SendMessageFunction.MESSAGE);
    scope.putMember("sendActionBar", SendMessageFunction.ACTIONBAR);
    scope.putMember("renderPlaceholders", RenderPlaceholdersFunction.RENDER);
    scope.putMember("lerp", LerpFunction.LERP);

    scope.putMember("playSound", PlaySoundFunction.PLAY_SOUND);

    scope.putMember("giveItem", ItemFunction.GIVE);
    scope.putMember("removeItem", ItemFunction.TAKE);

    scope.putMember("currentTimeMillis", TimeFunction.MILLIS);
    scope.putMember("currentTimeSeconds", TimeFunction.SECONDS);

    scope.putMember("server", Bukkit.getServer());
    scope.putMember("arcServer", ArcadiusServer.server());

    scope.putMember("vec2", VectorFunctions.VEC2);
    scope.putMember("vec3", VectorFunctions.VEC3);

    scope.putMember("particles", PARTICLES);
    scope.putMember("serverStorage", STORAGE);
    scope.putMember("worlds", WORLDS);
    scope.putMember("scoreboard", SCORES);
  }
}
