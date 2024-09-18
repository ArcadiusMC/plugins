package net.arcadiusmc.cosmetics.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.function.Consumer;
import net.arcadiusmc.Cooldowns;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.user.User;
import net.arcadiusmc.utils.Cooldown;
import net.arcadiusmc.utils.Tasks;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.scheduler.BukkitTask;

public class HugEmote extends Emote {

  private static final int HUG_TICKS = 100;
  private static final String COOLDOWN = "emote_hug_received";

  public HugEmote() {
    super("hug");
  }

  @Override
  public void emoteSelf(User user) {
    sendSelfMessage(user);
    spawnParticles(user.getLocation());
  }

  @Override
  public void emote(User sender, User target) throws CommandSyntaxException {
    Cooldowns cooldowns = Cooldowns.cooldowns();

    if (cooldowns.onCooldown(target.getUniqueId(), COOLDOWN)) {
      throw Messages.render("cosmetics.emotes", messageKey, "alreadyHugged")
          .addValue("sender", sender)
          .addValue("target", target)
          .exception(sender);
    }

    sendMessages(sender, target);
    cooldowns.cooldown(target.getUniqueId(), COOLDOWN, HUG_TICKS);
    Tasks.runTimer(new HugTask(target), 0, 2);
  }

  static void spawnParticles(Location l) {
    Particle.HEART.builder()
        .location(l.add(0, 0.5, 0))
        .count(3)
        .offset(0.25, 0.25, 0.25)
        .spawn();
  }

  public static class HugTask implements Consumer<BukkitTask> {

    private int i = 0;
    private final User user;

    public HugTask(User user) {
      this.user = user;
    }

    @Override
    public void accept(BukkitTask task) {
      if (i == HUG_TICKS || !user.isOnline()) {
        Tasks.cancel(task);
        Cooldown.remove(user, COOLDOWN);

        return;
      }

      if (user.getGameMode() != GameMode.SPECTATOR) {
        spawnParticles(user.getLocation());
      }

      i++;
    }
  }
}
