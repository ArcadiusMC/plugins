package net.arcadiusmc.scripts;

import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

public abstract class NonCtorFunction extends BaseFunction {

  @Override
  public abstract Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args);

  @Override
  public Scriptable construct(Context cx, Scriptable scope, Object[] args) {
    throw ScriptUtils.notConstructor();
  }
}
