package net.arcadiusmc.ui.resource;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.xml.parsers.ParserConfigurationException;
import lombok.Getter;
import lombok.Setter;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.ui.struct.Document;
import net.arcadiusmc.ui.style.StyleParser;
import net.arcadiusmc.ui.style.Stylesheet;
import net.arcadiusmc.ui.util.ParserErrors;
import net.arcadiusmc.ui.util.ParserErrors.Error;
import net.arcadiusmc.ui.util.ParserException;
import net.arcadiusmc.utils.inventory.ItemStacks;
import net.arcadiusmc.utils.io.SerializationHelper;
import org.bukkit.inventory.ItemStack;
import org.slf4j.Logger;
import org.slf4j.event.Level;
import org.xml.sax.SAXException;

public class PageResources implements Closeable {

  private static final Logger LOGGER = Loggers.getLogger();

  @Getter
  private final Path resourcesRoot;
  private final FileSystemRef resourcesSystem;

  @Setter @Getter
  private Path workingDirectory;

  @Getter
  private final Map<String, Object> styleVariables = new HashMap<>();

  private final Document view;

  public PageResources(
      Document view,
      Path resourcesRoot,
      FileSystemRef resourcesSystem
  ) {
    this.view = view;
    this.resourcesRoot = resourcesRoot;
    this.resourcesSystem = resourcesSystem;
    this.workingDirectory = resourcesRoot;
  }

  @Override
  public void close() throws IOException {
    resourcesSystem.free();

    if (resourcesSystem.getReferenceCount() > 0 || resourcesSystem.isDefaultSystem()) {
      return;
    }

    resourcesSystem.getSystem().close();
  }

  public Path resourcePath(String uri) {
    Path dir;

    if (uri.startsWith("../") || uri.startsWith("..\\")) {
      uri = uri.substring(3);
      Path parent = workingDirectory.getParent();
      dir = Objects.requireNonNullElse(parent, workingDirectory);
    } else if (uri.startsWith("./") || uri.startsWith(".\\")) {
      uri = uri.substring(2);
      dir = workingDirectory;
    } else {
      dir = resourcesRoot;
    }

    if (Strings.isNullOrEmpty(uri)) {
      return dir;
    }

    return dir.resolve(uri).toAbsolutePath();
  }

  private boolean ensureResourceExists(Path p, String uri) {
    if (!Files.exists(p)) {
      LOGGER.error("Cannot load resource {}: doesn't exist", uri);
      return false;
    }

    if (Files.isDirectory(p)) {
      LOGGER.error("Cannot load resource {}: path points to a directory", uri);
      return false;
    }

    return true;
  }

  public Optional<ItemStack> loadItemStack(String uri) {
    Path p = resourcePath(uri);

    if (!ensureResourceExists(p, uri)) {
      return Optional.empty();
    }

    JsonObject obj;

    try {
      obj = SerializationHelper.readAsJson(p);
    } catch (IOException exc) {
      LOGGER.error("Failed to load item data from {}: IO Error", uri, exc);
      return Optional.empty();
    }

    return ItemStacks.NMS_CODEC.parse(JsonOps.INSTANCE, obj)
        .ifError(itemStackError -> {
          String msg = itemStackError.message();
          String[] split = msg.split("; ");

          LOGGER.error("Failed to load item data from {}:", uri);
          for (String s : split) {
            LOGGER.error("- {}", s);
          }
        })
        .resultOrPartial();
  }

  public Optional<ParsedDocument> loadDocument(String uri) {
    Path p = resourcePath(uri);

    if (!ensureResourceExists(p, uri)) {
      return Optional.empty();
    }

    BufferedReader reader;

    try {
      reader = Files.newBufferedReader(p);
    } catch (IOException exception) {
      LOGGER.error("Failed to read page file {}: IO Error", uri, exception);
      return Optional.empty();
    }

    PageXmlLoader loader = new PageXmlLoader(uri, reader, view, this);

    try {
      return loader.parse();
    } catch (ParserConfigurationException | IOException | SAXException e) {
      LOGGER.error("Failed to read page file {}: Error", uri, e);
    }

    return Optional.empty();
  }

  public Optional<Stylesheet> loadStylesheet(String uri) {
    Path p = resourcePath(uri);

    if (!ensureResourceExists(p, uri)) {
      return Optional.empty();
    }

    StringBuffer buffer;

    try {
      BufferedReader input = Files.newBufferedReader(p);
      StringWriter writer = new StringWriter();
      input.transferTo(writer);
      buffer = writer.getBuffer();
    } catch (IOException e) {
      LOGGER.error("Failed to read resource {}: IO Error", uri, e);
      return Optional.empty();
    }

    StyleParser parser = new StyleParser(buffer);
    Stylesheet stylesheet;

    parser.getVariables().putAll(styleVariables);

    try {
      stylesheet = parser.stylesheet();
    } catch (ParserException exc) {
      stylesheet = null;
      // Ignored, errors are handled ahead
    }

    ParserErrors errors = parser.getErrors();
    boolean failed = errors.isErrorPresent();

    if (failed) {
      LOGGER.error("Failed to load resource {}:", uri);
    }

    styleVariables.putAll(parser.getVariables());

    for (Error error : errors.getErrors()) {
      Level level = switch (error.level()) {
        case WARN -> Level.WARN;
        case ERROR -> Level.ERROR;
      };

      LOGGER.atLevel(level)
          .setMessage(error.message())
          .log();
    }

    if (failed) {
      return Optional.empty();
    }

    return Optional.ofNullable(stylesheet);
  }
}
