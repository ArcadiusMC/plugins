package net.arcadiusmc.utils.io.source;

import com.google.gson.JsonElement;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import net.arcadiusmc.utils.io.TagOps;
import net.forthecrown.nbt.BinaryTag;

/**
 * A source for a stream of characters
 */
public interface Source {

  /**
   * Reads the source's content
   * @return Source content
   * @throws IOException If an IO Error occurred
   */
  StringBuffer read() throws IOException;

  /**
   * Gets the source's name
   * @return Source's name
   */
  String name();

  /**
   * Saves this source
   * @param ops Dynamic serialization format to save to
   * @return Save result
   */
  <S> DataResult<S> save(DynamicOps<S> ops);

  /**
   * Saves this source into JSON
   * @return JSON representation
   */
  default JsonElement saveAsJson() {
    return save(JsonOps.INSTANCE).getOrThrow();
  }

  /**
   * Saves this source into an NBT tag
   * @return NBT representation
   */
  default BinaryTag saveAsTag() {
    return save(TagOps.OPS).getOrThrow();
  }
}