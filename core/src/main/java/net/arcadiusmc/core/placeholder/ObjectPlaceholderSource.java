package net.arcadiusmc.core.placeholder;

import net.arcadiusmc.Loggers;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.text.placeholder.ObjectPlaceholder;
import net.arcadiusmc.text.placeholder.PlaceholderContext;
import net.arcadiusmc.text.placeholder.PlaceholderSource;
import net.arcadiusmc.text.placeholder.TextPlaceholder;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

public class ObjectPlaceholderSource implements PlaceholderSource {

  private static final Logger LOGGER = Loggers.getLogger();

  private final PlaceholderServiceImpl service;

  public ObjectPlaceholderSource(PlaceholderServiceImpl service) {
    this.service = service;
  }

  @Override
  public TextPlaceholder getPlaceholder(String name, PlaceholderContext ctx) {
    String[] nodes = name.split("\\.");
    int nodeIndex = 1;

    Object value = ctx.context().get(nodes[0]);

    if (value == null) {
      return null;
    }

    while (nodeIndex <= nodes.length) {
      String node;
      boolean last;

      if (nodeIndex >= nodes.length) {
        node = "";
        last = true;
      } else {
        node = nodes[nodeIndex];
        last = false;
      }

      nodeIndex++;

      ObjectPlaceholder placeholder = service.getTypePlaceholder(value);

      if (placeholder != null) {
        value = placeholder.lookup(value, node, ctx);
        continue;
      }

      if (last) {
        break;
      }

      LOGGER.debug("Couldn't find placeholder type for value {}", value);
      return null;
    }

    return new ObjectTextPlaceholder(value);
  }

  record ObjectTextPlaceholder(Object o) implements TextPlaceholder {

    @Override
    public Component render(String match, PlaceholderContext render) {
      return Text.valueOf(o, render.viewer());
    }
  }
}
