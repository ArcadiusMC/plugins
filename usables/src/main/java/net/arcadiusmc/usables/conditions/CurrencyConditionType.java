package net.arcadiusmc.usables.conditions;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import java.util.Objects;
import lombok.Getter;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.types.ArgumentTypes;
import net.forthecrown.grenadier.types.IntRangeArgument.IntRange;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.usables.Condition;
import net.arcadiusmc.usables.Interaction;
import net.arcadiusmc.usables.UsableCodecs;
import net.arcadiusmc.usables.Usables;
import net.arcadiusmc.usables.ObjectType;
import net.arcadiusmc.user.currency.Currency;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
public class CurrencyConditionType implements ObjectType<CurrencyCondition> {

  private final Currency currency;
  private final String currencyName;

  public CurrencyConditionType(Currency currency) {
    Objects.requireNonNull(currency);
    this.currency = currency;
    this.currencyName = currency.pluralName();
  }

  @Override
  public CurrencyCondition parse(StringReader reader, CommandSource source)
      throws CommandSyntaxException
  {
    IntRange range = ArgumentTypes.intRange().parse(reader);

    if (range.isExact()) {
      range = IntRange.atLeast(range.min().orElse(0));
    }

    return new CurrencyCondition(range, this);
  }

  @Override
  public @NotNull <S> DataResult<CurrencyCondition> load(@Nullable Dynamic<S> dynamic) {
    return UsableCodecs.INT_RANGE.decode(dynamic)
        .map(Pair::getFirst)
        .map(intRange -> new CurrencyCondition(intRange, this));
  }

  @Override
  public <S> DataResult<S> save(@NotNull CurrencyCondition value, @NotNull DynamicOps<S> ops) {
    return UsableCodecs.INT_RANGE.encodeStart(ops, value.getRange());
  }
}

@Getter
class CurrencyCondition implements Condition {

  private final IntRange range;
  private final CurrencyConditionType type;

  public CurrencyCondition(IntRange range, CurrencyConditionType type) {
    this.range = range;
    this.type = type;
  }

  @Override
  public boolean test(Interaction interaction) {
    return interaction.getPlayerId()
        .map(playerId -> {
          int value = type.getCurrency().get(playerId);
          return range.contains(value);
        })
        .orElse(false);
  }

  @Override
  public Component failMessage(Interaction interaction) {
    return Text.format("You need {0} {1}", NamedTextColor.GRAY,
        Usables.boundsDisplay(range), type.getCurrencyName()
    );
  }

  @Override
  public @Nullable Component displayInfo() {
    return Component.text(range.toString());
  }
}