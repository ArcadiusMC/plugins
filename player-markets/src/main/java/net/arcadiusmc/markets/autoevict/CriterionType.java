package net.arcadiusmc.markets.autoevict;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Objects;
import lombok.Getter;
import net.arcadiusmc.utils.io.ExtraCodecs;
import net.kyori.adventure.text.Component;

@Getter
public abstract class CriterionType<V> {

  private final Codec<V> valueCodec;

  public CriterionType(Codec<V> valueCodec) {
    Objects.requireNonNull(valueCodec, "Null value codec");
    this.valueCodec = valueCodec;
  }

  public abstract boolean test(V value, MarketScanResult result);

  public abstract Component getReasonDisplay(V value);

  public abstract String toString(V value);

  public DataResult<V> fromString(String string) {
    ArgumentType<V> argumentType = argumentType();
    return ExtraCodecs.safeParse(string, argumentType);
  }

  protected abstract ArgumentType<V> argumentType();
}
