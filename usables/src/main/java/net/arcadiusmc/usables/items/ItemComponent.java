package net.arcadiusmc.usables.items;

import java.util.Optional;
import lombok.Getter;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.usables.Interaction;
import net.arcadiusmc.usables.UsableComponent;
import net.arcadiusmc.utils.Result;
import net.arcadiusmc.utils.inventory.ItemList;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang3.function.ToBooleanBiFunction;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

@Getter
public abstract class ItemComponent implements UsableComponent {

  private static final Logger LOGGER = Loggers.getLogger();

  protected final ItemProvider provider;

  public ItemComponent(ItemProvider provider) {
    this.provider = provider;
  }

  protected boolean consumeInteraction(
      Interaction interaction,
      ToBooleanBiFunction<Player, ItemList> function
  ) {
    Optional<Player> playerOpt = interaction.getPlayer();
    if (playerOpt.isEmpty()) {
      return false;
    }

    Player player = playerOpt.get();
    Result<ItemList> listResult = provider.getItems(interaction);

    if (listResult.isError()) {
      LOGGER.error(listResult.getError());
      return false;
    }

    ItemList list = listResult.getValue();
    return function.applyAsBoolean(player, list);
  }

  @Override
  public @Nullable Component displayInfo() {
    return provider.displayInfo();
  }
}
