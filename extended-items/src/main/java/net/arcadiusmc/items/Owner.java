package net.arcadiusmc.items;

import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import net.forthecrown.nbt.CompoundTag;

public class Owner extends ItemComponent {

  @Getter @Setter
  private UUID playerId;

  @Override
  public void save(CompoundTag tag) {
    if (playerId == null) {
      return;
    }

    tag.putUUID("owner_id", playerId);
  }

  @Override
  public void load(CompoundTag tag) {
    if (!tag.contains("owner_id")) {
      playerId = null;
      return;
    }

    playerId = tag.getUUID("owner_id");
  }
}
