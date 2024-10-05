package net.arcadiusmc.dungeons.gen;

import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.block.data.type.Stairs.Shape;

public class StairQuadrants {

  // Quadrant bit masks
  public static final byte Q_NONE = 0x0;
  public static final byte Q_NE = 0x1;
  public static final byte Q_NW = 0x2;
  public static final byte Q_SE = 0x4;
  public static final byte Q_SW = 0x8;
  public static final byte Q_ALL = Q_NE | Q_NW | Q_SE | Q_SW;

  public static String quadrantName(int mask) {
    return switch (mask) {
      // Single quadrants, inner corners
      case Q_NW -> "Q_NW";
      case Q_NE -> "Q_NE";
      case Q_SW -> "Q_SW";
      case Q_SE -> "Q_SE";

      // Straights
      case Q_SE | Q_SW -> "Q_SE | Q_SW";
      case Q_SW | Q_NW -> "Q_SW | Q_NW";
      case Q_NW | Q_NE -> "Q_NW | Q_NE";
      case Q_SE | Q_NE -> "Q_SE | Q_NE";

      // Triples, outer corners
      case Q_SW | Q_NW | Q_NE -> "Q_SW | Q_NW | Q_NE";
      case Q_SE | Q_NE | Q_NW -> "Q_SE | Q_NE | Q_NW";
      case Q_NE | Q_SE | Q_SW -> "Q_NE | Q_SE | Q_SW";
      case Q_SE | Q_SW | Q_NW -> "Q_SE | Q_SW | Q_NW";

      default -> "INVALID(0x" + Integer.toHexString(mask) + ")";
    };
  }

  public static boolean isValidMask(int mask) {
    switch (mask) {
      case Q_NW:
      case Q_NE:
      case Q_SW:
      case Q_SE:
      case Q_SE | Q_SW:
      case Q_SW | Q_NW:
      case Q_NW | Q_NE:
      case Q_SE | Q_NE:
      case Q_SW | Q_NW | Q_NE:
      case Q_SE | Q_NE | Q_NW:
      case Q_NE | Q_SE | Q_SW:
      case Q_SE | Q_SW | Q_NW:
        return true;

      default:
        return false;
    }
  }

  public static BlockData createStairs(Material stairMat, int quadMask) {
    Stairs stairs = (Stairs) stairMat.createBlockData();

    switch (quadMask) {
      // Single quadrants, inner corners
      case Q_NW -> {
        stairs.setFacing(BlockFace.SOUTH);
        stairs.setShape(Shape.INNER_LEFT);
      }
      case Q_NE -> {
        stairs.setFacing(BlockFace.WEST);
        stairs.setShape(Shape.INNER_LEFT);
      }
      case Q_SW -> {
        stairs.setFacing(BlockFace.EAST);
        stairs.setShape(Shape.INNER_LEFT);
      }
      case Q_SE -> {
        stairs.setFacing(BlockFace.NORTH);
        stairs.setShape(Shape.INNER_LEFT);
      }

      // Straights
      case Q_SE | Q_SW -> {
        stairs.setFacing(BlockFace.NORTH);
        stairs.setShape(Shape.STRAIGHT);
      }
      case Q_SW | Q_NW -> {
        stairs.setFacing(BlockFace.EAST);
        stairs.setShape(Shape.STRAIGHT);
      }
      case Q_NW | Q_NE -> {
        stairs.setFacing(BlockFace.SOUTH);
        stairs.setShape(Shape.STRAIGHT);
      }
      case Q_SE | Q_NE -> {
        stairs.setFacing(BlockFace.WEST);
        stairs.setShape(Shape.STRAIGHT);
      }

      // Triples, outer corners
      case Q_SW | Q_NW | Q_NE -> {
        stairs.setFacing(BlockFace.SOUTH);
        stairs.setShape(Shape.OUTER_LEFT);
      }
      case Q_SE | Q_NE | Q_NW -> {
        stairs.setFacing(BlockFace.WEST);
        stairs.setShape(Shape.OUTER_LEFT);
      }
      case Q_NE | Q_SE | Q_SW -> {
        stairs.setFacing(BlockFace.NORTH);
        stairs.setShape(Shape.OUTER_LEFT);
      }
      case Q_SE | Q_SW | Q_NW -> {
        stairs.setFacing(BlockFace.EAST);
        stairs.setShape(Shape.OUTER_LEFT);
      }

      default -> {
        return null;
      }
    }

    return stairs;
  }
}
