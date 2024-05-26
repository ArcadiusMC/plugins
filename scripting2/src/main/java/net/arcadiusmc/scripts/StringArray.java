package net.arcadiusmc.scripts;

import java.util.Objects;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyArray;

public class StringArray implements ProxyArray {

  String[] values;

  public StringArray(String[] values) {
    Objects.requireNonNull(values);
    this.values = values;
  }

  @Override
  public long getSize() {
    return values.length;
  }

  @Override
  public void set(long index, Value value) {
    values[(int) index] = value.asString();
  }

  @Override
  public Object get(long index) {
    return values[(int) index];
  }
}
