package net.arcadiusmc.scripts.nbt;

import lombok.Getter;
import net.arcadiusmc.scripts.StringArray;
import net.forthecrown.nbt.BinaryTag;
import net.forthecrown.nbt.CompoundTag;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;

public class ScriptCompoundTag implements ProxyObject {

  @Getter
  private final CompoundTag backing;

  public ScriptCompoundTag(CompoundTag backing) {
    this.backing = backing;
  }

  @Override
  public Object getMember(String key) {
    return ScriptNbt.scriptWrap(backing.get(key));
  }

  @Override
  public Object getMemberKeys() {
    return new StringArray(backing.keySet().toArray(String[]::new));
  }

  @Override
  public boolean hasMember(String key) {
    return backing.contains(key);
  }

  @Override
  public void putMember(String key, Value value) {
    BinaryTag unwrapped = ScriptNbt.unwrap(value);
    backing.put(key, unwrapped);
  }
}
