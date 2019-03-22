package com.lightningkite.mirror.archive.postgres

import io.reactiverse.pgclient.PgClient
import io.reactiverse.pgclient.PgRowSet
import io.reactiverse.pgclient.impl.ArrayTuple
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

suspend fun PgClient.suspendQuery(sql: String) = suspendCoroutine<PgRowSet> { cont ->
    println("Executing... $sql")
    try {
        this.query(sql) {
            if (it.succeeded()) {
                cont.resume(it.result())
            } else {
                cont.resumeWithException(it.cause()!!)
            }
        }
    } catch (t: Throwable) {
        cont.resumeWithException(t)
    }
}

suspend fun PgClient.suspendQuery(sqlBuilder: QueryBuilder) = suspendCoroutine<PgRowSet> { cont ->
    println("Executing... ${sqlBuilder.builder}")
    try {
        this.preparedQuery(sqlBuilder.builder.toString(), sqlBuilder.arguments) {
            if (it.succeeded()) {
                cont.resume(it.result())
            } else {
                cont.resumeWithException(it.cause()!!)
            }
        }
    } catch (t: Throwable) {
        cont.resumeWithException(t)
    }
}

suspend inline fun PgClient.suspendQuery(sqlBuilderAction: QueryBuilder.() -> Unit) = suspendQuery(QueryBuilder().apply(sqlBuilderAction))