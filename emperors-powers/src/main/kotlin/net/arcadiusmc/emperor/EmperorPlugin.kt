package net.arcadiusmc.emperor

import com.mojang.serialization.Codec
import net.arcadiusmc.markets.MarketsPlugin
import net.arcadiusmc.text.loader.MessageLoader
import net.arcadiusmc.utils.io.ConfigCodec
import net.arcadiusmc.utils.io.ExistingObjectCodec
import org.bukkit.plugin.java.JavaPlugin

class EmperorPlugin: JavaPlugin() {

  var listPage: ClickableListPage? = null
  var emperorConfig: EmperorConfig? = null

  override fun onEnable() {
    reloadConfig()
    CommandEmperorPowers(this)
  }

  override fun onDisable() {

  }

  override fun reloadConfig() {
    MessageLoader.loadPluginMessages(this)

    val markets = MarketsPlugin.plugin()
    val settings = markets.lists.currentSettings

    listPage = ClickableListPage(settings)

    emperorConfig = ConfigCodec.loadPluginConfig(this, CONFIG_CODEC)
      .orElseGet { EmperorConfig() }
  }
}

fun getPlugin(): EmperorPlugin {
  return JavaPlugin.getPlugin(EmperorPlugin::class.java)
}

val CONFIG_CODEC = ExistingObjectCodec.createCodec({EmperorConfig()}, { builder ->
  builder.optional("tax-change-amount", Codec.FLOAT).apply {
    setter { t, u -> t.taxChangeAmount = u }
    getter { t -> t.taxChangeAmount}
  }

  builder.optional("rent-change-rate", Codec.FLOAT).apply {
    setter { t, u -> t.rentChangeRate = u }
    getter { t -> t.rentChangeRate}
  }
})

class EmperorConfig {
  var taxChangeAmount: Float = 0.02f
  var rentChangeRate: Float = 0.25f;
}