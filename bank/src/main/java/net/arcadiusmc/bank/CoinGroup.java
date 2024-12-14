package net.arcadiusmc.bank;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import lombok.Getter;
import lombok.Setter;
import net.arcadiusmc.ArcadiusServer;
import net.arcadiusmc.ArcadiusServer.CoinPileSize;
import net.arcadiusmc.utils.io.ExistingObjectCodec;
import net.arcadiusmc.utils.io.JomlCodecs;
import org.bukkit.Location;
import org.bukkit.World;
import org.joml.Vector3f;

@Getter @Setter
public class CoinGroup {

  static final Codec<CoinGroup> CODEC = ExistingObjectCodec.createCodec(CoinGroup::new, builder -> {
    builder.field("positions", JomlCodecs.VEC3F.listOf())
        .getter(CoinGroup::getPositions)
        .setter(CoinGroup::setPositions);

    builder.field("min-value", Codec.INT)
        .getter(CoinGroup::getMinValue)
        .setter(CoinGroup::setMinValue);

    builder.field("max-value", Codec.INT)
        .getter(CoinGroup::getMaxValue)
        .setter(CoinGroup::setMaxValue);

    builder.optional("max-spawned-value", Codec.INT)
        .defaultsTo(Integer.MAX_VALUE)
        .getter(CoinGroup::getMaxSpawnValue)
        .setter(CoinGroup::setMaxSpawnValue);

    builder.optional("max-spawned-coins", Codec.INT)
        .defaultsTo(Integer.MAX_VALUE)
        .getter(CoinGroup::getMaxSpawnCount)
        .setter(CoinGroup::setMaxSpawnCount);
  });

  private List<Vector3f> positions = ObjectLists.emptyList();
  private int minValue = 10;
  private int maxValue = 100;
  private int maxSpawnValue = Integer.MAX_VALUE;
  private int maxSpawnCount = Integer.MAX_VALUE;

  public void spawn(World world, Random random) {
    if (positions.isEmpty()) {
      return;
    }

    int coinMin = Math.min(minValue, maxValue);
    int coinMax = Math.max(minValue, maxValue);

    int coinValueDif = coinMax - coinMin;
    int valueThird = coinValueDif / 3;

    Location location = new Location(world, 0, 0, 0);
    ArcadiusServer server = ArcadiusServer.server();

    int spawnedCount = 0;
    int spawnedValue = 0;

    List<Vector3f> positions = new ArrayList<>(this.positions);
    Collections.shuffle(positions, random);

    for (Vector3f coinPosition : positions) {
      location.set(coinPosition.x, coinPosition.y, coinPosition.z);

      int value = random.nextInt(coinMin, coinMax + 1);
      CoinPileSize size;

      if (value <= valueThird) {
        size = CoinPileSize.SMALL;
      } else if (value <= (valueThird * 2)) {
        size = CoinPileSize.MEDIUM;
      } else {
        size = CoinPileSize.LARGE;
      }

      int rounded = roundCoinValue(value);
      spawnedValue += rounded;
      spawnedCount++;

      server.spawnCoinPile(location, rounded, size);

      if (spawnedValue > maxSpawnValue || spawnedCount > maxSpawnCount) {
        break;
      }
    }
  }

  static int roundCoinValue(int value) {
    if (value <= 10) {
      return value;
    }
    if (value < 100) {
      return value - (value % 10);
    }
    if (value < 1000) {
      return value - (value % 100);
    }
    return value - (value % 1000);
  }
}
