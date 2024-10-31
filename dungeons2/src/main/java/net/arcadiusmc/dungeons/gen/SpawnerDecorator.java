package net.arcadiusmc.dungeons.gen;

import java.util.List;
import net.arcadiusmc.dungeons.DungeonPiece;
import net.arcadiusmc.dungeons.LevelFunctions;

public class SpawnerDecorator extends Decorator<SpawnerConfig> {

  public static final DecoratorType<SpawnerDecorator, SpawnerConfig> TYPE
      = DecoratorType.create(SpawnerConfig.CODEC, SpawnerDecorator::new);

  public SpawnerDecorator(SpawnerConfig config) {
    super(config);
  }

  @Override
  public void execute() {
    getRootPiece().forEachDescendant(this::processPiece);
  }

  private void processPiece(DungeonPiece piece) {
    List<GeneratorFunction> trialSpawners = getFunctionsIn(LevelFunctions.TRIAL_SPAWNER, piece);
    List<GeneratorFunction> vaults = getFunctionsIn(LevelFunctions.VAULT, piece);
    List<GeneratorFunction> regularSpawners = getFunctionsIn(LevelFunctions.SPAWNER, piece);

    
  }

}
