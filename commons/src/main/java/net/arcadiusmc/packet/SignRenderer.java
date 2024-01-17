package net.arcadiusmc.packet;

import net.arcadiusmc.utils.math.WorldVec3i;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;

public interface SignRenderer {

  boolean test(Player player, WorldVec3i pos, Sign sign);

  void render(Player player, WorldVec3i pos, Sign sign);
}
