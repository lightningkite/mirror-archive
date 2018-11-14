package com.lightningkite.mirror.archive


/**
 * Represents a database transaction
 * Created by josep on 7/12/2017.
 */
class Transaction(
        var untypedUser: Any? = null,
        val atomic: Boolean = false,
        val readOnly: Boolean = false
) {
    val cache = HashMap<Any, Any?>()
    val onCommit = ArrayList<suspend () -> Unit>()
    val onFail = ArrayList<suspend () -> Unit>()
    var finished = false

    suspend fun commit() {
        if (finished) return
        finished = true
        var commitFail: Throwable? = null
        onCommit.forEach {
            try {
                it.invoke()
            } catch(e: Throwable){
                commitFail = e
            }
        }
        if(commitFail != null) throw commitFail!!
    }

    suspend fun fail() {
        if (finished) return
        finished = true
        var commitFail: Throwable? = null
        onFail.forEach {
            try {
                it.invoke()
            } catch(e: Throwable){
                commitFail = e
            }
        }
        if(commitFail != null) throw commitFail!!
    }
}

suspend inline fun <T> Transaction.use(action: (Transaction) -> T): T = try {
    val result = action.invoke(this)
    commit()
    result
} finally {
    fail()
}