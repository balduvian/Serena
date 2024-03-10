package org.balduvian.timing

import java.time.LocalTime

sealed class EventResult
data object Complete : EventResult()
data object Fail : EventResult()

abstract class DailyEvent(val time: LocalTime, val maxTries: Int) {
	abstract fun onEvent(): EventResult

	fun getId() = this::class.simpleName!!
}
