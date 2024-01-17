package net.arcadiusmc.scripts.module;

import net.arcadiusmc.scripts.Script;
import org.mozilla.javascript.Scriptable;

public interface JsModule {

  Scriptable getSelfObject(Scriptable scope);

  default void onImportFail(Script script) {

  }

  default void close() {

  }
}
