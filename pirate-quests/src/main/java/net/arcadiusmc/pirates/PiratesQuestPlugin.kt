package net.arcadiusmc.pirates;

import net.arcadiusmc.events.Events
import net.arcadiusmc.pirates.captain.CaptainListener
import net.arcadiusmc.pirates.catacombs.CatacombListener
import net.arcadiusmc.pirates.catacombs.catacombsShutdown
import org.bukkit.plugin.java.JavaPlugin

fun getPiratesPlugin(): PiratesQuestPlugin {
  return JavaPlugin.getPlugin(PiratesQuestPlugin::class.java)
}

class PiratesQuestPlugin: JavaPlugin() {

  override fun onEnable() {
    Events.register(CaptainListener())
    Events.register(CatacombListener())
  }

  override fun onDisable() {
    catacombsShutdown()
  }
}