package net.arcadiusmc.dungeons;

public interface Visitor {

  Result visit(DungeonPiece piece);

  enum Result {
    CONTINUE,
    BREAK,
    SKIP_CHILDREN,
    SKIP_SIBLINGS
  }
}
