package net.arcadiusmc.pirates;

import net.arcadiusmc.events.Events
import net.arcadiusmc.pirates.captain.CaptainListener
import org.bukkit.plugin.java.JavaPlugin

fun getPiratesPlugin(): PiratesQuestPlugin {
  return JavaPlugin.getPlugin(PiratesQuestPlugin::class.java)
}

class PiratesQuestPlugin: JavaPlugin() {

  override fun onEnable() {
    Events.register(CaptainListener())
  }

  override fun onDisable() {

  }
}