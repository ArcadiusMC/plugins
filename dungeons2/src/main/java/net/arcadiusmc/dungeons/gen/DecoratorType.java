package net.arcadiusmc.dungeons.gen;

import com.mojang.serialization.Codec;
import java.util.function.Function;

public interface DecoratorType<D extends Decorator<C>, C> {

  static <D extends Decorator<C>, C> DecoratorType<D, C> create(Codec<C> codec, Function<C, D> ctor) {
    return new DecoratorType<D, C>() {
      @Override
      public Codec<C> configCodec() {
        return codec;
      }

      @Override
      public D newDecorator(C config) {
        return ctor.apply(config);
      }
    };
  }

  Codec<C> configCodec();

  D newDecorator(C config);
}
