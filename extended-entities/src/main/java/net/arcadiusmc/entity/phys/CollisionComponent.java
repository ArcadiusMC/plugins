package net.arcadiusmc.entity.phys;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.signals.Signal;
import it.unimi.dsi.fastutil.longs.LongSet;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.block.Block;

@Getter @Setter
public class CollisionComponent implements Component {

  transient final Signal<Collision<Block>> blockCollided
      = new Signal<>();

  transient final Signal<Collision<Entity>> entityCollided
      = new Signal<>();

  transient final Signal<Collision<org.bukkit.entity.Entity>> BukkitEntityCollided
      = new Signal<>();

  transient LongSet sectors;

  Shape shape;
}
