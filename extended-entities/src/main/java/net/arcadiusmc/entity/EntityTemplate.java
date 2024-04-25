package net.arcadiusmc.entity;

import com.badlogic.ashley.core.Entity;
import org.bukkit.Location;

public interface EntityTemplate {

  Entity summon(Location location);
}
