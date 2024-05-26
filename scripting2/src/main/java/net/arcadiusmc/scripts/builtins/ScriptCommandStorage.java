package net.arcadiusmc.scripts.builtins;

import net.arcadiusmc.scripts.Scripts;
import net.arcadiusmc.scripts.nbt.ScriptCompoundTag;
import net.arcadiusmc.scripts.nbt.ScriptNbt;
import net.arcadiusmc.utils.VanillaAccess;
import net.forthecrown.nbt.BinaryTag;
import net.forthecrown.nbt.CompoundTag;
import org.bukkit.NamespacedKey;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;

public class ScriptCommandStorage implements ProxyObject {

  NamespacedKey getKey(String name) {
    int colonIndex = name.indexOf(':');
    if (colonIndex == -1) {
      name = "arcadius-scripts:" + name;
    }

    return NamespacedKey.fromString(name);
  }

  @Override
  public Object getMember(String key) {
    NamespacedKey rkey = getKey(key);
    if (rkey == null) {
      return null;
    }

    CompoundTag tag = VanillaAccess.getStoredData(rkey);
    return new ScriptCompoundTag(tag);
  }

  @Override
  public Object getMemberKeys() {
    return VanillaAccess.getStorageKeys()
        .map(NamespacedKey::asString)
        .toArray(String[]::new);
  }

  @Override
  public boolean hasMember(String key) {
    return getKey(key) != null;
  }

  @Override
  public void putMember(String key, Value value) {
    BinaryTag tag = ScriptNbt.unwrap(value);
    NamespacedKey rkey = getKey(key);

    if (rkey == null) {
      throw Scripts.typeError("Invalid namespaced key: %s", key);
    }
    if (tag.isCompound()) {
      throw Scripts.typeError("Value not an object");
    }

    VanillaAccess.setStoredData(rkey, tag.asCompound());
  }
}
