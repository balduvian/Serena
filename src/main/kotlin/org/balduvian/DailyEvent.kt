package org.balduvian

import java.time.Duration

sealed class EventResult
object Done : EventResult()
class Reschedule(val span: Duration) : EventResult()

abstract class DailyEvent<Props>(val hour: Int) {
	abstract fun onEvent(currentTry: Int, props: Props): EventResult

	abstract fun createDefaultProps(): Props

	fun getId(): String {
		return this::class.simpleName ?: throw Error("event has no id")
	}
}
