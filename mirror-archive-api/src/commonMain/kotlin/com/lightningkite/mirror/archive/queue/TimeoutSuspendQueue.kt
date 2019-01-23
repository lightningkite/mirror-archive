package com.lightningkite.mirror.archive.queue

import com.lightningkite.lokalize.time.DateTime
import com.lightningkite.lokalize.time.Duration
import com.lightningkite.mirror.archive.database.SuspendMap
import com.lightningkite.mirror.archive.database.SuspendMapProvider
import com.lightningkite.mirror.archive.model.*
import com.lightningkite.mirror.info.*

//class TimeoutSuspendQueue(
//        val backingMap: SuspendMap<Id, ScheduledTask>,
//        val dateTimeMarkerMap: SuspendMap<String, DateTime>
//) {
//    fun enqueue(scheduledTask: ScheduledTask): Id {
//        ba
//    }
//}
//
//suspend fun SuspendMap<Id, ScheduledTask>.dequeue(scheduledForFieldInfo: FieldInfo<ScheduledTask, DateTime>) {
//    this.find(sortedBy = Sort.Field(scheduledForFieldInfo, true))
//}
//
//data class ScheduledTask(
//        var task: Task,
//        @Indexed var scheduledFor: DateTime,
//        var timeout: Duration = Duration.seconds(30),
//        var attempts: Int = 0,
//        var tries: Int = 0,
//        var lastError: String? = null
//)

/*
Schedule tasks for a particular time
On consumption, move back that time
On completion, delete - optionally dump to other table

Queue underlying
Copy ready tasks into the queue regularly
Consumers dequeue, execute, and report completion back to main

 */