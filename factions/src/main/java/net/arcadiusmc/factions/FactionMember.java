package net.arcadiusmc.factions;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import net.arcadiusmc.utils.io.ExistingObjectCodec;
import net.arcadiusmc.utils.io.ExtraCodecs;
import org.spongepowered.math.GenericMath;

public class FactionMember {

  static final Codec<FactionMember> BASE_CODEC = RecordCodecBuilder.create(instance -> {
    return instance
        .group(
            ExtraCodecs.UUID_CODEC.fieldOf("player_id")
                .forGetter(o -> o.playerId)
        )
        .apply(instance, FactionMember::new);
  });

  static final Codec<FactionMember> CODEC;

  static final Codec<List<FactionMember>> LIST_CODEC;

  @Getter
  private final UUID playerId;

  @Getter @Setter
  private boolean active;

  @Getter @Setter
  private int baseReputation;

  public FactionMember(UUID playerId) {
    this.playerId = playerId;
  }

  public int getUncappedReputation() {
    return baseReputation;
  }

  public int getReputation() {
    FactionsConfig config = Factions.getConfig();
    int uncapped = getUncappedReputation();

    return GenericMath.clamp(uncapped, config.getMinReputation(), config.getMaxReputation());
  }

  static {
    ExistingObjectCodec<FactionMember> memberCodec = ExistingObjectCodec.create(builder -> {
      builder.optional("active", Codec.BOOL)
          .getter(member -> member.active)
          .setter((member, active) -> member.active = active);

      builder.optional("reputation", Codec.INT)
          .getter(member -> member.baseReputation)
          .setter((member, value) -> member.baseReputation = value);
    });

    CODEC = memberCodec.codec(BASE_CODEC);
    LIST_CODEC = CODEC.listOf();
  }
}
