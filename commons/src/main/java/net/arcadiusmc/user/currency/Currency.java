package net.arcadiusmc.user.currency;

import java.util.UUID;
import net.arcadiusmc.text.Text;
import net.kyori.adventure.text.Component;

public interface Currency {

  default float getGainMultiplier(UUID playerId) {
    return 1.0F;
  }

  String singularName();

  String pluralName();

  Component format(int amount);

  default Component format(int amount, float multiplier) {
    if (multiplier > 1) {
      return Text.format("{0} ({1, number}x multiplier)", format(amount), multiplier);
    }

    return format(amount);
  }

  int get(UUID playerId);

  void set(UUID playerId, int value);

  default void add(UUID playerId, int value) {
    set(playerId, get(playerId) + value);
  }

  default void remove(UUID playerId, int value) {
    set(playerId, get(playerId) - value);
  }
}
