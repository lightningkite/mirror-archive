package com.lightningkite.kotlinx.db.postgres

import com.lightningkite.mirror.archive.ModificationOnItem
import com.lightningkite.mirror.archive.postgres.*
import com.lightningkite.mirror.serialization.DefaultRegistry
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
    fun postTest() {
        runBlocking {
            //Set up the database
            val db = PostgresDatabase(pool, DefaultRegistry + TestRegistry)

            //Set up the table
            val table = db.table(Post::class)
            val insertResult = table.insert(Post(
                    userId = 0,
                    title = "Title",
                    body = "This is a test post."
            ))
            println("Insert result: $insertResult")

            val insert2Result = table.insert(Post(
                    userId = 1,
                    title = "Other Post",
                    body = "This is a different test post."
            ))
            println("Insert2 result: $insert2Result")

            val getResult = table.get(insertResult.id)
            println("Get result: $getResult")
            assertEquals(insertResult, getResult)

            val fullQueryResult = table.query()
            println("Full Query result: $fullQueryResult")
            assertEquals(insertResult, fullQueryResult.results[0])
            assertEquals(insert2Result, fullQueryResult.results[1])

            val page1QueryResult = table.query(count = 1)
            println("Page 1 Query Result result: $page1QueryResult")
            assertEquals(insertResult, page1QueryResult.results[0])

            val page2QueryResult = table.query(count = 1, continuationToken = page1QueryResult.continuationToken)
            println("Page 2 Query Result result: $page2QueryResult")
            assertEquals(insert2Result, page2QueryResult.results[0])

            val updateResult = table.update(insertResult.copy(userId = 1))
            println("Update result: $updateResult")

            val modifyResult = table.modify(insertResult.id, listOf(
                    ModificationOnItem.Set(PostClassInfo.fieldTitle, "Test Post")
            ))
            println("Modify result: $modifyResult")

            table.delete(insertResult.id)
        }

    }
}
