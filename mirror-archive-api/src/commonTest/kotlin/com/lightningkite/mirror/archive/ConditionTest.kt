package com.lightningkite.mirror.archive

import com.lightningkite.mirror.archive.model.Condition
import com.lightningkite.mirror.archive.model.ConditionMirror
import com.lightningkite.mirror.archive.model.Operation
import com.lightningkite.mirror.archive.model.registerArchive
import com.lightningkite.mirror.info.IntMirror
import com.lightningkite.mirror.info.MirrorType
import com.lightningkite.mirror.info.StringMirror
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class ConditionTest {

    init {
        registerArchive()
    }

    fun <T> test(value: T, type: MirrorType<T>) {
        val result = Json.plain.stringify(type, value)
        println(result)
        val back = Json.plain.parse(type, result)
        assertEquals(value, back)
    }

    @Test fun condition(){
        test(Condition.Always, ConditionMirror(IntMirror))
        test(Condition.Never, ConditionMirror(IntMirror))
        test(Condition.Equal(1), ConditionMirror(IntMirror))
        test(Condition.GreaterThanOrEqual(1), ConditionMirror(IntMirror))
        test(Condition.LessThanOrEqual(1), ConditionMirror(IntMirror))
        test(Condition.LessThan(1), ConditionMirror(IntMirror))
        test(Condition.GreaterThan(1), ConditionMirror(IntMirror))
        test(Condition.And(listOf(Condition.Always, Condition.Never)), ConditionMirror(IntMirror))
        test(Condition.Or(listOf(Condition.Always, Condition.Never)), ConditionMirror(IntMirror))
        test(Condition.EqualToOne(listOf(1, 2, 3)), ConditionMirror(IntMirror))
        test(Condition.TextSearch("asdf"), ConditionMirror(StringMirror))
        test(Condition.RegexTextSearch("asdf"), ConditionMirror(StringMirror))
    }
}