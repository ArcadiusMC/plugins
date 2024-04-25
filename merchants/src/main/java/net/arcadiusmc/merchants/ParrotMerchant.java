package net.arcadiusmc.merchants;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import net.arcadiusmc.menu.Menu;
import net.arcadiusmc.utils.io.ExtraCodecs;
import net.forthecrown.nbt.CompoundTag;
import org.bukkit.entity.Parrot.Variant;

public class ParrotMerchant extends Merchant {

  private Config config = Config.EMPTY;

  private final Map<UUID, List<Variant>> owned = new HashMap<>();

  @Getter
  private Menu menu;

  public ParrotMerchant(MerchantsPlugin plugin) {
    super(plugin, "parrots");
  }

  @Override
  public void reloadConfig() {
    loadConfig(Config.CODEC, cfg -> this.config = cfg);
  }

  @Override
  protected void loadDataFrom(CompoundTag tag) {

  }

  @Override
  protected void saveDataTo(CompoundTag tag) {

  }

  private record Config(List<SellableParrot> parrots) {
    static final Config EMPTY = new Config(List.of());

    static final Codec<Config> CODEC = RecordCodecBuilder.create(instance -> {
      return instance
          .group(
              ExtraCodecs.strictOptional(SellableParrot.CODEC.listOf(), "parrots", List.of())
                  .forGetter(Config::parrots)
          )
          .apply(instance, Config::new);
    });
  }

  private record SellableParrot(
      Variant variant,
      int price,
      String textureId
  ) {
    static final Codec<SellableParrot> CODEC = RecordCodecBuilder.create(instance -> {
      return instance
          .group(
              ExtraCodecs.enumCodec(Variant.class)
                  .fieldOf("variant")
                  .forGetter(SellableParrot::variant),

              Codec.INT.fieldOf("price")
                  .forGetter(SellableParrot::price),

              Codec.STRING.fieldOf("texture-id")
                  .forGetter(SellableParrot::textureId)
          )
          .apply(instance, SellableParrot::new);
    });
  }
}
