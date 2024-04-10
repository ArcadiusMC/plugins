package net.arcadiusmc.menu;

import static net.arcadiusmc.menu.Menus.MAX_INV_SIZE;
import static net.arcadiusmc.menu.Menus.MIN_INV_SIZE;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.serialization.Codec;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import net.arcadiusmc.utils.io.ExtraCodecs;
import net.arcadiusmc.utils.io.Results;
import net.forthecrown.grenadier.types.ArgumentTypes;

public final class MenuCodecs {
  private MenuCodecs() {}

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
    suffixes.put("row", Slot.X_SIZE);
    suffixes.put("rows", Slot.X_SIZE);
    suffixes.put("r", Slot.X_SIZE);

    ArgumentType<Integer> sizeParser
        = ArgumentTypes.suffixedInt(suffixes, MIN_INV_SIZE, MAX_INV_SIZE);

    INV_SIZE_STRING = Codec.STRING.comapFlatMap(string -> {
      return ExtraCodecs.safeParse(string, sizeParser);
    }, String::valueOf);

    INVENTORY_SIZE = ExtraCodecs.combine(INV_SIZE_INT, INV_SIZE_STRING);
  }

}
