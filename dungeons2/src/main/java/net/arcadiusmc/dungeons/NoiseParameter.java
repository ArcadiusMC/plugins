package net.arcadiusmc.dungeons;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import lombok.Setter;
import net.arcadiusmc.dungeons.gen.NoiseDecorator.NoiseSettingHolder;
import org.bukkit.util.noise.NoiseGenerator;

@Getter @Setter
public class NoiseParameter implements NoiseSettingHolder {

  public static final Codec<NoiseParameter> CODEC = RecordCodecBuilder.create(instance -> {
    return instance
        .group(
            Codec.FLOAT.optionalFieldOf("frequency", 1f)
                .forGetter(NoiseParameter::getFrequency),
            Codec.FLOAT.optionalFieldOf("amplitude", 1f)
                .forGetter(NoiseParameter::getAmplitude),
            Codec.FLOAT.optionalFieldOf("noise-gate", 0.65f)
                .forGetter(NoiseParameter::getNoiseGate),
            Codec.FLOAT.optionalFieldOf("noise-scale", 0.08f)
                .forGetter(NoiseParameter::getNoiseScale),
            Codec.INT.optionalFieldOf("octaves", 1)
                .forGetter(NoiseParameter::getOctaves),
            Codec.STRING.optionalFieldOf("noise-map-name", "")
                .forGetter(NoiseParameter::getNoiseMapName)
        )
        .apply(instance, (f, a, n, s, o, nmn) -> {
          NoiseParameter parameter = new NoiseParameter();
          parameter.setNoiseScale(s);
          parameter.setFrequency(f);
          parameter.setAmplitude(a);
          parameter.setNoiseGate(n);
          parameter.setOctaves(o);
          parameter.setNoiseMapName(nmn);
          return parameter;
        });
  });

  private float frequency = 1f;
  private float amplitude = 1f;
  private float noiseGate = 0.65f;
  private float noiseScale = 0.08f;
  private int octaves = 1;

  private String noiseMapName = "";

  public double sample(NoiseGenerator generator, double x, double y, double z) {
    if (octaves < 1) {
      return 0.0;
    }

    double nx = x * noiseScale;
    double ny = y * noiseScale;
    double nz = z * noiseScale;

    double noise = generator.noise(nx, ny, nz, octaves, frequency, amplitude, true);
    return (noise + 1.0) / 2.0;
  }

  @Override
  public NoiseParameter noiseParameters() {
    return this;
  }
}
