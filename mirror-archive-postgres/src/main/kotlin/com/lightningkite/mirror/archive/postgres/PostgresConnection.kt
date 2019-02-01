//package com.lightningkite.mirror.archive.postgres
//
//import com.lightningkite.lokalize.*
//import com.lightningkite.mirror.archive.model.Id
//import com.lightningkite.mirror.archive.sql.*
//import io.reactiverse.pgclient.PgClient
//import io.reactiverse.pgclient.PgPool
//import io.reactiverse.pgclient.PgRowSet
//import kotlin.coroutines.resume
//import kotlin.coroutines.resumeWithException
//import kotlin.coroutines.suspendCoroutine
//
//class PostgresConnection(val client: PgPool) : SQLConnection {
//    override suspend fun execute(sql: SQLQuery): Sequence<List<Any?>> = suspendCoroutine { cont ->
//        println("SQL: ${sql.string}")
//        client.query(sql.string) {
//            if (it.succeeded()) {
//                cont.resume(it.result().asSequence().map {
//                    val list = ArrayList<Any?>(it.size())
//                    for (i in 0 until it.size()) {
//                        val raw = it.getValue(i)
//                        val processed = when (raw) {
//                            is java.time.LocalDate -> Date(raw.toEpochDay().toInt())
//                            is java.time.LocalTime -> Time(raw.toNanoOfDay().div(1000000).toInt())
//                            is java.time.LocalDateTime -> raw.let {
//                                DateTime(
//                                        Date(it.toLocalDate().toEpochDay().toInt()),
//                                        Time(it.toLocalTime().toNanoOfDay().div(1000000).toInt())
//                                )
//                            }
//                            is java.util.UUID -> Id(raw.mostSignificantBits, raw.leastSignificantBits)
//                            else -> raw
//                        }
//                        list.add(processed)
//                    }
//                    list
//                })
//            } else {
//                cont.resumeWithException(it.cause()!!)
//            }
//        }
//    }
//
//    override suspend fun executeBatch(sql: List<SQLQuery>): Sequence<List<Any?>> = suspendCoroutine { cont ->
//        client.begin {
//            if(it.succeeded()){
//                val txn = it.result()
//                for(other in sql.subList(0, sql.lastIndex)) {
//                    println("SQL: ${other.string}")
//                    txn.query(other.string) {
//                        if(it.failed()){
//                            cont.resumeWithException(it.cause()!!)
//                        }
//                    }
//                }
//                txn.query(sql.last().string) {
//                    println("SQL: ${sql.last().string}")
//                    if (it.succeeded()) {
//                        cont.resume(it.result().asSequence().map {
//                            val list = ArrayList<Any?>(it.size())
//                            for (i in 0 until it.size()) {
//                                val raw = it.getValue(i)
//                                val processed = when (raw) {
//                                    is java.time.LocalDate -> Date(raw.toEpochDay().toInt())
//                                    is java.time.LocalTime -> Time(raw.toNanoOfDay().div(1000000).toInt())
//                                    is java.time.LocalDateTime -> raw.let {
//                                        DateTime(
//                                                Date(it.toLocalDate().toEpochDay().toInt()),
//                                                Time(it.toLocalTime().toNanoOfDay().div(1000000).toInt())
//                                        )
//                                    }
//                                    is java.util.UUID -> Id(raw.mostSignificantBits, raw.leastSignificantBits)
//                                    else -> raw
//                                }
//                                list.add(processed)
//                            }
//                            list
//                        })
//                    } else {
//                        cont.resumeWithException(it.cause()!!)
//                    }
//                }
//                txn.commit()
//            }
//        }
//    }
//
//    override suspend fun reflect(schema: String, table: String): Table? {
//        val columns = client.suspendQuery("""
//        SELECT
//        columns.column_name,
//        columns.data_type,
//        columns.character_maximum_length
//        FROM information_schema.columns as columns
//        WHERE columns.table_schema = '${schema.toLowerCase()}' AND columns.table_name = '${table.toLowerCase()}';
//        """.trimIndent()
//        ).map {
//            Column(
//                    name = it.getString("column_name"),
//                    type = it.getString("data_type"),
//                    size = it.getInteger("character_maximum_length")
//            )
//        }
//        if(columns.isEmpty()) return null
//        val constraints = client.suspendQuery("""
//        SELECT
//        usage.constraint_name,
//        usage.column_name,
//        constraints.constraint_type,
//        target.column_name as target_column,
//        target.table_name as target_table,
//        target.table_schema as target_schema
//        FROM information_schema.constraint_column_usage as usage
//        LEFT JOIN information_schema.table_constraints as constraints ON constraints.constraint_name = usage.constraint_name
//        LEFT JOIN information_schema.constraint_column_usage as target ON target.constraint_name = constraints.constraint_name
//        WHERE usage.table_schema = '${schema.toLowerCase()}' AND usage.table_name = '${table.toLowerCase()}';
//        """.trimIndent()
//        )
//                .asSequence()
//                .mapNotNull {
//                    Constraint(
//                            name = it.getString("constraint_name"),
//                            type = it.getString("constraint_type").let { type ->
//                                when (type) {
//                                    "PRIMARY KEY" -> Constraint.Type.PrimaryKey
//                                    "FOREIGN KEY" -> Constraint.Type.ForeignKey(
//                                            otherSchema = it.getString("target_schema"),
//                                            otherTable = it.getString("target_table"),
//                                            otherColumns = listOf(it.getString("target_column"))
//                                    )
//                                    "UNIQUE" -> Constraint.Type.Unique
//                                    else -> return@mapNotNull null
//                                }
//                            },
//                            columns = listOf(it.getString("column_name"))
//                    )
//                }
//                .groupBy { it.name }
//                .values
//                .map {
//                    val first = it.first()
//                    val firstType = first.type
//                    first.copy(
//                            columns = it.map { it.columns.first() },
//                            type = when(firstType) {
//                                Constraint.Type.PrimaryKey -> Constraint.Type.PrimaryKey
//                                Constraint.Type.Unique -> Constraint.Type.Unique
//                                is Constraint.Type.ForeignKey -> firstType.copy(
//                                        otherColumns = it.map { it.type.let{ it as Constraint.Type.ForeignKey }.otherColumns.first() }
//                                )
//                            }
//                    )
//                }
//
//        val indexes = client.suspendQuery("""
//        SELECT indexdef
//        FROM pg_indexes
//        WHERE pg_indexes.schemaname = '${schema.toLowerCase()}' AND pg_indexes.tablename = '${table.toLowerCase()}'
//    """.trimIndent())
//                .mapNotNull { Index.parse(it.getString(0)) }
//
//        //Check columns for serial
//        columns.find { it.name == "id" }?.let{
//            if(it.type.endsWith("int", true)){
//                it.type = it.type.toLowerCase().removeSuffix("int").plus("serial")
//            }
//        }
//
//        return Table(
//                schemaName = schema,
//                name = table,
//                columns = columns,
//                constraints = constraints,
//                indexes = indexes
//        )
//    }
//}