package org.balduvian

sealed class EventResult
data object Complete : EventResult()
data object Fail : EventResult()

abstract class DailyEvent(val hour: Int, val maxTries: Int) {
	abstract fun onEvent(): EventResult

	fun getId() = this::class.simpleName!!
}
