package here.lenrik.chili_map.client.config

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import fi.dy.masa.malilib.config.ConfigType
import fi.dy.masa.malilib.config.IConfigOptionList
import fi.dy.masa.malilib.config.IConfigOptionListEntry
import fi.dy.masa.malilib.config.options.ConfigBase

class ConfigEnum<E : Enum<E>>(val enum: Class<E>, val default: E = enum.enumConstants[0], name: String? = null, comment: String? = null, prefix: String = "") :
	ConfigBase<ConfigEnum<E>>(ConfigType.OPTION_LIST, name ?: comment?.removeSuffix(".comment") ?: (prefix + enum.simpleName), comment ?: name?.plus(".comment") ?: (prefix + enum.simpleName + ".comment")), IConfigOptionList {

	class EnumOptionEntry<E : Enum<E>>(private val default: E) : IConfigOptionListEntry, Cloneable {
		var enumValue: E = default

		override fun getStringValue(): String = enumValue.name

		override fun getDisplayName(): String = enumValue.name

		override fun cycle(forward: Boolean): EnumOptionEntry<E> {
			enumValue = values[(enumValue.ordinal + values.size + if(forward) 1 else -1) % values.size]
			return this
		}

		override fun fromString(json: String?): EnumOptionEntry<E> {
			when (json) {
				null, "" -> Unit
				else -> enumValue = values.first { it.name == json }
			}
			return this
		}

		fun reset() {
			enumValue = default
		}

		private val values: Array<E>
			get() = enumValue.declaringClass.enumConstants
	}

	var enumValue: E
		get() = current.enumValue
		set(value) {
			current.enumValue = value
		}


	private val defaultEntry: EnumOptionEntry<E> = EnumOptionEntry(default)

	private var current: EnumOptionEntry<E> = defaultEntry

	override fun setValueFromJsonElement(json: JsonElement?) {
		current = defaultEntry.fromString(json?.asString)
	}

	override fun getAsJsonElement(): JsonElement = JsonPrimitive(current.enumValue.name)

	override fun isModified(): Boolean = current.enumValue != default

	override fun resetToDefault() {
		current.reset()
	}

	override fun getOptionListValue(): IConfigOptionListEntry = current

	override fun getDefaultOptionListValue(): IConfigOptionListEntry = defaultEntry

	override fun setOptionListValue(value: IConfigOptionListEntry?) {
		@Suppress("UNCHECKED_CAST")
		when (value) {
			is EnumOptionEntry<*> -> current = value as EnumOptionEntry<E>
			else -> current
		}
	}
}