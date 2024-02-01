package net.arcadiusmc.core.placeholder;

import com.google.gson.JsonElement;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import lombok.Getter;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.core.CorePlugin;
import net.arcadiusmc.registry.Registries;
import net.arcadiusmc.text.ViewerAwareMessage;
import net.arcadiusmc.text.placeholder.ObjectPlaceholder;
import net.arcadiusmc.text.placeholder.PlaceholderContext;
import net.arcadiusmc.text.placeholder.PlaceholderService;
import net.arcadiusmc.text.placeholder.PlaceholderSource;
import net.arcadiusmc.text.placeholder.TextPlaceholder;
import net.arcadiusmc.user.User;
import net.arcadiusmc.utils.io.JsonUtils;
import net.arcadiusmc.utils.io.PathUtil;
import net.arcadiusmc.utils.io.PluginJar;
import net.arcadiusmc.utils.io.SerializationHelper;
import net.forthecrown.grenadier.CommandSource;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

@Getter
public class PlaceholderServiceImpl implements PlaceholderService {

  private final CorePlugin plugin;

  private final ObjectList<PlaceholderSource> defaultSources = new ObjectArrayList<>();
  private final PlaceholderListImpl defaults = new PlaceholderListImpl();

  private final List<PlaceholderType<?>> types = new ArrayList<>();

  public PlaceholderServiceImpl(CorePlugin plugin) {
    this.plugin = plugin;

    defaultSources.add(defaults);
    defaultSources.add(new TranslatedPlaceholders());
    defaultSources.add(new ObjectPlaceholderSource(this));

    defaults.add("text",    new ComponentPlaceholder());
    defaults.add("border",  new RepeatedCharPlaceholder(true));
    defaults.add("spaces",  new RepeatedCharPlaceholder(false));

    addObjectPlaceholder(Number.class,        ObjectPlaceholder.NUMBER);
    addObjectPlaceholder(Boolean.class,       ObjectPlaceholder.BOOLEAN);
    addObjectPlaceholder(Server.class,        ObjectPlaceholder.SERVER);
    addObjectPlaceholder(Location.class,      ObjectPlaceholder.LOCATION);
    addObjectPlaceholder(ItemStack.class,     ObjectPlaceholder.ITEM);
    addObjectPlaceholder(World.class,         ObjectPlaceholder.WORLD);
    addObjectPlaceholder(Player.class,        ObjectPlaceholder.PLAYER);
    addObjectPlaceholder(User.class,          ObjectPlaceholder.USER);
    addObjectPlaceholder(Duration.class,      ObjectPlaceholder.DURATION);
    addObjectPlaceholder(CommandSource.class, ObjectPlaceholder.COMMAND_SOURCE);
  }

  @Override
  public <T> void addObjectPlaceholder(Class<T> type, ObjectPlaceholder<T> placeholder) {
    Objects.requireNonNull(type, "Null type");
    Objects.requireNonNull(placeholder, "Null placeholder");
    types.add(new PlaceholderType<>(type, placeholder));
  }

  @Override
  public void addDefaultSource(PlaceholderSource source) {
    Objects.requireNonNull(source, "Null source");
    defaultSources.add(source);
  }

  @Override
  public void removeDefaultSource(PlaceholderSource source) {
    defaultSources.remove(source);
  }

  @Override
  public PlaceholderListImpl newList() {
    return new PlaceholderListImpl();
  }

  @Override
  public PlaceholderRendererImpl newRenderer() {
    return new PlaceholderRendererImpl(this);
  }

  public void load() {
    var path = PathUtil.pluginPath("placeholders.json");
    PluginJar.saveResources("placeholders.json", path);

    var logger = Loggers.getLogger();

    SerializationHelper.readAsJson(path, json -> {
      for (Entry<String, JsonElement> entry : json.entrySet()) {
        String key = entry.getKey();

        if (!Registries.isValidKey(key)) {
          logger.error("Cannot load placeholder '{}': Invalid key", key);
          continue;
        }

        try {
          ViewerAwareMessage message = JsonUtils.readMessage(entry.getValue());
          TextPlaceholder placeholder = new LoadedPlaceholder(message, true);

          defaults.add(key, placeholder);

          logger.debug("Loaded placeholder {}", key);
        } catch (Throwable t) {
          logger.error("Cannot load placeholder '{}'", key, t);
        }
      }
    });
  }

  public ObjectPlaceholder<?> getTypePlaceholder(Object value) {
    for (PlaceholderType<?> type : types) {
      if (!type.type.isInstance(value)) {
        continue;
      }

      return type.placeholder;
    }

    return null;
  }

  private record LoadedPlaceholder(ViewerAwareMessage message, boolean subRendering)
      implements TextPlaceholder
  {

    @Override
    public @Nullable Component render(String match, PlaceholderContext render) {
      Component text = message.create(render.viewer());

      return subRendering
          ? render.renderer().render(text, render.viewer(), render.context())
          : text;
    }
  }

  private record PlaceholderType<T>(Class<T> type, ObjectPlaceholder<T> placeholder) {

  }
}
