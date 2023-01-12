package here.lenrik.chili_map.client.compat.modmenu

import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import here.lenrik.chili_map.client.config.ChiliMapConfigScreen
import net.minecraft.client.gui.screen.Screen

class ChiliMapModMenuCompat : ModMenuApi {
	override fun getModConfigScreenFactory(): ConfigScreenFactory<*> {
		return ConfigScreenFactory<Screen>(::ChiliMapConfigScreen)
	}
}