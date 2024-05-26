package net.arcadiusmc.scripts;

import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

public abstract class IdProxyObject implements ProxyObject {

  public static final int UNKNOWN_ID = -1;

  private CachedMethod[] methods;
  private KeyArray array;

  protected final void initMembers(int maxId) {
    int length = maxId + 1;
    methods = new CachedMethod[length];

    for (int i = 0; i < length; i++) {
      String name = getName(i);
      CachedMethod method = new CachedMethod(i, name, this);

      methods[i] = method;
    }
  }

  protected abstract String getName(int id);

  protected abstract int getId(String name);

  public Object invokeMember(String member, Value... args) {
    int id = getId(member);
    Invocation invocation = new Invocation(id, member);

    if (id == UNKNOWN_ID) {
      throw invocation.unknown();
    }

    return invoke(invocation, args);
  }

  public Object invoke(Invocation f, Value... args) {
    return null;
  }

  @Override
  public Object getMember(String key) {
    int id = getId(key);
    if (id == UNKNOWN_ID || methods == null) {
      return null;
    }

    return methods[id];
  }

  @Override
  public void putMember(String key, Value value) {
    int id = getId(key);
    if (id == UNKNOWN_ID) {
      return;
    }

    throw new IllegalStateException("Cannot override method");
  }

  @Override
  public boolean hasMember(String key) {
    return getId(key) != UNKNOWN_ID;
  }

  @Override
  public Object getMemberKeys() {
    if (array == null) {
      array = new KeyArray();
    }

    return array;
  }

  class KeyArray implements ProxyArray {

    @Override
    public Object get(long index) {
      CachedMethod method = methods[(int) index];
      return method.name;
    }

    @Override
    public void set(long index, Value value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public long getSize() {
      return methods.length;
    }
  }

  public record Invocation(int methodId, String member) {

    public RuntimeException unknown() {
      throw new IllegalStateException("Unknown method ID: " + methodId + ", for method: " + member);
    }
  }

  private record CachedMethod(int id, String name, IdProxyObject handle)
      implements ProxyExecutable
  {

    @Override
    public Object execute(Value... arguments) {
      Invocation invocation = new Invocation(id, name);
      return handle.invoke(invocation, arguments);
    }
  }
}
