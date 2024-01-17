package net.arcadiusmc.text.placeholder;

public interface PlaceholderSource {

  TextPlaceholder getPlaceholder(String name, PlaceholderContext ctx);
}
