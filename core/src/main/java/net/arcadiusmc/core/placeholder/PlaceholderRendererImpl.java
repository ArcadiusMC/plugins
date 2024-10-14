package net.arcadiusmc.core.placeholder;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.MatchResult;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.text.placeholder.PlaceholderContext;
import net.arcadiusmc.text.placeholder.PlaceholderList;
import net.arcadiusmc.text.placeholder.PlaceholderRenderer;
import net.arcadiusmc.text.placeholder.PlaceholderSource;
import net.arcadiusmc.text.placeholder.TextPlaceholder;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class PlaceholderRendererImpl implements PlaceholderRenderer {

  private static final Logger LOGGER = Loggers.getLogger();

  private final PlaceholderServiceImpl service;
  private final List<PlaceholderSource> sources;
  private final PlaceholderListImpl selfList;

  public PlaceholderRendererImpl(PlaceholderServiceImpl service) {
    this.service = service;
    this.sources = new ArrayList<>();
    this.selfList = service.newList();

    addSource(selfList);
  }

  Map<String, Object> createContext(Audience viewer, Map<String, Object> ctx) {
    Map<String, Object> result = new Object2ObjectOpenHashMap<>();

    result.put("server", Bukkit.getServer());
    result.put("viewer", viewer);
    result.put("time", TimeProvider.INSTANCE);

    if (ctx != null) {
      result.putAll(ctx);
    }

    return result;
  }

  @Override
  public Component render(Component base, @Nullable Audience viewer, Map<String, Object> ctx) {
    if (service.getPlugin().getCoreConfig().placeholdersDisabled()) {
      return base;
    }

    PlaceholderContext render = new PlaceholderContext(viewer, this, createContext(viewer, ctx));

    TextReplacementConfig config = TextReplacementConfig.builder()
        .match(PATTERN)
        .replacement((result, builder) -> renderPlaceholder(result, render))
        .build();

    return base.replaceText(config);
  }

  private Component renderPlaceholder(MatchResult result, PlaceholderContext render) {
    if (result.group().startsWith("\\")) {
      return text(result.group().substring(1));
    }

    String placeholderName = result.group(1);

    String input = result.group(2);
    if (input == null) {
      input = "";
    } else {
      input = input.replace("\\}", "}").trim();
    }

    TextPlaceholder placeholder = getPlaceholder(placeholderName, render);

    if (placeholder == null) {
      LOGGER.debug("Unknown placeholder named '{}', full input '{}'",
          placeholderName, result.group()
      );

      return text(result.group());
    }

    Component rendered;

    try {
      rendered = placeholder.render(input, render);
    } catch (Exception t) {
      LOGGER.error("Error rendering placeholder! placeholder='{}' Skipping!", result.group(), t);
      return text(result.group());
    }

    if (rendered == null) {
      return text(result.group());
    }

    // Do this wrapping to prevent accidental style overflow
    // IE, stop the rendered text's color and decorations from
    // flooding over to any following text
    return empty().append(rendered);
  }

  @Override
  public TextPlaceholder getPlaceholder(String name, PlaceholderContext ctx) {
    for (PlaceholderSource source : sources) {
      var placeholder = source.getPlaceholder(name, ctx);

      if (placeholder == null) {
        continue;
      }

      return placeholder;
    }

    return null;
  }

  @Override
  public PlaceholderRenderer useDefaults() {
    sources.addAll(service.getDefaultSources());
    return this;
  }

  @Override
  public PlaceholderRenderer addSource(PlaceholderSource source) {
    sources.add(source);
    return this;
  }

  @Override
  public PlaceholderList getPlaceholderList() {
    return selfList;
  }
}
