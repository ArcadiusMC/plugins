package net.arcadiusmc.markets;

import com.google.common.base.Strings;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.Getter;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.placeholder.Placeholders;
import net.arcadiusmc.utils.io.ExtraCodecs;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;

@Getter
public class ValueModifierList {

  public static final Codec<ValueModifierList> CODEC = Modifier.CODEC.listOf()
      .xmap(
          discounts1 -> {
            ValueModifierList container = new ValueModifierList();
            container.modifiers.addAll(discounts1);
            return container;
          },
          container -> container.modifiers
      );

  private final List<Modifier> modifiers = new ArrayList<>();

  public boolean isEmpty() {
    return modifiers.isEmpty();
  }

  public void addAll(ValueModifierList list) {
    Objects.requireNonNull(list);
    modifiers.addAll(list.modifiers);
  }

  public void tick(Instant now) {
    if (modifiers.isEmpty()) {
      return;
    }

    Iterator<Modifier> it = modifiers.iterator();

    while (it.hasNext()) {
      Modifier n = it.next();

      if (n.ends == null || n.ends.isAfter(now)) {
        continue;
      }

      it.remove();
    }
  }

  public int apply(int base) {
    return (int) apply((float) base);
  }

  public float apply(float base) {
    if (modifiers.isEmpty()) {
      return base;
    }

    Instant now = Instant.now();

    float value = base;
    for (Modifier modifier : modifiers) {
      if (modifier.ends != null && now.isAfter(modifier.ends)) {
        continue;
      }

      value = modifier.op.apply(base, value, modifier.amount);
    }

    return value;
  }

  public void clear() {
    modifiers.clear();
  }

  public void removeTagged(String tag) {
    modifiers.removeIf(modifier -> Objects.equals(tag, modifier.tag));
  }

  public void add(Modifier modifier) {
    Objects.requireNonNull(modifier, "Null modifier");
    modifiers.add(modifier);
  }

  public record Modifier(
      float amount,
      ModifierOp op,
      Instant ends,
      String tag,
      String displayName
  ) {

    public static final Codec<Modifier> CODEC = RecordCodecBuilder.create(instance -> {
      return instance
          .group(
              Codec.FLOAT.fieldOf("amount").forGetter(Modifier::amount),

              ExtraCodecs.enumCodec(ModifierOp.class)
                  .optionalFieldOf("operation", ModifierOp.DISCOUNT_STACKING)
                  .forGetter(Modifier::op),

              ExtraCodecs.INSTANT.optionalFieldOf("end_date")
                  .forGetter(o -> Optional.ofNullable(o.ends)),

              Codec.STRING.optionalFieldOf("tag", "")
                  .forGetter(Modifier::tag),

              Codec.STRING.optionalFieldOf("display_name", "")
                  .forGetter(Modifier::displayName)
          )
          .apply(instance, (amount, op, end, tag, displayName) -> {
            return new Modifier(amount, op, end.orElse(null), tag, displayName);
          });
    });

    public Modifier(float amount, ModifierOp op, Instant ends, String tag, String displayName) {
      this.amount = amount;
      this.op = op;
      this.ends = ends;
      this.tag = Strings.nullToEmpty(tag);
      this.displayName = Strings.nullToEmpty(displayName);
    }

    public Modifier copy() {
      return new Modifier(amount, op, ends, tag, displayName);
    }

    public Component displayText(Audience viewer) {
      Component reason;

      if (Strings.isNullOrEmpty(displayName)) {
        reason = Messages.renderText("markets.modifiers.noDisplay", viewer);
      } else {
        reason = Placeholders.renderString(displayName, viewer);
      }

      float displayAmount = amount;

      if (op == ModifierOp.DISCOUNT_BASE || op == ModifierOp.DISCOUNT_STACKING) {
        displayAmount = -displayAmount;
      }

      return Messages.render("markets", "modifiers", op.name().toLowerCase())
          .addValue("value", (displayAmount * 100f))
          .addValue("reason", reason)
          .create(viewer);
    }
  }

  public enum ModifierOp {
    DISCOUNT_BASE {
      @Override
      float apply(float base, float value, float modifier) {
        return value - (base * modifier);
      }
    },

    DISCOUNT_STACKING {
      @Override
      float apply(float base, float value, float modifier) {
        return value - (value * modifier);
      }
    },

    ADD_MULTIPLIED_BASE {
      @Override
      float apply(float base, float value, float modifier) {
        return value + base * modifier;
      }
    },

    MULTIPLY {
      @Override
      float apply(float base, float value, float modifier) {
        return value * modifier;
      }
    };

    abstract float apply(float base, float value, float modifier);
  }
}
