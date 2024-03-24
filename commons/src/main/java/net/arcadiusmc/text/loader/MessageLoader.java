package net.arcadiusmc.text.loader;

import com.google.common.base.Joiner;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Stack;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.text.parse.ChatParser;
import net.arcadiusmc.utils.io.FormatConversions;
import net.arcadiusmc.utils.io.PathUtil;
import net.arcadiusmc.utils.io.PluginJar;
import net.arcadiusmc.utils.io.SerializationHelper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.Style.Builder;
import net.kyori.adventure.text.format.Style.Merge.Strategy;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;

public class MessageLoader {

  private static final String STYLE_KEY = "_style";
  private static final String STYLES_KEY = "_styles";

  private static final Logger LOGGER = Loggers.getLogger();

  private static final char YAML_PATH_SEPARATOR = ';';
  public static final String MESSAGE_FILE_NAME = "messages.yml";

  /**
   * Load plugin message file
   * <p>
   * Loads a plugin message file from plugin data folder and uses the message file
   * in a plugin's jar resources to complete the messages file if any are missing
   * from the disk
   * <p>
   * This function attempts to load a file named {@link #MESSAGE_FILE_NAME} in the
   * plugin's directory and attempts to load a resource with the same name.
   * <p>
   * If it fails, error is logged to output
   *
   * @param plugin Plugin loading the message file
   * @param list List the messages are being loaded into
   */
  public static void loadPluginMessages(JavaPlugin plugin, MessageList list) {
    list.clear();

    Path messageFile = PathUtil.pluginPath(plugin, MESSAGE_FILE_NAME);
    Path messageResource = PluginJar.resourcePath(plugin, MESSAGE_FILE_NAME);

    boolean loadDefaults;

    if (!Files.exists(messageResource)) {
      if (!Files.exists(messageFile)) {
        LOGGER.error("Plugin {} has no '{}' file or jar resource, cannot load messages!",
            plugin.getName(),
            MESSAGE_FILE_NAME
        );
        return;
      }

      LOGGER.warn(
          "Plugin {} has no '{}' jar resource! Cannot load default messages, "
              + "using only on-disk message file",
          plugin.getName(), MESSAGE_FILE_NAME
      );

      loadDefaults = false;
    } else {
      loadDefaults = true;
      PluginJar.saveResources(plugin, MESSAGE_FILE_NAME, messageFile);
    }

    try {
      YamlConfiguration fileConfig = loadConfig(messageFile);
      fileConfig.options().copyDefaults(true);

      if (loadDefaults) {
        YamlConfiguration resourceConfig = loadConfig(messageResource);
        fileConfig.setDefaults(resourceConfig);
      }

      JsonObject json = yamlToJson(fileConfig);
      loadFromJson(json, list);

    } catch (IOException exc) {
      LOGGER.error("IO error while trying to load plugin message files", exc);
    }
  }

  /**
   * Load messages from file
   * <p>
   * This function supports 4 file formats: YAML, TOML, JSON, and '.properties'. <b>Warning:</b>
   * while .properties loading is supported, it lacks support for any style options that both JSON
   * and YAML have.
   * <p>
   * If any errors occur during the execution of this function, the method returns and logs the
   * error
   *
   * @param path File to load from
   * @param target Message list to load messages into
   */
  public static void loadInto(Path path, MessageList target) {
    target.clear();

    if (!Files.exists(path)) {
      LOGGER.error("Couldn't load messages from file {}, it doesn't exist", path);
      return;
    }

    String fileFormat = getExtension(path);

    if (fileFormat == null) {
      LOGGER.error("Message file {} has no extension", path);
      return;
    }

    switch (fileFormat.toLowerCase()) {
      case "yml", "yaml" -> {
        SerializationHelper.readFileObject(path, MessageLoader::loadConfig)
            .resultOrPartial(LOGGER::error)

            // It's a lot easier to just convert YAML into JSON and
            // to feed that to the JSON loader than have 2 separate
            // implementations of the loader
            .map(MessageLoader::yamlToJson)

            .ifPresent(object -> loadFromJson(object, target));
      }

      case "toml", "json" -> {
        SerializationHelper.readAsJson(path, json -> loadFromJson(json, target));
      }

      case "properties" -> {
        SerializationHelper.readFileObject(path, MessageLoader::loadProperties)
            .resultOrPartial(LOGGER::error)
            .ifPresent(properties -> loadFromProperties(properties, target));
      }

      default -> {
        LOGGER.error("Unsupported message file format: {} (file = {})", fileFormat, path);
      }
    }
  }

  private static YamlConfiguration loadConfig(Path path) throws IOException {
    try (BufferedReader input = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
      YamlConfiguration config = new YamlConfiguration();

      // Has to be changed because, by default, it's '.' and the '.' is used
      // as just a regular key character in the messages files
      config.options().pathSeparator(YAML_PATH_SEPARATOR);
      config.load(input);

      input.close();

      return config;
    } catch (InvalidConfigurationException exc) {
      throw new IOException(exc);
    }
  }

  private static Properties loadProperties(Path path) throws IOException {
    Properties properties = new Properties();

    try (var input = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
      properties.load(input);
    }

    return properties;
  }

  private static String getExtension(Path path) {
    String fileName = path.getFileName().toString();
    int lastIndex = fileName.lastIndexOf('.');

    if (lastIndex == -1) {
      return null;
    }

    return fileName.substring(lastIndex + 1);
  }

  private static JsonObject yamlToJson(ConfigurationSection config) {
    return FormatConversions.ymlToJson(config).getAsJsonObject();
  }

  public static void loadFromProperties(Properties properties, MessageList list) {
    for (Entry<Object, Object> entry : properties.entrySet()) {
      String key = String.valueOf(entry.getKey());
      String value = String.valueOf(entry.getValue());
      list.add(key, value);
    }
  }

  public static void loadFromJson(JsonObject obj, MessageList list) {
    LoadContext ctx = new LoadContext();

    if (obj.has(STYLES_KEY)) {
      JsonObject stylesObject = obj.getAsJsonObject(STYLES_KEY);

      for (Entry<String, JsonElement> entry : stylesObject.entrySet()) {
        String name = entry.getKey();
        JsonElement value = entry.getValue();

        if (!value.isJsonPrimitive()) {
          LOGGER.error("Cannot read style variable {}: Not a string", name);
          continue;
        }

        Mutable<StyleScope> mutable = new MutableObject<>();
        parseStyle(value.getAsString(), mutable, ctx);

        if (mutable.getValue() != null) {
          ctx.addStyle(name, mutable.getValue());
        }
      }
    }

    loadFromPrefixedJson(ctx, obj, list);

    LOGGER.debug("Loaded a total of {} entries", ctx.loadCounter);
  }

  private static void loadFromPrefixedJson(
      LoadContext ctx,
      JsonObject obj,
      MessageList list
  ) {
    boolean stylePushed = false;

    if (obj.has(STYLE_KEY)) {
      Mutable<StyleScope> mutable = new MutableObject<>();
      parseStyle(obj.get(STYLE_KEY).getAsString(), mutable, ctx);

      if (mutable.getValue() != null) {
        stylePushed = true;
        ctx.pushStyle(mutable.getValue());
      }
    }

    for (Entry<String, JsonElement> entry : obj.entrySet()) {
      String key = entry.getKey();

      // Skip both reserved style keywords as we've probably
      // already handled the data inside them
      if (key.equals(STYLE_KEY) || key.equals(STYLES_KEY)) {
        continue;
      }

      String formatKey = ctx.prefix() + key;
      JsonElement element = entry.getValue();

      if (element.isJsonPrimitive() || element.isJsonNull()) {
        String strValue = element.isJsonNull() ? "null" : element.getAsString();

        Component component = ChatParser.parser().parseBasic(strValue, ListImpl.TEXT_CONTEXT)
            .applyFallbackStyle(ctx.style());

        list.add(formatKey, component);

        LOGGER.debug("Added message {}: {}", String.format("%-45s", formatKey), strValue);
        ctx.onMessageLoaded();

        continue;
      }

      if (element.isJsonObject()) {
        var object = element.getAsJsonObject();
        ctx.pushPrefix(key);
        loadFromPrefixedJson(ctx, object, list);
        ctx.popPrefix();
        continue;
      }

      LOGGER.error("Don't know how to load value at '{}' into message: {}",
          formatKey, element
      );
    }

    if (stylePushed) {
      ctx.popStyle();
    }
  }

  private static void parseStyle(String string, Mutable<StyleScope> out, LoadContext ctx) {

    // Style variable reference
    if (string.startsWith("$")) {
      String variableRef = string.substring(1);
      StyleScope style = ctx.getStyle(variableRef);

      if (style == null) {
        LOGGER.warn("Unknown variable reference '{}' in {}", variableRef, ctx.prefix());
        return;
      }

      out.setValue(style);
      return;
    }

    if (string.trim().equalsIgnoreCase("reset")) {
      out.setValue(StyleReset.RESET);
      return;
    }

    try {
      StringReader reader = new StringReader(string);
      Builder builder = Style.style();

      StyleStringCodec.parseStyle(reader, builder);

      out.setValue(new SimpleStyleScope(builder.build()));
    } catch (CommandSyntaxException exc) {
      LOGGER.error("Error parsing style string at {}: {}", ctx.prefix(), exc.getMessage());
    }
  }

  private static class LoadContext {
    private final Stack<StyleScope> styleStack = new Stack<>();
    private final Stack<String> prefixes = new Stack<>();

    private Style cachedStyle = Style.empty();
    private String cachedPrefix = "";

    private final Map<String, StyleScope> styles = new HashMap<>();

    private int loadCounter = 0;

    private void onMessageLoaded() {
      loadCounter++;
    }

    void addStyle(String label, StyleScope style) {
      styles.put(label, style);
    }

    StyleScope getStyle(String name) {
      return styles.get(name);
    }

    Style style() {
      return cachedStyle;
    }

    String prefix() {
      return cachedPrefix;
    }

    void pushPrefix(String prefix) {
      prefixes.push(prefix);
      cachePrefix();
    }

    void popPrefix() {
      prefixes.pop();
      cachePrefix();
    }

    void pushStyle(StyleScope style) {
      styleStack.push(style);
      cacheStyle();
    }

    void popStyle() {
      styleStack.pop();
      cacheStyle();
    }

    void cachePrefix() {
      if (prefixes.isEmpty()) {
        cachedPrefix = "";
        return;
      }

      cachedPrefix = Joiner.on('.').join(prefixes) + ".";
    }

    void cacheStyle() {
      if (styleStack.isEmpty()) {
        cachedStyle = Style.empty();
      }

      Style.Builder builder = Style.style();

      for (var s : styleStack) {
        builder = s.combine(builder);
      }

      cachedStyle = builder.build();
    }
  }

  interface StyleScope {

    Style.Builder combine(Style.Builder builder);
  }

  record SimpleStyleScope(Style style) implements StyleScope {

    @Override
    public Builder combine(Builder builder) {
      return builder.merge(style, Strategy.IF_ABSENT_ON_TARGET);
    }
  }

  enum StyleReset implements StyleScope {
    RESET;

    @Override
    public Builder combine(Builder builder) {
      return Style.style();
    }
  }
}
