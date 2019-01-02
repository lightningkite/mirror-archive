package com.lightningkite.mirror.archive

import com.lightningkite.mirror.archive.model.Condition
import com.lightningkite.mirror.archive.model.Operation
import com.lightningkite.mirror.info.Type
import com.lightningkite.mirror.info.TypeProjection
import com.lightningkite.mirror.info.type
import com.lightningkite.mirror.serialization.DefaultRegistry
import com.lightningkite.mirror.serialization.json.JsonSerializer
import com.lightningkite.rekwest.server.TestRegistry
import kotlin.test.Test

class TestSerdes {

    val serializer = JsonSerializer(
            DefaultRegistry + TestRegistry
    )

    fun <T> test(value: T, type: Type<T>) {
        val result = serializer.write(value, type)
        println(result)
        val back = serializer.read(result, type)
    }

    @Test fun condition(){
        test(Condition.Always<Int>(), Condition::class.type.copy(typeParameters = listOf(TypeProjection(Int::class.type))))
        test(Condition.Never<Int>(), Condition::class.type.copy(typeParameters = listOf(TypeProjection(Int::class.type))))
        test(Condition.Equal<Int>(1), Condition::class.type.copy(typeParameters = listOf(TypeProjection(Int::class.type))))
        test(Condition.GreaterThanOrEqual<Int>(1), Condition::class.type.copy(typeParameters = listOf(TypeProjection(Int::class.type))))
        test(Condition.LessThanOrEqual<Int>(1), Condition::class.type.copy(typeParameters = listOf(TypeProjection(Int::class.type))))
        test(Condition.LessThan<Int>(1), Condition::class.type.copy(typeParameters = listOf(TypeProjection(Int::class.type))))
        test(Condition.GreaterThan<Int>(1), Condition::class.type.copy(typeParameters = listOf(TypeProjection(Int::class.type))))
        test(Condition.And<Int>(listOf(Condition.Always<Int>(), Condition.Never<Int>())), Condition::class.type.copy(typeParameters = listOf(TypeProjection(Int::class.type))))
        test(Condition.Or<Int>(listOf(Condition.Always<Int>(), Condition.Never<Int>())), Condition::class.type.copy(typeParameters = listOf(TypeProjection(Int::class.type))))
        test(Condition.EqualToOne<Int>(listOf(1, 2, 3)), Condition::class.type.copy(typeParameters = listOf(TypeProjection(Int::class.type))))
        test(Condition.TextSearch<String>("asdf"), Condition::class.type.copy(typeParameters = listOf(TypeProjection(String::class.type))))
        test(Condition.RegexTextSearch<String>(Regex("asdf")), Condition::class.type.copy(typeParameters = listOf(TypeProjection(String::class.type))))
    }

    @Test fun operation() {
        test(Operation.Set(3), Operation::class.type.copy(typeParameters = listOf(TypeProjection(Int::class.type))))
    }
}