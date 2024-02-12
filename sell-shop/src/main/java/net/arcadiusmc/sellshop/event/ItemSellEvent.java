package net.arcadiusmc.sellshop.event;

import lombok.Getter;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.event.UserEvent;
import org.bukkit.Material;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Getter
public class ItemSellEvent extends UserEvent {

  @Getter
  private static final HandlerList handlerList = new HandlerList();

  private final int sold;
  private final int earned;

  private final Material material;

  public ItemSellEvent(
      User user,
      int sold,
      int earned,
      Material material
  ) {
    super(user);
    this.sold = sold;
    this.earned = earned;
    this.material = material;
  }

  @Override
  public @NotNull HandlerList getHandlers() {
    return handlerList;
  }
}
