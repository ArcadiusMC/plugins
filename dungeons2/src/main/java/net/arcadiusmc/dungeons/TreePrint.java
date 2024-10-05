package net.arcadiusmc.dungeons;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class TreePrint {

  private final StringBuilder builder = new StringBuilder();
  private final Set<UUID> dejaVu = new HashSet<>();
  private int indent;

  public StringBuilder nlIndent() {
    return builder.append("\n").append("  ".repeat(indent));
  }

  public void visit(DungeonPiece piece) {
    if (!dejaVu.add(piece.getUniqueId())) {
      return;
    }

    nlIndent().append("uuid: ").append(piece.getUniqueId());

    if (piece.getStructure() != null) {
      nlIndent().append("structure: ").append(piece.getStructure().getKey());
    }

    nlIndent().append("palette-name: ").append(piece.getPaletteName());
    nlIndent().append("bounding-box: ").append(piece.getBoundingBox());
    nlIndent().append("rotation: ").append(piece.getRotation());
    nlIndent().append("depth: ").append(piece.getDepth());

    Doorway[] doorways = piece.getDoorways();
    if (doorways == null) {
      return;
    }

    nlIndent()
        .append("doorways(")
        .append(doorways.length)
        .append("): ");

    indent++;

    for (Doorway doorway : doorways) {
      nlIndent().append("- direction: ").append(doorway.getDirection());
      indent++;

      nlIndent().append("center: ").append(doorway.getCenter());
      nlIndent().append("offset: ").append(doorway.getCenter().sub(piece.getPivotPoint()));
      nlIndent().append("opening: ").append(doorway.getOpening());
      nlIndent().append("entrance: ").append(doorway.isEntrance());

      if (doorway.getTo() != null) {
        nlIndent().append("destination-piece:");
        DungeonPiece to = doorway.getTo().getFrom();

        if (dejaVu.contains(to.getUniqueId())) {
          builder.append(" DejaVu!!! unique-id=").append(to.getUniqueId());
        } else {
          indent++;
          visit(to);
        }
      }

      indent--;
    }

    indent--;
  }

  @Override
  public String toString() {
    return builder.toString();
  }
}
