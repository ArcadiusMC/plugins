package net.arcadiusmc.dungeons.room;

import java.util.LinkedList;
import java.util.List;
import lombok.Getter;
import net.arcadiusmc.dungeons.DungeonLevel;
import net.arcadiusmc.dungeons.DungeonPiece;
import net.arcadiusmc.dungeons.LevelBiome;
import net.arcadiusmc.dungeons.PieceVisitor;
import net.arcadiusmc.dungeons.PieceVisitor.Result;
import net.forthecrown.nbt.CompoundTag;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class RoomPiece extends DungeonPiece {

  @Getter
  private final List<Player> players = new LinkedList<>();

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

  public void onTick(World world, DungeonLevel level) {
  }

  public void onIdleTick(World world, DungeonLevel level) {
  }

  public void onEnter(Player user, DungeonLevel level) {
  }

  public void onExit(Player user, DungeonLevel level) {
  }

  /* --------------------------- SERIALIZATION ---------------------------- */

  @Override
  protected void saveAdditional(CompoundTag tag) {

  }
}