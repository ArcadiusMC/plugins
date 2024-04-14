package net.arcadiusmc.titles;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import net.arcadiusmc.menu.Slot;
import net.arcadiusmc.titles.Tier.MenuDecoration;
import net.arcadiusmc.user.currency.CurrencyMap;
import net.arcadiusmc.user.currency.CurrencyMaps;
import net.arcadiusmc.utils.io.ExtraCodecs;
import net.arcadiusmc.utils.io.Results;
import net.kyori.adventure.text.Component;
import org.bukkit.Registry;

public class TitleCodecs {

  static final MapCodec<ImmutableList<Component>> DESC_MAP_CODEC
      = Codec.either(ExtraCodecs.COMPONENT, ExtraCodecs.immutableList(ExtraCodecs.COMPONENT))
      .xmap(either -> either.map(ImmutableList::of, components -> components), Either::right)
      .optionalFieldOf("description", ImmutableList.of());

  static final Codec<MenuDecoration> DECORATION_CODEC = RecordCodecBuilder.create(instance -> {
    return instance
        .group(
            Slot.CODEC.fieldOf("slot").forGetter(MenuDecoration::slot),
            ExtraCodecs.MATERIAL_CODEC.fieldOf("item").forGetter(MenuDecoration::material)
        )
        .apply(instance, MenuDecoration::new);
  });

  static final Codec<Tier> TIER = RecordCodecBuilder.create(instance -> {
    return instance
        .group(
            ExtraCodecs.COMPONENT.fieldOf("display-name")
                .forGetter(Tier::getDisplayName),

            ExtraCodecs.registryCodec(Registry.MATERIAL)
                .fieldOf("display-item")
                .forGetter(Tier::getDisplayItem),

            DESC_MAP_CODEC.forGetter(Tier::getDescription),

            Codec.STRING.fieldOf("permission-group")
                .forGetter(Tier::getPermissionGroup),

            Codec.INT.fieldOf("priority")
                .forGetter(Tier::getPriority),

            DECORATION_CODEC.listOf()
                .optionalFieldOf("menu-decorations", List.of())
                .forGetter(Tier::getDecorations),

            Codec.BOOL.optionalFieldOf("permission-sync", true)
                .forGetter(Tier::isPermissionSync),

            Codec.BOOL.optionalFieldOf("hidden", false)
                .forGetter(Tier::isHidden)
        )
        .apply(instance, (name, item, desc, group, prio, decos, sync, hide) -> {
          return new Tier(name, item, desc, group, prio, decos, sync, hide, true);
        });
  });

  static final Codec<Map<String, Tier>> TIER_MAP = Codec.unboundedMap(ExtraCodecs.KEY_CODEC, TIER);

  static final Codec<CurrencyMap<Integer>> PRICE_CODEC = CurrencyMaps.createCodec(Codec.INT);

  static final Codec<Title> RANK_CODEC = RecordCodecBuilder.<Title>create(instance -> {
    return instance
        .group(
            Tiers.REGISTRY.registryCodec().fieldOf("tier")
                .forGetter(Title::getTier),

            ExtraCodecs.COMPONENT.fieldOf("prefix")
                .forGetter(Title::getTruncatedPrefix),

            ExtraCodecs.KEY_CODEC.optionalFieldOf("gender-equivalent", "")
                .forGetter(Title::getGenderEquivalentKey),

            Slot.CODEC.optionalFieldOf("slot")
                .forGetter(o -> Optional.ofNullable(o.getMenuSlot())),

            DESC_MAP_CODEC.forGetter(Title::getDescription),

            Codec.BOOL.optionalFieldOf("hidden", false)
                .forGetter(Title::isHidden),

            PRICE_CODEC.optionalFieldOf("prices", CurrencyMaps.emptyMap())
                .forGetter(Title::getPrice)
        )
        .apply(instance, (tier, prefix, genderEq, slot, desc, hidden, prices) -> {
          Title t = new Title(
              tier,
              prefix,
              genderEq,
              slot.orElse(null),
              desc,
              hidden,
              true
          );

          t.setPrice(prices);

          return t;
        });
  })
      // flatMap in which verification checks are ran
      .comapFlatMap(
          rank -> {
            if (!rank.getPrice().isEmpty() && rank.isHidden()) {
              return Results.error("Titles with a price cannot also be hidden");
            }

            if (rank.getMenuSlot() != null) {
              var slot = rank.getMenuSlot();
              int x = slot.getX();
              int y = slot.getY();

              if (x < 1 || x >= 8) {
                return Results.error("Slot's X coordinate must be in range 1..8");
              }

              if (y < 1 || y >= 3) {
                return Results.error("Slot's Y coordinate must be in range 1..3");
              }
            }

            return Results.success(rank);
          },
          Function.identity()
      );

  static final Codec<Map<String, Title>> RANK_MAP
      = Codec.unboundedMap(ExtraCodecs.KEY_CODEC, RANK_CODEC);
}
