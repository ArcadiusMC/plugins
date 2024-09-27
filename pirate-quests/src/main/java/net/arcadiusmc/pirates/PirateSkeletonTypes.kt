package net.arcadiusmc.pirates

import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import net.arcadiusmc.utils.WeightedList
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.*
import org.bukkit.attribute.Attributable
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Skeleton
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.LeatherArmorMeta
import org.bukkit.loot.LootTables
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scoreboard.Team
import java.util.*
import java.util.function.Consumer

class PirateSkeletonType {
  var name: String? = null

  var health: Float? = null
  var attackDamage: Float? = null
  var followRange: Float? = null
  var moveSpeed: Float? = null

  var helmet: List<ItemStack?>? = null
  var chestplate: List<ItemStack?>? = null
  var leggings: List<ItemStack?>? = null
  var boots: List<ItemStack?>? = null

  var mainhand: List<ItemStack?>? = null
  var offhand: List<ItemStack?>? = null

  var mainhandDropChance: Float = 0f
  var offhandDropChance: Float = 0f;
}

private const val ARMOR_MOD_KEY = "arcadiusmc:armor_boost"
private const val ARMOR_TOUGHNESS_MOD_KEY = "arcadiusmc:armor_toughness_boost"
private const val TEAM_NAME = "pirate_skeletons"

const val LEVITATION_POTION_AMPLIFIER = 2
const val LEVITATION_POTION_DURATION = 1 * 20 // 2 seconds

val SKELETON_TYPES: WeightedList<PirateSkeletonType> = createTypes()

private fun createTypes(): WeightedList<PirateSkeletonType> {
  val list = WeightedList<PirateSkeletonType>()

  val crew = PirateSkeletonType().apply {
    name = "Skeleton Crew"

    health = 40f
    attackDamage = 20f
    followRange = 40f
    moveSpeed = 0.32f

    helmet = listOf(
      createHelmet(10080001),
      createHelmet(10080002)
    )

    chestplate = listOf(
      ItemStack(Material.DIAMOND_CHESTPLATE),
      ItemStack(Material.IRON_CHESTPLATE),

      ItemStack(Material.DIAMOND_CHESTPLATE).apply {
        addEnchantment(Enchantment.PROTECTION, 1)
      },
      ItemStack(Material.IRON_CHESTPLATE).apply {
        addEnchantment(Enchantment.PROTECTION, 1)
      }
    )

    leggings = listOf(
      null,
      ItemStack(Material.CHAINMAIL_LEGGINGS),
      ItemStack(Material.IRON_LEGGINGS)
    )

    boots = listOf(
      null,
      ItemStack(Material.CHAINMAIL_BOOTS),
      ItemStack(Material.IRON_BOOTS)
    )

    mainhand = listOf(
      ItemStack(Material.DIAMOND_SWORD),
      ItemStack(Material.IRON_SWORD)
    )
  }

  val swabbie = PirateSkeletonType().apply {
    name = "Swabbie"

    health = 20f
    attackDamage = 20f
    followRange = 40f
    moveSpeed = 0.32f

    helmet = listOf(
      null,
      createHelmet(10080001)
    )

    chestplate = listOf(
      null,
      ItemStack(Material.LEATHER_CHESTPLATE).apply {
        editMeta(LeatherArmorMeta::class.java) {
          it.setColor(Color.fromRGB(10362912))
        }
      }
    )

    leggings = listOf(
      null,
      ItemStack(Material.LEATHER_LEGGINGS).apply {
        editMeta(LeatherArmorMeta::class.java) {
          it.setColor(Color.fromRGB(1842204))
        }
      }
    )

    boots = listOf(
      null,
      ItemStack(Material.LEATHER_BOOTS).apply {
        editMeta(LeatherArmorMeta::class.java) {
          it.setColor(Color.fromRGB(1842204))
        }
      }
    )

    mainhand = listOf(
      ItemStack(Material.WOODEN_SWORD),
      ItemStack(Material.STONE_SWORD)
    )
  }

  list.add(10, crew)
  list.add(30, swabbie)

  return list
}

private fun createHelmet(modelId: Int): ItemStack {
  return ItemStack(Material.BAMBOO_BUTTON).apply {
    val meta = itemMeta

    meta.addAttributeModifier(Attribute.GENERIC_ARMOR, createModifier(ARMOR_MOD_KEY))
    meta.addAttributeModifier(
      Attribute.GENERIC_ARMOR_TOUGHNESS,
      createModifier(ARMOR_TOUGHNESS_MOD_KEY)
    )
    meta.setCustomModelData(modelId)

    itemMeta = meta
  }
}

private fun createModifier(key: String): AttributeModifier {
  val namespacedKey: NamespacedKey = NamespacedKey.fromString(key)!!
  return AttributeModifier(namespacedKey, 2.0, AttributeModifier.Operation.ADD_NUMBER)
}

fun getRandomSkeletonType(random: Random): PirateSkeletonType {
  return SKELETON_TYPES.get(random)
}

fun riseFromTheGrave(skeleton: Skeleton) {
  skeleton.isInvulnerable = true

  val effect = PotionEffect(
    PotionEffectType.LEVITATION,
    LEVITATION_POTION_DURATION,
    LEVITATION_POTION_AMPLIFIER,
    true,
    false,
    false
  )
  skeleton.addPotionEffect(effect)

  val moveAttr = skeleton.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)!!
  val moveSpeed = moveAttr.baseValue

  moveAttr.baseValue = 0.0;

  val scheduler = skeleton.scheduler

  graveParticles(skeleton.location.add(0.0, 0.5, 0.0))

  scheduler.runDelayed(
    getPiratesPlugin(),
    GraveRiseTask(skeleton, moveSpeed),
    null,
    LEVITATION_POTION_DURATION.toLong()
  )
}

private fun graveParticles(location: Location) {
  Particle.CAMPFIRE_COSY_SMOKE.builder()
    .extra(0.0)
    .offset(0.25, 0.25, 0.25)
    .location(location.world, location.x, location.y + 0.5, location.z)
    .count(10)
    .allPlayers()
    .spawn()
}

private class GraveRiseTask(val skeleton: Skeleton, val moveSpeed: Double):
  Consumer<ScheduledTask>
{

  override fun accept(t: ScheduledTask) {
    if (skeleton.isDead) {
      return
    }

    val moveAttr = skeleton.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)!!
    moveAttr.baseValue = moveSpeed
    skeleton.isInvulnerable = false
  }
}

fun spawnSkeletonTypeAt(type: PirateSkeletonType, location: Location, random: Random): Skeleton {
  val w = location.world

  return w.spawn(location, Skeleton::class.java) {
    if (type.name != null) {
      it.customName(text(type.name!!))
    }

    it.lootTable = LootTables.EMPTY.lootTable

    val equip = it.equipment

    for (value in EquipmentSlot.entries) {
      equip.setDropChance(value, 0.0f)
    }

    equip.itemInMainHandDropChance = type.mainhandDropChance
    equip.itemInOffHandDropChance = type.offhandDropChance

    equip.helmet = getClonedItem(type.helmet, random)
    equip.chestplate = getClonedItem(type.chestplate, random)
    equip.leggings = getClonedItem(type.leggings, random)
    equip.boots = getClonedItem(type.boots, random)

    equip.setItemInMainHand(getClonedItem(type.mainhand, random))
    equip.setItemInOffHand(getClonedItem(type.offhand, random))

    applyAttributeValue(it, Attribute.GENERIC_MAX_HEALTH, type.health)
    applyAttributeValue(it, Attribute.GENERIC_MOVEMENT_SPEED, type.moveSpeed)
    applyAttributeValue(it, Attribute.GENERIC_ATTACK_DAMAGE, type.attackDamage)
    applyAttributeValue(it, Attribute.GENERIC_FOLLOW_RANGE, type.followRange)

    it.health = it.getAttribute(Attribute.GENERIC_MAX_HEALTH)!!.value

    getSkeletonTeam().addEntity(it)
  }
}

private fun applyAttributeValue(e: Attributable, attr: Attribute, value: Float?) {
  if (value == null) {
    return
  }

  val inst = e.getAttribute(attr) ?: return
  inst.baseValue = value.toDouble()
}

private fun getClonedItem(list: List<ItemStack?>?, random: Random): ItemStack? {
  if (list.isNullOrEmpty()) {
    return null
  }

  val item: ItemStack?

  if (list.size == 1) {
    item = list[0]
  } else {
    item = list[random.nextInt(list.size)]
  }

  return (item ?: return null).clone()
}

private fun getSkeletonTeam(): Team {
  val scoreboard = Bukkit.getScoreboardManager().mainScoreboard
  var team: Team? = scoreboard.getTeam(TEAM_NAME)

  if (team == null) {
    val nteam = scoreboard.registerNewTeam(TEAM_NAME)
    nteam.color(NamedTextColor.RED)
    nteam.setAllowFriendlyFire(false)
    team = nteam
  }

  return team
}