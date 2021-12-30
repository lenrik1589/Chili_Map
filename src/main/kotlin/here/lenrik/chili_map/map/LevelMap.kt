package here.lenrik.chili_map.map

import here.lenrik.chili_map.misc.Misc
import net.minecraft.text.Text
import net.minecraft.util.Identifier

data class LevelMap(val levelId: Identifier, val name: Text) {
	val regions = HashMap<Misc.Vec2i, MapRegion>()
}