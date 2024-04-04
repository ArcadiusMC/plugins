package net.arcadiusmc.scripts.modules;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.arcadiusmc.scripts.RhinoScript;
import net.arcadiusmc.scripts.Script;
import net.arcadiusmc.scripts.module.JsModule;
import net.arcadiusmc.utils.io.PathUtil;
import net.arcadiusmc.utils.io.source.PathSource;
import net.arcadiusmc.utils.io.source.Source;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.IdFunctionObject;
import org.mozilla.javascript.IdScriptableObject;
import org.mozilla.javascript.NativeJSON;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.json.JsonParser;
import org.mozilla.javascript.json.JsonParser.ParseException;

public class IoModule extends IdScriptableObject {

  static final int ID_getPath = 1;
  static final int ID_readText = 2;
  static final int ID_readLines = 3;
  static final int ID_readJson = 4;
  static final int ID_writeFile = 5;
  static final int ID_writeJson = 6;
  static final int ID_fileExists = 7;
  static final int ID_deleteFile = 8;

  static final String NAME_getPath = "getPath";
  static final String NAME_readText = "readText";
  static final String NAME_readLines = "readLines";
  static final String NAME_readJson = "readJson";
  static final String NAME_writeFile = "writeFile";
  static final String NAME_writeJson = "writeJson";
  static final String NAME_fileExists = "fileExists";
  static final String NAME_deleteFile = "deleteFile";

  static final int MAX_ID = ID_writeJson;

  public static final JsModule MODULE = scope -> {
    IoModule module = new IoModule();
    module.setParentScope(scope);
    module.activatePrototypeMap(MAX_ID);
    return module;
  };

  @Override
  protected int getMaxInstanceId() {
    return MAX_ID;
  }

  @Override
  protected void initPrototypeId(int id) {
    String name = getInstanceIdName(id);
    int arity = switch (id) {
      case ID_getPath, ID_readLines, ID_readText -> 1;
      case ID_writeFile, ID_writeJson -> 2;
      default -> 1;
    };

    initPrototypeMethod(getClassName(), id, name, arity);
  }

  @Override
  protected String getInstanceIdName(int id) {
    return switch (id) {
      case ID_getPath -> NAME_getPath;
      case ID_readText -> NAME_readText;
      case ID_readLines -> NAME_readLines;
      case ID_readJson -> NAME_readJson;
      case ID_writeFile -> NAME_writeFile;
      case ID_writeJson -> NAME_writeJson;
      case ID_fileExists -> NAME_fileExists;
      case ID_deleteFile -> NAME_deleteFile;
      default -> super.getInstanceIdName(id);
    };
  }

  @Override
  protected int findPrototypeId(String name) {
    return switch (name) {
      case NAME_getPath -> ID_getPath;
      case NAME_readText -> ID_readText;
      case NAME_readLines -> ID_readLines;
      case NAME_readJson -> ID_readJson;
      case NAME_writeFile -> ID_writeFile;
      case NAME_writeJson -> ID_writeJson;
      case NAME_fileExists -> ID_fileExists;
      case NAME_deleteFile -> ID_deleteFile;
      default -> 0;
    };
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
      case ID_getPath -> {
        return js_getPath(args);
      }

      case ID_deleteFile -> {
        Path path = js_getPath(args);

        if (Files.exists(path)) {
          PathUtil.safeDelete(path);
        }
      }

      case ID_fileExists -> {
        Path path = js_getPath(args);
        return Files.exists(path);
      }

      case ID_readText -> {
        Path path = js_getPath(args);
        ensureExists(path);

        try  {
          return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException exc) {
          throw ScriptRuntime.typeError("IO Error reading file: " + exc.getMessage());
        }
      }

      case ID_readLines -> {
        Path path = js_getPath(args);
        ensureExists(path);

        try {
          List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
          Scriptable arr = cx.newArray(scope, lines.size());

          for (int i = 0; i < lines.size(); i++) {
            putProperty(arr, i, lines.get(i));
          }

          return arr;
        } catch (IOException e) {
          throw ScriptRuntime.typeError("IO Error reading file: " + e.getMessage());
        }
      }

      case ID_readJson -> {
        Path path = js_getPath(args);
        ensureExists(path);

        try  {
          String str = Files.readString(path, StandardCharsets.UTF_8);
          return new JsonParser(cx, scope).parseValue(str);
        } catch (IOException exc) {
          throw ScriptRuntime.typeError("IO Error reading file: " + exc.getMessage());
        } catch (ParseException e) {
          throw ScriptRuntime.typeError("Error reading JSON: " + e.getMessage());
        }
      }

      case ID_writeFile -> {
        if (args.length < 2) {
          throw ScriptRuntime.typeError("Not enough arguments, expected path and file contents");
        }

        Path path = js_getPath(args[0]);

        try {
          PathUtil.ensureParentExists(path);
          BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8);

          for (int i = 1; i < args.length; i++) {
            String string = ScriptRuntime.toString(args, i);
            writer.write(string);
            writer.newLine();
          }

          writer.close();
        } catch (IOException e) {
          throw ScriptRuntime.typeError("IO Error writing to file: " + e.getMessage());
        }
      }

      case ID_writeJson -> {
        if (args.length < 2) {
          throw ScriptRuntime.typeError("Not enough arguments, expected path and file contents");
        }

        Path path = js_getPath(args[0]);
        String jsonString = String.valueOf(NativeJSON.stringify(cx, scope, args[1], null, 2));

        try {
          PathUtil.ensureParentExists(path);
          Files.writeString(path, jsonString, StandardCharsets.UTF_8);
        } catch (IOException e) {
          throw ScriptRuntime.typeError("IO Error writing to file: " + e.getMessage());
        }
      }

      default -> throw f.unknown();
    }

    return Context.getUndefinedValue();
  }

  private static void ensureExists(Path path) {
    if (Files.exists(path)) {
      return;
    }

    throw ScriptRuntime.typeError("File " + path + " doesn't exist");
  }

  private Path js_getPath(Object... args) {
    if (args.length == 0) {
      return Path.of("");
    }

    StringBuilder pathBuilder = new StringBuilder();
    for (int i = 0; i < args.length; i++) {
      if (i != 0) {
        pathBuilder.append("/");
      }

      pathBuilder.append(ScriptRuntime.toString(args, i));
    }


    String path = pathBuilder.toString();
    Script script = RhinoScript.fromScope(this.getParentScope());
    Path parentPath;

    if (path.startsWith("./")) {
      Source source = script.getSource();
      path = path.substring(2);

      if (!(source instanceof PathSource fileSource)) {
        parentPath = script.getService().getScriptsDirectory();
      } else {
        parentPath = fileSource.path().getParent();
      }
    } else {
      parentPath = Path.of("");
    }

    return parentPath.resolve(Path.of(path)).normalize();
  }

  @Override
  public String getClassName() {
    return "IoModule";
  }
}
