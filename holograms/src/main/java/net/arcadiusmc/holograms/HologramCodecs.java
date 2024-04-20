package net.arcadiusmc.holograms;

import com.google.common.base.Strings;
import com.mojang.serialization.Codec;
import net.arcadiusmc.holograms.Leaderboard.Order;
import net.arcadiusmc.registry.Holder;
import net.arcadiusmc.text.PlayerMessage;
import net.arcadiusmc.utils.io.ExistingObjectCodec;
import net.arcadiusmc.utils.io.ExtraCodecs;
import net.forthecrown.grenadier.types.ArgumentTypes;
import net.forthecrown.grenadier.types.IntRangeArgument.IntRange;

public final class HologramCodecs {
  private HologramCodecs() {}

  static final Codec<Holder<LeaderboardSource>> sourceCodec
      = Codec.STRING.comapFlatMap(LeaderboardSources::get, Holder::getKey);

  static final Codec<IntRange> FILTER_CODEC = Codec.STRING
      .comapFlatMap(
          string -> ExtraCodecs.safeParse(string, ArgumentTypes.intRange()),
          IntRange::toString
      );

  static final ExistingObjectCodec<TextImpl> TEXT_CODEC = ExistingObjectCodec.create(builder -> {
    addFields(builder);
    builder.optional("text", Codec.STRING)
        .getter(TextImpl::getText)
        .setter(TextImpl::setText)
        .excludeIf(Strings::isNullOrEmpty);
  });

  static final ExistingObjectCodec<BoardImpl> BOARD_CODEC = ExistingObjectCodec.create(builder -> {
    addFields(builder);

    builder.optional("source", sourceCodec)
        .setter((board, o) -> board.source = o)
        .getter(board -> board.source);

    builder.optional("footer", PlayerMessage.CODEC)
        .excludeIf(m -> m == null || m.getMessage().isEmpty())
        .getter(BoardImpl::getFooter)
        .setter(BoardImpl::setFooter);

    builder.optional("header", PlayerMessage.CODEC)
        .excludeIf(m -> m == null || m.getMessage().isEmpty())
        .getter(BoardImpl::getHeader)
        .setter(BoardImpl::setHeader);

    builder.optional("format", PlayerMessage.CODEC)
        .excludeIf(m -> m == null || m.getMessage().isEmpty())
        .getter(BoardImpl::getFormat)
        .setter(BoardImpl::setFormat);

    builder.optional("you_format", PlayerMessage.CODEC)
        .excludeIf(m -> m == null || m.getMessage().isEmpty())
        .getter(BoardImpl::getYouFormat)
        .setter(BoardImpl::setYouFormat);

    builder.optional("empty_format", PlayerMessage.CODEC)
        .excludeIf(m -> m == null || m.getMessage().isEmpty())
        .getter(BoardImpl::getEmptyFormat)
        .setter(BoardImpl::setEmptyFormat);

    builder.optional("order", ExtraCodecs.enumCodec(Order.class))
        .excludeIf(order -> order == null || order == Order.DESCENDING)
        .getter(BoardImpl::getOrder)
        .setter(BoardImpl::setOrder);

    builder.optional("filter", FILTER_CODEC)
        .getter(BoardImpl::getFilter)
        .setter(BoardImpl::setFilter);

    builder.optional("max_entries", Codec.INT)
        .getter(BoardImpl::getMaxEntries)
        .setter(BoardImpl::setMaxEntries);

    builder.optional("fill_empty", Codec.BOOL)
        .getter(BoardImpl::fillMissingSlots)
        .setter(BoardImpl::setFillMissingSlots);

    builder.optional("include_you", Codec.BOOL)
        .excludeIf(aBoolean -> !aBoolean)
        .getter(BoardImpl::isIncludeYou)
        .setter(BoardImpl::setIncludeYou);
  });

  static <T extends Hologram> void addFields(ExistingObjectCodec.Builder<T> builder) {
    builder.optional("spawned", Codec.BOOL)
        .getter(Hologram::isSpawned)
        .setter(Hologram::setSpawned);

    builder.optional("display_meta", TextDisplayMeta.CODEC)
        .getter(Hologram::getDisplayMeta)
        .setter(Hologram::setDisplayMeta);

    builder.optional("location", ExtraCodecs.LOCATION_CODEC)
        .getter(Hologram::getLocation)
        .setter(Hologram::setLocation);
  }
}
