package net.arcadiusmc.sellshop.event;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.event.UserEvent;
import org.bukkit.Material;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Getter @Setter
public class ItemPriceCalculateEvent extends UserEvent {

  @Getter
  static final HandlerList handlerList = new HandlerList();

  private final List<String> tags;
  private final Material material;

  private int earned;
  private int sold;

  public ItemPriceCalculateEvent(User user, List<String> tags, Material material) {
    super(user);

    this.tags = tags;
    this.material = material;
  }

  @Override
  public @NotNull HandlerList getHandlers() {
    return handlerList;
  }
}
