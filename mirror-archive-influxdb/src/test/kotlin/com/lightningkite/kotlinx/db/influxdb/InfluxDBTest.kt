package com.lightningkite.kotlinx.db.influxdb

import com.lightningkite.mirror.archive.database.insert
import com.lightningkite.mirror.archive.influxdb.EmbeddedInflux
import com.lightningkite.mirror.archive.influxdb.InfluxSuspendMap
import com.lightningkite.mirror.archive.model.Id
import com.lightningkite.mirror.info.type
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
        val db = InfluxSuspendMap.Provider(
                connection = connection!!,
                serializer = JsonSerializer(registry = registry)
        )
        val cpuUsageTable = db.suspendMap(Id::class.type, CPUUsage::class.type)
        runBlocking {
            cpuUsageTable.insert(CPUUsage(amount = .12))
            cpuUsageTable.insert(CPUUsage(amount = .22))
        }
    }

    @Test
    fun testGet() {
        val db = InfluxSuspendMap.Provider(
                connection = connection!!,
                serializer = JsonSerializer(registry = registry)
        )
        val cpuUsageTable = db.suspendMap(Id::class.type, CPUUsage::class.type)
        runBlocking {
            val newUsage = CPUUsage(amount = .23)
            cpuUsageTable.insert(newUsage)
            val copy = cpuUsageTable.get(newUsage.id)
            assertEquals(newUsage, copy)

        }
    }

    @Test
    fun testQuery() {
        val db = InfluxSuspendMap.Provider(
                connection = connection!!,
                serializer = JsonSerializer(registry = registry)
        )
        val cpuUsageTable = db.suspendMap(Id::class.type, CPUUsage::class.type)
        runBlocking {
            val newUsage = CPUUsage(amount = .23)
            cpuUsageTable.insert(newUsage)

            delay(100)

            val newUsage2 = CPUUsage(amount = .24)
            cpuUsageTable.insert(newUsage2)

            delay(100)

            val result = cpuUsageTable.query()
            assertEquals(newUsage, result[0].second)
            assertEquals(newUsage2, result[1].second)

        }
    }

}