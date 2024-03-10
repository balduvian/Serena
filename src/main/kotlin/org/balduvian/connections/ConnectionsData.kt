package org.balduvian.connections

import org.balduvian.createStorage
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.io.path.Path

data class ConnectionsData(var days: ArrayList<SubmitDay>) {
	data class SubmitDay(val day: LocalDate, val submittedIds: HashSet<Long>, var complete: Boolean)

	fun getSubmitDay(day: LocalDate): SubmitDay? {
		return days.find { it.day == day }
	}

	fun addSubmittedId(submitDay: SubmitDay, id: Long) {
		submitDay.submittedIds.add(id)
		dataStorage.save(this)
	}

	fun completeDay(submitDay: SubmitDay) {
		submitDay.complete = true
		dataStorage.save(this)
	}

	companion object {
		private const val DATE_TOLERANCE = 5

		private var instance: ConnectionsData? = null

		private val dataStorage = createStorage<ConnectionsData>(
			Path("./connections/data.txt"),
			readData = { file ->
				val lines = file.readLines()
				val days = lines.map { line ->
					val parts = line.split(' ')

					val day = LocalDate.parse(parts[0], DateTimeFormatter.ISO_LOCAL_DATE)
					val submittedIds = if(parts[1] == "") HashSet() else parts[1].split(',').map { it.toLong() }.toHashSet()
					val complete = parts[2] == "true"

					SubmitDay(day, submittedIds, complete)
				} as ArrayList<SubmitDay>
				ConnectionsData(days)
			},
			writeData = { file, connectionData ->
				file.writeText(connectionData.days.joinToString("\n") { (day, submittedIds, complete) ->
					"${day.format(DateTimeFormatter.ISO_LOCAL_DATE)} ${submittedIds.joinToString(",")} $complete"
				})
			},
			createDefault = { ConnectionsData(ArrayList()) }
		)

		fun get(): ConnectionsData {
			val connectionsData = instance ?: dataStorage.load()

			val newDays = ArrayList<SubmitDay>()

			val today = LocalDate.now()

			for (i in -DATE_TOLERANCE..DATE_TOLERANCE) {
				val day = today.plusDays(i.toLong())
				newDays.add(connectionsData.getSubmitDay(day) ?: SubmitDay(day, HashSet(), false))
			}

			connectionsData.days = newDays

			return connectionsData
		}
	}
}
