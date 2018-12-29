package com.lightningkite.mirror.archive.model

import com.lightningkite.mirror.info.ClassInfo
import com.lightningkite.mirror.info.FieldInfo
import com.lightningkite.mirror.serialization.toAttributeHashMap

interface Operation<T>{

    operator fun invoke(item: T): T
    
    data class Set<T>(var value: T) : Operation<T> {
        override fun invoke(item: T): T = value
    }

    interface AddNumeric<T>: Operation<T> {
        val amount: Number
    }
    data class AddInt(override var amount: Int) : AddNumeric<Int> {
        override fun invoke(item: Int): Int = item + amount
    }
    data class AddLong(override var amount: Long) : AddNumeric<Long> {
        override fun invoke(item: Long): Long = item + amount
    }
    data class AddFloat(override var amount: Float) : AddNumeric<Float> {
        override fun invoke(item: Float): Float = item + amount
    }
    data class AddDouble(override var amount: Double) : AddNumeric<Double> {
        override fun invoke(item: Double): Double = item + amount
    }

    data class Append(var string: String): Operation<String> {
        override fun invoke(item: String): String = item + string
    }

//    data class AppendArray<T>(var item: T): Operation<Array<T>> {
//        override fun invoke(item: Array<T>): Array<T> = item + this.item
//    }
//
//    data class RemoveArray<T>(var item: T): Operation<Array<T>> {
//        override fun invoke(item: Array<T>): Array<T> = item.toMutableList().also{ it.remove(this.item) }.toTypedArray()
//    }

    data class Fields<T: Any>(var classInfo: ClassInfo<T>, var changes: Map<FieldInfo<T, *>, Operation<*>>): Operation<T> {
        override fun invoke(item: T): T {
            val map = item.toAttributeHashMap(classInfo)
            for((field, operation) in changes){
                val anyOp = operation as Operation<Any?>
                map[field.name] = anyOp.invoke(map[field.name])
            }
            return classInfo.construct(map)
        }
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