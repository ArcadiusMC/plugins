package net.arcadiusmc.core.placeholder;

import java.util.Iterator;
import java.util.NoSuchElementException;
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
    Iterator<String> it = breakIntoElements(name);
    Object resultSoFar = findValue(it, ctx);

    if (resultSoFar == null) {
      return null;
    }

    while (true) {
      String fieldName;

      if (it.hasNext()) {
        fieldName = it.next();
      } else {
        fieldName = "";
      }

      ObjectPlaceholder<Object> type = (ObjectPlaceholder<Object>)
          service.getTypePlaceholder(resultSoFar);

      if (type != null) {
        resultSoFar = type.lookup(resultSoFar, fieldName, ctx);
      }

      if (resultSoFar instanceof TextPlaceholder placeholder) {
        return placeholder;
      }
      if (resultSoFar instanceof Component c) {
        return TextPlaceholder.simple(c);
      }

      if (!it.hasNext()) {
        type = (ObjectPlaceholder<Object>) service.getTypePlaceholder(resultSoFar);

        if (type != null) {
          return new ObjectTextPlaceholder(type.lookup(resultSoFar, "", ctx));
        }

        return new ObjectTextPlaceholder(resultSoFar);
      }
    }
  }

  private Object findValue(Iterator<String> it, PlaceholderContext ctx) {
    String el = "";
    Object result = null;

    while (result == null) {
      if (!it.hasNext()) {
        return null;
      }

      el = (el.isEmpty() ? "" : ".") + it.next();
      result = ctx.context().get(el);
    }

    return result;
  }

  private Iterator<String> breakIntoElements(String path) {
    String[] arr = path.split("\\.");
    return new ObjectPathIterator(arr);
  }

  record ObjectTextPlaceholder(Object o) implements TextPlaceholder {

    @Override
    public Component render(String match, PlaceholderContext render) {
      return Text.valueOf(o, render.viewer());
    }
  }

  static class ObjectPathIterator implements Iterator<String> {

    final String[] elements;
    int idx = 0;

    ObjectPathIterator(String[] elements) {
      this.elements = elements;
    }

    @Override
    public boolean hasNext() {
      return idx < elements.length;
    }

    @Override
    public String next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }

      return elements[idx++];
    }
  }
}
