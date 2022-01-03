package here.lenrik.chili_map.map

import here.lenrik.chili_map.client.ChilliMapClient
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.math.Vec3i
import net.minecraft.world.World
import kotlin.system.exitProcess

class MapUpdater {
	companion object {
		fun updateColors(world: World, player: PlayerEntity) {
			world.profiler.push("chili_map:map_updates")
			ChilliMapClient.updateCounter = 0
			++ ChilliMapClient.autoSaveCounter
			var areas = listOf<Vec3i>()

			for (zoomLevel in 0..4) for (oX in -1..1) for (oY in -1..1) {
				val pos = AreaMap.toMapPosAtZoomLevel(player.pos.add(128.0 * oX, .0, 128.0 * oY), zoomLevel)
				if (!areas.contains(pos))
					areas = areas + pos
			}

			for (pos in areas) {
				++ChilliMapClient.updateCounter
				ChilliMapClient.container?.getLevel(world.registryKey.value)?.getMap(pos)!!.updateColors(world, player)
			}
			world.profiler.pop()
		}
	}
}