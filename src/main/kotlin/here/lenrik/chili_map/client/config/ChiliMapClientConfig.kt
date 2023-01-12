package here.lenrik.chili_map.client.config

import com.google.gson.JsonObject
import fi.dy.masa.malilib.config.ConfigUtils
import fi.dy.masa.malilib.config.IConfigBase
import fi.dy.masa.malilib.config.IConfigHandler
import fi.dy.masa.malilib.config.options.ConfigBoolean
import fi.dy.masa.malilib.config.options.ConfigDouble
import fi.dy.masa.malilib.config.options.ConfigHotkey
import fi.dy.masa.malilib.config.options.ConfigInteger
import fi.dy.masa.malilib.hotkeys.KeyAction
import fi.dy.masa.malilib.hotkeys.KeybindSettings
import fi.dy.masa.malilib.util.FileUtils
import fi.dy.masa.malilib.util.JsonUtils
import here.lenrik.chili_map.ChiliMap
import java.io.File
import kotlin.reflect.full.memberProperties

@Suppress("unused")
class ChiliMapClientConfig : IConfigHandler {

	val RELEASE_CANCEL: KeybindSettings = KeybindSettings.create(KeybindSettings.Context.INGAME, KeyAction.RELEASE, false, true, false, true)

	enum class MinimapMode {
		SingleMap,
		Centered,
		Static
	}

	enum class MappingMode {
		Vanilla,
		All
	}

	@Suppress("unused")
	enum class WaterMode(val limit: Int) {
		Vanilla(Int.MAX_VALUE / 256),
		Thorough(40),
		Optimistic(10),
		Skip(0)
	}

	// -------------------------------------------------------------------------

	val mappingModeOption = ConfigEnum(MappingMode::class.java, prefix = "option.chili_map.render.")
	val minimapModeOption = ConfigEnum(MinimapMode::class.java, name = "option.chili_map.minimap.mode")
	val waterModeOption = ConfigEnum(WaterMode::class.java, WaterMode.Optimistic, prefix = "option.chili_map.render.")

	val baseRenderDistanceOption = ConfigInteger("option.chili_map.render.renderDistance", 128, 0, 512, "option.chili_map.render.renderDistance.comment")
	val borderRadiusOption = ConfigInteger("option.chili_map.render.borderRadius", 2, 0, 32, "option.chili_map.render.borderRadius.comment")
	val minimapScaleOption = ConfigDouble("option.chili_map.minimap.scale", 2.0, 0.0, 5.0, "option.chili_map.minimap.scale.comment")

	val openSettingsKey = ConfigHotkey("option.chili_map.key.settings.open", "N,C", "key.categories.gameplay")
	val openWorldMapKey = ConfigHotkey("option.chili_map.key.world_map.open", "N", RELEASE_CANCEL, "key.categories.gameplay")
	val zoomInKey = ConfigHotkey("option.chili_map.key.mini_map.zoom_in", "", "key.categories.gameplay")
	val zoomOutKey = ConfigHotkey("option.chili_map.key.mini_map.zoom_out", "", "key.categories.gameplay")
	val createWaypointKey = ConfigHotkey("option.chili_map.key.waypoint.create", "", "key.categories.gameplay")

	val chunkDebugCompatMapOverlayOption = ConfigBoolean("option.chili_map.chunkDebugCompatMapOverlayOption", false, "option.chili_map.chunkDebugCompatMapOverlayOption.comment")

	// -------------------------------------------------------------------------  /execute in minecraft:overworld run tp @s 3263.58 68.63 -148191.00 145.71 41.71
	// execute as @e[type=minecraft:boat, nbt={NoGravity:1b}] run data modify entity @s Motion[2] set value -10

	var mappingMode by mappingModeOption::enumValue
	var minimapMode by minimapModeOption::enumValue
	var waterMode by waterModeOption::enumValue

	var minimapZoom = 0
	var borderRadius by borderRadiusOption::integerValue
	var baseRenderDistance by baseRenderDistanceOption::integerValue
	var minimapScale by minimapScaleOption::doubleValue

	var chunkDebugCompatMapOverlay by chunkDebugCompatMapOverlayOption::booleanValue

	override fun load() {
		val configFile = File(FileUtils.getConfigDirectory(), "${ChiliMap.ModId}.json")
		if(configFile.exists() && configFile.isFile && configFile.canRead()) {
			val element = JsonUtils.parseJsonFile(configFile)
			if(element != null && element.isJsonObject) {
				val root = element.asJsonObject
				ConfigUtils.readConfigBase(root, "Generic", ChiliMapClientConfig::class.memberProperties.filter { it.isFinal }.mapNotNull { it.get(this) as? IConfigBase })
			}
		}

	}

	override fun save() {
		val dir = FileUtils.getConfigDirectory()
		if(dir.exists() && dir.isDirectory || dir.mkdirs()) {
			val root = JsonObject()
			ConfigUtils.writeConfigBase(root, "Generic", ChiliMapClientConfig::class.memberProperties.filter { it.isFinal }.mapNotNull { it.get(this) as? IConfigBase })
			JsonUtils.writeJsonToFile(root, File(dir, "${ChiliMap.ModId}.json"))
		}
	}
}