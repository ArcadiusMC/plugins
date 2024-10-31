package net.arcadiusmc.dungeons.gen;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import java.util.stream.Stream;
import net.arcadiusmc.registry.Registries;
import net.arcadiusmc.registry.Registry;

public class DecoratorTypes {

  public static final Registry<DecoratorType<?, ?>> TYPES = Registries.newFreezable();
  public static final MapCodec<Decorator<?>> DECORATOR_CODEC;

  static {
    registerAll();
    DECORATOR_CODEC = decoratorCodec();
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static MapCodec<Decorator<?>> decoratorCodec() {
    MapCodec<DecoratorType<?, Object>> typeCodec = (MapCodec) TYPES.registryCodec().fieldOf("type");
    return new DecoratorCodec(typeCodec);
  }

  private static void registerAll() {
    TYPES.register("foliage", FoliageDecorator.TYPE);
    TYPES.register("candles", CandleDecorator.TYPE);
    TYPES.register("vines", HangingVineDecorator.TYPE);
    TYPES.register("leaves", LeafDecorator.TYPE);
    TYPES.register("puddles", PuddleDecorator.TYPE);
    TYPES.register("overgrowth", MossDecorator.TYPE);
    TYPES.register("edge-rot", EdgeRotDecorator.TYPE);
    TYPES.register("treasure", TreasureDecorator.TYPE);
    TYPES.register("spawners", SpawnerDecorator.TYPE);

    TYPES.register("functions/hanging-lights", HangingLightsDecorator.TYPE);
    TYPES.register("functions/fire", FireDecorator.TYPE);
  }

  private static class DecoratorCodec extends MapCodec<Decorator<?>> {

    private final MapCodec<DecoratorType<?, Object>> typeCodec;

    public DecoratorCodec(MapCodec<DecoratorType<?, Object>> typeCodec) {
      this.typeCodec = typeCodec;
    }

    @Override
    public <T> Stream<T> keys(DynamicOps<T> ops) {
      return Stream.of(ops.createString("settings"), ops.createString("type"));
    }

    @Override
    public <T> DataResult<Decorator<?>> decode(DynamicOps<T> ops, MapLike<T> input) {
      return typeCodec.decode(ops, input)
          .flatMap(type -> {
            T settings = input.get("settings");
            if (settings == null) {
              settings = ops.emptyMap();
            }

            return type.configCodec().parse(ops, settings).map(type::newDecorator);
          });
    }

    @Override
    public <T> RecordBuilder<T> encode(
        Decorator<?> input,
        DynamicOps<T> ops,
        RecordBuilder<T> prefix
    ) {
      // They don't get serialized
      return prefix;
    }
  }
}
