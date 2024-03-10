package org.balduvian

import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.time.LocalDateTime

class LastDay(val year: Int, val month: Int, val day: Int) {
    fun save() {
        val file = File(FILE_NAME)

        if (file.exists() && file.isDirectory) file.deleteRecursively()

        val writer = FileWriter(file, false)

        writer.write("$year $month $day")

        writer.close()
    }

    fun today(): Boolean {
        val time = LocalDateTime.now()
        return time.year == year && time.monthValue == month && time.dayOfMonth == day
    }

    companion object {
        const val FILE_NAME = "./storage.txt"

        fun get(): LastDay? {
            val file = File(FILE_NAME)

            if (file.exists() && file.isFile) {
                val reader = BufferedReader(FileReader(file))
                val line = reader.readLine() ?: return null
                reader.close()

                val parts = line.split(" ")
                if (parts.size != 3) return null

                val year = parts[0].toIntOrNull() ?: return null
                val month = parts[1].toIntOrNull() ?: return null
                val day = parts[2].toIntOrNull() ?: return null

                return LastDay(year, month, day)
            }

            return null
        }

        fun create(): LastDay {
            val time = LocalDateTime.now()
            return LastDay(time.year, time.monthValue, time.dayOfMonth)
        }
    }
}
