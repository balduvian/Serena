package org.balduvian

import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File

object Settings {
	var internalCache: JsonElement? = null

	val SETTINGS_FILENAME = "./settings.json"

	val gson = GsonBuilder().create()

	fun load() {
		val file = File(SETTINGS_FILENAME)
		if (!file.exists()) {
			println("settings json (${SETTINGS_FILENAME}) not found")
			return
		}

		val parsed = JsonParser.parseReader(file.reader())
		internalCache = parsed
	}

	inline fun <reified Type> get(key: String): Type? {
		val cache = internalCache ?: return null
		if (!cache.isJsonObject) return null

		return try {
			gson.fromJson(cache.asJsonObject[key], Type::class.java)
		} catch (ex: Throwable) {
			println("incorrect format for setting \"${key}\"")
			null
		}
	}

	inline fun <reified Type> save(key: String, setting: Type) {
		var cache = internalCache ?: JsonObject()
		if (!cache.isJsonObject) cache = JsonObject()

		cache.asJsonObject.remove(key)
		cache.asJsonObject.addProperty(key, gson.toJson(setting, Type::class.java))

		try {
			val file = File(SETTINGS_FILENAME)
			file.writer().write(gson.toJson(cache))
		} catch (ex: Throwable) {
			println("could not save settings")
		}

		internalCache = cache
	}
}
