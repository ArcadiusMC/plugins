package net.arcadiusmc.challenges;

import static net.arcadiusmc.command.Exceptions.format;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.arcadiusmc.user.User;

public interface ChallengeExceptions {

  static CommandSyntaxException nonActiveChallenge(Challenge challenge, User viewer) {
    return format("Challenge {0} is not active!", challenge.displayName(viewer));
  }
}
