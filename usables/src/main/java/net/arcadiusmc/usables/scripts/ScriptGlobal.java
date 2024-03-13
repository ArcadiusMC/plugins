package net.arcadiusmc.usables.scripts;

import java.util.Optional;
import net.arcadiusmc.usables.Interaction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.IdFunctionObject;
import org.mozilla.javascript.IdScriptableObject;
import org.mozilla.javascript.NativeJSON;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.json.JsonParser;
import org.mozilla.javascript.json.JsonParser.ParseException;

class ScriptGlobal extends IdScriptableObject {

  static final int ID_getData = 1;
  static final int ID_setData = 2;
  static final int ID_requireContextValue = 3;
  static final int ID_hasContextValue = 4;
  static final int ID_getContextValue = 5;
  static final int ID_setContextValue = 6;
  static final int ID_listContextValues = 7;

  static final int MAX_ID = ID_listContextValues;

  static final String NAME_getData = "getData";
  static final String NAME_setData = "setData";
  static final String NAME_requireContextValue = "requireContextValue";
  static final String NAME_hasContextValue = "hasContextValue";
  static final String NAME_getContextValue = "getContextValue";
  static final String NAME_setContextValue = "setContextValue";
  static final String Name_listContextValues = "listContextValues";

  private final ScriptInstance instance;
  private final Interaction interaction;

  public ScriptGlobal(ScriptInstance instance, Interaction interaction) {
    this.instance = instance;
    this.interaction = interaction;
  }

  @Override
  protected int getMaxInstanceId() {
    return MAX_ID;
  }

  @Override
  public String getClassName() {
    return "UsableScriptGlobal";
  }

  @Override
  protected void initPrototypeId(int id) {
    int arity = switch (id) {
      case ID_getData -> 0;
      case ID_setData -> 1;
      case ID_requireContextValue -> 1;
      case ID_hasContextValue -> 1;
      case ID_getContextValue -> 1;
      case ID_setContextValue -> 2;
      case ID_listContextValues -> 0;
      default -> 0;
    };

    String name = getInstanceIdName(id);
    initPrototypeMethod(getClassName(), id, name, arity);
  }

  @Override
  protected int findPrototypeId(String name) {
    return switch (name) {
      case NAME_getData -> ID_getData;
      case NAME_setData -> ID_setData;
      case NAME_requireContextValue -> ID_requireContextValue;
      case NAME_hasContextValue -> ID_hasContextValue;
      case NAME_getContextValue -> ID_getContextValue;
      case NAME_setContextValue -> ID_setContextValue;
      case Name_listContextValues -> ID_listContextValues;

      default -> throw new IllegalStateException(name);
    };
  }

  @Override
  protected String getInstanceIdName(int id) {
    return switch (id) {
      case ID_getData -> NAME_getData;
      case ID_setData -> NAME_setData;
      case ID_requireContextValue -> NAME_requireContextValue;
      case ID_hasContextValue -> NAME_hasContextValue;
      case ID_getContextValue -> NAME_getContextValue;
      case ID_setContextValue -> NAME_setContextValue;
      case ID_listContextValues -> Name_listContextValues;

      default -> throw new IllegalStateException(String.valueOf(id));
    };
  }

  Optional<Object> getContextValue(String name) {
    return interaction.getValue(name);
  }

  @Override
  public Object execIdCall(
      IdFunctionObject f,
      Context cx,
      Scriptable scope,
      Scriptable thisObj,
      Object[] args
  ) {
    switch (f.methodId()) {
      case ID_getData -> {
        String jsonData = instance.getDataString();

        if (jsonData == null || jsonData.isEmpty()) {
          return Undefined.instance;
        }

        try {
          return new JsonParser(cx, scope).parseValue(jsonData);
        } catch (ParseException exc) {
          throw ScriptRuntime.constructError("SyntaxError", exc.getMessage());
        }
      }

      case ID_setData -> {
        Object jsonData;

        if (args == null || args.length < 1 || args[0] == null) {
          instance.setDataString(null);
          return Undefined.instance;
        } else if (args.length == 1) {
          jsonData = args[0];
        } else {
          jsonData = args;
        }

        String stringified = String.valueOf(NativeJSON.stringify(cx, scope, jsonData, null, null));
        instance.setDataString(stringified);

        return Undefined.instance;
      }

      case ID_requireContextValue -> {
        String name = ScriptRuntime.toString(args, 0);
        Optional<Object> valueOpt = getContextValue(name);

        if (valueOpt.isEmpty()) {
          throw ScriptRuntime.typeError("Missing context value '" + name + "'");
        }

        return valueOpt.get();
      }

      case ID_hasContextValue -> {
        String name = ScriptRuntime.toString(args, 0);
        Optional<Object> valueOpt = getContextValue(name);
        return valueOpt.isPresent();
      }

      case ID_getContextValue -> {
        String name = ScriptRuntime.toString(args, 0);
        Optional<Object> valueOpt = getContextValue(name);
        return valueOpt.orElse(Undefined.instance);
      }

      case ID_setContextValue -> {
        if (args.length != 2) {
          throw ScriptRuntime.typeError("2 Arguments required, name and value");
        }

        String name = ScriptRuntime.toString(args, 0);
        Object value = args[1];

        interaction.getContext().put(name, value);
        return Undefined.instance;
      }

      case ID_listContextValues -> {
        Scriptable arr = cx.newArray(scope, interaction.getContext().size());

        int i = 0;
        for (String s : interaction.getContext().keySet()) {
          ScriptableObject.putProperty(arr, i++, s);
        }

        return arr;
      }

      default -> throw f.unknown();
    }
  }
}
