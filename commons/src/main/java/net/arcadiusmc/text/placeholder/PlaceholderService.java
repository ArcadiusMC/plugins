package net.arcadiusmc.text.placeholder;

import java.util.List;

public interface PlaceholderService {

  <T> void addObjectPlaceholder(Class<T> type, ObjectPlaceholder<T> placeholder);

  void addDefaultSource(PlaceholderSource source);

  void removeDefaultSource(PlaceholderSource source);

  PlaceholderList getDefaults();

  List<PlaceholderSource> getDefaultSources();

  PlaceholderList newList();

  PlaceholderRenderer newRenderer();
}
