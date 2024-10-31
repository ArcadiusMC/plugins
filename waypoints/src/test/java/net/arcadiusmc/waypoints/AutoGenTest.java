package net.arcadiusmc.waypoints;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.spongepowered.math.vector.Vector2i;

class AutoGenTest {

  @Test
  void nearestGridCenter() {
    final Vector2i gridSize = Vector2i.from(500);
    Vector2i[][] posAndExpect = {
        {Vector2i.from(-944, -704), Vector2i.from(-1000, -500)},
        {Vector2i.from(944, 704), Vector2i.from(1000, 500)}
    };

    for (Vector2i[] vector2is : posAndExpect) {
      Vector2i pos = vector2is[0];
      Vector2i expected = vector2is[1];
      Vector2i gridCenter = AutoGen.nearestGridCenter(pos, gridSize);

      assertEquals(expected, gridCenter, "Expected " + expected + ", found " + gridCenter + " input: " + pos);
    }
  }
}