package net.arcadiusmc.scripts.modules;

import com.destroystokyo.paper.ParticleBuilder;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import net.arcadiusmc.command.arguments.Arguments;
import net.arcadiusmc.scripts.IdProxyObject;
import net.arcadiusmc.scripts.Scripts;
import net.kyori.adventure.key.Key;
import org.bukkit.Color;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.Particle.DustTransition;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.joml.Vector2f;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.spongepowered.math.GenericMath;

public class ParticlesObject extends IdProxyObject {

  static final float DEFAULT_INTERVAL = 0.5f;

  static final int ID_spawnParticle = 1;
  static final int ID_spawnParticleLine = 2;
  static final int ID_spawnParticleCircle = 3;
  static final int ID_spawnParticleSphere = 4;
  static final int ID_compileOptions = 5;

  static final int MAX_ID = ID_compileOptions;

  static final String NAME_spawnParticle = "spawnParticle";
  static final String NAME_spawnParticleLine = "spawnParticleLine";
  static final String NAME_spawnParticleCircle = "spawnParticleCircle";
  static final String NAME_spawnParticleSphere = "spawnParticleSphere";
  static final String NAME_compileOptions = "compileOptions";

  public ParticlesObject() {
    initMembers(MAX_ID);
  }

  @Override
  protected String getName(int id) {
    return switch (id) {
      case ID_spawnParticle -> NAME_spawnParticle;
      case ID_spawnParticleLine -> NAME_spawnParticleLine;
      case ID_spawnParticleCircle -> NAME_spawnParticleCircle;
      case ID_spawnParticleSphere -> NAME_spawnParticleSphere;
      case ID_compileOptions -> NAME_compileOptions;
      default -> null;
    };
  }

  @Override
  protected int getId(String name) {
    return switch (name) {
      case NAME_spawnParticle -> ID_spawnParticle;
      case NAME_spawnParticleLine -> ID_spawnParticleLine;
      case NAME_spawnParticleCircle -> ID_spawnParticleCircle;
      case NAME_spawnParticleSphere -> ID_spawnParticleSphere;
      case NAME_compileOptions -> ID_compileOptions;
      default -> UNKNOWN_ID;
    };
  }

  @Override
  public Object invoke(Invocation f, Value... args) {
    switch (f.methodId()) {
      case ID_spawnParticle -> spawnParticle(args);
      case ID_spawnParticleLine -> particleLine(args);
      case ID_spawnParticleCircle -> particleCircle(args);
      case ID_spawnParticleSphere -> particleSphere(args);

      case ID_compileOptions -> {
        if (args.length > 0) {
          return spawn(args[0]);
        } else {
          return new ParticleSpawn();
        }
      }

      default -> throw f.unknown();
    }

    return null;
  }

  private static Color getColorLiteral(Value value) {
    if (value.isNumber()) {
      return Color.fromARGB(value.asInt());
    }

    if (value.isString()) {
      String s = value.asString();

      try {
        return Arguments.BUKKIT_COLOR.parse(new StringReader(s));
      } catch (CommandSyntaxException exc) {
        throw Scripts.typeError("Invalid color: '%s': %s", s, exc.getRawMessage().getString());
      }
    }

    if (value.isHostObject() && value.asHostObject() instanceof Vector3f vec3) {
      return Color.fromRGB(
          (int) (255.0f * vec3.x),
          (int) (255.0f * vec3.y),
          (int) (255.0f * vec3.z)
      );
    }

    throw Scripts.cantLoad("Color", value);
  }

  private static ColorSupplier getColor(Value value) {
    if (value.isNull()) {
      return null;
    }

    if (value.canExecute()) {
      return new CallableColor(value);
    }

    if (value.hasArrayElements()) {
      int len = (int) value.getArraySize();
      Color[] colors = new Color[len];

      for (int i = 0; i < colors.length; i++) {
        colors[i] = getColorLiteral(value.getArrayElement(i));
      }

      return new LerpColor(colors);
    }

    Color literal = getColorLiteral(value);
    return new ConstColor(literal);
  }

  private static Particle getParticle(Value value) {
    if (value.isHostObject() && value.asHostObject() instanceof Particle particle) {
      return particle;
    }

    NamespacedKey particleKey;

    if (Scripts.isInstance(value, NamespacedKey.class)) {
      particleKey = value.asHostObject();
    } else if (Scripts.isInstance(value, Key.class)) {
      Key key = value.asHostObject();
      particleKey = new NamespacedKey(key.namespace(), key.value());
    } else if (value.isString()) {
      String str = value.asString();
      particleKey = NamespacedKey.fromString(value.asString().toLowerCase());

      if (particleKey == null) {
        throw Scripts.typeError("Unknown particle: '" + str + "'");
      }
    } else {
      throw Scripts.cantLoad("Particle", value);
    }

    Particle particle = Registry.PARTICLE_TYPE.get(particleKey);

    if (particle == null) {
      throw Scripts.typeError("Unknown particle: " + particleKey);
    }

    return particle;
  }

  public static ParticleSpawn spawn(Value value) {
    if (value.isHostObject() && value.asHostObject() instanceof ParticleSpawn spawn) {
      return spawn;
    }

    if (!value.hasMembers()) {
      ParticleSpawn spawn = new ParticleSpawn();
      spawn.setParticle(getParticle(value));
      return spawn;
    }

    if (!value.hasMember("particleName")) {
      throw Scripts.typeError("No 'particleName' set");
    }

    ParticleSpawn spawn = new ParticleSpawn();

    for (String memberKey : value.getMemberKeys()) {
      if (!ParticleSpawn.KEYS.contains(memberKey)) {
        continue;
      }

      Value member = value.getMember(memberKey);
      spawn.putMember(memberKey, member);
    }

    return spawn;
  }

  private static Vector3f toRadius3(Value value) {
    if (value.isNumber()) {
      return new Vector3f(value.asFloat());
    }
    return Scripts.toVec3f(value);
  }

  private static Vector2f toRadius2(Value value) {
    if (value.isNumber()) {
      return new Vector2f(value.asFloat());
    }
    return Scripts.toVec2f(value);
  }

  private static float interval(Value[] args, int index) {
    if (args.length <= index) {
      return DEFAULT_INTERVAL;
    }

    return args[index].asFloat();
  }

  private static void spawnParticle(Value[] args) {
    Scripts.ensureParameterCount(args, 3);

    ParticleSpawn spawn = spawn(args[0]);
    World world = Scripts.toWorld(args[1]);
    Vector3f pos = Scripts.toVec3f(args[2]);

    var builder = spawn.createBuilder();
    spawn.applyBuilder(builder, 0f);

    builder
        .location(world, pos.x, pos.y, pos.z)
        .allPlayers()
        .spawn();
  }


  private static void particleLine(Value[] args) {
    Scripts.ensureParameterCount(args, 4);

    ParticleSpawn spawn = spawn(args[0]);
    World world = Scripts.toWorld(args[1]);
    Vector3f start = Scripts.toVec3f(args[2]);
    Vector3f end = Scripts.toVec3f(args[3]);
    float interval = interval(args, 4);

    Vector3f dif = new Vector3f();
    Vector3f point = new Vector3f(start);

    end.sub(start, dif);

    float len = dif.length();
    float points = len / interval;

    dif.div(points);

    ParticleBuilder builder = spawn.createBuilder();

    for (double i = 0; i < points; i++) {
      point.add(dif);

      double prog = i / points;

      spawn.applyBuilder(builder, prog);

      builder.location(world, point.x, point.y, point.z)
          .allPlayers()
          .spawn();
    }
  }

  private static void particleCircle(Value[] args) {
    Scripts.ensureParameterCount(args, 4);

    ParticleSpawn spawn = spawn(args[0]);
    World world = Scripts.toWorld(args[1]);
    Vector3f center = Scripts.toVec3f(args[2]);
    Vector2f radius = toRadius2(args[3]);
    float interval = interval(args, 4);

    double circumference = ellipseCircumference(radius.x, radius.y);
    double pointCount = circumference / interval;

    Vector3d pos = new Vector3d();
    pos.y = center.y;

    var builder = spawn.createBuilder();

    for (double i = 0; i < pointCount; i++) {
      double prog = i / pointCount;
      double angle = 2 * Math.PI * prog;

      pos.x = center.x + radius.x * Math.cos(angle);
      pos.z = center.z + radius.y * Math.sin(angle);

      spawn.applyBuilder(builder, prog);

      builder
          .location(world, pos.x, pos.y, pos.z)
          .allPlayers()
          .spawn();
    }
  }

  private static double ellipseCircumference(double a, double b) {
    return Math.PI * Math.sqrt(2 * (a * a + b * b) - Math.pow(a - b, 2));
  }

  private static void particleSphere(Value[] args) {
    Scripts.ensureParameterCount(args, 4);

    ParticleSpawn spawn = spawn(args[0]);
    World world = Scripts.toWorld(args[1]);
    Vector3f center = Scripts.toVec3f(args[2]);
    Vector3f radius = toRadius3(args[3]);
    double interval = interval(args, 4);

    double numPointsTheta = 2 * Math.PI * radius.x / interval;
    double numPointsPhi = Math.PI * radius.y / interval;

    var builder = spawn.createBuilder();

    for (double i = 0; i < numPointsTheta; i++) {
      double theta = i * (2 * Math.PI) / numPointsTheta;

      for (double j = 0; j < numPointsPhi; j++) {
        double phi = j * Math.PI / numPointsPhi;

        double x = center.x + radius.x * Math.sin(phi) * Math.cos(theta);
        double y = center.y + radius.y * Math.sin(phi) * Math.sin(theta);
        double z = center.z + radius.z * Math.cos(phi);

        double completion = (i * numPointsPhi + j) / (numPointsTheta * numPointsPhi);

        spawn.applyBuilder(builder, completion);
        builder.location(world, x, y, z)
            .allPlayers()
            .spawn();
      }
    }
  }

  @Getter
  @Setter
  public static final class ParticleSpawn implements ProxyObject {

    static final List<String> KEYS = List.of(
        "particleName",
        "offset",
        "count",
        "extra", "speed",
        "force",
        "size",
        "color",
        "colorTransition",
        "item",
        "block",
        "sculkCharge",
        "shriek"
    );

    private Particle particle;
    private Vector3f offset;
    private int count = 0;
    private double extra = 0;
    private boolean force = false;
    private Float size;
    private ColorSupplier color;
    private ColorSupplier colorTransition;
    private ItemStack item;
    private BlockData blockData;
    private Float sculkCharge;
    private Integer shriek;

    @Override
    public Object getMember(String key) {
      return switch (key) {
        case "particleName" -> {
          if (particle == null) {
            yield null;
          }

          yield particle.key().toString();
        }

        case "offset" -> offset;
        case "count" -> count;
        case "extra", "speed" -> extra;
        case "force" -> force;
        case "size" -> size;
        case "color" -> color;
        case "colorTransition" -> colorTransition;
        case "item" -> item;
        case "block" -> blockData;
        case "sculkCharge" -> sculkCharge;
        case "shriek" -> shriek;

        default -> null;
      };
    }

    @Override
    public Object getMemberKeys() {
      return new ArrayList<>(KEYS);
    }

    @Override
    public boolean hasMember(String key) {
      return KEYS.contains(key);
    }

    @Override
    public void putMember(String key, Value value) {
      switch (key) {
        case "particleName" -> {
          this.particle = value.isNull() ? null : ParticlesObject.getParticle(value);
        }

        case "offset" -> {
          this.offset = value.isNull() ? null : Scripts.toVec3f(value);
        }

        case "count" -> this.count = value.asInt();
        case "extra", "speed" -> this.extra = value.asDouble();
        case "force" -> this.force = value.asBoolean();

        case "color" -> this.color = ParticlesObject.getColor(value);
        case "colorTransition" -> this.colorTransition = ParticlesObject.getColor(value);

        case "size" -> {
          this.size = value.isNull() ? null : value.asFloat();
        }

        case "item" -> {
          this.item = value.isNull() ? null : Scripts.toItemStack(value);
        }

        case "block" -> {
          this.blockData = value.isNull() ? null : Scripts.toBlockData(value);
        }

        case "sculkCharge" -> {
          this.sculkCharge = value.isNull() ? null : value.asFloat();
        }

        case "shriek" -> {
          this.shriek = value.isNull() ? null : value.asInt();
        }

        default -> {
          throw Scripts.typeError("Member '%s' not supported here", key);
        }
      }
    }

    ParticleBuilder createBuilder() {
      ParticleBuilder builder = particle.builder();
      builder.force(force);

      if (offset != null) {
        builder.offset(offset.x, offset.y, offset.z);
      }
      if (count > 0) {
        builder.count(count);
      }

      builder.extra(extra);

      if (item != null) {
        builder.data(item.clone());
      } else if (blockData != null) {
        builder.data(blockData.clone());
      } else if (shriek != null) {
        builder.data(shriek);
      } else if (sculkCharge != null) {
        builder.data(sculkCharge);
      }

      return builder;
    }

    void applyBuilder(ParticleBuilder builder, double completion) {
      completion = GenericMath.clamp(completion, 0.0, 1.0);

      if (color != null) {
        float size = Objects.requireNonNullElse(this.size, 1.0f);
        Color color = this.color.getColor(completion);

        if (colorTransition != null) {
          Color colorTransition = this.colorTransition.getColor(completion);
          builder.data(new DustTransition(color, colorTransition, size));
        } else {
          builder.data(new DustOptions(color, size));
        }
      }
    }
  }


  public interface ColorSupplier {
    Color getColor(double completion);
  }

  record LerpColor(Color[] colors) implements ColorSupplier {

    @Override
    public Color getColor(double completion) {
      if (colors.length == 1) {
        return colors[0];
      }
      if (colors.length == 2) {
        return lerp2(colors[0], colors[1], completion);
      }

      final int maxIndex = colors.length - 1;

      int firstIndex = (int) (completion * maxIndex);
      double firstStep = (double) firstIndex / maxIndex;
      double localStep = (completion - firstStep) * maxIndex;

      Color c1 = colors[firstIndex];
      Color c2 = colors[firstIndex + 1];

      return lerp2(c1, c2, localStep);
    }

    Color lerp2(Color c1, Color c2, double p) {
      return Color.fromARGB(
          (int) (c1.getAlpha() + p * (c2.getAlpha() - c1.getAlpha())),
          (int) (c1.getRed()   + p * (c2.getRed()   - c1.getRed())),
          (int) (c1.getGreen() + p * (c2.getGreen() - c1.getGreen())),
          (int) (c1.getBlue()  + p * (c2.getBlue()  - c1.getBlue()))
      );
    }
  }

  record ConstColor(Color color) implements ColorSupplier {
    @Override
    public Color getColor(double completion) {
      return color;
    }
  }

  record CallableColor(Value callable) implements ColorSupplier {

    @Override
    public Color getColor(double completion) {
      Value result = callable.execute(completion);
      return getColorLiteral(result);
    }
  }
}
