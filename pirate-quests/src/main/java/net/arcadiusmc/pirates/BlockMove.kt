package net.arcadiusmc.pirates

import net.arcadiusmc.utils.math.WorldBounds3i
import org.bukkit.Location
import org.bukkit.World
import org.spongepowered.math.vector.Vector3i

fun copyBlocks(originA: Vector3i, originB: Vector3i, destination: Vector3i, world: World) {
  val bounds = WorldBounds3i.of(world, originA, originB)
  val min = bounds.min()

  for (block in bounds) {
    val offX = block.x - min.x()
    val offY = block.y - min.y()
    val offZ = block.z - min.z()

    val destPos = destination.add(offX, offY, offZ)
    val state = block.state

    val copy = state.copy(
      Location(
        world,
        destPos.x().toDouble(),
        destPos.y().toDouble(),
        destPos.z().toDouble()
      )
    )

    copy.update(true, false)
  }

}