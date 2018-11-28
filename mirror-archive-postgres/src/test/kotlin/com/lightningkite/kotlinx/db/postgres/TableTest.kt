package com.lightningkite.kotlinx.db.postgres

import com.lightningkite.mirror.archive.postgres.Column
import com.lightningkite.mirror.archive.postgres.Constraint
import com.lightningkite.mirror.archive.postgres.PostgresSerializer
import com.lightningkite.mirror.archive.postgres.Table
import com.lightningkite.mirror.serialization.DefaultRegistry
import org.junit.Test


class TableTest {

    val serializer = PostgresSerializer(registry = DefaultRegistry + TestRegistry)

    @Test
    fun test() {
        val table = serializer.table(PostClassInfo)
        println(table)
        println(table.toCreateSql())
    }

    @Test
    fun testMigrate() {
        val old = Table(
                schemaName = "public",
                name = "Post",
                columns = listOf(
                        Column(name = "id", type = "UUID", size = null),
                        Column(name = "body", type = "TEXT", size = null),
                        Column(name = "title", type = "TEXT", size = null),
                        Column(name = "userId", type = "BIGINT", size = null)
                ),
                constraints = listOf(Constraint(
                        type = Constraint.Type.PrimaryKey,
                        columns = listOf("id"),
                        otherTable = null,
                        otherColumns = listOf("id"),
                        name = "id_PrimaryKey"
                )),
                indexes = listOf()
        )
        val table = serializer.table(PostClassInfo)
        println(table)
        println(table.toCreateSql())
        println(table.toMigrateSql(old))
    }
}