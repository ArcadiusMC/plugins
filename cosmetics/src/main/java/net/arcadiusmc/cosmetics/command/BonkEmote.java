package net.arcadiusmc.cosmetics.command;

import net.arcadiusmc.user.User;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;

public class BonkEmote extends Emote {

  public BonkEmote() {
    super("bonk");
    setDescription("Bonks a player for being naughty");
    register();
  }

  @Override
  public void emoteSelf(User user) {
    sendSelfMessage(user);
  }

  @Override
  public void emote(User sender, User target) {
    sendMessages(sender, target);

    if (target.getGameMode() == GameMode.SPECTATOR) {
      return;
    }

    Location loc = target.getLocation();

    target.getPlayer().setRotation(
        loc.getYaw(),
        loc.getPitch() + 20.0F
    );

    target.getWorld().playSound(loc, Sound.ENTITY_SHULKER_HURT_CLOSED, 2.0F, 0.8F);

    Particle.CRIT.builder()
        .location(loc.add(0, 1, 0))
        .count(5)
        .offset(0.5, 0.5, 0.5)
        .spawn();
  }
}
