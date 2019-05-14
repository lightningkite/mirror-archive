package com.lightningkite.mirror.archive.server

import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.Timer
import com.lightningkite.mirror.Metrics
import com.lightningkite.mirror.archive.database.Database
import com.lightningkite.mirror.archive.model.Condition
import com.lightningkite.mirror.archive.model.Operation
import com.lightningkite.mirror.archive.model.Sort
import com.lightningkite.mirror.timeInline
import java.util.concurrent.TimeUnit

class DatabaseWithMetrics<T: Any>(val underlying: Database<T>, val name: String, val metrics: MetricRegistry = Metrics): Database<T> {

    val getTimer = metrics.timer(name + ".get")
    override suspend fun get(condition: Condition<T>, sort: List<Sort<T, *>>, count: Int, after: T?): List<T> = getTimer.timeInline {
        underlying.get(condition, sort, count, after)
    }

    val insertTimer = metrics.timer(name + ".insert")
    override suspend fun insert(values: List<T>): List<T> = insertTimer.timeInline {
        underlying.insert(values)
    }

    val updateTimer = metrics.timer(name + ".update")
    override suspend fun update(condition: Condition<T>, operation: Operation<T>): Int = updateTimer.timeInline {
        underlying.update(condition, operation)
    }

    val limitedUpdateTimer = metrics.timer(name + ".limitedUpdate")
    override suspend fun limitedUpdate(condition: Condition<T>, operation: Operation<T>, sort: List<Sort<T, *>>, limit: Int): Int = limitedUpdateTimer.timeInline {
        underlying.limitedUpdate(condition, operation, sort, limit)
    }

    val deleteTimer = metrics.timer(name + ".delete")
    override suspend fun delete(condition: Condition<T>): Int = deleteTimer.timeInline {
        underlying.delete(condition)
    }
}
fun <T: Any> Database<T>.withMetrics(name: String, metrics: MetricRegistry = Metrics) = DatabaseWithMetrics(this, name, metrics)
