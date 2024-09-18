package net.arcadiusmc.cosmetics.command;

import static net.arcadiusmc.cosmetics.command.JingleEmote.JingleNote.BASS;
import static net.arcadiusmc.cosmetics.command.JingleEmote.JingleNote.SNARE;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import net.arcadiusmc.Cooldowns;
import net.arcadiusmc.user.User;
import net.arcadiusmc.utils.Tasks;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;

public class JingleEmote extends Emote {

  static final Duration LENGTH = Duration.ofSeconds(6);

  public JingleEmote() {
    super("jingle");
    setDescription("Send other players a sick christmas beat!");
    register();
  }

  @Override
  protected Duration cooldownDuration() {
    return LENGTH;
  }

  @Override
  public void emoteSelf(User user) {
    Cooldowns.cooldowns().cooldown(user.getUniqueId(), LENGTH);
    jingle(user);
  }

  @Override
  public void emote(User sender, User target) {
    sendMessages(sender, target);
    jingle(target);
  }

  //Jingle, by the illustrious composer Woutzart xD
  private void jingle(User user) {
    Location loc = user.getPlayer().getLocation();
    loc.getWorld().spawnParticle(Particle.ITEM_SNOWBALL, loc, 25, 0.1, 0, 0.1, 1);
    loc.getWorld().spawnParticle(Particle.END_ROD, loc, 50, 0.1, 0, 0.1, 0.1);

    new JinglePlayer(loc).next();
  }

  @RequiredArgsConstructor(staticName = "note")
  static class JingleNote {

    static final byte
        SNARE = 0,
        BASS = 1,
        NONE = -1;

    static final float
        MID_TONE = 1.5f,
        HIGH_TONE = 1.7f;

    private static final JingleNote[] NOTES = {
        note(8, MID_TONE, BASS),
        note(4, MID_TONE, SNARE),
        note(4, MID_TONE, BASS),

        note(8, MID_TONE, BASS),
        note(4, MID_TONE, SNARE),
        note(4, MID_TONE, BASS),

        note(8, MID_TONE, BASS),
        note(4, 1.8f, SNARE),
        note(4, 1.2f, BASS),
        note(4, MID_TONE, SNARE),

        note(4, MID_TONE, BASS),
        note(4, MID_TONE, SNARE),
        note(4, MID_TONE, BASS),

        note(8, HIGH_TONE, BASS),
        note(4, HIGH_TONE, SNARE),
        note(4, HIGH_TONE, BASS),

        note(6, HIGH_TONE, SNARE),
        note(2, HIGH_TONE, BASS),
        note(4, MID_TONE, SNARE),
        note(4, MID_TONE, BASS),

        note(8, MID_TONE, BASS),
        note(4, 1.3f, SNARE),
        note(4, 1.3f, SNARE),
        note(4, HIGH_TONE, SNARE),
        note(4, MID_TONE, SNARE),

        note(8, 2.0f, NONE),
    };

    private final int delay;
    private final float pitch;
    private final byte instrument;
  }

  @RequiredArgsConstructor
  static class JinglePlayer implements Runnable {

    private final Location location;
    private int index = 0;

    @Override
    public void run() {
      var note = JingleNote.NOTES[index];
      play(note);

      index++;

      if (index < JingleNote.NOTES.length) {
        next();
      }
    }

    void next() {
      Tasks.runLater(this, JingleNote.NOTES[index].delay);
    }

    void play(JingleNote note) {
      if (note.instrument == BASS) {
        location.getWorld().playSound(
            location,
            Sound.BLOCK_NOTE_BLOCK_BASEDRUM,
            SoundCategory.MASTER,
            0.2F, 1F
        );
      } else if (note.instrument == SNARE) {
        location.getWorld().playSound(
            location,
            Sound.BLOCK_NOTE_BLOCK_SNARE,
            SoundCategory.MASTER,
            1F, 1F
        );
      }

      location.getWorld().playSound(
          location,
          Sound.BLOCK_NOTE_BLOCK_BELL,
          SoundCategory.MASTER,
          1F, note.pitch
      );
    }
  }
}
