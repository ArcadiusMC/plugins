package net.arcadiusmc.scripts.builtins;

import net.arcadiusmc.scripts.Scripts;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.Sound.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

public enum PlaySoundFunction implements ProxyExecutable {
  PLAY_SOUND;

  @Override
  public Object execute(Value... arguments) {
    Scripts.ensureParameterCount(arguments, 2);

    Audience audience = Scripts.toSource(arguments[0]);
    Key soundId;
    Value arg2 = arguments[1];

    if (arg2.isString()) {
      soundId = Key.key(arg2.asString());
    } else if (Scripts.isInstance(arg2, Key.class)) {
      soundId = arg2.asHostObject();
    } else {
      soundId = Key.key(String.valueOf(arg2));
    }

    float volume = 1;
    float pitch = 1;
    Source source = Source.MASTER;

    if (arguments.length > 2) {
      volume = arguments[2].asFloat();
    }
    if (arguments.length > 3) {
      pitch = arguments[3].asFloat();
    }
    if (arguments.length > 4) {
      String str = arguments[4].asString();

      source = switch (str) {
        case "music" -> Source.MUSIC;
        case "record" -> Source.RECORD;
        case "weather" -> Source.WEATHER;
        case "block" -> Source.BLOCK;
        case "hostile" -> Source.HOSTILE;
        case "neutral" -> Source.NEUTRAL;
        case "player" -> Source.PLAYER;
        case "ambient" -> Source.AMBIENT;
        case "voice" -> Source.VOICE;

        default -> Source.MASTER;
      };
    }

    Sound sound = Sound.sound()
        .type(soundId)
        .volume(volume)
        .pitch(pitch)
        .source(source)
        .build();

    audience.playSound(sound);
    return null;
  }
}
