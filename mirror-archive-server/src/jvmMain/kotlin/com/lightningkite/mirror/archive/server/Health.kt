package com.lightningkite.mirror.archive.server

import com.lightningkite.lokalize.time.Duration
import com.lightningkite.lokalize.time.TimeStamp
import com.lightningkite.lokalize.time.now
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory


object Health {

    val logger = LoggerFactory.getLogger(this::class.java)

    val URGENCY_IMMEDIATE = Duration.zero
    val URGENCY_TODAY = Duration.days(1)
    val URGENCY_THIS_WEEK = Duration.days(7)
    val URGENCY_NONE = Duration(Long.MAX_VALUE)

    data class Status(
            val urgency: Duration = URGENCY_NONE,
            val message: String = ""
    )

    data class Check(
            val name: String,
            var lastMessageSend: TimeStamp = TimeStamp(0),
            var status: Status = Status()
    ) {
        fun shouldSend(): Boolean = TimeStamp.now() > lastMessageSend + status.urgency
    }

    val checks = HashMap<String, Check>()

    var sendMethod: (Check)->Unit = {
        if(it.status.urgency <= URGENCY_TODAY){
            logger.error("${it.name}: ${it.status.message}")
        } else if(it.status.urgency <= URGENCY_THIS_WEEK){
            logger.warn("${it.name}: ${it.status.message}")
        } else {
            logger.info("${it.name}: ${it.status.message}")
        }
    }

    fun healthCheck(
            name: String,
            frequency: Duration = Duration.hours(1),
            maxStagger: Duration = Duration.minutes(5),
            action: suspend ()->Status
    ) {
        val check = Check(name = name)
        GlobalScope.launch(Dispatchers.Default) {
            while(true){
                delay((0L .. maxStagger.milliseconds).random())
                val result = action()
                check.status = result
                if(check.shouldSend()) {
                    sendMethod(check)
                    check.lastMessageSend = TimeStamp.now()
                }
                delay(frequency.milliseconds)
            }
        }
    }
}
