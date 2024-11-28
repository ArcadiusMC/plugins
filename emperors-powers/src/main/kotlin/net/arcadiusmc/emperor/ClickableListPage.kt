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
import net.arcadiusmc.text.TextWriter
import net.arcadiusmc.text.loader.MessageRender
import net.arcadiusmc.user.User
import net.arcadiusmc.user.Users
import net.arcadiusmc.utils.context.Context
import net.arcadiusmc.utils.inventory.ItemStacks
import net.arcadiusmc.utils.inventory.SkullItemBuilder
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import org.bukkit.inventory.ItemStack

private val config: MenuConfig = MenuConfig(
  text("Shop list"),
  text("Shop List"),
  listOf(text("Click a shop to perform actions on it")),
  listOf("*"),
  54
)

private const val ARROW_UP = "957a5bdf42f152178d154bb2237d9fd35772a7f32bcfd33beeb8edc4820ba"
private const val ARROW_DOWN = "96a011e626b71cead984193511e82e65c1359565f0a2fcd1184872f89d908c65"
private const val SELECTED_ARROW_UP = "45c588b9ec0a08a37e01a809ed0903cc34c3e3f176dc92230417da93b948f148"
private const val SELECTED_ARROW_DOWN = "1cb8be16d40c25ace64e09f6086d408ebc3d545cfb2990c5b6c25dabcedeacc"

const val MOD_ID = "emperors_powers"

private val raiseRentSlot  = Slot.of(3, 1)
private val lowerRentSlot  = Slot.of(3, 2)
private val raiseTaxesSlot = Slot.of(5, 1)
private val lowerTaxesSlot = Slot.of(5, 2)

class ClickableListPage(settings: MenuSettings): ShopListMenu(ShopLists.PAGE, settings, config) {

  override fun onClick(user: User, entry: Market, context: Context?, click: ClickContext) {
    if (entry.ownerId == null) {
      return
    }

    val page = ActionsPage(this, entry)
    page.menu.open(user, context)
  }

  public override fun getItem(user: User?, entry: Market?, context: Context?): ItemStack {
    return super.getItem(user, entry, context)
  }

  override fun writeLore(writer: TextWriter, market: Market) {
    val taxMods = getModifier(market.taxModifiers)
    val rentMods = getModifier(market.rentModifiers)

    val rentStatus: Component = Messages.render(getStatusText(rentMods, "rent"))
      .create(writer.viewer())

    val taxStatus: Component = Messages.render(getStatusText(taxMods, "tax"))
      .create(writer.viewer())

    val taxRate = market.taxRate
    val rent = market.rent

    writer.write(
      Messages.render("emperor.headerLore")
        .addValue("tax", taxRate)
        .addValue("rent", rent)
        .addValue("rentStatus", rentStatus)
        .addValue("taxStatus", taxStatus)
        .create(writer.viewer())
    )
  }

  private fun getStatusText(mods: FoundModifiers, key: String): String {
    var messageKey: String = "emperor.headerLore.status.$key."

    if (!mods.foundAny) {
      messageKey += "unchanged"
    } else if (mods.positive != null) {
      messageKey += "raised"
    } else {
      messageKey += "lowered"
    }

    return messageKey
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

  override fun createItem(user: User, context: Context): ItemStack {
    val p: ClickableListPage = parent as ClickableListPage
    return p.getItem(user, market, context)
  }

  private fun createTaxNode(raise: Boolean): MenuNode {
    return MenuNode.builder()
      .setItem { user, context ->
        val builder = createItem("taxes", user, raise, market.taxModifiers)
        val bracket = market.taxBracket

        builder.addLore("")

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
        applyModifierTo(
          user,
          market.taxModifiers,
          amount,
          "taxes",
          raise,
          market.baseTaxRate
        )
        click.shouldReloadMenu(true)
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
        applyModifierTo(
          user,
          market.rentModifiers,
          amount,
          "rent",
          raise,
          market.baseRent.toFloat()
        )
        click.shouldReloadMenu(true)
      }
      .build()
  }

  private fun applyModifierTo(
    user: User,
    list: ValueModifierList,
    amount: Float,
    messageKey: String,
    raise: Boolean,
    base: Float
  ) {
    val messagePrefix = makeMessagePrefix(messageKey, raise)
    val found = getModifier(list)

    list.modifiers.remove(found.positive)
    list.modifiers.remove(found.negative)

    if (found.foundMatching(raise)) {
      user.sendMessage(makeMessage("$messagePrefix.removed").create(user))
      return
    }

    val modOp = if (raise) ModifierOp.ADD_MULTIPLIED else ModifierOp.SUB_MULTIPLIED
    val mod = Modifier(amount, modOp, null, MOD_ID, "Imperial Decree")

    list.add(mod)

    user.sendMessage(
      makeMessage("$messagePrefix.applied")
        .addValue("amount", amount)
        .addValue("newAmount", list.apply(base))
        .create(user)
    )
  }

  private fun createItem(
    messageKey: String,
    user: User,
    raise: Boolean,
    list: ValueModifierList
  ): SkullItemBuilder {
    val builder = ItemStacks.headBuilder()

    val modifier = getModifier(list)
    val foundMatching = modifier.foundMatching(raise)

    if (raise) {
      builder.setTextureId(if (foundMatching) SELECTED_ARROW_UP else ARROW_UP)
    } else {
      builder.setTextureId(if (foundMatching) SELECTED_ARROW_DOWN else ARROW_DOWN)
    }

    val messagePrefix = makeMessagePrefix(messageKey, raise)

    val name = makeMessage("$messagePrefix.name").create(user)
    val lore = makeMessage("$messagePrefix.lore").create(user)

    builder.setName(name)
    builder.addLore(lore)

    if (foundMatching) {
      val activeText = makeMessage("$messagePrefix.active").create(user)

      builder
        .addLore("")
        .addLore(activeText)
        .addEnchantGlint()
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

    val config = getPlugin().emperorConfig
    val baseRent = market.baseRent.toFloat()
    val rentChange = getChange(baseRent, market.rentModifiers, config?.rentChangeRate)
    val taxChange = getChange(market.baseTaxRate, market.taxModifiers, config?.taxChangeAmount)

    return Messages.render(messageKey)
      .addValue("shopOwner", owner)
      .addValue("tax", taxRate)
      .addValue("rent", rent)
      .addValue("rentChange", rentChange)
      .addValue("taxChange", taxChange)
  }

  private fun getChange(base: Float, list: ValueModifierList, emperorMod: Float?): Float {
    if (emperorMod == null) {
      return base
    }

    val without = applyWithoutEmperorMods(base, list)
    val with = without + (without * emperorMod)

    return with - without
  }

  private fun applyWithoutEmperorMods(base: Float, list: ValueModifierList): Float {
    var result: Float = base

    for (modifier in list.modifiers) {
      if (modifier.tag == MOD_ID) {
        continue
      }

      result = modifier.applyModifer(base, result)
    }

    return result
  }
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