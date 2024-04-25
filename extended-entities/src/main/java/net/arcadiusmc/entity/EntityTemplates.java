package net.arcadiusmc.entity;

import net.arcadiusmc.entity.dungeons.GuardianBeamTemplate;
import net.arcadiusmc.entity.dungeons.GuardianTemplate;
import net.arcadiusmc.registry.Registries;
import net.arcadiusmc.registry.Registry;

public final class EntityTemplates {
  private EntityTemplates() {}

  public static final Registry<EntityTemplate> TEMPLATES = Registries.newRegistry();

  static void registerAll() {
    register("shulker_guardian", new GuardianTemplate());
    register("shulker_guardian_beam", new GuardianBeamTemplate());
  }

  private static void register(String key, EntityTemplate template) {
    TEMPLATES.register(key, template);
  }
}
