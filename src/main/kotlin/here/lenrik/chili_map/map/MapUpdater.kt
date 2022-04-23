package here.lenrik.chili_map.map

import here.lenrik.chili_map.client.ChilliMapClient
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.world.World

class MapUpdater {
	companion object {
		fun updateContainer(world: World, player: PlayerEntity) {
			world.profiler.push("chili_map:map_updates")
			ChilliMapClient.updateCounter = 0
			++ ChilliMapClient.autoSaveCounter
			ChilliMapClient.container?.getLevel(world.registryKey.value)?.updateMaps(world, player)
			world.profiler.pop()
		}
	}
}