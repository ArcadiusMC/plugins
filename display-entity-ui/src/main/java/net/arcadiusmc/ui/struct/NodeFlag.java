package net.arcadiusmc.ui.struct;

public enum NodeFlag {
  ROOT;

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
}
