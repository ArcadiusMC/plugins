package net.arcadiusmc.scripts.builtins;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import net.arcadiusmc.scripts.Scripts;
import net.arcadiusmc.text.Text;
import net.kyori.adventure.text.format.TextColor;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.joml.Vector2f;
import org.joml.Vector3f;

public enum LerpFunction implements ProxyExecutable {
  LERP;

  private final List<Interpolator> interpolators;

  LerpFunction() {
    interpolators = new ArrayList<>();

    interpolators.add(
        new Interpolator<>(Vector3f.class) {
          @Override
          Vector3f lerp(float progress, Vector3f t1, Vector3f t2) {
            Vector3f dif = new Vector3f(t2);
            dif.sub(t1);
            return new Vector3f(t1).add(dif.mul(progress));
          }
        }
    );

    interpolators.add(
        new Interpolator<>(Vector2f.class) {
          @Override
          Vector2f lerp(float progress, Vector2f t1, Vector2f t2) {
            Vector2f dif = new Vector2f(t2);
            dif.sub(t1);
            return new Vector2f(t1).add(dif.mul(progress));
          }
        }
    );

    interpolators.add(
        new Interpolator<>(TextColor.class) {
          @Override
          TextColor lerp(float progress, TextColor t1, TextColor t2) {
            return Text.hsvLerp(progress, t1, t2);
          }
        }
    );
  }

  @Override
  public Object execute(Value... arguments) {
    Scripts.ensureParameterCount(arguments, 2);

    float progress = arguments[0].asFloat();
    int size = arguments.length - 1;
    Value[] values;

    if (size == 1) {
      Value first = arguments[1];

      if (!first.hasArrayElements()) {
        return first;
      }

      int arrSize = (int) first.getArraySize();
      values = new Value[arrSize];

      for (int i = 0; i < arrSize; i++) {
        values[i] = first.getArrayElement(i);
      }
    } else if (size == 2) {
      return lerp2(progress, arguments[1], arguments[2]);
    } else {
      values = arguments;
    }

    final int maxIndex = size - 1;
    int firstIndex = (int) (progress * maxIndex);
    float firstStep = (float) firstIndex / maxIndex;
    float localStep = (progress - firstStep) * maxIndex;

    Value v1 = values[firstIndex];
    Value v2 = values[firstIndex + 2];

    return lerp2(localStep, v1, v2);
  }

  private Object lerp2(float prog, Value v1, Value v2) {
    if (v1.isNumber()) {
      double d1 = v1.asDouble();
      double d2 = v2.asDouble();
      return d1 + ((d2 - d1) * prog);
    }

    if (v1.isHostObject()) {
      Object h1 = v1.asHostObject();
      Object h2 = v2.asHostObject();

      for (Interpolator<Object> interpolator : interpolators) {
        Class<?> type  = interpolator.getType();

        if (!type.isInstance(h1) || !type.isInstance(h2)) {
          continue;
        }

        return interpolator.lerp(prog, h1, h2);
      }
    }

    throw Scripts.typeError("Don't know how to interpolate between %s and %s", v1, v2);
  }

  abstract class Interpolator<T> {

    @Getter
    private final Class<T> type;

    public Interpolator(Class<T> type) {
      this.type = type;
    }

    abstract T lerp(float progress, T t1, T t2);
  }
}
