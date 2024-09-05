package net.arcadiusmc.events;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

@Getter
public class CoinpileCollectEvent extends PlayerEvent implements Cancellable {

  @Getter
  private static final HandlerList handlerList = new HandlerList();

  private final Entity interactionEntity;
  private final Entity coinDisplayEntity;

  @Setter
  private int coinValue;

  @Setter
  private boolean cancelled;

  public CoinpileCollectEvent(
      @NotNull Player who,
      Entity interactionEntity,
      Entity coinDisplayEntity,
      int coinValue
  ) {
    super(who);

    this.interactionEntity = interactionEntity;
    this.coinDisplayEntity = coinDisplayEntity;

    this.coinValue = coinValue;
  }

  @Override
  public @NotNull HandlerList getHandlers() {
    return handlerList;
  }
}
