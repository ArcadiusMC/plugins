package net.arcadiusmc.menu;

import com.google.common.base.Preconditions;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.PrimitiveCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import lombok.Getter;
import net.arcadiusmc.utils.io.FtcCodecs;
import net.arcadiusmc.utils.io.JsonWrapper;
import net.arcadiusmc.utils.io.Results;
import org.apache.commons.lang3.Validate;

/**
 * A slot is basically an 2D integer vector for inventory slot positions.
 * <p>
 * They use a column (X) position and row (Y) position to map themselves to an inventory slot
 * index.
 * <p>
 * Using these coordinates instead of slots, for square container inventories, helps make placing
 * items in them easier and more human-readable, after all,
 * <code>column=2 row=3</code> is a lot easier to understand and visualize than
 * <code>slot=29</code>
 */
@Getter
public class Slot {

  /* ----------------------------- CONSTANTS ------------------------------ */

  public static final Codec<Slot> CODEC;

  /**
   * The size of possible column positions (x coordinates) in an inventory, basically max column pos
   * + 1
   */
  public static final int X_SIZE = 9;

  /**
   * The size of possible row positions (y coordinate) in an inventory, basically max row pos + 1
   */
  public static final int Y_SIZE = 6;

  public static final Slot ZERO;

  private static final Slot[]   INDEX_CACHE;
  private static final Slot[][] XY_CACHE;

  static {
    int slotsSize = X_SIZE * Y_SIZE;
    INDEX_CACHE = new Slot[slotsSize];
    XY_CACHE = new Slot[X_SIZE][Y_SIZE];

    // Cache all slots
    int i = 0;
    for (int y = 0; y < Y_SIZE; y++) {
      for (int x = 0; x < X_SIZE; x++) {
        Slot slot = new Slot(x, y, i);

        INDEX_CACHE[i] = slot;
        XY_CACHE[x][y] = slot;

        i++;
      }
    }

    ZERO = INDEX_CACHE[0];
  }

  /* -------------------------- INSTANCE FIELDS --------------------------- */

  /**
   * The slot's column (x) position
   */
  private final byte x;

  /**
   * slot's row (y) position
   */
  private final byte y;

  /**
   * The slot's inventory index
   */
  private final int index;

  /* ---------------------------- CONSTRUCTOR ----------------------------- */

  private Slot(int x, int y, int index) {
    this.y = (byte) y;
    this.x = (byte) x;
    this.index = index;
  }

  /* -------------------------- STATIC FUNCTIONS -------------------------- */

  /**
   * Gets the slot value of the given column (x) and row (y)
   *
   * @param x The column (X) position of the slot
   * @param y    The row (Y) position of the slot
   * @return The slot at the given coordinates
   */
  public static Slot of(int x, int y) {
    validateSlot(x, y);
    return XY_CACHE[x][y];
  }

  /**
   * Gets a slot from the given inventory index
   *
   * @param slot The slot to get the object of
   * @return The slot at the given index
   */
  public static Slot of(int slot) {
    return INDEX_CACHE[slot];
  }

  /**
   * Translates the given column and row into a singular inventory index
   *
   * @param column The column (X) position
   * @param row    The row (Y) position
   * @return The inventory index
   */
  public static int toIndex(int column, int row) {
    return (row * X_SIZE) + column;
  }

  public static void validateSlot(int index) {
    Objects.checkIndex(index, INDEX_CACHE.length);
  }

  public static void validateSlot(int x, int y) {
    // Ensure both column and row are in
    // inventory bounds
    Validate.isTrue(x >= 0 && x < X_SIZE,
        "Invalid column, must be in range [0..%s], found: %s",
        X_SIZE - 1, x
    );

    Validate.isTrue(y >= 0 && y < Y_SIZE,
        "Invalid row, must be in range [0..%s], found: %s",
        Y_SIZE - 1, y
    );
  }

  /* ------------------------- INSTANCE METHODS --------------------------- */

  /**
   * Adds the given amount to this slot's column (X) and row (Y) positions
   *
   * @param x X-axis addition amount
   * @param y    Y-axis addition amount
   * @return The slot at that position
   */
  public Slot add(int x, int y) {
    return of(this.x + x, this.y + y);
  }

  /* ----------------------------- LOADER ------------------------------ */

  public static Slot load(JsonElement element) {
    if (element.isJsonPrimitive()) {
      int number = element.getAsInt();
      return of(number);
    }

    if (element.isJsonArray()) {
      JsonArray arr = element.getAsJsonArray();

      Preconditions.checkState(arr.size() == 2,
          "Slot array must be made of 2 numbers, a column and row position"
      );

      int col = arr.get(0).getAsInt();
      int row = arr.get(1).getAsInt();

      return of(col, row);
    }

    JsonWrapper json = JsonWrapper.wrap(element.getAsJsonObject());

    int x = getInt(json, "column", "x");
    int y = getInt(json, "row", "y");

    return of(x, y);
  }

  private static int getInt(JsonWrapper json, String key, String fallbackKey) {
    if (json.has(key)) {
      return json.getInt(key);
    }

    if (json.has(fallbackKey)) {
      return json.getInt(fallbackKey);
    }

    throw new IllegalStateException("No '" + key + "' or '" + fallbackKey + "' element in JSON");
  }

  // --- OBJECT OVERRIDES ---

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Slot slot)) {
      return false;
    }

    return index == slot.index;
  }

  @Override
  public int hashCode() {
    return index;
  }

  @Override
  public String toString() {
    return "(x=" + x + ", y=" + y + ", index=" + index + ")";
  }

  static {
    final Codec<Slot> recordCodec = RecordCodecBuilder.create(instance -> {
      return instance
          .group(
              Codec.mapEither(Codec.BYTE.fieldOf("x"), Codec.BYTE.fieldOf("column"))
                  .xmap(e -> e.map(b -> b, b -> b), Either::left)
                  .forGetter(Slot::getX),

              Codec.mapEither(Codec.BYTE.fieldOf("y"), Codec.BYTE.fieldOf("row"))
                  .xmap(e -> e.map(b -> b, b -> b), Either::left)
                  .forGetter(Slot::getY)
          )
          .apply(instance, Slot::of);
    });

    final Codec<Slot> stringCodec = new PrimitiveCodec<>() {
      @Override
      public <T> DataResult<Slot> read(DynamicOps<T> ops, T input) {
        return ops.getStringValue(input).flatMap(string -> {
          return FtcCodecs.safeParse(string, reader -> {
            reader.skipWhitespace();
            int x = reader.readInt();
            reader.skipWhitespace();
            reader.expect(',');
            reader.skipWhitespace();
            int y = reader.readInt();
            return Slot.of(x, y);
          });
        });
      }

      @Override
      public <T> T write(DynamicOps<T> ops, Slot value) {
        return ops.createString(value.getX() + "," + value.getY());
      }
    };

    final Codec<Slot> indexCodec = Codec.INT
        .comapFlatMap(
            integer -> {
              if (integer < 0) {
                return Results.error("Inventory index may not be less than 0");
              }

              int max = X_SIZE * Y_SIZE;
              if (integer >= max) {
                return Results.error("Inventory index may not be greater than %s", max-1);
              }

              return Results.success(of(integer));
            },
            Slot::getIndex
        );

    CODEC = FtcCodecs.combine(stringCodec, recordCodec, indexCodec);
  }
}