package com.lightningkite.kotlinx.db.postgres

import com.lightningkite.mirror.archive.database.insert
import com.lightningkite.mirror.archive.model.Condition
import com.lightningkite.mirror.archive.model.Id
import com.lightningkite.mirror.archive.model.Operation
import com.lightningkite.mirror.archive.postgres.*
import com.lightningkite.mirror.archive.sql.SQLSuspendMap
import com.lightningkite.mirror.info.FieldInfo
import com.lightningkite.mirror.info.type
import com.lightningkite.mirror.serialization.DefaultRegistry
import com.lightningkite.mirror.serialization.json.JsonSerializer
import io.reactiverse.pgclient.PgClient
import io.reactiverse.pgclient.PgConnectOptions
import io.reactiverse.pgclient.PgPool
import io.reactiverse.pgclient.PgPoolOptions
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.io.File
import kotlin.test.*

class FullTest {

    val poolProvider = EmbeddedPG.PoolProvider(
            cache = File("/postgresql-cache"),
            version = EmbeddedPG.Versions.VERSION_10,
            storeFiles = File("./build/run/files").also { it.deleteRecursively() }
    )
    lateinit var pool: PgPool

    @BeforeTest
    fun before() {
        println("Starting...")
        pool = poolProvider.start()
    }

    @AfterTest
    fun after() {
        println("Stopping...")
        poolProvider.stop()
    }

    @Test
    fun testAll() {
        runBlocking {
            //Set up the database
            val provider = SQLSuspendMap.Provider(
                    serializer = PostgresSerializer(DefaultRegistry + TestRegistry),
                    connection = PostgresConnection(pool)
            )

            val post1 = Post(
                    userId = 0,
                    title = "Title",
                    body = "This is a test post."
            )
            val post2 = Post(
                    userId = 1,
                    title = "Other Post",
                    body = "This is a different test post."
            )

            //Set up the table
            val table = provider.suspendMap(Id::class.type, Post::class.type)
            table.insert(post1)
            table.insert(post2)

            val getResult = table.get(post1.id)
            println("Get result: $getResult")
            assertEquals(post1, getResult)

            val fullQueryResult = table.query()
            println("Full Query result: $fullQueryResult")
            assertTrue(fullQueryResult.any { it.second == post1 })
            assertTrue(fullQueryResult.any { it.second == post2 })

            val page1QueryResult = table.query(count = 1)
            println("Page 1 Query Result result: $page1QueryResult")
            val page2QueryResult = table.query(count = 1, after = page1QueryResult[0])
            println("Page 2 Query Result result: $page2QueryResult")
            val totalPagedResults = page1QueryResult + page2QueryResult

            assertTrue(totalPagedResults.any { it.second == post1 })
            assertTrue(totalPagedResults.any { it.second == post2 })

            val updateResult = table.insert(post1.copy(userId = 1))
            println("Update result: $updateResult")

            val modifyResult = table.modify(post1.id, Operation.Fields(
                    classInfo = PostClassInfo,
                    changes = mapOf<FieldInfo<Post, *>, Operation<*>>(
                            PostClassInfo.fieldTitle to Operation.Set("Test Post")
                    )
            ))
            println("Modify result: $modifyResult")

            table.remove(post1.id)
        }

    }
}
