package here.lenrik.chili_map.map

import here.lenrik.chili_map.client.ChiliMapClient
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.world.World

class MapUpdater {
	companion object {

		@JvmStatic
		fun updateContainer(world: World, player: PlayerEntity) {
			world.profiler.push("chili_map:map_updates")
			ChiliMapClient.updateCounter = 0
			++ ChiliMapClient.autoSaveCounter
			ChiliMapClient.container?.getLevel(world.registryKey.value)?.updateMaps(world, player)
			world.profiler.pop()
		}
	}
}