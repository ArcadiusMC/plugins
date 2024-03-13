package net.arcadiusmc.usables.items;

import com.google.common.base.Strings;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.function.Function;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.usables.Interaction;
import net.arcadiusmc.usables.Usables;
import net.arcadiusmc.utils.Result;
import net.arcadiusmc.utils.inventory.ItemList;
import net.arcadiusmc.utils.inventory.ItemLists;
import net.arcadiusmc.utils.io.ExtraCodecs;
import net.forthecrown.grenadier.types.ParsedPosition.Coordinate;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

interface ItemProvider {
  Codec<ItemProvider> CODEC = makeCodec();

  private static Codec<ItemProvider> makeCodec() {
    Codec<ItemProvider> listCodec = (Codec) ItemListProvider.CODEC;
    Codec<ItemProvider> refCodec = (Codec) PositionRefItemProvider.CODEC;

    return Codec.either(listCodec, refCodec)
        .xmap(
            either -> either.map(Function.identity(), Function.identity()),
            provider -> {
              if (provider instanceof ItemListProvider) {
                return Either.left(provider);
              }
              return Either.right(provider);
            }
        );
  }

  Result<ItemList> getItems(Interaction interaction);

  Component displayInfo();

  record ItemListProvider(ItemList list) implements ItemProvider {
    static final Codec<ItemListProvider> CODEC = ExtraCodecs.ITEM_LIST_CODEC
        .xmap(ItemListProvider::new, ItemListProvider::list);

    @Override
    public Result<ItemList> getItems(Interaction interaction) {
      return Result.success(list);
    }

    @Override
    public Component displayInfo() {
      return Usables.hoverableItemList(list);
    }
  }

  record PositionRefItemProvider(String worldName, Coordinate x, Coordinate y, Coordinate z)
      implements ItemProvider
  {
    static final Codec<Coordinate> COORDINATE_CODEC = Codec.STRING
        .comapFlatMap(
            s -> ExtraCodecs.safeParse(s, CoordinateParser.X),
            PositionRefItemProvider::toString
        );

    static final Codec<PositionRefItemProvider> CODEC = RecordCodecBuilder.create(instance -> {
      return instance
          .group(
              Codec.STRING.optionalFieldOf("world_name")
                  .forGetter(o -> Optional.ofNullable(Strings.emptyToNull(o.worldName))),

              COORDINATE_CODEC.fieldOf("x").forGetter(o -> o.x),
              COORDINATE_CODEC.fieldOf("y").forGetter(o -> o.y),
              COORDINATE_CODEC.fieldOf("z").forGetter(o -> o.z)
          )
          .apply(instance, (s, x, z, y) -> new PositionRefItemProvider(s.orElse(null), x, z, y));
    });

    World getWorld(Interaction interaction) {
      if (!Strings.isNullOrEmpty(worldName)) {
        World world = Bukkit.getWorld(worldName);

        if (world != null) {
          return world;
        }
      }

      return interaction.getValue("world", World.class)
          .or(() -> interaction.getValue("location", Location.class).map(Location::getWorld))
          .orElse(null);
    }

    Location getBaseLocation(World world, Interaction interaction) {
      return interaction.getValue("location", Location.class)
          .map(location -> {
            location = location.clone();
            location.setWorld(world);
            return location;
          })
          .orElseGet(() -> new Location(world, 0, 0, 0));
    }

    @Override
    public Result<ItemList> getItems(Interaction interaction) {
      World world = getWorld(interaction);

      if (world == null) {
        return Result.error("No world found (world-name='%s')", worldName);
      }

      Location base = getBaseLocation(world, interaction);
      double x = this.x.apply(base.x());
      double y = this.y.apply(base.y());
      double z = this.z.apply(base.z());

      base.setX(x);
      base.setY(y);
      base.setZ(z);

      Block block = base.getBlock();
      BlockState state = block.getState();

      if (!(state instanceof InventoryHolder holder)) {
        return Result.error("Block at %s %s %s is not a container!", x, y, z);
      }

      Inventory inventory = holder.getInventory();
      ItemList list = ItemLists.cloneAllItems(ItemLists.fromInventory(inventory));

      return Result.success(list);
    }

    @Override
    public Component displayInfo() {
      Component hover = Text.format("world={0}\nx={1}\ny={2}\nz={3}",
          Strings.isNullOrEmpty(worldName) ? "UNSET" : worldName,
          toString(x), toString(y), toString(z)
      );

      return Component.text("[Container reference; hover for more details]", NamedTextColor.AQUA)
          .hoverEvent(hover);
    }

    static String toString(Coordinate coordinate) {
      return (coordinate.relative() ? "~" : "") + coordinate.value();
    }
  }
}
