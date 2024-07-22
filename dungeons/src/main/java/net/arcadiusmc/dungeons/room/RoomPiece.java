package net.arcadiusmc.dungeons.room;

import net.arcadiusmc.dungeons.DungeonPiece;
import net.arcadiusmc.dungeons.LevelBiome;
import net.arcadiusmc.dungeons.PieceVisitor;
import net.arcadiusmc.dungeons.PieceVisitor.Result;
import net.forthecrown.nbt.CompoundTag;

public class RoomPiece extends DungeonPiece {

  /* ---------------------------- CONSTRUCTORS ---------------------------- */

  public RoomPiece(RoomType type) {
    super(type);
  }

  public RoomPiece(RoomType piece, CompoundTag tag) {
    super(piece, tag);
  }

  /* ------------------------------ METHODS ------------------------------- */

  @Override
  public String getPaletteName(LevelBiome biome) {
    return getType().getPalette(biome);
  }

  @Override
  public RoomType getType() {
    return (RoomType) super.getType();
  }

  @Override
  protected Result onVisit(PieceVisitor walker) {
    return walker.onRoom(this);
  }

  /* --------------------------- SERIALIZATION ---------------------------- */

  @Override
  protected void saveAdditional(CompoundTag tag) {

  }
}