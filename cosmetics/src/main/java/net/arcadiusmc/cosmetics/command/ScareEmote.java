package net.arcadiusmc.cosmetics.command;

import java.time.Duration;
import net.arcadiusmc.user.User;
import net.arcadiusmc.utils.Tasks;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class ScareEmote extends Emote {

  public ScareEmote() {
    super("scare");

    setAliases("spook");
    setDescription("Scare another player!");

    register();
  }

  @Override
  protected Duration cooldownDuration() {
    return Duration.ofSeconds(30);
  }

  @Override
  public void emoteSelf(User user) {
    scare(user.getPlayer());
  }

  @Override
  public void emote(User sender, User target) {
    sendMessages(sender, target);
    scare(target.getPlayer());
  }

  private void scare(Player player) {
    Location loc = player.getLocation();
    player.spawnParticle(Particle.ELDER_GUARDIAN, loc.getX(), loc.getY(), loc.getZ(), 1);

    Tasks.runLater(() -> {
      player.playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.MASTER, 2.0F, 1F);
      player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 9, false, false, false));

      for (int i = 0; i < 3; i++) {
        Tasks.runLater(() -> {
          player.playSound(loc, Sound.ENTITY_ENDERMAN_SCREAM, SoundCategory.MASTER, 1.5F, 1F);
        }, i * 3L);
      }
    }, 3L);
  }
}
