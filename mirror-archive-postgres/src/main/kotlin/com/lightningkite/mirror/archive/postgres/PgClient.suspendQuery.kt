package com.lightningkite.mirror.archive.postgres

import com.lightningkite.mirror.archive.sql.SQLQuery
import io.reactiverse.pgclient.PgClient
import io.reactiverse.pgclient.PgRowSet
import io.reactiverse.pgclient.impl.ArrayTuple
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

suspend fun PgClient.suspendQuery(sql: SQLQuery) = suspendCoroutine<PgRowSet> { cont ->
    println("Executing... $sql")
    this.preparedQuery(sql.sql, ArrayTuple(sql.arguments)) { result ->
        if(result.failed()){
            cont.resumeWithException(result.cause())
        } else {
            cont.resume(result.result())
        }
    }
}