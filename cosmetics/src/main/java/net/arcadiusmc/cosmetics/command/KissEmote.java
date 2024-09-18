package net.arcadiusmc.cosmetics.command;

import java.time.Duration;
import java.time.LocalDate;
import java.time.Month;
import java.time.MonthDay;
import net.arcadiusmc.user.User;
import net.arcadiusmc.utils.MonthDayPeriod;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;

public class KissEmote extends Emote {

  private static final MonthDayPeriod VALENTINES = MonthDayPeriod.between(
      MonthDay.of(Month.FEBRUARY, 9),
      MonthDay.of(Month.FEBRUARY, 19)
  );

  public KissEmote() {
    super("smooch");

    setAliases("kiss", "mwah");
    setDescription("Kiss someone <3");

    register();
  }

  @Override
  protected Duration cooldownDuration() {
    if (VALENTINES.contains(LocalDate.now())) {
      return Duration.ZERO;
    }

    return super.cooldownDuration();
  }

  @Override
  public void emote(User sender, User target) {
    sendMessages(sender, target);

    if (target.getPlayer().getGameMode() == GameMode.SPECTATOR) {
      return;
    }

    Location loc = sender.getLocation();
    Location targetLoc = target.getLocation();

    Particle.HEART.builder()
        .location(targetLoc.add(0, 1, 0))
        .count(5)
        .offset(0.5, 0.5, 0.5)

        // Spawn, relocate and spawn again
        .spawn()
        .location(loc.add(0, 1, 0))
        .spawn();

    targetLoc.getWorld().playSound(targetLoc, Sound.ENTITY_PUFFER_FISH_BLOW_UP, 3.0F, 2F);
    loc.getWorld().playSound(loc, Sound.ENTITY_PUFFER_FISH_BLOW_UP, 3.0F, 2F);
  }

  @Override
  public void emoteSelf(User user) {
    sendSelfMessage(user);

    Location loc = user.getLocation();
    user.getPlayer().playSound(loc, Sound.ENTITY_PUFFER_FISH_BLOW_UP, 3.0F, 2F);

    Particle.HEART.builder()
        .location(loc.add(0, 1, 0))
        .count(5)
        .offset(0.5, 0.5, 0.5)
        .spawn();
  }
}
