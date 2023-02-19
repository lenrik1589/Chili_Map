package here.lenrik.chili_map.client.config

import fi.dy.masa.malilib.gui.GuiConfigsBase
import fi.dy.masa.malilib.gui.button.ButtonGeneric
import fi.dy.masa.malilib.util.StringUtils
import here.lenrik.chili_map.ChiliMap
import here.lenrik.chili_map.client.ChiliMapClient
import net.minecraft.client.gui.screen.Screen

class ChiliMapConfigScreen(previous: Screen?) : GuiConfigsBase(10, 50, ChiliMap.ModId, previous, "${ChiliMap.ModId}.config.title") {
	private var selectedTab: ConfigTab = ConfigTab.Generic

	override fun initGui() {
		super.initGui()
		clearOptions()
		var x = 10
		var y = 26
		for(tab in ConfigTab.values()) {
			if (tab == ConfigTab.Debug) continue
			x += createButton(x, y, -1, tab) + 2
		}
	}

	fun createButton(x: Int, y: Int, width: Int, tab: ConfigTab) = ButtonGeneric(x, y, width, 20, tab.displayName).let { button ->
		addButton(button) { _, _ ->
			selectedTab = tab
			reCreateListWidget()
			listWidget?.resetScrollbarPosition()
			initGui()
		}
		button.width
	}

	override fun getConfigs(): MutableList<ConfigOptionWrapper> = ConfigOptionWrapper.createFor(
		when (selectedTab) {
			ConfigTab.Generic -> mutableListOf(
				ChiliMapClient.config.openSettingsKey,
				ChiliMapClient.config.openWorldMapKey,
				ChiliMapClient.config.zoomInKey,
				ChiliMapClient.config.zoomOutKey,
				ChiliMapClient.config.createWaypointKey,
			)
			ConfigTab.Compat -> mutableListOf(
				ChiliMapClient.config.chunkDebugCompatMapOverlayOption
			)
			ConfigTab.Debug -> mutableListOf(

			)
			ConfigTab.Rendering -> mutableListOf(
				ChiliMapClient.config.mappingModeOption,
				ChiliMapClient.config.minimapModeOption,
				ChiliMapClient.config.waterModeOption,
				ChiliMapClient.config.baseRenderDistanceOption,
				ChiliMapClient.config.borderRadiusOption,
				ChiliMapClient.config.minimapScaleOption,
			)
		}
	)

	enum class ConfigTab {
		Generic,
		Debug,
		Rendering,
		Compat;

		val displayName
			get() = StringUtils.translate("${ChiliMap.ModId}.tab.title.${name.lowercase()}")!!
	}

	override fun onClose() {
		super.onClose()
		ChiliMapClient.config.save()
	}
}
