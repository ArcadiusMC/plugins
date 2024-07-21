package net.arcadiusmc.ui.resource;

import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import lombok.Getter;

@Getter
public class PagePath {

  private final List<String> elements = new ArrayList<>();
  private final Map<String, String> query = new HashMap<>();

  public int elementCount() {
    return elements.size();
  }

  public String element(int index) {
    return elements.get(index);
  }

  public String getQuery(String key) {
    return query.get(key);
  }

  public PagePath addElement(String element) {
    elements.add(element);
    return this;
  }

  public PagePath putQuery(String key, String value) {
    query.put(key, value);
    return this;
  }

  @Override
  public String toString() {
    return elements() + query();
  }

  public String elements() {
    StringBuilder builder = new StringBuilder();
    Iterator<String> it = elements.iterator();

    while (it.hasNext()) {
      String el = it.next();

      if (el.contains(" ")) {
        builder.append('"')
            .append(el)
            .append('"');
      } else {
        builder.append(el);
      }

      if (it.hasNext()) {
        builder.append("/");
      }
    }

    return builder.toString();
  }

  public String query() {
    if (query.isEmpty()) {
      return "";
    }

    StringBuilder builder = new StringBuilder();
    builder.append('?');

    for (Entry<String, String> entry : query.entrySet()) {
      builder.append(entry.getKey());

      if (Strings.isNullOrEmpty(entry.getValue())) {
        continue;
      }

      builder.append('=').append(entry.getValue());
    }

    return builder.toString();
  }
}
