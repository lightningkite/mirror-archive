package com.lightningkite.mirror.archive.queue

import com.lightningkite.mirror.archive.model.Condition

interface TaskQueue<T : Task> {
    suspend fun insert(tasks: List<T>)
    suspend fun request(condition: Condition<T>): T?
    suspend fun complete(task: T)
}