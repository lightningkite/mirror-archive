package com.lightningkite.mirror.archive

import com.lightningkite.mirror.archive.database.Database
import com.lightningkite.mirror.archive.database.RAMDatabase
import com.lightningkite.mirror.archive.database.secure
import com.lightningkite.mirror.archive.database.secureFields
import com.lightningkite.mirror.archive.model.Condition
import com.lightningkite.mirror.archive.model.Operation
import com.lightningkite.mirror.archive.model.Sort
import com.lightningkite.mirror.test.IntData
import com.lightningkite.mirror.test.IntDataMirror
import com.lightningkite.mirror.test.Zoo
import com.lightningkite.mirror.test.ZooMirror
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DatabaseTest {

    lateinit var database: Database<Zoo>

    @BeforeTest
    fun before() {
        database = RAMDatabase<Zoo>()
    }

    @AfterTest
    fun after() {
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
            Operation.Set(Zoo.instance().copy(defaultIfNotPresent = 999)),
            Operation.Field(ZooMirror.fieldIntN, Operation.Set<Int?>(null)),
            Operation.Field(ZooMirror.fieldInt, Operation.AddInt(1)),
            Operation.Field(ZooMirror.fieldLong, Operation.AddLong(1)),
            Operation.Field(ZooMirror.fieldFloat, Operation.AddFloat(1f)),
            Operation.Field(ZooMirror.fieldDouble, Operation.AddDouble(1.0)),
            Operation.Field(ZooMirror.fieldIntData, Operation.Field(IntDataMirror.fieldIntV, Operation.AddInt(1)))
    )

    @Test
    fun secureSyntax() {
        database.secure(
                limitRead = Condition.Always,
                limitUpdate = Condition.Never,
                limitInsert = Condition.Never
        ).secureFields(ZooMirror, Zoo.instance()) {
            ZooMirror.fieldInt.apply {
                read(Condition.Never)
                update(Condition.Never)
                tweaks("Adds one to given value.") { it + 1 }
            }
        }
    }

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
                println("getConditions: Testing ${condition}")
                val expected = toInsert.filter { condition(it) }
                val retrieved = database.get(condition = condition)
                println("getConditions: Expected ${expected.joinToString { it.hashCode().toString() }}, got ${retrieved.joinToString { it.hashCode().toString() }}")
                assertEquals(expected.size, retrieved.size)
                for (element in expected) {
                    assert(element in retrieved)
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
                println("deleteConditions: Testing ${condition}")

                //Reset the DB
                after()
                before()

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
            }
        }
    }

    @Test
    fun updateConditions() {
        runBlocking {
            for (condition in testConditions) {
                println("updateConditions: Testing ${condition}")

                //Reset the DB
                after()
                before()

                val toInsert = testData
                database.insert(toInsert)

                val operation = Operation.SetField(ZooMirror.fieldIntN, null)

                database.update(condition = condition, operation = operation)
                val expected = toInsert.filter { condition(it) }.map { operation(it) } + toInsert.filter { !condition(it) }

                val retrieved = database.get()
                println("updateConditions: Expected ${expected.joinToString { it.hashCode().toString() }}, got ${retrieved.joinToString { it.hashCode().toString() }}")
                assertEquals(expected.size, retrieved.size)
                for (element in expected) {
                    assert(element in retrieved)
                }
            }
        }
    }

    @Test
    fun updateOperations() {
        val condition = Condition.Field(ZooMirror.fieldInt, Condition.Equal(2))
        runBlocking {
            for (operation in testOperations) {

                println("updateOperations: Testing $operation")

                //Reset the DB
                after()
                before()

                val toInsert = testData
                database.insert(toInsert)


                database.update(condition = condition, operation = operation)
                val expected = toInsert.filter { condition(it) }.map { operation(it) } + toInsert.filter { !condition(it) }

                val retrieved = database.get()
                println("updateOperations: Expected ${expected.joinToString { it.hashCode().toString() }}, got ${retrieved.joinToString { it.hashCode().toString() }}")
                assertEquals(expected.size, retrieved.size)
                for (element in expected) {
                    assert(element in retrieved)
                }
            }
        }
    }
}