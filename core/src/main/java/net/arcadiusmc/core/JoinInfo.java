package net.arcadiusmc.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BooleanSupplier;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.text.ViewerAwareMessage;
import net.arcadiusmc.text.placeholder.PlaceholderRenderer;
import net.arcadiusmc.text.placeholder.Placeholders;
import net.arcadiusmc.user.User;
import net.arcadiusmc.utils.DateRange;
import net.arcadiusmc.utils.MonthDayPeriod;
import net.arcadiusmc.utils.Result;
import net.arcadiusmc.utils.io.JsonUtils;
import net.arcadiusmc.utils.io.JsonWrapper;
import net.arcadiusmc.utils.io.PathUtil;
import net.arcadiusmc.utils.io.PluginJar;
import net.arcadiusmc.utils.io.SerializationHelper;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

public class JoinInfo {

  private static final Logger LOGGER = Loggers.getLogger();

  static final BooleanSupplier ALWAYS_TRUE = () -> true;
  static final BooleanSupplier ALWAYS_FALSE = () -> false;

  private final Path path;

  private final Map<String, Section> sections = new HashMap<>();

  public JoinInfo() {
    this.path = PathUtil.pluginPath("join-info.toml");
  }

  public void show(User user) {
    for (Section value : sections.values()) {
      if (!value.isVisible()) {
        continue;
      }

      value.show(user);
    }
  }

  public void clear() {
    sections.clear();
  }

  public void load() {
    clear();

    PluginJar.saveResources("join-info.toml", path);
    SerializationHelper.readAsJson(path, this::load);
  }

  private void load(JsonObject json) {
    for (Entry<String, JsonElement> entry : json.entrySet()) {
      var sectionResult = loadSection(entry.getValue());

      sectionResult.apply(string -> {
        LOGGER.error("Failed to load section '{}': {}", entry.getKey(), string);
      }, section -> {
        sections.put(entry.getKey(), section);
        LOGGER.debug("Loaded join-info section: '{}'", entry.getKey());
      });
    }
  }

  private Result<Section> loadSection(JsonElement element) {
    if (!element.isJsonObject()) {
      ViewerAwareMessage message = JsonUtils.readMessage(element);
      return Result.success(new Section(message, ALWAYS_TRUE));
    }

    JsonWrapper json = JsonWrapper.wrap(element.getAsJsonObject());

    if (!json.has("info")) {
      return Result.error("No 'info' set");
    }

    BooleanSupplier supplier;
    ViewerAwareMessage message;

    if (json.has("visible")) {
      var result = loadVisibleSupplier(json.get("visible"));
      if (result.isError()) {
        return result
            .mapError(string -> "Couldn't load visible condition: " + string)
            .cast();
      }
      supplier = result.getValue();
    } else {
      supplier = ALWAYS_TRUE;
    }

    message = json.get("info", JsonUtils::readMessage);

    return Result.success(new Section(message, supplier));
  }

  Result<BooleanSupplier> loadVisibleSupplier(JsonElement element) {
    if (element.isJsonPrimitive()) {
      boolean state = element.getAsBoolean();
      return Result.success(state ? ALWAYS_TRUE : ALWAYS_FALSE);
    }

    JsonWrapper json = JsonWrapper.wrap(element.getAsJsonObject());

    if (json.has("days")) {
      var period = MonthDayPeriod.load(json.get("days"));
      DateBasedCondition dateBasedCondition = period::contains;
      return Result.success(dateBasedCondition);
    }

    if (json.has("dates")) {
      var dateRange = DateRange.load(json.get("dates"));
      return dateRange.map(localDates -> (DateBasedCondition) localDates::contains);
    }

    return Result.error("Don't know how to load from: %s", element);
  }

  interface DateBasedCondition extends BooleanSupplier {

    @Override
    default boolean getAsBoolean() {
      LocalDate now = LocalDate.now();
      return test(now);
    }

    boolean test(LocalDate date);
  }

  record Section(ViewerAwareMessage message, BooleanSupplier visible) {

    public boolean isVisible() {
      return visible.getAsBoolean();
    }

    public void show(User user) {
      PlaceholderRenderer renderer = Placeholders.newRenderer().useDefaults();

      Component base = message.create(user);
      Component text = renderer.render(base);

      user.sendMessage(text);
    }
  }
}
