package net.arcadiusmc.bank;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import lombok.Getter;
import lombok.Setter;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.utils.io.ExistingObjectCodec;
import net.arcadiusmc.utils.io.ExtraCodecs;
import net.arcadiusmc.utils.math.Bounds3i;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.slf4j.Logger;

@Getter @Setter
public class BankVault {

  private static final Logger LOGGER = Loggers.getLogger();

  static final Codec<BankVault> CODEC
      = ExistingObjectCodec.<BankVault>create(builder -> {
        builder.optional("variations", VaultVariationTable.CODEC)
            .getter(BankVault::getTable)
            .setter((v, t) -> v.table.getVariantTable().putAll(t.getVariantTable()));

        builder.optional("variation-names", Codec.unboundedMap(Codec.STRING, Codec.STRING))
            .getter(BankVault::getVariationNames)
            .setter((vault, map) -> vault.variationNames.putAll(map));

        builder.optional("chests", ChestGroup.CODEC.listOf())
            .getter(BankVault::getChestGroups)
            .setter((vault, groups) -> vault.chestGroups.addAll(groups));

        builder.optional("coins", CoinGroup.CODEC.listOf())
            .getter(BankVault::getCoinGroups)
            .setter((vault, groups) -> vault.coinGroups.addAll(groups));

        builder.optional("exit-position", FullPosition.CODEC)
            .getter(BankVault::getExitPosition)
            .setter((bankVault, pos) -> bankVault.exitPosition.set(pos));

        builder.optional("enter-position", FullPosition.CODEC)
            .getter(BankVault::getEnterPosition)
            .setter((bankVault, pos) -> bankVault.enterPosition.set(pos));

        builder.optional("menu-enter-position", FullPosition.CODEC)
            .getter(BankVault::getMenuEnterPosition)
            .setter((bankVault, pos) -> bankVault.menuEnterPosition.set(pos));

        builder.optional("menu-exit-position", FullPosition.CODEC)
            .getter(BankVault::getMenuExitPosition)
            .setter((bankVault, pos) -> bankVault.menuExitPosition.set(pos));

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

        builder.optional("inner-vault", InnerVault.CODEC)
            .getter(BankVault::getInnerVault)
            .setter(BankVault::setInnerVault);

        builder.optional("advancement", ExtraCodecs.NAMESPACED_KEY)
            .getter(BankVault::getAdvancementKey)
            .setter(BankVault::setAdvancementKey);

        builder.optional("run-count-objective", Codec.STRING)
            .getter(BankVault::getCounterObjectiveName)
            .setter(BankVault::setCounterObjectiveName);
      })
      .codec(Codec.unit(BankVault::new));

  private final VaultVariationTable table = new VaultVariationTable();
  private final Map<String, String> variationNames = new Object2ObjectOpenHashMap<>();

  private final List<ChestGroup> chestGroups = new ArrayList<>();
  private final List<CoinGroup> coinGroups = new ArrayList<>();

  private final FullPosition exitPosition = new FullPosition();
  private final FullPosition enterPosition = new FullPosition();
  private final FullPosition menuEnterPosition = new FullPosition();
  private final FullPosition menuExitPosition = new FullPosition();

  private NamespacedKey advancementKey;
  private String counterObjectiveName = "";

  private String worldName;
  private Bounds3i vaultRoom = Bounds3i.EMPTY;

  private Duration runTime = Duration.ofMinutes(1);
  private Duration endingTime = Duration.ofSeconds(5);

  private InnerVault innerVault;

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
    for (CoinGroup group : coinGroups) {
      group.spawn(world, random);
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
