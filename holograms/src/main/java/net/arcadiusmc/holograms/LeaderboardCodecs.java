package net.arcadiusmc.holograms;

import com.mojang.serialization.Codec;
import net.arcadiusmc.holograms.Leaderboard.Order;
import net.arcadiusmc.registry.Holder;
import net.arcadiusmc.text.PlayerMessage;
import net.arcadiusmc.utils.io.ExistingObjectCodec;
import net.arcadiusmc.utils.io.ExtraCodecs;
import net.forthecrown.grenadier.types.ArgumentTypes;
import net.forthecrown.grenadier.types.IntRangeArgument.IntRange;

public final class LeaderboardCodecs {
  private LeaderboardCodecs() {}

  static final Codec<Holder<LeaderboardSource>> sourceCodec
      = Codec.STRING.comapFlatMap(LeaderboardSources::get, Holder::getKey);

  static final Codec<IntRange> FILTER_CODEC = Codec.STRING
      .comapFlatMap(
          string -> ExtraCodecs.safeParse(string, ArgumentTypes.intRange()),
          IntRange::toString
      );

  static final ExistingObjectCodec<BoardImpl> BOARD_CODEC = ExistingObjectCodec.create(builder -> {
    builder.optional("source", sourceCodec)
        .setter((board, o) -> board.source = o)
        .getter(board -> board.source);

    builder.optional("location", ExtraCodecs.LOCATION_CODEC)
        .getter(BoardImpl::getLocation)
        .setter(BoardImpl::setLocation);

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

    builder.optional("display_meta", TextDisplayMeta.CODEC)
        .getter(BoardImpl::getDisplayMeta)
        .setter(BoardImpl::setDisplayMeta);

    builder.optional("spawned", Codec.BOOL)
        .excludeIf(aBoolean -> !aBoolean)
        .getter(BoardImpl::isSpawned)
        .setter(BoardImpl::setSpawned);

    builder.optional("include_you", Codec.BOOL)
        .excludeIf(aBoolean -> !aBoolean)
        .getter(BoardImpl::isIncludeYou)
        .setter(BoardImpl::setIncludeYou);
  });
}
