package net.arcadiusmc.text.channel;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;

@FunctionalInterface
public interface MessageRenderer {

  MessageRenderer DEFAULT = (viewer, baseMessage) -> baseMessage;

  Component render(Audience viewer, Component baseMessage);

  default MessageRenderer then(MessageRenderer renderer) {
    return (viewer, baseMessage) -> {
      var rendered = MessageRenderer.this.render(viewer, baseMessage);
      return renderer.render(viewer, rendered);
    };
  }
}
