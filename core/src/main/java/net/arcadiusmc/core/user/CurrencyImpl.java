package net.arcadiusmc.core.user;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.text.loader.MessageRef;
import net.arcadiusmc.user.currency.Currency;
import net.arcadiusmc.utils.ScoreIntMap;
import net.kyori.adventure.text.Component;

@RequiredArgsConstructor
public class CurrencyImpl implements Currency {

  private final ScoreIntMap<UUID> map;
  private final MessageRef singular;
  private final MessageRef plural;
  private final MessageRef format;

  @Override
  public String singularName() {
    return Text.plain(singular.get().create(null));
  }

  @Override
  public String pluralName() {
    return Text.plain(plural.get().create(null));
  }

  @Override
  public Component format(int amount) {
    return format.get()
        .addValue("amount", amount)
        .addValue("unit", amount == 1 ? singularName() : pluralName())
        .create(null);
  }

  @Override
  public int get(UUID playerId) {
    return map.get(playerId);
  }

  @Override
  public void set(UUID playerId, int value) {
    map.set(playerId, value);
  }
}
