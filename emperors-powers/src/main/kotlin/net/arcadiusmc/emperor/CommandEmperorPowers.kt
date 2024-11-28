package net.arcadiusmc.emperor

import com.mojang.brigadier.Command.SINGLE_SUCCESS
import net.arcadiusmc.command.BaseCommand
import net.arcadiusmc.command.arguments.Arguments
import net.arcadiusmc.markets.Markets
import net.arcadiusmc.markets.ValueModifierList
import net.arcadiusmc.markets.gui.ShopLists
import net.arcadiusmc.text.Messages
import net.arcadiusmc.user.User
import net.forthecrown.grenadier.GrenadierCommand

class CommandEmperorPowers: BaseCommand {

  val plugin: EmperorPlugin

  constructor(plugin: EmperorPlugin) : super("emperors-powers") {
    this.plugin = plugin
    setDescription("Emperor's Powers command")
    register()
  }

  override fun createCommand(command: GrenadierCommand) {
    command
      .then(literal("open")
        .then(argument("user", Arguments.ONLINE_USER)
          .executes { c ->
            val user: User = Arguments.getUser(c, "user")

            val ctx = ShopLists.SET.createContext()
              .set(ShopLists.PAGE, 0)

            plugin.listPage!!.menu.open(user, ctx)

            return@executes SINGLE_SUCCESS
          }
        )
      )

      .then(literal("clear-all-modifers")
        .executes { c ->
          for (market in Markets.getManager().markets) {
            removeMods(market.taxModifiers)
            removeMods(market.rentModifiers)
            removeMods(market.priceModifiers)
          }

          c.source.sendSuccess(Messages.renderText("emperor.clearedMods", c.source))
          return@executes SINGLE_SUCCESS
        }
      )
  }

  fun removeMods(list: ValueModifierList) {
    list.modifiers.removeIf { t -> t.tag == MOD_ID }
  }
}