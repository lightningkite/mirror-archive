package com.lightningkite.mirror.archive.postgres

import io.reactiverse.pgclient.PgClient
import io.reactiverse.pgclient.PgPool
import io.reactiverse.pgclient.PgRowSet
import io.reactiverse.pgclient.PgTransaction
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

suspend fun PgClient.suspendQuery(sql: String) = suspendCoroutine<PgRowSet> { cont ->
    println("Executing... $sql")
    this.query(sql){
        if(it.succeeded()){
            cont.resume(it.result())
        } else {
            cont.resumeWithException(it.cause()!!)
        }
    }
}