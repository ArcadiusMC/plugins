package net.arcadiusmc.utils.io;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Codec.ResultFunction;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import lombok.Setter;
import lombok.experimental.Accessors;

public class ExistingObjectCodec<T> {

  private final ImmutableList<CodecField<T, Object>> fields;

  @SuppressWarnings({"unchecked", "rawtypes"})
  ExistingObjectCodec(ImmutableList<CodecField<T, ?>> fields) {
    this.fields = (ImmutableList) fields;
  }

  public static <T> Builder<T> builder() {
    return new Builder<>();
  }

  public static <T> Codec<T> createCodec(Supplier<T> ctor, Consumer<Builder<T>> consumer) {
    ExistingObjectCodec<T> existing = create(consumer);
    return existing.codec(Codec.unit(ctor));
  }

  public static <T> ExistingObjectCodec<T> create(Consumer<Builder<T>> consumer) {
    Builder<T> builder = builder();
    consumer.accept(builder);
    return builder.build();
  }

  public Codec<T> codec(Codec<T> codec) {
    return codec.mapResult(new ResultFunction<>() {
      @Override
      public <S> DataResult<Pair<T, S>> apply(
          DynamicOps<S> ops,
          S input,
          DataResult<Pair<T, S>> result
      ) {
        return result.flatMap(tsPair -> {
          return ExistingObjectCodec.this.decode(ops, input, tsPair.getFirst())
              .map(t -> Pair.of(t, tsPair.getSecond()));
        });
      }

      @Override
      public <S> DataResult<S> coApply(DynamicOps<S> ops, T input, DataResult<S> result) {
        return result.flatMap(s -> ExistingObjectCodec.this.encode(ops, input, s));
      }
    });
  }

  public <S> DataResult<T> decode(Dynamic<S> dynamicLike, T object) {
    return decode(dynamicLike.getOps(), dynamicLike.getValue(), object);
  }

  public <S> DataResult<T> decode(DynamicOps<S> ops, S value, T object) {
    return ops.getMap(value).flatMap(mapLike -> {
      DataResult<T> result = Results.success(object);

      for (CodecField<T, Object> field : fields) {
        S foundValue = mapLike.get(field.name);

        if (foundValue == null) {
          if (field.optional) {
            if (field.defaultValue != null) {
              field.setter.accept(object, field.defaultValue.get());
            }

            continue;
          }

          result = result.apply2(
              (t, o) -> t,
              Results.error("Missing value for field '%s'", field.name)
          );
          continue;
        }

        DataResult<?> fieldResult = field.codec.parse(ops, foundValue)
            .mapError(s -> "Failed to decode field " + field.name + ": " + s);

        result = result.apply2(
            (t, o) -> {
              field.setter.accept(t, o);
              return t;
            },
            fieldResult
        );
      }

      return result;
    });
  }

  public <S> DataResult<S> encode(DynamicOps<S> ops, T object) {
    return encode(ops, object, ops.emptyMap());
  }

  public <S> DataResult<S> encode(DynamicOps<S> ops, T object, S prefix) {
    var builder = ops.mapBuilder();

    for (CodecField<T, Object> field : fields) {
      Object fieldValue = field.getter.apply(object);

      if (field.optional && field.excludeIf.test(fieldValue)) {
        continue;
      } else if (fieldValue == null) {
        builder.add(field.name, Results.error("Missing value for field '%s'", field.name));
        continue;
      }

      DataResult<S> encodeResult = field.codec.encodeStart(ops, fieldValue)
          .mapError(s -> "Failed to encode field " + field.name + ": " + s);

      builder.add(field.name, encodeResult);
    }

    return builder.build(prefix);
  }

  public static class Builder<T> {

    ImmutableList.Builder<FieldBuilder<T, ?>> builder = ImmutableList.builder();

    public <V> FieldBuilder<T, V> field(String name, Codec<V> codec) {
      return addField(name, false, codec);
    }

    public <V> FieldBuilder<T, V> optional(String name, Codec<V> codec) {
      return addField(name, true, codec);
    }

    private <V> FieldBuilder<T, V> addField(String name, boolean optional, Codec<V> codec) {
      FieldBuilder<T, V> builder = new FieldBuilder<>(name, optional, codec);
      this.builder.add(builder);
      return builder;
    }

    public ExistingObjectCodec<T> build() {
      ImmutableList<FieldBuilder<T, ?>> fieldBuilders = builder.build();
      ImmutableList.Builder<CodecField<T, ?>> builder = ImmutableList.builder();

      for (FieldBuilder<T, ?> fieldBuilder : fieldBuilders) {
        if (fieldBuilder.setter == null) {
          throw new IllegalArgumentException("Field " + fieldBuilder.name + " is missing a setter");
        }
        if (fieldBuilder.getter == null) {
          throw new IllegalArgumentException("Field " + fieldBuilder.name + " is missing a getter");
        }

        builder.add(fieldBuilder.build());
      }

      return new ExistingObjectCodec<>(builder.build());
    }
  }

  @Setter
  @Accessors(fluent = true, chain = true)
  public static class FieldBuilder<T, V> {
    final String name;
    final Codec<V> codec;
    final boolean optional;

    Function<T, V> getter;
    BiConsumer<T, V> setter;
    Predicate<V> excludeIf = Objects::isNull;

    Supplier<V> defaultValue;

    FieldBuilder(String name, boolean optional, Codec<V> codec) {
      this.name = name;
      this.codec = codec;
      this.optional = optional;
    }

    public FieldBuilder<T, V> defaultsTo(V value) {
      return defaultValue(() -> value);
    }

    CodecField<T, V> build() {
      Objects.requireNonNull(getter, "Null getter");
      Objects.requireNonNull(setter, "Null setter");
      return new CodecField<>(name, codec, optional, getter, setter, excludeIf, defaultValue);
    }
  }

  record CodecField<T, V>(
      String name,
      Codec<V> codec,
      boolean optional,
      Function<T, V> getter,
      BiConsumer<T, V> setter,
      Predicate<V> excludeIf,
      Supplier<V> defaultValue
  ) {

  }
}
