package net.arcadiusmc.dialogues;

import net.kyori.adventure.text.Component;

public enum ButtonType {
  REGULAR,
  HIGHLIGHTED,
  UNAVAILABLE;

  public Component render(DialogueRenderer renderer, Component hoverText) {
    DialogueOptions options = renderer.getOptions();
    Component format = switch (this) {
      case REGULAR -> options.getAvailableFormat();
      case HIGHLIGHTED -> options.getHighlightFormat();
      case UNAVAILABLE -> options.getUnavailableFormat();
    };

    return renderer.render(format.hoverEvent(hoverText));
  }
}
