package com.lightningkite.mirror.archive

import com.lightningkite.mirror.info.ClassInfo
import com.lightningkite.mirror.info.FieldInfo
import com.lightningkite.mirror.serialization.toAttributeHashMap

sealed class ModificationOnItem<T : Any, V> {

    abstract val field: FieldInfo<T, V>
    abstract operator fun invoke(item: MutableMap<String, Any?>)
    abstract fun invokeOnSub(value: V): V

    
    data class Set<T : Any, V>(override var field: FieldInfo<T, V>, var value: V) : ModificationOnItem<T, V>() {
        override fun invoke(item: MutableMap<String, Any?>)  {
            item[field.name] = value
        }
        override fun invokeOnSub(value: V): V = value
    }

    
    data class Add<T : Any, V : Number>(override var field: FieldInfo<T, V>, var amount: V) : ModificationOnItem<T, V>() {
        override fun invoke(item: MutableMap<String, Any?>)  {
            item[field.name] = invokeOnSub(amount)
        }
        @Suppress("UNCHECKED_CAST", "IMPLICIT_CAST_TO_ANY")
        override fun invokeOnSub(value: V): V = when (field.type.kClass) {
            Byte::class -> value.let { it as Byte? }?.plus(amount as Byte)
            Short::class -> value.let { it as Short? }?.plus(amount as Short)
            Int::class -> value.let { it as Int? }?.plus(amount as Int)
            Long::class -> value.let { it as Long? }?.plus(amount as Long)
            Float::class -> value.let { it as Float? }?.plus(amount as Float)
            Double::class -> value.let { it as Double? }?.plus(amount as Double)
            else -> throw IllegalArgumentException()
        } as V
    }

    
    data class Multiply<T : Any, V : Number>(override var field: FieldInfo<T, V>, var amount: V) : ModificationOnItem<T, V>() {
        override fun invoke(item: MutableMap<String, Any?>)  {
            item[field.name] = invokeOnSub(amount)
        }
        @Suppress("UNCHECKED_CAST", "IMPLICIT_CAST_TO_ANY")
        override fun invokeOnSub(value: V): V = when (field.type.kClass) {
            Byte::class -> value.let { it as Byte? }?.times(amount as Byte)
            Short::class -> value.let { it as Short? }?.times(amount as Short)
            Int::class -> value.let { it as Int? }?.times(amount as Int)
            Long::class -> value.let { it as Long? }?.times(amount as Long)
            Float::class -> value.let { it as Float? }?.times(amount as Float)
            Double::class -> value.let { it as Double? }?.times(amount as Double)
            else -> throw IllegalArgumentException()
        } as V
    }

//    
//    data class Place<T : Any, V : Collection<I>, I>(override var field: FieldInfo<T, V>, var element: V) : ModificationOnItem<T, V>() {
//        override fun invoke(item: T) {
//            field.set.untyped(item, invokeOnSub(amount))
//        }
//        @Suppress("UNCHECKED_CAST", "IMPLICIT_CAST_TO_ANY")
//        override fun invokeOnSub(value: V): V = when (field.type.base) {
//            ByteReflection -> value.let { it as Byte? }?.times(amount as Byte)
//            ShortReflection -> value.let { it as Short? }?.times(amount as Short)
//            IntReflection -> value.let { it as Int? }?.times(amount as Int)
//            LongReflection -> value.let { it as Long? }?.times(amount as Long)
//            FloatReflection -> value.let { it as Float? }?.times(amount as Float)
//            DoubleReflection -> value.let { it as Double? }?.times(amount as Double)
//            else -> throw IllegalArgumentException()
//        } as V
//    }
//
//    
//    data class Remove<T : Any, V : Collection<I>, I>(override var field: FieldInfo<T, V>, var element: V) : ModificationOnItem<T, V>() {
//        override fun invoke(item: T) {
//            field.set.untyped(item, invokeOnSub(amount))
//        }
//        @Suppress("UNCHECKED_CAST", "IMPLICIT_CAST_TO_ANY")
//        override fun invokeOnSub(value: V): V = when (field.type.base) {
//            ByteReflection -> value.let { it as Byte? }?.times(amount as Byte)
//            ShortReflection -> value.let { it as Short? }?.times(amount as Short)
//            IntReflection -> value.let { it as Int? }?.times(amount as Int)
//            LongReflection -> value.let { it as Long? }?.times(amount as Long)
//            FloatReflection -> value.let { it as Float? }?.times(amount as Float)
//            DoubleReflection -> value.let { it as Double? }?.times(amount as Double)
//            else -> throw IllegalArgumentException()
//        } as V
//    }
}

fun <T : Any> Iterable<ModificationOnItem<T, *>>.invoke(item: MutableMap<String, Any?>) = forEach { it.invoke(item) }
fun <T : Any> T.apply(classInfo: ClassInfo<T>, modifications: List<ModificationOnItem<T, *>>): T {
    val values = this.toAttributeHashMap(classInfo)
    modifications.invoke(values)
    return classInfo.construct(values)
}