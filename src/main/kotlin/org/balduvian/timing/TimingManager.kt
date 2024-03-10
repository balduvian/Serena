package org.balduvian.timing

import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class TimingManager {
	private fun nextTime(days: Long, hour: Int): LocalDateTime {
		return LocalDateTime.now().withHour(hour).withMinute(0).withSecond(0).plusDays(days)
	}

	private val RETRY_TIME = Duration.ofMinutes(1)

	data class ScheduledEvent(
		val time: LocalDateTime,
		val isDaily: Boolean,
		val dailyEvent: DailyEvent,
		val currentTry: Int,
	) {
		companion object {
			fun dailyEvent(time: LocalDateTime, dailyEvent: DailyEvent) = ScheduledEvent(time, true, dailyEvent, 0)

			fun interruptEvent(dailyEvent: DailyEvent, currentTry: Int) = ScheduledEvent(LocalDateTime.now(), false, dailyEvent, currentTry)
		}

		override fun toString(): String {
			return "Event (${if (isDaily) "daily" else "interrupt" })"
		}

		fun withTime(time: LocalDateTime) = ScheduledEvent(time, isDaily, dailyEvent, 0)
	}

	private val dailyEvents = ArrayList<DailyEvent>()
	private val eventQueue = ArrayList<ScheduledEvent>()

	fun registerDailyEvent(dailyEvent: DailyEvent) {
		dailyEvents.add(dailyEvent)
	}

	private fun getTimeTomorrow(event: ScheduledEvent): LocalDateTime {
		return event.time.plusDays(1L).withMinute(0).withSecond(0).withHour(event.dailyEvent.hour)
	}

	fun executeEvent(event: ScheduledEvent): ScheduledEvent? {
		val dailyEvent = event.dailyEvent

		val result = try {
			dailyEvent.onEvent()
		} catch (ex: Throwable) {
			ex.printStackTrace()
			Fail
		}

		val nextTry = event.currentTry + 1
		val eventTomorrow = if (event.isDaily) ScheduledEvent(getTimeTomorrow(event), true, dailyEvent, 0) else null
		val eventRetry = ScheduledEvent(event.time.plus(RETRY_TIME), event.isDaily, dailyEvent, nextTry)

		return if (result is Complete) {
			eventTomorrow
		} else {
			if (nextTry < dailyEvent.maxTries) {
				eventTomorrow
			} else {
				eventRetry
			}
		}
	}

	fun timingLoop() {
		while (true) {
			val now = LocalDateTime.now()

			val queuedEvents = ArrayList<ScheduledEvent>()

			eventQueue.removeIf { event ->
				if (ChronoUnit.SECONDS.between(now, event.time) <= 0) {
					executeEvent(event)?.let { queuedEvents.add(it) }
					true
				} else {
					false
				}
			}

			eventQueue.addAll(queuedEvents)

			Thread.sleep(ChronoUnit.SECONDS.duration.toMillis())
		}
	}
}
