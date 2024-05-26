package net.arcadiusmc.entity.persistence;

import com.badlogic.ashley.core.Component;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.Getter;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.entity.persistence.PersistentTypes.ObjectSerializer;
import net.arcadiusmc.utils.io.ExistingObjectCodec;
import net.arcadiusmc.utils.io.ExistingObjectCodec.FieldBuilder;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

@Getter
public class PersistentType<T extends Component> implements Codec<T> {

  private static final Logger LOGGER = Loggers.getLogger();

  final Class<T> componentClass;
  final Codec<T> codec;

  public PersistentType(Class<T> componentClass, @Nullable Codec<T> supplier) {
    this.componentClass = componentClass;
    this.codec = Objects.requireNonNullElseGet(supplier, () -> createClassCodec(componentClass));
  }

  private static <T extends Component> Codec<T> createClassCodec(Class<T> type) {
    Constructor<T> constructor = findConstructor(type);

    Supplier<T> supplier = () -> {
      try {
        return constructor.newInstance();
      } catch (InvocationTargetException e) {
        throw new RuntimeException(e.getCause());
      } catch (ReflectiveOperationException exc) {
        throw new RuntimeException(exc);
      }
    };

    return createCodec(type).codec(Codec.unit(supplier));
  }

  private static <T> Constructor<T> findConstructor(Class<T> type) {
    try {
      Constructor<T> constructor = type.getDeclaredConstructor();
      constructor.setAccessible(true);
      return constructor;
    } catch (ReflectiveOperationException exc) {
      throw new IllegalStateException(
          "Component class must have a no-args constructor to allow "
              + "for reflective instantiation in " + type,
          exc
      );
    }
  }

  @Override
  public <S> DataResult<Pair<T, S>> decode(DynamicOps<S> ops, S input) {
    return codec.decode(ops, input);
  }

  @Override
  public <S> DataResult<S> encode(T input, DynamicOps<S> ops, S prefix) {
    return codec.encode(input, ops, prefix);
  }

  private static <T> ExistingObjectCodec<T> createCodec(Class<T> type) {
    ExistingObjectCodec.Builder<T> builder = ExistingObjectCodec.builder();

    for (Field field : type.getDeclaredFields()) {
      int mod = field.getModifiers();

      if (Modifier.isStatic(mod) | Modifier.isTransient(mod)) {
        continue;
      }

      Class<Object> fieldType = (Class<Object>) field.getType();
      ObjectSerializer<Object> fieldSerializer = PersistentTypes.getTypeCodec(fieldType);

      if (fieldSerializer == null) {
        LOGGER.error("Couldn't get codec for field {}", field);
        continue;
      }

      field.setAccessible(true);

      FieldBuilder<T, Object> fieldBuilder = builder.optional(
          field.getName(),
          fieldSerializer.codec()
      );

      fieldBuilder.getter(new FieldGetter<>(field));

      if (Modifier.isFinal(field.getModifiers())) {
        if (fieldSerializer.applicator() == null) {
          LOGGER.error("Field {} is final and no merger function found", field);
          continue;
        }

        fieldBuilder.setter(new FinalSetter<>(field, fieldSerializer.applicator()));
      } else {
        fieldBuilder.setter(new SimpleFieldSetter<>(field));
      }
    }

    return builder.build();
  }

  private record FinalSetter<T, V>(Field field, BiConsumer<V, V> merger)
      implements BiConsumer<T, V>
  {

    @Override
    public void accept(T t, V v) {
      try {
        V gotten = (V) field.get(t);

        if (gotten == null) {
          LOGGER.error("Couldn't merge value into field {}: Exisitng value is null", field);
          return;
        }

        merger.accept(gotten, v);
      } catch (IllegalAccessException e) {
        LOGGER.error("Error merging loaded value into 'final' field {}", field, e);
      }
    }
  }

  private record SimpleFieldSetter<T, V>(Field field) implements BiConsumer<T, V> {

    @Override
    public void accept(T t, V v) {
      try {
        field.set(t, v);
      } catch (IllegalAccessException e) {
        LOGGER.error("Error setting value of field {}", field, e);
      }
    }
  }

  private record FieldGetter<T, V>(Field field) implements Function<T, V> {

    @Override
    public V apply(T t) {
      try {
        return (V) field.get(t);
      } catch (IllegalAccessException e) {
        LOGGER.error("Error getting value of field {}", field, e);
        return null;
      }
    }
  }
}