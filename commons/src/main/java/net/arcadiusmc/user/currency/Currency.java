package net.arcadiusmc.user.currency;

import java.util.UUID;
import net.kyori.adventure.text.Component;

public interface Currency {

  String singularName();

  String pluralName();

  Component format(int amount);

  int get(UUID playerId);

  void set(UUID playerId, int value);

  default void add(UUID playerId, int value) {
    set(playerId, get(playerId) + value);
  }

  default void remove(UUID playerId, int value) {
    set(playerId, get(playerId) - value);
  }
}
