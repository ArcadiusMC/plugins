package net.arcadiusmc.ui.struct;

import java.util.ArrayList;
import java.util.List;

public enum NodeFlag {
  HOVERED,
  CLICKED;

  final int mask;

  NodeFlag() {
    this.mask = 1 << ordinal();
  }

  static int combineMasks(NodeFlag... flags) {
    int m = 0;
    for (NodeFlag flag : flags) {
      m |= flag.mask;
    }
    return m;
  }

  public static List<NodeFlag> ofMask(int flags) {
    List<NodeFlag> list = new ArrayList<>();

    for (NodeFlag value : values()) {
      if ((value.mask & flags) != value.mask) {
        continue;
      }
      list.add(value);
    }

    return list;
  }
}
