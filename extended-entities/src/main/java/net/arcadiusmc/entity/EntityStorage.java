package net.arcadiusmc.entity;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import java.nio.file.Path;
import java.util.Map.Entry;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.entity.persistence.PersistentType;
import net.arcadiusmc.entity.persistence.PersistentTypes;
import net.arcadiusmc.registry.Holder;
import net.arcadiusmc.utils.io.SerializationHelper;
import net.arcadiusmc.utils.io.TagOps;
import net.forthecrown.nbt.BinaryTag;
import net.forthecrown.nbt.BinaryTags;
import net.forthecrown.nbt.CompoundTag;
import net.forthecrown.nbt.ListTag;
import net.forthecrown.nbt.TagTypes;
import org.slf4j.Logger;

public class EntityStorage {

  private static final Logger LOGGER = Loggers.getLogger();

  private static final String NBT_FLAGS = "flags";
  private static final String NBT_COMPONENTS = "components";
  private static final String NBT_ENTITY_LIST = "entity_list";

  private final Path entitiesFile;

  public EntityStorage(EntityPlugin plugin) {
    this.entitiesFile = plugin.getDataFolder().toPath().resolve("entities.dat");
  }

  public void loadEntities(Engine engine) {
    SerializationHelper.readTagFile(entitiesFile, tag -> {
      ListTag list = tag.getList(NBT_ENTITY_LIST, TagTypes.compoundType());

      for (BinaryTag binaryTag : list) {
        CompoundTag entityTag = binaryTag.asCompound();
        Entity entity = load(entityTag);
        engine.addEntity(entity);
      }
    });
  }

  public void saveEntities(Engine engine) {
    SerializationHelper.writeTagFile(entitiesFile, tag -> {
      ListTag list = BinaryTags.listTag();

      for (Entity entity : engine.getEntities()) {
        CompoundTag entityTag = save(entity);
        list.add(entityTag);
      }

      tag.put(NBT_ENTITY_LIST, list);
    });
  }


  public static CompoundTag save(Entity entity) {
    CompoundTag tag = BinaryTags.compoundTag();

    if (entity.flags != 0) {
      tag.putInt(NBT_FLAGS, entity.flags);
    }

    if (entity.getComponents().size() > 0) {
      CompoundTag componentsTag = BinaryTags.compoundTag();

      for (Component component : entity.getComponents()) {
        Holder<PersistentType<Component>> type
            = PersistentTypes.getType((Class<Component>) component.getClass());

        String key = type.getKey();

        type.getValue().encodeStart(TagOps.OPS, component)
            .mapError(s -> "Failed to save component " + key + ": " + s)
            .resultOrPartial(LOGGER::error)
            .ifPresent(binaryTag -> componentsTag.put(key, binaryTag));
      }

      tag.put(NBT_COMPONENTS, componentsTag);
    }

    return tag;
  }

  public static Entity load(CompoundTag tag) {
    Entity entity = new Entity();
    entity.flags = tag.getInt(NBT_FLAGS, 0);

    CompoundTag components = tag.getCompound(NBT_COMPONENTS);

    for (Entry<String, BinaryTag> entry : components.entrySet()) {
      PersistentTypes.getType(entry.getKey())
          .mapError(s -> "Failed to load component " + entry.getKey() + ": " + s)
          .map(persistentType -> (PersistentType<Component>) persistentType)
          .flatMap(pType -> pType.parse(TagOps.OPS, entry.getValue()))
          .resultOrPartial(LOGGER::error)
          .ifPresent(entity::add);
    }

    return entity;
  }
}
