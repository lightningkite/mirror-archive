package com.lightningkite.kotlinx.db.influxdb

import com.lightningkite.mirror.archive.influxdb.EmbeddedInflux
import com.lightningkite.mirror.archive.influxdb.InfluxDatalog
import com.lightningkite.mirror.serialization.DefaultRegistry
import com.lightningkite.mirror.serialization.json.JsonSerializer
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.influxdb.InfluxDB
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class InfluxDBTest {

    val registry = DefaultRegistry + TestRegistry
    var connection: InfluxDB? = null

    @Before
    fun setup() {
        connection = EmbeddedInflux.start(clearFiles = true)
    }

    @After
    fun teardown() {
        connection?.close()
        connection = null
        EmbeddedInflux.stop()
    }

    @Test
    fun testAccess() {
        connection?.ping()
    }

    @Test
    fun testInsert() {
        val db = InfluxDatalog(
                registry = registry,
                connection = connection!!,
                backupStringSerializer = JsonSerializer(registry = registry),
                database = "main"
        )
        val cpuUsageTable = db.table(CPUUsage::class)
        runBlocking {
            cpuUsageTable.insert(CPUUsage(amount = .12))
            cpuUsageTable.insert(CPUUsage(amount = .22))

        }
    }

    @Test
    fun testGet() {
        val db = InfluxDatalog(
                registry = registry,
                connection = connection!!,
                backupStringSerializer = JsonSerializer(registry = registry),
                database = "main"
        )
        val cpuUsageTable = db.table(CPUUsage::class)
        runBlocking {
            val newUsage = CPUUsage(amount = .23)
            cpuUsageTable.insert(newUsage)
            val copy = cpuUsageTable.get(newUsage.id)
            assertEquals(newUsage, copy)

        }
    }

    @Test
    fun testQuery() {
        val db = InfluxDatalog(
                registry = registry,
                connection = connection!!,
                backupStringSerializer = JsonSerializer(registry = registry),
                database = "main"
        )
        val cpuUsageTable = db.table(CPUUsage::class)
        runBlocking {
            val newUsage = CPUUsage(amount = .23)
            cpuUsageTable.insert(newUsage)

            delay(100)

            val newUsage2 = CPUUsage(amount = .24)
            cpuUsageTable.insert(newUsage2)

            delay(100)

            val result = cpuUsageTable.query().results
            assertEquals(newUsage, result[0])
            assertEquals(newUsage2, result[1])

        }
    }

}