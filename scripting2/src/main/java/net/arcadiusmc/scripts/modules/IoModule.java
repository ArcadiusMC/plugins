package net.arcadiusmc.scripts.modules;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.arcadiusmc.scripts.IdProxyObject;
import net.arcadiusmc.scripts.Scripts;
import net.arcadiusmc.utils.io.PathUtil;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.io.FileSystem;

public class IoModule extends IdProxyObject {

  static final int ID_getPath = 1;
  static final int ID_readText = 2;
  static final int ID_readLines = 3;
  static final int ID_readJson = 4;
  static final int ID_writeFile = 5;
  static final int ID_writeJson = 6;
  static final int ID_fileExists = 7;
  static final int ID_deleteFile = 8;
  static final int ID_copyFile = 9;
  static final int ID_moveFile = 10;

  static final String NAME_getPath = "getPath";
  static final String NAME_readText = "readText";
  static final String NAME_readLines = "readLines";
  static final String NAME_readJson = "readJson";
  static final String NAME_writeFile = "writeFile";
  static final String NAME_writeJson = "writeJson";
  static final String NAME_fileExists = "fileExists";
  static final String NAME_deleteFile = "deleteFile";
  static final String NAME_copyFile = "copyFile";
  static final String NAME_moveFile = "moveFile";

  static final int MAX_ID = ID_deleteFile;

  private final FileSystem fileSystem;

  public IoModule(FileSystem fileSystem) {
    this.fileSystem = fileSystem;
    initMembers(MAX_ID);
  }

  @Override
  protected String getName(int id) {
    return switch (id) {
      case ID_getPath -> NAME_getPath;
      case ID_readText -> NAME_readText;
      case ID_readLines -> NAME_readLines;
      case ID_readJson -> NAME_readJson;
      case ID_writeFile -> NAME_writeFile;
      case ID_writeJson -> NAME_writeJson;
      case ID_fileExists -> NAME_fileExists;
      case ID_deleteFile -> NAME_deleteFile;
      case ID_copyFile -> NAME_copyFile;
      case ID_moveFile -> NAME_moveFile;
      default -> throw new IllegalStateException();
    };
  }

  @Override
  protected int getId(String name) {
    return switch (name) {
      case NAME_getPath -> ID_getPath;
      case NAME_readText -> ID_readText;
      case NAME_readLines -> ID_readLines;
      case NAME_readJson -> ID_readJson;
      case NAME_writeFile -> ID_writeFile;
      case NAME_writeJson -> ID_writeJson;
      case NAME_fileExists -> ID_fileExists;
      case NAME_deleteFile -> ID_deleteFile;
      case NAME_copyFile -> ID_copyFile;
      case NAME_moveFile -> ID_moveFile;
      default -> UNKNOWN_ID;
    };
  }

  @Override
  public Object invoke(Invocation f, Value... args) {
    switch (f.methodId()) {
      case ID_getPath -> {
        return parsePath(args);
      }

      case ID_deleteFile -> {
        Path path = parsePath(args);

        if (Files.exists(path)) {
          return PathUtil.safeDelete(path).result().orElse(0);
        }

        return 0;
      }

      case ID_fileExists -> {
        Path path = parsePath(args);
        return Files.exists(path);
      }

      case ID_readText -> {
        Path path = parsePath(args);
        ensureExists(path);

        try  {
          return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException exc) {
          throw Scripts.typeError("IO Error reading file: " + exc.getMessage());
        }
      }

      case ID_readLines -> {
        Path path = parsePath(args[0]);
        ensureExists(path);

        Charset encoding;

        if (args.length > 1) {
          encoding = switch (args[1].asString().toLowerCase()) {
            case "ascii", "us_ascii" -> StandardCharsets.US_ASCII;
            case "iso_8859_1" -> StandardCharsets.ISO_8859_1;
            case "utf_16le" -> StandardCharsets.UTF_16LE;
            case "utf_16be" -> StandardCharsets.UTF_16BE;
            case "utf_16" -> StandardCharsets.UTF_16;
            default -> StandardCharsets.UTF_8;
          };
        } else {
          encoding = StandardCharsets.UTF_8;
        }

        try {
          List<String> lines = Files.readAllLines(path, encoding);
          return lines;
        } catch (IOException e) {
          throw Scripts.typeError("IO Error reading file: " + e.getMessage());
        }
      }

      case ID_readJson -> {
        Path path = parsePath(args);
        ensureExists(path);

        try  {
          String str = Files.readString(path, StandardCharsets.UTF_8);
          return Scripts.jsonParse(str, Context.getCurrent());
        } catch (IOException exc) {
          throw Scripts.typeError("IO Error reading file: " + exc.getMessage());
        }
      }

      case ID_writeFile -> {
        if (args.length < 2) {
          throw Scripts.typeError("Not enough arguments, expected path and file contents");
        }

        Path path = parsePath(args[0]);

        try {
          PathUtil.ensureParentExists(path);
          BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8);

          int len = args.length;

          if (len > 2) {
            for (int i = 1; i < args.length; i++) {
              String string = args[i].asString();
              writer.write(string);
              writer.newLine();
            }
          } else {
            writer.write(args[1].asString());
          }

          writer.close();
        } catch (IOException e) {
          throw Scripts.typeError("IO Error writing to file: " + e.getMessage());
        }
      }


      case ID_writeJson -> {
        if (args.length < 2) {
          throw Scripts.typeError("Not enough arguments, expected path and file contents");
        }

        Path path = parsePath(args[0]);
        String jsonString = Scripts.jsonStringify(args[1]);

        try {
          PathUtil.ensureParentExists(path);
          Files.writeString(path, jsonString, StandardCharsets.UTF_8);
        } catch (IOException e) {
          throw Scripts.typeError("IO Error writing to file: " + e.getMessage());
        }
      }

      case ID_copyFile, ID_moveFile -> {
        if (args.length < 2) {
          throw Scripts.typeError(
              "Not enough arguments, expected source path and destination path"
          );
        }

        Path sourcePath = parsePath(args[0]);
        Path targetPath = parsePath(args[1]);

        try {
          if (f.methodId() == ID_copyFile) {
            PathUtil.copy(sourcePath, targetPath);
          } else {
            PathUtil.move(sourcePath, targetPath);
          }
        } catch (IOException exc) {
          throw Scripts.typeError("IO error copying/moving files: " + exc.getMessage());
        }
      }

      default -> throw f.unknown();
    }

    return null;
  }


  private static void ensureExists(Path path) {
    if (Files.exists(path)) {
      return;
    }

    throw Scripts.typeError("File " + path + " doesn't exist");
  }

  private Path parsePath(Value... args) {
    if (args.length == 0) {
      return Path.of("");
    }
    if (args.length == 1 && Scripts.isInstance(args[0], Path.class)) {
      return args[0].asHostObject();
    }

    StringBuilder pathBuilder = new StringBuilder();
    for (int i = 0; i < args.length; i++) {
      if (i != 0) {
        pathBuilder.append("/");
      }

      pathBuilder.append(args[i].asString());
    }

    String path = pathBuilder.toString();
    Path parsed = fileSystem.parsePath(path);

    return fileSystem.toAbsolutePath(parsed);
  }
}
