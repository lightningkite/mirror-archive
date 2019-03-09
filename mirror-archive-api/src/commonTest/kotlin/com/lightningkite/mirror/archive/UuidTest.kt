package com.lightningkite.mirror.archive

import com.lightningkite.mirror.archive.model.Uuid
import kotlin.test.Test
import kotlin.test.assertEquals

class UuidTest {
    @Test
    fun toStringAndBack(){
        val id = Uuid.randomUUID4()
        val str = id.toUUIDString()
        println("STR: $str")
        val copy = Uuid.fromUUIDString(id.toUUIDString())
        assertEquals(id, copy)
    }
}