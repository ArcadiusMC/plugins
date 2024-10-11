package net.arcadiusmc.dungeons.gen;

import static org.bukkit.Material.COBBLESTONE_SLAB;

import net.arcadiusmc.dungeons.NoiseParameter;
import net.arcadiusmc.dungeons.gen.BlockIterations.BlockIteration;
import net.arcadiusmc.utils.math.Direction;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.block.data.type.Stairs.Shape;

public class EdgeRotDecorator extends NoiseDecorator<NoiseParameter> implements XyzFunction {

  public static final DecoratorType<EdgeRotDecorator, NoiseParameter> TYPE
      = DecoratorType.create(NoiseParameter.CODEC, EdgeRotDecorator::new);

  Direction[] arr2 = new Direction[2];
  Direction[] arr3 = new Direction[3];

  public EdgeRotDecorator(NoiseParameter config) {
    super(config);
  }

  static BlockIteration getMaterial(Material base) {
    return BlockIterations.getIteration(base);
  }

  @Override
  public void execute() {
    runForEachBlock();
  }

  @Override
  public void accept(int x, int y, int z) {
    if (!isGroundBlock(x, y, z) || isAir(x, y - 1, z) || isSkyAbove(x, y, z)) {
      return;
    }

    int ey = y + 1;
    Direction edgeDir = edgeDirection(x, ey, z);

    if (edgeDir == null) {
      return;
    }

    double noise = getNoise(x, y, z);
    float rnd = random.nextFloat();

    if (rnd >= noise) {
      return;
    }

    Material type = getBlockType(x, y, z);
    if (type == null) {
      return;
    }

    BlockIteration mat = getMaterial(type);
    int size = random.nextBoolean() ? 1 : 2;

    if (mat == null) {
      return;
    }

    Direction[] potentialDirections;

    if (random.nextBoolean()) {
      placeStair(x, y, z, mat, edgeDir.opposite());

      potentialDirections = arr2;
      arr2[0] = edgeDir.left();
      arr2[1] = edgeDir.right();
    } else {
      setBlock(x, y, z, mat.getSlab().createBlockData());

      potentialDirections = arr3;
      arr3[0] = edgeDir.left();
      arr3[1] = edgeDir.right();
      arr3[2] = edgeDir.opposite();
    }

    if (size == 1) {
      return;
    }

    if (random.nextFloat() < 0.1f) {
      BlockData data = COBBLESTONE_SLAB.createBlockData();
      int slabX = x - edgeDir.getMod().x();
      int slabZ = z - edgeDir.getMod().z();

      if (random.nextFloat() < 0.25) {
        if (random.nextBoolean()) {
          slabX = 0;
        } else  {
          slabZ = 0;
        }
      }

      if (isGroundBlock(slabX, y, slabZ)) {
        setBlock(slabX, y + 1, slabZ, data);
      }
    }

    for (int i = 0; i < potentialDirections.length; i++) {
      Direction dir = potentialDirections[i];
      int nx = dir.getMod().x() + x;
      int nz = dir.getMod().z() + z;

      if (!isGroundBlock(nx, y, nz)) {
        continue;
      }

      Material b = getBlockType(x, y, z);
      BlockIteration dirMat;

      if (b == null) {
        dirMat = mat;
      } else {
        dirMat = getMaterial(b);
        if (dirMat == null) {
          dirMat = mat;
        }
      }

      if (i == 2 || random.nextBoolean()) {
        if (random.nextBoolean()) {
          dir = edgeDir.opposite();
        } else if (i < 2 && potentialDirections.length == 2) {
          if (i == 0) {
            placeStair(nx, y, nz, dirMat, dir, Shape.INNER_LEFT);
          } else {
            placeStair(nx, y, nz, dirMat, dir, Shape.INNER_RIGHT);
          }

          continue;
        }

        placeStair(nx, y, nz, dirMat, dir);
      } else {
        setBlock(nx, y, nz, dirMat.getSlab().createBlockData());
      }
    }
  }

  void placeStair(int x, int y, int z, BlockIteration mat, Direction direction) {
    placeStair(x, y, z, mat, direction, Shape.STRAIGHT);
  }

  void placeStair(int x, int y, int z, BlockIteration mat, Direction direction, Shape shape) {
    Stairs stair = (Stairs) mat.getStairs().createBlockData();
    stair.setFacing(direction.asBlockFace());
    stair.setWaterlogged(false);
    stair.setShape(shape);

    setBlock(x, y, z, stair);
  }
}
