package com.lightningkite.mirror.archive.model

import com.lightningkite.mirror.info.MirrorClass

infix fun <T : Any, V> MirrorClass.Field<T, V>.equal(value: V) = Condition.Field(this, Condition.Equal(value))
infix fun <T : Any, V> MirrorClass.Field<T, V>.notEqual(value: V) = Condition.Field(this, Condition.NotEqual(value))
infix fun <T : Any, V : Comparable<V>> MirrorClass.Field<T, V>.lessThan(value: V) = Condition.Field(this, Condition.LessThan(value))
infix fun <T : Any, V : Comparable<V>> MirrorClass.Field<T, V>.greaterThan(value: V) = Condition.Field(this, Condition.GreaterThan(value))
infix fun <T : Any, V : Comparable<V>> MirrorClass.Field<T, V>.lessThanOrEqual(value: V) = Condition.Field(this, Condition.LessThanOrEqual(value))
infix fun <T : Any, V : Comparable<V>> MirrorClass.Field<T, V>.greaterThanOrEqual(value: V) = Condition.Field(this, Condition.GreaterThanOrEqual(value))
infix fun <T : Any, V> MirrorClass.Field<T, V>.equalToOne(values: List<V>) = Condition.Field(this, Condition.EqualToOne(values))

infix fun <T : Any> MirrorClass.Field<T, String>.textSearch(value: String) = Condition.Field(this, Condition.TextSearch(value))
infix fun <T : Any> MirrorClass.Field<T, String>.startsWith(value: String) = Condition.Field(this, Condition.StartsWith(value))
infix fun <T : Any> MirrorClass.Field<T, String>.endsWith(value: String) = Condition.Field(this, Condition.EndsWith(value))

infix fun <T> Condition<T>.and(other: Condition<T>): Condition<T> = Condition.And(listOf(this, other)).simplify()
infix fun <T> Condition<T>.or(other: Condition<T>): Condition<T> = Condition.Or(listOf(this, other)).simplify()
operator fun <T> Condition<T>.not() = Condition.Not(this)