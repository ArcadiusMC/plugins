package net.arcadiusmc.scripts.builtins;

import net.arcadiusmc.scripts.CtorFunction;
import net.arcadiusmc.scripts.Scripts;
import org.graalvm.polyglot.Value;
import org.joml.Math;
import org.joml.Vector2d;
import org.joml.Vector2f;
import org.joml.Vector3d;
import org.joml.Vector3f;

public interface VectorFunctions {

  CtorFunction VEC3 = arguments -> {
    int len = Math.min(arguments.length, 3);

    switch (len) {
      case 3 -> {
        float x = arguments[0].asFloat();
        float y = arguments[1].asFloat();
        float z = arguments[2].asFloat();
        return new Vector3f(x, y, z);
      }

      case 2 -> {
        float x = arguments[0].asFloat();
        float y = arguments[1].asFloat();
        return new Vector3f(x, y, 0);
      }

      case 1 -> {
        Value arg = arguments[0];

        if (arg.isNumber()) {
          float f = arg.asFloat();
          return new Vector3f(f, f, f);
        }

        if (arg.isHostObject()) {
          Object object = arg.asHostObject();

          if (object instanceof Vector3f vec3) {
            return new Vector3f(vec3);
          }
          if (object instanceof Vector3d vec3d) {
            return new Vector3f((float) vec3d.x, (float) vec3d.y, (float) vec3d.z);
          }
          if (object instanceof Vector2f vec2) {
            return new Vector3f(vec2.x, 0, vec2.y);
          }
          if (object instanceof Vector2d vec2) {
            return new Vector3f((float) vec2.x, 0f, (float) vec2.y);
          }
        }

        throw Scripts.typeError("Can't load vector from " + arg);
      }

      default -> {
        return new Vector3f();
      }
    }
  };

  CtorFunction VEC2 = arguments -> {
    int len = Math.min(arguments.length, 2);

    switch (len) {
      case 2 -> {
        float x = arguments[0].asFloat();
        float y = arguments[1].asFloat();
        return new Vector2f(x, y);
      }

      case 1 -> {
        Value arg = arguments[0];

        if (arg.isNumber()) {
          float f = arg.asFloat();
          return new Vector2f(f, f);
        }

        if (arg.isHostObject()) {
          Object object = arg.asHostObject();

          if (object instanceof Vector3f vec3) {
            return new Vector2f(vec3.x, vec3.y);
          }
          if (object instanceof Vector3d vec3d) {
            return new Vector2f((float) vec3d.x, (float) vec3d.y);
          }
          if (object instanceof Vector2f vec2) {
            return new Vector2f(vec2);
          }
          if (object instanceof Vector2d vec2) {
            return new Vector2f((float) vec2.x, (float) vec2.y);
          }
        }

        throw Scripts.typeError("Don't know how to get vec2 from " + arg);
      }

      default -> {
        return new Vector2f();
      }
    }
  };
}
