package net.arcadiusmc.dungeons.gen;

import java.util.List;
import java.util.Random;
import net.arcadiusmc.dungeons.DungeonPiece;
import net.arcadiusmc.utils.math.Direction;
import net.forthecrown.grenadier.types.BlockFilterArgument;
import net.forthecrown.grenadier.types.BlockFilterArgument.Result;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;

public abstract class Decorator<C> {

  protected DungeonGenerator generator;
  protected Random random;

  protected final C config;

  public Decorator(C config) {
    this.config = config;
  }

  final void bind(DungeonGenerator generator) {
    this.generator = generator;
    this.random = generator.getRandom();

    onBind();
  }

  protected void onBind() {

  }

  protected void runForEachBlock() {
    if (!(this instanceof XyzFunction function)) {
      return;
    }

    forEachBlock(function);
  }

  protected void forEachBlock(XyzFunction function) {
    generator.getBoundsSet().forEachBlock(function);
  }

  protected boolean matchesAny(int x, int y, int z, List<BlockFilterArgument.Result> filters) {
    BlockState state = getBlock(x, y, z);

    if (state == null) {
      return false;
    }

    return matchesAny(state, filters);
  }

  protected boolean matchesAny(BlockState state, List<BlockFilterArgument.Result> filters) {
    for (int i = 0; i < filters.size(); i++) {
      Result filter = filters.get(i);

      if (!filter.test(state)) {
        continue;
      }

      return true;
    }

    return false;
  }

  protected <T> T randomFrom(List<T> list) {
    if (list == null || list.isEmpty()) {
      return null;
    }
    if (list.size() == 1) {
      return list.getFirst();
    }

    return list.get(random.nextInt(list.size()));
  }

  protected float randomFloat() {
    return random.nextFloat();
  }

  protected int randomInt(int upperExc) {
    return random.nextInt(upperExc);
  }

  protected int randomInt(int lowerInc, int upperExc) {
    return random.nextInt(lowerInc, upperExc);
  }

  protected boolean randomBool() {
    return random.nextBoolean();
  }

  protected boolean randomBool(double chance) {
    if (chance >= 1.0) {
      return true;
    }

    return randomFloat() < chance;
  }

  protected DungeonPiece getRootPiece() {
    return generator.getRootPiece();
  }

  public boolean isSupportedByAnyFace(int x, int y, int z) {
    return generator.isSupportedByAnyFace(x, y, z);
  }

  protected List<GeneratorFunction> getFunctionsIn(String functionKey, DungeonPiece piece) {
    return generator.getFunctionsIn(functionKey, piece);
  }

  protected List<GeneratorFunction> getFunctions(String functionType) {
    return generator.getFunctions(functionType);
  }

  public int freeSpaceDown(int x, int y, int z) {
    return generator.freeSpaceDown(x, y, z);
  }

  protected boolean hasSupport(int x, int y, int z, BlockFace face) {
    return generator.hasSupport(x, y, z, face);
  }

  protected boolean isVoidBelow(int x, int y, int z) {
    return generator.isVoidBelow(x, y, z);
  }

  protected boolean isSkyAbove(int x, int y, int z) {
    return generator.isSkyAbove(x, y, z);
  }

  protected boolean isAir(int x, int y, int z) {
    return generator.isAir(x, y, z);
  }

  protected boolean isGroundBlock(int x, int y, int z) {
    return generator.isGroundBlock(x, y, z);
  }

  protected BlockState getBlock(int x, int y, int z) {
    return generator.getBlock(x, y, z);
  }

  protected BlockData getBlockData(int x, int y, int z) {
    return generator.getBlockData(x, y, z);
  }

  protected Material getBlockType(int x, int y, int z) {
    return generator.getBlockType(x, y, z);
  }

  protected boolean isTagged(int x, int y, int z, Tag<Material> tag) {
    return generator.isTagged(x, y, z, tag);
  }

  protected boolean matchesBlock(int x, int y, int z, Material... anyOf) {
    return generator.matchesBlock(x, y, z, anyOf);
  }

  protected void setBlock(int x, int y, int z, BlockData data) {
    generator.setBlock(x, y, z, data);
  }

  protected void clearBlock(int x, int y, int z) {
    generator.clearBlock(x, y, z);
  }

  protected void mossify(int x, int y, int z) {
    generator.mossify(x, y, z);
  }

  protected boolean isEdge(int x, int y, int z) {
    return generator.isEdge(x, y, z);
  }

  protected boolean isWallBlock(int x, int y, int z) {
    return generator.isWallBlock(x, y, z);
  }

  protected Direction edgeDirection(int x, int y, int z) {
    return generator.edgeDirection(x, y, z);
  }

  protected Direction wallDirection(int x, int y, int z) {
    return generator.wallDirection(x, y, z);
  }

  public abstract void execute();
}
