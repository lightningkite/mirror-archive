package com.lightningkite.mirror.archive.model

import com.lightningkite.lokalize.location.Geohash
import com.lightningkite.lokalize.location.GeohashCoverage
import com.lightningkite.lokalize.location.GeohashMirror
import com.lightningkite.lokalize.time.DayOfWeek
import com.lightningkite.lokalize.time.DaysOfWeekMirror
import com.lightningkite.lokalize.time.DaysOfWeek
import com.lightningkite.mirror.info.ClosedRangeMirror
import com.lightningkite.mirror.info.MirrorClass
import com.lightningkite.mirror.info.MirrorType

infix fun <T : Any, V> MirrorClass.Field<T, V>.equal(value: V): Condition<T?> = Condition.Field(this, Condition.Equal(value))
infix fun <T : Any, V> MirrorClass.Field<T, V>.notEqual(value: V): Condition<T?> = Condition.Field(this, Condition.NotEqual(value))
infix fun <T : Any, V : Comparable<V>> MirrorClass.Field<T, V>.lessThan(value: V): Condition<T?> = Condition.Field(this, Condition.LessThan(value))
infix fun <T : Any, V : Comparable<V>> MirrorClass.Field<T, V>.greaterThan(value: V): Condition<T?> = Condition.Field(this, Condition.GreaterThan(value))
infix fun <T : Any, V : Comparable<V>> MirrorClass.Field<T, V>.lessThanOrEqual(value: V): Condition<T?> = Condition.Field(this, Condition.LessThanOrEqual(value))
infix fun <T : Any, V : Comparable<V>> MirrorClass.Field<T, V>.greaterThanOrEqual(value: V): Condition<T?> = Condition.Field(this, Condition.GreaterThanOrEqual(value))
infix fun <T : Any, V : Comparable<V>> MirrorClass.Field<T, V>.between(value: ClosedRange<V>): Condition<T?> = (this lessThanOrEqual value.endInclusive) and (this greaterThanOrEqual value.start)
infix fun <T : Any, V> MirrorClass.Field<T, V>.equalToOne(values: List<V>): Condition<T?> = Condition.Field(this, Condition.EqualToOne(values))

infix fun <T : Any, V> MirrorClass.Field<T, V>.sub(condition: Condition<V>): Condition<T?> = Condition.Field(this, condition)

infix fun <T : Any> MirrorClass.Field<T, String>.textSearch(value: String): Condition<T?> = Condition.Field(this, Condition.TextSearch(value))
infix fun <T : Any> MirrorClass.Field<T, String>.startsWith(value: String): Condition<T?> = Condition.Field(this, Condition.StartsWith(value))
infix fun <T : Any> MirrorClass.Field<T, String>.endsWith(value: String): Condition<T?> = Condition.Field(this, Condition.EndsWith(value))

fun <T : Any, V: Comparable<V>> MirrorClass.Field<T, ClosedRange<V>>.contains(VMirror: MirrorType<V>, value: V): Condition<T?> {
    val m = ClosedRangeMirror(VMirror)
    return Condition.Field(this, m.fieldStart.lessThanOrEqual(value) and m.fieldEndInclusive.greaterThanOrEqual(value))
}

infix fun <T> Condition<T>.and(other: Condition<T>): Condition<T> = Condition.And(listOf(this, other)).simplify()
infix fun <T> Condition<T>.or(other: Condition<T>): Condition<T> = Condition.Or(listOf(this, other)).simplify()
operator fun <T> Condition<T>.not() = Condition.Not(this)


fun <T : Any> MirrorClass.Field<T, Geohash>.within(km: Double, of: Geohash): Condition<T> {
    val coverage = GeohashCoverage.coverRadiusRatio(
            center = of,
            radiusKm = km
    )
    return Condition.Or(coverage.ranges.map {
        Condition.And(listOf(
                this.sub(GeohashMirror.fieldBits greaterThanOrEqual it.start),
                this.sub(GeohashMirror.fieldBits lessThanOrEqual it.endInclusive)
        ))
    })
}


infix fun <T : Any> MirrorClass.Field<T, DaysOfWeek>.includes(dayOfWeek: DayOfWeek): Condition.Field<T, T, DaysOfWeek> {
    return when (dayOfWeek) {
        DayOfWeek.Sunday -> Condition.Field(this, Condition.Field(DaysOfWeekMirror.fieldSunday, Condition.Equal(true)))
        DayOfWeek.Monday -> Condition.Field(this, Condition.Field(DaysOfWeekMirror.fieldMonday, Condition.Equal(true)))
        DayOfWeek.Tuesday -> Condition.Field(this, Condition.Field(DaysOfWeekMirror.fieldTuesday, Condition.Equal(true)))
        DayOfWeek.Wednesday -> Condition.Field(this, Condition.Field(DaysOfWeekMirror.fieldWednesday, Condition.Equal(true)))
        DayOfWeek.Thursday -> Condition.Field(this, Condition.Field(DaysOfWeekMirror.fieldThursday, Condition.Equal(true)))
        DayOfWeek.Friday -> Condition.Field(this, Condition.Field(DaysOfWeekMirror.fieldFriday, Condition.Equal(true)))
        DayOfWeek.Saturday -> Condition.Field(this, Condition.Field(DaysOfWeekMirror.fieldSaturday, Condition.Equal(true)))
    }
}