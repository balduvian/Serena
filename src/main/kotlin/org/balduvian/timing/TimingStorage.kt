package org.balduvian.timing

import org.balduvian.createStorage
import java.time.LocalDate
import java.util.HashSet
import kotlin.io.path.Path

object TimingStorage {
	private data class RecordEntry(val date: LocalDate, val completedEvents: HashSet<String>)
	private data class Record(val entries: ArrayList<RecordEntry>)

	private val storage = createStorage(Path("./timing/record.txt"),
		readData = {},
		writeData = {},
		createDefault = {}
	)
}