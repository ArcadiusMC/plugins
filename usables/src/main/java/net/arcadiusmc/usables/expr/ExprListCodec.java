package net.arcadiusmc.usables.expr;

import com.google.common.base.Strings;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.arcadiusmc.registry.Holder;
import net.arcadiusmc.registry.Registry;
import net.arcadiusmc.usables.Action;
import net.arcadiusmc.usables.Condition;
import net.arcadiusmc.usables.ObjectType;
import net.arcadiusmc.usables.UsableComponent;
import net.arcadiusmc.usables.UsablesPlugin;
import net.arcadiusmc.usables.list.ActionsList;
import net.arcadiusmc.usables.list.ComponentList;
import net.arcadiusmc.usables.list.ConditionsList;
import net.arcadiusmc.utils.io.ExtraCodecs;
import net.arcadiusmc.utils.io.Results;
import net.arcadiusmc.utils.io.TagOps;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.Grenadier;
import net.forthecrown.nbt.BinaryTag;
import net.forthecrown.nbt.BinaryTags;
import net.forthecrown.nbt.CompoundTag;
import org.bukkit.Bukkit;

public class ExprListCodec {

  public static final Codec<ActionsList> ACTIONS_LIST_CODEC;
  public static final Codec<ConditionsList> CONDITIONS_LIST_CODEC;

  public static final Codec<ExprList> LIST_CODEC;

  public static final MapCodec<Action> ACTION_STRING_CODEC
      = componentCodec(UsablesPlugin.get().getActions());

  public static final MapCodec<Condition> CONDITION_STRING_CODEC
      = componentCodec(UsablesPlugin.get().getConditions());

  static final Codec<ConditionEntry> ENTRY_CODEC = RecordCodecBuilder.create(instance -> {
    return instance
        .group(
            CONDITION_STRING_CODEC.forGetter(ConditionEntry::condition),

            Codec.STRING.optionalFieldOf("error-message", "")
                .forGetter(ConditionEntry::errorMessage)
        )
        .apply(instance, ConditionEntry::new);
  });

  public static final Codec<ConditionsList> CONDITION_STRING_LIST = ENTRY_CODEC
      .listOf()
      .xmap(
          conditionEntries -> {
            ConditionsList list = new ConditionsList();
            for (int i = 0; i < conditionEntries.size(); i++) {
              ConditionEntry entry = conditionEntries.get(i);
              list.add(entry.condition, i);
              list.setError(i, entry.errorMessage);
            }
            return list;
          },
          conditions -> {
            List<ConditionEntry> entries = new ArrayList<>();
            for (int i = 0; i < conditions.size(); i++) {
              Condition condition = conditions.get(i);
              String error = conditions.getError(i);
              ConditionEntry entry = new ConditionEntry(condition, error);
              entries.add(entry);
            }
            return entries;
          }
      );

  public static final Codec<ActionsList> ACTIONS_STRING_LIST = ACTION_STRING_CODEC.codec()
      .listOf()
      .xmap(
          actions -> {
            ActionsList list = new ActionsList();
            for (Action action : actions) {
              list.addLast(action);
            }
            return list;
          },
          actions -> {
            List<Action> actionList = new ArrayList<>();
            for (Action action : actions) {
              actionList.add(action);
            }
            return actionList;
          }
      );

  public record ConditionEntry(Condition condition, String errorMessage) {

    public ConditionEntry(Condition condition, String errorMessage) {
      this.condition = condition;
      this.errorMessage = Strings.nullToEmpty(errorMessage);
    }
  }

  static {
    ACTIONS_LIST_CODEC = new ComponentListCodec<>(ActionsList::new);
    CONDITIONS_LIST_CODEC = new ComponentListCodec<>(ConditionsList::new);

    LIST_CODEC = new Codec<>() {
      @Override
      public <T> DataResult<Pair<ExprList, T>> decode(DynamicOps<T> ops, T input) {
        BinaryTag tag = ops.convertTo(TagOps.OPS, input);

        if (!tag.isCompound()) {
          return Results.error("Not an object value: %s", input);
        }

        ExprList list = new ExprList();
        list.load(tag.asCompound());

        return Results.success(Pair.of(list, input));
      }

      @Override
      public <T> DataResult<T> encode(ExprList input, DynamicOps<T> ops, T prefix) {
        CompoundTag tag = BinaryTags.compoundTag();
        input.save(tag);
        T converted = TagOps.OPS.convertTo(ops, tag);
        return Results.success(converted);
      }
    };
  }


  static <U extends UsableComponent> MapCodec<U> componentCodec(
      Registry<ObjectType<? extends U>> registry
  ) {
    return new MapCodec<>() {
      @Override
      public <T> Stream<T> keys(DynamicOps<T> ops) {
        return registry.keys().stream().map(ops::createString);
      }

      @Override
      public <T> DataResult<U> decode(DynamicOps<T> ops, MapLike<T> input) {
        for (Holder<ObjectType<? extends U>> entry : registry.entries()) {
          T value = input.get(entry.getKey());
          if (value == null) {
            continue;
          }

          ObjectType<U> type = (ObjectType<U>) entry.getValue();
          return ops.getStringValue(value)
              .flatMap(s -> {
                CommandSource source = Grenadier.createSource(Bukkit.getConsoleSender());
                return ExtraCodecs.safeParse(s, reader -> type.parse(reader, source));
              });
        }

        return Results.error("Unknown type");
      }

      @Override
      public <T> RecordBuilder<T> encode(
          U input,
          DynamicOps<T> ops,
          RecordBuilder<T> prefix
      ) {
        if (input.getType() == null) {
          return prefix;
        }

        ObjectType<U> type = (ObjectType<U>) input.getType();
        Optional<String> keyOpt = registry.getKey(type);

        if (keyOpt.isEmpty()) {
          return prefix;
        }

        String key = keyOpt.get();
        DataResult<T> result = type.save(input, ops);

        return prefix.add(key, result);
      }
    };
  }

  static class ComponentListCodec<T extends ComponentList<?>> implements Codec<T> {

    final Supplier<T> supplier;

    public ComponentListCodec(Supplier<T> supplier) {
      this.supplier = supplier;
    }

    @Override
    public <S> DataResult<Pair<T, S>> decode(DynamicOps<S> ops, S input) {
      T value = supplier.get();
      value.load(new Dynamic<>(ops, input));
      return Results.success(Pair.of(value, input));
    }

    @Override
    public <S> DataResult<S> encode(T input, DynamicOps<S> ops, S prefix) {
      return input.save(ops);
    }
  }
}
