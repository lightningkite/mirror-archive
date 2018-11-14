package com.lightningkite.kotlinx.db.postgres

import com.lightningkite.kotlinx.persistence.Transaction
import io.reactiverse.pgclient.PgClient
import io.reactiverse.pgclient.PgPool
import io.reactiverse.pgclient.PgRowSet
import io.reactiverse.pgclient.PgTransaction
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

val Transaction_pg = WeakHashMap<PgPool, WeakHashMap<Transaction, PgClient>>()
suspend fun Transaction.pg(pool: PgPool): PgClient {
    return Transaction_pg.getOrPut(pool) { WeakHashMap() }.getOrPut(this) {
        if(readOnly || !atomic){
            pool
        } else {
            val txn = suspendCoroutine<PgTransaction> { cont ->
                pool.begin {
                    if(it.succeeded()){
                        cont.resume(it.result())
                    } else {
                        cont.resumeWithException(it.cause())
                    }
                }
            }
            onCommit += {
                suspendCoroutine { cont ->
                    txn.commit{
                        cont.resume(Unit)
                    }
                }
            }
            onFail += {
                suspendCoroutine { cont ->
                    txn.rollback{
                        cont.resume(Unit)
                    }
                }
            }
            txn
        }
    }
}

suspend fun PgClient.suspendQuery(sql: String) = suspendCoroutine<PgRowSet> { cont ->
//    println("Executing... $sql")
    this.query(sql){
        if(it.succeeded()){
            cont.resume(it.result())
        } else {
            cont.resumeWithException(it.cause()!!)
        }
    }
}