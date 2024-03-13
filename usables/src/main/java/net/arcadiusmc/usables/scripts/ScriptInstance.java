package net.arcadiusmc.usables.scripts;

import java.util.Arrays;
import lombok.Getter;
import lombok.Setter;
import net.arcadiusmc.scripts.ExecResult;
import net.arcadiusmc.scripts.ExecResults;
import net.arcadiusmc.scripts.Script;
import net.arcadiusmc.scripts.Scripts;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.text.TextJoiner;
import net.arcadiusmc.usables.Action;
import net.arcadiusmc.usables.Condition;
import net.arcadiusmc.usables.Interaction;
import net.arcadiusmc.usables.ObjectType;
import net.arcadiusmc.usables.UsableComponent;
import net.arcadiusmc.utils.io.source.DirectSource;
import net.arcadiusmc.utils.io.source.Source;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.IdFunctionObject;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Undefined;

@Getter
public class ScriptInstance implements Condition, Action {

  public static final ObjectType<ScriptInstance> FILE_TYPE = new ScriptType(false);
  public static final ObjectType<ScriptInstance> RAW_TYPE = new ScriptType(true);

  private final Source source;
  private final String[] args;

  @Setter
  private String dataString;

  public ScriptInstance(Source source, String... args) {
    this.source = source;
    this.args = args;
  }

  private Script compile(Interaction interaction) {
    Script script = Scripts.newScript(source);
    script.setArguments(args);
    script.compile();

    Context ctx = script.context();

    interaction.getPlayer().ifPresentOrElse(player -> {
      script.putConst("player", player);
    }, () -> {
      script.putConst("player", Undefined.instance);
    });

    script.putConst("holder", interaction.getObject());

    ScriptGlobal global = new ScriptGlobal(this, interaction);
    NativeObject scope = script.getScriptObject();

    global.setParentScope(scope);
    global.activatePrototypeMap(ScriptGlobal.MAX_ID);

    String tag = global.getClassName();

    for (int i = 1; i <= ScriptGlobal.MAX_ID; i++) {
      int arity = switch (i) {
        case ScriptGlobal.ID_getData -> 0;
        case ScriptGlobal.ID_setData -> 1;
        case ScriptGlobal.ID_requireContextValue -> 1;
        case ScriptGlobal.ID_hasContextValue -> 1;
        case ScriptGlobal.ID_getContextValue -> 1;
        case ScriptGlobal.ID_setContextValue -> 2;
        default -> 0;
      };

      String funcName = global.getInstanceIdName(i);
      IdFunctionObject func = new IdFunctionObject(global, tag, i, funcName, arity, scope);

      func.exportAsScopeProperty();
    }

    ctx.close();

    return script;
  }

  @Override
  public void onUse(Interaction interaction) {
    try (Script script = compile(interaction)) {
      script.evaluate().logError();
    }
  }

  @Override
  public boolean test(Interaction interaction) {
    try (Script script = compile(interaction)) {
      ExecResult<Object> result = script.evaluate().logError();

      if (!result.isSuccess()) {
        return false;
      }

      return ExecResults.toBoolean(result).result().orElse(false);
    }
  }

  @Override
  public Component failMessage(Interaction interaction) {
    try (Script script = compile(interaction)) {
      var result = script.evaluate()
          .flatMapScript(s -> {
            if (!s.hasMethod("getFailMessage")) {
              return null;
            }

            return s.invoke("getFailMessage");
          })
          .map(o -> Text.valueOf(o, interaction.getPlayer().orElse(null)))
          .logError();

      if (!result.isSuccess()) {
        return null;
      }

      return result.result().orElse(null);
    }
  }

  @Override
  public void afterTests(Interaction interaction) {
    try (Script script = compile(interaction)) {
      script.evaluate()
          .flatMapScript(s -> {
            if (!s.hasMethod("onTestsPassed")) {
              return null;
            }

            return s.invoke("onTestsPassed");
          })
          .logError();
    }
  }

  @Override
  public @Nullable Component displayInfo() {
    var builder = Component.text()
        .append(Component.text(source.name()));

    if (args.length > 0) {
      builder.append(Component.text(", args="))
          .append(
              TextJoiner.onComma()
                  .add(Arrays.stream(args).map(Component::text))
                  .setPrefix(Component.text("["))
                  .setSuffix(Component.text("]"))
                  .asComponent()
          );
    }

    return builder.build();
  }

  @Override
  public ObjectType<? extends UsableComponent> getType() {
    return source instanceof DirectSource ? RAW_TYPE : FILE_TYPE;
  }

}
