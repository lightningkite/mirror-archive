package com.lightningkite.kotlinx.db.redis

import com.lightningkite.mirror.archive.database.insert
import com.lightningkite.mirror.archive.model.Id
import com.lightningkite.mirror.archive.model.Operation
import com.lightningkite.mirror.archive.redis.*
import com.lightningkite.mirror.archive.sql.SQLSuspendMap
import com.lightningkite.mirror.info.FieldInfo
import com.lightningkite.mirror.info.type
import com.lightningkite.mirror.serialization.DefaultRegistry
import com.lightningkite.mirror.serialization.json.JsonSerializer
import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.io.File
import kotlin.test.*

class FullTest {

    var connection: StatefulRedisConnection<String, String>? = null

    @BeforeTest
    fun before() {
        println("Starting...")
        val settings = EmbeddedRedis.start()
        println("Redis started...")
        connection = settings.connect()
        println("Redis connected successfully.")
    }

    @AfterTest
    fun after() {
        println("Stopping...")
        connection?.close()
        connection = null
        EmbeddedRedis.stop()
    }

    @Test fun connects(){
        connection?.sync()?.set("key", "value")
        println("Set successfully!")
    }

    @Test fun basic(){
        runBlocking {
            val provider = RedisSuspendMap.Provider(
                    connection = connection!!,
                    serializer = RedisSerializer(DefaultRegistry + TestRegistry)
            )
            val simple = provider.suspendMap(String::class.type, Int::class.type)

            simple.put("setting", 32)
            simple.put("answer", 42)
            assertEquals(32, simple.get("setting"))
            assertEquals(33, simple.modify("setting", Operation.AddInt(1)))
            assertEquals(33, simple.get("setting"))
            simple.remove("setting")
            assertEquals(null, simple.get("setting"))
        }
    }
//
//    @Test
//    fun testSimpleKV() {
//        runBlocking {
//            //Set up the database
//            val provider = SQLSuspendMap.Provider(
//                    serializer = PostgresSerializer(DefaultRegistry + TestRegistry),
//                    connection = PostgresConnection(pool)
//            )
//
//            val simple = provider.suspendMap(String::class.type, Int::class.type)
//
//            simple.put("setting", 32)
//            simple.put("answer", 42)
//            assert(simple.query().contains("setting" to 32))
//            assert(simple.query().contains("answer" to 42))
//            assertEquals(32, simple.get("setting"))
//            assertEquals(33, simple.modify("setting", Operation.AddInt(1)))
//            assert(simple.query().contains("setting" to 33))
//            assert(simple.query().contains("answer" to 42))
//            assertEquals(33, simple.get("setting"))
//            simple.remove("setting")
//            assertFalse(simple.query().any { it.first == "setting" })
//            assert(simple.query().contains("answer" to 42))
//            assertEquals(null, simple.get("setting"))
//        }
//    }
//
//    @Test
//    fun testAll() {
//        runBlocking {
//            //Set up the database
//            val provider = SQLSuspendMap.Provider(
//                    serializer = PostgresSerializer(DefaultRegistry + TestRegistry),
//                    connection = PostgresConnection(pool)
//            )
//
//            val post1 = Post(
//                    userId = 0,
//                    title = "Title",
//                    body = "This is a test post."
//            )
//            val post2 = Post(
//                    userId = 1,
//                    title = "Other Post",
//                    body = "This is a different test post."
//            )
//
//            //Set up the table
//            val table = provider.suspendMap(Id::class.type, Post::class.type)
//            table.insert(post1)
//            table.insert(post2)
//
//            val getResult = table.get(post1.id)
//            println("Get result: $getResult")
//            assertEquals(post1, getResult)
//
//            val fullQueryResult = table.query()
//            println("Full Query result: $fullQueryResult")
//            assertTrue(fullQueryResult.any { it.second == post1 })
//            assertTrue(fullQueryResult.any { it.second == post2 })
//
//            val page1QueryResult = table.query(count = 1)
//            println("Page 1 Query Result result: $page1QueryResult")
//            val page2QueryResult = table.query(count = 1, after = page1QueryResult[0])
//            println("Page 2 Query Result result: $page2QueryResult")
//            val totalPagedResults = page1QueryResult + page2QueryResult
//
//            assertTrue(totalPagedResults.any { it.second == post1 })
//            assertTrue(totalPagedResults.any { it.second == post2 })
//
//            val updateResult = table.insert(post1.copy(userId = 1))
//            println("Update result: $updateResult")
//
//            val modifyResult = table.modify(post1.id, Operation.Fields(
//                    classInfo = PostClassInfo,
//                    changes = mapOf<FieldInfo<Post, *>, Operation<*>>(
//                            PostClassInfo.fieldTitle to Operation.Set("Test Post")
//                    )
//            ))
//            println("Modify result: $modifyResult")
//
//            table.remove(post1.id)
//        }
//
//    }
}
