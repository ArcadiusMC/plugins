package net.arcadiusmc.dungeons.gen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class DecorationPass {

  public static final Codec<DecorationPass> CODEC = RecordCodecBuilder.create(instance -> {
    return instance
        .group(
            Codec.STRING.fieldOf("name").forGetter(DecorationPass::getName),
            Codec.BOOL.optionalFieldOf("disabled", false).forGetter(DecorationPass::isDisabled),
            DecoratorTypes.DECORATOR_CODEC.forGetter(DecorationPass::getDecorator)
        )
        .apply(instance, (name, disabled, decorator) -> {
          DecorationPass pass = new DecorationPass();
          pass.setName(name);
          pass.setDisabled(disabled);
          pass.setDecorator(decorator);
          return pass;
        });
  });

  private Decorator<?> decorator;
  private String name;
  private boolean disabled;

  public DecorationPass() {

  }

  public DecorationPass(String name, Decorator<?> decorator) {
    this.decorator = decorator;
    this.name = name;
  }
}
