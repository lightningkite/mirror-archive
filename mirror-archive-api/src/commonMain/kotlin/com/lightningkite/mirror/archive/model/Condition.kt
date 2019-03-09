package com.lightningkite.mirror.archive.model

import com.lightningkite.kommon.collection.treeWalkDepthSequence
import com.lightningkite.mirror.info.MirrorClass


sealed class Condition<in T> {

    abstract operator fun invoke(item: T): Boolean
    open fun iterable(): Iterable<Condition<*>>? = null
    fun recursing(): Sequence<Condition<*>> {
        return sequenceOf(this).treeWalkDepthSequence<Condition<*>> { it.iterable()?.asSequence() ?: emptySequence() }
    }
    open fun simplify(): Condition<T> = this


    object Never : Condition<Any?>() {
        override fun invoke(item: Any?): Boolean = false
    }


    object Always : Condition<Any?>() {
        override fun invoke(item: Any?): Boolean = true
    }


    data class And<T>(val conditions: List<Condition<T>>) : Condition<T>() {
        override fun invoke(item: T): Boolean = conditions.all { it(item) }
        override fun iterable(): Iterable<Condition<*>> = conditions
        override fun simplify(): Condition<T> {
            val result = ArrayList<Condition<T>>()
            for(condition in conditions){
                val innerSimp = condition.simplify()
                when(innerSimp){
                    is Never -> return Never
                    is Always -> {}
                    is And -> result.addAll(innerSimp.conditions)
                    else -> result.add(innerSimp)
                }
            }
            if(result.size == 1) return result.first()
            return Condition.And(result)
        }
    }


    data class Or<T>(val conditions: List<Condition<T>>) : Condition<T>() {
        override fun invoke(item: T): Boolean = conditions.any { it(item) }
        override fun iterable(): Iterable<Condition<*>> = conditions
        override fun simplify(): Condition<T> {
            val result = ArrayList<Condition<T>>()
            for(condition in conditions){
                val innerSimp = condition.simplify()
                when(innerSimp){
                    is Always -> return Always
                    is Never -> {}
                    is Or -> result.addAll(innerSimp.conditions)
                    else -> result.add(innerSimp)
                }
            }
            if(result.size == 1) return result.first()
            return Condition.Or(result)
        }
    }


    data class Not<T>(val condition: Condition<T>) : Condition<T>() {
        override fun invoke(item: T): Boolean = !condition(item)
        override fun iterable(): Iterable<Condition<*>> = listOf(condition)
    }


    data class Field<T : Any, V>(val field: MirrorClass.Field<T, V>, val condition: Condition<V>) : Condition<T>() {
        override fun invoke(item: T): Boolean = condition.invoke(field.get(item))
        override fun iterable(): Iterable<Condition<*>> = listOf(condition)
    }


    data class Equal<T>(val value: T) : Condition<T>() {
        override fun invoke(item: T): Boolean = item == value
    }


    data class EqualToOne<T>(val values: List<T>) : Condition<T>() {
        override fun invoke(item: T): Boolean = item in values
    }


    data class NotEqual<T>(val value: T) : Condition<T>() {
        override fun invoke(item: T): Boolean = item != value
    }


    data class LessThan<T : Comparable<T>>(val value: T) : Condition<T>() {
        override fun invoke(item: T): Boolean = item < value
    }


    data class GreaterThan<T : Comparable<T>>(val value: T) : Condition<T>() {
        override fun invoke(item: T): Boolean = item > value
    }


    data class LessThanOrEqual<T : Comparable<T>>(val value: T) : Condition<T>() {
        override fun invoke(item: T): Boolean = item <= value
    }


    data class GreaterThanOrEqual<T : Comparable<T>>(val value: T) : Condition<T>() {
        override fun invoke(item: T): Boolean = item >= value
    }


    data class TextSearch(val query: String) : Condition<String>() {
        override fun invoke(item: String): Boolean = item.contains(query)
    }

    data class StartsWith(val query: String) : Condition<String>() {
        override fun invoke(item: String): Boolean = item.startsWith(query)
    }

    data class EndsWith(val query: String) : Condition<String>() {
        override fun invoke(item: String): Boolean = item.endsWith(query)
    }


    data class RegexTextSearch(val query: String) : Condition<String>() {
        override fun invoke(item: String): Boolean = item.contains(Regex(query))
    }
}

infix fun <T> Condition<T>.and(other:Condition<T>):Condition<T> = Condition.And(listOf(this, other)).simplify()
infix fun <T> Condition<T>.or(other:Condition<T>):Condition<T> = Condition.Or(listOf(this, other)).simplify()
operator fun <T> Condition<T>.not() = Condition.Not(this)