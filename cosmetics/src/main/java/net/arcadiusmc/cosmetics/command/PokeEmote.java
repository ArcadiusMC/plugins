package net.arcadiusmc.cosmetics.command;

import net.arcadiusmc.user.User;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class PokeEmote extends Emote {

  public PokeEmote() {
    super("poke");
    setDescription("Poke people back a bit");
    register();
  }

  @Override
  public void emoteSelf(User user) {
    sendSelfMessage(user);
    user.getWorld().playSound(user.getLocation(), Sound.ENCHANT_THORNS_HIT, 3.0F, 1.8F);
  }

  @Override
  public void emote(User sender, User target) {
    sendMessages(sender, target);

    if (target.getPlayer().getGameMode() == GameMode.SPECTATOR) {
      return;
    }

    Location targetLoc = target.getLocation();
    Player targetPlayer = target.getPlayer();

    targetLoc.getWorld().playSound(target.getLocation(), Sound.ENCHANT_THORNS_HIT, 3.0F, 1.8F);

    targetPlayer.setVelocity(
        targetPlayer.getVelocity()
            .add(target.getLocation()
                .getDirection()
                .normalize()
                .multiply(-0.3)
                .setY(.1)
            )
    );
  }
}
