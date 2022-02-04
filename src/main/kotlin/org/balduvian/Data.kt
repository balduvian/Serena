package org.balduvian

import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.FileWriter

class Data(var grabChannelID: Long, var stageChannelID: Long, var sendChannelID: Long) {
	fun save() {
		val file = File(DATA_FILENAME)

		if (file.exists() && file.isDirectory) file.deleteRecursively()

		val writer = FileWriter(file, false)

		writer.write("${grabChannelID}\n")
		writer.write("${stageChannelID}\n")
		writer.write("${sendChannelID}\n")

		writer.close()
	}

	companion object {
		val DATA_FILENAME = "./data.txt"

		fun get(): Data? {
			val file = File(DATA_FILENAME)

			if (file.exists()) {
				if (file.isDirectory) {
					file.deleteRecursively()
					return createDummy()
				}

				val reader = BufferedReader(FileReader(file))
				val lines = reader.readLines()
				reader.close()

				val grabId = lines[0].toLongOrNull() ?: return createDummy()
				val stageId = lines[1].toLongOrNull() ?: return createDummy()
				val sendId = lines[2].toLongOrNull() ?: return createDummy()

				return Data(grabId, stageId, sendId)

			} else {
				return createDummy()
			}
		}

		fun createDummy(): Data? {
			val file = File(DATA_FILENAME)

			if (file.exists() && file.isDirectory) file.deleteRecursively()

			val writer = FileWriter(file, false)

			writer.write("PUT GRAB CHANNEL ID ON THIS LINE\n")
			writer.write("PUT STAGE CHANNEL ID ON THIS LINE\n")
			writer.write("PUT SEND CHANNEL ID ON THIS LINE\n")

			writer.close()

			return null
		}
	}
}
