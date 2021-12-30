package here.lenrik.chili_map.misc

import net.minecraft.text.MutableText
import net.minecraft.text.Text

class TextTensions {
	operator fun MutableText.plus(next: String): MutableText{
		return this.append(next)
	}
}