package net.arcadiusmc.scripts;

import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyInstantiable;

/**
 * Object that is both a constructor and a function. (Calling it either as a constructor or as a
 * function yields the same result)
 */
public interface CtorFunction extends ProxyExecutable, ProxyInstantiable {

  @Override
  default Object newInstance(Value... arguments) {
    return execute(arguments);
  }
}
