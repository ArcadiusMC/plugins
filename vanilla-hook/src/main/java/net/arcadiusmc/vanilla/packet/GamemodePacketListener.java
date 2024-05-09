package net.arcadiusmc.vanilla.packet;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import java.util.UUID;
import net.arcadiusmc.packet.PacketCall;
import net.arcadiusmc.packet.PacketHandler;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Action;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Entry;
import net.minecraft.world.level.GameType;

/**
 * This stops the tab list from displaying users in gray and italic so people don't know who's in
 * spectator
 */
class GamemodePacketListener {

  @PacketHandler(ignoreCancelled = true)
  public void onGameModePacket(ClientboundPlayerInfoUpdatePacket packet, PacketCall call) {
    var actions = packet.actions();

    if (!actions.contains(Action.UPDATE_GAME_MODE) && !actions.contains(Action.ADD_PLAYER)) {
      return;
    }

    UUID target = call.getPlayer().getUniqueId();
    List<Entry> entries = new ObjectArrayList<>(packet.entries());

    var it = entries.listIterator();

    boolean changed = false;

    while (it.hasNext()) {
      var n = it.next();

      if (n.profileId().equals(target)) {
        continue;
      }

      if (n.gameMode() != GameType.SPECTATOR) {
        continue;
      }

      changed = true;
      it.set(
          new ClientboundPlayerInfoUpdatePacket.Entry(
              n.profileId(),
              n.profile(),
              n.listed(),
              n.latency(),
              GameType.DEFAULT_MODE,
              n.displayName(),
              n.chatSession()
          )
      );
    }

    if (!changed) {
      return;
    }

    ClientboundPlayerInfoUpdatePacket replacementPacket
        = new ClientboundPlayerInfoUpdatePacket(actions, entries);

    call.setReplacementPacket(replacementPacket);
  }
}
