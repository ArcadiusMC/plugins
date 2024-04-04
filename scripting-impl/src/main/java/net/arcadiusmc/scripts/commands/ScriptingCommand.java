package net.arcadiusmc.scripts.commands;

import com.google.common.base.Strings;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Map;
import java.util.Objects;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.command.Exceptions;
import net.arcadiusmc.command.arguments.RegistryArguments;
import net.arcadiusmc.registry.Holder;
import net.arcadiusmc.scripts.CachingScriptLoader;
import net.arcadiusmc.scripts.ExecResult;
import net.arcadiusmc.scripts.Script;
import net.arcadiusmc.scripts.ScriptLoadException;
import net.arcadiusmc.scripts.ScriptService;
import net.arcadiusmc.scripts.ScriptingPlugin;
import net.arcadiusmc.scripts.Scripts;
import net.arcadiusmc.scripts.pack.ScriptPack;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.utils.io.source.Source;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.annotations.Argument;
import net.forthecrown.grenadier.annotations.CommandData;
import net.forthecrown.grenadier.annotations.VariableInitializer;
import net.forthecrown.grenadier.types.options.ArgumentOption;
import net.forthecrown.grenadier.types.options.FlagOption;
import net.forthecrown.grenadier.types.options.Options;
import net.forthecrown.grenadier.types.options.OptionsArgument;
import net.forthecrown.grenadier.types.options.ParsedOptions;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;

@CommandData("file = scripts.gcn")
public class ScriptingCommand {

  public static final FlagOption KEEP_OPEN = Options.flag("keep-open");

  public static final ArgumentOption<String[]> SCRIPT_ARGS
      = Options.argument(ScriptArgsArgument.SCRIPT_ARGS)
      .setLabel("args")
      .setDefaultValue(new String[0])
      .build();

  public static final ArgumentOption<String> METHOD
      = Options.argument(StringArgumentType.string())
      .setLabel("method")
      .build();

  public static final OptionsArgument OPTIONS = OptionsArgument.builder()
      .addFlag(KEEP_OPEN)
      .addOptional(SCRIPT_ARGS)
      .addOptional(METHOD)
      .build();

  private final ScriptingPlugin plugin;

  public ScriptingCommand(ScriptingPlugin plugin) {
    this.plugin = plugin;
  }

  @VariableInitializer
  void initVars(Map<String, Object> vars) {
    vars.put("script_argument", ScriptArgument.SCRIPT);
    vars.put("run_options", OPTIONS);

    var packs = plugin.getPacks();
    vars.put("active_script", new RegistryArguments<>(packs.getPacks(), "Loaded Script"));
  }

  void configReload(CommandSource source) {
    plugin.reloadConfig();
    source.sendSuccess(Messages.renderText("scripts.reloaded.config", source));
  }

  void scriptsReload(CommandSource source) {
    plugin.getPacks().reload();
    source.sendSuccess(Messages.renderText("scripts.reloaded.packs", source));
  }

  void listActive(CommandSource source) {

  }

  void reloadActive(CommandSource source) {

  }

  void closeActive(CommandSource source, @Argument("active") Holder<ScriptPack> holder) {
    holder.getRegistry().remove(holder.getKey());
    holder.getValue().close();

    source.sendSuccess(
        Messages.render("scripts.closed")
            .addValue("pack", holder.getKey())
            .create(source)
    );
  }

  void runScript(
      CommandSource source,
      @Argument("script_name") Source script,
      @Argument(value = "options", optional = true) ParsedOptions options
  ) throws CommandSyntaxException {
    options = Objects.requireNonNullElse(options, ParsedOptions.EMPTY);
    options.checkAccess(source);

    boolean keepOpen = options.has(KEEP_OPEN);
    String[] args = options.getValue(SCRIPT_ARGS);
    String method = options.getValue(METHOD);

    executeScript(source, script, keepOpen, method, args);
  }

  static void executeScript(
      CommandSource source,
      Source scriptSource,
      boolean keepOpen,
      String method,
      String... args
  ) throws CommandSyntaxException {
    ScriptService service = Scripts.getService();
    CachingScriptLoader loader = service.getGlobalLoader();

    Script script = service.newScript(loader, scriptSource);
    script.setArguments(args);

    try {
      script.compile();
    } catch (ScriptLoadException exc) {
      throw Messages.render("scripts.loadFail")
          .addValue("error", exc.getMessage())
          .exception(source);
    }

    script.put("source", source);

    ExecResult<Object> result = script.evaluate().logError();

    if (result.error().isPresent()) {
      script.close();
      throw Exceptions.create(result.error().get());
    }

    if (!Strings.isNullOrEmpty(method)) {
      result = script.invoke(method);

      if (result.error().isPresent()) {
        result.logError();
        script.close();

        throw Exceptions.create(result.error().get());
      }
    }

    result.result().ifPresentOrElse(o -> {
      Context context = script.context();

      source.sendSuccess(
          Messages.render("scripts.executed.regular")
              .addValue("result", toText(o, source))
              .create(source)
      );

      context.close();
    }, () -> {
      source.sendSuccess(Messages.renderText("scripts.executed.noResult", source));
    });

    if (!keepOpen) {
      script.close();
      loader.remove(scriptSource);
    }
  }

  static Component toText(Object o, Audience viewer) {
    try {
      o = Context.jsToJava(o, Object.class);

      if (o instanceof Scriptable object) {
        var obj = ScriptRuntime.toString(object);
        return Text.valueOf(obj, viewer);
      }
    } catch (RuntimeException exc) {
      Loggers.getLogger().error("Error getting string from script object", exc);
      return Messages.renderText("scripts.conversionFail", viewer);
    }

    return Text.valueOf(o, viewer);
  }
}