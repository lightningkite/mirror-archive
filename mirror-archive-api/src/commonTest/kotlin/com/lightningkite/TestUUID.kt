package com.lightningkite

import com.lightningkite.mirror.archive.Id
import kotlin.test.Test
import kotlin.test.assertEquals

class TestUUID {
    @Test
    fun toStringAndBack(){
        val id = Id.key()
        val str = id.toUUIDString()
        println("STR: $str")
        val copy = Id.fromUUIDString(id.toUUIDString())
        assertEquals(id, copy)
    }
}