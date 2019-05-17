package com.lightningkite.mirror.archive.postgres

import io.reactiverse.pgclient.PgClient
import io.reactiverse.pgclient.PgRowSet
import io.reactiverse.pgclient.impl.ArrayTuple
import org.slf4j.LoggerFactory
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
    LoggerFactory.getLogger("PgClient.suspendQuery").debug("Starting send for query: ${sqlBuilder.builder}")
    try {
        this.preparedQuery(sqlBuilder.builder.toString(), sqlBuilder.arguments) {
            try {
                if (it.succeeded()) {
                    cont.resume(it.result())
                } else {
                    LoggerFactory.getLogger("PgClient.suspendQuery").debug("Failed for query: ${sqlBuilder.builder}")
                    cont.resumeWithException(it.cause()!!)
                }
            } catch (t: Throwable) {
                LoggerFactory.getLogger("PgClient.suspendQuery").debug("Failed for query: ${sqlBuilder.builder}")
                cont.resumeWithException(t)
            }
        }
    } catch (t: Throwable) {
        LoggerFactory.getLogger("PgClient.suspendQuery").debug("Failed for query: ${sqlBuilder.builder}")
        cont.resumeWithException(t)
    }
    LoggerFactory.getLogger("PgClient.suspendQuery").debug("Sent query: ${sqlBuilder.builder}")
}

suspend inline fun PgClient.suspendQuery(sqlBuilderAction: QueryBuilder.() -> Unit) = suspendQuery(QueryBuilder().apply(sqlBuilderAction))