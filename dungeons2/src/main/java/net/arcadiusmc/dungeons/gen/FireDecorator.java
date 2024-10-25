package net.arcadiusmc.dungeons.gen;

import static org.bukkit.Material.FIRE;
import static org.bukkit.Material.SOUL_FIRE;
import static org.bukkit.Material.SOUL_SAND;
import static org.bukkit.Material.SOUL_SOIL;

import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Codec;
import java.util.List;
import net.arcadiusmc.dungeons.LevelFunctions;
import net.forthecrown.nbt.CompoundTag;
import org.bukkit.Material;
import org.spongepowered.math.vector.Vector3i;

public class FireDecorator extends Decorator<Unit> {

  static final Material[] SOUL_FIRE_GENERATES_ON = {
      SOUL_SAND,
      SOUL_SOIL,
  };

  public static final DecoratorType<FireDecorator, Unit> TYPE
      = DecoratorType.create(Codec.unit(Unit.INSTANCE), FireDecorator::new);

  public FireDecorator(Unit config) {
    super(config);
  }

  private boolean usesSoulFire(int x, int y, int z, GeneratorFunction func) {
    CompoundTag data = func.getData();
    if (data.contains("soul")) {
      return data.getBoolean("soul");
    }

    Material below = getBlockType(x, y - 1, z);

    for (Material material : SOUL_FIRE_GENERATES_ON) {
      if (material == below) {
        return true;
      }
    }

    return false;
  }

  @Override
  public void execute() {
    List<GeneratorFunction> list = getFunctions(LevelFunctions.FIRE);

    for (GeneratorFunction generatorFunction : list) {
      float chance = generatorFunction.getData().getFloat("chance", 1f);

      if (!randomBool(chance)) {
        continue;
      }

      Vector3i pos = generatorFunction.getPosition();

      Material material = usesSoulFire(pos.x(), pos.y(), pos.z(), generatorFunction)
          ? SOUL_FIRE
          : FIRE;

      setBlock(pos.x(), pos.y(), pos.z(), material.createBlockData());
    }
  }
}
