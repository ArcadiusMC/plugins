package net.arcadiusmc.ui.resource;

import static net.arcadiusmc.ui.math.Screen.DEFAULT_HEIGHT;
import static net.arcadiusmc.ui.math.Screen.DEFAULT_WIDTH;

import com.mojang.serialization.DataResult;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.Getter;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.ui.UiPlugin;
import net.arcadiusmc.ui.math.Screen;
import net.arcadiusmc.ui.struct.Document;
import net.arcadiusmc.ui.style.DocumentStyles;
import net.arcadiusmc.ui.style.StyleParser;
import net.arcadiusmc.ui.style.Stylesheet;
import net.arcadiusmc.ui.util.ParserErrors;
import net.arcadiusmc.ui.util.ParserErrors.Error;
import net.arcadiusmc.ui.util.ParserException;
import net.arcadiusmc.utils.io.PathUtil;
import net.arcadiusmc.utils.io.Results;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.joml.Vector3f;
import org.slf4j.Logger;

public class PageLoader {

  private static final String DEFAULT_PAGE_FILE = "./index.xml";
  private static final String DEFAULT_STYLES_PATH = "default-style.scss";

  private static final Logger LOGGER = Loggers.getLogger();

  @Getter
  private final Path directory;

  private FileSystemProvider zipProvider;
  private final Map<FileSystem, FileSystemRef> refMap = new HashMap<>();

  private Stylesheet defaultStyle;

  public PageLoader(UiPlugin plugin) {
    this.directory = plugin.getDataPath().resolve("menus");
    referenceSystem(FileSystems.getDefault());
  }

  public void loadDefaultStyles() {
    URL url = getClass().getClassLoader().getResource(DEFAULT_STYLES_PATH);

    if (url == null) {
      LOGGER.error("Failed to load default style: no {} file found in jar path",
          DEFAULT_STYLES_PATH
      );

      return;
    }

    StringBuffer input;

    try (InputStream in = url.openStream()) {
      InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
      StringWriter writer = new StringWriter();

      reader.transferTo(writer);

      input = writer.getBuffer();
    } catch (IOException e) {
      LOGGER.error("Failed to load default style from {}: IO Error", DEFAULT_STYLES_PATH, e);
      return;
    }

    StyleParser parser = new StyleParser(input);
    ParserErrors errors = parser.getErrors();
    Stylesheet sheet;

    try {
      sheet = parser.stylesheet();
    } catch (ParserException ignored) {
      sheet = null;
      // Ignored, errors are handled ahead
    }

    for (Error error : errors.getErrors()) {
      switch (error.level()) {
        case ERROR -> LOGGER.error("Error loading default style: {}", error.message());
        case WARN -> LOGGER.warn("Warning loading default style: {}", error.message());
      }
    }

    if (errors.isErrorPresent()) {
      return;
    }

    defaultStyle = sheet;
  }

  public Set<String> getModuleNames() {
    Set<String> result = new HashSet<>();

    PathUtil.iterateDirectory(directory, false, true, path -> {
      String relativeName = path.getFileName().toString();

      if (Files.isDirectory(path)) {
        result.add(relativeName);
        return;
      }

      if (!relativeName.endsWith(".zip")) {
        return;
      }

      int dotIndex = relativeName.lastIndexOf('.');
      if (dotIndex >= 0 && !Files.isDirectory(path)) {
        relativeName = relativeName.substring(0, dotIndex);
      }

      result.add(relativeName);
    });

    return result;
  }

  public DataResult<Path> findModulePath(String moduleName) {
    Path p1 = directory.resolve(moduleName);
    Path p2 = directory.resolve(moduleName + ".zip");

    if (Files.isDirectory(p1)) {
      return DataResult.success(p1);
    }

    if (Files.exists(p2)) {
      return DataResult.success(p2);
    }

    return Results.error(
        "Unable to find page module with name '%s'. Must either be a directory or a '.zip' file",
        moduleName
    );
  }

  public Vector3f getInitialOpeningPosition(Player player) {
    Location location = player.getLocation();

    Vector direction = location.getDirection();
    direction.setY(0);
    direction.normalize();

    Vector3f vec = new Vector3f();
    vec.x = (float) (location.getX() + direction.getX());
    vec.y = (float) (location.getY() + direction.getY());
    vec.z = (float) (location.getZ() + direction.getZ());

    return vec;
  }

  public DataResult<Document> openDocument(PageRef reference, Player player) {
    DataResult<Path> pathResult = findModulePath(reference.moduleName());

    if (pathResult.isError()) {
      return pathResult.map(path -> null);
    }

    Path path = pathResult.getOrThrow();

    Path resourcesRoot;
    FileSystemRef systemRef;

    if (Files.isDirectory(path)) {
      resourcesRoot = path;
      systemRef = referenceSystem(path.getFileSystem());
    } else {
      try {
        systemRef = referenceSystem(loadZipSystem(path));
      } catch (IOException exc) {
        LOGGER.error("Error opening ZIP file system", exc);

        return Results.error(
            "Failed to open module '%s': IO error opening zip file",
            reference.moduleName()
        );
      }

      if (systemRef == null) {
        return Results.error("Failed to find ZIP file system provider (????)");
      }

      resourcesRoot = systemRef.getSystem().getPath("");
    }

    Document document = new Document(player.getWorld());
    document.setPlayer(player);

    PageResources resources = new PageResources(document, resourcesRoot, systemRef);

    PagePath uri = reference.path();
    String pagePath;

    if (uri.getElements().isEmpty()) {
      pagePath = DEFAULT_PAGE_FILE;
    } else {
      pagePath = uri.elements();
    }

    if (uri.getElements().size() > 1) {
      StringBuilder builder = new StringBuilder();
      Iterator<String> it = uri.getElements().iterator();

      while (it.hasNext()) {
        String el = it.next();

        if (it.hasNext()) {
          builder.append(el).append('/');
        }
      }

      Path workingDir = resourcesRoot.resolve(builder.toString());
      resources.setWorkingDirectory(workingDir);
    }

    Optional<ParsedDocument> opt = resources.loadDocument(pagePath);

    if (opt.isEmpty()) {
      return Results.error("Failed to open module '%s': Error loading page XML",
          reference.moduleName()
      );
    }

    ParsedDocument parsedDoc = opt.get();
    document.setBody(parsedDoc.body());

    Screen screen = document.getScreen();
    float w = parsedDoc.width();
    float h = parsedDoc.height();

    if (w <= 0) {
      w = DEFAULT_WIDTH;
    }
    if (h <= 0) {
      h = DEFAULT_HEIGHT;
    }

    Vector3f initialPos = getInitialOpeningPosition(player);
    screen.set(initialPos, w, h);

    document.getOptions().putAll(parsedDoc.options());

    DocumentStyles styles = document.getStyles();

    if (defaultStyle != null) {
      styles.addStylesheet(defaultStyle);
    }

    for (Stylesheet stylesheet : parsedDoc.stylesheets()) {
      styles.addStylesheet(stylesheet);
    }

    return DataResult.success(document);
  }

  private FileSystemRef referenceSystem(FileSystem sys) {
    boolean defaultSystem = Objects.equals(sys, FileSystems.getDefault());
    return refMap.computeIfAbsent(sys, fileSystem -> new FileSystemRef(fileSystem, defaultSystem));
  }

  private FileSystem loadZipSystem(Path path) throws IOException {
    URI uri = path.toRealPath().toUri();
    FileSystemProvider provider = getZipProvider();

    if (provider == null) {
      return null;
    }

    try {
      return provider.getFileSystem(uri);
    } catch (FileSystemNotFoundException exc) {
      // ignored, just means we need to create the system
    }

    return provider.newFileSystem(uri, Map.of());
  }

  private FileSystemProvider getZipProvider() {
    if (zipProvider != null) {
      return zipProvider;
    }

    return zipProvider = FileSystemProvider.installedProviders().stream()
        .filter(provider -> provider.getScheme().equals("zip"))
        .findFirst()
        .orElse(null);
  }
}
