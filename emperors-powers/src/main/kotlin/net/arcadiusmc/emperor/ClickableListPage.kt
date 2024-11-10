package net.arcadiusmc.emperor

import net.arcadiusmc.markets.Market
import net.arcadiusmc.markets.ValueModifierList
import net.arcadiusmc.markets.ValueModifierList.Modifier
import net.arcadiusmc.markets.ValueModifierList.ModifierOp
import net.arcadiusmc.markets.gui.ShopListMenu
import net.arcadiusmc.markets.gui.ShopLists
import net.arcadiusmc.markets.gui.ShopLists.MenuConfig
import net.arcadiusmc.markets.gui.ShopLists.MenuSettings
import net.arcadiusmc.menu.*
import net.arcadiusmc.menu.page.MenuPage
import net.arcadiusmc.text.Messages
import net.arcadiusmc.text.loader.MessageRender
import net.arcadiusmc.user.User
import net.arcadiusmc.user.Users
import net.arcadiusmc.utils.context.Context
import net.arcadiusmc.utils.inventory.ItemBuilder
import net.arcadiusmc.utils.inventory.ItemStacks
import net.arcadiusmc.utils.inventory.SkullItemBuilder
import net.kyori.adventure.text.Component.text
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag

private val config: MenuConfig = MenuConfig(
  text("Shop list"),
  text("Shop List"),
  listOf(text("Click a shop to perform actions on it")),
  listOf("*"),
  54
)

private const val ARROW_UP_TEXTURE_ID = "957a5bdf42f152178d154bb2237d9fd35772a7f32bcfd33beeb8edc4820ba"
private const val ARROW_DOWN_TEXTURE_ID = "96a011e626b71cead984193511e82e65c1359565f0a2fcd1184872f89d908c65"

private const val MOD_ID = "emperors_powers"

private val raiseRentSlot = Slot.of(1)
private val lowerRentSlot = Slot.of(1)
private val raiseTaxesSlot = Slot.of(1)
private val lowerTaxesSlot = Slot.of(1)

class ClickableListPage(settings: MenuSettings): ShopListMenu(ShopLists.PAGE, settings, config) {

  override fun onClick(user: User, entry: Market, context: Context?, click: ClickContext) {
    if (entry.ownerId == null) {
      return
    }

    val page = ActionsPage(this, entry)
    page.menu.open(user, context)
  }
}

class ActionsPage(parent: MenuPage, val market: Market): MenuPage(parent) {

  init {
    initMenu(
      Menus.builder(Menus.sizeFromRows(4), text("Perform an action")),
      true
    )
  }

  override fun createMenu(builder: MenuBuilder) {

    builder.add(raiseTaxesSlot, createTaxNode(true))
    builder.add(lowerTaxesSlot, createTaxNode(false))

    builder.add(raiseRentSlot, createRentNode(true))
    builder.add(lowerRentSlot, createRentNode(false ))
  }

  class FoundModifiers {
    var positive: Modifier? = null
    var negative: Modifier? = null

    val foundAny: Boolean get() {
      return positive != null || negative != null
    }

    fun foundMatching(raise: Boolean): Boolean {
      if (raise) {
        return positive != null
      } else {
        return negative != null
      }
    }
  }

  private fun getModifier(list: ValueModifierList): FoundModifiers {
    val result = FoundModifiers()

    for (modifier in list.modifiers) {
      if (MOD_ID != modifier.tag) {
        continue
      }

      if (modifier.amount > 0) {
        result.positive = modifier
      } else if (modifier.amount < 0) {
        result.negative = modifier
      }
    }

    return result
  }

  private fun createTaxNode(raise: Boolean): MenuNode {
    return MenuNode.builder()
      .setItem { user, context ->
        val builder = createItem("taxes", user, raise, market.taxModifiers)
        val bracket = market.taxBracket

        if (bracket != null) {
          builder.addLore(
            Messages.render("emperor.taxBracketInfo.fallsInto")
              .addValue("amount", market.taxModifiers.apply(bracket.rate))
              .create(user)
          )
        } else {
          builder.addLore(
            Messages.render("emperor.taxBracketInfo.none")
              .create(user)
          )
        }

        return@setItem builder.build()
      }
      .setRunnable { user, context, click ->
        val amount = getPlugin().emperorConfig?.taxChangeAmount ?: 0.0f
        applyModifierTo(user, market.taxModifiers, amount, "taxes", raise)
      }
      .build()
  }

  private fun createRentNode(raise: Boolean): MenuNode {
    return MenuNode.builder()
      .setItem { user, context ->
        val builder = createItem("rent", user, raise, market.rentModifiers)

        return@setItem builder.build()
      }
      .setRunnable { user, context, click ->
        val amount = getPlugin().emperorConfig?.rentChangeRate ?: 0.0f
        applyModifierTo(user, market.rentModifiers, amount, "rent", raise)
      }
      .build()
  }

  private fun applyModifierTo(
    user: User,
    list: ValueModifierList,
    amount: Float,
    messageKey: String,
    raise: Boolean
  ) {
    var amount: Float = amount
    if (!raise) {
      amount = -amount
    }

    val messagePrefix = makeMessagePrefix(messageKey, raise)
    val found = getModifier(list)

    if (found.foundAny) {
      list.modifiers.remove(found.positive)
      list.modifiers.remove(found.negative)

      user.sendMessage(makeMessage("$messagePrefix.removed").create(user))
      return
    }

    val mod = Modifier(
      amount,
      ModifierOp.MULTIPLY,
      null,
      MOD_ID,
      "Emperor said so :("
    )

    list.add(mod)
    user.sendMessage(makeMessage("$messagePrefix.applied").create(user))
  }

  private fun createItem(
    messageKey: String,
    user: User,
    raise: Boolean,
    list: ValueModifierList
  ): SkullItemBuilder {
    val builder = ItemStacks.headBuilder()

    if (raise) {
      builder.setTextureId(ARROW_UP_TEXTURE_ID)
    } else {
      builder.setTextureId(ARROW_DOWN_TEXTURE_ID)
    }

    val messagePrefix = makeMessagePrefix(messageKey, raise)

    val name = makeMessage("$messagePrefix.name").create(user)
    val lore = makeMessage("$messagePrefix.lore").create(user)

    builder.setName(name)
    builder.addLore(lore)

    val modifier = getModifier(list)
    if (modifier.foundMatching(raise)) {
      val activeText = makeMessage("$messagePrefix.active").create(user)

      builder
        .addLore(activeText)
        .addEnchant(Enchantment.BINDING_CURSE, 1)
        .addFlags(ItemFlag.HIDE_ENCHANTS)
    }

    return builder;
  }

  private fun makeMessagePrefix(messageKey: String, raise: Boolean): String {
    return "emperor.${messageKey}.${if (raise) "raise" else "lower"}"
  }

  private fun makeMessage(messageKey: String): MessageRender {
    val owner = Users.get(market.ownerId)

    val rent = market.rent
    val taxRate = market.taxRate

    return Messages.render(messageKey)
      .addValue("shopOwner", owner)
      .addValue("taxRate", taxRate)
      .addValue("rent", rent)
  }
}