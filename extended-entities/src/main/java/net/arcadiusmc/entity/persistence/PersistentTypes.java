package net.arcadiusmc.entity.persistence;

import com.badlogic.ashley.core.Component;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import net.arcadiusmc.entity.phys.Shape;
import net.arcadiusmc.entity.phys.ShapeType;
import net.arcadiusmc.entity.system.EntityId;
import net.arcadiusmc.registry.Holder;
import net.arcadiusmc.registry.Registries;
import net.arcadiusmc.registry.Registry;
import net.arcadiusmc.utils.PluginUtil;
import net.arcadiusmc.utils.io.ExtraCodecs;
import net.arcadiusmc.utils.io.JomlCodecs;
import net.arcadiusmc.utils.io.Results;
import org.bukkit.World;
import org.joml.Vector2d;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector3i;

@SuppressWarnings("rawtypes")
public class PersistentTypes {

  public static final List<ObjectSerializer<?>> SERIALIZERS;
  public static final Registry<PersistentType> REGISTRY;

  static {
    SERIALIZERS = new ArrayList<>();
    REGISTRY = Registries.newRegistry();
  }

  /* --------------------------- STATIC METHODS --------------------------- */

  public static void registerAll() {
    registerComponent(EntityId.class, ExtraCodecs.STRING_UUID.xmap(EntityId::new, EntityId::getId));

    registerPrimitive(Boolean.TYPE, Boolean.class,  Codec.BOOL);

    registerPrimitive(Byte.TYPE,    Byte.class,     Codec.BYTE);
    registerPrimitive(Short.TYPE,   Short.class,    Codec.SHORT);
    registerPrimitive(Integer.TYPE, Integer.class,  Codec.INT);
    registerPrimitive(Float.TYPE,   Float.class,    Codec.FLOAT);
    registerPrimitive(Long.TYPE,    Long.class,     Codec.LONG);
    registerPrimitive(Double.TYPE,  Double.class,   Codec.DOUBLE);

    registerSerializer(String.class, Codec.STRING);
    registerSerializer(UUID.class, ExtraCodecs.STRING_UUID);

    registerSerializer(Vector3i.class, JomlCodecs.VEC3I, Vector3i::set);
    registerSerializer(Vector3d.class, JomlCodecs.VEC3D, Vector3d::set);
    registerSerializer(Vector3f.class, JomlCodecs.VEC3F, Vector3f::set);
    registerSerializer(Vector2i.class, JomlCodecs.VEC2I, Vector2i::set);
    registerSerializer(Vector2d.class, JomlCodecs.VEC2D, Vector2d::set);
    registerSerializer(Vector2f.class, JomlCodecs.VEC2F, Vector2f::set);

    registerSerializer(World.class, ExtraCodecs.WORLD_CODEC);
    registerSerializer(Shape.class, ShapeType.SHAPE_CODEC);
  }

  private static <T extends Component> void registerComponent(Class<T> type, Codec<T> supplier) {
    String key = type.getName();

    if (REGISTRY.contains(key)) {
      throw new IllegalStateException("Type " + key + "already registered");
    }

    PersistentType<T> persistentType = new PersistentType<>(type, supplier);
    REGISTRY.register(key, persistentType);
  }

  public static <T extends Component> Holder<PersistentType<T>> getType(Class<T> type) {
    String key = type.getName();
    Optional<Holder<PersistentType<T>>> opt = (Optional) REGISTRY.getHolder(key);

    if (opt.isPresent()) {
      return opt.get();
    }

    PersistentType<T> persistentType = new PersistentType<>(type, null);
    Holder<PersistentType<T>> holder = (Holder) REGISTRY.register(key, persistentType);

    return holder;
  }

  public static DataResult<PersistentType> getType(String name) {
    Optional<PersistentType> opt = REGISTRY.get(name);

    if (opt.isPresent()) {
      return DataResult.success(opt.get());
    }

    Class<?> type;

    try {
      ClassLoader loader = PluginUtil.getPlugin().getClass().getClassLoader();
      type = Class.forName(name, true, loader);
    } catch (ClassNotFoundException e) {
      return Results.error("Unknown class");
    }

    if (!Component.class.isAssignableFrom(type)) {
      return Results.error("Not a Component class");
    }

    Class<Component> cType = (Class<Component>) type;
    PersistentType<Component> persistentType = new PersistentType<>(cType, null);

    REGISTRY.register(name, persistentType);

    return DataResult.success(persistentType);
  }

  static <T> ObjectSerializer<T> getTypeCodec(Class<T> type) {
    for (ObjectSerializer<?> serializer : SERIALIZERS) {
      if (!serializer.predicate.test(type)) {
        continue;
      }

      return (ObjectSerializer<T>) serializer;
    }

    if (type.isArray()) {
      Class<Object> componentType = (Class<Object>) type.componentType();
      ObjectSerializer<Object> serializer = getTypeCodec(componentType);

      Codec<T> arr = (Codec<T>) arrayCodec(componentType, serializer.codec);
      return registerSerializer(type, arr);
    }

    if (type.isEnum()) {
      Class noType = type;
      Codec<T> codec = ExtraCodecs.enumCodec(noType);
      return registerSerializer(type, codec);
    }

    return null;
  }

  private static <T> Codec<T[]> arrayCodec(Class<T> type, Codec<T> codec) {
    return codec.listOf()
        .xmap(
            ts -> {
              T[] arr = (T[]) Array.newInstance(type, ts.size());
              ts.toArray(arr);
              return arr;
            },
            ObjectArrayList::new
        );
  }

  private static <T> void registerPrimitive(Class<T> prim, Class<T> wrapper, Codec<T> codec) {
    registerSerializer(clazz -> clazz == prim || clazz == wrapper, codec, null);
  }

  private static <T> ObjectSerializer<T> registerSerializer(Class<T> type, Codec<T> codec) {
    return registerSerializer(type, codec, null);
  }

  private static <T> ObjectSerializer<T> registerSerializer(
      Class<T> type,
      Codec<T> codec,
      BiConsumer<T, T> merger
  ) {
    return registerSerializer(clazz -> clazz == type, codec, merger);
  }

  private static <T> ObjectSerializer<T> registerSerializer(
      Predicate<Class<?>> predicate,
      Codec<T> codec,
      BiConsumer<T, T> merger
  ) {
    ObjectSerializer<T> serializer = new ObjectSerializer<>(predicate, codec, merger);
    SERIALIZERS.add(serializer);
    return serializer;
  }

  private static <T> ObjectSerializer<T> registerSerializer(Predicate<Class<?>> predicate, Codec<T> codec) {
    return registerSerializer(predicate, codec, null);
  }

  record ObjectSerializer<T>(
      Predicate<Class<?>> predicate,
      Codec<T> codec,
      BiConsumer<T, T> applicator
) {

  }
}