package here.lenrik.chili_map.map

import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.Identifier

data class MapContainer(private val name: Text, private val levelId: Identifier) {
	val levels = HashMap<Identifier, LevelMap>(4)

	constructor(identifier: Identifier) : this(Text.of(identifier.toString()), identifier)

	init {
		levels[levelId] = LevelMap(levelId, (name as MutableText) + levelId.toString())
	}
}

private operator fun MutableText.plus(next: String): MutableText {
	return append(next)
}
