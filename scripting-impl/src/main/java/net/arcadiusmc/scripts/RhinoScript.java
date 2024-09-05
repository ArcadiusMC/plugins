package net.arcadiusmc.scripts;

import static org.mozilla.javascript.Context.VERSION_ES6;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.scripts.preprocessor.PreProcessor;
import net.arcadiusmc.utils.io.source.Source;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeJavaClass;
import org.mozilla.javascript.NativeJavaMethod;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;
import org.slf4j.Logger;

@Accessors(chain = true)
public class RhinoScript implements Script {

  private static final String SCRIPT_INST = "__scriptInstance";

  public static final String[] EMPTY_STRING_ARRAY = {};
  public static final Object[] EMPTY_OBJECT_ARRAY = {};

  @Getter
  private final Source source;

  @Getter
  private final ScriptManager service;

  @Getter
  private final Logger logger;

  @Getter
  private String[] arguments = EMPTY_STRING_ARRAY;

  NativeObject bindingScope;
  NativeObject evaluationScope;

  private org.mozilla.javascript.Script compiled;

  @Getter @Setter
  private Script parent;
  private final List<Script> children = new ObjectArrayList<>();

  private boolean useClassGen;

  @Getter @Setter
  private Path workingDirectory;

  @Getter
  private final ScriptLoader loader;

  private final SchedulerExtension scheduler;
  private final EventsExtension events;

  public RhinoScript(ScriptLoader loader, ScriptManager service, Source source) {
    this.source  = Objects.requireNonNull(source, "Null source");
    this.service = Objects.requireNonNull(service, "Null service");
    this.loader  = Objects.requireNonNull(loader, "Null loader");

    this.logger = Loggers.getLogger(getName());

    this.events = new EventsExtension(service.getScriptPlugin(), this);
    this.scheduler = new SchedulerExtension(service.getScriptPlugin(), this);
  }

  public static Script fromScope(Scriptable scope) {
    Object value;

    while (scope != null) {
      value = ScriptableObject.getProperty(scope, SCRIPT_INST);

      if (value == Scriptable.NOT_FOUND || !(value instanceof Script script)) {
        scope = scope.getParentScope();
        continue;
      }

      return script;
    }

    return null;
  }

  @Override
  public Script setArguments(String... arguments) {
    if (arguments == null || arguments.length < 1) {
      this.arguments = EMPTY_STRING_ARRAY;
    } else {
      this.arguments = arguments.clone();
    }

    return this;
  }

  @Override
  public boolean useClassGen() {
    return useClassGen;
  }

  @Override
  public Script setClassGen(boolean classGen) {
    this.useClassGen = classGen;
    return this;
  }

  @Override
  public boolean isCompiled() {
    return compiled != null;
  }

  private void ensureCompiled() throws IllegalStateException {
    Preconditions.checkState(
        compiled != null,
        "Script '%s' is not compiled", getName()
    );
  }

  @Override
  public Object get(@NotNull String bindingName) throws IllegalStateException {
    ensureCompiled();
    Object value = ScriptableObject.getProperty(evaluationScope, bindingName);

    if (value == Context.getUndefinedValue() || value == null) {
      return null;
    }

    if (value == Scriptable.NOT_FOUND) {
      return value;
    }

    return toJava(value);
  }

  @Override
  public boolean hasMethod(String methodName) throws IllegalStateException {
    return get(methodName) instanceof Callable;
  }

  @Override
  public Script put(@NotNull String bindingName, @Nullable Object binding)
      throws IllegalStateException
  {
    ensureCompiled();
    Object wrapped = toScriptObject(bindingName, binding);
    ScriptableObject.putProperty(bindingScope, bindingName, wrapped);

    if (binding instanceof RhinoScript script && !script.equals(this)) {
      addChild(script);
    }

    return this;
  }

  @Override
  public Script putConst(@NotNull String bindingName, @Nullable Object binding)
      throws IllegalStateException
  {
    ensureCompiled();
    Object wrapped = toScriptObject(bindingName, binding);
    bindingScope.putConst(bindingName, bindingScope, wrapped);

    if (binding instanceof RhinoScript script) {
      addChild(script);
    }

    return this;
  }

  @Override
  public Script putValue(
      @NotNull String bindingName,
      Supplier<Object> getter,
      Consumer<Object> setter
  ) throws IllegalStateException {
    ensureCompiled();
    bindingScope.defineProperty(bindingName, getter, setter, 0);
    return this;
  }

  Object toScriptObject(String label, Object o) {
    if (o instanceof Class<?> clazz) {
      return new NativeJavaClass(bindingScope, clazz);
    }

    if (o instanceof Method method) {
      return new NativeJavaMethod(method, label);
    }

    if (o instanceof RhinoScript other) {
      other.ensureCompiled();
      return new ScriptObject(other);
    }

    return o;
  }

  @Override
  public Object remove(@NotNull String bindingName) throws IllegalStateException {
    ensureCompiled();
    Object existing = get(bindingName);
    bindingScope.delete(bindingName);

    if (existing instanceof Script script) {
      removeChild(script);
    }

    return existing;
  }

  @Override
  public Set<Entry<Object, Object>> bindingEntries() throws IllegalStateException {
    ensureCompiled();
    return bindingScope.entrySet();
  }

  @Override
  public Context context() {
    Context ctx = Context.enter();

    ctx.setLanguageVersion(VERSION_ES6);
    ctx.setOptimizationLevel(useClassGen ? 9 : -1);
    ctx.setApplicationClassLoader(getClass().getClassLoader());

    return ctx;
  }

  @Override
  public Script compile() {
    if (compiled != null) {
      close();
    }

    StringBuffer buf;

    try {
      buf = source.read();
    } catch (IOException exc) {
      throw new ScriptLoadException(exc);
    }

    PreProcessor processor = new PreProcessor(buf);
    String str = processor.run();

    try (Context ctx = context()) {
      compiled = ctx.compileString(str, getName(), 1, null);
      bindingScope = new NativeObject();
      evaluationScope = new NativeObject();

      Scriptable topLevelScope = service.getTopLevelScope(ctx);
      bindingScope.setParentScope(topLevelScope);
      evaluationScope.setParentScope(bindingScope);

      put("args", createArgsArray(ctx));
      put("logger", logger);
      put("_script", bindingScope);
      put(SCRIPT_INST, this);
      ScriptableObject.putProperty(evaluationScope, SCRIPT_INST, this);

      put("events", events);
      put("scheduler", scheduler);

      var modules = service.getModules();
      modules.applyAutoImports(this).orThrow(s -> {
        return new ScriptLoadException("Failed to auto-import modules: " + s);
      });

      var callbackResult = processor.runCallbacks(this);

      if (callbackResult.isError()) {
        throw new ScriptLoadException(
            "Error(s) during script loading:\n" + callbackResult.getError()
        );
      }

    } catch (Throwable t) {
      var message = getMessage(t);
      throw new ScriptLoadException(message, t);
    }

    return this;
  }

  private Scriptable createArgsArray(Context cx) {
    Scriptable array = cx.newArray(evaluationScope, arguments.length);

    for (int i = 0; i < arguments.length; i++) {
      String arg = arguments[i];
      ScriptableObject.putProperty(array, i, arg);
    }

    return array;
  }

  @Override
  public ExecResult<Object> evaluate() {
    if (compiled == null) {
      return ExecResultImpl.error("Script not compiled", this);
    }

    try (Context ctx = context()) {
      try {
        Object o = compiled.exec(ctx, evaluationScope);
        return ExecResultImpl.success(transformResult(o), this);
      } catch (Throwable t) {
        return ExecResultImpl.wrap(t, this);
      }
    }
  }

  @Override
  public Script clearEvaluationBindings() {
    if (evaluationScope == null) {
      return this;
    }

    evaluationScope = new NativeObject();
    evaluationScope.setParentScope(bindingScope);

    return this;
  }

  @Override
  public ExecResult<Object> invoke(String methodName, Object... arguments) {
    Objects.requireNonNull(methodName);

    if (compiled == null) {
      return ExecResultImpl.error("Script not compiled", this);
    }

    Object value = get(methodName);

    if (!(value instanceof Callable callable)) {
      return ExecResultImpl.error("No such method found", methodName, this);
    }

    try (Context ctx = context()) {
      Object[] args;

      if (arguments.length > 0) {
        args = new Object[arguments.length];
        for (int i = 0; i < arguments.length; i++) {
          args[i] = Context.javaToJS(arguments[i], evaluationScope, ctx);
        }
      } else {
        args = ScriptRuntime.emptyArgs;
      }

      try {
        Object returnValue = callable.call(ctx, evaluationScope, evaluationScope, args);
        return ExecResultImpl.success(transformResult(returnValue), methodName, this);
      } catch (Throwable exc) {
        return ExecResultImpl.wrap(exc, methodName, this);
      }
    }
  }

  static Object transformResult(Object o) {
    if (o instanceof Undefined) {
      return null;
    }

    return toJava(o);
  }

  static String getMessage(Throwable throwable) {
    String message = throwable.getMessage();

    while (message == null && throwable.getCause() != null) {
      var thr = throwable.getCause();
      message = thr.getMessage();
    }

    return Objects.requireNonNullElse(message, throwable.toString());
  }

  @Override
  public ImmutableList<Script> getChildren() {
    if (children.isEmpty()) {
      return ImmutableList.of();
    } else {
      return ImmutableList.copyOf(children);
    }
  }

  @Override
  public Script addChild(Script child) {
    Objects.requireNonNull(child, "Null child");
    Preconditions.checkArgument(!child.equals(this), "addChild called with self");

    if (children.add(child)) {
      ((RhinoScript) child).setParent(this);
    }

    return this;
  }

  @Override
  public Script removeChild(Script child) {
    Objects.requireNonNull(child, "Null child");

    if (children.remove(child)) {
      ((RhinoScript) child).setParent(null);
    }

    return this;
  }

  @Override
  public NativeObject getScriptObject() throws IllegalStateException {
    ensureCompiled();
    return evaluationScope;
  }

  @Override
  public void close() {
    if (compiled == null) {
      return;
    }

    var onCloseResult = invoke("__onClose");
    onCloseResult.error().ifPresent(string -> {
      if (string.contains("No such method found")) {
        return;
      }

      onCloseResult.logError();
    });

    if (parent != null) {
      parent.removeChild(this);
    }

    events.onScriptClose();
    scheduler.onScriptClose();

    children.forEach(Script::close);
    children.clear();

    compiled = null;
    bindingScope = null;
    evaluationScope = null;
  }

  static Object toJava(Object jsObject) {
    return Context.jsToJava(jsObject, Object.class);
  }
}