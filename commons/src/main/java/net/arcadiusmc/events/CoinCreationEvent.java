package net.arcadiusmc.events;

import lombok.Getter;
import net.arcadiusmc.user.User;
import net.arcadiusmc.utils.inventory.DefaultItemBuilder;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
public class CoinCreationEvent extends Event {

  @Getter
  private static final HandlerList handlerList = new HandlerList();

  private final DefaultItemBuilder builder;
  private final int rhines;

  @Nullable
  private final User withdrawer;

  public CoinCreationEvent(DefaultItemBuilder builder, User withdrawer, int rhines) {
    this.builder = builder;
    this.rhines = rhines;
    this.withdrawer = withdrawer;
  }

  public int totalRhineAmount() {
    return builder.getStack().getAmount() * rhines;
  }

  @Override
  public @NotNull HandlerList getHandlers() {
    return handlerList;
  }
}
