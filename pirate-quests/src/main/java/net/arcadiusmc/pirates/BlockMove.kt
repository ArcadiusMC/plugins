package net.arcadiusmc.pirates

import net.arcadiusmc.utils.BlockCopy
import org.bukkit.World
import org.spongepowered.math.vector.Vector3i

fun copyBlocks(originA: Vector3i, originB: Vector3i, destination: Vector3i, world: World) {
  BlockCopy.copyBlocks(originA, originB, destination, world)
}