package here.lenrik.chili_map.client

import net.minecraft.world.level.storage.LevelStorage

interface SessionAccessor {
	fun getSession(): LevelStorage.Session
}