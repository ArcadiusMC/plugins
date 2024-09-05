package net.arcadiusmc.bank;

import com.mojang.serialization.Codec;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import lombok.Getter;
import lombok.Setter;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.core.Coinpile;
import net.arcadiusmc.utils.io.ExistingObjectCodec;
import net.arcadiusmc.utils.io.ExtraCodecs;
import net.arcadiusmc.utils.io.JomlCodecs;
import net.arcadiusmc.utils.math.Bounds3i;
import org.bukkit.Location;
import org.bukkit.World;
import org.joml.Vector3f;
import org.slf4j.Logger;

@Getter @Setter
public class BankVault {

  private static final Logger LOGGER = Loggers.getLogger();

  static final Codec<BankVault> CODEC
      = ExistingObjectCodec.<BankVault>create(builder -> {
        builder.optional("variations", VaultVariationTable.CODEC)
            .getter(BankVault::getTable)
            .setter((v, t) -> v.table.getVariantTable().putAll(t.getVariantTable()));

        builder.optional("chests", ChestGroup.CODEC.listOf())
            .getter(BankVault::getChestGroups)
            .setter((bankVault, chestGroups1) -> bankVault.chestGroups.addAll(chestGroups1));

        builder.optional("coins", JomlCodecs.VEC3F.listOf())
            .getter(BankVault::getCoinPositions)
            .setter((bankVault, vector3fs) -> bankVault.coinPositions.addAll(vector3fs));

        builder.optional("min-coin-value", Codec.INT)
            .getter(BankVault::getMinCoinValue)
            .setter(BankVault::setMinCoinValue);

        builder.optional("max-coin-value", Codec.INT)
            .getter(BankVault::getMaxCoinValue)
            .setter(BankVault::setMaxCoinValue);

        builder.optional("exit-position", FullPosition.CODEC)
            .getter(BankVault::getExitPosition)
            .setter((bankVault, pos) -> bankVault.exitPosition.set(pos));

        builder.optional("enter-position", FullPosition.CODEC)
            .getter(BankVault::getEnterPosition)
            .setter((bankVault, pos) -> bankVault.enterPosition.set(pos));

        builder.optional("run-time", ExtraCodecs.DURATION)
            .defaultsTo(Duration.ofMinutes(1))
            .getter(BankVault::getRunTime)
            .setter(BankVault::setRunTime);

        builder.optional("world-name", ExtraCodecs.KEY_CODEC)
            .defaultsTo("void")
            .getter(BankVault::getWorldName)
            .setter(BankVault::setWorldName);

        builder.optional("ending-duration", ExtraCodecs.DURATION)
            .defaultsTo(Duration.ofSeconds(5))
            .getter(BankVault::getEndingTime)
            .setter(BankVault::setEndingTime);

        builder.optional("vault-room", Bounds3i.CODEC)
            .defaultsTo(Bounds3i.EMPTY)
            .getter(BankVault::getVaultRoom)
            .setter(BankVault::setVaultRoom);
      })
      .codec(Codec.unit(BankVault::new));

  private final VaultVariationTable table = new VaultVariationTable();
  private final List<ChestGroup> chestGroups = new ArrayList<>();

  private final List<Vector3f> coinPositions = new ArrayList<>();
  private int minCoinValue = 10;
  private int maxCoinValue = 1000;

  private final FullPosition exitPosition = new FullPosition();
  private final FullPosition enterPosition = new FullPosition();

  private String worldName;
  private Bounds3i vaultRoom = Bounds3i.EMPTY;

  private Duration runTime = Duration.ofMinutes(1);
  private Duration endingTime = Duration.ofSeconds(5);

  public void place(String variant, World world) {
    Random random = new Random();
    spawnCoins(world, random);
    spawnChests(world, random, variant);
  }

  private void spawnChests(World world, Random random, String variant) {
    for (ChestGroup chestGroup : chestGroups) {
      chestGroup.spawn(world, random, variant, table);
    }
  }

  private void spawnCoins(World world, Random random) {
    int coinMin = Math.min(minCoinValue, maxCoinValue);
    int coinMax = Math.max(minCoinValue, maxCoinValue);

    int coinValueDif = coinMax - coinMin;
    int valueThird = coinValueDif / 3;

    Location location = new Location(world, 0, 0, 0);

    for (Vector3f coinPosition : coinPositions) {
      location.set(coinPosition.x, coinPosition.y, coinPosition.z);

      int value = random.nextInt(coinMin, coinMax + 1);
      int modelId;

      if (value <= valueThird) {
        modelId = Coinpile.MODEL_SMALL;
      } else if (value <= (valueThird * 2)) {
        modelId = Coinpile.MODEL_MEDIUM;
      } else {
        modelId = Coinpile.MODEL_LARGE;
      }

      Coinpile.create(location, roundCoinValue(value), modelId);
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
