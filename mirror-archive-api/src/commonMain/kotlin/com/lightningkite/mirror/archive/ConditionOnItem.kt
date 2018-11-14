package com.lightningkite.mirror.archive

import com.lightningkite.mirror.info.SerializedFieldInfo


sealed class ConditionOnItem<in T : Any> {

    abstract operator fun invoke(item: T): Boolean
    open fun iterable(): Iterable<ConditionOnItem<T>>? = null
    fun recursing(): Sequence<ConditionOnItem<T>> {
        return sequenceOf(this).recursiveFlatMap { it.iterable()?.asSequence() ?: emptySequence() }
    }

    
    interface OnField<T : Any, V> {
        val field: SerializedFieldInfo<T, V>
    }

    
    class Never<T : Any> : ConditionOnItem<T>() {
        override fun invoke(item: T): Boolean = false
    }

    
    class Always<T : Any> : ConditionOnItem<T>() {
        override fun invoke(item: T): Boolean = true
    }

    
    data class And<T : Any>(val conditions: List<ConditionOnItem<T>>) : ConditionOnItem<T>() {
        override fun invoke(item: T): Boolean = conditions.all { it(item) }
        override fun iterable(): Iterable<ConditionOnItem<T>> = conditions
    }

    
    data class Or<T : Any>(val conditions: List<ConditionOnItem<T>>) : ConditionOnItem<T>() {
        override fun invoke(item: T): Boolean = conditions.any { it(item) }
        override fun iterable(): Iterable<ConditionOnItem<T>> = conditions
    }

    
    data class Not<T : Any>(val condition: ConditionOnItem<T>) : ConditionOnItem<T>() {
        override fun invoke(item: T): Boolean = !condition(item)
        override fun iterable(): Iterable<ConditionOnItem<T>> = listOf(condition)
    }

    
    data class Equal<T : Any, V>(override val field: SerializedFieldInfo<T, V>, val value: V) : ConditionOnItem<T>(), ConditionOnItem.OnField<T, V> {
        override fun invoke(item: T): Boolean = field.get.invoke(item) == value
    }

    
    data class EqualToOne<T : Any, V>(override val field: SerializedFieldInfo<T, V>, val values: Collection<V>) : ConditionOnItem<T>(), ConditionOnItem.OnField<T, V> {
        override fun invoke(item: T): Boolean = field.get.invoke(item) in values
    }

    
    data class NotEqual<T : Any, V>(override val field: SerializedFieldInfo<T, V>, val value: V) : ConditionOnItem<T>(), ConditionOnItem.OnField<T, V> {
        override fun invoke(item: T): Boolean = field.get.invoke(item) != value
    }

    
    data class LessThan<T : Any, V : Comparable<V>>(override val field: SerializedFieldInfo<T, V>, val value: V) : ConditionOnItem<T>(), ConditionOnItem.OnField<T, V> {
        override fun invoke(item: T): Boolean = field.get.invoke(item) < value
    }

    
    data class GreaterThan<T : Any, V : Comparable<V>>(override val field: SerializedFieldInfo<T, V>, val value: V) : ConditionOnItem<T>(), ConditionOnItem.OnField<T, V> {
        override fun invoke(item: T): Boolean = field.get.invoke(item) > value
    }

    
    data class LessThanOrEqual<T : Any, V : Comparable<V>>(override val field: SerializedFieldInfo<T, V>, val value: V) : ConditionOnItem<T>(), ConditionOnItem.OnField<T, V> {
        override fun invoke(item: T): Boolean = field.get.invoke(item) <= value
    }

    
    data class GreaterThanOrEqual<T : Any, V : Comparable<V>>(override val field: SerializedFieldInfo<T, V>, val value: V) : ConditionOnItem<T>(), ConditionOnItem.OnField<T, V> {
        override fun invoke(item: T): Boolean = field.get.invoke(item) >= value
    }

    
    data class TextSearch<T : Any, V : CharSequence>(override val field: SerializedFieldInfo<T, V>, val query: String) : ConditionOnItem<T>(), ConditionOnItem.OnField<T, V> {
        override fun invoke(item: T): Boolean = field.get.invoke(item).contains(query)
    }

    
    data class RegexTextSearch<T : Any, V : CharSequence>(override val field: SerializedFieldInfo<T, V>, val query: Regex) : ConditionOnItem<T>(), ConditionOnItem.OnField<T, V> {
        override fun invoke(item: T): Boolean = field.get.invoke(item).contains(query)
    }
}
