package com.lightningkite.mirror.archive.queue

import com.lightningkite.lokalize.time.Duration
import com.lightningkite.lokalize.time.TimeStamp
import com.lightningkite.lokalize.time.now
import com.lightningkite.mirror.archive.database.Database
import com.lightningkite.mirror.archive.model.*
import com.lightningkite.mirror.info.MirrorClass

class DatabaseTaskQueue<T : Task>(
        val type: MirrorClass<T>,
        val database: Database<T>,
        val myIdentifier: Int,
        val timeout: Duration = Duration.minutes(15)
) : TaskQueue<T> {

    @Suppress("UNCHECKED_CAST")
    val idField = type.fields[type.fieldsIndex["id"]!!] as MirrorClass.Field<T, Uuid>
    @Suppress("UNCHECKED_CAST")
    val claimedByField = type.fields[type.fieldsIndex["claimedBy"]!!] as MirrorClass.Field<T, Int>
    @Suppress("UNCHECKED_CAST")
    val claimedAtField = type.fields[type.fieldsIndex["claimedAt"]!!] as MirrorClass.Field<T, TimeStamp>

    override suspend fun insert(tasks: List<T>) {
        database.insert(tasks)
    }

    override suspend fun request(condition: Condition<T>): T? {
        val now = TimeStamp.now()
        val tooOld = now - timeout
        val modified = database.update(
                condition = condition and (claimedAtField lessThan tooOld),
                operation = (claimedAtField setTo now) and (claimedByField setTo myIdentifier),
                limit = 1
        )
        if (modified == 0) return null
        return database.get(
                condition = (claimedByField equal myIdentifier) and (claimedAtField equal now),
                count = 1
        ).firstOrNull()
    }

    override suspend fun complete(task: T) {
        database.delete(idField equal task.id)
    }
}