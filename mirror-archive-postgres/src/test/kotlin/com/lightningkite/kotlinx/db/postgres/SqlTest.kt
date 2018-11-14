package com.lightningkite.kotlinx.db.postgres

import com.lightningkite.kotlin.crossplatform.kotlinxDbPostgresTestReflections
import com.lightningkite.kotlinx.reflection.kxReflect
import com.lightningkite.kotlinx.serialization.CommonSerialization
import org.junit.Test


class SqlTest {

    init {
        kotlinxDbPostgresTestReflections.forEach {
            CommonSerialization.ExternalNames.register(it)
        }
    }

    val serializer = PostgresSerializer()

    @Test
    fun test() {
        val table = serializer.table(Post::class.kxReflect)
        println(table)
        println(table.toCreateSql())
    }

    @Test
    fun testMigrate() {
        val old = Table(
                schemaName = "public",
                name = "Post",
                columns = listOf(
                        Column(name = "body", type = "TEXT", size = null),
                        Column(name = "id", type = "BIGSERIAL", size = null),
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
        val table = serializer.table(Post::class.kxReflect)
        println(table)
        println(table.toCreateSql())
        println(table.toMigrateSql(old))
    }
}