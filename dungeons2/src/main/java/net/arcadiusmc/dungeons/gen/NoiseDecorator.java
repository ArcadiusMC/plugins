package net.arcadiusmc.dungeons.gen;

import com.google.common.base.Strings;
import net.arcadiusmc.dungeons.NoiseParameter;
import net.arcadiusmc.dungeons.gen.NoiseDecorator.NoiseSettingHolder;
import org.bukkit.util.noise.NoiseGenerator;

public abstract class NoiseDecorator<C extends NoiseSettingHolder> extends Decorator<C> {

  protected NoiseGenerator noisegen;

  public NoiseDecorator(C config) {
    super(config);
  }

  @Override
  protected void onBind() {
    String name = config.noiseParameters().getNoiseMapName();
    if (Strings.isNullOrEmpty(name)) {
      name = getClass().getName();
    }

    noisegen = generator.getNoiseMap(name);
  }

  protected double getNoise(double x, double y, double z) {
    NoiseParameter param = config.noiseParameters();
    return param.sample(noisegen, x, y, z);
  }

  protected boolean testNoise(double noise) {
    return config.noiseParameters().getNoiseGate() <= noise;
  }

  protected boolean testNoise(double x, double y, double z) {
    return testNoise(getNoise(x, y, z));
  }

  public interface NoiseSettingHolder {
    NoiseParameter noiseParameters();
  }
}
