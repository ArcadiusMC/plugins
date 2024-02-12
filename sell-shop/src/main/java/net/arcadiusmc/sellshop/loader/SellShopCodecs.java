package net.arcadiusmc.sellshop.loader;

import static net.arcadiusmc.menu.Menus.MAX_INV_SIZE;
import static net.arcadiusmc.menu.Menus.MIN_INV_SIZE;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import net.arcadiusmc.menu.Menus;
import net.arcadiusmc.sellshop.loader.PageLoader.LoadingPage;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.utils.inventory.ItemStacks;
import net.arcadiusmc.utils.io.ExtraCodecs;
import net.arcadiusmc.utils.io.Results;
import net.forthecrown.grenadier.types.ArgumentTypes;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

final class SellShopCodecs {
  private SellShopCodecs() {}

  public static final Codec<Integer> INV_SIZE_INT = Codec.INT.comapFlatMap(integer -> {
    if (Menus.isValidSize(integer)) {
      return Results.success(integer);
    }
    return Results.error("Invalid inventory size: %s", integer);
  }, Function.identity());

  public static final Codec<Integer> INV_SIZE_STRING;
  public static final Codec<Integer> INVENTORY_SIZE;

  static {
    Map<String, Integer> suffixes = new HashMap<>();
    suffixes.put("row", 9);
    suffixes.put("rows", 9);
    suffixes.put("r", 9);

    ArgumentType<Integer> sizeParser
        = ArgumentTypes.suffixedInt(suffixes, MIN_INV_SIZE, MAX_INV_SIZE);

    INV_SIZE_STRING = Codec.STRING.comapFlatMap(string -> {
      return ExtraCodecs.safeParse(string, sizeParser);
    }, String::valueOf);

    INVENTORY_SIZE = ExtraCodecs.combine(INV_SIZE_INT, INV_SIZE_STRING);
  }

  /* ------------------------------------------------------- */

  private static final Codec<ItemStack> BORDER_ITEM_CODEC
      = Codec.either(ExtraCodecs.ITEM_CODEC, Codec.BOOL)
      .xmap(
          either -> either.map(i -> i, hasBorder -> hasBorder ? Menus.defaultBorderItem() : ItemStack.empty()),
          itemStack -> {
            if (ItemStacks.isEmpty(itemStack)) {
              return Either.right(false);
            }
            return Either.left(itemStack);
          }
      );

  static final Codec<LoadingPage> PAGE_CODEC = RecordCodecBuilder.create(instance -> {
    return instance
        .group(
            INVENTORY_SIZE
                .optionalFieldOf("size", -1)
                .forGetter(o -> o.size),

            ExtraCodecs.COMPONENT
                .optionalFieldOf("title")
                .forGetter(o -> Optional.ofNullable(o.title)),

            ExtraCodecs.COMPONENT.listOf()
                .xmap(components -> components.toArray(Component[]::new), Arrays::asList)
                .optionalFieldOf("description")
                .forGetter(o -> Optional.ofNullable(o.desc)),

            ExtraCodecs.registryCodec(Registry.MATERIAL)
                .optionalFieldOf("header-item", Material.STONE)
                .forGetter(o -> o.headerItem),

            BORDER_ITEM_CODEC.optionalFieldOf("border", ItemStack.empty())
                .forGetter(o -> o.border),

            Codec.BOOL.optionalFieldOf("template", false)
                .forGetter((o) -> o.abstractPage),

            Codec.STRING.optionalFieldOf("extends", "")
                .forGetter((o) -> o.extendsName)
        )
        .apply(instance, (size, title, desc, header, border, abstractPage, ext) -> {
          LoadingPage page = new LoadingPage();
          page.size = size;
          page.border = border;
          page.abstractPage = abstractPage;
          page.extendsName = ext;
          page.headerItem = header;

          title.ifPresent((component) -> {
            page.title = component;
          });

          desc.ifPresent((components) -> {
            page.desc = components;
          });
          return page;
        });
  });

  public static final Codec<ItemStack> ITEM_CODEC = RecordCodecBuilder.create((instance) -> {
    return instance
        .group(
            ExtraCodecs.MATERIAL_CODEC.fieldOf("material")
                .forGetter(ItemStack::getType),

            Codec.INT.optionalFieldOf("amount", 1)
                .forGetter(ItemStack::getAmount),

            ExtraCodecs.COMPONENT.optionalFieldOf("name")
                .forGetter((o) -> Optional.ofNullable(o.getItemMeta().displayName())),

            ExtraCodecs.listOrValue(ExtraCodecs.COMPONENT)
                .optionalFieldOf("lore", List.of())
                .forGetter((item) -> {
                    ItemMeta meta = item.getItemMeta();
                    List<Component> lore = meta.lore();
                    return Objects.requireNonNullElseGet(lore, List::of);
                }),

            Codec.INT.optionalFieldOf("custom-model-data").forGetter((o) -> {
              ItemMeta meta = o.getItemMeta();
              return meta.hasCustomModelData() ? Optional.of(meta.getCustomModelData()) : Optional.empty();
            })
        )

        .apply(instance, (material, integer, name, lore, textureId) -> {
          ItemStack item = new ItemStack(material, integer);
          ItemMeta meta = item.getItemMeta();

          name.ifPresent((component) -> meta.displayName(Text.wrapForItems(component)));

          List<Component> lores = new ArrayList<>();

          lore.stream()
              .map(Text::splitNewlines)
              .flatMap(Collection::stream)
              .map(Text::wrapForItems)
              .forEach(lores::add);

          meta.lore(lore);
          item.setItemMeta(meta);

          return item;
    });
  });
}
