package net.arcadiusmc.utils.io;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Encoder;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.codecs.PrimitiveCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.lang.reflect.Array;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.experimental.UtilityClass;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.registry.Registries;
import net.arcadiusmc.utils.TomlConfigs;
import net.arcadiusmc.utils.inventory.ItemList;
import net.arcadiusmc.utils.inventory.ItemLists;
import net.arcadiusmc.utils.inventory.ItemStacks;
import net.forthecrown.nbt.BinaryTag;
import net.forthecrown.nbt.CompoundTag;
import net.forthecrown.nbt.string.TagParseException;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.Sound.Builder;
import net.kyori.adventure.sound.Sound.Source;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.NbtOps;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;

public @UtilityClass class ExtraCodecs {

  public static Function<BinaryTag, DataResult<CompoundTag>> TAG_TO_COMPOUND = binaryTag -> {
    if (binaryTag == null || !binaryTag.isCompound()) {
      return Results.error("Not a compound tag: %s", binaryTag);
    }
    return Results.success(binaryTag.asCompound());
  };

  /* ----------------------------------------------------------- */

  public final Codec<World> WORLD_CODEC = Codec.STRING.comapFlatMap(s -> {
    NamespacedKey key = NamespacedKey.fromString(s);
    World world;

    if (key != null) {
      world = Bukkit.getWorld(key);
    } else {
      world = Bukkit.getWorld(s);
    }

    if (world == null) {
      return Results.error("Unknown world '%s'", s);
    }

    return Results.success(world);
  }, world -> world.key().asString());

  public final Codec<String> KEY_CODEC = Codec.STRING.comapFlatMap(s -> {
    if (!Registries.isValidKey(s)) {
      return Results.error("Invalid key '%s'", s);
    }

    return DataResult.success(s);
  }, Function.identity());

  public static final Codec<Key> KYORI_KEY = Codec.STRING.comapFlatMap(s -> {
    NamespacedKey key = NamespacedKey.fromString(s);

    if (key == null) {
      return Results.error("Invalid key '%s'", s);
    }

    return Results.success(key);
  }, Key::asString);

  public static final Codec<NamespacedKey> NAMESPACED_KEY = Codec.STRING.comapFlatMap(string -> {
    NamespacedKey key = NamespacedKey.fromString(string);

    if (key == null) {
      return Results.error("Invalid key '%s'", string);
    }

    return Results.success(key);
  }, NamespacedKey::asString);

  public static final Codec<ItemStack> ITEM_CODEC = new PrimitiveCodec<>() {
    @Override
    public <T> DataResult<ItemStack> read(DynamicOps<T> ops, T input) {
      BinaryTag tag;

      if (ops instanceof TagOps) {
        tag = (BinaryTag) input;
      } else {
        tag = ops.convertTo(TagOps.OPS, input);
      }

      if (tag.isString()) {
        try {
          return DataResult.success(ItemStacks.fromNbtString(tag.toString()));
        } catch (TagParseException exc) {
          return Results.error("Invalid Itemstack: " + input);
        }
      }

      if (!tag.isCompound()) {
        return Results.error("Not an object: " + input);
      }

      return DataResult.success(ItemStacks.load(tag.asCompound()));
    }

    @Override
    public <T> T write(DynamicOps<T> ops, ItemStack value) {
      CompoundTag tag = ItemStacks.save(value);

      if (ops instanceof TagOps) {
        return (T) tag;
      } else {
        return ops.createString(tag.toNbtString());
      }
    }
  };

  public static final Codec<ItemList> ITEM_LIST_CODEC = ITEM_CODEC.listOf()
      .xmap(ItemLists::newList, itemStacks -> itemStacks);

  public final Codec<Duration> DURATION = new PrimitiveCodec<>() {
    @Override
    public <T> DataResult<Duration> read(DynamicOps<T> ops, T input) {
      var strResult = ops.getStringValue(input);

      if (strResult.result().isPresent()) {
        return safeParse(strResult.result().get(), TomlConfigs::parseDuration);
      }

      return ops.getNumberValue(input)
          .map(number -> Duration.ofMillis(number.longValue()));
    }

    @Override
    public <T> T write(DynamicOps<T> ops, Duration value) {
      return ops.createLong(value.toMillis());
    }
  };

  public final Codec<UUID> UUID_CODEC = new PrimitiveCodec<>() {
    @Override
    public <T> DataResult<UUID> read(DynamicOps<T> ops, T input) {
      var intResult = ops.getIntStream(input)
          .map(intStream -> uuidFromIntArray(intStream.toArray()));

      var stringResult = ops.getStringValue(input)
          .flatMap(s -> {
            try {
              return DataResult.success(UUID.fromString(s));
            } catch (IllegalArgumentException exc) {
              return Results.error("Invalid UUID: '%s'", s);
            }
          });

      if (stringResult.result().isPresent()) {
        return stringResult;
      }

      if (intResult.result().isPresent()) {
        return intResult;
      }

      return Results.error("Not a string or int[]: %s", input);
    }

    @Override
    public <T> T write(DynamicOps<T> ops, UUID value) {
      if (ops instanceof TagOps || ops instanceof NbtOps) {
        return ops.createIntList(Arrays.stream(uuidToIntArray(value)));
      }

      return ops.createString(value.toString());
    }
  };

  public static final Codec<Character> CHAR = Codec.STRING.comapFlatMap(s -> {
    if (s.length() > 1) {
      return Results.error("String '%s' is not a single character!", s);
    }

    if (s.isEmpty()) {
      return Results.error("Empty string");
    }

    return Results.success(s.charAt(0));
  }, Object::toString);

  /* ----------------------------------------------------------- */

  public final Codec<Location> LOCATION_CODEC = RecordCodecBuilder.create(instance -> {
    return instance
        .group(
            WORLD_CODEC.optionalFieldOf("world").forGetter(o -> Optional.ofNullable(o.getWorld())),

            Codec.DOUBLE.fieldOf("x").forGetter(Location::getX),
            Codec.DOUBLE.fieldOf("y").forGetter(Location::getY),
            Codec.DOUBLE.fieldOf("z").forGetter(Location::getZ),

            Codec.FLOAT.optionalFieldOf("yaw", 0f).forGetter(Location::getYaw),
            Codec.FLOAT.optionalFieldOf("pitch", 0f).forGetter(Location::getPitch)
        )
        .apply(instance, (world, x, y, z, yaw, pitch) -> {
          return new Location(world.orElse(null), x, y, z, yaw, pitch);
        });
  });

  public static final Codec<Component> COMPONENT = ofJson(JsonUtils::writeText, JsonUtils::readText);

  public static final Codec<UUID> INT_ARRAY_UUID = UUIDUtil.CODEC;
  public static final Codec<UUID> STRING_UUID = UUIDUtil.STRING_CODEC;
  public static final Codec<TextColor> COLOR = Codec.STRING.comapFlatMap(
      string -> safeParse(string, Arguments.COLOR),
      Object::toString
  );

  public static final Codec<Material> MATERIAL_CODEC = registryCodec(Registry.MATERIAL);

  public static final Codec<Instant> INSTANT = combine(
      Codec.STRING.flatXmap(
          s -> {
            try {
              return Results.success(JsonUtils.DATE_FORMAT.parse(s))
                  .map(date -> Instant.ofEpochMilli(date.getTime()));
            } catch (Exception e) {
              return Results.error(e.getMessage());
            }
          },
          o -> {
            try {
              return Results.success(JsonUtils.DATE_FORMAT.format(new Date(o.toEpochMilli())));
            } catch (Exception e) {
              return Results.error(e.getMessage());
            }
          }
      ),

      Codec.LONG.xmap(Instant::ofEpochMilli, Instant::toEpochMilli)
  );

  public static final Codec<Sound> SOUND_CODEC = combine(
      RecordCodecBuilder.create(instance -> {
        return instance
            .group(
                KYORI_KEY.fieldOf("name")
                    .forGetter(Sound::name),

                Codec.FLOAT.optionalFieldOf("pitch", 1.0f)
                    .forGetter(Sound::pitch),

                Codec.FLOAT.optionalFieldOf("volume", 1.0f)
                    .forGetter(Sound::volume),

                enumCodec(Source.class)
                    .optionalFieldOf("channel", Source.MASTER)
                    .forGetter(Sound::source),

                Codec.LONG.optionalFieldOf("seed")
                    .forGetter(sound -> {
                      var seed = sound.seed();
                      if (seed.isPresent()) {
                        return Optional.of(seed.getAsLong());
                      }
                      return Optional.empty();
                    })
            )
            .apply(instance, (key, pitch, volume, source, seed) -> {
              Builder builder = Sound.sound()
                  .type(key)
                  .pitch(pitch)
                  .volume(volume)
                  .source(source);

              seed.ifPresent(builder::seed);

              return builder.build();
            });
      }),

      KYORI_KEY.xmap(
          namespacedKey -> Sound.sound().type(namespacedKey).build(),
          Sound::name
      )
  );

  /* ----------------------------------------------------------- */

  public static <V> Codec<List<V>> listOrValue(Codec<V> codec) {
    return Codec.either(codec.listOf(), codec)
        .xmap(
            vListEither -> vListEither.map(Function.identity(), List::of),
            vs -> {
              if (vs.size() == 1) {
                return Either.right(vs.get(0));
              }
              return Either.left(vs);
            }
        );
  }

  public static <V> Codec<ImmutableList<V>> immutableList(Codec<V> baseType) {
    return baseType.listOf().xmap(ImmutableList::copyOf, Function.identity());
  }

  public static <V> Codec<V[]> arrayOf(Codec<V> baseType, Class<V> arrayValueType) {
    return baseType.listOf().xmap(
        vs -> {
          V[] arr = (V[]) Array.newInstance(arrayValueType, vs.size());
          vs.toArray(arr);
          return arr;
        },
        Arrays::asList
    );
  }

  @SafeVarargs
  public static <V> MapCodec<V> combineMap(MapCodec<V>... codecs) {
    return new MapCodec<>() {
      @Override
      public <T> Stream<T> keys(DynamicOps<T> ops) {
        return Arrays.stream(codecs)
            .map(vMapCodec -> vMapCodec.keys(ops))
            .reduce(Stream.empty(), Stream::concat);
      }

      @Override
      public <T> DataResult<V> decode(DynamicOps<T> ops, MapLike<T> input) {
        DataResult<V> result = null;

        for (MapCodec<V> codec : codecs) {
          var res = codec.decode(ops, input);

          if (res.result().isPresent()) {
            return res;
          }

          if (result == null) {
            result = res;
            continue;
          }

          result = result.apply2((v, o) -> o, res);
        }

        return result;
      }

      @Override
      public <T> RecordBuilder<T> encode(V input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
        return null;
      }
    };
  }

  public static <V> Codec<V> combine(List<Codec<V>> codecs) {
    return combine(0, codecs);
  }

  @SafeVarargs
  public static <V> Codec<V> combine(Codec<V>... codecs) {
    Objects.requireNonNull(codecs, "Null codecs");
    return combine(0, Arrays.asList(codecs));
  }

  public static <V> Codec<V> combine(int encoderIndex, List<Codec<V>> codecs) {
    Objects.requireNonNull(codecs, "Null codec list");
    if (codecs.isEmpty()) {
      throw new IllegalArgumentException("Empty codecs list");
    }

    Objects.checkIndex(encoderIndex, codecs.size());
    Codec<V> encoder = codecs.get(encoderIndex);

    return new Codec<>() {
      @Override
      public <T> DataResult<Pair<V, T>> decode(DynamicOps<T> ops, T input) {
        DataResult<Pair<V, T>> errors = null;

        for (Codec<V> codec : codecs) {
          DataResult<Pair<V, T>> result = codec.decode(ops, input);

          if (result.result().isPresent()) {
            return result;
          }

          if (errors == null) {
            errors = result;
            continue;
          }

          errors = errors.apply2((vtPair, o) -> vtPair, result);
        }

        return errors;
      }

      @Override
      public <T> DataResult<T> encode(V input, DynamicOps<T> ops, T prefix) {
        // Try to use the user-defined encoder, however, if that fails, try
        // to use the encoding of any codec provided

        var encodeResult = encoder.encode(input, ops, prefix);

        if (encodeResult.result().isPresent()) {
          return encodeResult;
        }

        for (int i = 0; i < codecs.size(); i++) {
          if (i == encoderIndex) {
            continue;
          }

          var res = codecs.get(i).encode(input, ops, prefix);

          if (res.result().isPresent()) {
            return res;
          }

          encodeResult = encodeResult.apply2((t, o) -> o, res);
        }

        return encodeResult;
      }
    };
  }

  public static <T extends Keyed> Codec<T> registryCodec(Registry<T> registry) {
    return NAMESPACED_KEY.comapFlatMap(key -> {
      var value = registry.get(key);

      if (value == null) {
        return Results.error("No value named '%s' found", key);
      }

      return Results.success(value);
    }, t -> t.getKey());
  }

  public static <T> DataResult<T> safeParse(String str, ArgumentType<T> parser) {
    try {
      StringReader reader = new StringReader(str);
      return DataResult.success(parser.parse(reader));
    } catch (CommandSyntaxException exc) {
      return DataResult.error(exc::getMessage);
    }
  }

  public static <V> Codec<V> ofJson(
      Function<V, JsonElement> serializer,
      Function<JsonElement, V> deserializer
  ) {
    return Codec.of(
        new Encoder<>() {
          @Override
          public <T> DataResult<T> encode(V input, DynamicOps<T> ops, T prefix) {
            JsonElement json = serializer.apply(input);
            if (ops instanceof JsonOps js) {
              return DataResult.success((T) json);
            }

            T val = JsonOps.INSTANCE.convertTo(ops, json);
            return DataResult.success(val);
          }
        },

        new Decoder<>() {
          @Override
          public <T> DataResult<Pair<V, T>> decode(DynamicOps<T> ops, T input) {
            JsonElement element;

            if (ops instanceof JsonOps) {
              element = (JsonElement) input;
            } else {
              element = ops.convertTo(JsonOps.INSTANCE, input);
            }

            V value = deserializer.apply(element);
            return DataResult.success(Pair.of(value, input));
          }
        }
    );
  }

  public <E extends Enum<E>> Codec<E> enumCodec(Class<E> eClass) {
    if (!eClass.isEnum()) {
      throw new IllegalArgumentException(
          String.format("Class '%s' is not an enum", eClass)
      );
    }

    E[] constants = eClass.getEnumConstants();

    if (constants.length > 16) {
      Map<String, E> map = new HashMap<>();
      for (var e : constants) {
        map.put(e.name().toUpperCase(), e);
      }

      return Codec.STRING.comapFlatMap(s -> {
        var result = map.get(s.toUpperCase());

        if (result == null) {
          return Results.error(
              "Unknown '%s' constant: '%s'",
              eClass, s
          );
        }

        return DataResult.success(result);
      }, e -> e.name().toLowerCase());
    }

    return Codec.STRING.comapFlatMap(s -> {
      s = s.toUpperCase();

      for (var e : constants) {
        if (e.name().toUpperCase().equals(s)) {
          return DataResult.success(e);
        }
      }

      return Results.error(
          "Unknown '%s' constant: '%s'",
          eClass, s
      );
    }, e -> e.name().toLowerCase());
  }

  public static UUID uuidFromIntArray(int[] arr) {
    return UUIDUtil.uuidFromIntArray(arr);
  }

  public static int[] uuidToIntArray(UUID uuid) {
    return UUIDUtil.uuidToIntArray(uuid);
  }

  @Deprecated
  public static <V> MapCodec<V> strictOptional(Codec<V> codec, String field, V defaultValue) {
    return codec.optionalFieldOf(field, defaultValue);
  }

  @Deprecated
  public static <V> MapCodec<Optional<V>> strictOptional(Codec<V> codec, String field) {
    return codec.optionalFieldOf(field);
  }

  public static <T> Codec<Set<T>> set(Codec<T> codec) {
    return codec.listOf().xmap(HashSet::new, ArrayList::new);
  }
}