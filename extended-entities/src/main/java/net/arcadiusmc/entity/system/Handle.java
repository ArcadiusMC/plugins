package net.arcadiusmc.entity.system;

import com.badlogic.ashley.core.Component;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Entity;

@Getter @Setter
public class Handle implements Component {

  private UUID minecraftId;
  private transient Entity entity;

  public <T extends Entity> T getAs(Class<T> type) {
    return type.isInstance(entity) ? type.cast(entity) : null;
  }
}
