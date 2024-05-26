package net.arcadiusmc.voicechat;

import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import de.maxhenkel.voicechat.api.packets.MicrophonePacket;
import javax.sound.sampled.AudioFormat;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.utils.Tasks;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.slf4j.Logger;

public class HookVcPlugin implements VoicechatPlugin {

  static final double THRESHOLD = 175;

  public static final int SAMPLE_RATE = 48000;
  public static final int FRAME_SIZE = (SAMPLE_RATE / 1000) * 20;

  private static final Logger LOGGER = Loggers.getLogger();

  private final HookPlugin plugin;
  private OpusDecoder decoder;
  private AudioFormat format;

  public HookVcPlugin(HookPlugin plugin) {
    this.plugin = plugin;
  }

  @Override
  public String getPluginId() {
    return "arcadius_voice_chat_hook";
  }

  @Override
  public void initialize(VoicechatApi api) {
    decoder = api.createDecoder();
  }

  @Override
  public void registerEvents(EventRegistration registration) {
    registration.registerEvent(MicrophonePacketEvent.class, this::onSound);
  }

  private double calculateAverageAmplitude(short[] audioData) {
    double average = 0;
    for (short sample : audioData) {
      average += sample;
    }
    return average / audioData.length;
    /*double sumOfSquares = 0;
    int len = audioData.length;

    for (int i = 0; i < len; i++) {
      double sample = toLittleEndian(audioData[i]);
      sumOfSquares += sample * sample;
    }

    double rms = Math.sqrt(sumOfSquares / len);
    return 20 * Math.log10(rms / Short.MAX_VALUE);*/
  }

  /**
   * Calculates the audio level of a signal with specific samples.
   *
   * @param samples the samples of the signal to calculate the audio level of
   * @param offset  the offset in samples in which the samples start
   * @param length  the length in bytes of the signal in samples starting at offset
   * @return the audio level of the specified signal in db
   */
  public static double calculateAudioLevel(short[] samples, int offset, int length) {
    double rms = 0D; // root mean square (RMS) amplitude

    for (int i = offset; i < length; i++) {
      double sample = (double) samples[i] / (double) Short.MAX_VALUE;
      rms += sample * sample;
    }

    int sampleCount = length / 2;

    rms = (sampleCount == 0) ? 0 : Math.sqrt(rms / sampleCount);

    double db;

    if (rms > 0D) {
      db = Math.min(Math.max(20D * Math.log10(rms), -127D), 0D);
    } else {
      db = -127D;
    }

    return db;
  }

  /**
   * Calculates the highest audio level in packs of 100
   *
   * @param samples the audio samples
   * @return the audio level in db
   */
  public static double getHighestAudioLevel(short[] samples) {
    double highest = -127D;
    for (int i = 0; i < samples.length; i += 100) {
      double level = calculateAudioLevel(samples, i, Math.min(i + 100, samples.length));
      if (level > highest) {
        highest = level;
      }
    }
    return highest;
  }

  private void onSound(MicrophonePacketEvent event) {
    MicrophonePacket packet = event.getPacket();
    byte[] data = packet.getOpusEncodedData();

    if (data.length < 1) {
      return;
    }

    short[] rawAudioData = decoder.decode(data);
    decoder.resetState();

    double highest = Math.abs(getHighestAudioLevel(rawAudioData));
    double hiInv = 127 - highest;

    if (hiInv < plugin.getHookConfig().sculkThreshold()) {
      return;
    }

    Player player = Bukkit.getPlayer(event.getSenderConnection().getPlayer().getUuid());
    World world = player.getWorld();

    Tasks.runSync(() -> {
      Vector vector = player.getEyeLocation().toVector();
      world.sendGameEvent(player, plugin.getHookConfig().sculkEvent(), vector);
    });
  }
}
