package com.lightningkite.mirror.archive.model

import com.lightningkite.mirror.info.MirrorClass

infix fun <T : Any, V> MirrorClass.Field<T, V>.setTo(value: V) = Operation.Field(this, Operation.Set(value))
infix fun <T : Any> MirrorClass.Field<T, Int>.addTo(value: Int) = Operation.Field(this, Operation.AddInt(value))
infix fun <T : Any> MirrorClass.Field<T, Long>.addTo(value: Long) = Operation.Field(this, Operation.AddLong(value))
infix fun <T : Any> MirrorClass.Field<T, Float>.addTo(value: Float) = Operation.Field(this, Operation.AddFloat(value))
infix fun <T : Any> MirrorClass.Field<T, Double>.addTo(value: Double) = Operation.Field(this, Operation.AddDouble(value))
infix fun <T> Operation<T>.and(other: Operation<T>) = Operation.Multiple(listOf(this, other))