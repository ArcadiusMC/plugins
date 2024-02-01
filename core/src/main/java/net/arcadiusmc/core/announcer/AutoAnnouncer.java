package net.arcadiusmc.core.announcer;

import static net.kyori.adventure.text.Component.text;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import net.arcadiusmc.core.PrefsBook;
import net.arcadiusmc.text.ViewerAwareMessage;
import net.arcadiusmc.text.channel.ChannelledMessage;
import net.arcadiusmc.text.channel.MessageRenderer;
import net.arcadiusmc.text.placeholder.PlaceholderRenderer;
import net.arcadiusmc.text.placeholder.Placeholders;
import net.arcadiusmc.user.User;
import net.arcadiusmc.utils.Audiences;
import net.arcadiusmc.utils.Tasks;
import net.arcadiusmc.utils.io.JsonUtils;
import net.arcadiusmc.utils.io.JsonWrapper;
import net.arcadiusmc.utils.io.PathUtil;
import net.arcadiusmc.utils.io.PluginJar;
import net.arcadiusmc.utils.io.SerializationHelper;
import net.kyori.adventure.text.Component;
import org.bukkit.scheduler.BukkitTask;

public class AutoAnnouncer implements Runnable {

  private final Path path;
  private final List<ViewerAwareMessage> messages = new ArrayList<>();

  private Duration interval = Duration.ofMinutes(5);
  private Component format = text("${message}");
  private AnnouncementIterator iterator;
  private Order order = Order.INCREMENTING;

  private BukkitTask task;

  public AutoAnnouncer() {
    this.path = PathUtil.pluginPath("auto_announcer.toml");
  }

  public void load() {
    messages.clear();
    interval = Duration.ofMinutes(5);

    PluginJar.saveResources("auto_announcer.toml", path);
    SerializationHelper.readAsJson(path, this::loadFrom);
  }

  private void loadFrom(JsonWrapper json) {
    interval = json.get("interval", JsonUtils::readDuration);
    messages.addAll(json.getList("messages", JsonUtils::readMessage));
    format = json.getComponent("format");
    order = json.getEnum("order", Order.class, Order.INCREMENTING);
  }

  public void start() {
    stop();
    iterator = order.createIterator(messages);
    task = Tasks.runTimer(this, interval, interval);
  }

  public void stop() {
    Tasks.cancel(task);
    task = null;
    iterator = null;
  }

  @Override
  public void run() {
    if (messages.isEmpty()) {
      return;
    }

    if (iterator == null) {
      iterator = order.createIterator(messages);
    } else if (!iterator.hasNext()) {
      iterator.reset();
    }

    ViewerAwareMessage base = iterator.next();
    PlaceholderRenderer placeholders = Placeholders.newRenderer().useDefaults();

    ChannelledMessage.create(base)
        .setChannelName("auto_broadcast")
        .setBroadcast()

        .setRenderer((viewer, baseMessage) -> {
          var rendered = placeholders.render(baseMessage, viewer);

          return Placeholders.newRenderer()
              .useDefaults()
              .add("message", rendered)
              .render(format);
        })

        .filterTargets(audience -> {
          User user = Audiences.getUser(audience);

          // Null most likely means the viewer is the console.
          // Console doesn't need to see announcements
          if (user == null) {
            return false;
          }

          return !user.get(PrefsBook.IGNORE_AUTO_BROADCASTS);
        })
        .send();
  }

  public MessageRenderer renderer(PlaceholderRenderer placeholders) {
    return (viewer, baseMessage) -> {
      var rendered = placeholders.render(baseMessage, viewer);

      return Placeholders.newRenderer()
          .useDefaults()
          .add("message", rendered)
          .render(format);
    };
  }
}
