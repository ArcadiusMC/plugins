package net.arcadiusmc.dungeons

import com.mojang.serialization.Codec
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import net.arcadiusmc.Loggers
import net.arcadiusmc.text.Messages
import net.arcadiusmc.text.Text
import net.arcadiusmc.utils.io.ExistingObjectCodec
import net.arcadiusmc.utils.io.ExtraCodecs
import net.arcadiusmc.utils.io.SerializationHelper
import net.arcadiusmc.utils.io.TagOps
import java.nio.file.Path
import java.util.*

val USER_SETTINGS_CODEC = ExistingObjectCodec.createCodec({UserDungeonSettings()}) { builder ->
  builder.optional("size", ExtraCodecs.enumCodec(LevelSize::class.java))
    .getter { it.size }
    .setter { it, v -> it.size = v }

  builder.optional("difficulty", ExtraCodecs.enumCodec(DungeonDifficulty::class.java))
    .getter { it.difficulty }
    .setter { it, v -> it.difficulty = v }
}

val MAP_CONFIG = Codec.unboundedMap(ExtraCodecs.STRING_UUID, USER_SETTINGS_CODEC)

private val LOGGER = Loggers.getLogger()

class SettingsManager {
  private val playerSettings: MutableMap<UUID, UserDungeonSettings> = Object2ObjectOpenHashMap()
  private val dataFile: Path

  constructor(plugin: DungeonsPlugin) {
    this.dataFile = plugin.dataPath.resolve("user-settings.yml")
  }

  fun save() {
    MAP_CONFIG
      .encodeStart(TagOps.OPS, playerSettings)
      .flatMap(ExtraCodecs.TAG_TO_COMPOUND)
      .mapError { "Failed to encode dungeon user settings: $it" }
      .resultOrPartial { LOGGER.error(it) }
      .ifPresent {
        SerializationHelper.writeTag(dataFile, it)
      }
  }

  fun load() {
    playerSettings.clear()

    SerializationHelper.readTag(dataFile)
      .flatMap { MAP_CONFIG.parse(TagOps.OPS, it) }
      .mapError { "Failed to load dungeon user settings $it" }
      .resultOrPartial { LOGGER.error(it) }
      .ifPresent {
        playerSettings.putAll(it)
      }
  }

  operator fun set(playerId: UUID, settings: UserDungeonSettings) {
    playerSettings[playerId] = settings
  }

  operator  fun get(playerId: UUID): UserDungeonSettings {
    var found = playerSettings[playerId]

    if (found == null) {
      found = UserDungeonSettings()
      playerSettings[playerId] = found
    }

    return found
  }
}

class UserDungeonSettings {

  var size: LevelSize = LevelSize.MEDIUM
  var difficulty: DungeonDifficulty = DungeonDifficulty.NORMAL
}

enum class DungeonDifficulty {
  EXTREME,
  HARD,
  NORMAL,
  EASY,
  ;
}

enum class LevelSize {
  SMALL,
  MEDIUM,
  LARGE,
  ;

  val displayName: String
    get() = Text.plain(Messages.render("dungeons.sizes.${name.lowercase()}").asComponent())
}
