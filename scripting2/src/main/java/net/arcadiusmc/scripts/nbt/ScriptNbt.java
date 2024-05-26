package net.arcadiusmc.scripts.nbt;

import static net.forthecrown.nbt.TypeIds.BYTE;
import static net.forthecrown.nbt.TypeIds.BYTE_ARRAY;
import static net.forthecrown.nbt.TypeIds.COMPOUND;
import static net.forthecrown.nbt.TypeIds.DOUBLE;
import static net.forthecrown.nbt.TypeIds.FLOAT;
import static net.forthecrown.nbt.TypeIds.INT;
import static net.forthecrown.nbt.TypeIds.INT_ARRAY;
import static net.forthecrown.nbt.TypeIds.LIST;
import static net.forthecrown.nbt.TypeIds.LONG;
import static net.forthecrown.nbt.TypeIds.LONG_ARRAY;
import static net.forthecrown.nbt.TypeIds.SHORT;
import static net.forthecrown.nbt.TypeIds.STRING;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.arcadiusmc.scripts.Scripts;
import net.forthecrown.nbt.BinaryTag;
import net.forthecrown.nbt.BinaryTags;
import net.forthecrown.nbt.CompoundTag;
import net.forthecrown.nbt.ListTag;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.Proxy;

public final class ScriptNbt {
  private ScriptNbt() {}

  public static Object scriptWrap(BinaryTag tag) {
    return switch (tag.getId()) {
      case BYTE -> tag.asNumber().byteValue();
      case SHORT -> tag.asNumber().shortValue();
      case INT -> tag.asNumber().intValue();
      case FLOAT -> tag.asNumber().floatValue();
      case DOUBLE -> tag.asNumber().doubleValue();
      case LONG -> tag.asNumber().longValue();

      case STRING -> tag.asString().toString();

      case COMPOUND -> new ScriptCompoundTag(tag.asCompound());

      case BYTE_ARRAY, LONG_ARRAY, INT_ARRAY -> tag;

      case LIST -> {
        List<Object> values = new ArrayList<>();
        for (BinaryTag binaryTag : tag.asList()) {
          values.add(scriptWrap(binaryTag));
        }
        yield values;
      }

      default -> Scripts.typeError("Unknown tag type: " + tag.getId());
    };
  }

  public static BinaryTag unwrap(Value value) {
    if (value == null || value.isNull()) {
      return BinaryTags.endTag();
    }

    if (value.isHostObject()) {
      if (value.asHostObject() instanceof BinaryTag tag) {
        return tag;
      }

      throw Scripts.cantLoad("BinaryTag", value);
    }

    if (value.isProxyObject()) {
      Proxy proxy = value.asProxyObject();

      if (proxy instanceof ScriptCompoundTag compoundTag) {
        return compoundTag.getBacking();
      }
    }

    if (value.hasMembers()) {
      Set<String> members = value.getMemberKeys();
      CompoundTag tag = BinaryTags.compoundTag();

      for (String member : members) {
        Value memberValue = value.getMember(member);
        BinaryTag unwrapped = unwrap(memberValue);
        tag.put(member, unwrapped);
      }

      return tag;
    }

    if (value.hasArrayElements()) {
      ListTag ltag = BinaryTags.listTag();

      for (long i = 0; i < value.getArraySize(); i++) {
        Value elem = value.getArrayElement(i);
        ltag.add(unwrap(elem));
      }

      return ltag;
    }

    if (value.isBoolean()) {
      return BinaryTags.byteTag(value.asBoolean() ? 1 : 0);
    }

    if (value.isString()) {
      return BinaryTags.stringTag(value.asString());
    }

    if (value.isNumber()) {
      if (value.fitsInByte()) {
        return BinaryTags.byteTag(value.asByte());
      }
      if (value.fitsInShort()) {
        return BinaryTags.shortTag(value.asShort());
      }
      if (value.fitsInInt()) {
        return BinaryTags.intTag(value.asInt());
      }
      if (value.fitsInLong()) {
        return BinaryTags.longTag(value.asLong());
      }
      if (value.fitsInFloat()) {
        return BinaryTags.floatTag(value.asFloat());
      }

      return BinaryTags.doubleTag(value.asDouble());
    }

    throw Scripts.cantLoad("BinaryTag", value);
  }
}
