package com.lightningkite.kotlinx.db.postgres

import com.lightningkite.mirror.archive.database.SuspendMap
import com.lightningkite.mirror.archive.model.Condition
import com.lightningkite.mirror.archive.model.Operation
import com.lightningkite.mirror.archive.postgres.*
import com.lightningkite.mirror.archive.sql.SQLSuspendMap
import com.lightningkite.mirror.info.type
import com.lightningkite.mirror.serialization.DefaultRegistry
import io.reactiverse.pgclient.PgPool
import kotlinx.coroutines.runBlocking
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import kotlin.test.*

class SimpleKVTest {

    companion object {
        val poolProvider = EmbeddedPG.PoolProvider(
                cache = File("/postgresql-cache"),
                version = EmbeddedPG.Versions.VERSION_10,
                storeFiles = File("./build/run/files").also { it.deleteRecursively() }
        )
        lateinit var pool: PgPool

        @BeforeClass
        @JvmStatic
        fun before() {
            println("Starting...")
            pool = poolProvider.start()
        }

        @AfterClass
        @JvmStatic
        fun after() {
            println("Stopping...")
            poolProvider.stop()
        }
    }

    fun runWithSimpleMap(map: suspend SuspendMap<String, Int>.()->Unit) {
        //Set up the database
        val provider = PostgresSuspendMap.Provider(
                serializer = PostgresSerializer(DefaultRegistry + TestRegistry),
                connection = PostgresConnection(pool)
        )

        val simple = provider.suspendMap(String::class.type, Int::class.type)

        runBlocking {
            simple.checkSetup()
            pool.suspendQuery("DELETE FROM ${simple.table.fullName}")
            map(simple)
        }
    }

    @Test
    fun putAndGet(){
        runWithSimpleMap {
            assertEquals(true, put("a", 1))
            assertEquals(1, get("a"))
        }
    }

    @Test
    fun remove(){
        runWithSimpleMap {
            assertEquals(true, put("a", 1))
            assertEquals(1, get("a"))
            remove("a")
            assertEquals(null, get("a"))
        }
    }

    @Test
    fun removeConditionalSuccess(){
        runWithSimpleMap {
            assertEquals(true, put("a", 1))
            assertEquals(1, get("a"))
            remove("a", Condition.Equal(1))
            assertEquals(null, get("a"))
        }
    }

    @Test
    fun removeConditionalFailure(){
        runWithSimpleMap {
            assertEquals(true, put("a", 1))
            assertEquals(1, get("a"))
            remove("a", Condition.Equal(2))
            assertEquals(1, get("a"))
        }
    }

    @Test
    fun modifySet(){
        runWithSimpleMap {
            assertEquals(true, put("a", 1))
            assertEquals(1, get("a"))
            assertEquals(2, modify("a", Operation.Set(2)))
            assertEquals(2, get("a"))
        }
    }

    @Test
    fun modifyIncrement(){
        runWithSimpleMap {
            assertEquals(true, put("a", 1))
            assertEquals(1, get("a"))
            assertEquals(2, modify("a", Operation.AddInt(1)))
            assertEquals(2, get("a"))
        }
    }

    @Test
    fun putOverwrite(){
        runWithSimpleMap {
            assertEquals(true, put("a", 1))
            assertEquals(1, get("a"))
            assertEquals(true, put("a", 2))
            assertEquals(2, get("a"))
        }
    }

    @Test
    fun putDoNotOverwrite(){
        runWithSimpleMap {
            assertEquals(true, put("a", 1))
            assertEquals(1, get("a"))
            put("a", 2, Condition.Never())
            assertEquals(1, get("a"))
        }
    }

    @Test
    fun putOnlyOverwrite(){
        runWithSimpleMap {
            assertEquals(false, put("a", 1, create = false))
            assertEquals(null, get("a"))
            assertEquals(true, put("a", 2))
            assertEquals(2, get("a"))
        }
    }

    @Test
    fun query(){
        runWithSimpleMap {
            assertEquals(true, put("a", 1))
            assertEquals(true, put("b", 2))
            val queryResult = query()
            assert(queryResult.any { it == "a" to 1 })
            assert(queryResult.any { it == "b" to 2 })
            assertEquals(2, queryResult.size)
        }
    }

    @Test
    fun putConditionallySuccess(){
        runWithSimpleMap {
            assertEquals(true, put("a", 1))
            assertEquals(1, get("a"))
            assertEquals(true, put("a", 2, Condition.Equal(1)))
            assertEquals(2, get("a"))
        }
    }

    @Test
    fun putConditionallyFail(){
        runWithSimpleMap {
            assertEquals(true, put("a", 1))
            assertEquals(1, get("a"))
            assertEquals(false, put("a", 2, Condition.Equal(3)))
            assertEquals(1, get("a"))
        }
    }

    @Test
    fun modifySetConditionallySuccess(){
        runWithSimpleMap {
            assertEquals(true, put("a", 1))
            assertEquals(1, get("a"))
            assertEquals(2, modify("a", Operation.Set(2), Condition.Equal(1)))
            assertEquals(2, get("a"))
        }
    }

    @Test
    fun modifySetConditionallyFail(){
        runWithSimpleMap {
            assertEquals(true, put("a", 1))
            assertEquals(1, get("a"))
            assertEquals(null, modify("a", Operation.Set(2), Condition.Equal(3)))
            assertEquals(1, get("a"))
        }
    }

//    @Test
//    fun test() {
//        //Set up the database
//        val provider = SQLSuspendMap.Provider(
//                serializer = PostgresSerializer(DefaultRegistry + TestRegistry),
//                connection = PostgresConnection(pool)
//        )
//
//        val simple = provider.suspendMap(String::class.type, Int::class.type)
//
//        runBlocking {
//
//            //Test simple puts
//            simple.put("a", 32)
//            simple.put("answer", 42)
//
//            //Test query, assure puts worked
//            assert(simple.query().contains("a" to 32))
//            assert(simple.query().contains("answer" to 42))
//            assertEquals(32, simple.get("a"))
//            assertEquals(33, simple.modify("a", Operation.AddInt(1)))
//            assert(simple.query().contains("a" to 33))
//            assertEquals(33, simple.get("a"))
//            assertEquals(34, simple.modify("a", Operation.AddInt(1), Condition.Equal(33)))
//            assertEquals(34, simple.get("a"))
//            assertEquals(null, simple.modify("a", Operation.AddInt(1), Condition.Equal(33)))
//            assertEquals(34, simple.get("a"))
//            assert(simple.query().contains("a" to 33))
//            assert(simple.query().contains("answer" to 42))
//            simple.remove("a")
//            assertFalse(simple.query().any { it.first == "a" })
//            assert(simple.query().contains("answer" to 42))
//            assertEquals(null, simple.get("a"))
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
