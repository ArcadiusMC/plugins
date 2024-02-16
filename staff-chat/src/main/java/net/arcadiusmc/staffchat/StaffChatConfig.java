package net.arcadiusmc.staffchat;

import lombok.Getter;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
@Getter
public class StaffChatConfig {
  private String discordChannel = "";
}
