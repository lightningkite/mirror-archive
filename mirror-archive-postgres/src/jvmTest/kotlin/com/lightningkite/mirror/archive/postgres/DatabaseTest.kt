package com.lightningkite.mirror.archive.postgres

import com.lightningkite.mirror.archive.database.Database
import com.lightningkite.mirror.archive.model.Condition
import com.lightningkite.mirror.archive.model.Operation
import com.lightningkite.mirror.archive.model.Sort
import com.lightningkite.mirror.test.IntData
import com.lightningkite.mirror.test.IntDataMirror
import com.lightningkite.mirror.test.Zoo
import com.lightningkite.mirror.test.ZooMirror
import kotlinx.coroutines.runBlocking
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DatabaseTest {

    companion object {

        lateinit var database: Database<Zoo>
        var provider: EmbeddedPG.PoolProvider? = null

        @JvmStatic
        @BeforeClass
        fun before() {
            provider = EmbeddedPG.PoolProvider(
                    cache = File("/pgcache"),
                    version = EmbeddedPG.Versions.VERSION_10,
                    clearBeforeStarting = true,
                    storeFiles = File("build/pg")
            )
            database = PostgresDatabase<Zoo>(
                    mirror = ZooMirror,
                    default = Zoo.zero(),
                    primaryKey = listOf(ZooMirror.fieldInt),
                    client = provider!!.start()
            )
        }

        @JvmStatic
        @AfterClass
        fun after() {
            provider?.stop()
            provider = null
        }

    }

    @Before
    fun beforeTest() {
        runBlocking {
            database.delete(Condition.Always)
        }
    }

    val testData = listOf(
            Zoo.instance().copy(int = 1, intN = 1, intData = IntData(42), string = "asdf"),
            Zoo.instance().copy(int = 2, intN = 2, string = "qwer"),
            Zoo.instance().copy(int = 3, intN = 3, string = "zxcv"),
            Zoo.instance().copy(int = 4, intN = 4, string = "string")
    )
    val testConditions = listOf<Condition<Zoo>>(
            Condition.Never,
            Condition.Always,
            Condition.And(listOf(Condition.Always, Condition.Field(ZooMirror.fieldInt, Condition.Equal(2)))),
            Condition.Or(listOf(Condition.Field(ZooMirror.fieldInt, Condition.Equal(2)), Condition.Never)),
            Condition.Not(Condition.Field(ZooMirror.fieldInt, Condition.Equal(2))),
            Condition.Field(ZooMirror.fieldInt, Condition.Equal(2)),
            Condition.Field(ZooMirror.fieldInt, Condition.EqualToOne(listOf(1, 2, 3))),
            Condition.Field(ZooMirror.fieldInt, Condition.NotEqual(2)),
            Condition.Field(ZooMirror.fieldInt, Condition.LessThan(2)),
            Condition.Field(ZooMirror.fieldInt, Condition.GreaterThan(2)),
            Condition.Field(ZooMirror.fieldInt, Condition.LessThanOrEqual(2)),
            Condition.Field(ZooMirror.fieldInt, Condition.GreaterThanOrEqual(2)),
            Condition.Field(ZooMirror.fieldString, Condition.TextSearch("asdf")),
            Condition.Field(ZooMirror.fieldString, Condition.StartsWith("asd")),
            Condition.Field(ZooMirror.fieldString, Condition.EndsWith("xcv")),
            Condition.Field(ZooMirror.fieldString, Condition.RegexTextSearch("[aq][sw][de][fr]")),
            Condition.Field(ZooMirror.fieldIntData, Condition.Field(IntDataMirror.fieldIntV, Condition.Equal(42)))
    )
    val testOperations = listOf(
            Operation.Field(ZooMirror.fieldLong, Operation.AddLong(1)),
            Operation.Field(ZooMirror.fieldFloat, Operation.AddFloat(1f)),
            Operation.Field(ZooMirror.fieldDouble, Operation.AddDouble(1.0)),
            Operation.Field(ZooMirror.fieldIntN, Operation.Set<Int?>(null)),
            Operation.Field(ZooMirror.fieldIntData, Operation.Field(IntDataMirror.fieldIntV, Operation.AddInt(1))),
            Operation.Set(Zoo.instance().copy(defaultIfNotPresent = 999))
    )

    @Test
    fun insertOne() {
        runBlocking {
            val toInsert = listOf(
                    Zoo.instance().copy(int = 1)
            )
            val result = database.insert(toInsert)
            assertEquals(toInsert.size, result.size)
            for (i in result.indices) {
                assertEquals(toInsert[i], result[i])
            }
            val retrieved = database.get()
            assertEquals(toInsert.size, retrieved.size)
            for (element in toInsert) {
                assert(element in retrieved)
            }
        }
    }

    @Test
    fun insertMany() {
        runBlocking {
            val toInsert = testData
            val result = database.insert(toInsert)
            assertEquals(toInsert.size, result.size)
            for (i in result.indices) {
                assertEquals(toInsert[i], result[i])
            }
            val retrieved = database.get()
            assertEquals(toInsert.size, retrieved.size)
            for (element in toInsert) {
                assert(element in retrieved)
            }
        }
    }

    @Test
    fun getPlain() {
        runBlocking {
            val toInsert = testData
            database.insert(toInsert)

            val retrieved = database.get()
            assertEquals(toInsert.size, retrieved.size)
            for (element in toInsert) {
                assert(element in retrieved)
            }
        }
    }

    @Test
    fun getConditions() {
        runBlocking {
            val toInsert = testData
            database.insert(toInsert)

            for (condition in testConditions) {
                try {
                    println("getConditions: Testing ${condition}")
                    val expected = toInsert.filter { condition(it) }
                    val retrieved = database.get(condition = condition)
                    println("getConditions: Expected ${expected.joinToString { it.hashCode().toString() }}, got ${retrieved.joinToString { it.hashCode().toString() }}")
                    assertEquals(expected.size, retrieved.size)
                    for (element in expected) {
                        assert(element in retrieved)
                    }
                } catch (t: Throwable) {
                    throw Exception("Failed while testing ${condition}", t)
                }
            }
        }
    }

    @Test
    fun getAfterSorted() {
        runBlocking {
            val toInsert = testData
            database.insert(toInsert)

            for (index in toInsert.indices) {
                val expected = toInsert.subList(index + 1, toInsert.size)
                val retrieved = database.get(after = toInsert[index], sort = listOf(Sort(ZooMirror.fieldInt)))
                println("getAfterSorted: Expected ${expected.joinToString { it.hashCode().toString() }}, got ${retrieved.joinToString { it.hashCode().toString() }}")
                assertEquals(expected.size, retrieved.size)
                for (i in retrieved.indices) {
                    assertEquals(expected[i], retrieved[i])
                }
            }
        }
    }

    @Test
    fun getAfterUnsorted() {
        runBlocking {
            val toInsert = testData
            database.insert(toInsert)

            var iterationsRemaining = toInsert.size
            val missed = toInsert.toMutableSet()
            var previous: Zoo? = null
            do {
                val received = database.get(count = 1, after = previous).firstOrNull()
                if (received != null) {
                    missed.remove(received)
                }
                println("getAfterUnsorted: Received ${received?.int}")
                previous = received
                iterationsRemaining--
            } while (previous != null && iterationsRemaining >= 0)

            println("getAfterUnsorted: Didn't ever get $missed")
            assert(missed.isEmpty())
        }
    }


    @Test
    fun deleteConditions() {
        runBlocking {
            for (condition in testConditions) {
                try {
                    println("deleteConditions: Testing ${condition}")

                    //Reset the DB
                    beforeTest()

                    val toInsert = testData
                    database.insert(toInsert)

                    val expected = toInsert.filter { !condition(it) }
                    database.delete(condition = condition)
                    val retrieved = database.get()
                    println("deleteConditions: Expected ${expected.joinToString { it.hashCode().toString() }}, got ${retrieved.joinToString { it.hashCode().toString() }}")
                    assertEquals(expected.size, retrieved.size)
                    for (element in expected) {
                        assert(element in retrieved)
                    }
                } catch (t: Throwable) {
                    throw Exception("Failed while testing ${condition}", t)
                }
            }
        }
    }

    @Test
    fun updateConditions() {
        runBlocking {
            for (condition in testConditions) {
                try {
                    println("updateConditions: Testing ${condition}")

                    //Reset the DB
                    beforeTest()

                    val toInsert = testData
                    database.insert(toInsert)

                    val operation = Operation.SetField(ZooMirror.fieldIntN, null)

                    val rowCount = database.update(condition = condition, operation = operation)
                    val expected = toInsert.filter { condition(it) }.map { operation(it) } + toInsert.filter { !condition(it) }

                    val retrieved = database.get()
                    println("updateConditions: Modified $rowCount, Expected ${expected.joinToString { it.hashCode().toString() }}, got ${retrieved.joinToString { it.hashCode().toString() }}")
                    assertEquals(expected.size, retrieved.size)
                    for (element in expected) {
                        assert(element in retrieved)
                    }
                } catch (t: Throwable) {
                    throw Exception("Failed while testing ${condition}", t)
                }
            }
        }
    }

    @Test
    fun updateOperations() {
        val condition = Condition.Field(ZooMirror.fieldInt, Condition.Equal(2))
        runBlocking {
            for (operation in testOperations) {
                try {

                    println("updateOperations: Testing $operation")

                    //Reset the DB
                    beforeTest()

                    val toInsert = testData
                    database.insert(toInsert)


                    val rowCount = database.update(condition = condition, operation = operation)
                    val expected = toInsert.filter { condition(it) }.map { operation(it) } + toInsert.filter { !condition(it) }

                    val retrieved = database.get()
                    println("updateOperations: Modified $rowCount, Expected ${expected.joinToString { it.hashCode().toString() }}, got ${retrieved.joinToString { it.hashCode().toString() }}")
                    assertEquals(expected.size, retrieved.size)
                    for (element in expected) {
                        assert(element in retrieved) {
                            "Expected element not found in retrieved.\nExpected: $element\nFound:${retrieved.joinToString("\n")}"
                        }
                    }
                } catch (t: Throwable) {
                    throw Exception("Failed while testing ${operation}", t)
                }
            }
        }
    }
}