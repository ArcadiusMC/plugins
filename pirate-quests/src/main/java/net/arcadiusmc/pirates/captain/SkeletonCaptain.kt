package net.arcadiusmc.pirates.captain

import net.arcadiusmc.McConstants
import net.arcadiusmc.pirates.PirateSkeletonType
import net.arcadiusmc.pirates.getRandomSkeletonType
import net.arcadiusmc.pirates.riseFromTheGrave
import net.arcadiusmc.pirates.spawnSkeletonTypeAt
import net.arcadiusmc.user.Users
import net.arcadiusmc.utils.Tasks
import net.arcadiusmc.utils.math.WorldBounds3i
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.empty
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.*
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.attribute.AttributeModifier.Operation
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarFlag
import org.bukkit.boss.BarStyle
import org.bukkit.boss.BossBar
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.AbstractSkeleton
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Skeleton
import org.bukkit.entity.WitherSkeleton
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ArmorMeta
import org.bukkit.inventory.meta.trim.ArmorTrim
import org.bukkit.inventory.meta.trim.TrimMaterial
import org.bukkit.inventory.meta.trim.TrimPattern
import java.util.*
import kotlin.math.max

const val MINION_SPAWN_COUNT = 3
const val CAPTAIN_TAG = "blunderbeard.skeleton_captain"
const val DEATH_PLAYER_FLAG = "quest.blunderbeard.note-found"

private val CAPTAIN_TYPE = PirateSkeletonType().apply {
  name = text("Captain", NamedTextColor.GOLD)
  entityType = WitherSkeleton::class.java

  offhandDropChance = 2f

  attackDamage = 35f
  followRange = 40f
  moveSpeed = 0.32f
  health = 60f

  offhand = listOf(
    ItemStack(Material.PAPER).apply {
      editMeta { meta ->
        meta.itemName(text("Note from Blunder Beard", NamedTextColor.GOLD))

        var lore = ArrayList<Component>()
        lore.add(text("Dear me successor,"))
        lore.add(empty())
        lore.add(text("If this note be getting to ye, then I must be at Davy Jones' locker!"))
        lore.add(text("I be sending this message to let ye know of the secret location"))
        lore.add(text("of the key to my most prized possession... Do with this what I would"))
        lore.add(text("have and rule over the high seas forever!"))
        lore.add(empty())
        lore.add(text("The location of the Key is:"))
        lore.add(text("INSERT KEY LOCATION HERE :)"))
        lore.add(empty())
        lore.add(text("Signed,"))
        lore.add(text("Blunder Beard"))

        lore = lore.mapTo(ArrayList(lore.size)) {
          if (it == empty()) {
            return@mapTo empty()
          }

          return@mapTo text()
            .append(it)
            .decoration(TextDecoration.ITALIC, false)
            .color(NamedTextColor.WHITE)
            .build()
        }

        meta.lore(lore)
      }
    }
  )

  mainhand = listOf(
    ItemStack(Material.GOLDEN_SWORD).apply {
      editMeta {
        it.addEnchant(Enchantment.SHARPNESS, 5, true)
        it.addEnchant(Enchantment.SWEEPING_EDGE, 3, true)
        it.isUnbreakable = true

        val attackMod = AttributeModifier(
          NamespacedKey("arcadiusmc", "attack_boost"),
          8.0,
          Operation.ADD_NUMBER
        )
        val attackSpeedMod = AttributeModifier(
          NamespacedKey("arcadiusmc", "attack_speed_boost"),
          2.0,
          Operation.ADD_NUMBER
        )

        it.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, attackMod)
        it.addAttributeModifier(Attribute.GENERIC_ATTACK_SPEED, attackSpeedMod)
      }
    }
  )

  helmet = listOf(
    ItemStack(Material.BAMBOO_BUTTON).apply {
      editMeta {
        it.setCustomModelData(10080002)

        val modifier = AttributeModifier(
          NamespacedKey.fromString("arcadiusmc:armor_boost")!!
          , 2.0,
          Operation.ADD_NUMBER
        )

        it.addAttributeModifier(Attribute.GENERIC_ARMOR, modifier)
      }
    }
  )

  chestplate = listOf(
    ItemStack(Material.IRON_CHESTPLATE).apply {
      editMeta(ArmorMeta::class.java) {
        it.trim = ArmorTrim(TrimMaterial.NETHERITE, TrimPattern.SNOUT)
      }
    }
  )

  leggings = listOf(
    ItemStack(Material.DIAMOND_LEGGINGS).apply {
      editMeta(ArmorMeta::class.java) {
        it.trim = ArmorTrim(TrimMaterial.IRON, TrimPattern.SILENCE)
      }
    }
  )

  boots = listOf(
    ItemStack(Material.GOLDEN_BOOTS).apply {
      editMeta(ArmorMeta::class.java) {
        it.trim = ArmorTrim(TrimMaterial.IRON, TrimPattern.SPIRE)
      }
    }
  )
}

private val currentFight = CaptainFight()

// Invoked by Usable script_file action that's triggered by the
// area trigger around the battle area.
fun trySpawnSkeletonCaptain(boundingBox: WorldBounds3i) {
  if (currentFight.state != CaptainState.DEAD) {
    return
  }

  if (currentFight.worldBorder == null) {
    currentFight.worldBorder = Bukkit.createWorldBorder()
  }

  val expanded = boundingBox.expand(20)
  val world = expanded.world
  val players = expanded.players

  currentFight.players.clear()

  currentFight.worldBorder!!.center = Location(
    world,
    expanded.centerX(),
    expanded.centerY(),
    expanded.centerZ()
  )
  currentFight.worldBorder!!.size = max(expanded.sizeX(), expanded.sizeZ()).toDouble()

  val bar = currentFight.bossbar
  bar.progress = 1.0
  bar.isVisible = true

  for (player in players) {
    bar.addPlayer(player)
    player.worldBorder = currentFight.worldBorder!!
    currentFight.players.addLast(player.uniqueId)
  }

  val skeletonMinions = MINION_SPAWN_COUNT + (players.size - 1)
  val spawnArea = boundingBox.expand(5)

  // Spawn minions
  for (i in 0 until skeletonMinions) {
    val location = findSpawnLocation(spawnArea)
    val type = getRandomSkeletonType(currentFight.random)
    spawnWithTypeAt(type, location)
  }

  // Spawn boss
  val boss = spawnWithTypeAt(CAPTAIN_TYPE, findSpawnLocation(spawnArea))
  boss.addScoreboardTag(CAPTAIN_TAG)
  boss.isGlowing = true

  currentFight.bossEntity = boss
  currentFight.state = CaptainState.ALIVE
}

fun findSpawnLocation(area: WorldBounds3i): Location {
  val world = area.world

  val center = area.center()
  val radiusX = (area.sizeX() / 2).toDouble()
  val radiusZ = (area.sizeZ() / 2).toDouble()
  val maxY = 80
  val minY = 50

  var x: Double = center.x()
  var y: Double = center.y()
  var z: Double = center.z()

  var attempts = 0
  val maxAttempts = 1024

  while (attempts < maxAttempts) {
    attempts++

    x = currentFight.random.nextDouble(-radiusX, radiusX) + center.x()
    z = currentFight.random.nextDouble(-radiusZ, radiusZ) + center.z()

    y = findHighestYAt(world, x.toInt(), z.toInt())

    if (y > maxY || y < minY) {
      continue
    } else {
      break
    }
  }

  val yaw = currentFight.random.nextFloat(-180f, 180f)
  val pitch = currentFight.random.nextFloat(-45f, 45f)

  return Location(world, x, y - 2.0, z, yaw, pitch)
}

private fun findHighestYAt(world: World, x: Int, z: Int): Double {
  val startY = 70
  val block = world.getBlockAt(x, startY, z)

  val dir = if (block.isPassable) -1 else 1
  val targetIsPassable = !block.isPassable

  var y = startY

  while (y > McConstants.MIN_Y && y < McConstants.MAX_Y) {
    val nextY = y + dir
    val nextBlock = world.getBlockAt(x, nextY, z)

    if (nextBlock.isPassable == targetIsPassable) {
      if (targetIsPassable) {
        return y.toDouble()
      }

      return y.toDouble() + 1.0
    }

    y = nextY
  }

  return y.toDouble()
}

private fun spawnWithTypeAt(type: PirateSkeletonType, location: Location): AbstractSkeleton {
  val spawned = spawnSkeletonTypeAt(type, location, currentFight.random)

  spawned.removeWhenFarAway = false
  spawned.isPersistent = true
  spawned.setShouldBurnInDay(false)

  currentFight.skeletons.add(spawned)

  riseFromTheGrave(spawned)
  return spawned
}

class CaptainListener: Listener {

  @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
  fun onEntityDeath(event: EntityDeathEvent) {
    val entity = event.entity

    currentFight.skeletons.removeIf { it.uniqueId == entity.uniqueId }

    val minionCount = getMinionCount()
    if (minionCount < 1 && currentFight.bossEntity != null && currentFight.bossEntity!!.isGlowing) {
      currentFight.bossEntity?.isGlowing = false

      entity.world.playSound(
        Sound.sound()
          .type(org.bukkit.Sound.BLOCK_CONDUIT_DEACTIVATE)
          .build(),

        entity.x,
        entity.y,
        entity.z,
      )
    }

    if (entity != currentFight.bossEntity) {
      return
    }

    val bar = currentFight.bossbar

    bar.isVisible = false
    bar.removeAll()
    bar.progress = 1.0

    val userService = Users.getService()
    val userFlags = userService.flags

    for (playerId in currentFight.players) {
      userFlags.setFlag(playerId, DEATH_PLAYER_FLAG)

      val player = Bukkit.getPlayer(playerId) ?: continue

      if (!player.isOnline) {
        continue
      }

      player.worldBorder = null
    }

    currentFight.state = CaptainState.DEAD
    currentFight.players.clear()
    currentFight.bossEntity = null
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  fun onCaptainDamage(event: EntityDamageEvent) {
    val entity = event.entity

    if (!entity.scoreboardTags.contains(CAPTAIN_TAG)) {
      return
    }

    entity as Skeleton

    val minionCount = getMinionCount()
    if (minionCount <= 0) {
      return
    }

    event.isCancelled = true

    Particle.SQUID_INK.builder().apply {
      location(entity.eyeLocation)
      count(5)
      offset(0.1, 0.1, 0.1)
      extra(0.05)
      spawn()
    }

    entity.world.playSound(
      Sound.sound()
        .type(org.bukkit.Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR)
        .volume(0.3f)
        .pitch(1.2f)
        .build(),
      entity.x,
      entity.y,
      entity.z,
    )
  }

  private fun getMinionCount(): Int {
    return currentFight.skeletons.count { !it.isDead && !it.scoreboardTags.contains(CAPTAIN_TAG) }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  fun onEntityDamage(event: EntityDamageEvent) {
    val entity = event.entity

    if (entity != currentFight.bossEntity) {
      return
    }

    entity as LivingEntity

    val damage = event.finalDamage
    val currentHealth = entity.health
    val after = max(currentHealth - damage, 0.0)

    val maxHealth = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH)!!.value
    val nprog = Math.clamp(after / maxHealth, 0.0, 1.0)

    currentFight.bossbar.progress = nprog
  }

  @EventHandler
  fun onPlayerJoin(event: PlayerJoinEvent) {
    val player = event.player

    if (!currentFight.players.contains(player.uniqueId)) {
      return
    }
    if (currentFight.state != CaptainState.ALIVE) {
      return
    }

    val bar = currentFight.bossbar
    bar.addPlayer(player)

    Tasks.runLater({
      if (currentFight.state != CaptainState.ALIVE) {
        return@runLater
      }

      player.worldBorder = currentFight.worldBorder
    }, 5)
  }
}

enum class CaptainState {
  DEAD,
  ALIVE,
  ;
}

class CaptainFight {
  val players: MutableList<UUID> = ArrayList()
  val skeletons: MutableList<AbstractSkeleton> = ArrayList()
  val random: Random = Random()

  var state: CaptainState = CaptainState.DEAD
  var bossEntity: AbstractSkeleton? = null
  var worldBorder: WorldBorder? = null

  private var internalBossBar: BossBar? = null

  val bossbar: BossBar
    get() {
      if (internalBossBar == null) {
        internalBossBar = Bukkit.createBossBar(
          "Captain",
          BarColor.RED,
          BarStyle.SOLID,
          BarFlag.CREATE_FOG, BarFlag.DARKEN_SKY
        )
      }

      return internalBossBar!!
    }
}