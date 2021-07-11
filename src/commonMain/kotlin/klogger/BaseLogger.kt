package klogger

import klogger.context.LogContext
import klogger.events.Level
import klogger.events.LogEvent
import klogger.events.newId
import klogger.events.now
import kotlin.coroutines.coroutineContext

class BaseLogger(
    override val name: String,
    private val minLevel: Level = Level.INFO,
) : Klogger {

    override fun minLevel() = minLevel

    override suspend fun logMessage(level: Level, exception: Exception?, event: Any) {
        val eventToLog = when (event) {
            is LogEvent -> event
            else -> {
                val (message, stackTrace) = when (event) {
                    is Exception -> (event.message ?: "Exception") to event.stackTraceToString()
                    else -> event.toString() to exception?.stackTraceToString()
                }
                LogEvent(
                    id = newId(),
                    timestamp = now(),
                    logger = name,
                    level = level,
                    message = message,
                    stackTrace = stackTrace,
                    items = contextItems()
                )
            }
        }

        Logging.sendEvent(eventToLog)
    }

    private suspend inline fun contextItems() =
        coroutineContext[LogContext]?.getAll() ?: mapOf()

    override suspend fun e(template: String, vararg items: Any): LogEvent {
        return LogEvent(
            id = newId(),
            timestamp = now(),
            logger = name,
            level = minLevel,
            message = template,
            stackTrace = null,
            items = contextItems(),
        )
    }
}
