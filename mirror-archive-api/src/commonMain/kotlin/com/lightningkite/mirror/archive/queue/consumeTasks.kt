package com.lightningkite.mirror.archive.queue

import com.lightningkite.kommon.Closeable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun SuspendQueue<Task>.consumeTasks(): Closeable {
    var open = true
    GlobalScope.launch(Dispatchers.Default) {
        while(open) {
            val next = dequeue()
            if(next == null) {
                delay(15_000L)
                continue
            }
            try {
                next.second.run()
            } catch(e:Throwable) {
                enqueue(next.second)
            }
        }
    }
    return object : Closeable {
        override fun close() {
            open = false
        }
    }
}