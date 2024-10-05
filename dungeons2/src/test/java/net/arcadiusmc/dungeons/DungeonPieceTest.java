package net.arcadiusmc.dungeons;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.arcadiusmc.utils.math.Bounds3i;
import net.arcadiusmc.utils.math.Direction;
import net.arcadiusmc.utils.math.Rotation;
import org.junit.jupiter.api.Test;
import org.spongepowered.math.vector.Vector3i;

class DungeonPieceTest {

  static final Vector3i GATE_SIZE = Vector3i.from(3, 22, 13);

  @Test
  void testAlignment() {
    Vector3i firstMin = Vector3i.from(-295, 117, 401);

    DungeonPiece first = new DungeonPiece();
    first.setBoundingBox(Bounds3i.of(firstMin, Vector3i.from(-263, 139, 433)));
    first.setRotation(Rotation.NONE);

    Doorway[] doorways = new Doorway[4];
    doorways[0] = gateway(-295, 118, 417, Direction.WEST, first);
    doorways[1] = gateway(-279, 118, 433, Direction.SOUTH, first);
    doorways[2] = gateway(-263, 118, 417, Direction.EAST, first);
    doorways[3] = gateway(279, 118, 401, Direction.NORTH, first);
    first.setDoorways(doorways);

    DungeonPiece attach1 = createAttachmentPiece();
    DungeonPiece attach2 = createAttachmentPiece();
    DungeonPiece attach3 = createAttachmentPiece();
    DungeonPiece attach4 = createAttachmentPiece();

    doorways[0].connect(attach1.getDoorways()[0]);
    doorways[1].connect(attach2.getDoorways()[0]);
    doorways[2].connect(attach3.getDoorways()[0]);
    doorways[3].connect(attach4.getDoorways()[0]);

    Doorway g1 = attach1.getDoorways()[0];
    assertEquals(Direction.EAST, g1.getDirection());
    assertEquals(doorways[0].getCenter().add(g1.getDirection().getMod()), g1.getCenter());

    Doorway g2 = attach2.getDoorways()[0];
    assertEquals(Direction.NORTH, g2.getDirection());

    Doorway g3 = attach3.getDoorways()[0];
    assertEquals(Direction.WEST, g3.getDirection());

    Doorway g4 = attach4.getDoorways()[0];
    assertEquals(Direction.SOUTH, g4.getDirection());
  }

  private static DungeonPiece createAttachmentPiece() {
    DungeonPiece piece = new DungeonPiece();
    piece.setRotation(Rotation.NONE);
    piece.setBoundingBox(Bounds3i.of(Vector3i.from(-319, 116, 460), Vector3i.from(-317, 137, 480)));

    Doorway[] doorways = new Doorway[1];
    doorways[0] = gateway(-317, 117, 474, Direction.EAST, piece);
    piece.setDoorways(doorways);

    return piece;
  }

  private static Doorway gateway(int x, int y, int z, Direction direction, DungeonPiece from) {
    Vector3i center = new Vector3i(x, y, z).add(direction.getMod()).sub(0, 1, 0);

    Doorway doorway = new Doorway();
    doorway.setCenter(center);
    doorway.setDirection(direction);
    doorway.setFrom(from);

    return doorway;
  }
}