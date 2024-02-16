package net.arcadiusmc.punish;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.bukkit.Material;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
@Getter
@Accessors(fluent = true)
public class PunishConfig {
  private boolean broadcastNotesOnJoin = true;
  private boolean announcePunishmentExpirations = true;
  private boolean overrideIpBannedMotd = true;
  private Material[] oreMinerMaterials = {};
}
