package net.arcadiusmc.challenges;

import static net.arcadiusmc.Permissions.register;

import org.bukkit.permissions.Permission;

public interface ChallengePermissions {
  Permission CHALLENGES              = register("ftc.challenges");
  Permission CHALLENGES_ADMIN        = register(CHALLENGES, "admin");
}
